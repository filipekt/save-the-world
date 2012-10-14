package cz.filipekt;

/** 
 *  The enums representing some of the requests sent between a server and a client.
 *  It is a part of the communication protocol.
 * @author Lifpa
 */
public enum Requests {
    /**
     * Clients requests a valid instance of Databaze to be delivered
     */
    GET_LIST, 
    
    /**
     * Client requests to create a new DSoubor in the server database
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
     * Client finishes requesting
     */
    END
}