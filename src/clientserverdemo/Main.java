/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package clientserverdemo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Main class thet runs the demo
 * @author Yasser Jaffal
 */
public class Main extends JFrame implements ActionListener, WindowListener {

    JButton create, clear;
    JTextField nameField;
    JTextArea logArea;
    static final int GROUP_BASE = 1;
    TreeSet<ClientManager> clients;

    /**
     * default constructor
     */
    public Main() {
        nameField = new JTextField(20);
        logArea = new JTextArea(20, 30);
        create = new JButton("Create");
        clear = new JButton("Clear");
        JLabel clientLabel = new JLabel("Client name");
        JPanel p = new JPanel();
        p.add(clientLabel);
        p.add(nameField);
        p.add(create);
        p.add(clear);
        logArea.setEditable(false);
        create.addActionListener(this);
        clear.addActionListener(this);
        Container contentPane = getContentPane();
        contentPane.add(p, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(logArea);
        contentPane.add(sc);
        setSize(480, 500);
        setResizable(false);
        setTitle("Client Manager");
        nameField.setText("A, B, C, D");
        addWindowListener(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
/*
        File f = new File(System.getProperty("user.home"));
        File[] files = f.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".csdemo");
            }
        });
        for (File ff : files) {
            //ff.delete();
        }
*/
        clients = new TreeSet<ClientManager>();
        setVisible(true);
    }

    /**
     * main method
     * @param args not used
     */
    public static void main(String[] args) {
        new Main();
    }

    /**
     * Handles frame's events (Create and Clear buttons)
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == create) {
            String name = nameField.getText().toUpperCase().trim();
            if (contains(name)) {
                JOptionPane.showMessageDialog(this, "Dublicate name: " + name);
                return;
            } else if(name.contains(":")){
                JOptionPane.showMessageDialog(this, "Invalid name: " + name +
                                    " (Name cannot contain ':')");
                return;
            }
            if (name.length() > 0) {
                if (name.indexOf(',') == -1) {
                    ClientManager mgr = new ClientManager(name,
                            "127.0.0.1", this);
                    logArea.append("Client \"" + mgr.getName() + "\" created successfully\n");
                } else {
                    String[] clts = name.split(",");
                    for (String clientName : clts) {
                        clientName = clientName.trim();
                        if (clientName.length() == 0) {
                            continue;
                        }
                        if (contains(clientName)) {
                            logArea.append("WARNING: Dublicate Client Name: " + clientName + "\n");
                            continue;
                        }
                        
                        ClientManager mgr = new ClientManager(clientName,
                                "127.0.0.1", this);
                        clients.add(mgr);
                        logArea.append("Client \"" + mgr.getName() + "\" created successfully\n");
                    }
                }
                nameField.setText("");
            } else {
                logArea.append("Invalid client name\n");
            }
            logArea.setCaretPosition(logArea.getText().length());
        } else if (e.getSource() == clear) {
            logArea.setText("");
        }
    }

    public void windowOpened(WindowEvent arg0) {

    }

    public void windowClosing(WindowEvent arg0) {
        while(!clients.isEmpty()){
           ClientManager mgr = clients.first();
           mgr.setVisible(false);
           clients.remove(mgr);
       }
    }

    public void windowClosed(WindowEvent arg0) {
       
    }

    public void windowIconified(WindowEvent arg0) {

    }

    public void windowDeiconified(WindowEvent arg0) {

    }

    public void windowActivated(WindowEvent arg0) {

    }

    public void windowDeactivated(WindowEvent arg0) {

    }

    /**
     * Checks whether a client of this name already exists
     * @param name name to check
     * @return true if client with name exists, false otherwise
     */
    private boolean contains(String name) {
        for(ClientManager m : clients){
            if(m.getName().equalsIgnoreCase(name))
                return true;
        }
        return false;
    }
}
