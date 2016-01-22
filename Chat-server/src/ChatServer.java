import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

// OBS: tänk på synchronized, hur löser vi detta så att man inte tar bort trådar samtidigt som man loopar igenom dem?

/*
 * En klass för servern till en multi-trådad chatt som kan ha flera klienter kopplade till sig.
 */
public class ChatServer {

	// En random port som vi valt för servern
	private int port = 1337;
	private ServerSocket serverSocket;
	private ArrayList<ServerThread> threads;

	public ChatServer() throws IOException {
		threads = new ArrayList<ServerThread>();
		startServer();
		listen();
	}

	private void startServer() throws IOException {
		System.out.println("Attempting to start server...");
		serverSocket = new ServerSocket(port);
		System.out.println("Successfully started server!");

	}

	private void listen() throws IOException {

		System.out.println("Listening on port " + port + ".");

		// Vi accepterar connections för evigt
		while (true) {

			// Returnerar en ny socket för varje ny connection. Om det inte
			// finns någon blockar den.
			System.out.println("Waiting for client to connect...");
			Socket clientSocket = serverSocket.accept();
			System.out.println("New client connected on port " + clientSocket.getPort() + ".");

			// Skapa en ny tråd för varje ny klient
			ServerThread thread = new ServerThread(this, clientSocket);

			thread.output
					.writeUTF("Welcome to the chat " + thread.username + "! There are currently " + (threads.size() + 1)
							+ " users in the chat room. If you want to leave the chat room, simply type 'leave'.");
			clientAdded(thread);

			// Lägg till den nya chat-användaren till listan
			threads.add(thread);
		}

	}

	public void sendMsgToAll(String message) {
		for (ServerThread clientThread : threads) {
			try {
				clientThread.output.writeUTF(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * Används från ServerThread.
	 */
	public void removeThread(ServerThread thread) {
		System.out.println("A user wants to leave the chat room. Processing...");
		try {
			thread.clientSocket.close();
		} catch (IOException e) {
			System.err.println("Couldn't close thread.");
			e.printStackTrace();
		}
		threads.remove(thread);
		sendMsgToAll("A user has left the chat room.");
	}

	/*
	 * Skickar ett meddelande till alla andra när en ny klient ansluter till
	 * chatten.
	 */
	private void clientAdded(ServerThread newThread) {
		for (ServerThread clientThread : threads) {
			try {
				clientThread.output.writeUTF(newThread.username + " joined the chat! There are now "
						+ (threads.size() + 1) + " users in the chat room.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Main: skapa en ny server
	public static void main(String[] args) throws IOException {
		new ChatServer();
	}

}
