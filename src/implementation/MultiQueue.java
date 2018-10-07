package implementation;

import java.util.HashSet;
import java.util.Set;

public class MultiQueue<T> {
    private Set<MessageQueue<T>> outputs = new HashSet<MessageQueue<T>>();
    private static MultiQueue instance = null;

    public static MultiQueue getMultiQueue(){
        if (instance == null){
            synchronized (MultiQueue.class) {
                if (instance == null){ instance = new MultiQueue(); }
            }
        }
        return instance;
    }
    private MultiQueue(){ }
    public synchronized void register(MessageQueue<T> q) {
        if (!outputs.contains(q))  outputs.add(q);
    }

    public synchronized void deregister(MessageQueue<T> q) {
         outputs.remove(q);
    }

    public synchronized void put(T message) {
            for (MessageQueue<T> q : outputs) {
                q.put(message);
        }
    }

}