package cz.filipekt;

/** 
 *  The enums representing some of the requests sent between a server and a client.
 *  It is a part of the communication protocol.
 * @author Tomas Filipek
 */
public enum Requests {        
    
    /**
     * Client wants to know whether an item is present on the server.
     */
    ITEM_EXISTS,
    
    /**
     * Client requests to create a new DFile in the server database.
     */
    CREAT_FILE, 
    
    /**
     * Client requests to add a new version to a file
     */
    CREAT_VERS, 
    
    /**
     * Client requests the hash value of a file's contents.
     */
    CHECK_CHANGES,
    
    /**
     * Client requests to create a new directory in the server database
     */
    CREAT_DIR,
    
    /**
     * Client requests to delete a version of a file, unless it the last remaining version.
     */
    DEL_VERS, 
    
    /**
     * Client requests a certain file to be delivered
     */
    GET_FILE,
    
    /**
     * Client requests a certain file or directory to be retrieved as a zip archive
     */
    GET_ZIP,
    
    /**
     * Client finishes requesting
     */
    END,
    
    /**
     * Client requests a garbage collection over blocks to be performed on the server.
     */
    GC, 
    
    /**
     * Client requests a DItem object to be sent.
     */
    GET_D_ITEM,
    
    /**
     * Client requests the server database filesystem objects to be delivered.
     */
    GET_FS,
    
    /**
     * Client requests all the server block objects to be delivered.
     */
    GET_SERVER_BLOCKS
}