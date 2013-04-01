package cz.filipekt;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
     * @return The value of rolling hash for "data".
     */
    public long getHash(){
        return B;
    }
    
    /**
     * 
     * @return The value of safe hash for "data" , represented in hexadecimal format
     * @throws NoSuchAlgorithmException 
     */
    public String getHexHash2() throws NoSuchAlgorithmException{
        return computeHash2(data.getInternalArray(), data.left, data.right);
    }       
    
    /**
     * Data, over which is the "counter" of rolling hash
     */
    private RoundArray data;
    
    /**
     * 
     * @return "data" as byte[], but just those bytes in the "valid" section
     */
    public byte[] getValidData(){
        byte[] res = new byte[valid];
        int i = 0;
        byte[] validData = Arrays.copyOfRange(data.asArray(), counterLength-valid, counterLength);
        for (byte b : validData){
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
    private final int counterLength;    

    public int getCounterLength() {
        return counterLength;
    }

    public RollingHash(int n){
        counterLength = n;
        data = new RoundArray(n * 16);
        for (int i = 0; i<n; i++){
            data.addLast((byte)0);
        }        
        valid = 0;        
    }

    /**
     * Moves the "counter" by one byte. The new byte is "b", the old byte is the return value.
     * @param b
     * @return The byte which was dropped from the "counter"
     */
    public byte add(byte b){        
        byte old_byte = data.getFirst();
        byte new_byte = b;
        data.addLast(b);
        A -= old_byte;
        A += new_byte;
        B -= (counterLength * old_byte);
        B += A;        
        if (valid<counterLength){
            valid++;
        }
        return data.removeFirst();
    }           
    
    /**
     * 
     * @param in_data
     * @return Rolling hash for "in_data", with "counter" of size equal to the size of "in_data"
     */    
    public static long computeHash(byte[] in_data){
        RollingHash rh = new RollingHash(in_data.length);
        for(byte b : in_data){
            rh.add(b);
        }
        return rh.getHash();
    }    
    
    /**
     * 
     * @param in_data
     * @return Rolling hash for "in_data", with "counter" of size equal to the size of "in_data"
     */    
    public static long computeHash(List<Byte> in_data){
        RollingHash rh = new RollingHash(in_data.size());
        for(byte b : in_data){
            rh.add(b);
        }
        return rh.getHash();
    }        
    

    /**
     * 
     * @param in_data
     * @return Hexadecimal SHA-2 hash for "in_data"
     * @throws NoSuchAlgorithmException 
     */
    public static String computeHash2(byte[] in_data) throws NoSuchAlgorithmException {
        if ((in_data == null) || (in_data.length == 0)){
            return "";
        }
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(in_data);         
        byte[] h = md.digest();
        return new BigInteger(h).toString(16);       
    }
    
    private String computeHash2(byte[] in_data, int from, int to) throws NoSuchAlgorithmException{
       MessageDigest md = MessageDigest.getInstance("SHA-512");
       md.update(in_data, from, to - from); 
       byte[] h = md.digest();
       return new BigInteger(h).toString(16);        
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
       return RollingHash.computeHash2(data2);
    }  
    
    /**
     * Custom implementation of a byte queue.
     */
    private class RoundArray {        

        RoundArray(final int length) {
            this.length = length;        
            data = new byte[length];        
        }

        void addLast(byte b){
            data[right] = b;
            right++;        
            if (right >= length){
                goBack();
            }
        }

        byte getFirst(){
            return data[left];
        }

        byte removeFirst(){
            byte val = data[left];
            left++;
            return val;
        }

        private void goBack(){
            int j = 0;
            for (int i = left; i<right; i++){
                data[j++] = data[i];
            }
            right = right - left;
            left = 0;
        }        

        byte[] asArray(){
            return Arrays.copyOfRange(data, left, right);
        }

        private final int length;

        private final byte[] data;

        private byte[] getInternalArray(){
            return data;
        }

        /**
         * Left, inclusive bound on the currently used positions in "data".
         */
        private int left = 0;

        /**
         * Right, exclusive bound on the currently used positions in "data".
         */
        private int right = 0;    
    }    
    
}