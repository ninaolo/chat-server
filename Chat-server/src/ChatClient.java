import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * En klass för ett användargränssnitt som beskriver en klient till chatt-programmet.
 */
public class ChatClient {

	Socket clientSocket;
	DataInputStream serverInput;
	DataOutputStream output;
	BufferedReader clientInput;
	String username;
	JSONParser parser = new JSONParser();

	public ChatClient(String username, String serverIP, int port) {
		this.username = username;
		try {

			// Starta upp vår connection
			System.out.println("Trying to connect...");
			clientSocket = new Socket(serverIP, port);
			System.out.println("Successfully connected to port " + clientSocket.getPort() + " on host "
					+ clientSocket.getLocalAddress() + ".");

			// Hämta input och output-strömmar
			serverInput = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());
			clientInput = new BufferedReader(new InputStreamReader(System.in));

			// Skickar username på output så ServerThread kan hämta det
			username = this.getValidUserName(username);

			// Starta upp en tråd som lyssnar på meddelanden från servern
			new Thread(new ChatListener()).start();

		} catch (UnknownHostException e) {
			System.out.println("Unknown host");
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}

		while (true) {

			// Väntar på att klienten ska skriva meddelanden i chatten
			try {
				String message = clientInput.readLine() + "";

				JSONObject json = parseMessage(message);

				output.writeUTF(json.toJSONString());

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	/*
	 * Parses the written message and creates a JSON object to store the
	 * protocol values which later shall be sent to the server.
	 */
	private JSONObject parseMessage(String message) {

		JSONObject json = new JSONObject();

		if (message.compareTo("leave") == 0) {
			System.out.println("Goodbye " + username);
			System.exit(0);
			json.put("REQUEST", "leave");
			json.put("TO", "");
			json.put("FROM", username);
			json.put("CONTENT", "");
		}

		else if (message.length() < 3) {
			json.put("REQUEST", "send_to_all");
			json.put("TO", "");
			json.put("FROM", username);
			json.put("CONTENT", message);
		}

		else if (message.substring(0, 3).compareTo("/s ") == 0) {
			String sendToUser = getUser(message);
			String fileName = message.substring(3 + sendToUser.length(), message.length());
			json.put("REQUEST", "send_file");
			json.put("TO", sendToUser);
			json.put("FROM", username);
			json.put("CONTENT", fileName);
		}

		else if (message.substring(0, 3).compareTo("/w ") == 0) {
			String sendToUser = getUser(message);
			String content = message.substring(3 + sendToUser.length(), message.length());
			json.put("REQUEST", "whisper");
			json.put("TO", sendToUser);
			json.put("FROM", username);
			json.put("CONTENT", content);
		}

		else {
			json.put("REQUEST", "send_to_all");
			json.put("TO", "");
			json.put("FROM", username);
			json.put("CONTENT", message);
		}

		return json;
	}

	/*
	 * Parses the message to retrieve the username within it.
	 */
	private String getUser(String message) {
		String user = "";

		message = message.substring(3, message.length());
		for (int i = 0; i < message.length(); i++) {
			String c = message.substring(i, i + 1);
			if (c.compareTo(" ") == 0) {
				break;
			} else {
				user = user + c;
			}
		}
		return user;
	}

	private String getValidUserName(String user) {
		String valid = "";
		user = user.replaceAll("\\s+", "");
		try {
			output.writeUTF(user);
			valid = serverInput.readUTF();
			while (valid.compareTo("false") == 0) {
				System.out.print("Username already taken..., please pick another: ");
				user = clientInput.readLine();
				user = user.replaceAll("\\s+", "");
				output.writeUTF(user);
				valid = serverInput.readUTF();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return user;

	}

	/*
	 * En privat klass som gör att vi kan ha en kontinuerlig tråd som kör och
	 * väntar på att meddelanden ska komma från servern.
	 */
	private class ChatListener extends Thread {

		public void run() {

			try {

				// Wait for requests/messages from other clients or the server.
				while (true) {

					String message = serverInput.readUTF();

					JSONObject json = (JSONObject) parser.parse(message);

					String request = (String) json.get("REQUEST");
					String to = (String) json.get("TO");
					String from = (String) json.get("FROM");
					String content = (String) json.get("CONTENT");

					if (request.compareTo("send_to_all") == 0) {
						System.out.println(from + ": " + content);
					}

					else if (request.compareTo("whisper") == 0) {
						System.out.println(from + " (whisper): " + content);
					}

					else if (request.compareTo("server_info") == 0) {
						System.out.println(content);
					}

					else if (request.compareTo("send_file") == 0) {
						Scanner scanner = new Scanner(System.in);
						System.out.println("### " + from
								+ " wants to send a file to you. Do you accept the request? Type yes/no. ###");
						while (true) {
							String answer = scanner.nextLine();
							if (answer.compareTo("yes") == 0) {
								break;
							} else if (answer.compareTo("no") == 0) {
								break;
							} else {
								System.out.println("Please type 'yes' or 'no'.");
							}
						}
					}

					else if (request.compareTo("accept_file") == 0) {
						System.out.println("### " + from + " accepted your request to send a file. ###");

					} else if (request.compareTo("reject_file") == 0) {
						System.out.println("### " + from + " rejected your request to send a file. ###");
					}
				}

			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (ParseException p) {
				p.printStackTrace();
			}
		}
	}

	/*
	 * 127.0.0.1 är den lokala datorn och args[0] är ett användarnamn som man
	 * får välja
	 */
	public static void main(String[] args) {
		new ChatClient(args[0], args[1], 1337);
	}

}
