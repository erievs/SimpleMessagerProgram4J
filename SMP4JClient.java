package chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClient extends JFrame {

    private static final long serialVersionUID = 4188520938010695731L;
    private final JTextField nameField;
    private final JTextField addressField;
    private final JTextField portField;
    private final JButton connectButton;

    private final JTextField messageField;
    private final JTextArea chatArea;

    private ObjectOutputStream objectOutputStream;
    private long startTime;
    private int lastDataSize;

    public ChatClient() {
    	
    	// this is the default name before you type in a username
    	
        super("SimpleMessagerProgram4J Setup");

        setLayout(new BorderLayout());

        JPanel setupPanel = new JPanel(new GridLayout(4, 2));
        setupPanel.add(new JLabel("Name:"));
        nameField = new JTextField();
        setupPanel.add(nameField);

        setupPanel.add(new JLabel("Server Address:"));
        addressField = new JTextField();
        setupPanel.add(addressField);

        setupPanel.add(new JLabel("Port:"));
        portField = new JTextField();
        setupPanel.add(portField);

        connectButton = new JButton("Connect");
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        setupPanel.add(connectButton);

        add(setupPanel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        messageField = new JTextField();
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        add(messageField, BorderLayout.SOUTH);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connect() {
        String userName = nameField.getText();
        String serverAddress = addressField.getText();
        int port = Integer.parseInt(portField.getText());

        try {
        	
        	// your ide may whine about warnings but it is used
        	
            final Socket socket = new Socket(serverAddress, port);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            
            startTime = System.currentTimeMillis(); // system time in mills no need for nano seconds

            // this sends your user name to the server or well the object 
            
            objectOutputStream.writeObject(userName);
            objectOutputStream.flush();

            final ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String message = (String) inputStream.readObject();
                            appendMessage(message);
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // this changes the title to your name
            
            setTitle("SimpleMessagerProgram4J User: " + userName);
            connectButton.setEnabled(false);

        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Connection failed. Check the server address and port.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String clientMessage = messageField.getText();
        if (!clientMessage.isEmpty()) {
        
        	// this adds the timestamp to the message string
        	
            String timestampedMessage = "[" + getCurrentTimestamp() + "] " + clientMessage;

            try {
                objectOutputStream.writeObject(timestampedMessage);
                objectOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            long endTime = System.currentTimeMillis(); // we need this for logConnectionSpeed

            
            lastDataSize = timestampedMessage.getBytes().length; // gets the data size for logging 
            
            logConnectionSpeed(startTime, endTime, lastDataSize);

            messageField.setText("");
        }
    }

    private void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());

        // displays the connection speed and data (object packet or whatever) size in the title 
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        // combines all the strings for it
        
        double connectionSpeed = (lastDataSize * 8.0 / 1024) / (elapsedTime / 1000.0);
        setTitle(nameField.getText() + " |" + "SMP4J: " + nameField.getText() +
                " | Connection Speed: " + String.format("%.2f Kbps", connectionSpeed) +
                " | Last Data Size: " + lastDataSize + " bytes");
    }

    // makes the timestamp
    
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    private void logConnectionSpeed(long startTime, long endTime, int dataSize) {
    	
        // calculates the time taken to send/receive  data
    	
        long elapsedTime = endTime - startTime;

        // Calculate the connection speed in kilobits per second
        double connectionSpeed = (dataSize * 8.0 / 1024) / (elapsedTime / 1000.0);

        // logs the connection speed and data size
        System.out.println("Connection Speed: " + connectionSpeed + " Kbps, Data Size: " + dataSize + " bytes");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ChatClient();
            }
        });
    }
}
