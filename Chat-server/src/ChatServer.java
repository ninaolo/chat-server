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
		listen();
	}

	private void listen() throws IOException {
		System.out.println("Attempting to start server...");
		try {
			// Vi börjar lyssna på porten
			serverSocket = new ServerSocket(port);
			System.out.println("Successfully started server. Listening on port " + port + ".");

		} catch (Exception e) {
			e.printStackTrace();

		}

		// Vi accepterar connections för evigt
		while (true) {

			// Returnerar en ny socket för varje ny connection. Om det inte
			// finns någon blockar den.
			System.out.println("Waiting for client to connect...");
			Socket clientSocket = serverSocket.accept();
			System.out.println("Client connected: " + clientSocket);

			// Skapa en ny tråd för varje ny klient
			ServerThread thread = new ServerThread(this, clientSocket);

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

	// Main: skapa en ny server
	public static void main(String[] args) throws IOException {
		new ChatServer();
	}

	public void removeThread(ServerThread thread) {
		try {
			thread.clientSocket.close();
		} catch (IOException e) {
			System.out.println("Couldn't close thread");
			e.printStackTrace();
		}
		threads.remove(thread);
		sendMsgToAll("User has left the chat room");
	}

}
