import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/*
 * Represents a server thread which deals with the communication with a client in a chat server.
 */
public class ServerThread extends Thread {

	private ChatServer server;
	Socket clientSocket;
	DataInputStream input;
	DataOutputStream output;
	public String username;
	JSONObject json;
	JSONParser parser = new JSONParser();
	private String request;
	private String content;
	private String to;
	private String from;

	public ServerThread(ChatServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
		try {
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());

			// Vi har gjort så att ChatClient alltid skickar sitt username när
			// den connectar
			username = input.readUTF();

			// If the username already exists, it sends back false to the
			// chatclient
			// It exits the loop when the user enters a valid username
			while (server.userExists(username)) {
				output.writeUTF("false");
				username = input.readUTF();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Startar upp tråden genom att kalla på run()
		start();
	}

	public String getUsername() {
		return username;
	}

	public String getIP() {
		return clientSocket.getRemoteSocketAddress().toString();
	}

	public int getPort() {
		return clientSocket.getPort();
	}

	public void run() {
		try {
			while (true) {
				// Read client messages from input
				String message = input.readUTF();
				try {
					json = (JSONObject) parser.parse(message);
				} catch (org.json.simple.parser.ParseException e) {
					e.printStackTrace();
				}
				request = (String) json.get("REQUEST");
				content = (String) json.get("CONTENT");
				to = (String) json.get("TO");
				from = (String) json.get("from");

				if (request.compareTo("leave") == 0) {
					server.removeThread(this);
					break;
				}

				else if (request.compareTo("send_to_all") == 0) {
					server.sendMsgToAll(json);
				}

				else if (request.compareTo("whisper") == 0) {
					if (server.userExists(to)) {
						server.sendPrivateMessage(json);
					} else {
						JSONObject server_json = new JSONObject();
						server_json.put("REQUEST", "server_info");
						server_json.put("CONTENT", "That user does not exist");
						server_json.put("TO", username);
						server_json.put("FROM", "server");
						String sendmessage = server_json.toJSONString();
						output.writeUTF(sendmessage);
					}
				} else if (request.compareTo("send_file") == 0) {
				}

			}

		} catch (IOException ioe) {
			ioe.printStackTrace();

		} finally {
			// If this isn't done, many dead threads can be left
			server.removeThread(this);
		}

	}
}