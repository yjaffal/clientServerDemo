package clientserverdemo;
import java.io.*;
import java.net.*;
import javax.swing.JTextArea;

/**
 * This client works as message inbox, it receives messages from the server and
 * sends them to the client manager to be handled
 * @author Yasser Jaffal
 */
public class ReceivingClient extends Thread {

    private boolean active = true;
    private DatagramSocket serverSocket;
    private ClientManager manager;
    private JTextArea history;

    /**
     * Constructor
     * @param port communication port on which this client will be listening
     * @param name name of this client (should be same as server's name), unique
     * @param rGroup IP group from which this client will be receivng messages
     * @param manager reference to ClientManager object that will handle received messages
     */
    public ReceivingClient(int port, String name, String rGroup, ClientManager manager) {
        super(name);
        history = manager.getHistoryArea();
        this.manager = manager;
        try {
            serverSocket = new DatagramSocket(port);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * shuts down the client
     */
    public void deactivate() {
        active = false;
    }

    /**
     * Overrides Thread.run() method:<BR>
     * This method keeps listening on listening port, receiving messages from server
     * and forewards it to ClientManager<BR>
     * A call to deactivate() stops this method from listening
     */
    @Override
    public void run() {
        byte[] buffer = new byte[OutgoingServer.BUFFER_SIZE];
        DatagramPacket fromServer = null;

        try {
            serverSocket.setSoTimeout(OutgoingServer.TIMEOUT);
            while (active) {
                buffer = new byte[OutgoingServer.BUFFER_SIZE];
                fromServer = new DatagramPacket(buffer, buffer.length);


                try {
                    serverSocket.receive(fromServer);
                } catch (SocketTimeoutException err) {
                    continue;
                }

                buffer = fromServer.getData();
                String data = new String(buffer, 0, buffer.length, "Windows-1256");
                fromServer = new DatagramPacket(buffer, buffer.length);
                Message clientMessage = Message.decodeMessage(data);

                System.out.println(getName() + "<--" + clientMessage);

                if (clientMessage.getType() == Message.DATA) {
                    /*history.append(clientMessage.getSource() + ": " +
                            clientMessage.getContent() + "\n");*/
                    manager.addMessage(clientMessage);

                    if (!clientMessage.getSource().equals(getName())) {

                        manager.sendAck(clientMessage);
                    }
                } else if (clientMessage.getType() == Message.ACKNOWLEDGE) {
                    if (clientMessage.getSource().equals(getName())) {
                        continue;
                    }
                    //System.out.println(getName() + " RC received ACK");
                    manager.acknowledgeMessage(clientMessage);
                } else if (clientMessage.getType() == Message.COMMAND) {
                    manager.processCommand(clientMessage);
                }

            }
        } catch (IOException err) {
            System.err.println(err.getMessage());
            err.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }
}
