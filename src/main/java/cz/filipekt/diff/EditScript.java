/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt.diff;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author filipekt
 */
public class EditScript {            
    private final List<Operation> operations;
    
    private final int sizeOfB;   

    private EditScript(List<Operation> operations, int sizeOfB) {
        this.operations = operations;
        this.sizeOfB = sizeOfB;
    }
    
    public byte[] applyTo(byte[] src){
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
    
    public static EditScript createScript(byte[] A, byte[] B){
        return createScript(A, B, 0);
    }
    
    public static EditScript createScript(byte[] A, byte[] B, int sizeLimit){
        Myers myers = new Myers(A, B, sizeLimit);
//        List<Vector> snapshots = myers.goThrough();       
//        List<Snake> snakes = myers.reconstructPath(snapshots);
        List<Snake> snakes = myers.compare(0, A.length, 0, B.length);
        if (snakes != null){
            return EditScript.fromSnakes(snakes, A, B);
        } else {
            return null;
        }
    }

    private static EditScript fromSnakes(List<Snake> snakes, byte[] A, byte[] B){        
        List<Operation> ops = new ArrayList<>();                                            
        for (Snake s : snakes){
            Vrchol direction = s.getMid().minus(s.getStart());                    
            if (direction.equals(Vrchol.DOWN)){
                ops.add(new Insert(B[s.getStart().getY()]));
            } else if (direction.equals(Vrchol.RIGHT)){
                ops.add(new Delete());
            }
            Vrchol tail = s.getEnd().minus(s.getMid());
            if (!tail.equals(Vrchol.ZERO)){                                            
                ops.add(new Diagonal(tail.getX()));
            }
        }
        return new EditScript(ops, B.length);
    }
    
    public static Serializer<EditScript> getSerializer(){
        return new EditScriptSerializer();
    }

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
