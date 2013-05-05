package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Tomas Filipek
 */
interface DItem {
    boolean isDir();   
    String getName();
}

/**
 * Representation of a directory for the use by Database and others
 * @author Tomas Filipek
 */
class DDirectory implements DItem {    

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

    Map<String, DItem> getItemMap() {
        synchronized (lockObject){
            return Collections.unmodifiableMap(itemMap);
        }
    }
    
    /**
     * Used for synchronization.
     */
    private final Object lockObject = new Object();
    
    /**
     * Adds a file/directory into this directory.
     * @param item 
     */
    void addItem(DItem item){
        if (item != null){
            synchronized (lockObject){
                itemMap.put(item.getName(), item);
            }
        }
    }
    
    @Override
    public boolean isDir() {
        return true;
    }

    DDirectory(String name) {
        this.name = name;
        this.itemMap = new HashMap<>();
    }
    
    private DDirectory(){}
    
    static Serializer<DDirectory> getSerializer(){
        return new DDirectory.DDirectorySerializer();
    }
    
    private static class DDirectorySerializer extends Serializer<DDirectory> {

        @Override
        public void write(Kryo kryo, Output output, DDirectory t) {
            output.writeString(t.name);
            output.writeInt(t.itemMap.size());
            for(Map.Entry<String,DItem> entry : t.itemMap.entrySet()){
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

        @Override
        public DDirectory read(Kryo kryo, Input input, Class<DDirectory> type) {
            DDirectory res = new DDirectory();
            Map<String,DItem> map = new HashMap<>();
            res.name = input.readString();
            int length = input.readInt();
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

/**
 * Representation of a file for the use by Database
 * @author Tomas Filipek
 */
class DFile implements DItem, Comparable<DFile>{   
    
    /**
     * The name of this file
     */
    private final String name;   
    
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * The list of all present versions
     */
    private final List<DVersion> versionList;
    
    List<DVersion> getVersionList() {        
        synchronized (lockObject){
            return Collections.unmodifiableList(versionList);
        }
    }
    
    /**
     * Adds the specified version to this file.
     * @param version 
     */
    void addVersion(DVersion version){
        if (version != null){
            synchronized (lockObject){
                versionList.add(version);
            }
        }
    }
    
    /**
     * Removes the "index"-th version of this file.
     * @param index 
     */
    void removeVersion(int index){
        synchronized (lockObject){
            if ((versionList.size() > index) && (index >= 0)){
                versionList.remove(index);
            }
        }
    }
    
    /**
     * Removes the specified version of this file.
     * @param version 
     */
    void removeVersion(DVersion version){
        if (version != null){
            synchronized (lockObject){
                if (versionList.contains(version)){
                    versionList.remove(version);
                }
            }
        }
    }
    
    /**
     * Returns the number of existing versions of this file.
     * @return 
     */
    int getVersionCount(){
        synchronized (lockObject){
            return versionList.size();
        }
    }
    
    /**
     * Used for thread synchronization.
     */
    private final Object lockObject = new Object();
    
    /**
     * Complete path to this file in the server database.
     */
    private final List<String> path;    
    
    /**
     * "path" attribute concatenated into a single String.
     * Used mainly for fast comparing of DFile objects.
     */
    private final String path2;

    DFile(List<String> path){
        this(path, new LinkedList<DVersion>());
    }    
    
    private DFile(List<String> path, List<DVersion> versions){
        if ((path == null) || (path.isEmpty()) || (versions == null)){
            throw new NullPointerException("Invalid DFile parameters.");
        }
        this.path = path;
        this.name = path.get(path.size()-1);
        this.versionList = versions;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<path.size(); i++){
            sb.append(path.get(i));
            if (i != path.size()-1){
                sb.append("/");
            }
        }
        this.path2 = sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.path2);
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
        final DFile other = (DFile) obj;
        if (!Objects.equals(this.path2, other.path2)) {
            return false;
        }
        return true;
    }
           
    /**
     * Returns the latest version not saved as a script
     * @return 
     */
    DVersion getLatestNonScript(){        
        synchronized (lockObject){
            for(int i = versionList.size() - 1; i>=0; i--){
                if (!versionList.get(i).isScriptForm()) {
                    return versionList.get(i);
                }
            }   
            return null;        
        }
    }    
    
    /**
     * Returns true if and only if there exists a version of this file which is not saved as a script.
     * @return 
     */
    boolean blockVersionExists(){    
        synchronized (lockObject){
            return (versionList != null) && !versionList.isEmpty();
        }
    }
    
    /**
     * Returns the latest version of the file.
     * @return 
     */
    DVersion getLatestVersion(){   
        synchronized (lockObject){
            if (versionList != null){
                int versionCount = versionList.size();
                if (versionCount > 0){
                    return versionList.get(versionCount-1);
                } else {
                    return null;
                }
            } else {
                return null;
            }             
        }
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
    
    static Serializer<DFile> getSerializer(){
        return new DFile.DFileSerializer();                
    }

    @Override
    public int compareTo(DFile t) {
        return path2.compareTo(t.path2);
    }
                
    private static class DFileSerializer extends Serializer<DFile>{

        @Override
        public void write(Kryo kryo, Output output, DFile t) {               
            if (t.versionList == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.versionList.size());
                for (DVersion dv : t.versionList){
                    kryo.writeObject(output, dv, DVersion.getSerializer());
                }
            } 
            if (t.path == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.path.size());
                for (String s : t.path){
                    output.writeString(s);
                }
            }            
        }

        @Override
        public DFile read(Kryo kryo, Input input, Class<DFile> type) {                      
            List<DVersion> versions = new LinkedList<>();
            int length = input.readInt();            
            for (int i = 0; i<length; i++){
                DVersion item = kryo.readObject(input, DVersion.class, DVersion.getSerializer());
                versions.add(item);
            }
            List<String> path = new LinkedList<>();
            int pathLength = input.readInt();
            for (int i = 0; i<pathLength; i++){
                path.add(input.readString());
            }
            return new DFile(path, versions);                        
        }
        
    }
}