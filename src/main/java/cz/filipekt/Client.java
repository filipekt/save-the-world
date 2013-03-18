package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Representation of the client that connects to server
 * @author Lifpa
 */
public class Client {
    public static void main(String[] args){        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        start(args, br);
    }
    
    /**
     * Contains all the localized messages that are shown to the user.
     */
    private ResourceBundle messages;    
    
    /**
     * The valid locale
     */
    private Locale locale;    
    
    private static void start(String[] args, BufferedReader br){
        try {                        
            String[] args2;
            String comp;
            int port_num;
            Locale locale = new Locale("en","US");
            String localeArgs = ServerUtils.getArgVal(args, "locale", false);
            if (localeArgs!=null){
                if (localeArgs.equalsIgnoreCase("cz")){
                    locale = new Locale("cs","CZ");    
                }
            }
            ResourceBundle messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale); 
            
            PrintStream stdout = System.out;
            if (ServerUtils.isSwitchPresent(args, "output")){
                String outPath = ServerUtils.getArgVal(args, "output", true);
                Path outPath2 = Paths.get(outPath);
                stdout = new PrintStream(Files.newOutputStream(outPath2));
            }            
            
            if((args2=ServerUtils.getAddr(args))!=null){
                if ((args2.length < 2) || args2[0]==null || args2[1]==null){
                    stdout.println(messages.getString("parameter_error"));                    
                    return;                    
                }
                comp = args2[0];
                port_num = Integer.parseInt(args2[1]);
            } else {
                stdout.println(messages.getString("which_computer"));        
                comp = br.readLine();            

                port_num = 0;
                boolean jeKorektni = false;
                while(!jeKorektni){
                    stdout.println(messages.getString("which_port_connect"));
                    String port = br.readLine();                
                    try{
                        port_num = Integer.valueOf(port);
                        if (port_num<0 || port_num>50000){
                            throw new NumberFormatException();
                        }
                        jeKorektni = true;
                    } catch (NumberFormatException ex){
                        stdout.println(messages.getString("invalid_port"));
                    }
                }
            }   

            boolean interactive = ServerUtils.isSwitchPresent(args, "interactive");
            
            try{
                Client cl = new Client(comp, port_num, br, locale, stdout, interactive);
                try{
                    cl.work();                
                } catch (Exception ex){
                    stdout.println(ex.toString());
                } finally{
                    cl.end();
                }
            } catch (IOException ex){
                stdout.println(messages.getString("connection_not_established"));
            }
        } catch (IOException ex){
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);            
        }        
    }
    
    /**
     * Input stream from server
     */
    private InputStream in_s;        
    
    /**
     * Output stream to server
     */
    private OutputStream out_s;    
    
    /**
     * Standard input for client
     */
    private BufferedReader stdin;
    
    /**
     * Standard output for client
     */
    private PrintStream stdout;
    
    /**
     * The upper limit on how many times the client tries to <br/>
     * take care of a user's request
     */
    private static final int attempt_limit = 3;
    
    /**
     * Socket used to communicate with server
     */
    private Socket socket;
    
    /**
     * Base class of the Kryo framework for fast (de-)serialization of objects
     */
    private Kryo kryo;
    
    /**
     * Specific output stream for the use by Kryo framework
     */
    private Output kryo_output;
    
    /**
     * Specific input stream for the use by Kryo framework
     */
    private Input kryo_input;  
    
    /**
     * Marks whether the program is expecting a batch input or not, so that 
     * questions and other pieces of infomation are printed to stdout only if needed.
     */
    private final boolean interactive;
    
    public Client(String comp, int port, BufferedReader stdin, Locale locale, PrintStream stdout, boolean interactive) throws IOException{
        InetAddress addr = InetAddress.getByName(comp);
        socket = new Socket(addr,port);        
        in_s = socket.getInputStream();
        out_s = socket.getOutputStream();
        kryo = new Kryo();        
        kryo_output = new Output(out_s);
        kryo_input = new Input(in_s);
        this.stdin = (stdin == null) ? new BufferedReader(new InputStreamReader(System.in)) : stdin;
        this.locale = locale;
        this.messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale);
        this.stdout = (stdout == null) ? System.out : stdout;     
        this.interactive = interactive;
    }
       
    /**
     * Attempts to serve the client's requests
     */
    private void work(){
        boolean pokr = true;
        while (pokr){            
            pokr = serveOperation();
        }
    }    
    
    /**
     * Stops the client and shuts all connections down
     * @throws IOException 
     */
    void end() throws IOException{
        kryo_output.close();
        kryo_input.close();
        socket.close();
    }
    
    /**
     * Pre-processes the user request before handing it to the switchToOperation() method.
     * @return Success/failure of operation
     */
    private boolean serveOperation(){
        try {
            List<String> request;            
            if (interactive){
                stdout.println(messages.getString("enter_op"));
            }
            String request1 = stdin.readLine();
            String[] request2 = request1.split(" ");                
            request = new ArrayList<>();
            for (String s : request2){
                if (!s.equals("")){
                    request.add(s);
                }
            }
            if (interactive){
                if(request.isEmpty()){
                    stdout.println(messages.getString("noop_entered"));
                    throw new IOException();
                }
            }
            request = ServerUtils.concatQuotes(request);
            if (switchToOperation(request) == false){
                return false;
            }
        } catch (IOException ex) {
//            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            stdout.println(ex.getLocalizedMessage());
        }
        return true;
    }
    
    /**
     * According to the type of the request, this method delegates further actions
     * to specialized methods or serves the request in place.
     * @param request
     * @return 
     */
    boolean switchToOperation(List<String> request){
        if ((request == null) || (request.isEmpty())){
            terminateConnection();
            return false;
        }
        switch(request.get(0)){
            case "add":
                if(request.size()<2){
                    if (interactive){
                        stdout.println(messages.getString("usage_add"));
                    }
                    break;
                }
                if(request.size()==2){
                    String name = ServerUtils.parseName(request.get(1)).getLast();
                    request.add(name);
                }
                try{                            
                    serveAdd(request, null);
                } catch (IOException | NoSuchAlgorithmException ex){
                    stdout.println(messages.getString("attempt_failed"));
                }
                break;
            case "get":
            case "get_zip":
                if(request.size()<3){
                    if (interactive){
                        if (request.get(0).equals("get_zip")){
                            stdout.println(messages.getString("usage_get_zip"));
                        } else {
                            stdout.println(messages.getString("usage_get"));
                        }
                    }
                    break;
                }                                        
                try{
                    if(request.size()>=4){
                        Integer.parseInt(request.get(3));
                    }
                    serveGet(request);
                    break;
                } catch (IOException | ClassNotFoundException | WrongVersionNumber ex){
                    stdout.println(messages.getString("attempt_failed"));
                }
                break;            
            case "delete":
                if(request.size() != 3){
                    if (interactive){                        
                        stdout.println(messages.getString("usage_delete"));                        
                    }                    
                } else {
                    boolean result = serveDelete(request);
                    if (interactive){
                        if (result){
                            stdout.println(messages.getString("delete_ok"));
                        } else {
                            stdout.println(messages.getString("delete_fail"));
                        }
                    }
                }                 
                break;
            case "exit":
                terminateConnection();
                return false;
            case "list":                  
                boolean verbose = false;
                if ((request.size() > 1) && request.get(1).equalsIgnoreCase("verbose")){
                    verbose = true;
                }
                Database db = getDB();
                if (db!=null){
                    db.printContents(null, 0, verbose, stdout, messages);
                }                    
                break;
            default:
                if (interactive){                        
                    stdout.println(messages.getString("unknown_request"));                        
                }  
                break;
        }
        return true;
    }
    
    void terminateConnection(){
        try {
            kryo.writeObject(kryo_output, Requests.END);
        } catch (Exception ex){}            
    }

    
    private boolean askUser(String question) throws IOException{        
        boolean answered = false;
        while (!answered){
            stdout.println(question);
            stdout.println(messages.getString("answer_please") + " " + messages.getString("yes") + " " + messages.getString("or") + " " + messages.getString("no") + ":");
            String answer = stdin.readLine();
            if (answer!=null){
                if (answer.equalsIgnoreCase(messages.getString("yes")) || answer.equalsIgnoreCase(messages.getString("y"))){
                    return true;
                } else if (answer.equalsIgnoreCase(messages.getString("no")) || answer.equalsIgnoreCase(messages.getString("n"))){
                    return false;
                }
            }
        }
        return false;
    }
    
    private void serveGet(List<String> request) throws IOException, ClassNotFoundException, WrongVersionNumber{
        Database db = getDB();
        if (request.size() < 3){
            return;
        }
        final boolean zip = (request.get(0) != null) && (request.get(0).equals("get_zip"));
        final String source = request.get(1);
        final List<String> source2 = ServerUtils.parseName(source);
        final String destination = request.get(2);
        final Path destination2 = Paths.get(destination);
        Integer versionNumber = null;
        if (request.size() >= 4){
            try {
                versionNumber = Integer.valueOf(request.get(3));
            } catch (NumberFormatException ex){}
        }
        if (!zip && !isCompatible(source, destination2, db)){            
            stdout.println(messages.getString("dir_into_file"));
            return;
        }
        if (Files.exists(destination2) && Files.isRegularFile(destination2)){            
            if (!askUser(messages.getString("replace_1"))){
                return;
            }
        }
        DItem item = db.getItem(source2);
        boolean res;
        if (item != null){
            if (item.isDir()){
                res = receiveDirectory(source2, destination2, zip, true, db, null);
            } else {
                res = receiveVersion(destination2, source2, versionNumber, zip, db, null);
            }
            if (res) {
                stdout.println(messages.getString("download_finished"));
            }
        }
    }
        
    int[] serveGetBin(List<String> sourceFile, Integer versionNumber, final boolean zip, Database db) throws IOException, ClassNotFoundException, WrongVersionNumber{       
        if (db == null){
            db = getDB();
        }
        if(!db.itemExists(sourceFile)){
            stdout.println(messages.getString("sorry_the_file") + " " + messages.getString("doesnt_exist"));
            return null;
        }      
        DItem item = db.getItem(sourceFile);
        if (zip){
            kryo.writeObject(kryo_output, Requests.GET_ZIP);
        } else {
            kryo.writeObject(kryo_output, Requests.GET_FILE);
        }
        kryo.writeObject(kryo_output, sourceFile.toArray(new String[0]));        
        int verze;
        if(versionNumber != null){
            verze = versionNumber;
        } else if (!item.isDir()) {
            verze = db.findFile(sourceFile).getVersionList().size() - 1;
        } else {
            verze = 0;
        }
        if((!item.isDir()) && (verze >= db.findFile(sourceFile).getVersionList().size())){
            throw new WrongVersionNumber();
        }
        kryo.writeObject(kryo_output, Integer.valueOf(verze));
        kryo_output.flush();

        byte[] data;
        if (!zip) {
            data = kryo.readObject(kryo_input, byte[].class);
            int c; 
            int[] res = new int[data.length];
            int i = 0;
            for(byte b : data){
                c = ((int)b) + 128;
                res[i++] = c;
            }        
            return res;
        } else {            
            byte[] src = kryo.readObject(kryo_input, byte[].class);
            int[] res;
            try (ByteArrayInputStream is = new ByteArrayInputStream(src)){
                int c;
                List<Integer> list = new ArrayList<>();
                while ((c=is.read())!=-1){
                    list.add(c);
                }                
                res = new int[list.size()];
                int i = 0;
                for (int val : list){
                    res[i++] = val;
                }                
            }
            return res;
        }
        
    }

    /**
     * Returns false if and only if the user is trying to download a directory into a regular file.
     * @param source
     * @param destination
     * @return 
     */
    private boolean isCompatible(String source, Path destination, Database db){
        List<String> sorce2 = ServerUtils.parseName(source);
        if (db.itemExists(sorce2)){
            DItem item = db.getItem(sorce2);
            return !item.isDir() || Files.isDirectory(destination);
        } else {
            return false;
        }
    } 
    
    /**
     * Given the file "sourceFile" on server and the path "dest" at the client, </br>
     * this method downloads the contents of "sourceFile" to "dest", creating the </br>
     * target file if necessary
     * @param dest
     * @param sourceFile
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveFile(Path dest, List<String> sourceFile, final boolean zip, Database db, JFrame frame) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{  
        return receiveVersion(dest, sourceFile, null, zip, db, frame);
    }    
    
    /**
     * Given the file "sourceFile" on server, it's version "version" and the path "destination" at the client, </br>
     * this method downloads the contents of the version to "destination"
     * @param destination
     * @param sourceFile
     * @param version
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveVersion(Path destination, List<String> sourceFile, final Integer versionNumber, 
            final boolean zip, Database db, JFrame frame) throws IOException, ClassNotFoundException, WrongVersionNumber{
        if (sourceFile == null){
            return false;
        } 
        Path dest2;
        if (Files.isDirectory(destination)){
            String fname = sourceFile.get(sourceFile.size()-1);
            dest2 = Paths.get(destination.toAbsolutePath().toString(), fname + (zip ? ".zip" : ""));
        } else {
            dest2 = destination;
        }     
        if (Files.exists(dest2)){
            if ((frame==null) && interactive && (!askUser(dest2.toString() + ": " + messages.getString("replace_1")))){
                return false;
            } else if (frame != null){
                int answer = JOptionPane.showConfirmDialog(frame, messages.getString("replace_1"), messages.getString("replace_2"), JOptionPane.YES_NO_CANCEL_OPTION);
                if (answer != JOptionPane.YES_OPTION){
                    return false;
                } 
            }
        }
        int[] data = serveGetBin(sourceFile, versionNumber, zip, db);
        if (data == null){            
            throw new FileNotFoundException();
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(dest2))){
            for(int i : data){
                bos.write(i);
            }
        }           
        return true;
    }    
    
    /**
     * Given the directory "src" on server and the path "dest" at the client, </br>
     * this method downloads the contents of "src" to "dest"
     * @param src
     * @param dest
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveDirectory(List<String> src, Path dest, final boolean zip, final boolean toplevel, Database db, JFrame frame) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{
        boolean res = false;
        if (zip){
            res = receiveFile(dest, src, true, db, frame);
        } else {
            if (toplevel){
                Path dest2 = Paths.get(dest.toAbsolutePath().toString(), src.get(src.size()-1));
                Files.createDirectories(dest2);
                dest = dest2;
            }
            if (db.getItem(src).isDir()){
                DDirectory dir = (DDirectory) db.getItem(src);
                for (Map.Entry<String,DItem> entry : dir.getItemMap().entrySet()){
                    Path dest2 = Paths.get(dest.toAbsolutePath().toString(), entry.getKey());
                    List<String> src2 = new ArrayList<>();
                    src2.addAll(src);
                    src2.add(entry.getKey());                    
                    if(entry.getValue().isDir()){
                        // create the dir if needed                    
                        if (Files.notExists(dest2)){
                            Files.createDirectories(dest2);
                        }
                        // continue recursively in depth                    
                        receiveDirectory(src2, dest2, false, false, db, frame);
                    } else {
                        // handle entry as a regular file
                        receiveFile(dest2, src2, false, db, frame);
                    }
                }
                res = true;
            }            
        }
        return res;
    }    
    
    /**
     * Downloads a valid instance of Database from the server
     * @return 
     */
    Database getDB() {
        for(int i = 0; i<attempt_limit; i++){
            try {                            
                kryo.writeObject(kryo_output, Requests.GET_DB);
                kryo_output.flush();
//                Database db = (Database) ois.readObject();
                Database db = kryo.readObject(kryo_input, Database.class, Database.getSerializer());
                return db;
            } catch (KryoException ex) {
//                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                stdout.println(ex.getLocalizedMessage());
            }            
        }
        return null;
    }
    
    private LightDatabase getLightDatabase(boolean enumForm) {
        for(int i = 0; i<attempt_limit; i++){
            try {            
                if (enumForm){
                    kryo.writeObject(kryo_output, Requests.GET_LIGHT_DB);
                } else {
                    kryo.writeObject(kryo_output, "get_light_db");
                }
                kryo_output.flush();
                LightDatabase ld = kryo.readObject(kryo_input, LightDatabase.class, LightDatabase.getSerializer());
                return ld;
            } catch (KryoException ex) {
//                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                stdout.println(ex.getLocalizedMessage());
            }            
        }
        return null;
    }    
    
    private boolean itemExistsOnServer(Collection<String> target){
        if (target == null){
            throw new IllegalArgumentException();
        }        
        String[] target2 = target.toArray(new String[0]);
        kryo.writeObject(kryo_output, Requests.ITEM_EXISTS);
        kryo.writeObject(kryo_output, target2);
        kryo_output.flush();
        return kryo.readObject(kryo_input, boolean.class);
    }
    
    /**
     * Serves the user request of type "add" , which adds a new file/directory into the system
     * @param request
     * @throws PrenosSeNepovedl 
     */
    void serveAdd(List<String> request, JFrame frame) throws IOException, NoSuchAlgorithmException {                        
        String fname = request.get(1);
        Collection<String> target = ServerUtils.parseName(request.get(2));
        Path file = Paths.get(fname);
        if(Files.notExists(file)){
            if (frame == null){
                stdout.println(messages.getString("sorry_the_file") + " " + fname + " " + messages.getString("doesnt_exist"));
                throw new IOException();
            } else {
                JOptionPane.showMessageDialog(frame, messages.getString("sorry_the_file") + " " + fname + " " + messages.getString("doesnt_exist"),
                        messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                throw new IOException();
            }
        }
        if (Files.isSymbolicLink(file)){
            if (frame == null){
                stdout.println(messages.getString("sorry_the_file") + " " + fname + " " + messages.getString("is_symlink"));
                throw new IOException();
            } else {
                JOptionPane.showMessageDialog(frame, messages.getString("sorry_the_file") + " " + fname + " " + messages.getString("is_symlink"),
                        messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                throw new IOException();
            }
        }
        boolean alreadyPresent = itemExistsOnServer(target);
        if (!alreadyPresent){                                    
            if(Files.isDirectory(file)){
                kryo.writeObject(kryo_output, Requests.CREAT_DIR);
                kryo.writeObject(kryo_output, ServerUtils.getDirectorySize(file));
            } else {
                kryo.writeObject(kryo_output, Requests.CREAT_FILE);                
                kryo.writeObject(kryo_output, Files.size(file));
            }
            
            kryo.writeObject(kryo_output, target.toArray(new String[0]));
            kryo_output.flush();
            boolean enough_space = kryo.readObject(kryo_input, Boolean.TYPE);
            if(!enough_space){
                if (frame == null){
                    stdout.println(messages.getString("not_enough_space"));
                    throw new IOException();   
                } else {
                    JOptionPane.showMessageDialog(frame, messages.getString("not_enough_space"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                    throw new IOException();
                }                
            }
        }                                 
        if(Files.isDirectory(file)){
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(file)){
                for(Path p : ds){
                    List<String> newRequest = new ArrayList<>();
                    newRequest.add(request.get(0));
                    newRequest.add(p.toAbsolutePath().toString());
                    newRequest.add(request.get(2) + "/" + p.getFileName().toString());
                    serveAdd(newRequest, frame);
                }
            }
            return;
        }                                
        int vel_bloku = ServerUtils.getBlockSize(file);                     

        kryo.writeObject(kryo_output, Requests.CREAT_VERS);
        kryo.writeObject(kryo_output, Files.size(file));
        kryo_output.flush();
        boolean enough_space = kryo.readObject(kryo_input, Boolean.TYPE);
        if(!enough_space){
            if (frame == null){
                stdout.println(messages.getString("not_enough_space"));
                throw new IOException();    
            } else {
                JOptionPane.showMessageDialog(frame, messages.getString("not_enough_space"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                throw new IOException();
            }                
        }
        kryo.writeObject(kryo_output, target.toArray(new String[0]));
        kryo.writeObject(kryo_output, (Integer)vel_bloku);
        kryo_output.flush();  
        
        windowLoop(file, vel_bloku);
    }
    
    /**
     * Processes an input file with the "window" method. At every position of the window
     * it checks the database for the current block. Depending on whether the block is new 
     * or already present, furher actions differ.
     * @param file
     * @param db
     * @param block_size
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */
    private void windowLoop(Path file, int block_size) throws IOException, NoSuchAlgorithmException{
        LightDatabase ld = getLightDatabase(false);
        try (BufferedInputStream fin = new BufferedInputStream(Files.newInputStream(file))) {
            RollingHash rh = new RollingHash(block_size);
            for(int i = 0; i<block_size-1; i++){        
                int c = fin.read();                    
                if (c==-1) {
                    break;
                } else {
                    byte b = (byte) (c-128);
                    rh.add(b);
                }                    
            }
            List<Byte> not_matched = new ArrayList<>();
            int c;
            boolean add_from_rh = true;
            boolean beginning = true;
            while ((c=fin.read())!=-1){
                byte b = (byte) (c-128);
                byte old = rh.add(b);
                if(!beginning) {
                    not_matched.add(old);
                }
                beginning = false;
                
//                Hash collisions must be taken care of.
                long hash = rh.getHash();
                String hexHash2 = null;
                boolean is_in_db = ld.blockExists1(hash);
                if(is_in_db){                    
                    hexHash2 = rh.getHexHash2();
                    if (!ld.blockExists2(hexHash2)){
                        is_in_db = false;
                    }                    
                }

                if(is_in_db){
//                    Send the previous bytes.
                    if(!not_matched.isEmpty()){                        
                        kryo.writeObject(kryo_output, "raw_data");                        
                        byte[] res = new byte[not_matched.size()];
                        int i = 0;
                        for (Byte bb : not_matched){
                            res[i++] = bb;
                        }                                                
                        kryo.writeObject(kryo_output, res);
                        not_matched.clear();
                        ld = getLightDatabase(false);
                    }
                  
//                    Send the hash values of the block.
                    kryo.writeObject(kryo_output, "hash");
                    kryo.writeObject(kryo_output, hash);
                    kryo.writeObject(kryo_output, hexHash2);
                    kryo_output.flush();

//                    And move on.
                    rh.resetValid();
                    add_from_rh = false;
                    for (int i = 0; i < (rh.getCounterLength() - 1); i++){
                        int lc = fin.read();
                        if (lc==-1){                                
                            break;
                        }
                        byte lb = (byte) (lc-128);
                        rh.add(lb);
                        add_from_rh = true;        
                    }
                    beginning = true;

                } else {                    
                    add_from_rh = true;
                } 

            }
            
            // co ted se zbytky dat v nesparovane
            // pripad ze soubor je mensi nez velikost bloku
            if (add_from_rh){
                for (byte b : rh.getValidData()){
                    not_matched.add(b);
                }
            }

//          All the bytes that are left must be now sent.
            boolean finished = false;
            if(not_matched.size() < block_size){
                long hash = RollingHash.computeHash(not_matched);
                String hexHash2 = RollingHash.computeHash2(not_matched);                               
                if ((ld.blockExists1(hash)) && (ld.blockExists2(hexHash2))){
                    kryo.writeObject(kryo_output, "hash");
                    kryo.writeObject(kryo_output, hash);
                    kryo.writeObject(kryo_output, hexHash2);
                    finished = true;
                } 
                
            }
            if (!finished){
                kryo.writeObject(kryo_output, "raw_data");
                byte[] res = new byte[not_matched.size()];
                int i = 0;
                for (Byte b : not_matched){
                    res[i++] = b;
                }
                kryo.writeObject(kryo_output, res);
            }
            kryo.writeObject(kryo_output, "end");
            kryo_output.flush();
        }
    }
    
    /**
     * Serves the user request of type "delete_ver", which deletes a version of a file,
     * unless the version is the only one present.
     * @param request
     * @return 
     */
    private boolean serveDelete(List<String> request){
        if ((request == null) || (request.size() != 3)){
            return false;
        }
        String fileName = request.get(1);
        String verNum = request.get(2);
        int verNum2;
        try {
            verNum2 = Integer.parseInt(verNum);
        } catch (NumberFormatException ex){
            return false;
        }
        List<String> fileName2 = ServerUtils.parseName(fileName);
        kryo.writeObject(kryo_output, Requests.DEL_VERS);
        kryo.writeObject(kryo_output, fileName2.toArray(new String[0]));
        kryo.writeObject(kryo_output, verNum2);
        kryo_output.flush();
        return kryo.readObject(kryo_input, boolean.class);
    }
}

class WrongVersionNumber extends Exception {}