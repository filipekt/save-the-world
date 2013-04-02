/*
   Copyright 2010 Dmitry Naumenko (dm.naumenko@gmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

/*
 * Changed by Tomas Filipek, 2013.
 * Added:
 *      private static class PatchSerializer extends Serializer<Patch>;
 *      public static Serializer<Patch> getSerializer();
 */

package difflib;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Describes the patch holding all deltas between the original and revised texts.
 * 
 * @author <a href="dm.naumenko@gmail.com">Dmitry Naumenko</a>
 */
public class Patch implements Serializable{
    private List<Delta> deltas = new LinkedList<Delta>();
    
    private static class PatchSerializer extends Serializer<Patch>{

        @Override
        public void write(Kryo kryo, Output output, Patch t) {
            if (t.deltas == null){
                output.writeInt(0);
            } else {
                output.writeInt(t.deltas.size());
                for (Delta d : t.deltas){
                    if (d != null){
                        if (d instanceof ChangeDelta){
                            output.writeInt(0);                        
                            kryo.writeObject(output, d, ChangeDelta.getSerializer());
                        } else if (d instanceof DeleteDelta){
                            output.writeInt(1);
                            kryo.writeObject(output, d, DeleteDelta.getSerializer());
                        } else if (d instanceof InsertDelta){
                            output.writeInt(2);
                            kryo.writeObject(output, d, InsertDelta.getSerializer());
                        } else {
                            output.writeInt(-1);
                        }              
                    }
                }                
            }
        }

        @Override
        public Patch read(Kryo kryo, Input input, Class<Patch> type) {
            int size = input.readInt();
            List<Delta> deltas = new ArrayList<>();
            for (int i = 0; i<size; i++){
                Delta delta;
                int instanceType = input.readInt();
                switch(instanceType){
                    case 0:                        
                        delta = kryo.readObject(input, ChangeDelta.class, ChangeDelta.getSerializer());
                        break;
                    case 1:
                        delta = kryo.readObject(input, DeleteDelta.class, DeleteDelta.getSerializer());
                        break;
                    case 2:
                        delta = kryo.readObject(input, InsertDelta.class, InsertDelta.getSerializer());
                        break;
                    default:
                        delta = null;
                        break;
                }
                deltas.add(delta);
            }
            Patch res = new Patch();
            res.deltas = deltas;
            return res;
        }
        
    }
    
    public static Serializer<Patch> getSerializer(){
        return new PatchSerializer();
    }

    /**
     * Apply this patch to the given target
     * @return the patched text
     * @throws PatchFailedException if can't apply patch
     */
    public List<?> applyTo(List<?> target) throws PatchFailedException {
        List<Object> result = new LinkedList<Object>(target);
        ListIterator<Delta> it = getDeltas().listIterator(deltas.size());
        while (it.hasPrevious()) {
            Delta delta = (Delta) it.previous();
            delta.applyTo(result);
        }
        return result;
    }
    
    /**
     * Restore the text to original. Opposite to applyTo() method.
     * @param target the given target
     * @return the restored text
     */
    public List<?> restore(List<?> target) {
        List<Object> result = new LinkedList<Object>(target);
        ListIterator<Delta> it = getDeltas().listIterator(deltas.size());
        while (it.hasPrevious()) {
            Delta delta = (Delta) it.previous();
            delta.restore(result);
        }
        return result;
    }
    
    /**
     * Add the given delta to this patch
     * @param delta the given delta
     */
    public void addDelta(Delta delta) {
        deltas.add(delta);
    }

    /**
     * Get the list of computed deltas
     * @return the deltas
     */
    public List<Delta> getDeltas() {
        Collections.sort(deltas, DeltaComparator.INSTANCE);
        return deltas;
    }
}
