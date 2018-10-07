package implementation;

public class SafeMessageQueue<T> implements MessageQueue<T> {
    private static class Link<L> {
        L val;
        Link<L> next;
        Link(L val) {
            this.val = val;
            this.next = null;
        }
    }
    private Link<T> first = null;
    private Link<T> last = null;

    public synchronized void put(T val) {
        if (first == null){
            first = last = new Link<>(val);
        } else {
            last = last.next = new Link<>(val);
        }
        this.notifyAll();
    }

    public synchronized T take() {
        while(first == null) { //use a loop to block thread until data is available
            try {
                this.wait();
            } catch(InterruptedException ie) {
                return null;
            }
        }
        T value = first.val;
        first = first.next;
        if (first == null) last = null;
        return value;
    }
}