import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import javax.swing.*;

public class FileTransferServerGUI extends JFrame {

    private JTextArea logArea;
    private static final int BROADCAST_PORT = 8888;
    private static final int TCP_PORT = 5000;
    private static final String BROADCAST_MESSAGE = "FILE_TRANSFER_SERVER_HERE";

    public FileTransferServerGUI() {
        setTitle("FileTransfer Server");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);
        setVisible(true);

        log("Server starting...");

        startBroadcast();
        startTCPServer();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    private void startBroadcast() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                DatagramSocket ds = new DatagramSocket();
                ds.setBroadcast(true);
                byte[] msg = (BROADCAST_MESSAGE + ":" + TCP_PORT).getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(msg, msg.length, InetAddress.getByName("255.255.255.255"),
                        BROADCAST_PORT);
                ds.send(packet);
                ds.close();
            } catch (Exception e) {
                log("Broadcast error: " + e.getMessage());
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void startTCPServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                log("TCP server listening on port " + TCP_PORT);
                while (true) {
                    Socket client = serverSocket.accept();
                    log("Client connected: " + client.getRemoteSocketAddress());
                    handleClient(client);
                }
            } catch (IOException e) {
                log("Server error: " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

                String filename = dis.readUTF();
                long fileSize = dis.readLong();
                File outFile = new File("received_" + System.currentTimeMillis() + "_" + filename);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    long remaining = fileSize;
                    int read;
                    while (remaining > 0
                            && (read = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        fos.write(buffer, 0, read);
                        remaining -= read;
                    }
                }
                log("Received file: " + outFile.getAbsolutePath());

                dos.writeUTF("RECEIVED:" + outFile.getName());
                dos.flush();

                // Optional: send back a file named server_file_to_send.*
                File serverFile = findFileToSend();
                if (serverFile != null) {
                    dos.writeBoolean(true);
                    dos.writeUTF(serverFile.getName());
                    dos.writeLong(serverFile.length());
                    try (FileInputStream fis = new FileInputStream(serverFile)) {
                        byte[] buffer = new byte[8192];
                        int r;
                        while ((r = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, r);
                        }
                    }
                    dos.flush();
                    log("Sent file: " + serverFile.getName());
                } else {
                    dos.writeBoolean(false);
                    dos.flush();
                }

            } catch (IOException e) {
                log("Client error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
                log("Connection closed.");
            }
        }).start();
    }

    private File findFileToSend() {
        File cwd = new File(".");
        File[] matches = cwd.listFiles((dir, name) -> name.startsWith("server_file_to_send"));
        if (matches != null && matches.length > 0)
            return matches[0];
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileTransferServerGUI::new);
    }
}
