/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.TreeMap;


/**
 *  Trida pro testovaci ucely
 * @author Lifpa
 */
public class Test {        
    
    public static void main(String[] args) throws Exception {         
        Kryo kryo = new Kryo();
        Output out = new Output(new FileOutputStream("E:/pers.txt"));
        Database bi = new Database(new HashMap<String,DItem>(), new TreeMap<String,DBlock>());
        kryo.writeObject(out, bi);
        out.close();
        Input in = new Input(new FileInputStream("E:/pers.txt"));
        Database bi2 = kryo.readObject(in, Database.class);
        in.close();
        System.out.println(bi2.block_map.size());        
        
    }
}
