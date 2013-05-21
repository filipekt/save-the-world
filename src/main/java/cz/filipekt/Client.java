package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Representation of the client that connects to server.
 * @author Tomas Filipek
 */
public class Client {
    public static void main(String[] args){        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
     * Contains all the localized messages that are shown to the user.
     */
    private final ResourceBundle messages;             
    
    /**
     * Standard input for client
     */
    private final BufferedReader stdin;
    
    /**
     * Standard output for client
     */
    private final PrintStream stdout;      
    
    /**
     * Socket used to communicate with server
     */
    private final Socket socket;
    
    /**
     * Base class of the Kryo framework for fast (de-)serialization of objects
     */
    private final Kryo kryo;
    
    /**
     * Specific output stream for the use by Kryo framework
     */
    private final Output kryo_output;
    
    /**
     * Specific input stream for the use by Kryo framework
     */
    private final Input kryo_input;  
    
    /**
     * Marks whether the program is expecting a batch input or not, so that 
     * questions and other pieces of infomation are printed to stdout only if needed.
     */
    private final boolean interactive;
    
    Client(String comp, int port, BufferedReader stdin, Locale locale, PrintStream stdout, boolean interactive) throws IOException{
        InetAddress addr = InetAddress.getByName(comp);
        socket = new Socket(addr,port);        
        InputStream in_s = socket.getInputStream();
        OutputStream out_s = socket.getOutputStream();
        kryo = new Kryo(null); 
        kryo.setAutoReset(true);
        kryo_output = new Output(out_s);        
        kryo_input = new Input(in_s);
        this.stdin = (stdin == null) ? new BufferedReader(new InputStreamReader(System.in)) : stdin;        
        this.messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale);
        this.stdout = (stdout == null) ? System.out : stdout;     
        this.interactive = interactive;
    }
       
    /**
     * Attempts to serve the client's requests, until an "exit" request is encountered.
     */
    private void work(){
        boolean pokr = true;
        while (pokr){            
            pokr = serveOperation();
        }
    }    
    
    /**
     * Shuts down all the connections to server.
     * @throws IOException 
     */
    void end() throws IOException{
        kryo_output.close();
        kryo_input.close();
        socket.close();
    }
    
    /**
     * Reads and pre-processes the user request before handing it to the switchToOperation() method.
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
                }
            }
            request = ServerUtils.concatQuotes(request);
            if (switchToOperation(request) == false){
                return false;
            }
        } catch (IOException ex) {
            stdout.println(ex.getLocalizedMessage());
        }
        return true;
    }
    
    /**
     * According to the type of the request, this method delegates further actions
     * to specialized methods or serves the request in place.
     * @param request A user request parsed into a list.
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
                    serveAdd(request, null, true);
                } catch (IOException ex){
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
                } catch (WrongVersionNumber ex){
                    stdout.println(messages.getString("wrong_version"));
                } catch (IOException | ClassNotFoundException ex){
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
                printContents(null, 0, verbose, stdout, messages, getFS());                                                    
                break;
            case "gc":
                kryo.writeObject(kryo_output, Requests.GC);
                kryo_output.flush();
                break;
            default:
                if (interactive){                        
                    stdout.println(messages.getString("unknown_request"));                        
                }  
                break;
        }
        return true;
    }
    
    /**
     * Prints the contents of the server database in a human readable form.
     * @param item The directory whose contents shall be printed.
     * @param level Distance of the "item" from the root directory.
     * @param verbose If set, some additional information is printed.
     * @param out Where to print the output.
     * @param messages Source of localized text messages.
     * @param fileMap Server file database root directory.
     */
    private void printContents(DItem item, final int level, final boolean verbose, 
            PrintStream out, ResourceBundle messages, Map<String,DItem> fileMap){
        if (fileMap == null){
            return;
        }        
        if(level==0){
            out.println(messages.getString("db_contents") + ":");
        }
        if(item==null){
            for(DItem it : fileMap.values()){
                printContents(it, level+1, verbose, out, messages, fileMap);
            }
        } else if (item.isDir()){
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<level;i++){
                sb.append(' ');
            }
            String prefix = sb.toString();
            DDirectory dir = (DDirectory) item;
            out.println(prefix + dir.getName());            
            for(DItem it : ((DDirectory)item).getItemMap().values()){
                printContents(it, level+1, verbose, out, messages, fileMap);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<level;i++){
                sb.append(' ');
            }
            String prefix = sb.toString();
            DFile file = (DFile) item;
            out.println(prefix + file.getName());
            int i = 0;
            for(DVersion verze : file.getVersionList()){
                out.println(prefix + " |" + messages.getString("version") + " " + i++ + "|" + verze.getAddedDate().toString());
                if (verbose){
                    if(!verze.isScriptForm()){
                        for (DBlock db : verze.getBlocks()){
                            out.println(prefix + " |" + "---> " + db.getHexHash());
                        }
                    } else {
                        out.println(prefix + " |" + "---> " + messages.getString("edit_script"));
                    }
                }
            }
        }
        if(verbose && (level==0)){
            Map<String,DBlock> blockMap = getServerBlocks();
            out.println("-----------------");
            for (DBlock block : blockMap.values()){
                out.println(block.getHexHash() + " : refcount=" + block.getRefCount());
            }
        }            
    }       
    
    /**
     * Sends a special request to the server stating that the client is ready to </br>
     * terminate the connection. Its effect is equal to that of user request "exit".
     */
    void terminateConnection(){
        try {
            kryo.writeObject(kryo_output, Requests.END);            
        } catch (Exception ignored){}            
    }
    
    /**
     * Prints the specified "question" to standard output and returns the </br>
     * user's answer - either consent or disapproval.
     * @param question The question with only possible answers yes/no.
     * @return
     * @throws IOException 
     */
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
    
    /**
     * Takes care of a get request. Checks whether the requested file is present </br>
     * on server, whether the local target is directory when downloading a directory.. </br>    
     * The actual transmission is handed to other methods - receiveVersion(..), ...
     * @param request A user request of type "get", parsed into a list.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    private void serveGet(List<String> request) throws IOException, ClassNotFoundException, WrongVersionNumber{       
        if (request.size() < 3){
            return;
        }
        boolean zip = (request.get(0) != null) && (request.get(0).equals("get_zip"));
        String source = request.get(1);
        List<String> source2 = ServerUtils.parseName(source);
        String destination = request.get(2);
        Path destination2 = Paths.get(destination);
        Integer versionNumber = null;
        if (request.size() >= 4){
            try {
                versionNumber = Integer.valueOf(request.get(3));
            } catch (NumberFormatException ex){}
        }
        DItem item = getDItemFromServer(source2);
        if (!zip && !isCompatible(destination2, item)){            
            stdout.println(messages.getString("dir_into_file"));
            return;
        }        
        boolean res;
        if (item != null){
            if (item.isDir()){
                res = receiveDirectory(source2, destination2, zip, true, item, null);
            } else {
                res = receiveVersion(destination2, source2, versionNumber, zip, null);
            }
            if (res) {
                stdout.println(messages.getString("download_finished"));
            }
        }
    }
    
    /**
     * Downloads from server a DItem object representing the specified path.
     * @param path Path in the server database.
     * @return 
     */
    DItem getDItemFromServer(List<String> path){
        String[] path2 = path.toArray(new String[0]);
        kryo.writeObject(kryo_output, Requests.GET_D_ITEM);
        kryo.writeObject(kryo_output, path2);
        kryo_output.flush();
        byte res = kryo_input.readByte();
        switch (res){
            case (byte)1:
                DDirectory dir = kryo.readObject(kryo_input, DDirectory.class, DDirectory.getSerializer());
                return dir;
            case (byte)2:
                DFile file = kryo.readObject(kryo_input, DFile.class, DFile.getSerializer());
                return file;
            default:
                return null;
        }
    }
       
    /**
     * Operates the data transmission during the "get" request.
     * @param sourceFile Path in the server database to the requested file.
     * @param versionNumber If not null, marks the version number of the requested version. If null, the latest version is used.
     * @param zip If set, a zipped archive is requested instead of raw data.
     * @return The contents of the recieved file.
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    private int[] serveGetBin(List<String> sourceFile, Integer versionNumber, final boolean zip) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{       
        DItem item = getDItemFromServer(sourceFile);
        if (item == null){
            stdout.println(messages.getString("sorry_the_file") + " " + messages.getString("doesnt_exist"));
            return null;
        }      
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
            verze = ((DFile)item).getVersionCount() - 1;
        } else {
            verze = 0;
        }
        if((!item.isDir()) && (verze >= ((DFile)item).getVersionCount())){
            throw new WrongVersionNumber();
        }
        kryo_output.writeInt(verze);
        kryo_output.flush();        
        boolean versionPresent = kryo_input.readBoolean();        
        if (versionPresent){        
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
        } else {
            throw new WrongVersionNumber();
        }        
    }

    /**
     * Returns false if and only if the user is trying to download a directory into a regular file.
     * @param source
     * @param destination
     * @return 
     */
    private boolean isCompatible(Path destination, DItem it){
        if (it != null){
            return !it.isDir() || Files.isDirectory(destination);
        } else {
            return false;
        }
    } 
    
    /**
     * Given the file "sourceFile" on server and the path "dest" at the client, </br>
     * this method downloads the contents of "sourceFile" to "dest", creating the </br>
     * target file if necessary
     * @param dest Marks where the downloaded file should be saved.
     * @param sourceFile A path on the server.
     * @param zip Should the downloaded file(s) be zipped?
     * @param frame If null, command-line interface is used. Otherwise, a parent window is provided.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveFile(Path dest, List<String> sourceFile, final boolean zip, JFrame frame) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{  
        return receiveVersion(dest, sourceFile, null, zip, frame);
    }    
    
    /**
     * Given the file "sourceFile" on server, it's version "version" and the path "destination" at the client, </br>
     * this method downloads the contents of the version to "destination"
     * @param destination Marks where the downloaded file should be saved.
     * @param sourceFile A path on the server.
     * @param versionNumber A version number of the version to download.
     * @param zip Should the downloaded file(s) be zipped?
     * @param frame If null, command-line interface is used. Otherwise, a parent window is provided.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveVersion(Path destination, List<String> sourceFile, final Integer versionNumber, 
            final boolean zip, JFrame frame) throws IOException, ClassNotFoundException, WrongVersionNumber{
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
        int[] data = serveGetBin(sourceFile, versionNumber, zip);
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
     * @param src Path on the server.
     * @param dest Path on the local computer.
     * @param zip Should the downloaded files be zipped?
     * @param toplevel Marks whether we are at the top level of recursion.
     * @param item The DItem object representing the desired path on the server.
     * @param frame If null, command-line interface is used. Else, a parent window is given.
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    boolean receiveDirectory(List<String> src, Path dest, final boolean zip, final boolean toplevel, DItem item, JFrame frame) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{
        if ((src == null) || (dest == null) || (item == null)){
            return false;
        }
        boolean res = false;
        if (zip){
            res = receiveFile(dest, src, true, frame);
        } else {
            if (toplevel){
                Path dest2 = Paths.get(dest.toAbsolutePath().toString(), src.get(src.size()-1));
                Files.createDirectories(dest2);
                dest = dest2;
            }
            if (item.isDir()){
                DDirectory dir = (DDirectory) item;
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
                        DItem item2 = entry.getValue();
                        receiveDirectory(src2, dest2, false, false, item2, frame);
                    } else {
                        // handle entry as a regular file
                        receiveFile(dest2, src2, false, frame);
                    }
                }
                res = true;
            }            
        }
        return res;
    }            
    
    /**
     * Downloads a valid instance of the server filesystem root directory.
     * @return 
     */
    Map<String,DItem> getFS(){
        kryo.writeObject(kryo_output, Requests.GET_FS);
        kryo_output.flush();
        Map<String,DItem> res = new HashMap<>();
        int count = kryo_input.readInt();
        for (int i = 0; i<count; i++){
            String key = kryo_input.readString();
            boolean isDir = kryo_input.readBoolean();
            DItem val;
            if (isDir){
                val = kryo.readObject(kryo_input, DDirectory.class, DDirectory.getSerializer());
            } else {
                val = kryo.readObject(kryo_input, DFile.class, DFile.getSerializer());
            }
            res.put(key, val);
        }
        return res;
    }
        
    /**
     * Downloads a map of all the block objects from the server.
     * @return 
     */
    private Map<String,DBlock> getServerBlocks(){
        kryo.writeObject(kryo_output, Requests.GET_SERVER_BLOCKS);
        kryo_output.flush();
        Map<String,DBlock> res = new HashMap<>();
        int count = kryo_input.readInt();
        for (int i = 0; i<count; i++){
            String key = kryo_input.readString();
            DBlock val = kryo.readObject(kryo_input, DBlock.class, DBlock.getSerializer());
            res.put(key, val);
        }
        return res;
    }
    
    /**
     * Checks whether the specified "target" path points to an existing </br>
     * file/directory on server.
     * @param target
     * @return 
     */
    private boolean itemExistsOnServer(Collection<String> target){
        if (target == null){
            throw new IllegalArgumentException();
        }        
        String[] target2 = target.toArray(new String[0]);
        kryo.writeObject(kryo_output, Requests.ITEM_EXISTS);
        kryo.writeObject(kryo_output, target2);
        kryo_output.flush();
        return kryo_input.readBoolean();
    }
    
    /**
     * Serves the user request of type "add" , which adds a new file/directory into the system
     * @param request The user request of type "add", parsed into a list.
     * @param frame The JFrame used to show info/error messages. If null, standard ouput is used.
     * @param checkSize If set, the server checks whether the reserved space is not exceeded.
     * @throws IOException 
     */
    void serveAdd(List<String> request, JFrame frame, boolean checkSize) throws IOException {                        
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
                kryo_output.writeLong(ServerUtils.getDirectorySize(file));
            } else {
                kryo.writeObject(kryo_output, Requests.CREAT_FILE);                
                kryo_output.writeLong(Files.size(file));
            }
            kryo_output.writeBoolean(checkSize);
            kryo.writeObject(kryo_output, target.toArray(new String[0]));
            kryo_output.flush();
            if (checkSize){
                boolean enough_space = kryo_input.readBoolean();
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
        }                                 
        if(Files.isDirectory(file)){
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(file)){
                for(Path p : ds){
                    List<String> newRequest = new ArrayList<>();
                    newRequest.add(request.get(0));
                    newRequest.add(p.toAbsolutePath().toString());
                    newRequest.add(request.get(2) + "/" + p.getFileName().toString());
                    serveAdd(newRequest, frame, false);
                }
            }
            return;
        }                                                
        byte[] fileContents = Files.readAllBytes(file);        
        if (fileContents != null){
            kryo.writeObject(kryo_output, Requests.CHECK_CHANGES);
            kryo.writeObject(kryo_output, target.toArray(new String[0]));                
            String contentHash = ServerUtils.computeStrongHash(fileContents);
            kryo_output.writeString(contentHash);
            kryo_output.flush();
            boolean changed = kryo_input.readBoolean();
            if (changed){
                kryo.writeObject(kryo_output, Requests.CREAT_VERS);
                kryo_output.writeBoolean(checkSize);
                kryo_output.writeLong((long)fileContents.length);
                kryo_output.flush();
                int vel_bloku = kryo_input.readInt();
                if (checkSize){
                    boolean enough_space = kryo_input.readBoolean();
                    if(enough_space){
                        kryo.writeObject(kryo_output, target.toArray(new String[0]));
                        kryo_output.writeString(contentHash);                                        
                        windowLoop(fileContents, vel_bloku); 
                        kryo_input.readBoolean(); //server-side finished
                    } else {
                        if (frame == null){
                            stdout.println(messages.getString("not_enough_space"));
                            throw new IOException();    
                        } else {
                            JOptionPane.showMessageDialog(frame, messages.getString("not_enough_space"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                            throw new IOException();
                        }
                    }
                } else {
                    kryo.writeObject(kryo_output, target.toArray(new String[0]));
                    kryo_output.writeString(contentHash);                                        
                    windowLoop(fileContents, vel_bloku); 
                    kryo_input.readBoolean(); //server-side finished
                }
            }
        }
    }
    
    /**
     * Retrieves a set of weak hash values of all the blocks on the server.
     * @return 
     */
    private Set<Long> getHashValues(){
        kryo_output.writeString("get_vals");
        kryo_output.flush();
        Set<Long> res = new HashSet<>();
        int count = kryo_input.readInt();
        for (int i = 0; i<count; i++){
            res.add(kryo_input.readLong());
        }
        return res;
    }
    
    /**
     * After creating new blocks, the server sends the new weak hash values </br>
     * back to the client. This methods receives it and adds it to the specified set.
     * @param values A set to which the new values will be added.
     */
    private void updateHashValues(Set<Long> values){
        int count = kryo_input.readInt();
        for (int i = 0; i<count; i++){
            values.add(kryo_input.readLong());
        }
    }
    
    /**
     * If the section of input file that matches no known block grows over this limit, 
     * it is sent to the server.
     */
    private final int unmatchedLimit = 1024*1024*8;
    
    /**
     * Processes an input file with the "window" method. At every position of the window
     * it checks the database for the current block. Depending on whether the block is new 
     * or already present, furher actions differ.
     * @param fileContents Contents of the file to be processed.
     * @param blockSize Used block size.
     * @throws IOException 
     */
    private void windowLoop(byte[] fileContents, int blockSize) throws IOException {      
        Set<Long> hashValues = getHashValues();
        try (ByteArrayInputStream fin = new ByteArrayInputStream(fileContents)){
            RollingHash rh = new RollingHash(blockSize);
            for(int i = 0; i<blockSize-1; i++){        
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
                if (not_matched.size() > unmatchedLimit){
                    kryo_output.writeString("raw_data");
                    byte[] res = new byte[not_matched.size()];
                    int i = 0;
                    for (Byte bb : not_matched){
                        res[i++] = bb;
                    }                                                
                    kryo.writeObject(kryo_output, res);
                    kryo_output.flush();
                    not_matched.clear();
                    updateHashValues(hashValues);
                }
                
                byte b = (byte) (c-128);
                byte old = rh.add(b);
                if(!beginning) {
                    not_matched.add(old);
                }
                beginning = false;
                
//                Hash collisions must be taken care of.
                long hash = rh.getHash();
                String hexHash2 = null;
                boolean is_in_db = hashValues.contains(hash);
                if(is_in_db){                    
                    hexHash2 = rh.getHexHash2();
                    if (!blockExists(hexHash2)){
                        is_in_db = false;
                    }                    
                }

                if(is_in_db){
//                    Send the previous bytes.
                    if(!not_matched.isEmpty()){                        
                        kryo_output.writeString("raw_data");
                        byte[] res = new byte[not_matched.size()];
                        int i = 0;
                        for (Byte bb : not_matched){
                            res[i++] = bb;
                        }                                                
                        kryo.writeObject(kryo_output, res);
                        kryo_output.flush();
                        not_matched.clear();
                        updateHashValues(hashValues);
                    }
                  
//                    Send the hash values of the block.
                    kryo_output.writeString("hash");
                    kryo_output.writeLong(hash);
                    kryo_output.writeString(hexHash2);
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
                        
//            Deal with data in "not_matched".
//            Covers the case when the input file is smaller than the block size.
            if (add_from_rh){
                for (byte b : rh.getValidData()){
                    not_matched.add(b);
                }
            }

//          All the bytes that are left must be now sent.
            boolean finished = false;
            if(not_matched.size() < blockSize){
                long hash = RollingHash.computeHash(not_matched, not_matched.size());
                String hexHash2 = ServerUtils.computeStrongHash(not_matched, not_matched.size());
                if ((hashValues.contains(hash)) && (blockExists(hexHash2))){
                    kryo_output.writeString("hash");
                    kryo_output.writeLong(hash);
                    kryo_output.writeString(hexHash2);
                    kryo_output.flush();
                    finished = true;
                }                 
            }
            if (!finished){
                kryo_output.writeString("raw_data");
                byte[] res = new byte[not_matched.size()];
                int i = 0;
                for (Byte b : not_matched){
                    res[i++] = b;
                }
                kryo.writeObject(kryo_output, res);
                kryo_output.flush();
                updateHashValues(hashValues);
            }
            kryo_output.writeString("end");
            kryo_output.flush();
        }
    }
    
    /**
     * Contacts the server and checks whether a block with the specified strong </br>
     * hash is present.
     * @param strongHash
     * @return 
     */
    private boolean blockExists(String strongHash){
        kryo_output.writeString("check");
        kryo_output.writeString(strongHash);
        kryo_output.flush();
        return kryo_input.readBoolean();
    }
    
    /**
     * Serves the user request of type "delete", which deletes a version of a file,
     * unless the version is the only one present.
     * @param request A user request of type "delete", parsed into a list.
     * @return Whether the deletion succeded.
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
        kryo_output.writeInt(verNum2);
        kryo_output.flush();
        return kryo_input.readBoolean();
    }
}

class WrongVersionNumber extends Exception {}