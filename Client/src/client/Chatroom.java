/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.Font;
import static java.lang.Thread.sleep;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author MuYu
 */
public class Chatroom extends javax.swing.JFrame {
    Socket socket = new Socket();
    BufferedReader reader;
    PrintWriter writer;
    javax.swing.JDialog online_user_dialog = new javax.swing.JDialog();
    javax.swing.JTextArea online_user_textArea = new javax.swing.JTextArea();
    
    
    String chatUser;
    boolean revOnline = false;   
    JFileChooser chooser = new JFileChooser();
    
    public Chatroom() {
        initComponents();
        intiOnline_user();
    }
    public void getAccount(Socket sc, String username, String password) throws IOException {
        socket = sc;
        user_name.setText(username);
        InputStreamReader streamreader = new InputStreamReader(socket.getInputStream());
        reader = new BufferedReader(streamreader);
        writer = new PrintWriter(socket.getOutputStream());
        writer.flush();
        chatUser="";
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        chatUser = username;
        File myprofile = new File("./data/user/"+username);
        myprofile.mkdirs();
    }
    public void listen(){
        Thread listener = new Thread(new ListenHandler());
        listener.start();
    }
    public class ListenHandler implements Runnable{
        @Override
        public void run(){
            try {
                String[] data;
                String op;
                String message;
                String[] filenames;
                String senderName=null;
                String[] recv_file_list = null;
                int recv_file_num = 0;
                while((op = reader.readLine()) != null){
                    if(op.equals("UL")){
                        userList.removeAllItems();
                        int num = Integer.parseInt(reader.readLine());
                        for(int i = 0; i < num; i++){
                            userList.addItem(reader.readLine());
                        }
                    }
                    else if(op.equals("OUSER")){
                        online_user_textArea.setText("");
                        op = reader.readLine();
                        while(!(op.equals("\0"))){
                            online_user_textArea.append(op+"\n");
                            op = reader.readLine();
                        }
                    }
                    else if(op.equals("FILEREQ")) {
                        
                        senderName = reader.readLine();
                        String number = reader.readLine();
                        String filename = "";
                        filenames = new String [Integer.parseInt(number)];
                        for(int i = 0; i < filenames.length; i++){
                            filenames[i] = reader.readLine();
                            filename += "'"+(new File(filenames[i])).getName()+"'" +"\n";
                        }

                        int result=JOptionPane.showConfirmDialog(
                                textArea,"Do you want to receive file(s)\n "+filename+"from "+senderName+"?",
                                "File Receive",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                        //writer.flush();
                        writer.println("FILERES");
                        writer.println(senderName);
                        if (result == JOptionPane.YES_OPTION) {
                            writer.println("yes");
                            recv_file_list = filenames;//.clone();
                            recv_file_num = 0;
                            writer.println(recv_file_list[0]);                            
                        } else if (result == JOptionPane.NO_OPTION) {
                            writer.println("no");
                        } else {}    
                        writer.flush();
                    }
                    else if(op.equals("FILERES")) { 
                        message = reader.readLine();
                        if (message.equals("yes")) {
                            String file_path = reader.readLine();
                            send_file(file_path);
                        }
                    }
                    else if(op.equals("FILESEND")) {
                        textArea.append(senderName+" is sending you file......\n");
                        String filename = reader.readLine();
                        String len = reader.readLine();

                        File file = new File("./data/user/"+user_name.getText()+"/"+filename);
                        file.createNewFile();
                        textArea.append("File '"+filename+"' "+ len+"bytes\n");
                        FileOutputStream output = new FileOutputStream(file, false);
                        DataInputStream in = new DataInputStream(socket.getInputStream());
                        byte[] buffer = new byte[4096];
                        int bytesRead =0, current=0;
                        int length = Integer.parseInt(len);
                        int fileleft = length;
                        while ((bytesRead = in.read(buffer,0,Math.min(4096,fileleft))) != -1 ) {

                            output.write(buffer, 0, bytesRead);
                            current+=bytesRead;
                            fileleft -= bytesRead;
                            if(current >= length) break;
                        }
                        output.close();
                        textArea.append("downloaded (" + current+" bytes read).\n");

                        recv_file_num++;
                        if(recv_file_num < recv_file_list.length){
                            writer.println("FILERES");
                            writer.println(senderName);
                            writer.println("yes");
                            writer.println(recv_file_list[recv_file_num]);
                            writer.flush();
                        }
                    }
                    else if(op.equals("OLD")) {
                        message = reader.readLine();
                        while (!(message.equals("\0"))) {
                            textArea.append(message+'\n');
                            message = reader.readLine();
                        }
                    }
                    else if(op.equals("MES")) {
                        senderName = reader.readLine();
                        message = reader.readLine();
                        if (chatUser.equals(senderName)) {
                            textArea.append(message+'\n');
                        }
                    }
                    else if(op.equals("STATUS")) {
                        message = reader.readLine();
                        set_status(message);
                    }
                    else {
                        
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void intiOnline_user(){        
        online_user_dialog.setSize(150,400);
        online_user_dialog.setResizable(false);
        online_user_textArea.setSize(150,400);
        online_user_textArea.setEditable(false);
        online_user_dialog.setResizable(false);
        online_user_dialog.add(online_user_textArea);
        online_user_dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        online_user_textArea.setFont(new Font("Monospaced", Font.BOLD, 24));
        online_user_dialog.setTitle("Online User List");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jDialog1 = new javax.swing.JDialog();
        jFileChooser1 = new javax.swing.JFileChooser();
        user_name = new javax.swing.JLabel();
        Hello = new javax.swing.JLabel();
        scrollPane = new javax.swing.JScrollPane();
        textArea = new javax.swing.JTextArea();
        onlineUserList = new javax.swing.JButton();
        logout = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        fileToSend = new javax.swing.JTextField();
        chooseFile = new javax.swing.JButton();
        sendFile = new javax.swing.JButton();
        userList = new javax.swing.JComboBox<>();
        changeUser = new javax.swing.JButton();
        textInput = new javax.swing.JTextField();
        send = new javax.swing.JButton();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        user_name.setText("User");

        Hello.setText("Hello, ");

        textArea.setEditable(false);
        textArea.setColumns(20);
        textArea.setRows(5);
        scrollPane.setViewportView(textArea);

        onlineUserList.setText("Online User on/off");
        onlineUserList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onlineUserListActionPerformed(evt);
            }
        });

        logout.setText("Logout");
        logout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoutActionPerformed(evt);
            }
        });

        chooseFile.setText("Choose File");
        chooseFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chooseFileActionPerformed(evt);
            }
        });

        sendFile.setText("Send File");
        sendFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(fileToSend, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chooseFile, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(sendFile, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileToSend, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(chooseFile)
                    .addComponent(sendFile))
                .addGap(0, 27, Short.MAX_VALUE))
        );

        changeUser.setText("go");
        changeUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeUserActionPerformed(evt);
            }
        });

        send.setText("Send");
        send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addComponent(onlineUserList)
                        .addGap(18, 18, 18)
                        .addComponent(logout, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 429, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(textInput, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(send, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(Hello, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(432, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(user_name, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(userList, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(changeUser, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(44, 44, 44))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(Hello)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(user_name, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(userList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(changeUser))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(send)
                        .addGap(2, 2, 2)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(textInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(onlineUserList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(logout))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    public String message_replace(String msg){
        //String[] ret = new String[2];
        msg = msg.replaceAll("fuck","****");
        msg = msg.replaceAll("幹","*");
        msg = msg.replaceAll("幹你媽","***");
        msg = msg.replaceAll("幹你娘","***");
        msg = msg.replaceAll("他媽的","***");
        msg = msg.replaceAll("motherfucker","************");
        msg = msg.replaceAll("你好","你怎麼不去死");
        
        return msg;
    }
    private void sendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendActionPerformed
        // Send text
        writer.println("TEXT");                           
        String messag = textInput.getText();
        String message = message_replace(messag);
        String username = user_name.getText();
        writer.println(message);
        writer.flush();
        textInput.setText("");
        textArea.append(username+": "+message+'\n');
    }//GEN-LAST:event_sendActionPerformed

    private void chooseFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chooseFileActionPerformed
        // Choose File
        if (!revOnline) {
            JOptionPane.showMessageDialog(null, "You can't send file to a OFFLINE user.");
        }
        else {
            //chooser.setDragEnabled(true);
            chooser.setMultiSelectionEnabled(true);
            chooser.showOpenDialog(null);
            File[] file = chooser.getSelectedFiles();
            String filename="";
            for(int i = 0; i < file.length; i++){
                filename += "\""+file[i].getAbsolutePath()+"\"";
            }
            fileToSend.setText(filename);
        }
    }//GEN-LAST:event_chooseFileActionPerformed

    private void onlineUserListActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onlineUserListActionPerformed
        // See online Users List
        online_user_dialog.setLocation((this.getLocationOnScreen().x + 516),this.getLocationOnScreen().y);
        if(online_user_dialog.isVisible()){            
            online_user_dialog.setVisible(false);
        }
        else{
            writer.println("OUSER");
            writer.flush(); 
            online_user_dialog.setVisible(true);
        }
    }//GEN-LAST:event_onlineUserListActionPerformed

    private void logoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoutActionPerformed
        try {
            // Logout
            online_user_dialog.setVisible(false);
            writer.println("DIS");
            writer.flush();
            writer.close();
            reader.close();
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
        }

        JOptionPane.showMessageDialog(null, "Disconnected successfully.");
        this.dispose();
        new Client().setVisible(true);
    }//GEN-LAST:event_logoutActionPerformed

    private void changeUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeUserActionPerformed
        textArea.setText("");
        Object user = userList.getSelectedItem();
        chatUser = user.toString();
        writer.println("SEL");
        writer.println(chatUser);
        writer.flush(); 
    }//GEN-LAST:event_changeUserActionPerformed
    public void set_status(String status) {
        if (status.equals("online")) {
            revOnline = true;
        } else {
            JOptionPane.showMessageDialog(null, status);
            revOnline = false;
        }
    }
    
    private void sendFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendFileActionPerformed
        if (!revOnline || chatUser.equals(user_name.getText())) {
            JOptionPane.showMessageDialog(textArea, "You can't send file to\n1.yourself\n2.a OFFLINE user.");
        }
        else {
            File[] file = chooser.getSelectedFiles();
            writer.println("FILEREQ");
            writer.println(Integer.toString(file.length));
            for(int i = 0; i < file.length;i++){
                try {
                    writer.println(file[i].getCanonicalPath());
                } catch (IOException ex) {
                    Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
                }
            }          
            writer.flush();    
        }
    }//GEN-LAST:event_sendFileActionPerformed

    public void send_file(String path) throws IOException, InterruptedException {
        writer.println("FILESEND");
        //File file = chooser.getSelectedFile();
        String filename = null;
        try {
            File file = new File(path);
            filename = file.getName();
            writer.println(filename);
            Long len = file.length();            
            writer.println(len.toString());
            writer.flush();

            byte[] buffer = new byte[4096];
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(file);
            int count = 0;
            int current;
            sleep(100);
            while ((current = fis.read(buffer) ) != -1) {
                dos.write(buffer,0,current);
                count+=current;
                //textArea.append(count+" bytes transfer..."+len+"\n");
            }
            textArea.append("file:"+filename+", "+count+" bytes transfer...\n");
            fis.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Chatroom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
//    public void receive_file() throws IOException {
//
//    }
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Chatroom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Chatroom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Chatroom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Chatroom.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Chatroom().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Hello;
    private javax.swing.JButton changeUser;
    private javax.swing.JButton chooseFile;
    private javax.swing.JTextField fileToSend;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JButton logout;
    private javax.swing.JButton onlineUserList;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JButton send;
    private javax.swing.JButton sendFile;
    private javax.swing.JTextArea textArea;
    private javax.swing.JTextField textInput;
    private javax.swing.JComboBox<String> userList;
    private javax.swing.JLabel user_name;
    // End of variables declaration//GEN-END:variables
}
