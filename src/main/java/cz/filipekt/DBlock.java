package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.Objects;

/**
 * Representation of a block for the use by Database and others
 * @author Tomas Filipek
 */
class DBlock {    
    
    /**
     * The rolling hash value for this block
     */
    private final long hash;
    
    long getHash(){
        return hash;
    }
    
    String getHexHash(){
        return Long.toHexString(hash);
    }

    @Override
    public int hashCode() {
        int h = 3;
        h = 47 * h + Objects.hashCode(this.hash2);
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DBlock other = (DBlock) obj;
        if (!Objects.equals(this.hash2, other.hash2)) {
            return false;
        }
        return true;
    }
              
    /**
     * Composes the name of the file used to save the block contents
     * @return 
     */
    String getName(){        
        return DBlock.getName(getHexHash(), col);
    }
    
    /**
     * Composes the name of the file used to save the block contents
     * @param hexHash
     * @param col
     * @return 
     */
    static String getName(String hexHash, int col){
        if ((hexHash == null) || (col<0)){
            return null;
        } else {
            if (col==0){
                return  hexHash;
            } else {
                return hexHash + "v" + col;
            }   
        }
    }
    
    /**
     * The position in the hash collision list (the list can have holes in it, however)
     */
    private final int col;

    int getCol() {
        return col;
    }
           
    /**
     * The safe hash value for this block
     */
    private final String hash2;

    String getHash2() {
        return hash2;
    }
    
    /**
     * The block size used (in bytes)
     */
    private final int size;    

    int getSize() {
        return size;
    }
    
    /**
     * How many bytes (from the beginning) is valid
     */
    private final int used;
    
    int getUsed() {
        return used;
    }    
    
    /**
     * How many times is this block referenced
     */
    private int refCount;

    int getRefCount() {
        return refCount;
    }

    void incrementRefCount(){
        refCount++;
    }
    
    void decrementRefCount(){
        if (refCount > 0){
            refCount--;
        }
    }

    DBlock(long hash, String hash2, int size, int used, int col, int refCount){
        this.hash = hash;
        this.hash2 = hash2;
        this.size = size;
        this.used = used;
        this.col = col;
        this.refCount = refCount;
    }
    
    static Serializer<DBlock> getSerializer(){
        return new DBlock.DBlockSerializer();
    }
    
    private static class DBlockSerializer extends Serializer<DBlock> {

        @Override
        public void write(Kryo kryo, Output output, DBlock t) {
            output.writeInt(t.col);
            output.writeInt(t.refCount);
            output.writeInt(t.size);
            output.writeInt(t.used);
            output.writeLong(t.hash);
            output.writeString(t.hash2);            
        }

        @Override
        public DBlock read(Kryo kryo, Input input, Class<DBlock> type) {            
            int col = input.readInt();
            int refCount = input.readInt();
            int size = input.readInt();
            int used = input.readInt();
            long hash = input.readLong();
            String hash2 = input.readString();
            return new DBlock(hash, hash2, size, used, col, refCount);            
        }        
    }
}
