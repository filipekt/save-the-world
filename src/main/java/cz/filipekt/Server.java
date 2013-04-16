package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import cz.filipekt.diff.EditScript;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executor;
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
            Locale locale = new Locale("en","US");
            String localeArgs = ServerUtils.getArgVal(args, "locale", false);
            if ((localeArgs!=null) && (localeArgs.equalsIgnoreCase("cz"))){
                locale = new Locale("cs","CZ");    
            }
            ResourceBundle messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale); 
            
            String port = ServerUtils.getArgVal(args, "p", false);            
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
                System.out.println(messages.getString("which_port"));
                String s = br.readLine();
                try{                    
                    portNum = Integer.valueOf(s);
                    if (portNum<0 || portNum>50000){
                        throw new NumberFormatException();
                    }
                    isCorrect = true;
                } catch (NumberFormatException ex){
                      System.err.println(messages.getString("enter_valid_port"));
                }
            }           
            String homeDir = ServerUtils.getArgVal(args, "home", true);
            if ((homeDir!=null) && (!homeDir.equals(""))){                
                Path p = Paths.get(homeDir);
                try {
                    Files.createDirectories(p);
                } catch (Exception ex){
                    System.err.println(messages.getString("invalid_homedir"));
                    return;                    
                }             
            } else {
                isCorrect = false;
                while (!isCorrect){
                    System.out.println(messages.getString("what_home_dir"));
                    String s = br.readLine();
                    if ((s!=null) && !s.equals("")){
                        homeDir = s;
                        break;
                    }
                }                
            }
            Server server = new Server(portNum, br, homeDir, args, messages);
            long grantedSpace = server.getReservedSpace();
            if (grantedSpace == 0L){
                System.out.println(messages.getString("granted_space") + " " + messages.getString("unlimited"));
            } else {
                System.out.println(messages.getString("granted_space") + " " + server.getReservedSpace() + " " + "B");            
            }
            server.start();            
        }                        
    }
    
    /**
     * Contains all the localized messages that are shown to the user.
     */
    private final ResourceBundle messages;               
    
    /**
     * Standard input
     */
    private final BufferedReader stdin;
    
    /**
     * The server accepts new connections on this port
     */
    private final int listening_port;
    
    /**
     * Represents the filesystem of the files committed to the system <br/>
     * The actual data are not saved here, they are stored in separate files on disc instead
     */
    private final Database db;
    
    /**
     * Size (in bytes) of the HDD space reserved for the application
     */
    private final long reservedSpace;

    private long getReservedSpace() {
        return reservedSpace;
    }
    
    /**
     * Thread pool for serving user requests.
     */
    private final Executor threadPool;
    
    /**
     * Amount of hdd space that will always be left free by this application.
     */
    private final long spaceNotUsed;
    
    private Server(int port, BufferedReader stdin, String homeDir, String[] args, 
            ResourceBundle messages) throws IOException, ClassNotFoundException{
        this.listening_port = port;
        this.stdin = stdin;        
        if (homeDir == null) {
            this.home_dir = getDir();
        } else {
            this.home_dir = homeDir;
        }        
        Files.createDirectories(Paths.get(home_dir));
        this.spaceNotUsed = Files.getFileStore(Paths.get(this.home_dir)).getUsableSpace() / 10;
        db = loadDB();                    
        scripts = loadScripts();
        reservedSpace = computeReservedSpace(args);
        zipBuffer = new ByteArrayOutputStream();
        this.messages = messages;
        this.threadPool = java.util.concurrent.Executors.newCachedThreadPool();
    }    
    
    /**
     * Tells how much hdd space is available for the application.
     * @return
     * @throws IOException 
     */
    private long getAvailableSpace() throws IOException{        
        long usable = Files.getFileStore(Paths.get(home_dir)).getUsableSpace();
        if (reservedSpace == 0){            
            long res = usable - spaceNotUsed;
            return (res > 0) ? res : 0;
        } else {
            long atMost = reservedSpace - ServerUtils.getDirectorySize(Paths.get(home_dir));
            long res = (atMost > usable) ? usable : atMost;            
            return (res > 0) ? res : 0;
        }
    }
    
    /**
     * Begins to listen on the listening port and serves the clients
     */
    private void start() {
        try(final ServerSocket server_socket = new ServerSocket(listening_port)) {
            System.out.println(messages.getString("accepting_on_port") + " " + listening_port + " ...");
            while(true){
                final Socket socket = server_socket.accept();
                if (socket != null){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {                                                        
                            try {                                
                                System.out.println(messages.getString("conn_acc_with") + " " + socket.getInetAddress().getHostAddress());                    
                                serveClient(socket);
                                saveState();
                                System.out.println(messages.getString("conn_term_with") + " " + socket.getInetAddress().getHostAddress());
                            } catch (IOException | NoSuchAlgorithmException ex){
                                System.err.println(messages.getString("client_at") + " "  + socket.getInetAddress().getHostAddress() + " " + messages.getString("couldnt_be_served"));                                
                            } finally {                                
                                try {
                                    socket.close();
                                } catch (IOException ex) {
                                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                }                                
                            }
                        }
                    });             
                }
            }
        } catch (IOException ex) {                            
            System.err.println(messages.getString("socket_not_ack") + " " + ex.getLocalizedMessage());
        }                            
    }
    
    /**
     * Lock object is used for synchronization.
     */
    private final Object lockObject = new Object();   
    
    /**
     * Saves database data to a persistent storage
     */
    private void saveState(){
        synchronized(lockObject){
            saveDB();        
            saveScripts();
        }
    }
    
    /**
     * Serves the client connected with "socket"
     * @param socket
     * @throws IOException 
     */
    private void serveClient(Socket socket) throws IOException, NoSuchAlgorithmException{
        OutputStream rout = socket.getOutputStream();
        InputStream rin = socket.getInputStream();        
        Kryo kryo = new Kryo(null);
        kryo.setAutoReset(true);
        try (Output kryo_out = new Output(rout); Input kryo_in = new Input(rin)) {
            Vnejsi:
            while(true){
                try {                
                    Requests action_type = kryo.readObject(kryo_in, Requests.class);
                    List<String> fname;
                    long size;
                    switch(action_type){
                        case GET_DB:                             
                            kryo.writeObject(kryo_out, db, Database.getSerializer());
                            kryo_out.flush();
                            break;
                        case GET_LIGHT_DB:
                            LightDatabase ld = db.getLightDatabase();
                            kryo.writeObject(kryo_out, ld, LightDatabase.getSerializer());
                            kryo_out.flush();
                            break;
                        case ITEM_EXISTS:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            kryo_out.writeBoolean(db.itemExists(fname));
                            kryo_out.flush();
                            break;
                        case DEL_VERS:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            int versionIndex = kryo_in.readInt();
                            DItem item = db.getItem(fname);
                            if ((item == null) || item.isDir()){
                                kryo_out.writeBoolean(false);
                            } else {
                                try {
                                    DFile file = (DFile) item;
                                    safelyDeleteVersion(file, versionIndex);
                                    kryo_out.writeBoolean(true);
                                } catch (BlockNotFound | IOException | NoSuchAlgorithmException | 
                                        NotEnoughSpaceOnDisc | TooFewVersions ex){
                                    kryo_out.writeBoolean(false);
                                }
                            }
                            kryo_out.flush();
                            break;
                        case CREAT_FILE:
                            size = kryo_in.readLong();
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            if (size < getAvailableSpace()){
                                db.addFile(fname);
                                kryo_out.writeBoolean(true);
                            } else {                                
                                removeOldItems(1);                                                                    
                                if (size < getAvailableSpace()){
                                    db.addFile(fname);
                                    kryo_out.writeBoolean(true);
                                } else {
                                    kryo_out.writeBoolean(false);
                                }                            
                            }
                            kryo_out.flush();
                            break;
                        case CHECK_CHANGES:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            String contentHash = kryo_in.readString();
                            DFile file = db.findFile(fname);
                            if (file != null){
                                DVersion latestVersion = file.getLatestVersion();
                                if (latestVersion == null){
                                    kryo_out.writeBoolean(true);
                                } else {
                                    kryo_out.writeBoolean(!latestVersion.getContentHash().equals(contentHash));
                                }                                                                                                
                                kryo_out.flush();
                            }
                            break;
                        case CREAT_VERS:                             
                            size = kryo_in.readLong();
                            if (size < getAvailableSpace()){
                                kryo_out.writeBoolean(true);
                            } else {                                
                                removeOldItems(1);                                                                    
                                if (size >= getAvailableSpace()){
                                    kryo_out.writeBoolean(false);
                                    kryo_out.flush();
                                    break;
                                } else {
                                    kryo_out.writeBoolean(true);
                                }
                            }
                            kryo_out.flush();
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            int bsize = kryo_in.readInt();
                            contentHash = kryo_in.readString();
                            List<DBlock> new_blocks;
                            List<DBlock> block_list = new ArrayList<>();                        
                            do{
                                new_blocks = loadBlock(kryo_in, kryo_out, bsize);
                                if (new_blocks==null) {
                                    break;
                                }
                                if (!new_blocks.isEmpty()) {
                                    for (DBlock block : new_blocks){
                                        block_list.add(block);
                                    }
                                }
                            } while (true);                        
                            synchronized (lockObject){
                                file = db.findFile(fname);
                                DVersion version = new DVersion(block_list, bsize, file.getName(), contentHash, size); 
                                ServerUtils.linkBlocksToVersion(version);
                                if (file.blockVersionExists()){                                                                
                                    DVersion base = file.getLatestNonScript();
                                    transformBlocksToScript(base, version, true);
                                }
                                file.getVersionList().add(version);                                
                            }
                            kryo_out.writeBoolean(true);
                            kryo_out.flush();
                            break;
                        case GET_FILE:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            versionIndex = kryo_in.readInt();
                            DFile ds = db.findFile(fname);
                            if (ds != null){
                                byte[] res = serveGet(ds, versionIndex);
                                kryo.writeObject(kryo_out, res);
                                kryo_out.flush();
                            } 
                            break;
                        case END:
                            break Vnejsi;
                        case CREAT_DIR:
                            size = kryo_in.readLong();
                            List<String> path = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            if (size < getAvailableSpace()){                                
                                db.makeDirs(path);    
                                kryo_out.writeBoolean(true);
                            } else {                                
                                removeOldItems(1);                                                                    
                                if (size < getAvailableSpace()){
                                    db.makeDirs(path);    
                                    kryo_out.writeBoolean(true);
                                } else {
                                    kryo_out.writeBoolean(false);
                                }                                  
                            }
                            kryo_out.flush();
                            break;
                        case GET_ZIP:
                            fname = Arrays.asList(kryo.readObject(kryo_in, String[].class));
                            versionIndex = kryo_in.readInt();
                            item = db.getItem(fname);
                            if (item != null){
                                HashMap<DItem,String> fileList = new HashMap<>();
                                getAllFiles(item, fileList, "");                                
                                synchronized (zipBuffer) {
                                    if ((fileList.size() == 1) && (!fileList.keySet().iterator().next().isDir())){
                                        writeToZipBuffer(fileList, versionIndex);
                                    } else {
                                        writeToZipBuffer(fileList, -1);
                                    }                                              
                                    kryo.writeObject(kryo_out, zipBuffer.toByteArray());
                                    kryo_out.flush();
                                    zipBuffer.reset();
                                }
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
     * Loads a single block id, or some raw data as a byte array, from the "kryo_in" input.
     * In the case of raw data, it is then parsed into blocks and these new blocks are returned.
     * @param kryo_in 
     * @param kryo_out 
     * @param bsize Block size used when parsing raw data into new blocks.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NotEnoughSpaceOnDisc 
     */  
    private List<DBlock> loadBlock(Input kryo_in, Output kryo_out, int bsize) 
            throws IOException, ClassNotFoundException, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{   
        
        Kryo kryo = new Kryo(null);
        kryo.setAutoReset(true);
        String message = kryo_in.readString();
        while (message.equals("get_light_db")){
            LightDatabase ld = db.getLightDatabase();            
            kryo.writeObject(kryo_out, ld, LightDatabase.getSerializer());
            kryo_out.flush();
            message = kryo_in.readString();
        }
        switch(message){
            case "end":
                return null;
            case "raw_data":                    
                byte[] data = kryo.readObject(kryo_in, byte[].class);                
                return create_save_blocks(data,bsize);                    
            case "hash":
                long hash = kryo_in.readLong();
                String hash2 = kryo_in.readString();
                DBlock block = db.findBlock(hash, hash2);
                if (block == null){
                    return new LinkedList<>();
                } else {
                    return Arrays.asList(block);            
                }
            default:
                throw new ProtocolException();
        }                
    }    

    /**
     * Safely deletes "versionNum"-th version of "file" both from database and disc.
     * @param file
     * @param versionNum
     * @throws TooFewVersions
     * @throws IOException
     * @throws PatchFailedException
     * @throws BlockNotFound
     * @throws NoSuchAlgorithmException 
     */
    private void safelyDeleteVersion(DFile file, int versionNum) 
            throws TooFewVersions, IOException, BlockNotFound, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{
        synchronized (lockObject) {
            if (file == null){
                throw new IOException();
            }        
            if ((file.getVersionList() == null) || (file.getVersionList().size()<=1)){
                throw new TooFewVersions();
            }
            if((versionNum >= file.getVersionList().size())||(versionNum < 0)){
                throw new IndexOutOfBoundsException();
            }                         
            DVersion version = file.getVersionList().get(versionNum);
            if (version != null){
                if (version.isScriptForm()){
                    scripts.remove(version);
                    file.getVersionList().remove(versionNum);
                } else {
                    if (versionNum == file.getVersionList().size()-1){
                        ServerUtils.unlinkBlocksFromVersion(version);
                        scripts.remove(version);
                        file.getVersionList().remove(versionNum);
                    } else {
                        DVersion nextVersion = file.getVersionList().get(versionNum+1);
                        if (nextVersion.isScriptForm()){
                            transformScriptToBlocks(version, nextVersion);                        
                        }
                        for (int i = versionNum+2; i<file.getVersionList().size(); i++){
                            DVersion iversion = file.getVersionList().get(i);
                            if (!iversion.isScriptForm()){
                                break;
                            }                        
                            transformScriptToBlocks(version, iversion);
                            transformBlocksToScript(nextVersion, iversion, false);
                        }
                        ServerUtils.unlinkBlocksFromVersion(version);
                        scripts.remove(version);
                        file.getVersionList().remove(versionNum);                    
                    }
                }                                                            
            }    
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
        synchronized (lockObject) {
            if (file != null){
                if((versionNum >= file.getVersionList().size())||(versionNum < 0)){
                    throw new IndexOutOfBoundsException();
                }
                DVersion version = file.getVersionList().get(versionNum);
                if (version.isScriptForm()){
                    scripts.remove(version);
                    file.getVersionList().remove(versionNum);            
                } else {
                    ServerUtils.unlinkBlocksFromVersion(version);
                    file.getVersionList().remove(versionNum);
                    while (file.getVersionList().get(versionNum).isScriptForm()){
                        unsafelyDeleteVersion(file, versionNum);
                    }
                }
            }
        }
    }        
    
    /**
     * Collects and deletes all blocks that are not referenced anywhere
     */
    private void collect_blocks() throws IOException{
        synchronized (lockObject) {
            System.out.println(messages.getString("before_gc") + " " + getAvailableSpace() + messages.getString("b_available"));        
            for (DBlock block : db.collectBlocks()){
                String name = block.getName();
                ServerUtils.deleteBlock(name, home_dir);
            }
            System.out.println(messages.getString("after_gc") + " " + getAvailableSpace() + messages.getString("b_available"));
        }
    }
    
    /**
     * Deletes the file versions found by findUnnecessaryVersions()
     */
    private void removeOldItems(double fraction) throws IOException{                
        if ((fraction<0) || (fraction>1)){
            return;
        }
        Map<DVersion,DFile> versionsToDelete = findUnnecessaryVersions();        
        List<CustomTuple> versions = new ArrayList<>();
        for (Entry<DVersion,DFile> entry : versionsToDelete.entrySet()){
            versions.add(new CustomTuple(entry.getKey(), entry.getValue()));
        }
        Collections.sort(versions);
        int limit = (int) Math.floor(fraction * versions.size());
        for (int i = 0; i < limit; i++){
            try {
                CustomTuple ct = versions.get(i);
                int versionNumber = ct.getFile().getVersionList().indexOf(ct.getVersion());
                unsafelyDeleteVersion(ct.getFile(), versionNumber);
            } catch (TooFewVersions ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        collect_blocks();
    }
    
    /**
     * Finds the old file versions that are least likely to be missed, if deleted
     * @return 
     */
    private Map<DVersion,DFile> findUnnecessaryVersions(){
        List<CustomTuple> notScriptedVersions = new ArrayList<>();
        for (DFile file : db.getFileCollection()){
            List<DVersion> versionList = file.getVersionList();
            if (versionList != null){
                for (DVersion version : versionList){
                    if (!version.isScriptForm()){
                        notScriptedVersions.add(new CustomTuple(version,file));
                    }
                }
            }
        }
        Map<DVersion,DFile> res = new HashMap<>();
        for (CustomTuple t : notScriptedVersions){
            if(t.getNumberOfNewerVers() > 0){
                res.put(t.getVersion(), t.getFile());                
            }
        }
        return res;
    }
    
    
    /**
     * Loads a valid instance of Database from disc
     */
    private Database loadDB() throws IOException, ClassNotFoundException{    
        Path f = Paths.get(home_dir, "index");
        if (Files.notExists(f)){
            Files.createFile(f);
        }
        if(Files.size(f)==0){            
            return new Database(new HashMap<String,DItem>(), new TreeMap<String,DBlock>(), new TreeSet<Long>(), new TreeSet<String>());
        } else {            
            Kryo kryo = new Kryo(null);
            kryo.setAutoReset(true);
            try (Input kryo_in = new Input(Files.newInputStream(f))){
                return kryo.readObject(kryo_in, Database.class, Database.getSerializer());
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }
    
    /**
     * Saves a valid instance of Database, ie. "db", on disc
     */
    private void saveDB() {        
        Path f = Paths.get(home_dir, "index");        
        Kryo kryo = new Kryo(null);
        kryo.setAutoReset(true);
        try (Output kryo_out = new Output(Files.newOutputStream(f))){            
            kryo.writeObject(kryo_out, db, Database.getSerializer());
            kryo_out.flush();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }                        
    }
    
    /**
     * Home directory for the use by this program
     */
    private final String home_dir;
    
    /**
     * Asks the user for a valid directory, until the user enters a usable directory
     * @return
     * @throws IOException 
     */
    private String getDir() throws IOException{
        String addr = null;
        while(true){
            System.out.println(messages.getString("where_index"));
            addr = stdin.readLine();                    
            Path path = Paths.get(addr);                
            try{
                Files.createDirectories(path);
                break;
            } catch (FileAlreadyExistsException | SecurityException ex){
                System.err.println(messages.getString("dir_cant_be_used"));
            }
        }            
        return addr;
    }
    
    /**
     * Each DVersion represented as a script, is here mapped to its script
     */    
    private final Map<DVersion, EditScript> scripts;
    
    /**
     * Loads all the scripts from disc
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private Map<DVersion,EditScript> loadScripts() throws IOException, ClassNotFoundException{
        Path f = Paths.get(home_dir, "skripty");
        if (Files.notExists(f)){
            Files.createFile(f);
        }
        if(Files.size(f)==0){            
            return new HashMap<>();
        } else {   
            try (Input kryo_in = new Input(Files.newInputStream(f))){
                Kryo kryo = new Kryo(null);
                kryo.setAutoReset(true);
                int size = kryo_in.readInt();
                Map<DVersion,EditScript> res = new HashMap<>();
                for (int i = 0; i < size; i++){
                    DVersion key = kryo.readObject(kryo_in, DVersion.class, DVersion.getSerializer());
                    EditScript value = kryo.readObject(kryo_in, EditScript.class, EditScript.getSerializer());
                    res.put(key, value);
                }
                return res;
            }
        }        
    }
    
    /**
     * Saves all the scripts on disc
     */
    private void saveScripts(){        
        Path f = Paths.get(home_dir, "skripty");
        try (Output kryo_out = new Output(Files.newOutputStream(f))){
            Kryo kryo = new Kryo(null);
            kryo.setAutoReset(true);
            kryo_out.writeInt(scripts.size());
            for (Entry<DVersion,EditScript> entry : scripts.entrySet()){
                kryo.writeObject(kryo_out, entry.getKey(), DVersion.getSerializer());
                kryo.writeObject(kryo_out, entry.getValue(), EditScript.getSerializer());
            }
            kryo_out.flush();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    /**
     * Returns the contents of the "index"-th version of "fileToGet", in case <br/>
     * it is need a transformation from script form is done
     * @param versionList
     * @return
     * @throws IOException
     * @throws BlockNotFound 
     */
    private byte[] serveGet(DFile fileToGet, int index) {
        synchronized (lockObject) {
            try{
                DVersion verze = fileToGet.getVersionList().get(index);
                if(verze.isScriptForm()){
                    DVersion zaklad;
                    int i;
                    for(i = index-1; i>=0; i--){
                        if (!fileToGet.getVersionList().get(i).isScriptForm()){
                            break;
                        }
                    }
                    zaklad = fileToGet.getVersionList().get(i);
                    byte[] obsahZaklad = ServerUtils.loadVersionFromDisc(zaklad, home_dir);                    
                    EditScript editScript = scripts.get(verze);
                    return editScript.applyTo(obsahZaklad);
                    
                } else {
                    return ServerUtils.loadVersionFromDisc(verze, home_dir);
                }
            } catch (IOException | BlockNotFound ex){
                System.err.println("\"Get\" " + messages.getString("request_failed"));
                return null;
            }
        }
    } 
    
    /**
     * Returns the number of bytes on disc reserved for the application, or zero if
     * there is no reservation.
     * @param args Program arguments, as source of reservation information.
     * @return
     * @throws IOException 
     */
    private long computeReservedSpace(String[] args) throws IOException{
        String val = ServerUtils.getArgVal(args, "space", false);
        if (ServerUtils.isLong(val)){
            long desired = Long.parseLong(val);
            long available = Files.getFileStore(Paths.get(home_dir)).getUsableSpace();
            return (desired<available) ? desired : available;
        } else {                 
            return 0L;
        }
    }       
    
    /**
     * Transforms DVersion "actualVersion" into standard form - representation by blocks, <br/>
     * not by script </br>
     * @param referenceBase The base version to which the current script is referencing
     * @param actualVersion The version to be transformed
     * @throws IOException
     * @throws PatchFailedException
     * @throws BlockNotFound
     * @throws NoSuchAlgorithmException 
     */
    private void transformScriptToBlocks(DVersion referenceBase, DVersion actualVersion) 
            throws IOException, BlockNotFound, NoSuchAlgorithmException, NotEnoughSpaceOnDisc{
        synchronized (lockObject) {
            byte[] baseBytes = ServerUtils.loadVersionFromDisc(referenceBase, home_dir);
            EditScript editScript = scripts.get(actualVersion);
            byte[] newBytes = editScript.applyTo(baseBytes);
            List<DBlock> blocks = create_save_blocks(newBytes, actualVersion.getBlockSize());
            actualVersion.setScriptForm(false);
            actualVersion.setBlocks(blocks);
            ServerUtils.linkBlocksToVersion(actualVersion);
            scripts.remove(actualVersion);
        }
    }  
    
    /**
     * Transforms the "actualVersion" version into the script form against the "referenceBase" version </br>
     * @param referenceBase The base version to which the future script will be referenced
     * @param actualVersion The version to be transformed
     * @param checkPatchSize Whether the diff script size should be checked, and if too big, do not carry out the transformation.
     * @throws IOException
     * @throws BlockNotFound 
     */
    private void transformBlocksToScript(DVersion referenceBase, DVersion actualVersion, boolean checkPatchSize) 
            throws IOException, BlockNotFound{
        synchronized (lockObject) {            
            byte[] baseBytes = ServerUtils.loadVersionFromDisc(referenceBase, home_dir);
            byte[] newBytes = ServerUtils.loadVersionFromDisc(actualVersion, home_dir);
            int limit = actualVersion.getBlockSize();
            EditScript editScript = EditScript.createScript(baseBytes, newBytes, (checkPatchSize ? limit : 0), true);
            if (editScript != null){                
                actualVersion.setScriptForm(true);
                actualVersion.setBlocks(null);
                ServerUtils.unlinkBlocksFromVersion(actualVersion);
                scripts.put(actualVersion, editScript);                            
            }
        }
    }       
    
    /**
     * Parses "data" into blocks of size "blockSize". Their contents are saved <br/>
     * on disc, DBlocks are returned in a baseContents
     * @param data
     * @param blockSize
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    private List<DBlock> create_save_blocks(byte[] data, int blockSize) 
            throws NoSuchAlgorithmException, IOException, NotEnoughSpaceOnDisc{
        List<DBlock> res = new ArrayList<>();
        int left = 0;        
        while(left<data.length){
            int right = left + blockSize;  
            byte[] pars_data = Arrays.copyOfRange(data, left, right);
            int used;
            if ((data.length - left)<blockSize){
                used = data.length - left;                       
            } else {
                used = blockSize;
            }
            long hash = RollingHash.computeHash(pars_data);
            String hash2 = RollingHash.computeHash2(pars_data);
            if (pars_data.length >= getAvailableSpace()){                            
                removeOldItems(1);                     
                if(pars_data.length >= getAvailableSpace()){
                    throw new NotEnoughSpaceOnDisc();
                }
            }            
            DBlock newBlock = new DBlock(hash, hash2, blockSize, used);
            if(!db.blockExists(hash, hash2)){                
                int col = ServerUtils.saveBlock(pars_data, hash, home_dir);            
                newBlock.setCol(col);
                db.addBlock(newBlock);
            } else {
                newBlock = db.findBlock(hash, hash2);
            }            
            res.add(newBlock);
            left += blockSize;
        }        
        return res;
    }            
    
    /**
     * Determines which files should be included in the zip archive that is to be created.
     * The result is given in the "files" parameter.
     * @param location
     * @param files 
     * @param pathPrefix 
     */
    private void getAllFiles(DItem location, Map<DItem,String> files, String pathPrefix){
        if (pathPrefix.equals("")){
            files.put(location, pathPrefix);
            if (location.isDir()){
                getAllFiles(location, files, location.getName());
            } 
        } else {
            for (DItem file : ((DDirectory)location).getItemMap().values()){
                files.put(file, pathPrefix);
                if (file.isDir()){
                        getAllFiles(file, files, pathPrefix + File.separator + file.getName());
                }
            }       
        }             
    }

    /**
     * Temporary space to save zipped files && folders before they are sent to a client's machine.
     */
    private final ByteArrayOutputStream zipBuffer;    
    
    /**
     * Builds a zip archive in the zipBuffer variable.
     * @param fileList Contains all the files that should be included in the archive.
     * @param versionNumber The version number of the file to be zipped.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void writeToZipBuffer(Map<DItem,String> fileList, int versionNumber) 
            throws FileNotFoundException, IOException {            
            try (ZipOutputStream zos = new ZipOutputStream(zipBuffer)) {
                    for (Entry<DItem,String> entry : fileList.entrySet()) {
                        DItem file = entry.getKey();
                        if (!file.isDir()) {
                                addToZip(file, versionNumber, zos, entry.getValue());
                        } else if (((DDirectory)file).getItemMap().isEmpty()){
                                addToZip(file, versionNumber, zos, entry.getValue());                            
                        }
                    }
            }            
    }
    
    /**
     * Adds a file or an empty directory to a zip archive via "zos" output stream.
     * @param item The location of the root element being zipped.
     * @param versionNumber 
     * @param zos Output stream to the zip archive.
     * @param path Path prefix.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void addToZip(DItem item, int versionNumber, ZipOutputStream zos, String path) 
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

/**
 * Used to associate versions with the corresponding files and compute a metric on the pair,
 * used to determine, how "important" the version is. 
 * @author filipekt
 */
class CustomTuple implements Comparable<CustomTuple> {
    private DVersion version;
    public DVersion getVersion() {
        return version;
    }

    private DFile file;
    public DFile getFile() {
        return file;
    }
    
    public CustomTuple(DVersion v, DFile f){
        this.version = v;
        this.file = f;
    }

    @Override
    public int compareTo(CustomTuple o) {
        Integer val = metricValue();
        Integer o_val = o.metricValue();
        return val.compareTo(o_val);        
    }
    
    int getNumberOfNewerVers(){
        int index = file.getVersionList().indexOf(version);
        int count = 0;
        for (int i = index+1; i<file.getVersionList().size(); i++){
            if (!file.getVersionList().get(i).isScriptForm()){
                count++;
            }
        }
        return count;
    }
    private int getApprSize(){
        return (version.getBlockSize() * version.getBlocks().size());
    }
    
    /**
     * Computes the value of this object in the metric used to assess </br>
     * the importance of old file versions </br>
     * @return 
     */
    int metricValue(){
        return getNumberOfNewerVers() * getApprSize();
    }
}