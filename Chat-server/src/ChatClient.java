import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

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
	Frame fileFrame = new Frame(); // Used for displaying file option

	public ChatClient(String username, String serverIP, int port) {

		this.username = username;

		try {

			this.clientIP = InetAddress.getLocalHost().toString();

			// Start connection to chat server
			System.out.println("Trying to connect...");
			clientSocket = new Socket(serverIP, port);
			System.out.println("Successfully connected to port " + clientSocket.getPort() + " on host "
					+ clientSocket.getLocalAddress() + ".");

			// get input and output streams
			serverInput = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());
			clientInput = new BufferedReader(new InputStreamReader(System.in));

			// Sends username on output so that ServerThread can save it
			username = this.getValidUserName(username);

			// Start a thread which listened for chat messages
			new Thread(new ChatListener()).start();

		} catch (UnknownHostException e) {
			System.out.println("Unknown host");
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Waits for the client to write messages in the chat
		while (true) {
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

						JSONObject connectionInfo = new JSONObject();
						connectionInfo.put("REQUEST", "send_address");
						connectionInfo.put("TO", from);
						connectionInfo.put("FROM", username);
						connectionInfo.put("CONTENT", clientIP + ":" + clientPort);
						output.writeUTF(connectionInfo.toJSONString());
						sendBinaryFile(new File(content));

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
	private void sendBinaryFile(File file) {

		if (!file.exists()) {
			System.out.println("The file you wish to send does not exist.");

		} else {

			try {
				ServerSocket clientServerSocket = new ServerSocket(clientPort);
				Socket otherClientSocket = clientServerSocket.accept();
				ObjectOutputStream outStream = new ObjectOutputStream(otherClientSocket.getOutputStream());
				outStream.writeObject(file);
				clientServerSocket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * Opens a socket and attempts to receive a binary file from input stream.
	 */
	private void receiveBinaryFile(String IP, int port) {
		try {
			Socket socket = new Socket(IP, port);
			System.out.println("Successfully connected to IP " + IP + ". Trying to download file...");
			ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			File file = (File) input.readObject();
			System.out.println("Successfully downloaded file: [" + file.getName() + "].");

			// Opens a file dialog where you can choose a destination for the
			// file
			FileDialog fd = new FileDialog(fileFrame, "Choose a destination", FileDialog.LOAD);
			fd.setDirectory(null);
			fd.setFile(file.getName());
			fd.setVisible(true);

			// Copies the file object to the chosen path
			FileInputStream fileIn = null;
			FileOutputStream fileOut = null;
			fileIn = new FileInputStream(file);
			fileOut = new FileOutputStream(fd.getDirectory() + fd.getFile());
			int read;
			while ((read = fileIn.read()) != -1) {
				fileOut.write(read);
			}
			fileOut.close();
			fileIn.close();
			socket.close();

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
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
