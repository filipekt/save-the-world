package cz.filipekt.diff;

/**
 * Represents a single operation on array A that would change it to be more
 * similar to array B.
 * @author Tomas Filipek
 */
interface Operation {}

/**
 * A single byte deletion from array A.
 * @author Tomas Filipek
 */
class Delete implements Operation{}
        
/**
 * A single byte insertion into array A.
 * @author Tomas Filipek
 */        
class Insert implements Operation{
    private final byte data;

    byte getData() {
        return data;
    }
    
    Insert(byte data) { 
        this.data = data; 
    }            
}
      
/**
 * Represents a move along a diagonal in the edit graph.
 * That means, the following "count" bytes are identical in array A and B.
 * @author filipekt
 */
class Diagonal implements Operation{
    private final int count;

    int getCount() {
        return count;
    }
    
    Diagonal(int count) { 
        this.count = count; 
    }                
}
