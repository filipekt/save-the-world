package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeSet;

/** 
 *  Represents filesystem of the files which have been loaded to the server <br/>
 *  The actual data is not saved here, it is in separate files on disc
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
     * Set of all primary h values used in the present blocks.
     * Used mainly for determining whether a block is not present.
     */
    private TreeSet<Long> blockHashes;    
    
    /**
     * Set of all secondary h values used in the present blocks.
     * Used mainly for determining whether a block is present.
     */
    private TreeSet<String> blockHashes2;
     
    /**
     * Updates the blockHashes and blockHashes2 sets (databases of block h values).
     * Source of the new information is the blockMap.
     */
    private void refreshBlockSet(){
        synchronized (lockObject){
            blockHashes = new TreeSet<>();
            blockHashes2 = new TreeSet<>();
            for (DBlock block : blockMap.values()){
                if (block != null){
                    blockHashes.add(block.getHash());
                    blockHashes2.add(block.getHash2());            
                }
            }
        }
    }      
    
    /**
     * Lock object is used for synchronization.
     */
    private final Object lockObject = new Object();
    
    /**
     * A linear list view of all the DFiles present in the database
     */
    private Collection<DFile> regularFiles = new TreeSet<>();       
    
    Map<String,DItem> getFileMap(){
        synchronized (lockObject){
            return Collections.unmodifiableMap(fileMap);
        }
    }
    
    /**
     * Getter for regularFiles
     * @return 
     */
    Collection<DFile> getFileCollection(){
        synchronized (lockObject){
            return Collections.unmodifiableCollection(regularFiles);
        }
    }
    
    void addDirectory(DDirectory dir){
        if (dir != null){
            synchronized (lockObject){
                fileMap.put(dir.getName(), dir);
            }
        }
    }
    
    /**
     * Creates a path and all the items on it
     * @param path
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
     * Adds a new file specified by "path" to the database "filesystem"
     * @param name 
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
     * Finds a "path" in the database filesystem
     * @param path
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
     * Wrapper function for getItem(..)
     * @param path
     * @return 
     */
    DFile findFile(List<String> path){
        synchronized (lockObject){
            DItem item = getItem(path);
            if(item==null){
                return null;
            }
            if(!item.isDir()){
                return (DFile)item;
            } else {
                return null;
            }
        }
    }    
    
    /**
     * It adds a new version "ver" to the specified file ("fname") 
     * @param fname
     * @param ver 
     */
    void addVersion(List<String> path, DVersion ver){
        synchronized (lockObject){
            DFile file = (DFile) getItem(path);
            file.addVersion(ver);
        }
    }    
    
    /**
     * Returns true if and only if the path "path" exists in the database filesystem
     * @param path
     * @return 
     */
    boolean itemExists(List<String> path){
        return getItem(path)!=null;
    }
    
    /** 
     * Finds and return a DBlock specified by its two h values
     * @param h
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
     * Returns true if and only if a block specified by the two h <br/>
     * values is present in the database
     * @param hash1
     * @param hash2
     * @return 
     */
    boolean blockExists(long hash1, String hash2){
        synchronized (lockObject){
            return blockHashes.contains(hash1) && blockHashes2.contains(hash2);
        }
    }    

    private Database(){}
    Database(Map<String,DItem> fileMap, Map<String,DBlock> blockSet, TreeSet<Long> hashes, TreeSet<String> hashes2){
        this.fileMap = fileMap;
        this.blockMap = blockSet;
        this.blockHashes  = hashes;
        this.blockHashes2 = hashes2;
    }
    
    /**
     * Iterates through all the blocks and returns those with reference count equal to zero.
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
     * Add the specified block in the block database.
     * @param block 
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
    
    /**
     * Prints the basic structure of the database contents in a human readable form
     * @param item
     * @param level 
     */
    void printContents(DItem item, final int level, final boolean verbose, PrintStream out, ResourceBundle messages){
        synchronized (lockObject){
            if(level==0){
                out.println(messages.getString("db_contents") + ":");
            }
            if(item==null){
                for(DItem it : fileMap.values()){
                    printContents(it, level+1, verbose, out, messages);
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
                    printContents(it, level+1, verbose, out, messages);
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
                out.println("-----------------");
                for (DBlock block : blockMap.values()){
                    out.println(block.getHexHash() + " : refcount=" + block.getRefCount());
                }
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
            res.blockHashes = new TreeSet<>();
            for (int i = 0; i<hashesSize; i++){
                res.blockHashes.add(input.readLong());
            }           
            int hashes2Size = input.readInt();
            res.blockHashes2 = new TreeSet<>();
            for (int i = 0; i<hashes2Size; i++){
                res.blockHashes2.add(input.readString());
            }            
            int fileListSize = input.readInt();
            res.regularFiles = new TreeSet<>();
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
    
    LightDatabase getLightDatabase(){
        synchronized (lockObject){
            return new LightDatabase(blockHashes, blockHashes2);
        }
    }
   
}

/**
 * A reduced version of the Database, contains only h values of all the blocks.
 * Used mainly in the window-loop phase of adding new files on the client-side.
 * @author Tomas Filipek
 */
class LightDatabase {
    private final TreeSet<Long> primaryHashes;
    
    private final TreeSet<String> secondaryHashes;
    
    boolean blockExists1(long hash){
        return primaryHashes.contains(hash);
    }
    
    boolean blockExists2(String hash){
        return secondaryHashes.contains(hash);
    }

    LightDatabase(final TreeSet<Long> primaryHashes, final TreeSet<String> secondaryHashes) {
        this.primaryHashes = primaryHashes;
        this.secondaryHashes = secondaryHashes;
    }        
    
    static Serializer<LightDatabase> getSerializer(){
        return new LightDatabaseSerializer();
    }
    
    private static class LightDatabaseSerializer extends Serializer<LightDatabase>{

        @Override
        public void write(Kryo kryo, Output output, LightDatabase t) {
            kryo.reset();
            if (t.primaryHashes == null){
                output.writeInt(0, true);
            } else {
                output.writeInt(t.primaryHashes.size(), true);
                for (long l : t.primaryHashes){
                    output.writeLong(l);
                }
            }
            if (t.secondaryHashes == null){
                output.writeInt(0, true);
            } else {
                output.writeInt(t.secondaryHashes.size(), true);
                for (String s : t.secondaryHashes){
                    output.writeString(s);
                }
            }
            kryo.reset();
        }

        @Override
        public LightDatabase read(Kryo kryo, Input input, Class<LightDatabase> type) {
            int size1 = input.readInt(true);
            TreeSet<Long> primaryHashes = new TreeSet<>();
            for (int i = 0; i<size1; i++){
                long val = input.readLong();
                primaryHashes.add(val);
            }
            int size2 = input.readInt(true);
            TreeSet<String> secondaryHashes = new TreeSet<>();
            for (int i = 0; i<size2; i++){
                String val = input.readString();
                secondaryHashes.add(val);
            }  
            return new LightDatabase(primaryHashes, secondaryHashes);
        }
        
    }
}

class TooFewVersions extends Exception {}
class MalformedPath extends Exception {}
