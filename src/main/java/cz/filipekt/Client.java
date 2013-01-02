package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of the client that connects to server
 * @author Lifpa
 */
public class Client {
    public static void main(String[] args){        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        start(args, br);
    }
    
    static void start(String[] args, BufferedReader br){
        try {                        
            String[] args2;
            String comp;
            int port_num;
            if((args2=ServerUtils.getAddr(args))!=null){
                comp = args2[0];
                port_num = Integer.parseInt(args2[1]);
            } else {
                System.out.println("Which computer do you want to connect to? ");        
                comp = br.readLine();            

                port_num = 0;
                boolean jeKorektni = false;
                while(!jeKorektni){
                    System.out.println("Which port do you want to connect to? ");
                    String port = br.readLine();                
                    try{
                        port_num = Integer.valueOf(port);
                        if (port_num<0 || port_num>50000){
                            throw new NumberFormatException();
                        }
                        jeKorektni = true;
                    } catch (NumberFormatException ex){
                        System.err.println("The port number can not be used. ");
                    }
                }
            }            
            try{
                Client cl = new Client(comp, port_num ,br, args);
//                    Client cl = new Client("localhost", 5555, br, args);
                try{
                    cl.work();                
                } catch (Exception ex){
                    System.out.println(ex.toString());
                } finally{
                    cl.end();
                }
            } catch (IOException ex){
                System.err.println("Sorry, connection could not be established.");
            }
        } catch (IOException ex){
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    /**
     * Input stream from server
     */
    InputStream in_s;        
    
    /**
     * Output stream to server
     */
    OutputStream out_s;    
    
    /**
     * Standard input for client
     */
    BufferedReader stdin;
    
    /**
     * The upper limit on how many times the client tries to <br/>
     * take care of a user's request
     */
    private static final int attempt_limit = 3;
    
    /**
     * Socket used to communicate with server
     */
    Socket socket;
    
    /**
     * Base class of the Kryo framework for fast (de-)serialization of objects
     */
    Kryo kryo;
    
    /**
     * Specific output stream for the use by Kryo framework
     */
    Output kryo_output;
    
    /**
     * Specific input stream for the use by Kryo framework
     */
    Input kryo_input;
    
    /**
     * Object stream from the server
     */
    ObjectInputStream ois;
    
    public Client(String comp, int port, BufferedReader br) throws IOException{
        InetAddress addr = InetAddress.getByName(comp);
        socket = new Socket(addr,port);        
        in_s = socket.getInputStream();
        ois = new ObjectInputStream(in_s);
        out_s = socket.getOutputStream();
        kryo = new Kryo();        
        kryo_output = new Output(out_s);
        kryo_input = new Input(in_s);
        stdin = br;
    }
    
    public Client(String comp, int port, BufferedReader br, String[] args) throws IOException{
        this(comp, port, br);
        this.args = args;
    }
    
    /**
     * The program arguments
     */
    String[] args = null;
    
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
        ois.close();
    }
    
    /**
     * Gets the type of the user's request and delegetes <br/>
     * it to other methods if needed
     * @return Success/failure of operation
     */
    private boolean serveOperation(){
        try {
            List<String> request;
            if(args.length==0){
                System.out.println("Enter the opration:");
                String request1 = stdin.readLine();
                String[] request2 = request1.split(" ");                
                request = new LinkedList<>();
                for (String s : request2){
                    if (!s.equals("")){
                        request.add(s);
                    }
                }
                if(request.isEmpty()){
                    System.err.println("You entered no operation.");
                    throw new IOException();
                }
            } else {
                request = Arrays.asList(args);
            }
            request = ServerUtils.concatQuotes(request);
            if (switchToOperation(request) == false){
                return false;
            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(args.length>0){
            try {                
                kryo.writeObject(kryo_output, Requests.END);       
                kryo_output.flush();
            } catch (RuntimeException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
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
        switch(request.get(0)){
            case "add":
                if(request.size()<2){
                    System.err.println("Usage: add <SOURCE_FILE> [TARGET_FILE]");
                    break;
                }
                if(request.size()==2){
                    String name = ServerUtils.parseName(request.get(1)).getLast();
                    request.add(name);
                }
                for (int i = 0; i<attempt_limit; i++){
                    try{                            
                        serveAdd(request);
                        break;
                    } catch (IOException | NoSuchAlgorithmException ex){
                        String poradi = i==(attempt_limit-1) ? "Last" : (i+1 + ".");                            
                        System.err.println(poradi + " attempt to serve the operation was unsuccessful.");
                    }
                }                    
                break;
            case "get":
                if(request.size()<2){
                    System.err.println("Usage: get <FILE> [VERSION_NUMBER]");
                    break;
                }                                        
                for (int i = 0; i<attempt_limit; i++){
                    try{
                        if(request.size()>=3){
                            Integer.parseInt(request.get(2));
                        }
                        serveGet(request);
                        break;
                    } catch (NumberFormatException | IOException | ClassNotFoundException | WrongVersionNumber ex){
                        String poradi = i==(attempt_limit-1) ? "Last" : (i+1 + ".");                            
                        System.err.println(poradi + " attempt to serve the operation was unsuccessful.");
                    }
                }  
                break;
            case "delete_file":
                break;
            case "delete_ver":
                break;
            case "exit":
                kryo.writeObject(kryo_output, Requests.END);
                return false;
            case "list":                    
                Database db = getDB();
                if (db!=null){
                    db.printContents(null, 0);
                }                    
                break;
            default:
                break;
        }
        return true;
    }
    /**
     * Serves the "get" request - accepts data and prints it
     * @param request
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SpatneCisloVerze 
     */
    private void serveGet(List<String> request) throws IOException, ClassNotFoundException, WrongVersionNumber{                
//        List<String> name = ServerUtils.parseName(request.get(1));
//        Database db = getDB();
//        if(!db.fileExists(name)){
//            System.err.println("Sorry, the file does not exist.");
//            return;
//        }        
//        kryo.writeObject(kryo_output, Requests.GET_FILE);
//        kryo.writeObject(kryo_output, name.toArray(new String[0]));        
//        int verze;
//        if(request.size()>=3){
//            verze = Integer.parseInt(request.get(2)) - 1;
//        } else {
//            verze = db.findFile(name).version_list.size() - 1;
//        }
//        if(verze >= db.findFile(name).version_list.size()){
//            throw new WrongVersionNumber();
//        }
//        kryo.writeObject(kryo_output, Integer.valueOf(verze));
//        kryo_output.flush();
//
//        List<Byte> data = Arrays.asList(kryo.readObject(kryo_input, Byte[].class));
//        int c;
//        for(byte b : data){
//            c = (int)b + 128;
//            System.out.write((char) c);
//        }
//        System.out.println();
        int[] data = serveGetBin(request);
        if (data == null){
            return;
        }
        for(int i : data){
            System.out.print((char)i);
        }
        System.out.println();
    }
    
    int[] serveGetBin(List<String> request) throws IOException, ClassNotFoundException, WrongVersionNumber{       
        List<String> name = ServerUtils.parseName(request.get(1));
        Database db = getDB();
        if(!db.fileExists(name)){
            System.err.println("Sorry, the file does not exist.");
            return null;
        }        
        kryo.writeObject(kryo_output, Requests.GET_FILE);
        kryo.writeObject(kryo_output, name.toArray(new String[0]));        
        int verze;
        if(request.size()>=3){
//            verze = Integer.parseInt(request.get(2)) - 1;
            verze = Integer.parseInt(request.get(2));
        } else {
            verze = db.findFile(name).version_list.size() - 1;
        }
        if(verze >= db.findFile(name).version_list.size()){
            throw new WrongVersionNumber();
        }
        kryo.writeObject(kryo_output, Integer.valueOf(verze));
        kryo_output.flush();

        List<Byte> data = Arrays.asList(kryo.readObject(kryo_input, Byte[].class));
        int c; 
        int[] res = new int[data.size()];
        int i = 0;
        for(byte b : data){
            c = (int)b + 128;
            res[i++] = c;
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
                kryo.writeObject(kryo_output, Requests.GET_LIST);
                kryo_output.flush();
                Database db = (Database) ois.readObject();
                return db;
            } catch (KryoException | IOException | ClassNotFoundException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }            
        }
        return null;
    }
    
    /**
     * Serves the user request of type "add" , which adds a new file/directory into the system
     * @param request
     * @throws PrenosSeNepovedl 
     */
    void serveAdd(List<String> request) throws IOException, NoSuchAlgorithmException {        
    //        klient normalne posle vybrane block_map, "vytvori novou verzi", server pak ale vetsinou tuto
    //        "verzi" porovna s posledni vlozenou(tu sesklada z ed.skriptu), a ulozi jen editacni skript
    //        ..zustane minimalni traffic, prida to casovou komplexitu pro server, snizi prostorovou kompl. na serveru            
        Database db = getDB();
        if (db==null){
            System.out.println("Sorry, connection to the database could not be established. Operation \"add\" has not been completed.");
            return;
        }                                    
        String fname = request.get(1);
        LinkedList<String> target = ServerUtils.parseName(request.get(2));
        Path file = Paths.get(fname);
        if(Files.notExists(file)){
            System.err.println("Sorry, the file " + fname + " does not exist.");
            return;
        }
        boolean alreadyPresent = db.fileExists(target);
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
                System.err.println("Sorry, there is not enough space on server.");
                return;
            }
        }                         
        
        if(Files.isDirectory(file)){
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(file)){
                for(Path p : ds){
                    List<String> newRequest = new LinkedList<>();
                    newRequest.add(request.get(0));
                    newRequest.add(p.toAbsolutePath().toString());
                    newRequest.add(request.get(2) + "/" + p.getFileName().toString());
                    serveAdd(newRequest);
                }
            }
            return;
        }                                
        int vel_bloku = ServerUtils.getBlockSize(file);                     

        kryo.writeObject(kryo_output, Requests.CREAT_VERS);
        kryo.writeObject(kryo_output, Files.size(file));
        kryo_output.flush();
        boolean enough_space = kryo.readObject(kryo_input, Boolean.TYPE);
        if (!enough_space){
            System.err.println("Sorry, there is not enough space on server.");
            return;
        }
        kryo.writeObject(kryo_output, target.toArray(new String[0]));
        kryo.writeObject(kryo_output, (Integer)vel_bloku);
        kryo_output.flush();
        
//        System.out.println("Jde se projizdet okenkem.");

        try (BufferedInputStream fin = new BufferedInputStream(Files.newInputStream(file))) {
            RollingHash rh = new RollingHash(vel_bloku);
            for(int i = 0; i<vel_bloku-1; i++){        
                int c = fin.read();                    
                if (c==-1) {
                    break;
                } else {
                    byte b = (byte) (c-128);
                    rh.add(b);
                }                    
            }
            LinkedList<Byte> nesparovane = new LinkedList<>();
            int c;
            boolean pridat_z_rh = true;
            boolean zacatek = true;
            while ((c=fin.read())!=-1){
                byte b = (byte) (c-128);
                byte old = rh.add(b);
                if(!zacatek) {
                    nesparovane.add(old);
                }
                zacatek = false;

                //jeste zbyva osetrit hash kolize
                String hexHash = rh.getHexHash();                
                String hexHash2 = null;
                boolean je_v_db = db.blockExists(hexHash);                    
                if(je_v_db){                    
                    hexHash2 = rh.getHexHash2();
                    if (!db.blockExists(hexHash, hexHash2)){
                        je_v_db = false;
                    }                    
                }

                if(je_v_db){
                    //odeslat predchozi bajty(
                    if(!nesparovane.isEmpty()){                        
                        kryo.writeObject(kryo_output, "raw_data");
                        kryo.writeObject(kryo_output, nesparovane.toArray(new Byte[0]));                                       
                        nesparovane.clear();
                    }

                    //odeslat hash bloku                    
                    kryo.writeObject(kryo_output, hexHash);
                    kryo.writeObject(kryo_output, hexHash2);
                    kryo.writeObject(kryo_output, (Integer)vel_bloku);

                    // a preskocit dal
                    rh.resetValid();
                    pridat_z_rh = false;
                    for (int i = 0; i<rh.counter_length-1; i++){
                        int lc = fin.read();
                        if (lc==-1){                                
                            break;
                        }
                        byte lb = (byte) (lc-128);
                        rh.add(lb);
                        pridat_z_rh = true;        
                    }
                    zacatek = true;

                } else {                    
                    pridat_z_rh = true;
                } 

            }
            // co ted se zbytky dat v nesparovane
            // pripad ze soubor je mensi nez velikost bloku
            if (pridat_z_rh){
                for (byte b : rh.getValidData()){
                    nesparovane.addLast(b);
                }
            }

//            System.out.println("Jde se posilat.");            
            String hexHash = null;
            boolean maly = false;
            if(nesparovane.size() < vel_bloku){
                hexHash = RollingHash.computeHash(nesparovane);
                maly = true;
            }
            if (maly && db.blockExists(hexHash)){                
                kryo.writeObject(kryo_output, hexHash);
                String hexHash2 = RollingHash.computeHash2(nesparovane);
                kryo.writeObject(kryo_output, hexHash2);
                kryo.writeObject(kryo_output, (Integer)vel_bloku);
            } else {                
                kryo.writeObject(kryo_output, "raw_data");
                kryo.writeObject(kryo_output, nesparovane.toArray(new Byte[0]));
            }
            kryo.writeObject(kryo_output, "end");
            kryo_output.flush();
        }
    }    
}

class WrongVersionNumber extends Exception {}