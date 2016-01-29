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

	public JSONObject getJson(String request, String content, String to, String from) {
		JSONObject json = new JSONObject();
		json.put("REQUEST", request);
		json.put("CONTENT", content);
		json.put("TO", to);
		json.put("FROM", from);
		return json;
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
						JSONObject server_json = getJson("server_info", "That user does not exist", username, "server");
						output.writeUTF(server_json.toJSONString());
					}
				} else if (request.compareTo("send_file") == 0) {
					server.sendPrivateMessage(getJson("receive_file", "Wants to send you a file", to, from));
				}

				else if (request.compareTo("receive_file") == 0) {
					if (server.userExists(to)) {
						boolean loop = true;
						while (loop) {
							message = input.readUTF();
							JSONObject send_json = (JSONObject) parser.parse(message);
							String answer = (String) send_json.get("CONTENT");
							String fileSender = (String) json.get("FROM");
							if (answer.compareTo("yes") == 0) {
								JSONObject server_json = getJson("accept_file", username + " accepted the file request",
										fileSender, username);
								server.sendPrivateMessage(server_json);
								server_json = getJson("server_info", "You have accepted the clients request", username,
										"server");
								output.writeUTF(server_json.toJSONString());
								loop = false;
							} else if (answer.compareTo("no") == 0) {
								JSONObject server_json = getJson("reject_file", username + " rejected the file.",
										fileSender, username);
								server.sendPrivateMessage(server_json);
								server_json = getJson("server_info", "You have declined the clients request", username,
										"server");
								output.writeUTF(server_json.toJSONString());
								loop = false;
							} else {
								JSONObject server_json = getJson("server_info",
										"You need to input yes or no to the request: ", username, "server");
								output.writeUTF(server_json.toJSONString());
							}

						}

					} else {
						JSONObject server_json = getJson("server_info", "That user does not exist", username, "server");
						output.writeUTF(server_json.toJSONString());
					}
				}

			}

		} catch (IOException ioe) {
			ioe.printStackTrace();

		} catch (org.json.simple.parser.ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// Om inte detta görs kan det bli massa döda trådar kvar
			server.removeThread(this);
		}

	}
}