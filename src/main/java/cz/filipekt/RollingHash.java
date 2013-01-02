package cz.filipekt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 *  The main class for computing the rolling hash over any data <br/>
 *  represented as sequence of bytes
 * @author Lifpa
 */
public class RollingHash{
    /**
     * The helper value for updating the main rolling hash value
     */
    private long A = 0L;
    
    /**
     * The main rolling hash value
     */
    private long B = 0L;

    /**
     * 
     * @return The value of rolling hash for "data" , represented in hexadecimal format
     */
    public String getHexHash(){        
        return Long.toHexString(B);
    }    
    
    /**
     * 
     * @return The value of safe hash for "data" , represented in hexadecimal format
     * @throws NoSuchAlgorithmException 
     */
    public String getHexHash2() throws NoSuchAlgorithmException{
        return computeHash2(data);
    }    
    
    /**
     * Data, over which is the "counter" of rolling hash
     */
    private LinkedList<Byte> data;    
    
    /**
     * 
     * @return "data" as byte[], but just those bytes in the "valid" section
     */
    public byte[] getValidData(){
        byte[] res = new byte[valid];
        int i = 0;
        for (byte b : data.subList(counter_length-valid, counter_length)){
            res[i++] = b;
        }
        return res;
    }
    
    /**
     * How many bytes from the beggining of "data" is valid
     */
    private int valid;
    
    /**
     * Makes all bytes in "data" invalid
     */
    public void resetValid(){
        valid = 0;
    }
    
    /**
     * The length of the "counter" of the rolling hash
     */
    public final int counter_length;    

    public RollingHash(int n){
        counter_length = n;
        data = new LinkedList<>();
        for (int i = 0; i<n; i++){
            data.add((byte)0);
        }        
        valid = 0;        
    }

    /**
     * Moves the "counter" by one byte. The new byte is "b", the old byte is the return value.
     * @param b
     * @return The byte which was dropped from the "counter"
     */
    public byte add(byte b){        
        int old_byte = (int)data.getFirst();
        int new_byte = (int)b;
        A -= old_byte;
        A += new_byte;
        B -= counter_length * old_byte;
        B += A;
        data.addLast(b);
        if (valid<counter_length){
            valid++;
        }
        return data.removeFirst();
    }           
    
    /**
     * 
     * @param in_data
     * @return Hexadecimal rolling hash for "in_data", with "counter" of size equal to the size of "in_data"
     */    
    public static String computeHash(List<Byte> in_data){
        RollingHash rh = new RollingHash(in_data.size());
        for(byte b : in_data){
            rh.add(b);
        }
        return rh.getHexHash();
    }    

    /**
     * 
     * @param in_data
     * @return Hexadecimal SHA-2 hash for "in_data"
     * @throws NoSuchAlgorithmException 
     */
    public static String computeHash2(List<Byte> in_data) throws NoSuchAlgorithmException {
       byte[] data2 = new byte[in_data.size()];
       int i = 0;
       for(byte b : in_data){
           data2[i++] = b;
       }
       MessageDigest md = MessageDigest.getInstance("SHA-512");
       md.update(data2);                
       byte[] h = md.digest();
       return new BigInteger(h).toString(16);       
    }
}