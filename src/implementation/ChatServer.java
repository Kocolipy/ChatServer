package implementation;

import messages.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    public static void main(String args[]) {
        try{
            int port = Integer.parseInt(args[0]);
            final ServerSocket s = new ServerSocket(port);
            final MultiQueue<Message> mQueue = MultiQueue.getMultiQueue();

            while (true){
                Socket client = s.accept();
                new ClientHandler(client, mQueue);
            }

        } catch (IOException e) {
            System.err.println("Cannot use port number " + args[0]);
        } catch (ArrayIndexOutOfBoundsException|IllegalArgumentException e) {
            System.err.println("Usage: java ChatServer <port>");
        }
    }
}