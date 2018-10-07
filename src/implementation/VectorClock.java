package implementation;

import java.util.HashMap;
import java.util.Map;

public class VectorClock {

    private final Map<String, Integer> vectorClock;

    public VectorClock() {
        vectorClock = new HashMap<>();
    }

    public VectorClock(Map<String, Integer> existingClock) {
        vectorClock = existingClock==null ? new HashMap<>() : new HashMap<>(existingClock);
    }

    public synchronized void updateClock(Map<String,Integer> msgClock) {
        for (String key : msgClock.keySet()){
            if (vectorClock.containsKey(key)) vectorClock.put(key, Math.max(vectorClock.get(key),msgClock.get(key)));
            else vectorClock.put(key, msgClock.get(key));
        }
    }

    public synchronized Map<String,Integer> incrementClock(String uid) {
        vectorClock.put(uid, getClock(uid) + 1);
	// Return a copy of the contents of the clock
	// TODO: why is this required for thread safety
        //This is to ensure that other threads do not modify the internal state of the VectorClock without calling the appropriate setter methods
        //In addition, this ensures that the Hashmap does not change to reflect any changes made to the Hashmap after this function is called,
        //which could be undesirable as we want the hashmap to reflect the state just before we send the message.
        return new HashMap<>(vectorClock);
   }

    public synchronized int getClock(String uid) {
        return vectorClock.getOrDefault(uid, 0);
    }

    public synchronized boolean happenedBefore(Map<String,Integer> other) {
        boolean equals = true;
        int a, b;
        for (String key : other.keySet()){
            a = vectorClock.getOrDefault(key, 0);
            b = other.getOrDefault(key, 0);
            if (a > b) return false;
            if (a < b) equals = false;
        }
        return !equals;
    }
}