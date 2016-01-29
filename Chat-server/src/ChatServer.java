import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Optional;

import org.json.simple.JSONObject;

/*
 * Represents a chat server to a multi-threaded chat which can accept multiple clients.
 */
public class ChatServer {

	// A random port we chose for the server
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

	/*
	 * Listens for connecting clients forever.
	 */
	private void listen() throws IOException {

		System.out.println("Listening on port " + port + ".");

		while (true) {

			// Returns a new socket for every new connection. If there is no one
			// it blocks here
			System.out.println("Waiting for client to connect...");
			Socket clientSocket = serverSocket.accept();
			System.out.println("New client connected on port " + clientSocket.getPort() + ".");

			// Create a new thread for every client
			ServerThread thread = new ServerThread(this, clientSocket);

			clientAdded(thread);

			// Add the new chat user to a list
			threads.add(thread);
			sendWelcomeMsg(thread);
		}
	}

	public synchronized void sendWelcomeMsg(ServerThread thread) {
		String msg = "\n### Welcome to the chat room " + thread.username + "! ###\n";
		msg += "\nThere are currently " + threads.size()
				+ " users in the chat room. If you want to leave the chat room, simply type 'leave'.\n";
		msg += "\n-- USERS IN CHAT ROOM --\n";
		for (ServerThread clientThread : threads) {
			msg += clientThread.username + "\n";
		}
		msg += "\n-- COMMANDS --\n";
		msg += "Whisper to a user:   /w <username> <message>\n";
		msg += "Send arbitrary file to a user:   /s <username> <path/to/file.txt>\n";

		JSONObject json = getJson("whisper", msg, thread.username, "");
		try {
			thread.output.writeUTF(json.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Sends a message to all clients by retrieving data from a json object
	 * representing a request.
	 */
	public synchronized void sendMsgToAll(JSONObject json) {
		String server_json = json.toJSONString();
		for (ServerThread clientThread : threads) {
			try {
				clientThread.output.writeUTF(server_json);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * Sends a private message by retrieving data from a json object. Used for
	 * whispering and responding to file requests.
	 */
	public synchronized void sendPrivateMessage(JSONObject json) {
		String user = (String) json.get("TO");
		String server_json = json.toJSONString();
		for (ServerThread clientThread : threads) {
			try {
				if (clientThread.getUsername().compareTo(user) == 0) {
					clientThread.output.writeUTF(server_json);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Transforms protocol data into a JSON object.
	 */
	public JSONObject getJson(String request, String content, String to, String from) {
		JSONObject json = new JSONObject();
		json.put("REQUEST", request);
		json.put("CONTENT", content);
		json.put("TO", to);
		json.put("FROM", from);
		return json;
	}

	/*
	 * Checks if a user is connected to the chat.
	 */
	public synchronized boolean userExists(String userName) {
		for (ServerThread clientThread : threads) {
			if (clientThread.getUsername().compareTo(userName) == 0) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Used from ServerThread.
	 */
	public synchronized void removeThread(ServerThread thread) {
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
		JSONObject server_json = getJson("send_to_all", "User " + tempName + " has left the chatroom", "", "server");
		sendMsgToAll(server_json);
	}

	/*
	 * Sends a message to all others when a new client connects to the chat.
	 */
	private void clientAdded(ServerThread newThread) {
		String message = newThread.username + " joined the chat! There are now " + (threads.size() + 1)
				+ " users in the chat room.";
		JSONObject server_json = getJson("send_to_all", message, "", "Server");
		sendMsgToAll(server_json);
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

	// Main: create a new server
	public static void main(String[] args) throws IOException {
		new ChatServer();
	}

}
