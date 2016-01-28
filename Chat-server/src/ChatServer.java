import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

import org.json.simple.JSONObject;

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

	public void sendMsgToAll(JSONObject json) {
		String server_json = json.toJSONString();
		for (ServerThread clientThread : threads) {
			try {
				clientThread.output.writeUTF(server_json);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void sendPrivateMessage(JSONObject json) {
		String user = (String) json.get("TO");
		String server_json = json.toJSONString();
		for (ServerThread clientThread : threads) {
			try{
				if(clientThread.getUsername().compareTo(user)==0){
					clientThread.output.writeUTF(server_json);
					}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public boolean userExists(String userName) {
		for (ServerThread clientThread : threads) {
			if (clientThread.getUsername().compareTo(userName) == 0) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Används från ServerThread.
	 */
	public void removeThread(ServerThread thread) {
		System.out.println("A user wants to leave the chat room. Processing...");
		String tempName = thread.getUsername();
		try {
			thread.clientSocket.close();
			thread.interrupt();
		} catch (IOException e) {
			System.err.println("Couldn't close thread.");
			e.printStackTrace();
		}
		thread.interrupt();
		threads.remove(thread);
		JSONObject server_json = new JSONObject();
		server_json.put("REQUEST", "send_to_all");
		server_json.put("CONTENT", "User "+tempName+" has left the chatroom");
		server_json.put("TO", "");
		server_json.put("FROM","server");
		sendMsgToAll(server_json);
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

	/*
	 * Gets the remote IP corresponding to the given username.
	 */
	public String getClientIP(String username) {
		Optional<ServerThread> user = threads.stream().filter(t -> t.username == username).findFirst();
		if (user.isPresent()) {
			return user.get().getIP();
		} else {
			return "";
		}
	}

	/*
	 * Gets the remote port corresponding to the given username.
	 */
	public int getClientPort(String username) {
		Optional<ServerThread> user = threads.stream().filter(t -> t.username == username).findFirst();
		if (user.isPresent()) {
			return user.get().getPort();
		} else {
			return -1;
		}
	}

	// Main: skapa en ny server
	public static void main(String[] args) throws IOException {
		new ChatServer();
	}

}
