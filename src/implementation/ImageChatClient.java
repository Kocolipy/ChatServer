package implementation;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class ImageChatClient extends JFrame {
    private String server;
    private int port;
    private Socket s;

    private Canvas imageViewer;
    private BufferedImage latestImage;
    private Button uploadButton;

    public ImageChatClient(String server, int port) {
        this.server = server;
        this.port = port;
        try{
            s = new Socket(server, port);
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
        setTitle("Image Uploader");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        imageViewer = new Canvas() {
            @Override
            public void paint(Graphics g) {
                g.drawImage(latestImage, 0,0, null);
            }
        };

        uploadButton = new Button();
        uploadButton.setBounds(0,0, 780,40);
        uploadButton.setLabel("Upload");
        uploadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upload(evt);
            }
        });

        this.add(imageViewer, BorderLayout.CENTER);
        this.add(uploadButton, BorderLayout.SOUTH);
    }
    private void createListener() {
        Thread listener = new Thread() {
            @Override
            public void run() {
                try {
                    BufferedInputStream input = new BufferedInputStream(s.getInputStream());
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int i;
                    while(true){
                        Boolean sending = true;
                        while (sending) {
                            i = input.read();
                            os.write(i);
                            if (i == 0xFF) {
                                i = input.read();
                                os.write(i);
                                if (i == 0xD9) sending = false;
                            }
                        }
                        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
                        latestImage = ImageIO.read(is);
                        imageViewer.repaint();
                        is.close();
                    }
                } catch (IOException e) {
                    System.err.println(String.format("Cannot connect to %s on port %s", server, port));
                    return;
                }
            }
        };
        listener.setDaemon(true);
        listener.start();
    }
    private void upload(java.awt.event.ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                ImageIO.write(ImageIO.read(new File(selectedFile.getAbsolutePath())), "jpg", s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public static void main(String args[]) {
        new ImageChatClient("java-1b.cl.cam.ac.uk", 15002).setVisible(true);
    }
}
