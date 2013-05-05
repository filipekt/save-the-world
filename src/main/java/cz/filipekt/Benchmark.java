package cz.filipekt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import static java.nio.file.StandardOpenOption.*;

/**
 * Testing the application performace.
 * Beware, JVM warmup MUST be done BEFORE running these tests.
 * @author Tomas Filipek
 */
public class Benchmark {
    public static void main(String[] args) throws IOException{      
        List<String> args2 = ServerUtils.concatQuotes(Arrays.asList(args));
        String outputPath = args2.get(0);
        String type = args2.get(1);
        if (type != null){
            if (type.equalsIgnoreCase("add")){
                String filename = args2.get(2);
                String target = null;
                if (args2.size() >= 4){
                    target = args2.get(3);
                } 
                Benchmark bm = new Benchmark(outputPath);
                bm.measureAdd(filename,target);
                bm.close();
            } else if (type.equalsIgnoreCase("get")){
                String filename = args2.get(2);
                String target = args2.get(3);
                Integer version = null;
                if (args2.size() >= 5){
                    version = Integer.valueOf(args2.get(4));
                }
                Benchmark bm = new Benchmark(outputPath);
                bm.measureGet(filename, target, version, false);
                bm.close();
            } else if (type.equalsIgnoreCase("get_zip")){
                String filename = args2.get(2);
                String target = args2.get(3);
                Integer version = null;
                if (args2.size() >= 5){
                    version = Integer.valueOf(args2.get(4));
                }
                Benchmark bm = new Benchmark(outputPath);
                bm.measureGet(filename, target, version, true);
                bm.close();
            } else if (type.equalsIgnoreCase("delete")){
                String path = args2.get(2);
                int version = Integer.parseInt(args2.get(3));
                Benchmark bm = new Benchmark(outputPath);
                bm.measureDelete(path, version);
                bm.close();
            }
        }
    }
    
    private final String uri = "localhost";
    private final int port = 6666;
    private final Locale locale = new Locale("en","US");    

    Benchmark(String outputPath) throws IOException {
        OutputStream os = Files.newOutputStream(Paths.get(outputPath), APPEND, CREATE); 
        pw = new PrintWriter(os);        
    }
    
    private final PrintWriter pw;
    
    void close(){
        pw.close();
    }
    
    private void measureAdd(String fname, String target) throws IOException{        
        Client client = new Client(uri, port, null, locale, System.out, false);        
        List<String> command = new LinkedList<>();
        command.add("add");
        command.add(fname);
        if ((target != null) && !target.isEmpty()){
            command.add(target);
        }
        long before = System.currentTimeMillis();
        client.switchToOperation(command);        
        long after = System.currentTimeMillis();

        long elapsed = after - before;
        client.terminateConnection();
        client.end();
        pw.println(elapsed);                
    }
    
    private void measureGet(String fname, String target, Integer version, boolean zip) throws IOException{       
        Client client = new Client(uri, port, null, locale, System.out, false);        
        List<String> command = new LinkedList<>();
        if (zip){
            command.add("get_zip");
        } else {
            command.add("get");
        }
        command.add(fname);
        command.add(target);
        if (version != null){
            command.add(Integer.toString(version));
        }
        long before = System.currentTimeMillis();
        client.switchToOperation(command);        
        long after = System.currentTimeMillis();

        long elapsed = after - before;
        client.terminateConnection();
        client.end();
        pw.println(elapsed);        
    }
    
    private void measureDelete(String path, int version) throws IOException{
        Client client = new Client(uri, port, null, locale, System.out, false);        
        List<String> command = new LinkedList<>();
        command.add("delete");
        command.add(path);
        command.add(Integer.toString(version));
        
        long before = System.nanoTime();
        client.switchToOperation(command);        
        long after = System.nanoTime();

        long elapsed = after - before;
        elapsed /= 1000;
        client.terminateConnection();
        client.end();
        pw.println(elapsed);
    }
}
