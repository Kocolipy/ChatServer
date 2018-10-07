package implementation;

import java.io.*;
import java.net.Socket;
import java.util.Random;

import messages.*;

public class ClientHandler {
    private Socket socket;
    private MultiQueue<Message> multiQueue;
    private String nickname;
    private MessageQueue<Message> clientMessages;
    private volatile Thread input;
    private volatile Thread output;

    public ClientHandler(Socket s, MultiQueue<Message> q) {
        socket = s;
        multiQueue = q;
        clientMessages = new SafeMessageQueue<>();
        multiQueue.register(clientMessages);
        Random random = new Random();
        nickname = String.format("Anonymous%d%d%d%d%d", random.nextInt(10), random.nextInt(10),
                random.nextInt(10), random.nextInt(10), random.nextInt(10));
        multiQueue.put(new StatusMessage(String.format("%s connected from %s.", nickname,
                socket.getInetAddress().getHostName())));

        input = new Thread() {
            @Override
            public void run() {
                try {
                    ObjectInputStream in = new ObjectInputStream(s.getInputStream());
                    while (true) {
                        Object m = in.readObject();
                        if (m instanceof ChangeNickMessage) {
                            String oldName = nickname;
                            nickname = ((ChangeNickMessage) m).name;
                            multiQueue.put(new StatusMessage(String.format("%s is now known as %s.", oldName, nickname)));
                        } else if (m instanceof ChatMessage) {
                            ChatMessage message = (ChatMessage) m;
                            multiQueue.put(new RelayMessage(nickname, message.getMessage(), message.getCreationTime(), message.getUid(), message.getVectorClock()));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println(e.getMessage());
                } catch (IOException e) {
                    multiQueue.deregister(clientMessages);
                    multiQueue.put(new StatusMessage(String.format("%s has disconnected.", nickname)));

                    //close the threads
                    input = null;
                    output = null;
                    try {
                        s.close(); //close the socket
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        };
        input.setDaemon(true);
        input.start();

        output = new Thread() {
            @Override
            public void run() {
                try {
                    ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());

                    //Sending information of GUIListener class
                    NewMessageType guiListener = new NewMessageType(GUIListener.class.getName(), generateClassData(GUIListener.class));
                    os.writeObject(guiListener);

                    //Sending information of ChatClientGUI class
                    NewMessageType guiClient = new NewMessageType(ChatClientGUI.class.getName(), generateClassData(ChatClientGUI.class));
                    os.writeObject(guiClient);

                    //Sending information of GUIMessage class
                    NewMessageType guiMes = new NewMessageType(GUIMessage.class.getName(), generateClassData(GUIMessage.class));
                    os.writeObject(guiMes);

                    GUIMessage guiMessage = new GUIMessage(socket.getInetAddress().getHostName(), socket.getLocalPort());
                    os.writeObject(guiMessage);
                    os.flush();

                    while (true) {
                        Message m = clientMessages.take();
                        os.writeObject(m);
                        os.flush();
                    }
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        };
        output.setDaemon(true);
        output.start();
    }

    private byte[] generateClassData(Class clazz) {
        String name = clazz.getName();
        InputStream is = clazz.getClassLoader().getResourceAsStream(
                name.replaceAll("\\.", "/") + ".class");
        byte[] temp = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int n = 0;
        try {
            while ((n = is.read(temp)) != -1) {
                os.write(temp, 0, n);
            }
            os.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return os.toByteArray();
    }
}

