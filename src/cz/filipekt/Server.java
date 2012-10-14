package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *  Representation of the server which serves clients and takes care of the committed data
 * @author Lifpa
 */
public final class Server {
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException{  
        try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
            String port = ServerUtils.getArgVal(args, "p");            
            int portNum = 0;
            boolean isCorrect = false;
            if((port!=null) && (!port.equals(""))){
                try{
                    portNum = Integer.parseInt(port);
                    isCorrect = true;
                } catch (RuntimeException ex){
                    isCorrect = false;
                }
            }                                
            while(!isCorrect){
                System.out.println("On which port do you want to listen?");
                String s = br.readLine();
                try{                    
                    portNum = Integer.valueOf(s);
                    if (portNum<0 || portNum>50000){
                        throw new NumberFormatException();
                    }
                    isCorrect = true;
                } catch (NumberFormatException ex){
                      System.err.println("You have to enter a valid port number.");
                }
            }            
            Server server = new Server(portNum, br);
            try{
                server.start();
            } finally{
                server.stop();            
            }
        }                        
    }
    
    /**
     * Standard input
     */
    private BufferedReader stdin;
    
    /**
     * The server accepts new connections on this port
     */
    private int listening_port;
    
    /**
     * Represents the filesystem of the files committed to the system <br/>
     * The actual data are not saved here, they are stored in separate files on disc instead
     */
    private Database db;
    
    public Server(int port, BufferedReader stdin) throws IOException, ClassNotFoundException{
        this.listening_port = port;
        this.stdin = stdin;
        loadHomeDir();
        loadDB();                
        loadScripts();
    }    
    
    /**
     * Begins to listen on the listening port and serves the clients
     */
    public void start() throws NoSuchAlgorithmException{
        try {
            try(ServerSocket server_socket = new ServerSocket(listening_port)) {
                System.out.println("Accepting a connection on port " + listening_port + " ...");
                for(int i = 0 ; i<10; i++){      
                    try(Socket socket = server_socket.accept()) {
                        System.out.println("Connection accepted with " + socket.getInetAddress().getHostAddress());
                        serve_client(socket);
                        saveDB();
                        saveScripts();
                        System.out.println("Connection terminated with " + socket.getInetAddress().getHostAddress());
                    } catch (IOException ex){
                        System.err.println("The client could not be served.");
                    }
                }
            }                            
            System.out.println("Accepting connections on port " + listening_port + " has been stopped ...");
        } catch (IOException ex) {
            System.err.println("Server socket not acquired");
        }
    }
    
    /**
     * Stops the server, saves relevant data on a persistent storage
     */
    public void stop(){
        saveDB();        
        saveScripts();
    }
    
    /**
     * Serves the client connected with "socket"
     * @param socket
     * @throws IOException 
     */
    private void serve_client(Socket socket) throws IOException, NoSuchAlgorithmException{
        OutputStream rout = socket.getOutputStream();
        InputStream rin = socket.getInputStream();        
        Kryo kryo = new Kryo();
        try (Output kryo_out = new Output(rout); Input kryo_in = new Input(rin)) {
            Vnejsi:
            while(true){
                try {                
                    Requests action_type = kryo.readObject(kryo_in, Requests.class);
                    List<String> fname;
                    switch(action_type){
                        case GET_LIST:                                
                            kryo.writeObject(kryo_out, db);
                            kryo_out.flush();
                            break;
                        case CREAT_FILE:
                            fname = kryo.readObject(kryo_in, LinkedList.class);
                            db.addFile(fname);
                            break;
                        case CREAT_VERS:
                            fname = kryo.readObject(kryo_in, LinkedList.class);
                            int bsize = kryo.readObject(kryo_in, Integer.TYPE);
                            LinkedList<DBlock> new_blocks;
                            LinkedList<DBlock> block_list = new LinkedList<>();                        
                            do{
                                new_blocks = loadBlock(kryo,kryo_in,bsize);
                                if (new_blocks==null) {
                                    break;
                                }
                                if (!new_blocks.isEmpty()) {
                                    for (DBlock block : new_blocks){
                                        block_list.addLast(block);
                                    }
                                }
                            } while (true);                        
                            DFile file = db.findFile(fname);
                            DVersion version = new DVersion(block_list, bsize);                        
                            if (file.futureNoOfConsecutiveScripts()> db.script_limit || !file.nonScriptExists()){
                                db.addVersion(fname, version);
                            } else {
                                DVersion zaklad = file.getLatestNonScript();
                                transformBlocksToScript(zaklad, version);
                                db.addVersion(fname, version);
                            }
                            break;
                        case GET_FILE:
                            fname = kryo.readObject(kryo_in, LinkedList.class);
                            int version_index = kryo.readObject(kryo_in, int.class);
                            DFile ds = db.findFile(fname);
                            List<Byte> res = serveGet(ds, version_index);
                            kryo.writeObject(kryo_out, res);
                            kryo_out.flush();
                            break;
                        case END:
                            break Vnejsi;
                        case CREAT_DIR:
                            List<String> path = kryo.readObject(kryo_in, LinkedList.class);
                            db.makeDirs(path);
                            break;
                    }
                } catch (IOException | ClassNotFoundException | MalformedPath ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }                
    
    /**
     * From a client at "oin" gets either new block, a piece of "raw" data or a signal of the end of communication <br/>
     * Raw data has to be parsed into blocks later...
     * @param db
     * @param oin
     * @param bsize
     * @return Newly loaded blocks from "oin"
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private LinkedList<DBlock> loadBlock(Kryo kryo, Input kryo_in, int bsize) throws IOException, ClassNotFoundException, NoSuchAlgorithmException{        
        String hash = kryo.readObject(kryo_in, String.class);
        switch(hash){
            case "end":
                return null;
            case "raw_data":                    
                LinkedList<Byte> data = kryo.readObject(kryo_in, LinkedList.class);
                return create_save_blocks(data,bsize);                    
            default:
                String hash2 = kryo.readObject(kryo_in, String.class);
                int valid = kryo.readObject(kryo_in, Integer.TYPE);
                DBlock block = db.findBlock(hash, hash2);
                LinkedList<DBlock> list = new LinkedList<>();
                list.add(block);
                return list;
        }                
    }    
    /**
     * Saves the contents ("bytes") of the block "hash" into the appropriate file on disc
     * @param bytes
     * @param hash
     * @return Returns the position in the hash collision list
     * @throws IOException 
     */
    private int saveBlock(List<Byte> bytes, String hash) throws IOException{
        boolean hotovo = false;
        int v = 0;
        while(!hotovo){
            String name = ServerUtils.getName(hash, v);
            Path p = Paths.get(home_dir, name);
            if(Files.exists(p)){
                v++;
                continue;
            } else {
                Files.createFile(p);
                try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(p))){
                    for (byte b : bytes){
                        bos.write((int)b + 128);
                    }
                }
                hotovo = true;
            }                        
        }
        return v;
    }    
    
    /**
     * Loads a valid instance of Database from disc
     */
    void loadDB() throws IOException, ClassNotFoundException{    
        Path f = Paths.get(home_dir, "index");
        if (Files.notExists(f)){
            Files.createFile(f);
        }
        if(Files.size(f)==0){            
            db = new Database(new HashMap<String,DItem>(), new TreeMap<String,DBlock>());
        } else {            
            try(ObjectInputStream oin = new ObjectInputStream(Files.newInputStream(f))) {                                
                db = (Database) oin.readObject();                
            }
        }
    }
    
    /**
     * Saves a valid instance of Database, ie. "db", on disc
     */
    void saveDB(){
        Path f = Paths.get(home_dir, "index");
        try(ObjectOutputStream oout = new ObjectOutputStream(Files.newOutputStream(f))){
            oout.writeObject(db);
        } catch (IOException ex){
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Home directory for the use by this program
     */
    private String home_dir;

    /**
     * Loads the home directory name either from Java preferences or from the stdin, <br/>
     * in case it has never been specified before
     * @throws IOException 
     */
    private void loadHomeDir() throws IOException  {
        Preferences p = Preferences.userNodeForPackage(Server.class);
        String s = p.get("adresar", "nedefinovano");
        if (s.equals("nedefinovano")){
            String addr = getDir();
            try {
                p.put("adresar", addr);
                p.flush();
                home_dir = addr;
            } catch (BackingStoreException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException();
            }
        } else {
            home_dir = s;
            Path path = Paths.get(home_dir);
            if(!Files.isDirectory(path)){
                home_dir = getDir();
            }
            try {
                p.put("adresar", home_dir);
                p.flush();                
            } catch (BackingStoreException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException();
            }
        }                                    
    }    
    
    /**
     * Asks the user for a valid directory, until the user enters a usable directory
     * @return
     * @throws IOException 
     */
    private String getDir() throws IOException{
        String addr = null;
        while(true){
            System.out.println("Where do you want to save the program data?");
            addr = stdin.readLine();                    
            Path path = Paths.get(addr);                
            try{
                Files.createDirectories(path);
                break;
            } catch (FileAlreadyExistsException | SecurityException ex){
                System.err.println("Sorry, the directory can not be used.");
            }
        }            
        return addr;
    }
    
    /**
     * Parses "data" into blocks of size "block_size". Their contents are saved <br/>
     * on disc, DBlocks are returned in a list
     * @param data
     * @param block_size
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    private LinkedList<DBlock> create_save_blocks(List<Byte> data, int block_size) throws NoSuchAlgorithmException, IOException{
        LinkedList<DBlock> res = new LinkedList<>();
        int i = 0;
        while(i<data.size()){
            int j = i + block_size;
            if (j>data.size()){
                j = data.size();
            }
            List<Byte> pars_data = data.subList(i, j);            
            int pouzito = block_size;
            if ((data.size() - i)<block_size){
                pouzito = data.size() - i;                       
            }
            String hash = RollingHash.computeHash(pars_data);
            String hash2 = RollingHash.computeHash2(pars_data);
            if(!db.blockExists(hash, hash2)){
                db.block_map.put(hash, new DBlock(hash, hash2, block_size, pouzito));
            }
            DBlock blok = db.findBlock(hash, hash2);
            blok.col = saveBlock(pars_data, hash);            
            res.addLast(blok);
            i += block_size;
        }
        return res;
    }
    
    /**
     * Transforms DVersion "predmet" into standard form - representation by blocks, <br/>
     * not by script
     * @param zaklad
     * @param predmet
     * @throws IOException
     * @throws PatchFailedException
     * @throws BlockNotFound
     * @throws NoSuchAlgorithmException 
     */
    private void transformScriptToBlocks(DVersion base, DVersion actual_object) throws IOException, PatchFailedException, BlockNotFound, NoSuchAlgorithmException{
        String obsahZaklad = ServerUtils.byteToString(ServerUtils.loadVersionFromDisc(base, home_dir));
        List<String> list = new LinkedList<>();
        list.add(obsahZaklad);
        List<String> list2;
        list2 = (List<String>) DiffUtils.patch(list, getScript(actual_object));
        String novyObsah = list2.get(0);
        
        //mam ted obsah skriptovane version_list, je treba naparsovat do bloku                
        List<DBlock> bloky = create_save_blocks(ServerUtils.stringToByte(novyObsah), actual_object.block_size);
        actual_object.editational = false;
        actual_object.blocks = bloky;
        setScript(actual_object, null);
    }  
    
    /**
     * Add an editational script into database
     * @param version_list
     * @param skript 
     */
    private void setScript(DVersion verze, Patch skript){
        scripts.put(verze, skript);
    }
    
    /**
     * Retrieves an editational script from the database
     * @param version_list
     * @return 
     */
    private Patch getScript(DVersion verze){
        return scripts.get(verze);
    }
    
    /**
     * Each DVersion represented as a script, is here mapped to its script
     */
    Map<DVersion,Patch> scripts;
    
    /**
     * Loads all the scripts from disc
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    void loadScripts() throws IOException, ClassNotFoundException{
        Path f = Paths.get(home_dir, "skripty");
        if (Files.notExists(f)){
            Files.createFile(f);
        }
        if(Files.size(f)==0){            
            scripts = new HashMap<>();
        } else {            
            try(ObjectInputStream oin = new ObjectInputStream(Files.newInputStream(f))) {                                
                scripts = (Map<DVersion,Patch>) oin.readObject();
            }
        }
    }
    
    /**
     * Saves all the scripts on disc
     */
    void saveScripts(){
        Path f = Paths.get(home_dir, "skripty");
        try(ObjectOutputStream oout = new ObjectOutputStream(Files.newOutputStream(f))){
            oout.writeObject(scripts);
        } catch (IOException ex){
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    
    /**
     * Returns the contents of the "index"-th version of "file_to_get", in case <br/>
     * it is need a transformation from script form is done
     * @param version_list
     * @return
     * @throws IOException
     * @throws BlockNotFound 
     */
    private List<Byte> serveGet(DFile file_to_get, int index) {
        try{
            DVersion verze = file_to_get.version_list.get(index);
            if(verze.editational){
                DVersion zaklad;
                int i;
                for(i = index-1; i>=0; i--){
                    if (!file_to_get.version_list.get(i).editational){
                        break;
                    }
                }
                zaklad = file_to_get.version_list.get(i);
                List<Byte> obsahZaklad = ServerUtils.loadVersionFromDisc(zaklad, home_dir);
                String obsahZakladString = ServerUtils.byteToString(obsahZaklad);
                List<String> list = new LinkedList<>();
                list.add(obsahZakladString);
                List<String> res = (List<String>) DiffUtils.patch(list, getScript(verze));
                String res1 = res.get(0);
                return ServerUtils.stringToByte(res1);    
            } else {
                return ServerUtils.loadVersionFromDisc(verze, home_dir);
            }
        } catch (PatchFailedException | IOException | BlockNotFound ex){
            System.err.println("\"Get\" request could not be served.");
            return null;
        }
    }

    /**
     * Transforms the "new_vers" version into the script form against the "base" version
     * @param zakladni
     * @param nova
     * @throws IOException 
     */
    private void transformBlocksToScript(DVersion base, DVersion new_vers) throws IOException{
        List<Byte> obsahSouboru = new LinkedList<>();
        for(DBlock blok : base.blocks){
            List<Byte> blokData = ServerUtils.loadBlockFromDisc(blok, home_dir);
            obsahSouboru.addAll(blokData);
        }
        String obsahZakladni = ServerUtils.byteToString(obsahSouboru);
        
        obsahSouboru.clear();
        for(DBlock blok : new_vers.blocks){
            List<Byte> blokData = ServerUtils.loadBlockFromDisc(blok, home_dir);
            obsahSouboru.addAll(blokData);            
        }
        String obsahNova = ServerUtils.byteToString(obsahSouboru);
        List<String> l1 = new LinkedList<>();
        List<String> l2 = new LinkedList<>();
        l1.add(obsahZakladni);
        l2.add(obsahNova);
        Patch patch = DiffUtils.diff(l1,l2);
        new_vers.editational = true;
        setScript(new_vers, patch);
        new_vers.blocks = null;
    }    
    
}

class BlockNotFound extends Exception{}