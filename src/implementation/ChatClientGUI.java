package implementation;

import messages.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClientGUI extends JFrame implements Serializable {
    private String server;
    private int port;
    private Socket s;
    private DynamicObjectInputStream in;
    private ObjectOutputStream out;

    private JTextArea history;
    private JTextArea input;
    private JTextArea nickname;
    private String initialName;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("k:mm:ss") ;

    public ChatClientGUI(String server, int port){
        this.server = server;
        this.port = port;
        try{
            s = new Socket(server, port);
            in = new DynamicObjectInputStream(s.getInputStream());
            out = new ObjectOutputStream(s.getOutputStream());
        }catch (IOException e) {
            System.err.println(String.format("Cannot connect to %s on port %s", server, port));
            return;
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + port);
            return;
        }
        initWindow();
        createListener();
    }
    private void initWindow() {
        this.setSize(800,600);
        setTitle("Chat Client");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        history = new JTextArea(20, 20);
        history.setEditable(false);
        history.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(history);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        input = new JTextArea(5, 20);

        nickname = new JTextArea(1,20);
        nickname.setBackground(null);
        nickname.setBorder(null);
        nickname.setText("");

        Button changeNickBtn = new Button();
        changeNickBtn.setPreferredSize(new Dimension(100,30));
        changeNickBtn.setLabel("Change Name");
        changeNickBtn.addActionListener(e -> changeName(e));

        Button sendBtn = new Button();
        sendBtn.setPreferredSize(new Dimension(100,80));
        sendBtn.setLabel("Send");
        sendBtn.addActionListener(e->send(e));

        this.setLayout(new BorderLayout());
        JPanel top = new JPanel();
        JPanel bottom = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        top.setLayout(new GridBagLayout());
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        top.add(nickname, c);
        c.weightx = 0;
        c.gridx = 2;
        top.add(changeNickBtn, c);

        bottom.setLayout(new GridBagLayout());
        bottom.add(sendBtn, c);
        c.weightx = 1.0;
        c.gridx = 0;
        c.gridwidth = 2;
        bottom.add(input, c);

        this.add(top, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);
        this.add(bottom, BorderLayout.SOUTH);

    }
    private void createListener() {
        GUIListener listener = new GUIListener(server, port, history, nickname, in);
        listener.setDaemon(true);
        listener.start();
    }
    private void changeName(java.awt.event.ActionEvent evt) {
        ChangeNickMessage changeNick = new ChangeNickMessage(nickname.getText());
        try {
            out.writeObject(changeNick);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void send(java.awt.event.ActionEvent evt) {
        //Actively wait for user-input
        try {
            String temp = input.getText();
            input.setText("");
            //Perform this if the user input is a special command (i.e starts with \)
            if (temp.startsWith("\\")) {
                String spCommand = temp.substring(1);
                String text = "";
                if (temp.contains(" ")) {
                    spCommand = temp.substring(1, temp.indexOf(' '));
                    text = temp.substring(temp.indexOf(' ') + 1);
                }
                switch (spCommand) {
                    case "nick":
                        ChangeNickMessage changeNick = new ChangeNickMessage(text);
                        out.writeObject(changeNick);
                        break;
                    case "quit":
                        history.append(String.format("%s [Client] Connection terminated.\n", dateFormatter.format(new Date())));
                        s.close();
                        this.dispose();
                        return;
                    default:
                        history.append(String.format("%s [Client] Unknown command %s\n", dateFormatter.format(new Date()), spCommand));
                }
            } else {
                //Otherwise send the message to server as a ChatMessage
                out.writeObject(new ChatMessage(temp));
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        new ChatClientGUI(args[0], Integer.parseInt(args[1])).setVisible(true);
    }
}
