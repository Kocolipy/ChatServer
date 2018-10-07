package implementation;

import messages.Execute;
import messages.Message;

public class GUIMessage extends Message {
    private static final long serialVersionUID = 1L;
    private String server;
    private int port;

    public GUIMessage(String server, int port) {
        super();
        this.server = server;
        this.port = port;
    }
    @Execute
    public void openGUI(){
        new ChatClientGUI(server, port).setVisible(true);
    }
}
