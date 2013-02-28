package cz.filipekt;

/** 
 *  The enums representing some of the requests sent between a server and a client.
 *  It is a part of the communication protocol.
 * @author Lifpa
 */
public enum Requests {
    /**
     * Client requests a valid instance of Database to be delivered.
     */
    GET_LIST, 
    
    /**
     * Client requests a lightweight version of the server's database.
     */
    GET_LIGHT_DATABASE,
    
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
     * Client requests to create a new directory in the server database
     */
    CREAT_DIR,
    
    DEL_FILE, DEL_VERS, 
    
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
    END
}