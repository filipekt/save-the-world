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
 *      private static class DeleteDeltaSerializer extends Serializer<DeleteDelta>;
 *      public static Serializer<DeleteDelta> getSerializer();
 */

package difflib;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Serializable;
import java.util.List;

/**
 * Describes the delete-delta between original and revised texts.
 * 
 * @author <a href="dm.naumenko@gmail.com">Dmitry Naumenko</a>
 */
public class DeleteDelta extends Delta implements Serializable{
    
    /**
     * {@inheritDoc}
     */
    public DeleteDelta(Chunk original, Chunk revised) {
        super(original, revised);
    }
    
    private static class DeleteDeltaSerializer extends Serializer<DeleteDelta>{

        @Override
        public void write(Kryo kryo, Output output, DeleteDelta t) {
            kryo.writeObject(output, t.original, Chunk.getSerializer());
            kryo.writeObject(output, t.revised, Chunk.getSerializer());
        }

        @Override
        public DeleteDelta read(Kryo kryo, Input input, Class<DeleteDelta> type) {
            Chunk a = kryo.readObject(input, Chunk.class, Chunk.getSerializer());
            Chunk b = kryo.readObject(input, Chunk.class, Chunk.getSerializer());       
            return new DeleteDelta(a, b);
        }        
    }
    
    public static Serializer<DeleteDelta> getSerializer(){
        return new DeleteDeltaSerializer();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws PatchFailedException
     */
    @Override
    public void applyTo(List<Object> target) throws PatchFailedException {
        verify(target);
        int position = getOriginal().getPosition();
        int size = getOriginal().size();
        for (int i = 0; i < size; i++) {
            target.remove(position);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void restore(List<Object> target) {
        int position = this.getRevised().getPosition();
        List<?> lines = this.getOriginal().getLines();
        for (int i = 0; i < lines.size(); i++) {
            target.add(position + i, lines.get(i));
        }
    }
    
    @Override
    public TYPE getType() {
        return Delta.TYPE.DELETE;
    }
    
    @Override
    public void verify(List<?> target) throws PatchFailedException {
        getOriginal().verify(target);
    }
    
    @Override
    public String toString() {
        return "[DeleteDelta, position: " + getOriginal().getPosition() + ", lines: "
                + getOriginal().getLines() + "]";
    }
}
