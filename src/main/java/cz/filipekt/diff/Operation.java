/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

/**
 *
 * @author filipekt
 */
interface Operation {}

class Delete implements Operation{}
        
        
class Insert implements Operation{
    private final byte data;

    byte getData() {
        return data;
    }
    
    public Insert(byte data) { 
        this.data = data; 
    }            
}
        
class Diagonal implements Operation{
    private final int count;

    int getCount() {
        return count;
    }
    
    public Diagonal(int count) { 
        this.count = count; 
    }                
}
