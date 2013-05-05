package cz.filipekt;

import java.util.Arrays;
import java.util.List;

/**
 *  The main class for computing the rolling hash over any data <br/>
 *  represented as sequence of bytes
 * @author Tomas Filipek
 */
class RollingHash{
    /**
     * The helper value for updating the main rolling hash value
     */
    private long A = 0L;
    
    /**
     * The main rolling hash value
     */
    private long B = 0L;
    
    /**
     * Size of the (algebraic) field in which the computations are executed.
     */
    private static final long Mod = Integer.MAX_VALUE + 1L;
    
    /**
     * 
     * @return The value of rolling hash for "data".
     */
    long getHash(){
        return A + Mod * B;
    }
    
    /**
     * 
     * @return The value of safe hash for "data" , represented in hexadecimal format     
     */
    String getHexHash2() {
        return ServerUtils.computeStrongHash(data.getInternalArray(), counterLength, data.left, data.right);
    }       
    
    /**
     * Data, over which is the "counter" of rolling hash
     */
    private RoundArray data;
    
    /**
     * 
     * @return "data" as byte[], but just those bytes in the "valid" section
     */
    byte[] getValidData(){
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
    void resetValid(){
        valid = 0;
    }
    
    /**
     * The length of the "counter" of the rolling hash
     */
    private final int counterLength;    

    int getCounterLength() {
        return counterLength;
    }

    RollingHash(int n){
        counterLength = n;
        data = new RoundArray(n * 16);
        for (int i = 0; i<n; i++){
            data.addLast((byte)0);
        }        
        valid = 0;        
    }

    /**
     * Moves the "counter" by one byte. The new byte is "newByte", the old byte is the return value.
     * @param newByte
     * @return The byte which was dropped from the "counter"
     */
    byte add(byte b){        
        int oldByte = (int)data.getFirst() + 128;
        int newByte = (int)b + 128;
        data.addLast(b);
        A -= oldByte;
        A += newByte;
        A %= Mod;
        B -= (counterLength * oldByte);
        B += A;
        B %= Mod;
        if (valid < counterLength){
            valid++;
        }
        return data.removeFirst();
    }                   
    
    /**
     * Computes the primary hash for "inData", padding the input data with zero's at the end up to 
     * "capacity", if needed.
     * @param inData
     * @return Rolling hash for "inData", with "counter" of size equal to the size of "inData"
     */    
    static long computeHash(List<Byte> inData, int capacity){
        byte[] inData2 = new byte[capacity];
        int i = 0;
        for (Byte b : inData){
            inData2[i++] = b;
            if (i >= capacity){
                break;
            }
        }
        while (i < capacity){
            inData2[i++] = (byte)0;
        }
        return RollingHash.computeHash(inData2);
    }     
    
    static long computeHash(byte[] data){
        RollingHash rh = new RollingHash(data.length);
        for (byte b : data){
            rh.add(b);
        }
        return rh.getHash();
    }           
    
    static long computeHash(byte[] data, int windowSize, int from, int to) {
        byte[] completeData = new byte[windowSize];
        int i = 0;
        for (int j = from; j < to; j++){
            completeData[i++] = data[j];
        }
        while (i < windowSize){
            completeData[i++] = (byte)0;
        }
        return RollingHash.computeHash(completeData);
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
