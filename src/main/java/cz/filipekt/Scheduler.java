package cz.filipekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 *  Deals with automatization of operations.
 * @author Tomas Filipek
 */
public class Scheduler {    
    public static void main(String[] args){
        Locale locale = new Locale("en","US");
        String localeArgs = ServerUtils.getArgVal(args, "locale", false);
        if (localeArgs!=null){
            if (localeArgs.equalsIgnoreCase("cz")){
                locale = new Locale("cs","CZ");    
            }
        }
        ResourceBundle messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale); 
        
        String uri = ServerUtils.getArgVal(args, "c", false);
        String port = ServerUtils.getArgVal(args, "p", false);
        String time = ServerUtils.getArgVal(args, "time", false);        
        String input = ServerUtils.getArgVal(args, "input", true);
        String count = ServerUtils.getArgVal(args, "count", false);
        
        Path input2 = Paths.get(input);
        if ((uri==null) || uri.isEmpty() || (port==null) || port.isEmpty() ||
                (time==null) || time.isEmpty() || (input==null) || input.isEmpty() || Files.notExists(input2)){
            System.out.println(messages.getString("usage_scheduler"));
            return;
        }
        int port2, time2;
        long count2 = 0;        
        if ((count!=null) && !count.isEmpty()){
            if (ServerUtils.isLong(count)){
                count2 = Long.parseLong(count);
            }
        }
        
        try {
            port2 = Integer.parseInt(port);
            time2 = Integer.parseInt(time);
        } catch (NumberFormatException ex){
            System.out.println(messages.getString("usage_scheduler"));
            return;
        }
        try {
            Scheduler scheduler = new Scheduler(uri, port2, time2, input2, locale, count2);
            scheduler.loadCommands();
            scheduler.work();
        } catch (Exception ex){
            System.out.println(messages.getString("commands_not_loaded"));
        }
    }

    private Scheduler(String uri, int port, int time, Path input, Locale locale, long count) {
        this.uri = uri;
        this.port = port;
        this.time = time;
        this.input = input;
        this.locale = locale;        
        this.commands = new HashSet<>();
        this.count = count;
    }
    
    /**
     * URI of the server to which this schduler connects to.
     */
    private final String uri;
    
    /**
     * Port number used on the server referred to by "uri" variable
     */
    private final int port;
    
    /**
     * Time interval (in seconds), for which at most the scheduler waits before 
     * executing the scheduled code.
     */
    private final int time;
    
    /**
     * Number of executions of the scheduled code before the program shuts down.
     * If zero, the limit on number of executions is not set.
     */
    private final long count;
    
    /**
     * Path to the source file containing instructions that should be periodically
     * executed by this scheduler.
     */
    private final Path input;
    
    /**
     * The current locale.
     */
    private final Locale locale;
       
    /**
     * Conatins all the scheduled commands parsed by whitespace
     */
    private final Collection<List<String>> commands;
    
    /**
     * Connects to the server and executes the scheduled code.
     * @throws IOException 
     */
    private void executeCode() throws IOException{
        Client client = new Client(uri, port, null, locale, System.out, false);
        for (List<String> command : commands){
            client.switchToOperation(command);
        }
        client.terminateConnection();
        client.end();
    }
    
    /**
     * Loads and parses the scheduled commands from the source file "input".
     */
    private void loadCommands() throws IOException{
        try (BufferedReader br = Files.newBufferedReader(input, Charset.defaultCharset())){
            String line;
            while ((line=br.readLine())!=null){
                String[] split1 = line.split(" ");
                List<String> split2 = new ArrayList<>();
                for (String s : split1){
                    if ((s!=null) && !s.isEmpty()){
                        split2.add(s);
                    }
                }
                commands.add(split2);
            }        
        }
    }
    
    /**
     * The main loop executing the scheduled code in the specified intervals.
     */   
    private void work(){
        for (long L = 0; (L < count) || (count==0); L++){
            try {
                Thread.sleep(time * 1000L);
                executeCode();
            } catch (InterruptedException | IOException ex) {
                Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }    
}
