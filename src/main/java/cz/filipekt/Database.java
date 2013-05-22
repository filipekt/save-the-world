package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/** 
 *  Represents filesystem of the files which have been uploaded to the server. <br/>
 *  The actual data are held in separate files on disc.
 * @author Tomas Filipek
 */
class Database {

    /**
     * Maps filenames to DItems, which is the representation of files </br>
     * Represents the root node of the database filesystem
     */
    private Map<String,DItem> fileMap;
    
    /**
     * Contains the DBlock objects for all the present blocks.
     */
    private Map<String,DBlock> blockMap;
    
    /**
     * Set of all primary hash values used in the present blocks.
     * Used mainly for determining whether a block is not present.
     */
    private Set<Long> blockHashes;    
    
    /**
     * Set of all secondary hash values used in the present blocks.
     * Used mainly for determining whether a block is present.
     */
    private Set<String> blockHashes2;
     
    /**
     * Updates the blockHashes and blockHashes2 sets (databases of block hash values).
     * Source of the new information is the blockMap.
     */
    private void refreshBlockSet(){
        synchronized (lockObject){
            blockHashes = new HashSet<>();
            blockHashes2 = new HashSet<>();
            for (DBlock block : blockMap.values()){
                if (block != null){
                    blockHashes.add(block.getHash());
                    blockHashes2.add(block.getHash2());            
                }
            }
        }
    }   
    
    Set<Long> getBlockHashes(){
        synchronized (lockObject){
            return Collections.unmodifiableSet(blockHashes);
        }
    }
        
    Map<String,DBlock> getBlockMap(){
        synchronized (lockObject){
            return Collections.unmodifiableMap(blockMap);
        }
    }
    
    /**
     * Lock object is used for synchronization.
     */
    private final Object lockObject = new Object();
    
    /**
     * A linear list view of all the DFiles present in the database
     */
    private Collection<DFile> regularFiles = new HashSet();
    
    /**
     * Returns the root directory contents.
     * @return 
     */
    Map<String,DItem> getFileMap(){
        synchronized (lockObject){
            return Collections.unmodifiableMap(fileMap);
        }
    }
    
    /**
     * Getter for regularFiles.
     * @return 
     */
    Collection<DFile> getFileCollection(){
        synchronized (lockObject){
            return Collections.unmodifiableCollection(regularFiles);
        }
    }
    
    /**
     * Adds a new directory into the root directory.
     * @param dir The directory to be added.
     */
    private void addDirectory(DDirectory dir){
        if (dir != null){
            synchronized (lockObject){
                fileMap.put(dir.getName(), dir);
            }
        }
    }
    
    /**
     * Creates a path and all the items on it.
     * @param path The path to be created.
     * @return 
     */
    boolean makeDirs(List<String> path){
        synchronized (lockObject){
            Map<String,DItem> currentMap = fileMap;
            DDirectory currentDir = null;
            for(String s : path){
                if (currentDir == null){
                    if (!currentMap.containsKey(s)){
                        addDirectory(new DDirectory(s));
                    } else if (!currentMap.get(s).isDir()){
                        return false;
                    }
                } else {
                    if (!currentMap.containsKey(s)){
                        currentDir.addItem(new DDirectory(s));
                    } else if (!currentMap.get(s).isDir()){
                        return false;
                    }
                }
                currentDir = (DDirectory)currentMap.get(s);
                currentMap = currentDir.getItemMap();                                                
            }
            return true;
        }
    }
    
    /**
     * Adds a new file specified by "path" to the database filesystem.
     * @param path A path to the new file.
     * @throws MalformedPath 
     */
    void addFile(List<String> path) throws MalformedPath{  
        synchronized (lockObject){
            if(!makeDirs(path.subList(0, path.size()-1))){
                throw new MalformedPath();
            }
            DFile new_file = new DFile(path);
            DDirectory dir = (DDirectory) getItem(path.subList(0, path.size()-1));
            if(dir!=null){
                if (!dir.getItemMap().containsKey(new_file.getName())){
                    dir.addItem(new_file);
                } else {
                    DItem oldItem = dir.getItemMap().get(new_file.getName());
                    if (oldItem.isDir()){
                        throw new MalformedPath();
                    } else {
                        new_file = (DFile) oldItem;
                    }
                }
            } else {
                if (!fileMap.containsKey(new_file.getName())){
                    fileMap.put(new_file.getName(), new_file);
                } else {
                    DItem oldItem = fileMap.get(new_file.getName());
                    if (oldItem.isDir()){
                        throw new MalformedPath();
                    } else {
                        new_file = (DFile) oldItem;
                    }
                }
            }
            regularFiles.add(new_file);
        }
    }
    
    /**
     * Finds a "path" in the database filesystem.
     * @param path The path to search for.
     * @return 
     */
    DItem getItem(List<String> path){     
        if ((path == null) || (path.isEmpty())){
            return null;
        } else {
            synchronized (lockObject){
                DItem current = fileMap.get(path.get(0));
                if (current == null){
                    return null;
                } else {
                    for(int i = 1; i<path.size(); i++){
                        if ((current == null) || !(current instanceof DDirectory)){
                            return null;
                        } else {
                            current = ((DDirectory)current).getItemMap().get(path.get(i));            
                        }
                    }
                    return current;
                }
            }
        }
    }
    
    /**
     * Wrapper function for getItem(..), retrieves a regular file.
     * @param path Path to a regular file in the server database.
     * @return 
     */
    DFile findFile(List<String> path){
        synchronized (lockObject){
            DItem item = getItem(path);            
            if((item != null) && (!item.isDir())){
                return (DFile)item;
            } else {
                return null;
            }            
        }
    }    
    
    /**
     * It adds a new version to the file specified by a path.
     * @param path A path to the file to which we add a new version.
     * @param ver The version to be added.
     */
    void addVersion(List<String> path, DVersion ver){
        synchronized (lockObject){
            DFile file = (DFile) getItem(path);
            file.addVersion(ver);
        }
    }    
    
    /**
     * Returns true if and only if the path "path" exists in the database filesystem.
     * @param path The path to search for.
     * @return 
     */
    boolean itemExists(List<String> path){
        return getItem(path)!=null;
    }
    
    /**
     * Finds and returns a DBlock specified by its hash values.
     * @param hash A weak hash value.
     * @param hash2 A strong hash value.
     * @return 
     */
    DBlock findBlock(long hash, String hash2){
        synchronized (lockObject){
            if (!blockExists(hash, hash2)){
                return null;
            } else {                
                return blockMap.get(hash2);
            }
        }
    }    
    
    /**
     * Returns true if and only if a block specified by the two hash <br/>
     * values is present in the database
     * @param hash1 A weak hash value.
     * @param hash2 A strong hash value.
     * @return 
     */
    boolean blockExists(long hash1, String hash2){
        synchronized (lockObject){
            return blockHashes.contains(hash1) && blockHashes2.contains(hash2);
        }
    }    
    
    /**
     * Returns true if and only if a block with the specified strong hash exists.
     * @param strongHash A strong hash value.
     * @return 
     */
    boolean blockExists(String strongHash){
        synchronized (lockObject){
            return blockHashes2.contains(strongHash);
        }
    }

    private Database(){}
    Database(Map<String,DItem> fileMap, Map<String,DBlock> blockSet, Set<Long> hashes, Set<String> hashes2){
        this.fileMap = fileMap;
        this.blockMap = blockSet;
        this.blockHashes  = hashes;
        this.blockHashes2 = hashes2;
    }
    
    /**
     * Iterates through all of the blocks and returns those with reference count equal to zero.
     * These are also removed from the block database.
     * @return 
     */
    Collection<DBlock> collectBlocks(){
        synchronized (lockObject){
            Collection<DBlock> res = new HashSet<>();       
            Iterator<Entry<String,DBlock>> it = blockMap.entrySet().iterator();
            while (it.hasNext()){
                DBlock block = it.next().getValue();
                if ((block == null) || (block.getRefCount() == 0)){
                    if (block != null){
                        res.add(block);
                    }
                    it.remove();
                }
            }            
            refreshBlockSet();        
            return res;
        }
    }
    
    /**
     * Add the specified block into the block database.
     * @param block The block to be added.
     */
    void addBlock(DBlock block){
        synchronized(lockObject){
            if (block != null){
                blockMap.put(block.getHash2(), block);
                blockHashes.add(block.getHash());
                blockHashes2.add(block.getHash2());
            }
        }
    }         
    
    static Serializer<Database> getSerializer(){
        return new DatabaseSerializer();
    }
    
    private static class DatabaseSerializer extends Serializer<Database> {

        @Override
        public void write(Kryo kryo, Output output, Database t) {
            synchronized(t.lockObject){  
                if (t.blockMap == null){
                    output.writeInt(0);
                } else {
                    output.writeInt(t.blockMap.values().size());
                    for (DBlock block : t.blockMap.values()){
                        kryo.writeObject(output, block, DBlock.getSerializer());
                    }
                }
                if (t.blockHashes == null){
                    output.writeInt(0);
                } else {
                    output.writeInt(t.blockHashes.size());
                    for (Long l : t.blockHashes){
                        output.writeLong(l);
                    }
                }                
                if (t.blockHashes2 == null){
                    output.writeInt(0);
                } else {
                    output.writeInt(t.blockHashes2.size());
                    for (String s : t.blockHashes2){
                        output.writeString(s);
                    }
                }   
                if (t.regularFiles == null){
                    output.writeInt(0);
                } else {
                    output.writeInt(t.regularFiles.size());
                    for (DFile df : t.regularFiles){
                        kryo.writeObject(output, df, DFile.getSerializer());
                    }
                }
                if (t.fileMap == null){
                    output.writeInt(0);
                } else {
                    output.writeInt(t.fileMap.size());
                    for (Entry<String,DItem> entry : t.fileMap.entrySet()){
                        output.writeString(entry.getKey());
                        boolean isDir = entry.getValue().isDir();
                        output.writeBoolean(isDir);
                        if (isDir){
                            kryo.writeObject(output, (DDirectory) entry.getValue(), DDirectory.getSerializer());
                        } else {
                            kryo.writeObject(output, (DFile) entry.getValue(), DFile.getSerializer());
                        }
                    }
                }
            }
        }

        @Override
        public Database read(Kryo kryo, Input input, Class<Database> type) {
            Database res = new Database();            
            int blockSetSize = input.readInt();
            res.blockMap = new HashMap<>();
            for (int i = 0; i<blockSetSize; i++){
                DBlock val = kryo.readObject(input, DBlock.class, DBlock.getSerializer());
                res.blockMap.put(val.getHash2(), val);
            }
            
            int hashesSize = input.readInt();
            res.blockHashes = new HashSet<>();
            for (int i = 0; i<hashesSize; i++){
                res.blockHashes.add(input.readLong());
            }           
            int hashes2Size = input.readInt();
            res.blockHashes2 = new HashSet<>();
            for (int i = 0; i<hashes2Size; i++){
                res.blockHashes2.add(input.readString());
            }            
            int fileListSize = input.readInt();
            res.regularFiles = new HashSet<>();
            for (int i = 0; i< fileListSize; i++){                
                res.regularFiles.add(kryo.readObject(input, DFile.class, DFile.getSerializer()));
            }
            int fileMapSize = input.readInt();
            res.fileMap = new HashMap<>();
            for (int i = 0; i<fileMapSize; i++){
                String key = input.readString();
                boolean isDir = input.readBoolean();
                DItem val;
                if (isDir){
                    val = kryo.readObject(input, DDirectory.class, DDirectory.getSerializer());
                } else {
                    val = kryo.readObject(input, DFile.class, DFile.getSerializer());
                }
                res.fileMap.put(key, val);
            }
            return res;
        }        
    }     
}

class TooFewVersions extends Exception {}
class MalformedPath extends Exception {}
