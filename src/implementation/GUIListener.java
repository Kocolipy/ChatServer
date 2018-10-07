package implementation;

import messages.*;

import javax.swing.*;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GUIListener extends Thread implements Serializable{
    private String server;
    private int port;
    private DynamicObjectInputStream in;
    private JTextArea textArea;
    private JTextArea nickname;
    private String initialName = null;

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("k:mm:ss") ;
    public GUIListener(String server, int port, JTextArea textArea, JTextArea nameArea, DynamicObjectInputStream in) {
        this.server = server;
        this.port = port;
        this.textArea = textArea;
        this.in = in;
        this.nickname = nameArea;
    }
    @Override
    public void run() {
        try {
            textArea.append(String.format("%s [Client] Connected to %s on port %d.\n", dateFormatter.format(new Date()), server, port));
            while (true) {
                Message m = (Message) in.readObject();
                textArea.append(dateFormatter.format(m.getCreationTime())+ " ");
                if (m instanceof NewMessageType) {
                    NewMessageType message = (NewMessageType) m;
                    in.addClass(message.getName(), message.getClassData());
                    textArea.append((String.format("[Client] New class %s loaded.\n", message.getName())));
                } else if (m instanceof RelayMessage) {
                    RelayMessage message = (RelayMessage) m;
                    textArea.append(String.format("[%s] %s\n", message.getFrom(), message.getMessage()));
                } else if (m instanceof StatusMessage) {
                    StatusMessage message = (StatusMessage) m;
                    if (initialName == null){ //Name not set, set name as the one given by server.
                        initialName = message.getMessage().substring(0, message.getMessage().indexOf(" "));
                        nickname.setText(initialName);
                    }
                    textArea.append(String.format("[Server] %s\n", message.getMessage()));
                } else {
                    Class<?> newMessageClass = m.getClass();
                    String text = "";
                    for (Field field : newMessageClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        text += field.getName() + "(" + field.get(m) + "), ";
                    }
                    //textArea.append(String.format("[Client] %s: %s\n", newMessageClass.getSimpleName(), text.substring(0, text.length() - 2)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException | IllegalAccessException e){
            System.err.println(e.getMessage());
        }
    }
}
