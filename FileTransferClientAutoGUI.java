import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class FileTransferClientAutoGUI extends JFrame {

    private JTextArea logArea;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private static final int BROADCAST_PORT = 8888;
    private static final int TCP_PORT_DEFAULT = 5000;
    private static final int UDP_LISTEN_TIMEOUT_MS = 5000;
    private static final String EXPECTED_BROADCAST_PREFIX = "FILE_TRANSFER_SERVER_HERE";

    public FileTransferClientAutoGUI() {
        setTitle("FileTransfer Client (Auto-Detect Server)");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);

        JPanel dropPanel = new JPanel();
        dropPanel.setBorder(BorderFactory.createTitledBorder("Drag & Drop File Here"));
        dropPanel.setPreferredSize(new Dimension(480, 100));
        dropPanel.setBackground(Color.LIGHT_GRAY);

        new DropTarget(dropPanel, new DropTargetAdapter() {
            public void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> droppedFiles = (java.util.List<File>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        sendFile(file);
                    }
                } catch (Exception ex) {
                    log("Drop error: " + ex.getMessage());
                }
            }
        });

        JButton selectButton = new JButton("Select File to Send");
        selectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                sendFile(file);
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(dropPanel, BorderLayout.CENTER);
        panel.add(selectButton, BorderLayout.SOUTH);

        add(scroll, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        setVisible(true);

        connectToServerAuto();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
    }

    // Auto-discover server on LAN or same PC
    private void connectToServerAuto() {
        new Thread(() -> {
            InetSocketAddress serverAddr = discoverServer();
            if (serverAddr == null) {
                // fallback manual input
                String serverIP = JOptionPane
                        .showInputDialog("Server not found. Enter IP or leave blank for localhost:");
                if (serverIP == null || serverIP.isEmpty())
                    serverIP = "localhost";
                serverAddr = new InetSocketAddress(serverIP, TCP_PORT_DEFAULT);
            }
            try {
                socket = new Socket(serverAddr.getAddress(), serverAddr.getPort());
                dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                log("Connected to server: " + socket.getRemoteSocketAddress());
            } catch (Exception e) {
                log("Connection error: " + e.getMessage());
            }
        }).start();
    }

    private InetSocketAddress discoverServer() {
        log("Listening for server broadcast...");
        try (DatagramSocket ds = new DatagramSocket(BROADCAST_PORT)) {
            ds.setSoTimeout(UDP_LISTEN_TIMEOUT_MS);
            byte[] buf = new byte[512];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (true) {
                try {
                    ds.receive(packet);
                    String msg = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    if (msg.startsWith(EXPECTED_BROADCAST_PREFIX)) {
                        String[] parts = msg.split(":");
                        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : TCP_PORT_DEFAULT;
                        InetAddress addr = packet.getAddress();
                        log("Discovered server: " + addr.getHostAddress() + ":" + port);
                        return new InetSocketAddress(addr, port);
                    }
                } catch (SocketTimeoutException ste) {
                    log("No broadcast received.");
                    return null;
                }
            }
        } catch (IOException e) {
            log("Discovery error: " + e.getMessage());
            return null;
        }
    }

    private void sendFile(File file) {
        new Thread(() -> {
            try {
                log("Sending file: " + file.getAbsolutePath());
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, read);
                    }
                }
                dos.flush();

                String ack = dis.readUTF();
                log("Server ack: " + ack);

                boolean incoming = dis.readBoolean();
                if (incoming) {
                    String incomingName = dis.readUTF();
                    long incomingSize = dis.readLong();
                    log("Receiving file: " + incomingName);
                    File out = new File("received_from_server_" + System.currentTimeMillis() + "_" + incomingName);
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buffer = new byte[8192];
                        long remaining = incomingSize;
                        int r;
                        while (remaining > 0
                                && (r = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                            fos.write(buffer, 0, r);
                            remaining -= r;
                        }
                    }
                    log("Saved file: " + out.getAbsolutePath());
                } else {
                    log("No file from server.");
                }

            } catch (Exception e) {
                log("Error sending file: " + e.getMessage());
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileTransferClientAutoGUI::new);
    }
}
