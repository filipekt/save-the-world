package cz.filipekt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains various static helper methods.
 * @author Tomas Filipek
 */
class ServerUtils {        
                    
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
     * Computes the strong hash for "inData", padding the input data with zero's at the end up to 
     * "capacity", if needed.
     * @param in_data
     * @return Hexadecimal SHA-256 hash for "in_data"
     */
    static String computeStrongHash(List<Byte> in_data, int capacity) {
        byte[] data2 = new byte[capacity];
        int i = 0;
        for(byte b : in_data){
           data2[i++] = b;
           if (i >= capacity){
               break;
           }
        }
        while (i<capacity){
           data2[i++] = (byte)0;
        }
        return ServerUtils.computeStrongHash(data2);
    }  
    
    /**
     * Computes the strong hash for "inData"
     * @param in_data
     * @return 
     */
    static String computeStrongHash(byte[] in_data) {
        if ((in_data == null) || (in_data.length == 0)){
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(in_data);         
            byte[] h = md.digest();
            return new BigInteger(h).toString(16);       
        } catch (NoSuchAlgorithmException ex){
            System.out.println(ex.getLocalizedMessage());
            return "";
        }
    }
       
    /**
     * Computes the strong hash of data on positions "from" - "to" (exclusive).
     * Remaining bytes up to "windowSize" are filled with 0s.
     * @param data
     * @param windowSize 
     * @param from
     * @param to
     * @return 
     */
    static String computeStrongHash(byte[] data, int windowSize, int from, int to) {
        byte[] completeData = new byte[windowSize];
        int i = 0;
        for (int j = from; j < to; j++){
            completeData[i++] = data[j];
        }
        while (i < windowSize){
            completeData[i++] = (byte)0;
        }
        return ServerUtils.computeStrongHash(completeData);
    }    
    
    /**
     * Loads the contents of the "block" from disc and return it as array of bytes.
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
    
    /**
     * Checks whether the input String contains a String representation of a long variable.
     * @param s
     * @return 
     */
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
     * Saves the contents ("bytes") of the block "hash" into the appropriate file on disc
     * @param bytes
     * @param hash
     * @return Returns the position in the hash collision list
     * @throws IOException 
     */
    static int saveBlock(byte[] bytes, long hash, String homeDir, int validBytes) throws IOException{
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
                    for (int i = 0; i<validBytes; i++){
                        int b = (int)bytes[i] + 128;
                        bos.write(b);
                    }
                }
                hotovo = true;
            }                        
        }
        return v;
    }    
    
    /**
     * Deletes a file containing a block contents from disc.
     * @param name
     * @param home_dir
     * @throws IOException 
     */
    static void deleteBlock(String name, String home_dir) throws IOException{
        Path p = Paths.get(home_dir,name);
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
