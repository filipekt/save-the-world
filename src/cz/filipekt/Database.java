package cz.filipekt;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/** 
 *  Represents filesystem of the files which have been loaded to the server <br/>
 *  The actual data are not saved here, they are in separate files on disc
 * @author Lifpa
 */
public class Database implements Serializable{

    /**
     * Maps filenames to DItems, which is the representation of files
     */
    Map<String,DItem> file_map;
    
    /**
     * Maps names of files containing blocks to objects of type DBlok, <br/>
     * which is the representation of blocks
     */
    TreeMap<String,DBlock> block_map;
    
    /**
     * The upper limit on the number of consecutive versions saved as editational script
     */
    final int script_limit = 2;
    
    /**
     * Creates a path and all the items on it
     * @param path
     * @return 
     */
    boolean makeDirs(List<String> path){
        Map<String,DItem> current = file_map;
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
            file_map.put(new_file.filename, new_file);
        }
    }
    
    /**
     * Finds a "path" in the database filesystem
     * @param path
     * @return 
     */
    DItem getItem(List<String> path){        
        try{
            DItem current = file_map.get(path.get(0));
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
        if(!ver.editational){
            for (DBlock dbl : ver.blocks){
                dbl.ref_count++;
            }
        }                    
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
        for (DBlock bl : block_map.values()){
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
        return block_map.containsKey(hash);
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

    Database(){}
    Database(Map<String,DItem> file_map, TreeMap<String,DBlock> block_map){
        this.file_map = file_map;
        this.block_map = block_map;
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
            for(DItem it : file_map.values()){
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
                if(!verze.editational){
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
            for(DBlock blok : block_map.values()){
                System.out.println(blok.hash + " : refcount=" + blok.ref_count);
            }
        }        
    }
}

/**
 * Representation of a file for the use by Database
 * @author Lifpa
 */
class DFile implements Serializable, DItem{
    
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
    DFile() {}    
    
    /**
     * Return the future number of consecutive scripts, in case a new version is saved as a script
     * @return 
     */
    int futureNoOfConsecutiveScripts(){
        int count = 1;
        for(int i = version_list.size()-1; i>=0; i--){
            if (!version_list.get(i).editational) {
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
        for(int i = version_list.size()-1; i>=0; i--){
            if (!version_list.get(i).editational) {
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
}


/**
 * Representation of a version of a file for the use by Database, DFile
 * @author Lifpa
 */
class DVersion implements Serializable{
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
    boolean editational;
    
    /**
     * The block size used
     */
    int block_size;
    
    private DVersion(List<DBlock> blocks, Date added, int block_size){
        this.blocks = blocks;
        this.added_date = added;
        editational = false;
        this.block_size = block_size;
    }
    DVersion(List<DBlock> blocks, int block_size){
        this(blocks,new Date(),block_size);
    }    
    DVersion(){}
}


/**
 * Representation of a block for the use by Database and others
 * @author Lifpa
 */
class DBlock implements Serializable{
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
    DBlock(){}
}

interface DItem {
    boolean isDir();
}


/**
 * Representation of a directory for the use by Database and others
 * @author Lifpa
 */
class DDirectory implements Serializable, DItem {

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
    DDirectory(){}
}

class MalformedPath extends Exception {}