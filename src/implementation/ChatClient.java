package implementation;

import messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.Date;

public class ChatClient {
    public static void main(String[] args){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("k:mm:ss") ;
        String uid = UUID.randomUUID().toString();
        VectorClock vectorClock = new VectorClock();

        try{
            String server = args[0];
            int port = Integer.parseInt(args[1]);

            //We do not want to allow s to be changed once it is connected.
            final Socket s = new Socket(server, port);
            //Daemon Thread which pulls messages from server and print them out.
            Thread output = new Thread() {
                @Override
                public void run() {
                try {
                    DynamicObjectInputStream in = new DynamicObjectInputStream(s.getInputStream());
                    System.out.println(String.format("%s [Client] Connected to %s on port %d.", dateFormatter.format(new Date()), server, port));
                    ReorderBuffer buffer = null; //will be initialised once we receive the first message
                    while (true) {
                        Message msg = (Message)in.readObject();
                        if (buffer == null){
                            buffer = new ReorderBuffer(msg.getVectorClock());
                        }
                        buffer.addMessage(msg);

                        vectorClock.updateClock(msg.getVectorClock());

                        Collection<Message> coll = buffer.pop(); //Get all messages which will be ready to be displayed
                        for (Message m : coll) {
                            System.out.print(dateFormatter.format(m.getCreationTime())+ " ");
                            if (m instanceof NewMessageType) {
                                NewMessageType message = (NewMessageType) m;
                                in.addClass(message.getName(), message.getClassData());
                                System.out.println(String.format(" [Client] New class %s loaded.", message.getName()));
                            } else if (m instanceof RelayMessage) {
                                RelayMessage message = (RelayMessage) m;
                                System.out.println(String.format("[%s] %s", message.getFrom(), message.getMessage()));
                            } else if (m instanceof StatusMessage) {
                                StatusMessage message = (StatusMessage) m;
                                System.out.println(String.format("[Server] %s", message.getMessage()));
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println(String.format("Cannot connect to %s on port %s", args[0], args[1]));
                } catch (ClassNotFoundException e){
                    System.err.println(e.getMessage());
                }
                }
            };
            output.setDaemon(true);
            output.start();

            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
            //Actively wait for user-input
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            while(true) {
                byte[] content = (r.readLine()).getBytes();
                String temp = new String(content);
                //Perform this if the user input is a special command (i.e starts with \)
                if (temp.startsWith("\\")) {
                    String spCommand = temp.substring(1);
                    String text = "";
                    if (temp.contains(" ")){
                        spCommand = temp.substring(1, temp.indexOf(' '));
                        text = temp.substring(temp.indexOf(' ') + 1);
                    }
                    switch (spCommand) {
                        case "nick":
                            ChangeNickMessage changeNick = new ChangeNickMessage(text, uid, vectorClock.incrementClock(uid));
                            out.writeObject(changeNick);
                            break;
                        case "quit" :
                            System.out.println(String.format("%s [Client] Connection terminated.", dateFormatter.format(new Date())));
                            s.close(); //Close the socket
                            return;
                        default:
                            System.out.println(String.format("%s [Client] Unknown command %s", dateFormatter.format(new Date()), spCommand));
                    }
                } else {
                    //Otherwise send the message to server as a ChatMessage
                    out.writeObject(new ChatMessage(new String(content), uid, vectorClock.incrementClock(uid)));
                }
            }
        } catch (IOException e) {
            System.err.println(String.format("Cannot connect to %s on port %s", args[0], args[1]));
        } catch (ArrayIndexOutOfBoundsException|NumberFormatException e) {
            System.err.println("This application requires two arguments: <machine> <port>");
        }
    }
}
