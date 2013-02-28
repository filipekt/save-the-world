/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.filipekt;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;

/**
 *  This class is responsible of creating the GUI for the client-side application.
 * @author filipekt
 */
public class GUIclient {
    public static void main(String args[]){
        final GUIclient gui = new GUIclient();        
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {                
                gui.createAndShowConnectFrame();                
            }
        });
    }
    
    /**
     * The window containing connection options. It is displayed on the application startup.
     */
    private final JFrame connectFrame = new JFrame("Save the World");    
    
    /**
     * The window containing tree-like view of the server file database.
     */
    private final JFrame browserFrame = new JFrame("Save the World");
    
    /**
     * The Client instance associated with this GUI class.
     */
    private Client client;
    
    /**
     * Instantiates the "client" variable.
     * @param comp
     * @param port
     * @throws IOException 
     */
    private void connect(String comp, int port) throws IOException{        
        client = new Client(comp, port, new BufferedReader(new InputStreamReader(System.in)));        
    }
    
    /**
     * The valid locale
     */
    private Locale locale;
    
    /**
     * Contains all the localized messages that are shown to the user.
     */
    private ResourceBundle messages;
    
    public GUIclient(){
        locale = new Locale("en","US");
        messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale);
    }        
    
    /**
     * Creates and shows the window containing connection options.
     */
    private void createAndShowConnectFrame(){
       try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
       } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}               

        connectFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container pane = connectFrame.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        final JLabel compLabel = new JLabel(messages.getString("comp_uri"));
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;        
        pane.add(compLabel,c);
        
        final JTextField compField = new JTextField();
        c.gridx = 1;        
        pane.add(compField,c);        

        final JLabel portLabel = new JLabel(messages.getString("port"));
        c.gridx = 0;
        c.gridy = 1;      
        pane.add(portLabel,c);
        
        final JTextField portField = new JTextField();
        c.gridx = 1;
        c.gridy = 1;      
        pane.add(portField,c);
        
        final JLabel langLabel = new JLabel(messages.getString("localization"));
        c.gridx = 0;
        c.gridy = 2;
        pane.add(langLabel,c);
        
        final JComboBox langBox = new JComboBox(new String[]{"EN","CZ"});        
        c.gridx = 1;
        c.gridy = 2;
        pane.add(langBox,c);
        
        final JButton connectButton = new JButton(messages.getString("connect"));
        c.gridx = 0;
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        connectButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    connect(compField.getText(), Integer.parseInt(portField.getText()));
                    createAndShowBrowserFrame();
                } catch (NumberFormatException | IOException ex) {                    
                    JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
            }
        });
        pane.add(connectButton,c);
        
        final JButton exitButton = new JButton(messages.getString("exit"));
        c.gridx = 1;
        c.gridy = 3;
        exitButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        pane.add(exitButton,c);
        
        // for debugging
        compField.setText("localhost");
        portField.setText("6666");
        
        class LanguageActionListener implements ActionListener{

            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                String selection = (String)cb.getSelectedItem();
                switch(selection){
                    case "EN":
                        locale = new Locale("en","US");                        
                        break;
                    case "CZ":
                        locale = new Locale("cs","CZ");                        
                        break;
                }
                messages = ResourceBundle.getBundle("cz.filipekt.WorldBundle", locale);       
                updateLables();
            }
            
            private void updateLables(){
                compLabel.setText(messages.getString("comp_uri"));
                portLabel.setText(messages.getString("port"));
                langLabel.setText(messages.getString("localization"));
                connectButton.setText(messages.getString("connect"));
                exitButton.setText(messages.getString("exit"));
            }
        }
        langBox.addActionListener(new LanguageActionListener());
        
        connectFrame.setSize(300, 140);     
        connectFrame.setResizable(false);        
        connectFrame.setLocationByPlatform(true);
        connectFrame.setVisible(true);           
    }
    
    
    
    /**
     * The tree visible in the browser frame, its data model is the file database.
     */
    private JTree tree;
    
    /**
     * The root node of the data model of "tree"
     */
    private DefaultMutableTreeNode topNode;
    
    /**
     * Data model used for "tree"
     */
    private DefaultTreeModel treeModel;
    
    /**
     * In case the contents of the server file database changed, </br>
     * calling this method causes the "tree" reload the correct contents.
     */
    private void refreshTree(){
        if ((client != null) && (topNode != null)){
            db = client.getDB();            
            topNode.removeAllChildren();
            createTreeNodes(topNode);  
            treeModel.reload();
            if (browserFrame != null){
                browserFrame.repaint();
            }
        }
    }
    
    /**
     * Creates and shows the window containg the tree-like view of </br>
     * the contents of the server file database.
     */
    private void createAndShowBrowserFrame(){
        connectFrame.setVisible(false);       
        browserFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        browserFrame.addWindowListener(new BrowserWindowListener());
        Container pane = browserFrame.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        topNode = new DefaultMutableTreeNode("root", true);
        createTreeNodes(topNode);    
        treeModel = new DefaultTreeModel(topNode);
        tree = new JTree(treeModel);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(true);
        tree.addTreeSelectionListener(new CustomTreeSelectionListener());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        JScrollPane treeView = new JScrollPane(tree);
        pane.add(treeView,c);
        
        JButton addButton = new JButton(messages.getString("add") + "...");
        addButton.addActionListener(new AddButtonActionListener());
        c.gridx = 0;
        c.gridy = 1;
        c.gridheight = 1;
        c.gridwidth = 1;
        c.weighty = 0;
        pane.add(addButton,c);
        
        JButton getButton = new JButton(messages.getString("get") + "...");
        getButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                createAndShowDownloadFrame(false);
            }
        });
        c.gridx = 1;
        c.gridy = 1;
        pane.add(getButton,c);
        
        JButton refreshButton = new JButton(messages.getString("refresh"));
        refreshButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTree();
            }
        });
        c.gridx = 0;
        c.gridy = 2;
        pane.add(refreshButton,c);
        
        JButton zipButton = new JButton(messages.getString("get_zip") + "...");
        zipButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                createAndShowDownloadFrame(true);
            }
        });
        c.gridx = 1;
        c.gridy = 2;
        pane.add(zipButton,c);
        
        browserFrame.setSize(400, 400);
        browserFrame.setResizable(false);
        browserFrame.setLocationByPlatform(true);
        browserFrame.setVisible(true);       
    }
    
    /**
     * The small window shown when a time consuming operation is started.
     */
    private JFrame progressFrame;
    
    /**
     * Small windows that shows up when a download button is pressed.
     * It gives two options to continue - to download to a home directory
     * or to select a particular destination.
     */
    private JFrame downloadFrame;
    
    /**
     * Creates and shows the window which gives user options, where to
     * download a file - a default or custom location.
     */
    private void createAndShowDownloadFrame(final boolean zip){
        downloadFrame = new JFrame(messages.getString("download_frame"));
        downloadFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        downloadFrame.addWindowListener(new WindowListener() {
            @Override public void windowOpened(WindowEvent we) {}
            @Override public void windowClosing(WindowEvent we) {
                downloadFrame.setVisible(false);
            }            
            @Override public void windowClosed(WindowEvent we) {}            
            @Override public void windowIconified(WindowEvent we) {}
            @Override public void windowDeiconified(WindowEvent we) {}
            @Override public void windowActivated(WindowEvent we) {}
            @Override public void windowDeactivated(WindowEvent we) {}
        });
        Container pane = downloadFrame.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JLabel question = new JLabel(messages.getString("where_download"));
        c.gridheight = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        pane.add(question,c);
        
        JButton default_location = new JButton(messages.getString("default_location"));
        default_location.addActionListener(new GetButtonActionListener(zip, true));
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        pane.add(default_location,c);
        
        JButton custom_location = new JButton(messages.getString("custom_location"));
        custom_location.addActionListener(new GetButtonActionListener(zip,false));
        c.gridx = 1;
        c.gridy = 1;
        pane.add(custom_location,c);
        
        downloadFrame.pack();
        downloadFrame.setLocationByPlatform(true);
        downloadFrame.setVisible(true);
    }
    
    /**
     * Creates and shows the small window indicating a time consuming activity </br>
     * is being processed.
     */
    private void createAndShowProgessFrame(){
        progressFrame = new JFrame(messages.getString("progress"));
        progressFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);        
        Container pane = progressFrame.getContentPane();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        pane.add(progressBar,c);
        
        JLabel infoLabel = new JLabel(messages.getString("working") + "...");
        c.gridx = 0;
        c.gridy = 1;
        pane.add(infoLabel,c);
        
        progressFrame.setSize(300, 80);
        progressFrame.setLocationByPlatform(true);
        progressFrame.setVisible(true);
    }
    
    /**
     * Builds the "tree" according to the contents of the server file database.
     * @param top 
     */
    private void createTreeNodes(DefaultMutableTreeNode top){
        db = client.getDB();        
        Map<String,DItem> fileMap = db.getFileMap();
        createTreeNodes2(top, fileMap);
    }    
    
    /**
     * Determines, what path is selected in the GUIclient view of server files
     */
    private List<TreeNode> selectedPath = null;
    
    /**
     * When a file/directory is selected in the graphical view of server file database,
     * it is projected into the selectedPath variable.
     */    
    private class CustomTreeSelectionListener implements TreeSelectionListener{

        @Override
        public void valueChanged(TreeSelectionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null){
                selectedPath = null;
            } else {
                LinkedList<TreeNode> path = new LinkedList<>();
                path.addFirst(node);
                TreeNode tn = node;
                while((tn=(tn.getParent()))!=null){
                    path.addFirst(tn);
                }
                path.removeFirst();
                selectedPath = path;
            }              
        }
    }
    
    /**
     * When the "get" button is pressed, causes the right actions to be </br>
     * taken and afterwards, takes care of destroying the progressFrame.
     */
    private class GetButtonActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final SwingWorker<Void,Object> worker = new SwingWorker<Void,Object>() {

                        @Override
                        protected Void doInBackground() throws Exception {
                            downloadFrame.setVisible(false);
                            serveGet(zip, default_destination);
                            return null;
                        }
                        
                        @Override
                        public void done() {
                            if (progressFrame != null){
                                progressFrame.setVisible(false);
                            }
                        }
                    };
                    worker.execute();
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                }
            }   
            
            final boolean zip;
            final boolean default_destination;
            
            GetButtonActionListener(boolean zip, boolean default_destination){
                this.zip = zip;
                this.default_destination = default_destination;
            }
    }
    
    /**
     * When the "add" button is pressed, causes the right actions to be </br>
     * taken and afterwards, takes care of destroying the progressFrame.
     */
    private class AddButtonActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    final SwingWorker<Void,Object> worker = new SwingWorker<Void,Object>() {

                        @Override
                        protected Void doInBackground() throws Exception {
                            serveAdd();
                            return null;
                        }
                        
                        @Override
                        public void done() {
                            if (progressFrame != null){
                                progressFrame.setVisible(false);
                            }
                            refreshTree();
                        }
                    };
                    worker.execute();
                    
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
                }
            }        
    }
    
    /**
     * Makes sure that when the browser frame is closed, </br>
     * the connection to the server is terminated properly.
     */
    private class BrowserWindowListener implements WindowListener {

            @Override
            public void windowOpened(WindowEvent e) {}

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    client.terminateConnection();
                    client.end();                    
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(browserFrame, messages.getString("termination_fail"), messages.getString("warning"), JOptionPane.WARNING_MESSAGE);
                } finally {
                    System.exit(0);
                }         
            }

            @Override
            public void windowClosed(WindowEvent e) {}

            @Override
            public void windowIconified(WindowEvent e) {}

            @Override
            public void windowDeiconified(WindowEvent e) {}

            @Override
            public void windowActivated(WindowEvent e) {}

            @Override
            public void windowDeactivated(WindowEvent e) {}        
    }
    
    /**
     * Helper method for the use by createTreeNodes(..)
     * @param node
     * @param content 
     */
    private void createTreeNodes2(DefaultMutableTreeNode node, Map<String,DItem> content){
        if((content == null) || (node == null)){
            return;
        }        
        for(DItem item : content.values()){
            if(item.isDir()){
                DefaultMutableTreeNode child = new DefaultMutableTreeNode(item, true);
                node.add(child);
                createTreeNodes2(child, ((DDirectory)item).getItemMap());
            } else {
                DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(item, true);
                if (((DFile)item).getVersionList() != null){
                    for (DVersion version : ((DFile)item).getVersionList()){
                        fileNode.add(new DefaultMutableTreeNode(version, false));
                    }
                }
                node.add(fileNode);
            }
        }
    }    
    
    /**
     * Builds a typical String representation of the path specified </br>
     * as a list (src) of all the items on the path
     * @param src
     * @return 
     */
   private String constructPath(List<String> src){
        if (src == null){            
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i<src.size(); i++){
            if (i!=0){
                sb.append("/");
            }
            sb.append(src.get(i));
        }        
        return sb.toString();
    }
   
   /**
    * Takes care of the add command
    */
    private void serveAdd(){
        if (selectedPath == null){            
            JOptionPane.showMessageDialog(browserFrame, messages.getString("no_destin_1"), messages.getString("no_destin_2"), JOptionPane.WARNING_MESSAGE);          
            return;
        }  
        final JFileChooser fc = new JFileChooser();
        fc.setLocale(locale);
        fc.setDialogTitle(messages.getString("select_source"));
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int returnVal = fc.showOpenDialog(browserFrame);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    createAndShowProgessFrame();
                }
            });  
            Path source = fc.getSelectedFile().toPath().toAbsolutePath();
            LinkedList<String> selectedPathStrings = new LinkedList<>();
            for (TreeNode tn : selectedPath){
                selectedPathStrings.add(((DefaultMutableTreeNode)tn).getUserObject().toString());
            }
            String lastItem = source.getFileName().toString();
            if (isSelectedPathDir()){
                selectedPathStrings.add(lastItem);
            } else if (isSelectedPathVersion()){
                selectedPathStrings.removeLast();
            }
            String target = constructPath(selectedPathStrings);
            List<String> request = new LinkedList<>();
            request.add("add");
            request.add(source.toString());
            request.add(target);
            try {
                client.serveAdd(request);
                JOptionPane.showMessageDialog(browserFrame, messages.getString("upload"), messages.getString("success"), JOptionPane.PLAIN_MESSAGE);
            } catch (    IOException | NoSuchAlgorithmException ex) {
                JOptionPane.showMessageDialog(browserFrame, messages.getString("upload_error"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Tells whether the user is trying to download a directory into a regular file.
     * @param source
     * @param destination
     * @return 
     */
    private boolean isCompatible(List<TreeNode> source, Path destination){
        Object item = ((DefaultMutableTreeNode)source.get(source.size() - 1)).getUserObject();        
        return  item instanceof DVersion || !((DItem)item).isDir() || Files.isDirectory(destination);            
    }
    
    /**
     * Takes care of the get command
     */
    private void serveGet(final boolean zip, final boolean defaultDestination){    
        if (selectedPath == null){            
            JOptionPane.showMessageDialog(browserFrame, messages.getString("no_file_1"), messages.getString("no_file_2"), JOptionPane.WARNING_MESSAGE);
            return;
        }         
        if (selectedPath.isEmpty()){
            JOptionPane.showMessageDialog(browserFrame, messages.getString("root_disabled"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        Path destination;
        if (defaultDestination){
            destination = Paths.get(System.getProperty("user.home"));
        } else {
            final JFileChooser fc = new JFileChooser();
            fc.setLocale(locale);
            fc.setDialogTitle(messages.getString("select_destination"));
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            int returnVal = fc.showOpenDialog(browserFrame);            
            if (returnVal != JFileChooser.APPROVE_OPTION){
                return;
            }
            destination = fc.getSelectedFile().toPath();
        }
        if (!zip && !isCompatible(selectedPath, destination)){
            JOptionPane.showMessageDialog(browserFrame, messages.getString("dir_into_file"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                createAndShowProgessFrame();
            }
        });                        
        if (!Files.isDirectory(destination)){
            int answer = JOptionPane.showConfirmDialog(browserFrame, messages.getString("replace_1"), messages.getString("replace_2"), JOptionPane.YES_NO_CANCEL_OPTION);
            if (answer != JOptionPane.YES_OPTION){
                return;
            } 
        }
        if (isSelectedPathDir()){                    
            try {
                LinkedList<String> selectedPathStrings = new LinkedList<>();
                for(TreeNode tn : selectedPath){
                    selectedPathStrings.add(((DefaultMutableTreeNode)tn).getUserObject().toString());
                }
                receiveDirectory(selectedPathStrings, destination, zip, true);
                JOptionPane.showMessageDialog(browserFrame, messages.getString("download"), messages.getString("success"), JOptionPane.PLAIN_MESSAGE);
            } catch (FileNotFoundException ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("no_source"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            } catch (IOException | ClassNotFoundException | WrongVersionNumber ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        } else if (isSelectedPathVersion()){
            try {
                LinkedList<String> selectedPathStrings = new LinkedList<>();
                for(TreeNode tn : selectedPath){
                    selectedPathStrings.add(((DefaultMutableTreeNode)tn).getUserObject().toString());
                }
                DVersion version = (DVersion)((DefaultMutableTreeNode)selectedPath.get(selectedPath.size()-1)).getUserObject();
                DFile file = db.findFile(selectedPathStrings.subList(0, selectedPathStrings.size()-1));
                if (file == null) {
                    throw new FileNotFoundException();
                }
                receiveVersion(destination, selectedPathStrings.subList(0, selectedPathStrings.size()-1), file.getVersionList().indexOf(version), zip);
                JOptionPane.showMessageDialog(browserFrame, messages.getString("download"), messages.getString("success"), JOptionPane.PLAIN_MESSAGE);
            } catch (FileNotFoundException ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("no_source"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            } catch (IOException | ClassNotFoundException | WrongVersionNumber ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            }                

        } else { // selected item is a file
            try {
                LinkedList<String> selectedPathStrings = new LinkedList<>();
                for(TreeNode tn : selectedPath){
                    selectedPathStrings.add(((DefaultMutableTreeNode)tn).getUserObject().toString());
                }
                receiveFile(destination, selectedPathStrings, zip);
                JOptionPane.showMessageDialog(browserFrame, messages.getString("download"), messages.getString("success"), JOptionPane.PLAIN_MESSAGE);
            } catch (FileNotFoundException ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("no_source"), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            } catch (IOException | ClassNotFoundException | WrongVersionNumber ex){
                JOptionPane.showMessageDialog(browserFrame, messages.getString("exceptional_state") + " " + ex.getLocalizedMessage(), messages.getString("error"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Given the directory "src" on server and the path "dest" at the client, </br>
     * this method downloads the contents of "src" to "dest"
     * @param src
     * @param dest
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    private void receiveDirectory(List<String> src, Path dest, final boolean zip, final boolean toplevel) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{
        if (zip){
            receiveFile(dest, src, true);
        } else {
            if (toplevel){
                Path dest2 = Paths.get(dest.toAbsolutePath().toString(), src.get(src.size()-1));
                Files.createDirectories(dest2);
                dest = dest2;
            }
            if (db.getItem(src).isDir()){
                DDirectory dir = (DDirectory) db.getItem(src);
                for (Entry<String,DItem> entry : dir.getItemMap().entrySet()){
                    Path dest2 = Paths.get(dest.toAbsolutePath().toString(), entry.getKey());
                    LinkedList<String> src2 = new LinkedList<>();
                    src2.addAll(src);
                    src2.add(entry.getKey());                    
                    if(entry.getValue().isDir()){
                        // create the dir if needed                    
                        if (Files.notExists(dest2)){
                            Files.createDirectories(dest2);
                        }
                        // continue recursively in depth                    
                        receiveDirectory(src2, dest2, false, false);
                    } else {
                        // handle entry as a regular file
                        receiveFile(dest2, src2, false);
                    }
                }
            }            
        }
    }
    
    /**
     * Determines, whether the selected item in the graphical view of server </br>
     * database is a directory.
     * @return 
     */
    private boolean isSelectedPathDir(){   
        if (selectedPath == null){            
            return false;
        } 
        if (selectedPath.isEmpty()){
            return true;
        }
        return ((DefaultMutableTreeNode)selectedPath.get(selectedPath.size()-1)).getUserObject() instanceof DDirectory;
    }
    
    /**
     * Determines, whether the selected item in the graphical view of server </br>
     * database is a version of a file.
     * @return 
     */    
    private boolean isSelectedPathVersion(){
        return ((DefaultMutableTreeNode)selectedPath.get(selectedPath.size()-1)).getUserObject() instanceof DVersion;
    }
    
    /**
     * A local copy of the server file database
     */
    private Database db;
    
    /**
     * Given the file "src" on server and the path "dest" at the client, </br>
     * this method downloads the contents of "src" to "dest", creating the </br>
     * target file if necessary
     * @param dest
     * @param src
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    private void receiveFile(Path dest, List<String> src, final boolean zip) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{  
        if (src == null){
            return;
        } 
        Path dest2;
        if (Files.isDirectory(dest)){
            dest2 = Paths.get(dest.toAbsolutePath().toString(), src.get(src.size()-1) + (zip ? ".zip" : ""));        
        } else {
            dest2 = dest;
        }        
        List<String> request = new LinkedList<>();
        request.add(zip ? "get_zip" : "get");
        request.add(constructPath(src));
        int[] data = client.serveGetBin(request, zip);
        if (data == null){            
            throw new FileNotFoundException();
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(dest2))){
            for(int i : data){
                bos.write(i);
            }
        }        
    }
    
    /**
     * Given the file "sourceFile" on server, it's version "version" and the path "destination" at the client, </br>
     * this method downloads the contents of the version to "destination"
     * @param destination
     * @param sourceFile
     * @param version
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws WrongVersionNumber 
     */
    private void receiveVersion(Path destination, List<String> sourceFile, final int versionNumber, final boolean zip) 
            throws IOException, ClassNotFoundException, WrongVersionNumber{
        if (sourceFile == null){
            return;
        } 
        Path dest2;
        if (Files.isDirectory(destination)){
            String fname = sourceFile.get(sourceFile.size()-1);
            dest2 = Paths.get(destination.toAbsolutePath().toString(), fname + (zip ? ".zip" : ""));
        } else {
            dest2 = destination;
        }        
        List<String> request = new LinkedList<>();
        request.add(zip ? "get_zip" : "get");
        request.add(constructPath(sourceFile));
        request.add(Integer.toString(versionNumber));        
        
        int[] data = client.serveGetBin(request, zip);
        if (data == null){            
            throw new FileNotFoundException();
        }
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(dest2))){
            for(int i : data){
                bos.write(i);
            }
        }           
    }    
}
