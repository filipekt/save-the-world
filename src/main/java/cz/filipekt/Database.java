package cz.filipekt;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;



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
    private final List<DFile> fileList = new LinkedList<>();
    
    /**
     * Getter for scriptLimit
     * @return 
     */
    public int getScriptLimit(){
        return scriptLimit;
    }
    
    /**
     * Getter for blockMap
     * @return 
     */
    TreeMap<String,DBlock> getBlockMap(){
        return blockMap;
    }
    
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
            current = ((DDirectory)current.get(s)).item_map;
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
            dir.item_map.put(new_file.filename, new_file);
        } else {
            fileMap.put(new_file.filename, new_file);
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
                current = ((DDirectory)current).item_map.get(path.get(i));            
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
        file.version_list.add(ver);
//        if(!ver.script_form){
//            ServerUtils.linkBlocksToVersion(ver);
//        }                    
    }    
    
    /**
     * Returns true if and only if the path "path" exists in the database filesystem
     * @param path
     * @return 
     */
    boolean fileExists(List<String> path){
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
    DBlock findBlock(String hash, String hash2){        
        for (DBlock bl : blockMap.values()){
            if (bl.hash.equals(hash) && bl.hash2.equals(hash2)){
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
    boolean blockExists(String hash){
        return blockMap.containsKey(hash);
    }
    
    /**
     * Returns true if and only if a block specified by the two hash <br/>
     * values is present in the database
     * @param hash1
     * @param hash2
     * @return 
     */
    boolean blockExists(String hash1, String hash2){
        return findBlock(hash1, hash2)!=null;
    }    

    public Database(){}
    Database(Map<String,DItem> file_map, TreeMap<String,DBlock> block_map){
        this.fileMap = file_map;
        this.blockMap = block_map;
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
            System.out.println(prefix + dir.name);            
            for(DItem it : ((DDirectory)item).item_map.values()){
                printContents(it, level+1);
            }
        } else {
            StringBuilder sb = new StringBuilder();
            for(int i = 0;i<level;i++){
                sb.append(' ');
            }
            String prefix = sb.toString();
            DFile file = (DFile) item;
            System.out.println(prefix + file.filename);
            for(DVersion verze : file.version_list){
                System.out.println(prefix + " |" + verze.added_date.toString());
                if(!verze.script_form){
                    for (DBlock db : verze.blocks){
                        System.out.println(prefix + " |" + "---> " + db.hash);
                    }
                } else {
                    System.out.println(prefix + " |" + "---> editacni skript");
                }
            }
        }
        if(level==0){
            System.out.println("-----------------");
            for(DBlock blok : blockMap.values()){
                System.out.println(blok.hash + " : refcount=" + blok.ref_count);
            }
        }        
    }    

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(scriptLimit);
        out.writeInt(blockMap.size());        
        for(Map.Entry<String,DBlock> entry : blockMap.entrySet()){
            out.writeUTF(entry.getKey());            
            out.writeObject(entry.getValue());
        }
        out.writeInt(fileMap.size());
        for(Map.Entry<String,DItem> entry : fileMap.entrySet()){
            out.writeUTF(entry.getKey());
            out.writeBoolean(entry.getValue().isDir());
            out.writeObject(entry.getValue());
        }
        out.writeInt(fileList.size());
        for (DFile df : fileList){
            out.writeObject(df);
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
    String filename;
    
    /**
     * The list of all present versions
     */
    List<DVersion> version_list;
    
    DFile(String filename, List<DVersion> version_list){
        this.filename = filename;
        this.version_list = version_list;
    }
    DFile(String filename){
        this.filename = filename;
        this.version_list = new LinkedList<>();
    }          
    public DFile() {}    
    
    /**
     * Return the future number of consecutive scripts, in case a new version is saved as a script
     * @return 
     */
    int futureNoOfConsecutiveScripts(){
        int count = 1;
        for(int i = version_list.size()-1; i>=0; i--){
            if (!version_list.get(i).script_form) {
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
        return getLatestNonScript(version_list.size() - 1);
    }
    
    DVersion getLatestNonScript(int start){
        for(int i = start; i>=0; i--){
            if (!version_list.get(i).script_form) {
                return version_list.get(i);
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
        return filename;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(filename);
        out.writeInt(version_list.size());
        for(DVersion dv : version_list){
            out.writeObject(dv);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        filename = in.readUTF();
        int count = in.readInt();
        version_list = new LinkedList<>();
        for (int i = 0; i<count; i++){
            version_list.add((DVersion) in.readObject());
        }
    }

    @Override
    public String getName() {
        return toString();
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
    private static final long serialVersionUID = 2012_12_29L;
    
    /**
     * The list of blocks of this version - if editational==true, then blocks==null
     */
    List<DBlock> blocks;
    
    /**
     * Time of addition of this version to the database
     */
    Date added_date;        
    
    /**
     * Is this version represented as a script?
     */
    boolean script_form;
    
    String fileName;
    
    /**
     * The block size used
     */
    int block_size;
    
    @Override
    public boolean equals(Object o){
        DVersion dv = (DVersion)o;
        if ((block_size != dv.block_size)
                || (script_form != dv.script_form)
                || !(added_date.equals(dv.added_date))
                || !(fileName.equals(dv.fileName))){
            return false;
        }                
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 43 * hash + Objects.hashCode(this.added_date);
        hash = 43 * hash + (this.script_form ? 1 : 0);
        hash = 43 * hash + Objects.hashCode(this.fileName);
        hash = 43 * hash + this.block_size;
        return hash;
    }
    
    @Override
    public String toString(){        
        return added_date + (script_form ? " : scripted" : "");
    }
    
    private DVersion(List<DBlock> blocks, Date added, int block_size){
        this.blocks = blocks;
        this.added_date = added;
        script_form = false;
        this.block_size = block_size;
    }
    DVersion(List<DBlock> blocks, int block_size, String fileName){
        this(blocks,new Date(),block_size);
        this.fileName = fileName;
    }    
    public DVersion(){}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        String date = DateFormat.getDateTimeInstance().format(added_date);
        out.writeUTF(date);
        out.writeUTF(fileName);
        out.writeInt(block_size);
        out.writeBoolean(script_form);
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
        try {
            String s = in.readUTF();
            added_date = DateFormat.getDateTimeInstance().parse(s);
            fileName = in.readUTF();
            block_size = in.readInt();
            script_form = in.readBoolean();
            int count = in.readInt();
            blocks = new LinkedList<>();
            for(int i = 0; i<count; i++){                
                blocks.add((DBlock) in.readObject());
            }
        } catch (ParseException ex) {
            Logger.getLogger(DVersion.class.getName()).log(Level.SEVERE, null, ex);
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
    private static final long serialVersionUID = 2012_12_29L;
    
    /**
     * The rolling hash value for this block
     */
    String hash;
    
    /**
     * The position in the hash collision list (the list can have holes in it)
     */
    int col = 0;
    
    /**
     * The safe hash value for this block
     */
    String hash2;
    
    /**
     * The block size used (in bytes)
     */
    int size;    
    
    /**
     * How many bytes (from the beginning) is valid
     */
    int used;
    
    /**
     * How many times is this block referenced
     */
    int ref_count = 0;

    DBlock(String hash, String hash2, int size, int used){
        this.hash = hash;
        this.hash2 = hash2;
        this.size = size;
        this.used = used;
    }
    public DBlock(){}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(col);
        out.writeInt(ref_count);
        out.writeInt(size);
        out.writeInt(used);
        out.writeUTF(hash);
        out.writeUTF(hash2);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        col = in.readInt();
        ref_count = in.readInt();
        size = in.readInt();
        used = in.readInt();
        hash = in.readUTF();
        hash2 = in.readUTF();
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
    String name;
    
    /**
     * List of all items this directory contains
     */
    Map<String,DItem> item_map;
    
    @Override
    public boolean isDir() {
        return true;
    }

    DDirectory(String name) {
        this(name, new HashMap<String,DItem>());
    }
    
    DDirectory(String name, Map<String,DItem> items){
        this.name = name;
        this.item_map = items;
    }
    public DDirectory(){}

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeInt(item_map.size());
        for(Map.Entry<String,DItem> entry : item_map.entrySet()){
            out.writeUTF(entry.getKey());
            out.writeBoolean(entry.getValue().isDir());
            out.writeObject(entry.getValue());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        int count = in.readInt();
        item_map = new HashMap<>();
        for (int i = 0; i<count; i++){
            String key = in.readUTF();
            boolean is_dir = in.readBoolean();
            if(is_dir){
                DDirectory value = (DDirectory) in.readObject();
                item_map.put(key, value);
            } else {
                DFile value = (DFile) in.readObject();
                item_map.put(key, value);
            }
        }
    }
    
    @Override
    public String toString(){
        return name;
    }

    @Override
    public String getName() {
        return toString();
    }
}

class MalformedPath extends Exception {}