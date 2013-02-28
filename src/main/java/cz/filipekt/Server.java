package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 *  Representation of the server which serves clients and takes care of the committed data
 * @author Lifpa
 */
public final class Server {
    public static void main(String[] args) 
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException{  
        
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
            
            List<String> args2 = ServerUtils.concatQuotes(Arrays.asList(args));            
            String homeDir = ServerUtils.getArgVal(args2.toArray(new String[0]), "home");            
            if ((homeDir!=null) && (!homeDir.equals(""))){                
                Path p = Paths.get(homeDir);
                try {
                    Files.createDirectories(p);
                } catch (Exception ex){
                    System.err.println("Invalid home directory specified.");
                    return;                    
                }             
            }
            Server server = new Server(portNum, br, homeDir, args);
            System.out.println("Granted space: " + server.getReservedSpace(args) + " bytes");
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
    
    /**
     * Size (in bytes) of the HDD space reserved for the application
     */
    private final long reservedSpace;
    
    public Server(int port, BufferedReader stdin, String homeDir, String[] args) throws IOException, ClassNotFoundException{
        this.listening_port = port;
        this.stdin = stdin;
        this.home_dir = homeDir;
        if (homeDir == null) {
            loadHomeDir();
        }
        try {
            loadDB();
            loadScripts();
        } catch (IOException | ClassNotFoundException ex){
                System.err.println(ex.getMessage());
                System.exit(1);
        }                        
        reservedSpace = getReservedSpace(args);
        zipBuffer = new ByteArrayOutputStream();
    }    
    
    private long getAvailableSpace() throws IOException{        
        return reservedSpace - ServerUtils.getDirectorySize(Paths.get(home_dir));
    }
    
    /**
     * Begins to listen on the listening port and serves the clients
     */
    public void start() throws NoSuchAlgorithmException{
        try {
            try(ServerSocket server_socket = new ServerSocket(listening_port)) {
                System.out.println("Accepting a connection on port " + listening_port + " ...");
                while(true){
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
        try (Output kryo_out = new Output(rout); Input kryo_in = new Input(rin); ObjectOutputStream oos = new ObjectOutputStream(rout)) {            
            Vnejsi:
            while(true){
                try {                
                    Requests action_type = kryo.readObject(kryo_in, Requests.class);
                    List<String> fname;
                    long size;
                    switch(action_type){
                        case GET_LIST:                                
                            oos.reset();
                            oos.writeObject(db);
                            oos.flush();
                            break;
                        case GET_LIGHT_DATABASE:
                            LightDatabase ld = db.getLightDatabase();
                            kryo.writeObject(kryo_out, ld, LightDatabase.getSerializer());
                            kryo_out.flush();
                            break;
                        case ITEM_EXISTS:
                            String[] targetPath = kryo.readObject(kryo_in, String[].class);                            
                            if (targetPath != null){
                                kryo.writeObject(kryo_out, db.itemExists(Arrays.asList(targetPath)));
                                kryo_out.flush();
                            }
                            break;
                        case CREAT_FILE:
                            size = kryo.readObject(kryo_in, long.class);
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            if (size < getAvailableSpace()){
                                db.addFile(fname);
                                kryo.writeObject(kryo_out, Boolean.TRUE);                                  
                            } else {
                                for(int i = 0; (i<GC_ROUNDS_COUNT) && (size >= getAvailableSpace()); i++){
                                    removeOldItems();                                    
                                }
                                if (size < getAvailableSpace()){
                                    db.addFile(fname);
                                    kryo.writeObject(kryo_out, Boolean.TRUE);                                
                                } else {
                                    kryo.writeObject(kryo_out, Boolean.FALSE);
                                }                            
                            }
                            kryo_out.flush();
                            break;
                        case CREAT_VERS:                            
                            size = kryo.readObject(kryo_in, long.class);
                            if (size < getAvailableSpace()){
                                kryo.writeObject(kryo_out, Boolean.TRUE);
                            } else {
                                for (int i = 0; (i<GC_ROUNDS_COUNT) && (size >= getAvailableSpace()); i++){
                                    removeOldItems();                                    
                                }
                                if (size >= getAvailableSpace()){
                                    kryo.writeObject(kryo_out, Boolean.FALSE);
                                    break;
                                } else {
                                    kryo.writeObject(kryo_out, Boolean.TRUE);
                                }
                            }
                            kryo_out.flush();
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            int bsize = kryo.readObject(kryo_in, Integer.TYPE);
                            List<DBlock> new_blocks;
                            List<DBlock> block_list = new ArrayList<>();                        
                            do{
                                new_blocks = loadBlock(kryo, kryo_in, kryo_out, bsize);
                                if (new_blocks==null) {
                                    break;
                                }
                                if (!new_blocks.isEmpty()) {
                                    for (DBlock block : new_blocks){
                                        block_list.add(block);
                                    }
                                }
                            } while (true);                        
                            DFile file = db.findFile(fname);
                            DVersion version = new DVersion(block_list, bsize, file.getName()); 
                            ServerUtils.linkBlocksToVersion(version);
                            if (file.futureNoOfConsecutiveScripts()> db.getScriptLimit() || !file.nonScriptExists()){
                                db.addVersion(fname, version);
                            } else {
                                DVersion zaklad = file.getLatestNonScript();
                                ServerUtils.transformBlocksToScript(zaklad, version, home_dir, scripts);
                                db.addVersion(fname, version);
                            }
                            break;
                        case GET_FILE:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            int version_index = kryo.readObject(kryo_in, int.class);
                            DFile ds = db.findFile(fname);
                            if (ds != null){
                                byte[] res = serveGet(ds, version_index);
                                kryo.writeObject(kryo_out, res);
                                kryo_out.flush();
                            }
                            break;
                        case END:
                            break Vnejsi;
                        case CREAT_DIR:
                            size = kryo.readObject(kryo_in, long.class);
                            if (size < getAvailableSpace()){
                                List<String> path = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                                db.makeDirs(path);    
                                kryo.writeObject(kryo_out, Boolean.TRUE);                                
                            } else {
                                for(int i = 0; (i<GC_ROUNDS_COUNT) && (size >= getAvailableSpace()); i++){
                                    removeOldItems();                                    
                                }
                                if (size < getAvailableSpace()){
                                    List<String> path = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                                    db.makeDirs(path);    
                                    kryo.writeObject(kryo_out, Boolean.TRUE);                                
                                } else {
                                    kryo.writeObject(kryo_out, Boolean.FALSE);
                                }                                  
                            }
                            kryo_out.flush();
                            break;
                        case GET_ZIP:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            version_index = kryo.readObject(kryo_in, int.class);
                            DItem item = db.getItem(fname);
                            if (item != null){
                                HashMap<DItem,String> fileList = new HashMap<>();
                                getAllFiles(item, fileList, "");
                                if ((fileList.size() == 1) && (!fileList.keySet().iterator().next().isDir())){
                                    writeZipFile(item, fileList, version_index);
                                } else {
                                    writeZipFile(item, fileList, -1);
                                }                                
                                try (ByteArrayInputStream is = new ByteArrayInputStream(zipBuffer.toByteArray())){
                                    int c;
                                    List<Integer> data = new ArrayList<>();
                                    while ((c=is.read())!=-1){
                                        data.add(c);
                                    }
                                    kryo.writeObject(kryo_out, (Integer) data.size());
                                    for (int i = 0; i<data.size(); i++){
                                        kryo.writeObject(kryo_out, data.get(i));
                                    }
                                    kryo_out.flush();
                                }
                                zipBuffer.reset();
                            }
                            break;
                    }
                } catch (IOException | ClassNotFoundException | MalformedPath | NotEnoughSpaceOnDisc | BlockNotFound ex) {
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
    private List<DBlock> loadBlock(Kryo kryo, Input kryo_in, Output kryo_out, int bsize) 
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{   
        
        String message = kryo.readObject(kryo_in, String.class);
        while (message.equals("get_light_db")){
            LightDatabase ld = db.getLightDatabase();
            kryo.writeObject(kryo_out, ld, LightDatabase.getSerializer());
            kryo_out.flush();
            message = kryo.readObject(kryo_in, String.class);
        }
        switch(message){
            case "end":
                return null;
            case "raw_data":                    
                byte[] data = kryo.readObject(kryo_in, byte[].class);
                return create_save_blocks(data,bsize);                    
            case "hash":
                long hash = kryo.readObject(kryo_in, long.class);
                String hash2 = kryo.readObject(kryo_in, String.class);
                int valid = kryo.readObject(kryo_in, Integer.TYPE);
                DBlock block = db.findBlock(hash, hash2);
                return Arrays.asList(block);            
            default:
                throw new ProtocolException();
        }                
    }    

    /**
     * Safely deletes "versionNum"-th version of "file" both from database and disc
     * @param file
     * @param versionNum
     * @throws TooFewVersions
     * @throws IOException
     * @throws PatchFailedException
     * @throws BlockNotFound
     * @throws NoSuchAlgorithmException 
     */
    void safelyDeleteVersion(DFile file, int versionNum) 
            throws TooFewVersions, IOException, PatchFailedException, BlockNotFound, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{
        if (file.getVersionList().size()<=1){
            throw new TooFewVersions();
        }
        if((versionNum >= file.getVersionList().size())||(versionNum < 0)){
            throw new IndexOutOfBoundsException();
        } 
        
        // scripted version special case..?
        
        if (versionNum == file.getVersionList().size()-1){
            ServerUtils.unlinkBlocksFromVersion(file.getVersionList().get(versionNum));
            scripts.remove(file.getVersionList().get(versionNum));
            file.getVersionList().remove(versionNum);
        } else if (versionNum==0) {
            if(file.getVersionList().get(1).isScriptForm()){
                transformScriptToBlocks(file.getVersionList().get(0), file.getVersionList().get(1));
                ServerUtils.unlinkBlocksFromVersion(file.getVersionList().get(0));
                file.getVersionList().remove(0);
            }
        } else if (file.getVersionList().get(versionNum).isScriptForm()){
            scripts.remove(file.getVersionList().get(versionNum));
            file.getVersionList().remove(versionNum);
        } else {
            DVersion base = file.getLatestNonScript(versionNum - 1);
            DVersion base2 = file.getVersionList().get(versionNum - 1);            
            DVersion toDelete = file.getVersionList().get(versionNum);            
            if (base2.isScriptForm()){
                transformScriptToBlocks(base, base2);
            }
            for(int i = versionNum+1; ((i < file.getVersionList().size())&&(file.getVersionList().get(i).isScriptForm())); i++){
                transformScriptToBlocks(toDelete, file.getVersionList().get(i));
                ServerUtils.transformBlocksToScript(base2, file.getVersionList().get(i), home_dir, scripts);
            }
            ServerUtils.unlinkBlocksFromVersion(toDelete);
            file.getVersionList().remove(toDelete);
        }
    }
    
    /**
     * Deletes a version of a file. In case the version is saved saved as blocks, </br>
     * also all the dependent scripted versions are deleted. </br>
     * @param file
     * @param versionNum
     * @throws TooFewVersions 
     */
    private void unsafelyDeleteVersion(DFile file, int versionNum) throws TooFewVersions{
        if((versionNum >= file.getVersionList().size())||(versionNum < 0)){
            throw new IndexOutOfBoundsException();
        } 
        if (file.getVersionList().get(versionNum).isScriptForm()){
            scripts.remove(file.getVersionList().get(versionNum));
            file.getVersionList().remove(versionNum);            
        } else {
            ServerUtils.unlinkBlocksFromVersion(file.getVersionList().get(versionNum));
            file.getVersionList().remove(versionNum);
            while (file.getVersionList().get(versionNum).isScriptForm()){
                unsafelyDeleteVersion(file, versionNum);
            }
        }
        
    }        
    
    /**
     * Collects and deletes all blocks that are not referenced anywhere
     */
    private void collect_blocks() throws IOException{
        System.out.println("Before GC: " + getAvailableSpace() + "B available");        
        for (DBlock block : db.collectBlocks()){
            String name = block.getName();
            ServerUtils.deleteBlock(name, home_dir);
        }
        System.out.println("After GC: " + getAvailableSpace() + "B available");
    }
    
    /**
     * Deletes the file versions found by findUnnecessaryVersions()
     */
    void removeOldItems() throws IOException{        
        Map<DVersion,DFile> versionsToDelete = findUnnecessaryVersions();
        for (Entry<DVersion,DFile> entry : versionsToDelete.entrySet()){
            int verNum = entry.getValue().getVersionList().indexOf(entry.getKey());
            try {
                unsafelyDeleteVersion(entry.getValue(), verNum);
            } catch (TooFewVersions ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("System couldn't delete some of the older versions " + 
                        "of a file. The reserved HDD space will probably soon be filled up.");
            }
        }        
        collect_blocks();
    }
    
    /**
     * Finds the old file versions that are least likely to be missed, if deleted
     * @return 
     */
    private Map<DVersion,DFile> findUnnecessaryVersions(){
        List<CustomTuple> allVersions = new ArrayList<>();
        for (DFile file : db.getFileList()){
            for (DVersion version : file.getVersionList()){
                if (!version.isScriptForm()){
                    allVersions.add(new CustomTuple(version,file));
                }
            }
        }
        Collections.sort(allVersions);        
        Map<DVersion,DFile> res = new HashMap<>();
        for (CustomTuple t : allVersions){
            if(t.getNumberOfNewerVers()>=1){
                res.put(t.v, t.f);
            }
        }
        return res;
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
            db = new Database(new HashMap<String,DItem>(), new TreeMap<String,DBlock>(), new TreeSet<Long>(), new TreeSet<String>());
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
        if (home_dir != null){
            return;
        }
//        Preferences p = Preferences.userNodeForPackage(Server.class);
//        String s = p.get("adresar", "__nedefinovano");
//        if (s.equals("__nedefinovano")){
            String addr = getDir();
//            try {
//                p.put("adresar", addr);
//                p.flush();
                home_dir = addr;
//            } catch (BackingStoreException ex) {
//                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
//                throw new IOException();
//            }
//        } else {
//            home_dir = s;
//            Path path = Paths.get(home_dir);
//            if(!Files.isDirectory(path)){
//                home_dir = getDir();
//            }
//            try {
//                p.put("adresar", home_dir);
//                p.flush();                
//            } catch (BackingStoreException ex) {
//                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
//                throw new IOException();
//            }
//        }                                    
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
     * Each DVersion represented as a script, is here mapped to its script
     */
    private Map<DVersion,Patch> scripts;
    
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
     * @param versionList
     * @return
     * @throws IOException
     * @throws BlockNotFound 
     */
    private byte[] serveGet(DFile file_to_get, int index) {
        try{
            DVersion verze = file_to_get.getVersionList().get(index);
            if(verze.isScriptForm()){
                DVersion zaklad;
                int i;
                for(i = index-1; i>=0; i--){
                    if (!file_to_get.getVersionList().get(i).isScriptForm()){
                        break;
                    }
                }
                zaklad = file_to_get.getVersionList().get(i);
                byte[] obsahZaklad = ServerUtils.loadVersionFromDisc(zaklad, home_dir);
                String obsahZakladString = ServerUtils.byteToString(obsahZaklad);
                List<String> list = new ArrayList<>();
                list.add(obsahZakladString);
                List<String> res = (List<String>) DiffUtils.patch(list, ServerUtils.getScript(verze,scripts));
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
     * Returns the number of bytes avaliable on disc for the application.
     * @param args
     * @return
     * @throws IOException 
     */
    private long getReservedSpace(String[] args) throws IOException{
        String val = ServerUtils.getArgVal(args, "space");
        if (ServerUtils.isLong(val)){
            long desired = Long.parseLong(val);
            long available = FileSystems.getDefault().provider().getFileStore(Paths.get(home_dir)).getUsableSpace();
            return (desired<available) ? desired : available;
        } else {                 
            return FileSystems.getDefault().provider().getFileStore(Paths.get(home_dir)).getUsableSpace() / AVAILABLE_SPACE_FACTOR;
        }
    }
    
    private static final long AVAILABLE_SPACE_FACTOR = 5;       
    
    /**
     * Transforms DVersion "actual_object" into standard form - representation by blocks, <br/>
     * not by script </br>
     * @param reference_base
     * @param actual_object
     * @throws IOException
     * @throws PatchFailedException
     * @throws BlockNotFound
     * @throws NoSuchAlgorithmException 
     */
    private void transformScriptToBlocks(DVersion reference_base, DVersion actual_object) 
            throws IOException, PatchFailedException, BlockNotFound, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{
        String obsahZaklad = ServerUtils.byteToString(ServerUtils.loadVersionFromDisc(reference_base, home_dir));
        List<String> list = new ArrayList<>();
        list.add(obsahZaklad);
        List<String> list2;
        list2 = (List<String>) DiffUtils.patch(list, ServerUtils.getScript(actual_object,scripts));
        String novyObsah = list2.get(0);
        
        //mam ted obsah skriptovane versionList, je treba naparsovat do bloku                
        List<DBlock> bloky = create_save_blocks(ServerUtils.stringToByte(novyObsah), actual_object.getBlockSize());
        actual_object.setScriptForm(false);
        actual_object.setBlocks(bloky);
        ServerUtils.linkBlocksToVersion(actual_object);
        scripts.remove(actual_object);
    }  
    
    /**
     * Parses "data" into blocks of size "blockSize". Their contents are saved <br/>
     * on disc, DBlocks are returned in a list
     * @param data
     * @param blockSize
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    private List<DBlock> create_save_blocks(byte[] data, int block_size) 
            throws NoSuchAlgorithmException, IOException, NotEnoughSpaceOnDisc{
        List<DBlock> res = new ArrayList<>();
        int left = 0;
        
        while(left<data.length){
            int right = left + block_size;
//            if (right>data.length){
//                right = data.length;
//            }     
            byte[] pars_data = Arrays.copyOfRange(data, left, right);
            int used = block_size;
            if ((data.length - left)<block_size){
                used = data.length - left;                       
            }
            long hash = RollingHash.computeHash(pars_data);
            String hash2 = RollingHash.computeHash2(pars_data);

            if (pars_data.length >= getAvailableSpace()){                            
                for(int i1 = 0; (i1<GC_ROUNDS_COUNT) && (pars_data.length >= getAvailableSpace()); i1++){
                    removeOldItems();                    
                }            
                if(pars_data.length >= getAvailableSpace()){
                    throw new NotEnoughSpaceOnDisc();
                }
            }            
            int col = 0;            
            DBlock newBlock = new DBlock(hash, hash2, block_size, used);
            if(!db.blockExists(hash, hash2)){                
                col = ServerUtils.saveBlock(pars_data, hash, home_dir);            
                newBlock.setCol(col);
                db.addBlock(newBlock);
            } else {
                newBlock = db.findBlock(hash, hash2);
            }            
            res.add(newBlock);
            left += block_size;
        }
        return res;
    }    
    
    /**
     * Upper bound on the number of block collections before giving up
     */
    private static final int GC_ROUNDS_COUNT = 5;
    
    void getAllFiles(DItem dir, Map<DItem,String> fileList, String path){
        if (path.equals("")){
            fileList.put(dir, path);
            if (dir.isDir()){
                getAllFiles(dir, fileList, dir.getName());
            } 
        } else {
            for (DItem file : ((DDirectory)dir).getItemMap().values()){
                fileList.put(file, path);
                if (file.isDir()){
                        getAllFiles(file, fileList, path + File.separator + file.getName());
                }
            }       
        }
    }

    /**
     * Temporary space to save zipped files && folders before they are sent to a client's machine.
     */
    private ByteArrayOutputStream zipBuffer;    
    
    void writeZipFile(DItem directoryToZip, Map<DItem,String> fileList, int versionNumber) 
            throws FileNotFoundException, IOException {            
            try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
                    for (Entry<DItem,String> entry : fileList.entrySet()) {
                        DItem file = entry.getKey();
                        if (!file.isDir()) {
                                addToZip(directoryToZip, file, versionNumber, zos, entry.getValue());
                        } else if (((DDirectory)file).getItemMap().isEmpty()){
                                addToZip(directoryToZip, file, versionNumber, zos, entry.getValue());                            
                        }
                    }
            }            
    }
    
    void addToZip(DItem directoryToZip, DItem item, int versionNumber, ZipOutputStream zos, String path) 
            throws FileNotFoundException, IOException {
        if (item.isDir()){
            String zipFilePath;
            if (!path.equals("")) {
                zipFilePath = path + "/" + item.getName();
            } else {
                zipFilePath = item.getName();
            }    
            zipFilePath += "/";
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);                
            zos.closeEntry();
        } else {
            DFile file = (DFile) item;
            byte[] data = serveGet(file, versionNumber==-1 ? file.getVersionList().size()-1 : versionNumber);
            String zipFilePath;
            if (!path.equals("")) {
                zipFilePath = path + "/" + file.getName();
            } else {
                zipFilePath = file.getName();
            }    
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);
            for (Byte b : data){
                int c = ((int)b) + 128;
                zos.write(c);
            }
            zos.closeEntry();
        }
    }   
}

class BlockNotFound extends Exception {}
class NotEnoughSpaceOnDisc extends Exception {}

class CustomTuple implements Comparable<CustomTuple> {
    DVersion v;
    DFile f;
    public CustomTuple(DVersion v, DFile f){
        this.v = v;
        this.f = f;
    }

    @Override
    public int compareTo(CustomTuple o) {
        Integer val = metricValue();
        Integer o_val = o.metricValue();
        return val.compareTo(o_val);        
    }
    
    int getNumberOfNewerVers(){
        int index = f.getVersionList().indexOf(v);
        int count = 0;
        for (int i = index+1; i<f.getVersionList().size(); i++){
            if (!f.getVersionList().get(i).isScriptForm()){
                count++;
            }
        }
        return count;
    }
    private int getApprSize(){
        return (v.getBlockSize() * v.getBlocks().size());
    }
    
    /**
     * Computes the value of this object in the metric used to asses </br>
     * the importance of old file versions </br>
     * @return 
     */
    int metricValue(){
        return getNumberOfNewerVers() * getApprSize();
    }
}