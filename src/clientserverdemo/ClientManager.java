package clientserverdemo;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.DatagramSocket;
import java.util.TreeSet;
import javax.swing.*;

/**
 * A GUI interface that allows user to send and receive messages
 * @author Yasser Jaffal
 */
public class ClientManager extends JFrame implements
        ActionListener, KeyListener, Comparable<ClientManager> {

    private static final int DELAY = 3000;
    private static final int TRIES = 3;
    private OutgoingServer outgoing;
    private IncomingServer incoming;
    private SendingClient client;
    private ReceivingClient rClient;
    private JTextField inputField;
    private JButton send;
    private JTextArea history;
    MessageManager mManager;
    private javax.swing.Timer t;
    private Main parent;
    private TreeSet<Message> messageHistory;
    private static final int BASE_PORT = 1025;

    /**
     * Constructor
     * @param name the name to be used for Client, IncomingServer and
     * OutgoingServer threads (also identifies this client globally), must be unique
     * @param group IP group to be used for client-server communications
     * @param parent A reference to a Main object to which this ClientManager will write logs
     */
    public ClientManager(String name, String group, Main parent) {
        setName(name);
        setParent(parent);
        String userHome = System.getProperty("user.home");
        try {
            ObjectInputStream in = new ObjectInputStream(
                    new FileInputStream(userHome + "/" + getName() + ".csdemo"));
            mManager = (MessageManager) in.readObject();
        } catch (FileNotFoundException err) {
            mManager = new MessageManager();
        } catch (IOException err) {
            log("WARNING: unable to get missed messages");
            mManager = new MessageManager();
        } catch (ClassNotFoundException err) {
            err.printStackTrace();
            System.exit(1);
        }

        int port1 = getNextPort(BASE_PORT - 1);
        int port2 = getNextPort(port1);
        inputField = new JTextField(20);
        inputField.addKeyListener(this);
        history = new JTextArea(20, 20);
        history.setEditable(false);
        send = new JButton("Send");
        incoming = new IncomingServer(1024, port1, name, "225.0.0.1", group);
        outgoing = new OutgoingServer(port2, 1024, name, group, "225.0.0.1");

        rClient = new ReceivingClient(port1, name, group, this);

        incoming.start();
        log("incoming server started");
        outgoing.start();
        log("outgoing server started");
        rClient.start();
        log("receiving client started");

        client = new SendingClient(port2, name, group, this);
        send.addActionListener(client);
        t = new Timer(DELAY, this);

        setSize(330, 220);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel p = new JPanel();
        p.add(inputField);
        p.add(send);
        getContentPane().add(p, BorderLayout.SOUTH);
        JScrollPane sp = new JScrollPane(history);
        getContentPane().add(sp, BorderLayout.CENTER);

        messageHistory = new TreeSet<Message>();

        setTitle(name);
        setResizable(false);
        t.start();
        setVisible(true);
    }

    /**
     *
     * @return a unique integer to be used as message ID
     */
    public int getNextMessageID() {
        return mManager.nextMessageID++;
    }

    /**
     * Handles window closing event to perform thread stopping and state saving
     * tasks
     * @param e
     */
    

    @Override
    public void setVisible(boolean visible){
        super.setVisible(visible);
        if(!visible)
            dispose();
    }

    @Override
    public void dispose(){
        rClient.deactivate();
        log("receiving client stopped");
        incoming.deactivate();
        log("incoming server stopped");
        outgoing.deactivate();
        log("outgoing server stopped");
        client.dispose();
        log("client destroyed");
        t.stop();
        t = null;
        getParent().clients.remove(this);
        saveStatus();
        super.dispose();
    }

    /**
     * same as actionPerformed()
     * @param e
     */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER &&
                inputField.getText().trim().length() > 0) {
            client.actionPerformed(null);
        }
    }

    /**
     * handles acknowledgement message from remote peer
     * @param ack
     */
    void acknowledgeMessage(Message ack) {
        if (ack.getType() == Message.ACKNOWLEDGE &&
                !ack.getSource().equals(getName())) {
            mManager.acknowledge(ack.getSource(), ack.getID());
            mManager.resetTries(ack.getSource());
            log("acknowledged message " + ack.getID() + " from " + ack.getSource());
        }
    }

    /**
     * Adds new message to history and mdifies messages order if necessary
     * @param msg
     */
    void addMessage(Message msg) {
        if(messageHistory.isEmpty() ||
                messageHistory.last().getCreationDate() <
                msg.getCreationDate()){
            messageHistory.add(msg);
            history.append(msg.getSource() + ": " + msg.getContent() + "\n");
        } else {
            messageHistory.add(msg);
            history.setText("");
            for(Message m : messageHistory){
                history.append(m.getSource() + ": " + m.getContent() + "\n");
            }
        }
        history.setCaretPosition(history.getText().length());
    }

    /**
     * Saves missed messages information to hard drive before shutting down
     */
    void saveStatus() {
        String userDir = System.getProperty("user.home");
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(userDir + "/" + getName() + ".csdemo"));
            out.writeObject(mManager);
            out.close();
        } catch (Exception err) {
            log(" WARNING: unable to save missed messages");
        }
    }

    /**
     * Acknowledges an incoming message by sendign acknowledgement
     * message to the source of the received message
     * @param clientMessage the incoming message
     */
    void sendAck(Message clientMessage) {
        clientMessage.setType(Message.ACKNOWLEDGE);
        clientMessage.setDestination(clientMessage.getSource());
        clientMessage.setSource(getName());
        try {
            Thread.currentThread().sleep(OutgoingServer.TIMEOUT * 200);
            client.sendMessage(clientMessage);
        } catch (IOException err) {
            err.printStackTrace();
            System.exit(1);
        } catch (InterruptedException err) {
        } finally{
            clientMessage.setSource(clientMessage.getDestination());
            clientMessage.setDestination(getName());
        }
    }

    /**
     * Handles timer events and periodically check for any missed messages
     * and try to send them again
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        t.stop();
        t.removeActionListener(this);
        for (String peer : mManager.getKnownPeers()) {
            if (mManager.getTries(peer) == TRIES) {
                mManager.increaseTries(peer);
                log("now belives that " + peer + " is offline");
                continue;
            } else if (mManager.getTries(peer) > TRIES) {
                continue;
            }

            Message[] missed = mManager.getMissedMessages(peer);
            for (int i = 0; i < missed.length; i++) {
                try {
                    log("sending missed message " +
                            missed[i].getID() +
                            " to " + peer + "[" +
                            mManager.getTries(peer) + "]");

                    missed[i].resurrect();
                    if (i == 0) {
                        client.sendMessage(missed[i]);
                    } else if (missed[i].getID() != missed[i - 1].getID()) {
                        client.sendMessage(missed[i]);
                    }
                    Thread.currentThread().sleep(200);
                } catch (IOException err) {
                    err.printStackTrace();
                    System.exit(0);
                } catch (InterruptedException err) {
                }
            }
            if (missed.length > 0) {
                mManager.increaseTries(peer);
            }
        }
        t.setDelay(DELAY * mManager.getKnownPeers().size());
        t.addActionListener(this);
        t.start();
    }

    /**
     * 
     * @return reference to the parent Main frame
     */
    @Override
    public Main getParent() {
        return parent;
    }

    /**
     * partially synchronized method to pass the given message to the parent
     * window in order to be logged
     * @param message message text to be logged in the parent Main window
     */
    private void log(String message) {
        JTextArea logArea = this.getParent().logArea;
        synchronized (logArea) {
            logArea.append("\"" + getName() + "\" " + message + "\n");
            logArea.setCaretPosition(logArea.getText().length());
        }
    }

    /**
     * Sets the reference to parent Main window
     * @param parent reference to parent Main window
     */
    public void setParent(Main parent) {
        this.parent = parent;
    }

    /**
     * handles received command message
     * @param msg incoming command message
     */
    void processCommand(Message msg) {
        String source = msg.getSource();
        if (source.equals(getName())) {
            return;
        }
        if (msg.getContent().equals(Message.SYNC)) {
            if (mManager.getKnownPeers().contains(source)) {
                if (mManager.getTries(source) > TRIES) {
                    log("knows that " + source + " is up again");
                }
                mManager.resetTries(source);
            } else {
                mManager.addPeer(source);
                log("now knows " + source);

            }
            msg.setSource(getName());
            msg.setDestination(source);
        }

        try {
            Thread.currentThread().sleep(OutgoingServer.TIMEOUT * 100);
            client.sendMessage(msg);
        } catch (IOException err) {
            err.printStackTrace();
            System.exit(0);
        } catch (InterruptedException err) {
        }

    }

    /**
     * Utility method to look for open communication port to be used
     * @param startPort port number from which search should start
     * @return next available port
     */
    private int getNextPort(int startPort) {
        int port = startPort + 1;
        boolean done = false;

        while (!done) {
            try {
                DatagramSocket s = new DatagramSocket(port);
                s.close();
                s = null;
                done = true;
            } catch (Exception e) {
                port++;
            }
        }
        return port;
    }

    /**
     * inputField accessor
     * @return reference to inputField
     */
    JTextField getInputField() {
        return inputField;
    }

    /**
     * history area accessor
     * @return reference to history JTextArea
     */
    JTextArea getHistoryArea() {
        return history;
    }

    
    /**
     * not implemented
     * @param e
     */
    public void keyTyped(KeyEvent e) {
    }

    /**
     * not implemented
     * @param e
     */
    public void keyReleased(KeyEvent e) {
    }

    /**
     * overrides Comparable.compareTo() by comparing two client managers
     * using their names
     * @param arg0
     * @return
     */
    public int compareTo(ClientManager arg0) {
        return this.getName().compareTo(arg0.getName());
    }
}
