package cz.filipekt.diff;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a patch containing the operations needed to change
 * a file A into file B.
 * @author filipekt
 */
public class EditScript {     
    
    /**
     * Sorted sequence of operations that would turn file A into file B.
     */
    private final List<Operation> operations;     
    
    /**
     * Size of the file B , in bytes.
     */
    private final int sizeOfB;   

    private EditScript(List<Operation> operations, int sizeOfB) {
        this.operations = operations;
        this.sizeOfB = sizeOfB;
    }
    
    /**
     * Applies the operations contained in this EditScript to the byte array 
     * specified in parameters (== contents of file A). The result is the 
     * contents of file B.
     * @param src Contents of file A
     * @return Contents of file B
     */
    public byte[] applyTo(byte[] src){
        if ((src == null) || (operations == null)){
            return null;
        }
        byte[] res = new byte[sizeOfB];
        int isrc = 0;
        int ires = 0;
        for (Operation o : operations){
            if (o instanceof Delete){                
                isrc++;
            } else if (o instanceof Insert){
                Insert ins = (Insert)o;
                res[ires++] = ins.getData();
            } else if (o instanceof Diagonal){
                Diagonal dia = (Diagonal) o;
                for (int i = 0; i<dia.getCount(); i++){
                    res[ires++] = src[isrc++];
                }
            }
        }
        return res;
    }           
    
    /**
     * Compares two input byte arrays and returns an edit script, not neccesarilly
     * minimal, but close. The edit script contains instructions that transform 
     * array A into array B. If the number of instructions in the script is greater
     * than "sizeLimit", no script is created and null is returned.
     * @param A The script will contain instructions operating on this array.
     * @param B This array will be the result of applying the script to array A.
     * @param sizeLimit If greater than 0, a script longer than this non-zero value is never 
     * returned. Length of a script means the number of edit operations conatined in the script.
     * @param heuristics If true, heuristics may be used and the returned script is not guaranteed to be 
     * minimal. However, the computation runs considerably faster.
     * @return The edit script describing how array B differs from array A.
     */
    public static EditScript createScript(byte[] A, byte[] B, int sizeLimit, boolean heuristics){
        if ((A == null) || (B == null) || (sizeLimit < 0)){
            return null;
        }
        Myers myers = new Myers(A, B, sizeLimit);
        List<Operation> snakes = myers.compare(heuristics);
        if ((snakes == null) || ((sizeLimit > 0) && (snakes.size() > sizeLimit))){
            return null;
        }       
        return new EditScript(snakes, B.length);                      
    }         
    
    /**
     * Used for serialization by the Kryo framework.
     * @return 
     */
    public static Serializer<EditScript> getSerializer(){        
        return new EditScriptSerializer();
    }

    /**
     * Used for serialization by the Kryo framework.
     */
    private static class EditScriptSerializer extends Serializer<EditScript>{

        @Override
        public void write(Kryo kryo, Output output, EditScript t) {
            output.writeInt(t.sizeOfB);
            if (t.operations == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.operations.size());
                for (Operation o : t.operations){
                    if (o instanceof Delete){
                        output.writeByte((byte)1);
                    } else if (o instanceof Insert){
                        output.writeByte((byte)2);
                        output.writeByte(((Insert)o).getData());
                    } else if (o instanceof Diagonal){
                        output.writeByte((byte)3);
                        output.writeInt(((Diagonal)o).getCount());
                    } else {
                        output.writeByte((byte)0);
                    }
                }                        
            }
        }

        @Override
        public EditScript read(Kryo kryo, Input input, Class<EditScript> type) {
            int bsize = input.readInt();
            int opCount = input.readInt();
            List<Operation> res = new ArrayList<>();
            for (int i = 0; i<opCount; i++){
                byte operation = input.readByte();
                switch(operation){                    
                    case (byte)1:
                        res.add(new Delete());
                        break;
                    case (byte)2:
                        byte data = input.readByte();
                        res.add(new Insert(data));
                        break;
                    case (byte)3:
                        int count = input.readInt();
                        res.add(new Diagonal(count));
                        break;
                    default:
                        break;
                }
            }
            return new EditScript(res, bsize);
        }
    }
}
