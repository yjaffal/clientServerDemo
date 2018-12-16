/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserverdemo;


import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 *  This class represents a client that sends messages to its own server
 * in order to be broadcasted to all servers
 * @author Yasser Jaffal
 */

public class SendingClient implements ActionListener {

    protected int port;
    protected DatagramSocket serverSocket;
    protected JTextField inputField;
    protected String sendGroup;
    protected ClientManager manager;
    protected String name;
    protected Message clientMessage;
    protected DatagramPacket toServer = null;
    protected byte[] buffer = new byte[OutgoingServer.BUFFER_SIZE];

   /**
    * Constructor
    * @param port communication port between this client and its server
    * @param name the name of this client (also applies to its server), must be unique
    * @param sendGroup IP address through which server and client communicate
    * @param manager reference to ClientManager object from which this client receives commands to send messages
    */
    public SendingClient(int port, String name, String sendGroup, ClientManager manager) {
        this.name = name;
        this.manager = manager;
        this.port = port;
        this.sendGroup = sendGroup;
        inputField = manager.getInputField();

        try {
            serverSocket = new DatagramSocket();
            try {
                toServer = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName(sendGroup), port);

                String initMessage = "";
                clientMessage = new Message(getName(),
                        Message.ALL,
                        Message.SYNC,
                        Message.COMMAND);
                initMessage = clientMessage.toString();
                buffer = initMessage.getBytes();
                toServer.setData(buffer, 0, buffer.length);
                System.out.println(getName() + "-->" + initMessage);
                serverSocket.send(toServer);

            } catch (UnknownHostException err) {
                System.err.println(err.getMessage());
                err.printStackTrace();
            }

        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * @return name of this client
     */
    public String getName() {
        return name;
    }

    /**
     * @param inField reference to JTextField object where user types messages
     */
    public void setInputField(JTextField inField) {
        this.inputField = inField;
    }

    /**
     * sends Message to the server
     * @param m a Message object containing message to send
     * @throws IOException if error occures while sending message to server
     */
    void sendMessage(Message m) throws IOException {
        String msg = m.toString();

        buffer = msg.getBytes("Windows-1256");
        toServer = new DatagramPacket(buffer, buffer.length,
                InetAddress.getByName(sendGroup), port);
        if (m.getType() == Message.DATA) {
            manager.mManager.addMessage(m);
        }

        System.out.println(getName() + "-->" + m);
        serverSocket.send(toServer);
    }

    /**
     * handles action (usually from a ClientManager object) to send message to server
     * @param e
     */
    public void actionPerformed(ActionEvent e) {

        if (inputField.getText().trim().length() == 0) {
            return;
        }
        try {

            String input = inputField.getText();
            
            inputField.setText("");
            clientMessage = new Message(getName(), Message.ALL, input, Message.DATA);
            clientMessage.setID(manager.getNextMessageID());
            //JOptionPane.showMessageDialog(null, input);
            //JOptionPane.showMessageDialog(null, clientMessage.getContent());
            sendMessage(clientMessage);

        } catch (IOException err) {
            System.err.println(err.getMessage());
            err.printStackTrace();
        }
    }

    /**
     * disposes this object
     */
    public void dispose() {
        serverSocket.close();
    }
}
