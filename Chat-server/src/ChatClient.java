import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * Represents a client which can connect to a chat server.
 */
public class ChatClient {

	Socket clientSocket;
	DataInputStream serverInput;
	DataOutputStream output;
	BufferedReader clientInput;
	String username;
	JSONParser parser = new JSONParser();
	int clientPort = 5555; // Port used for sending byte files
	String clientIP;

	public ChatClient(String username, String serverIP, int port) throws IOException {

		this.username = username;

		this.clientIP = InetAddress.getLocalHost().getHostAddress();

		System.out.println("### Trying to connect... ###");
		clientSocket = new Socket(serverIP, port);
		System.out.println("### Successfully connected to chat server port " + clientSocket.getPort() + " on host "
				+ clientSocket.getLocalAddress() + ". ###");

		// Get input and output streams
		serverInput = new DataInputStream(clientSocket.getInputStream());
		output = new DataOutputStream(clientSocket.getOutputStream());
		clientInput = new BufferedReader(new InputStreamReader(System.in));

		// Sends username on output so that ServerThread can save it
		username = this.getValidUserName(username);

		// Start a thread which listens for chat messages
		new Thread(new ChatListener()).start();

		// Waits for the client to write messages in the chat
		while (true) {
			try {
				String message = clientInput.readLine() + "";

				JSONObject json = parseMessage(message);
				if (!json.isEmpty()) {

					output.writeUTF(json.toJSONString());

					if (((String) json.get("REQUEST")).compareTo("leave") == 0) {
						System.exit(0);
					}
				}
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
			if (message.length() <= 3 + sendToUser.length() + 1) {
				System.out.println("The /s command has to be on this format: /s <username> <path/to/file.txt>");
			} else {
				String fileName = message.substring(3 + sendToUser.length() + 1, message.length());
				File file = new File(fileName);
				if (!file.exists()) {
					System.out.println("The file you wish to send does not exist.");
				} else {
					json.put("REQUEST", "send_file");
					json.put("TO", sendToUser);
					json.put("FROM", username);
					json.put("CONTENT", fileName);
				}
			}
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
	 * Parses the message to retrieve the username within it. A message has to
	 * have at least three characters for a command ("/s ","/w ") that uses a
	 * username after.
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

	/*
	 * Handles username input if a username is written which is not valid. This
	 * is communicated back and forth with the server. The server checks if the
	 * user exists and sends back a "true" if the username is valid and a
	 * "false" if the username exists. This is done before the serverthreads run
	 * method is invoked.
	 */
	private String getValidUserName(String user) {
		String valid = "";
		user = user.replaceAll("\\s+", "");
		try {
			output.writeUTF(user);
			valid = serverInput.readUTF();
			while (valid.compareTo("false") == 0) {
				System.out.print("### Username already taken, please pick another: ###");
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
	 * A private thread class which (when run() is invoked) waits for messages
	 * from the server and takes care of handling different incoming requests.
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
						if (from.compareTo("") == 0) {
							System.out.println(content);
						} else {
							System.out.println(from + " (whisper): " + content);
						}
					}

					else if (request.compareTo("server_info") == 0) {
						System.out.println(content);
					}

					else if (request.compareTo("receive_file") == 0) {
						System.out.println(
								"### User " + from + " wants to send a file to you. To answer, type yes/no. ###");
						output.writeUTF(json.toJSONString());
					}

					else if (request.compareTo("accept_file") == 0) {
						System.out.println("### " + from + " accepted your request to send a file. ###");

						JSONObject connectionInfo = new JSONObject();
						connectionInfo.put("REQUEST", "send_address");
						connectionInfo.put("TO", from);
						connectionInfo.put("FROM", username);
						connectionInfo.put("CONTENT", clientIP + ":" + clientPort);

						sendBinaryFile(new File(content), connectionInfo);

					} else if (request.compareTo("reject_file") == 0) {
						System.out.println("### " + from + " rejected your request to send a file. ###");

					} else if (request.compareTo("send_address") == 0) {
						String[] connectionInfo = content.split(":");
						String IP = connectionInfo[0];
						int port = Integer.parseInt(connectionInfo[1]);
						receiveBinaryFile(IP, port);
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
	 * Sends an arbitrary binary file on the output stream for someone else to
	 * fetch.
	 */
	private void sendBinaryFile(File file, JSONObject connectionInfo) {

		if (!file.exists()) {
			System.out.println("### The file you wish to send does not exist. ###");

		} else {
			System.out.println("### Trying to send file... ###");
			try {
				ServerSocket clientServerSocket = new ServerSocket(clientPort);

				// Send connection info to client which shall receive the file
				output.writeUTF(connectionInfo.toJSONString());

				// Wait for client to start downloading the file
				Socket otherClientSocket = clientServerSocket.accept();
				ObjectOutputStream outStream = new ObjectOutputStream(otherClientSocket.getOutputStream());
				byte[] fileContent = Files.readAllBytes(file.toPath());

				// This is for sending the file information (name, size etc.)
				outStream.writeObject(file);

				// This is for sending the file content
				outStream.writeObject(fileContent);

				clientServerSocket.close();
				System.out.println("### Successfully sent file! ###");

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Opens a socket and attempts to receive a binary file from input stream.
	 */
	private void receiveBinaryFile(String IP, int port) {
		System.out.println("### Trying to download file...###");
		try {
			Socket socket = new Socket(IP, port);
			System.out.println("### Successfully connected to IP " + IP + ". Downloading file... ###");
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			File sentFile = (File) input.readObject();
			byte[] fileContent = (byte[]) input.readObject();
			File receivedFile = new File(System.getProperty("user.home") + "/" + sentFile.getName());
			Files.write(receivedFile.toPath(), fileContent);
			socket.close();
			System.out.println("### Successfully downloaded file to: [" + receivedFile.getAbsolutePath() + "]. ###");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/*
	 * args[0] is a username and args[1] is the host address. The port 1337 is
	 * hard coded for simplicity reasons.
	 */
	public static void main(String[] args) throws IOException {
		new ChatClient(args[0], args[1], 1337);
	}

}
