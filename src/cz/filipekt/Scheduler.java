package cz.filipekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 *  Class designed for automatic checking of directories and files
 * @author Lifpa
 */
public class Scheduler {
    public static void main(final String[] args) throws InterruptedException{
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Path path = null;
        String target = null;
        int time = Integer.MAX_VALUE;
        boolean ok = false;
        while(!ok){
            System.out.println("Please enter a path:");
            try {
                path = Paths.get(br.readLine());
                if (Files.exists(path)) {
                    ok = true;
                } else {
                    System.out.println("Sorry, the path does not exist.");
                }
            } catch (IOException ex) {
                Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ok = false;
        
        while(!ok){
            System.out.println("Please enter a path on the server:");
            try {
                target = br.readLine();
                ok = true;
            } catch (IOException ex) {
                System.out.println("Sorry, the path cannot be used.");
            }
        }        
        ok = false;
        
        while(!ok){
            System.out.println("Please enter the time interval of checking (in seconds):");
            try{
                time = Integer.parseInt(br.readLine());
                ok = true;
            } catch(IOException | NumberFormatException ex){
                System.out.println("Sorry, you have to enter a positive integer.");
            }
        }
        
        Scheduler pl = new Scheduler(time, path, target);
        try {
            pl.work();
        } catch (IOException ex) {
            Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }        

    Scheduler(int time, Path path, String target) {
        this.time = time;
        this.path = path;
        this.target = target;
    }            

    /**     
     * Target path on the server, where the reqgularly checked "path" is backed up
     */
    String target;
    
    void work() throws IOException, InterruptedException{
        doUpdate();
        WatchService watcher = FileSystems.getDefault().newWatchService();
        WatchKey key;
        path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        while(true){
            key = watcher.poll((long)time, TimeUnit.SECONDS);
            if(key==null){
                System.out.println(Calendar.getInstance().getTime() + " : no changes spotted, no update has been done");
            } else {
                for(WatchEvent<?> event : key.pollEvents()){
                    if(event.kind() == ENTRY_MODIFY){
                        doUpdate();
                        System.out.println(Calendar.getInstance().getTime() + " : updated");
                    }
                }
                key.reset();
            }            
        }
    }
    
    /**
     * Carries out an "add" request
     */
    private void doUpdate(){
       String[] args = new String[7];
       args[0] = "add";
       args[1] = path.toAbsolutePath().toString();
       args[2] = target;
       args[3] = "-p";
       args[4] = Integer.toString(6666);
       args[5] = "-c";
       args[6] = "localhost";
       Client.main(args);
    }        
    
    /**
     * Time interval of checking for changes in "path". In seconds.
     */
    int time;
    
    /**
     * The path monitored for changes
     */
    Path path;    
}
