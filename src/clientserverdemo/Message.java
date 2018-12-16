package clientserverdemo;

import java.io.Serializable;
import java.util.*;
/**
 * this class represents a communication message
 * @author Yasser Jaffal
 */
public class Message implements Serializable, Comparable<Message> {

    public static final int COMMAND = 0;
    public static final int ACKNOWLEDGE = 1;
    public static final int DATA = 2;
    
    public static final String ALL = "ALL";
    public static final String SYNC = "SYNC";
    
    private String source, dest;
    private int type;
    private String content;
    private int id;
    private long creationDate;
    private long sendingDate;
    /**
     * Constructor
     * @param source Name of the source client
     * @param dest namr of destination client
     * @param content content of this message
     * @param type message type: DATA, ACKNOWLEDGE, or COMMAND
     */
    public Message(String source, String dest, String content, int type) {
        setType(type);
        setContent(content);
        setSource(source);
        setDestination(dest);
        creationDate = GregorianCalendar.getInstance().getTimeInMillis();
        sendingDate = GregorianCalendar.getInstance().getTimeInMillis();
    }

    /**
     * creation date accessor
     * @return creation date of this message
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * sending date accessor
     * @return sending date of this message
     */
    public long getSendingDate() {
        return sendingDate;
    }

    /**
     * message ID accessor
     * @return message ID
     */
    public int getID() {
        return id;
    }

    /**
     * message ID mutator
     * @param id new message ID
     */
    public void setID(int id) {
        this.id = id;
    }

    /**
     * message type accessor
     * @return message type. Could be Message.DATA, Message.ACKNOWLEDGE, or
     * Message.COMMAND
     */
    public int getType() {
        return type;
    }

    /**
     * message type mutator
     * @param type new message type
     * @throws InvalidArgumentException if type is invalid
     */
    public void setType(int type) {
        if (type < COMMAND || type > DATA) {
            throw new IllegalArgumentException("Invalid message type");
        }
        this.type = type;
    }

    /**
     * message content accessor
     * @return message content
     */
    public String getContent() {
        return content;
    }

    /**
     * message content mutator
     * @param content new message content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * message source accessor
     * @return message source name
     */
    public String getSource() {
        return source;
    }

    /**
     * message source mutator
     * @param source new message source name
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * message destination accessor
     * @return message destination name
     */
    public String getDestination() {
        return dest;
    }

    /**
     * message destination mutator
     * @param dest new message destination name
     */
    public void setDestination(String dest) {
        this.dest = dest;
    }

    /**
     * renews message sending time to current
     */
    public void resurrect() {
        this.sendingDate = GregorianCalendar.getInstance().getTimeInMillis();
    }

    /**
     * returns a string representation of this message
     * @return message as string in the following format:<BR>
     * <B>ID:CREATION_TIME:SOURCE:DESTINATION:TYPE:CONTENT</B>
     */
    @Override
    public String toString() {
        return id + ":" +
                getCreationDate() + ":" +
                getSendingDate() + ":" +
                source + ":" +
                dest + ":" +
                encodeType(type) + ":" +
                content;
    }

    /**
     * Utility method to convert type from integer
     * to string for String representation
     * @param type integer type
     * @return String type
     */
    private static String encodeType(int type) {
        if (type == COMMAND) {
            return "COMMAND";
        }
        if (type == DATA) {
            return "DATA";
        }
        if (type == ACKNOWLEDGE) {
            return "ACKNOWLEDGE";
        }
        return "UNKNOWN";
    }

    /**
     * Utility method to convert type from String to integer
     * @param type String representation of type
     * @return integer value of the type, negative value if type is invalid
     */
    private static int decodeType(String type) {
        int result = -1;
        if (type.equals("COMMAND")) {
            result = COMMAND;
        } else if (type.equals("ACKNOWLEDGE")) {
            result = ACKNOWLEDGE;
        } else if (type.equals("DATA")) {
            result = DATA;
        }
        return result;
    }

    /**
     * Message decodiong factory method that creates message from
     * encoded String representation
     * @param message encoded String
     * @return message with decoded info
     */
    public static Message decodeMessage(String message) {
        Message result = null;
        message = message.trim();
        String[] parts = message.split(":", 7);
        if (parts != null && parts.length == 7) {
            int type = decodeType(parts[5]);
            result = new Message(parts[3], parts[4], parts[6], type);
            result.setID(Integer.parseInt(parts[0]));
            result.creationDate = Long.parseLong(parts[1]);
            result.sendingDate = Long.parseLong(parts[2]);
            /*
            for(int i = 7; i < parts.length; i++){
                result.content += ":" + parts[i];
            }*/
        }

        return result;
    }

    /**
     * Implements comparision between two messages based on their creation date
     * @param o other message to compare this message with
     * @return whether this message has less, equal, or creation date
     */
    public int compareTo(Message o) {
        return (int)(this.getCreationDate() - o.getCreationDate());
    }
}
