package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;


/**
 * Representation of a version of a file for the use by Database, DFile
 * @author Tomas Filipek
 */
class DVersion {    
    
    /**
     * The list of blocks of this version - if script-form is used, then 
     * "blocks" should be null.
     */
    private List<DBlock> blocks;

    void setBlocks(List<DBlock> blocks) {
        synchronized (lockObject){
            this.blocks = blocks;
        }
    }

    List<DBlock> getBlocks() {
        synchronized (lockObject){       
            if (blocks == null){
                return null;
            } else {
                return Collections.unmodifiableList(blocks);
            }
        }
    }
    
    /**
     * Returns the number of blocks that this version consists of.
     * @return 
     */
    int getBlockCount(){
        synchronized (lockObject){
            if (blocks == null){
                return 0;
            } else {
                return blocks.size();
            }
        }
    }
    
    /**
     * Used for synchronization.
     */
    private final Object lockObject = new Object();
    
    /**
     * Time of addition of this version to the database
     */
    private final Date addedDate;        
    
    Date getAddedDate() {
        return addedDate;
    }    
    
    /**
     * Is this version represented as a script?
     * If not, it is assumed, that the version is stored in blocks of data.
     */
    private boolean scriptForm;

    void setScriptForm(boolean scriptForm) {
        this.scriptForm = scriptForm;
    }
    
    boolean isScriptForm() {
        return scriptForm;
    }    
    
    /**
     * The name of the file this version belongs to.
     */
    private final String fileName;
    
    String getFileName() {
        return fileName;
    }    
    
    /**
     * The block size used
     */
    private final int blockSize;
    
    int getBlockSize() {
        return blockSize;
    }    
    
    /**
     * Strong hash value of the version contents.
     */
    private final String contentHash;        

    String getContentHash() {
        return contentHash;
    }
    
    /**
     * Size of the version data in bytes.
     */
    private final long size;

    long getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.addedDate);
        hash = 59 * hash + Objects.hashCode(this.fileName);
        hash = 59 * hash + Objects.hashCode(this.contentHash);
        hash = 59 * hash + (int) (this.size ^ (this.size >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DVersion other = (DVersion) obj;
        if (!Objects.equals(this.addedDate, other.addedDate)) {
            return false;
        }
        if (!Objects.equals(this.fileName, other.fileName)) {
            return false;
        }
        if (!Objects.equals(this.contentHash, other.contentHash)) {
            return false;
        }
        if (this.size != other.size) {
            return false;
        }
        return true;
    }
            
    @Override
    public String toString(){        
        return addedDate + (scriptForm ? " : scripted" : "");
    }
    
    DVersion(List<DBlock> blocks, int blockSize, String fileName, String contentHash, long size){
        this(blocks, new Date(), false, fileName, blockSize, contentHash, size);
    }    

    private DVersion(List<DBlock> blocks, Date addedDate, boolean scriptForm, String fileName, int blockSize, String contentHash, long size) {
        this.blocks = blocks;
        this.addedDate = addedDate;
        this.scriptForm = scriptForm;
        this.fileName = fileName;
        this.blockSize = blockSize;
        this.contentHash = contentHash;
        this.size = size;
    }        
    
    /**
     * Estimates the total number of bytes this version occupies.
     * @return 
     */
    int estimateSize(){
        synchronized (lockObject){
            if (blocks != null){
                int total = 0;
                for (DBlock block : blocks){
                    total += block.getUsed();
                }
                return total;
            } else {
                return 0;
            }
        }
    }
    
    static Serializer<DVersion> getSerializer(){
        return new DVersion.DVersionSerializer();
    }
    
    private static class DVersionSerializer extends Serializer<DVersion>{

        @Override
        public void write(Kryo kryo, Output output, DVersion t) {
            output.writeLong(t.addedDate.getTime());
            output.writeString(t.fileName);
            output.writeInt(t.blockSize);
            output.writeString(t.contentHash);
            output.writeLong(t.size);
            output.writeBoolean(t.scriptForm);
            if (t.blocks == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.blocks.size());                
                for (DBlock block : t.blocks){
                    kryo.writeObject(output, block, DBlock.getSerializer());
                }
            }            
        }

        @Override
        public DVersion read(Kryo kryo, Input input, Class<DVersion> type) {
            long date = input.readLong();
            Date addedDate = new Date(date);
            String fileName = input.readString();
            int blockSize = input.readInt();
            String contentHash = input.readString();
            long size = input.readLong();
            boolean scriptForm = input.readBoolean();
            List<DBlock> blocks = new ArrayList<>();
            int length = input.readInt();
            DVersion res = new DVersion(blocks, addedDate, scriptForm, fileName, blockSize, contentHash, size);
            for (int i = 0; i<length; i++){
                DBlock item = kryo.readObject(input, DBlock.class, DBlock.getSerializer());
                res.blocks.add(item);
            }
            return res;
        }        
    }
}
