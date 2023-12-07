package chat;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;

import java.util.List;
import java.util.Scanner;

public class HostTest {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // gets the user's ip
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            System.out.println("Your (local/network I think) IP address is: " + localHost.getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // this is the adress we want to connect to, it defaults to localhost

        System.out.print("Enter the address you want to bind bind to (or just leave it empty for localhost): ");
        String serverIPAddress = scanner.nextLine().trim();
        if (serverIPAddress.isEmpty()) {
            serverIPAddress = "localhost";
        }

        System.out.print("Enter the port to listen on: ");
        int port = scanner.nextInt();

        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(serverIPAddress));
            System.out.println("Server is waiting for connections...");
            System.out.println("Server IP address: " + serverIPAddress);
            System.out.println("Server Port: " + port);

            // this registers a shutdown hook to unbind the port when closed, to prevent issues when you wanna do something else
            
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
				    try {
				        if (serverSocket != null && !serverSocket.isClosed()) {
				            serverSocket.close();
				            System.out.println("Server socket closed.");
				        }
				    } catch (IOException e) {
				        e.printStackTrace();
				    }
				}
			}));

            // start a thread to our handle server input
            new Thread(new Runnable() {
				@Override
				public void run() {
				    while (true) {
				        handleServerInput();
				    }
				}
			}).start();

            while (true) {
            	
                // waits for any clients to connect before it does anything
            	
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());

                String userName = (String) objectInputStream.readObject();
                System.out.println("User '" + userName + "' joined the chat");

                ClientHandler clientHandler = new ClientHandler(userName, clientSocket, objectOutputStream, objectInputStream);

                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void handleServerInput() {
        Scanner scanner = new Scanner(System.in);
        String serverMessage = scanner.nextLine();

        if (!serverMessage.isEmpty()) {
            broadcastMessage("Server: " + serverMessage);
        }
    }

    private static class ClientHandler implements Runnable {
        private final String userName;
        private final Socket socket;
        private final ObjectOutputStream objectOutputStream;
        private final ObjectInputStream objectInputStream;

        public ClientHandler(String userName, Socket socket, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
            this.userName = userName;
            this.socket = socket;
            this.objectOutputStream = objectOutputStream;
            this.objectInputStream = objectInputStream;
        }

        @Override
        public void run() {
            try {
                new Thread(new Runnable() {
					@Override
					public void run() {
					    try {
					        while (true) {
					            String message = (String) objectInputStream.readObject();
					            broadcastMessage(userName + ": " + message);
					        }
					    } catch (IOException | ClassNotFoundException e) {
					        e.printStackTrace();
					    }
					}
				}).start();

                // sends a nice welcome message to the client who connected
                objectOutputStream.writeObject("Welcome to the chat, " + userName + "!");
                objectOutputStream.flush();

                // broadcasts a user joining to all connected clients
                broadcastMessage(userName + " joined the chat");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) throws IOException {
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
        }

        
        // this is not used anymore since we do it client side now
        
		/*
		 * private void logConnectionSpeed(long startTime, long endTime, int dataSize) {
		 * 
		 * endTime - startTime;
		 * 
		 * // Calculate the connection speed in kilobits per second double
		 * connectionSpeed = (dataSize * 8.0 / 1024) / (elapsedTime / 1000.0);
		 * 
		 * 
		 * userName + "' - Connection Speed: " + connectionSpeed + " Kbps, Data Size: "
		 * + dataSize + " bytes, Time: " + getCurrentTimestamp()); }
		 */


        
    }

    // let's you send a message to people
    
    private static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            try {
                client.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
