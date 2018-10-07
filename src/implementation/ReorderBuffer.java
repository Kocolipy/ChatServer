package implementation;

import java.util.*;

import messages.Message;

public class ReorderBuffer {

    private final List<Message> buffer;
    private final VectorClock lastDisplayed;

    public void addMessage(Message m) {
        buffer.add(m);
    }

    public ReorderBuffer(Map<String,Integer> initialMsg) {
        buffer = new ArrayList<>();
        System.out.println(initialMsg);
        lastDisplayed = new VectorClock(initialMsg);
    }

    public Collection<Message> pop() {
        //Return a collection of messages in order of causality. (In the event of non-causal messages, they will be in the order they were received
        //Removes the messages from buffer (Buffer will only contain messages which happened much after the lastDisplayed.
        //(i.e more than one differece in the vector clock)
        int i = 0;
        int a, b;
        boolean remove; //marker to indicate if we should remove the message from the buffer
        Message msg;
        Collection<Message> messages = new ArrayList<>(); //Collection of messages which we will return

        while (buffer.size() > i){
            msg = buffer.get(i);
            int steps = 0;  //steps refer to the number of times b - a == 1 occurs
            remove = true;
            for (String key : msg.getVectorClock().keySet()){ //For every key in msg.vectorclock
                a = lastDisplayed.getClock(key);
                b = msg.getVectorClock().get(key);
                //if the difference between the time is one
                if (b - a == 1) {
                    if (steps >= 1) { //There was already a difference of 1in the vector clocks
                        remove = false;
                        break;
                    } else steps++;
                }
                else if (b - a >= 1){ //The difference is too much, leave message in buffer
                    remove = false;
                    break;
                }
                else if (b < a) break;//Message occurred before the lastdisplayed time, delete this message (out of order)
            }
            if (remove){
                buffer.remove(msg);
                if (steps <= 1){ //Message should be displayed
                    messages.add(msg);
                    lastDisplayed.updateClock(msg.getVectorClock());
                    i = 0; //go through the buffer to find messages which would now be valid
                }
            } else {
                i++;
            }
        }
        return messages;
    }
}