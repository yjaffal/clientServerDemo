package clientserverdemo;

import java.net.*;
import java.util.*;

/**
 * This server handles message coming from other peers and forewards them to
 * the client (if this is suitable)
 * @author Yasser Jaffal
 */
public class IncomingServer extends Thread {

    private int listeningPort;
    private int sendingPort;
    public static final int BUFFER_SIZE = 256;
    public static final int TIMEOUT = 1;
    private boolean active = true;
    private String sendGroup, receiveGroup;
    private MulticastSocket listeningSocket;
    private DatagramSocket sendingSocket;
    private MessageArchive archive;

    /**
     * Constructor
     * @param inPort communication port on which this server will be listening
     * @param outPort communication port to which this server will be sending messages
     * @param name name of the server (should same as the name of its client), unique
     * @param receiveGroup IP broadcast group from which this server will be receiving messages
     * @param sendGroup IP group to which this server will be sending messages
     */
    public IncomingServer(int inPort, int outPort, String name, String receiveGroup, String sendGroup) {
        super(name);
        listeningPort = inPort;
        sendingPort = outPort;
        this.sendGroup = sendGroup;
        this.receiveGroup = receiveGroup;
        archive = new MessageArchive();
        try {
            listeningSocket = new MulticastSocket(listeningPort);
            listeningSocket.joinGroup(InetAddress.getByName(receiveGroup));
            sendingSocket = new DatagramSocket();
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
     * listening port accessor
     * @return listening port number
     */
    public int getPort() {
        return listeningPort;
    }

    /**
     * Overrides Thread.run() method:<BR>
     * This method keeps listening on listening port, receiving messages and
     * foreward messages that shuold be forewarded to the client, or otherwise
     * blocks them. Message is blocked if it is:<BR>
     * <llist>
     * <li>Outdated</li>
     * <li>Not intended to this recepient</li>
     * <li>Duplicated message</li>
     * </list>
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
                if (serverMessage.getSendingDate() + 3000 <
                        GregorianCalendar.getInstance().getTimeInMillis() ||
                        (!serverMessage.getDestination().equals(Message.ALL) &&
                        !serverMessage.getDestination().equals(getName())) ||
                        (serverMessage.getType() == Message.DATA &&
                        archive.exists(serverMessage))) {
                    continue;
                }

                if (serverMessage.getType() == Message.DATA) {
                    archive.add(serverMessage);
                }
                buffer = serverMessage.toString().getBytes("Windows-1256");
                outgoing = new DatagramPacket(buffer,
                        buffer.length,
                        InetAddress.getByName(sendGroup),
                        sendingPort);

                sendingSocket.send(outgoing);

            }
            listeningSocket.leaveGroup(InetAddress.getByName(receiveGroup));
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


    /**
     * Private custom class to handle archive of received messages and prevents
     * duplicated messages to be sent to the client
     */
    private class MessageArchive {

        Hashtable<String, TreeSet<Integer>> data;

        /**
         * default constructor
         */
        public MessageArchive() {
            data = new Hashtable<String, TreeSet<Integer>>();
        }

        /**
         * adds message to the archive
         * @param msg message to be added to the archive
         */
        public void add(Message msg) {
            if (!data.containsKey(msg.getSource())) {
                data.put(msg.getSource(), new TreeSet<Integer>());
            }
            data.get(msg.getSource()).add(msg.getID());
        }

        /**
         * checks whether passed message already exists in the archive using
         * (source, ID) pair
         * @param msg message to be checked
         * @return true if message exists, false otherwise
         */
        public boolean exists(Message msg) {
            TreeSet<Integer> ids = data.get(msg.getSource());
            return ids != null && ids.contains(msg.getID());
        }
    }
}
