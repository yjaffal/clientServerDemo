package clientserverdemo;

import java.io.Serializable;
import java.util.*;

/**
 * This class manages sent messages, acknowledgements, and known peers
 * @author Yasser Jaffal
 */
public class MessageManager implements Serializable {

    private Hashtable<String, PeerMessages> peers;
    private Set<Message> sentMessages;
    int nextMessageID = 0;

    /**
     * Constructor
     */
    public MessageManager() {
        peers = new Hashtable<String, PeerMessages>();
        sentMessages = new HashSet<Message>();
    }

    /**
     * adds a peer name to the collection of known peers
     * @param name name of peer to add
     */
    public void addPeer(String name) {
        if (!peers.containsKey(name)) {
            PeerMessages pm = new PeerMessages();
            pm.tries = 0;
            pm.missed = new TreeSet<Integer>();
            peers.put(name, pm);
        }
    }

    /**
     * adds message to the collection of sent messages and marks it as
     * missed message to all known peers
     * @param msg message to add
     */
    public void addMessage(Message msg) {
        if (sentMessages.add(msg)) {
            for (PeerMessages pms : peers.values()) {
                pms.missed.add(msg.getID());
            }
        }
    }

    /**
     * acknowledges passed message from the passed peer so it is no longer
     * considered missed
     * @param peerName peer name from which acknowledgement came
     * @param messageID ID of the acknowledged message
     */
    public void acknowledge(String peerName, int messageID) {
        if (!peers.containsKey(peerName)) {
            addPeer(peerName);
        }
        PeerMessages pm = peers.get(peerName);
        Set<Integer> msgs = pm.missed;
        if (msgs.contains(messageID)) {
            msgs.remove(new Integer(messageID));
        }
    }

    /**
     * returns array of missed messages for the passed peer name
     * @param peerName peer name to get missed messages for
     * @return array of missed messages
     */
    public Message[] getMissedMessages(String peerName) {
        Set<Integer> ids = peers.get(peerName).missed;
        TreeSet<Message> result = new TreeSet<Message>();
        
        for (Message msg : sentMessages) {
            if (ids.contains(msg.getID())) {
                msg.setDestination(peerName);
                result.add(msg);
            }
        }
        return result.toArray(new Message[0]);
    }

    /**
     * returns a set of known peer names
     * @return Set\<String\> containing known peers names
     */
    public Set<String> getKnownPeers() {
        return peers.keySet();
    }

    /**
     * resets number of failure sending tries of missed
     * messages for a given peer name
     * @param peerName peer name to reset tries for
     */
    public void resetTries(String peerName) {
        peers.get(peerName).tries = 0;
    }

    /**
     * increases number of failure tries of missed messages
     * for a given peer name by 1
     * @param peerName peer name to increase tries for
     */
    public void increaseTries(String peerName) {
        peers.get(peerName).tries++;
    }

    /**
     * resets number of failure sending tries of missed
     * messages for all known peers
     */
    public void resetAllTries() {
        for (String peer : peers.keySet()) {
            resetTries(peer);
        }
    }

    /**
     * gets the number of failure tries of sending missed messages
     * for a given peer name
     * @param peerName peer name to get number of tries for
     * @return number of failure sending tries
     */
    public int getTries(String peerName) {
        return peers.get(peerName).tries;
    }

    /**
     * Private class to handle missed messages and number of
     * tries for a single known peer
     */
    private class PeerMessages implements Serializable {

        public int tries;
        public TreeSet<Integer> missed;
    }
}
