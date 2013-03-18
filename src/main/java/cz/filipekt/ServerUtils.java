package cz.filipekt;

import difflib.Patch;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lifpa
 */
public class ServerUtils {    
    
    
    /**
     * Encodes the input array of bytes into a String
     * @param data
     * @return 
     */
    static String byteToString(byte[] data){
        StringBuilder sb = new StringBuilder();
        if(data!=null){
            for(Byte b : data){
                sb.append((char)((int)b + 128));
            }
        }
        return sb.toString();
    }
    
    /**
     * Helper class allowing for use of Long in anonymous inner classes, </br>
     * where only final variables can be references.
     */
    private static class HelperLong {
        private long val = 0;
        void addVal(long k){
            val += k;
        }
        long getVal(){
            return val;
        }
    }
    
    /**
     * Builds a typical String representation of the path specified </br>
     * as a list (src) of all the items on the path
     * @param src
     * @return 
     */
   static String constructPath(List<String> src){
        if (src == null){            
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<src.size(); i++){
            if (i!=0){
                sb.append("/");
            }
            sb.append(src.get(i));
        }        
        return sb.toString();
    }    
    
    /**
     * Computer the validBytes of the contents of the directory, recursively
     * @param p
     * @return
     * @throws IOException 
     */
    static long getDirectorySize(Path p) throws IOException{        
        final HelperLong hl = new HelperLong();
        Files.walkFileTree(p, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                hl.addVal(attrs.size());
                return super.visitFile(file, attrs);
            }
        });
        return hl.getVal();
    }    
    
    /**
     * Loads the contents of the "block" from disc and return it as array of bytes.
     * Only valid bytes are returned, ie. the padding 0s are not included.
     * @param block
     * @return
     * @throws IOException 
     */
    static byte[] loadBlockFromDisc(DBlock block, String dir) throws IOException{
        String name = block.getName();
        Path p = Paths.get(dir, name);
        int validBytes = block.getUsed();
        byte[] res = new byte[validBytes];        
        try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(p))){            
            int i = 0;        
            int c;
            while(((c=bis.read())!=-1) && (i < validBytes)){
                res[i++] = (byte) (c-128);                
            }                        
        }
        return res;
    }
    
    /**
     * Loads the contents of "version" from disc as a list of bytes
     * @param version
     * @return
     * @throws IOException
     * @throws BlockNotFound 
     */
    static byte[] loadVersionFromDisc(DVersion version, String dir) 
            throws IOException, BlockNotFound{        
        int versionSize = version.estimateSize();
        byte[] fileContent = new byte[versionSize];
        int i = 0;
        if (version.isScriptForm() || (version.getBlocks() == null)){
            throw new BlockNotFound();
        }
        for(DBlock block : version.getBlocks()){
            byte[] blockData = loadBlockFromDisc(block, dir);
            System.arraycopy(blockData, 0, fileContent, i, blockData.length);
            i += blockData.length;
        }
        return fileContent;
    }    
    
    
    
    /**
     * Encodes a String into an array of bytes.
     * The input String object is expected to have been created by the </br>
     * byteToString(..) method, because it only takes into account the lower </br>
     * 8 buts of each String character.
     * @param data
     * @return 
     */
    static byte[] stringToByte(String data){           
        if(data!=null && !data.isEmpty()){
            int length = data.length();
            byte[] res = new byte[length];
            int i = 0;            
            for(char c : data.toCharArray()){
                byte b = (byte)((int)c -128);
                res[i++] = b;
            }
            return res;
        } else {
            return new byte[0];
        }
    }          
    
    /**
     * With respect to the validBytes of file "f" determines the block validBytes, which will be used for storing the f's blocks.
     * @param f
     * @return 
     */
    static int getBlockSize(Path f){
        try {            
                    long size = Files.size(f);
                    String hexSize = Long.toHexString(size);
                    int c = 16;
                    switch(hexSize.length()){
                        case 1:
                        case 2:
                            return c;
                        case 3:
                            return c*16;
                        case 4:
                            return c*16*16;
                        default:
                            return c*16*16*16;
                    }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
        return 0;
    }    
    
    /**
     * Parses the path "fname" into a list of all the items on the path. File separator used: / or \\
     * @param fname
     * @return 
     */
    static LinkedList<String> parseName(String fname){
        LinkedList<String> res = new LinkedList<>();
        String[] spl = fname.split("[\\\\/]");
        for(String s : spl){
            if(!s.equals("")){
                res.add(s);
            }
        }        
        return res;
    }
    
    /**
     * From the program arguments "args" it delivers the values of switches -c and -p as a String[]
     * @param args
     * @return 
     */
    static String[] getAddr(String[] args){
        try{
            boolean comp = false;
            boolean port = false;
            String[] res = new String[2];
            for(int i = 0; i<args.length; i++){
                switch (args[i]) {
                    case "-c":
                        res[0] = args[i+1];
                        comp = true;
                        break;
                    case "-p":
                        res[1] = args[i+1];
                        port = true;
                        break;
                }
            }
            if(comp && port){
                return res;
            } else { 
                return null;
            }
        } catch (RuntimeException ex){
            return null;
        }
    }
    
    /**
     * Delivers the value of the -t switch from the program arguments "args"
     * @param args
     * @return 
     */
    static Integer getTime(String[] args){
        try{
            String time_s = null;
            for(int i = 0; i<args.length; i++){
                if(args[i].equals("-t")){
                    time_s = args[i+1];
                    break;
                }
            }
            return Integer.parseInt(time_s);
        } catch (RuntimeException ex){
            return null;
        }
    }
    
    /**
     * Delivers the value of the "opt" switch from the program arguments "args"
     * @param args
     * @return 
     */
    static String getArgVal(String[] args, String opt, boolean watchQuoting){
        if ((args == null) || (args.length==0) || (opt==null) || opt.isEmpty()){
            return null;
        }
        if (watchQuoting){
            List<String> args2 = ServerUtils.concatQuotes(Arrays.asList(args));
            args = args2.toArray(new String[0]);
        }
        try{
            String val = null;
            for(int i = 0; i<args.length; i++){
                if(args[i].equals("-" + opt)){
                    if (args[i+1].charAt(0)!='-'){
                        val = args[i+1];
                    }                    
                    break;
                }
            }
            return val;
        } catch (RuntimeException ex){
            return null;
        }
    }
    
    /**
     * Determines, whether the switch -opt is present in the program arguments.
     * @param args
     * @param opt
     * @return 
     */
    static boolean isSwitchPresent(String[] args, String opt){
        for (String s : args){
            if (s.equals("-" + opt)){
                return true;
            }
        }
        return false;
    }
    
    static boolean isLong(String s){
        try{
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException ex){
            return false;
        }
    }       
        
    /**
     * Decreases the reference count of every DBlock pointed to by the version
     * @param version 
     */
    static void unlinkBlocksFromVersion(DVersion version){
        if(version.getBlocks() != null){
            for (DBlock block : version.getBlocks()){
                block.decrementRefCount();
            }
            version.setBlocks(null);
        }
    }
    
    /**
     * For each DBlock used by the specified version, this method increases </br>
     * its reference count by one.
     * @param version 
     */
    static void linkBlocksToVersion(DVersion version){
        if(version.getBlocks() != null){
            for (DBlock block : version.getBlocks()){
                block.incrementRefCount();
            }
        }
    }
        
    /**
     * Retrieves an editational script from the database
     * @param version_list
     * @return 
     */
    static Patch getScript(DVersion verze, Map<DVersion,Patch> scripts){
        return scripts.get(verze);
    }
    
    /**
     * Saves the contents ("bytes") of the block "hash" into the appropriate file on disc
     * @param bytes
     * @param hash
     * @return Returns the position in the hash collision list
     * @throws IOException 
     */
    static int saveBlock(byte[] bytes, long hash, String homeDir) throws IOException{
        boolean hotovo = false;
        int v = 0;
        while(!hotovo){
            String name = DBlock.getName(Long.toHexString(hash), v);            
            Path p = Paths.get(homeDir, name);
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
    
    static void deleteBlock(String key, String home_dir) throws IOException{
        Path p = Paths.get(home_dir,key);
        Files.delete(p);
    }
    

    
    
    /**
     * In the given list of program arguments "args", that have been parsed <br/>
     * in the standard way with whitespace as delimiter, concatenates the <br/>
     * consecutive arguments that have been framed in double quotes, into a <br/>
     * single argument, without quotes. <br/>
     * Ex.: from {"add" , "\"ab" , "cd\""} makes: {"add", "ab cd"}
     * @param args
     * @return 
     */
    static List<String> concatQuotes(List<String> args){
        if(args==null){
            return null;
        }
        List<String> res = new LinkedList<>();
        boolean uvnitr = false;
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<args.size(); i++){
            String s = args.get(i);                     
            if(s.length()>0 && s.charAt(0)=='"'){
                s = s.substring(1);
                sb.append(s);
                uvnitr = true;
                continue;
            }
            if(s.length()>0 && s.charAt(s.length()-1)=='"'){
                s = s.substring(0, s.length()-1);                
                sb.append(" ");
                sb.append(s);
                res.add(sb.toString());
                sb = new StringBuilder();
                uvnitr = false;
                continue;
            }
            if(uvnitr){
                sb.append(" ");
                sb.append(s);
                continue;
            } else {
                res.add(s);
            }
        }
        return res;
    }
}

