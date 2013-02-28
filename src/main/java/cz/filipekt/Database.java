package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;



/** 
 *  Represents filesystem of the files which have been loaded to the server <br/>
 *  The actual data are not saved here, they are in separate files on disc
 * @author Lifpa
 */
public final class Database implements Externalizable{

    /**
     * Maps filenames to DItems, which is the representation of files </br>
     * Represents the root node of the database filesystem
     */
    private Map<String,DItem> fileMap;
    
    /**
     * Maps names of files containing blocks to objects of type DBlok, <br/>
     * which is the representation of blocks
     */
    private TreeMap<String,DBlock> blockMap;
    
    /**
     * Set of all primary hash values used in the present blocks.
     * Used mainly for determining whether a block is not present.
     */
    private TreeSet<Long> blockSet;    
    
    /**
     * Set of all secondary hash values used in the present blocks.
     * Used mainly for determining whether a block is present.
     */
    private TreeSet<String> blockSet2;
     
    void refreshBlockSet(){
        blockSet = new TreeSet<>();
        blockSet2 = new TreeSet<>();
        for (DBlock block : blockMap.values()){
            blockSet.add(block.getHash());
            blockSet2.add(block.getHash2());            
        }
    }
    /**
     * The upper limit on the number of consecutive versions saved as editational script
     */
    private int scriptLimit = 2;
    
    /**
     * Marks the version of serialized state used. 
     */
    private static final long serialVersionUID = 2012_12_29L;
    
    /**
     * A linear list view of all the DFiles present in the database
     */
    private List<DFile> fileList = new LinkedList<>();
    
    /**
     * Getter for scriptLimit
     * @return 
     */
    public int getScriptLimit(){
        return scriptLimit;
    }
    
//    /**
//     * Getter for blockMap
//     * @return 
//     */
//    TreeMap<String,DBlock> getBlockMap(){
//        return blockMap;
//    }
    
    Map<String,DItem> getFileMap(){
        return fileMap;
    }
    
    /**
     * Getter for fileList
     * @return 
     */
    List<DFile> getFileList(){
        return fileList;
    }
    
    /**
     * Creates a path and all the items on it
     * @param path
     * @return 
     */
    boolean makeDirs(List<String> path){
        Map<String,DItem> current = fileMap;
        for(String s : path){
            if(!current.containsKey(s)){
                current.put(s, new DDirectory(s));
            } else if (!current.get(s).isDir()){
                return false;
            }
            current = ((DDirectory)current.get(s)).getItemMap();
        }
        return true;
    }
    
    /**
     * Adds a new file specified by "path" to the database "filesystem"
     * @param name 
     */
    void addFile(List<String> path) throws MalformedPath{        
        if(!makeDirs(path.subList(0, path.size()-1))){
            throw new MalformedPath();
        }
        DFile new_file = new DFile(path.get(path.size()-1));
        DDirectory dir = (DDirectory) getItem(path.subList(0, path.size()-1));
        if(dir!=null){
            dir.getItemMap().put(new_file.getName(), new_file);
        } else {
            fileMap.put(new_file.getName(), new_file);
        }
        fileList.add(new_file);
    }
    
    /**
     * Finds a "path" in the database filesystem
     * @param path
     * @return 
     */
    DItem getItem(List<String> path){        
        try{
            DItem current = fileMap.get(path.get(0));
            for(int i = 1; i<path.size(); i++){
                current = ((DDirectory)current).getItemMap().get(path.get(i));            
            }
            return current;
        } catch (Exception ex){
            return null;
        }
    }
    
    /**
     * Wrapper function for getItem(..)
     * @param path
     * @return 
     */
    DFile findFile(List<String> path){
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
    
    /**
     * It adds a new version "ver" to the specified file ("fname") 
     * @param fname
     * @param ver 
     */
    void addVersion(List<String> path, DVersion ver){
        DFile file = (DFile) getItem(path);
        file.getVersionList().add(ver);  
    }    
    
    /**
     * Returns true if and only if the path "path" exists in the database filesystem
     * @param path
     * @return 
     */
    boolean itemExists(List<String> path){
        if (getItem(path)==null){
            return false;
        } else {
            return true;
        }
    }
    
    /** 
     * Finds and return a DBlok specified by its two hash values
     * @param hash
     * @return 
     */
    DBlock findBlock(long hash, String hash2){        
        for (DBlock bl : blockMap.values()){
            if ((bl.getHash() == hash) && bl.getHash2().equals(hash2)){
                return bl;
            }
        }
        return null;
    }

    
    /**
     * Returns true if and only if a block with a rolling hash value "hash" is present in the database
     * @param hash
     * @return 
     */
    boolean blockExists(long hash){
        return blockSet.contains(hash);
    }
    
    /**
     * Returns true if and only if a block specified by the two hash <br/>
     * values is present in the database
     * @param hash1
     * @param hash2
     * @return 
     */
    boolean blockExists(long hash1, String hash2){
        LightDatabase ld = getLightDatabase();
        return ld.blockExists1(hash1) && ld.blockExists2(hash2);
    }    

    public Database(){}
    Database(Map<String,DItem> file_map, TreeMap<String,DBlock> block_map, TreeSet<Long> blockSet, TreeSet<String> blockSet2){
        this.fileMap = file_map;
        this.blockMap = block_map;   
        this.blockSet  = blockSet;
        this.blockSet2 = blockSet2;
    }
    
    Collection<DBlock> collectBlocks(){
        Collection<DBlock> res = new HashSet<>();
        for(Iterator<Map.Entry<String,DBlock>> it = blockMap.entrySet().iterator(); it.hasNext(); ){
            Map.Entry<String,DBlock> entry = it.next();
            String key = entry.getKey();
            DBlock value = entry.getValue();
            if(value.getRefCount() == 0){
                res.add(value);
                it.remove();
            }                
        }        
        refreshBlockSet();        
        return res;
    }
    
    void addBlock(DBlock block){
        if (block != null){
            String name = block.getName();
            blockMap.put(name, block);
            blockSet.add(block.getHash());
            blockSet2.add(block.getHash2());
        }
    }
    
    /**
     * Prints the basic structure of the database contents in a human readable form
     * @param item
     * @param level 
     */
    void printContents(DItem item, int level){
        if(level==0){
            System.out.println("Obsah db:");
        }
        if(item==null){
            for(DItem it : fileMap.values()){
                printContents(it, level+1);
            }
        } else if (item.isDir()){
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<level;i++){
                sb.append(' ');
            }
            String prefix = sb.toString();
            DDirectory dir = (DDirectory) item;
            System.out.println(prefix + dir.getName());            
            for(DItem it : ((DDirectory)item).getItemMap().values()){
                printContents(it, level+1);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<level;i++){
                sb.append(' ');
            }
            String prefix = sb.toString();
            DFile file = (DFile) item;
            System.out.println(prefix + file.getName());
            for(DVersion verze : file.getVersionList()){
                System.out.println(prefix + " |" + verze.getAddedDate().toString());
                if(!verze.isScriptForm()){
                    for (DBlock db : verze.getBlocks()){
                        System.out.println(prefix + " |" + "---> " + db.getHexHash());
                    }
                } else {
                    System.out.println(prefix + " |" + "---> editacni skript");
                }
            }
        }
        if(level==0){
            System.out.println("-----------------");
            for(DBlock blok : blockMap.values()){
                System.out.println(blok.getHexHash() + " : refcount=" + blok.getRefCount());
            }
        }        
    }    

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(scriptLimit);
        if (blockMap == null){
            out.writeInt(0);
        } else {
            out.writeInt(blockMap.size());        
            for(Map.Entry<String,DBlock> entry : blockMap.entrySet()){
                out.writeUTF(entry.getKey());            
                out.writeObject(entry.getValue());
            }
        }
        if (fileMap == null){
            out.writeInt(0);
        } else {
            out.writeInt(fileMap.size());
            for(Map.Entry<String,DItem> entry : fileMap.entrySet()){
                out.writeUTF(entry.getKey());
                out.writeBoolean(entry.getValue().isDir());
                out.writeObject(entry.getValue());
            }
        }
        if (fileList == null){
            out.writeInt(0);
        } else {
            out.writeInt(fileList.size());
            for (DFile df : fileList){
                out.writeObject(df);
            }
        }
        if (blockSet == null){
            out.writeInt(0);
        } else {
            out.writeInt(blockSet.size());
            for (Long l : blockSet){
                out.writeLong(l);
            }
        }
        if (blockSet2 == null){
            out.writeInt(0);
        } else {
            out.writeInt(blockSet2.size());
            for (String s : blockSet2){
                out.writeUTF(s);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        scriptLimit = in.readInt();
        int block_map_size = in.readInt();
        blockMap = new TreeMap<>();        
        for (int i = 0; i<block_map_size; i++){
            String key = in.readUTF();
            DBlock value = (DBlock) in.readObject();
            blockMap.put(key, value);
        }
        int file_map_size = in.readInt();
        fileMap = new HashMap<>();
        for (int i = 0; i<file_map_size; i++){
            String key = in.readUTF();
            boolean is_dir = in.readBoolean();            
            if(is_dir){
                DDirectory value = (DDirectory) in.readObject();
                fileMap.put(key, value);
            } else {
                DFile value = (DFile) in.readObject();
                fileMap.put(key, value);
            }
        }                        
        int file_list_size = in.readInt();        
        for (int i = 0; i< file_list_size; i++){
            DFile df = (DFile) in.readObject();
            fileList.add(df);
        }        
        int block_set_size = in.readInt();
        blockSet = new TreeSet<>();
        for (int i = 0; i<block_set_size; i++){
            long val = in.readLong();
            blockSet.add(val);
        }
        int block_set2_size = in.readInt();
        blockSet2 = new TreeSet<>();
        for (int i = 0; i< block_set2_size; i++){
            String val = in.readUTF();
            blockSet2.add(val);
        }
    }
    
    private static class DatabaseSerializer extends Serializer<Database> {

        @Override
        public void write(Kryo kryo, Output output, Database t) {
            output.writeInt(t.scriptLimit);
            if (t.blockMap == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.blockMap.size());
                for (Entry<String,DBlock> entry : t.blockMap.entrySet()){
                    output.writeString(entry.getKey());
                    kryo.writeObject(output, entry.getValue(), DBlock.getSerializer());
                }
            }
            if (t.blockSet == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.blockSet.size());
                for (Long l : t.blockSet){
                    output.writeLong(l);
                }
            }
            if (t.blockSet2 == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.blockSet2.size());
                for (String s : t.blockSet2){
                    output.writeString(s);
                }
            }
            if (t.fileList == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.fileList.size());
                for (DFile df : t.fileList){
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
                        kryo.writeObject(output, entry.getValue(), DDirectory.getSerializer());
                    } else {
                        kryo.writeObject(output, entry.getValue(), DFile.getSerializer());
                    }
                }
            }
        }

        @Override
        public Database read(Kryo kryo, Input input, Class<Database> type) {
            Database res = new Database();
            res.scriptLimit = input.readInt();
            int blockMapSize = input.readInt();
            res.blockMap = new TreeMap<>();
            for (int i = 0; i<blockMapSize; i++){
                String key = input.readString();
                DBlock val = kryo.readObject(input, DBlock.class, DBlock.getSerializer());
                res.blockMap.put(key, val);
            }
            int blockSetSize = input.readInt();
            res.blockSet = new TreeSet<>();
            for (int i = 0; i<blockSetSize; i++){
                res.blockSet.add(input.readLong());
            }
            int blockSet2Size = input.readInt();
            res.blockSet2 = new TreeSet<>();
            for (int i = 0; i<blockSet2Size; i++){
                res.blockSet2.add(input.readString());
            }
            int fileListSize = input.readInt();
            res.fileList = new ArrayList<>();
            for (int i = 0; i< fileListSize; i++){
                res.fileList.add(kryo.readObject(input, DFile.class, DFile.getSerializer()));
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
        return new LightDatabase(blockSet, blockSet2);
    }
   
}

class LightDatabase implements Externalizable{
    private TreeSet<Long> primaryHashes;
    
    private TreeSet<String> secondaryHashes;
    
    boolean blockExists1(long hash){
        return primaryHashes.contains(hash);
    }
    
    boolean blockExists2(String hash){
        return secondaryHashes.contains(hash);
    }

    public LightDatabase(TreeSet<Long> primaryHashes, TreeSet<String> secondaryHashes) {
        this.primaryHashes = primaryHashes;
        this.secondaryHashes = secondaryHashes;
    }        

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        if (primaryHashes == null){
            oo.writeInt(0);
        } else {
            oo.writeInt(primaryHashes.size());
            for (long l : primaryHashes){
                oo.writeLong(l);
            }
        }
        if (secondaryHashes == null){
            oo.writeInt(0);
        } else {
            oo.writeInt(secondaryHashes.size());
            for (String s : secondaryHashes){
                oo.writeUTF(s);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        int size1 = oi.readInt();
        primaryHashes = new TreeSet<>();
        for (int i = 0; i<size1; i++){
            long val = oi.readLong();
            primaryHashes.add(val);
        }
        int size2 = oi.readInt();
        secondaryHashes = new TreeSet<>();
        for (int i = 0; i<size2; i++){
            String val = oi.readUTF();
            secondaryHashes.add(val);
        }
    }
    
    static Serializer<LightDatabase> getSerializer(){
        return new LightDatabaseSerializer();
    }
    
    private static class LightDatabaseSerializer extends Serializer<LightDatabase>{

        @Override
        public void write(Kryo kryo, Output output, LightDatabase t) {
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















/**
 * Representation of a file for the use by Database
 * @author Lifpa
 */
class DFile implements DItem{
    /**
     * Marks the version of serialized state used. 
     */
    private static final long serialVersionUID = 2012_12_29L;    
    
    /**
     * The name of this file
     */
    private String name;

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * The list of all present versions
     */
    private List<DVersion> versionList;

    public List<DVersion> getVersionList() {
        return versionList;
    }
    
    DFile(String filename, List<DVersion> version_list){
        this.name = filename;
        this.versionList = version_list;
    }
    DFile(String filename){
        this.name = filename;
        this.versionList = new LinkedList<>();
    }          
    public DFile() {}    
    
    /**
     * Return the future number of consecutive scripts, in case a new version is saved as a script
     * @return 
     */
    int futureNoOfConsecutiveScripts(){
        int count = 1;
        for(int i = versionList.size()-1; i>=0; i--){
            if (!versionList.get(i).isScriptForm()) {
                break;
            } else {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Returns the latest version not saved as a script
     * @return 
     */
    DVersion getLatestNonScript(){
        return getLatestNonScript(versionList.size() - 1);
    }
    
    DVersion getLatestNonScript(int start){
        for(int i = start; i>=0; i--){
            if (!versionList.get(i).isScriptForm()) {
                return versionList.get(i);
            }
        }   
        return null;
    }
    
    /**
     * Returns true if and only if there exists a version of this file which is not saved as a script
     * @return 
     */
    boolean nonScriptExists(){
        return getLatestNonScript()!=null;
    }
    
    /**
     * Returns true if and only if this object represents a directory. <br/>
     * Of course, always returns false.
     * @return 
     */
    @Override
    public boolean isDir() {
        return false;
    }
    
    @Override
    public String toString(){
        return name;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(versionList.size());
        for(DVersion dv : versionList){
            out.writeObject(dv);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        int count = in.readInt();
        versionList = new LinkedList<>();
        for (int i = 0; i<count; i++){
            versionList.add((DVersion) in.readObject());
        }
    }
    
    static Serializer<DFile> getSerializer(){
        return new DFileSerializer();                
    }
    
    private static class DFileSerializer extends Serializer<DFile>{

        @Override
        public void write(Kryo kryo, Output output, DFile t) {
            output.writeString(t.name);
            output.writeInt(t.versionList.size());
            output.flush();
            for (DVersion dv : t.versionList){
                kryo.writeObject(output, dv, DVersion.getSerializer());
            }
        }

        @Override
        public DFile read(Kryo kryo, Input input, Class<DFile> type) {
            DFile res = new DFile();
            res.name = input.readString();
            res.versionList = new LinkedList<>();
            int length = input.readInt();
            for (int i = 0; i<length; i++){
                DVersion item = kryo.readObject(input, DVersion.class, DVersion.getSerializer());
                res.versionList.add(item);
            }
            return res;
        }
        
    }
}





/**
 * Representation of a version of a file for the use by Database, DFile
 * @author Lifpa
 */
class DVersion implements Externalizable{
    /**
     * Marks the version of serialized state used. 
     */
    private static final long serialVersionUID = 2013_02_23L;
    
    /**
     * The list of blocks of this version - if editational==true, then blocks==null
     */
    private List<DBlock> blocks;

    public void setBlocks(List<DBlock> blocks) {
        this.blocks = blocks;
    }

    public List<DBlock> getBlocks() {
        return blocks;
    }
    
    /**
     * Time of addition of this version to the database
     */
    private Date addedDate;        
    
    public Date getAddedDate() {
        return addedDate;
    }    
    
    /**
     * Is this version represented as a script?
     */
    private boolean scriptForm;

    public void setScriptForm(boolean scriptForm) {
        this.scriptForm = scriptForm;
    }
    
    public boolean isScriptForm() {
        return scriptForm;
    }    
    
    private String fileName;
    
    public String getFileName() {
        return fileName;
    }    
    
    /**
     * The block size used
     */
    private int blockSize;
    
    public int getBlockSize() {
        return blockSize;
    }    
    
    @Override
    public boolean equals(Object o){
        if (o instanceof DVersion){        
            DVersion dv = (DVersion)o;
            if ((blockSize != dv.blockSize)
                    || (scriptForm != dv.scriptForm)
                    || !(addedDate.equals(dv.addedDate))
                    || !(fileName.equals(dv.fileName))){
                return false;
            }                
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.addedDate);
        hash = 43 * hash + (this.scriptForm ? 1 : 0);
        hash = 43 * hash + Objects.hashCode(this.fileName);
        hash = 43 * hash + this.blockSize;
        return hash;
    }
    
    @Override
    public String toString(){        
        return addedDate + (scriptForm ? " : scripted" : "");
    }
    
    private DVersion(List<DBlock> blocks, Date added, int block_size){
        this.blocks = blocks;
        this.addedDate = added;
        scriptForm = false;
        this.blockSize = block_size;
    }
    DVersion(List<DBlock> blocks, int block_size, String fileName){
        this(blocks,new Date(),block_size);
        this.fileName = fileName;
    }    
    public DVersion(){}
    
    /**
     * Estimates the total number of bytes this version occupies.
     * @return 
     */
    int estimateSize(){
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(addedDate.getTime());
        out.writeUTF(fileName);
        out.writeInt(blockSize);
        out.writeBoolean(scriptForm);
        if (blocks==null){
            out.writeInt(0);
        } else {
            out.writeInt(blocks.size());
            for(DBlock bl : blocks){
                out.writeObject(bl);
            }
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            long millis = in.readLong();
            addedDate = new Date(millis);
            fileName = in.readUTF();
            blockSize = in.readInt();
            scriptForm = in.readBoolean();
            int count = in.readInt();
            blocks = new ArrayList<>();
            for(int i = 0; i<count; i++){                
                blocks.add((DBlock) in.readObject());
            }
    }
    
    static Serializer<DVersion> getSerializer(){
        return new DVersionSerializer();
    }
    
    private static class DVersionSerializer extends Serializer<DVersion>{

        @Override
        public void write(Kryo kryo, Output output, DVersion t) {
            output.writeLong(t.addedDate.getTime());
            output.writeString(t.fileName);
            output.writeInt(t.blockSize);
            output.writeBoolean(t.scriptForm);
            if (t.blocks == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.blocks.size());
                output.flush();
                for (DBlock block : t.blocks){
                    kryo.writeObject(output, block, DBlock.getSerializer());
                }
            }
        }

        @Override
        public DVersion read(Kryo kryo, Input input, Class<DVersion> type) {
            DVersion res = new DVersion();
            long date = input.readLong();
            res.addedDate = new Date(date);
            res.fileName = input.readString();
            res.blockSize = input.readInt();
            res.scriptForm = input.readBoolean();
            res.blocks = new ArrayList<>();
            int length = input.readInt();
            for (int i = 0; i<length; i++){
                DBlock item = kryo.readObject(input, DBlock.class, DBlock.getSerializer());
                res.blocks.add(item);
            }
            return res;
        }
        
    }
}











/**
 * Representation of a block for the use by Database and others
 * @author Lifpa
 */
class DBlock implements Externalizable{
    /**
     * Marks the version of serialized state used. 
     */
    private static final long serialVersionUID = 2013_02_26L;
    
    /**
     * The rolling hash value for this block
     */
    private long hash;
    
    long getHash(){
        return hash;
    }
    
    String getHexHash(){
        return Long.toHexString(hash);
    }
    
    @Override public boolean equals(Object o){
        if (o instanceof DBlock){
            DBlock t = (DBlock)o;
            return  ((hash == t.hash) || hash2.equals(t.hash2) || (size == t.size));
        } else {
            return false;
        }
    }  

    @Override
    public int hashCode() {
        int h = 7;
        h = 71 * h + (int) (this.hash ^ (this.hash >>> 32));
        h = 71 * h + Objects.hashCode(this.hash2);
        h = 71 * h + this.size;
        return h;
    }

    
    /**
     * Composes the name of the file used to save the block contents
     * @return 
     */
    String getName(){
        String hexHash = getHexHash();
        if (col==0){
            return  hexHash;
        } else {
            return hexHash + "v" + col;
        }        
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
     * The position in the hash collision list (the list can have holes in it)
     */
    private int col = 0;

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }
    
    
    
    /**
     * The safe hash value for this block
     */
    private String hash2;

    public String getHash2() {
        return hash2;
    }
    
    /**
     * The block size used (in bytes)
     */
    private int size;    

    public int getSize() {
        return size;
    }
    
    /**
     * How many bytes (from the beginning) is valid
     */
    private int used;
    
    public int getUsed() {
        return used;
    }    
    
    /**
     * How many times is this block referenced
     */
    private int refCount = 0;

    public int getRefCount() {
        return refCount;
    }

    public void incrementRefCount(){
        refCount++;
    }
    
    public void decrementRefCount(){
        refCount--;
    }

    DBlock(long hash, String hash2, int size, int used){
        this.hash = hash;
        this.hash2 = hash2;
        this.size = size;
        this.used = used;
    }
    public DBlock(){}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(col);
        out.writeInt(refCount);
        out.writeInt(size);
        out.writeInt(used);
        out.writeLong(hash);
        out.writeUTF(hash2);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        col = in.readInt();
        refCount = in.readInt();
        size = in.readInt();
        used = in.readInt();
        hash = in.readLong();
        hash2 = in.readUTF();
    }
    
    static Serializer<DBlock> getSerializer(){
        return new DBlockSerializer();
    }
    
    private static class DBlockSerializer extends Serializer<DBlock> {

        @Override
        public void write(Kryo kryo, Output output, DBlock t) {
            output.writeInt(t.col, true);
            output.writeInt(t.refCount, true);
            output.writeInt(t.size, true);
            output.writeInt(t.used, true);
            output.writeLong(t.hash);
            output.writeString(t.hash2);            
        }

        @Override
        public DBlock read(Kryo kryo, Input input, Class<DBlock> type) {
            DBlock res = new DBlock();
            res.col = input.readInt(true);
            res.refCount = input.readInt(true);
            res.size = input.readInt(true);
            res.used = input.readInt(true);
            res.hash = input.readLong();
            res.hash2 = input.readString();
            return res;
        }
        
    }
}

interface DItem extends Externalizable{
    boolean isDir();   
    String getName();
}






/**
 * Representation of a directory for the use by Database and others
 * @author Lifpa
 */
class DDirectory implements DItem {
    /**
     * Marks the version of serialized state used. 
     */
    private static final long serialVersionUID = 2012_12_29L;    

    /**
     * The name of this directory
     */
    private String name;
    
    @Override
    public String getName() {
        return name;
    }    
    
    /**
     * List of all items this directory contains
     */
    private Map<String,DItem> itemMap;

    public Map<String, DItem> getItemMap() {
        return itemMap;
    }
    
    @Override
    public boolean isDir() {
        return true;
    }

    DDirectory(String name) {
        this(name, new HashMap<String,DItem>());
    }
    
    DDirectory(String name, Map<String,DItem> items){
        this.name = name;
        this.itemMap = items;
    }
    public DDirectory(){}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(itemMap.size());
        for(Map.Entry<String,DItem> entry : itemMap.entrySet()){
            out.writeUTF(entry.getKey());
            out.writeBoolean(entry.getValue().isDir());
            out.writeObject(entry.getValue());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        int count = in.readInt();
        itemMap = new HashMap<>();
        for (int i = 0; i<count; i++){
            String key = in.readUTF();
            boolean is_dir = in.readBoolean();
            if(is_dir){
                DDirectory value = (DDirectory) in.readObject();
                itemMap.put(key, value);
            } else {
                DFile value = (DFile) in.readObject();
                itemMap.put(key, value);
            }
        }
    }
    
    static Serializer<DDirectory> getSerializer(){
        return new DDirectorySerializer();
    }
    
    private static class DDirectorySerializer extends Serializer<DDirectory> {

        @Override
        public void write(Kryo kryo, Output output, DDirectory t) {
            output.writeString(t.name);
            output.writeInt(t.itemMap.size(), true);
            for(Map.Entry<String,DItem> entry : t.itemMap.entrySet()){
                output.writeString(entry.getKey()); 
                boolean isDir = entry.getValue().isDir();
                output.writeBoolean(isDir);
                output.flush();
                if (isDir){
                    kryo.writeObject(output, entry.getValue(), DDirectory.getSerializer());
                } else {
                    kryo.writeObject(output, entry.getValue(), DFile.getSerializer());
                }
            }            
        }

        @Override
        public DDirectory read(Kryo kryo, Input input, Class<DDirectory> type) {
            DDirectory res = new DDirectory();
            Map<String,DItem> map = new HashMap<>();
            res.name = input.readString();
            int length = input.readInt(true);
            for (int i = 0; i<length; i++){
                String name = input.readString();
                boolean isDir = input.readBoolean();
                DItem item;
                if (isDir){
                    item = kryo.readObject(input, DDirectory.class, DDirectory.getSerializer());
                } else {
                    item = kryo.readObject(input, DFile.class, DFile.getSerializer());
                }
                map.put(name, item);
            }
            res.itemMap = map;
            return res;
        }
        
    }
    
    @Override
    public String toString(){
        return name;
    }
}

class MalformedPath extends Exception {}