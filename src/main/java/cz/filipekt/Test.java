/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt;

import cz.filipekt.diff.EditScript;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

/**
 * Just for testing purposes.
 * @author Tomas Filipek
 */
class Test {
    
    public static final int size = 20000;
    public static final int size2 = 10000;        
   
    public static void main (String[] args) throws IOException{                
        similarFiles();
    }
    
    private static void manySmallFiles() throws IOException{
        for (int i = 0; i< 1000; i++){
            try (OutputStream os = Files.newOutputStream(Paths.get("D:/bm/file" + i)); PrintWriter pw = new PrintWriter(os)){
                int val = (i * i) % Integer.MAX_VALUE;
                pw.append(Integer.toString(val));
            }
        }
    }
    
    private static void fewBigFiles() throws IOException{
        for (int i = 0; i< 1; i++){
            try (OutputStream os = Files.newOutputStream(Paths.get("D:/bm2/file" + i)); PrintWriter pw = new PrintWriter(os)){
                int val = (i * i) % Integer.MAX_VALUE;
                for (int j = 0; j<10_000_000; j++){
                    pw.append(Integer.toString(val));
                }
            }
        }
    }
    
    private static void similarFiles() throws IOException{
        Random rnd = new Random(910124L);        
        int count = 0;
        for (int i = 0; i<5; i++){
            try (OutputStream os = Files.newOutputStream(Paths.get("D:/similar/file" + count++))){
                os.write(new byte[100_000]);
                byte[] data = new byte[500];
                rnd.nextBytes(data);
                os.write(data);                
            }
        }
        for (int i = 0; i<5; i++){
            try (OutputStream os = Files.newOutputStream(Paths.get("D:/similar/file" + count++))){
                os.write(new byte[100_000]);              
                byte[] data = new byte[4_000];
                rnd.nextBytes(data);
                os.write(data);                           
            }
        }
        for (int i = 0; i<5; i++){
            try (OutputStream os = Files.newOutputStream(Paths.get("D:/similar/file" + count++))){
                os.write(new byte[100_000]);     
                byte[] data = new byte[30_000];
                rnd.nextBytes(data);
                os.write(data);                                
            }
        }
        for (int i = 0; i<15; i++){
            System.out.println("java -cp ../target/save-the-world.jar cz.filipekt.Benchmark %output_file% " + 
                    "add %test_dir%/similar/file" + i + " similar");
        }
    }
    
    public static void testdiff (String[] args){
        byte[] a = new byte[size];       
        for (int i = 0; i<size; i++){
            a[i] = (byte) (1);
        }
        
        byte[] b = new byte[size2];
        for (int i = 0; i<size2; i++){
            b[i] = (byte) (i);
        }   
//        b = a;
        printarray(a, 50);        
        printarray(b, 50);
        EditScript es = EditScript.createScript(a, b, 66000, true, 1024);
        if (es != null){
            printarray(es.applyTo(a), 50);
        } else {
            System.out.println("Skript nevytvoren.");
        }                       
    } 
    
    private static void printarray(byte[] src, int count){
        if (src == null){
            return;
        }
        int limit = (src.length > count) ? count : src.length;
        for (int i = 0; i<limit; i++){
            System.out.print(src[i] + " ");
        }
        System.out.println();
    }
}
