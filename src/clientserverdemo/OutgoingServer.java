package clientserverdemo;
import java.net.*;

/**
 * A server that receives messages from client
 * and sends them to other peers
 * @author Yasser Jaffal
 */
public class OutgoingServer extends Thread {

    private int listeningPort;
    private int sendingPort;
    public static final int BUFFER_SIZE = 256;
    public static final int TIMEOUT = 1;
    private boolean active = true;
    private String sendGroup;
    private DatagramSocket listeningSocket;
    private MulticastSocket sendingSocket;

    /**
     * Constructor
     * @param inPort communication port on which this server will be listening
     * @param outPort communication port to which this server will be seding mesasges
     * @param name name of this server (should be same as client's name), unique
     * @param receiveGroup IP group from which this server will be receiving messages
     * @param sendGroup IP broadcasting group to which this server will be sending messages
     */
    public OutgoingServer(int inPort, int outPort, String name, String receiveGroup, String sendGroup) {
        super(name);
        listeningPort = inPort;
        sendingPort = outPort;
        this.sendGroup = sendGroup;

        try {
            listeningSocket = new DatagramSocket(listeningPort);
            sendingSocket = new MulticastSocket(sendingPort);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

    }

    /**
     * shuts down the server
     */
    public void deactivate() {
        active = false;
    }

    /**
     * listening port mutator
     * @return number of listening port
     */
    public int getPort() {
        return listeningPort;
    }

    /**
     * Overrides Thread.run() method:<BR>
     * This method keeps listening on listening port, receiving messages from client
     * and broadcasts them to other peers.<BR>
     * A call to deactivate() stops this method from listening
     */
    @Override
    public void run() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket incoming, outgoing;


        try {
            listeningSocket.setSoTimeout(TIMEOUT);
            while (active) {
                buffer = new byte[BUFFER_SIZE];
                incoming = new DatagramPacket(buffer, buffer.length);

                try {
                    listeningSocket.receive(incoming);
                } catch (SocketTimeoutException err) {
                    continue;
                }

                String data = new String(incoming.getData(), 0, incoming.getLength(), "Windows-1256");
                Message serverMessage = Message.decodeMessage(data);

                String msg = serverMessage.toString();
                buffer = msg.getBytes("Windows-1256");

                //javax.swing.JOptionPane.showMessageDialog(null, msg);
                outgoing = new DatagramPacket(buffer,
                        buffer.length,
                        InetAddress.getByName(sendGroup),
                        sendingPort);

                sendingSocket.send(outgoing);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        } finally {
            listeningSocket.close();
            if (sendingSocket != null) {
                sendingSocket.close();
            }
        }
    }
}
