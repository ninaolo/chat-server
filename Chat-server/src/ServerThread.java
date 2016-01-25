import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Klass för en server-tråd som sköter kommunikationen med en klient.
 */
public class ServerThread extends Thread {

	private ChatServer server;
	Socket clientSocket;
	DataInputStream input;
	DataOutputStream output;
	public String username;

	public ServerThread(ChatServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
		try {
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());

			// Vi har gjort så att ChatClient alltid skickar sitt username när
			// den connectar
			username = input.readUTF();

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

	public void run() {

		try {

			while (true) {

				// Läs klienternas meddelanden från input
				String message = input.readUTF();

				// If a client wants to leave
				if (message.compareTo("leave") == 0) {
					server.removeThread(this);
					break;
				}
				// Skicka privat meddelande om första tre i meddelandet = "/w "
				else if (message.substring(0, 3).compareTo("/w ") == 0) {

					// Få ut vilken användare som meddelandet ska skickas till
					// (efter "/w ")
					String sendToUser = getUser(message);

					// Kolla om användaren existerar, om den gör det så skickar
					// den.
					if (server.userExists(sendToUser)) {
						message = message.substring(3 + sendToUser.length(), message.length());
						server.sendPrivateMessage(sendToUser, "Private message from " + username + ":" + message);
					} else {
						output.writeUTF("That user does not exist");
					}

					// If we want to send a file to another user
				} else if (message.substring(0, 3).compareTo("/send ") == 0) {
					String sendToUser = getUser(message);
					if (server.userExists(sendToUser)) {
						String fileName = message.substring(3 + sendToUser.length(), message.length());
						// TODO: fix send request to other user
					} else {
						output.writeUTF("That user does not exist");
					}

					// If we want to just send a normal message
				} else {
					server.sendMsgToAll(username + ": " + message);
				}

			}

		} catch (IOException ioe) {
			ioe.printStackTrace();

		} finally {
			// Om inte detta görs kan det bli massa döda trådar kvar
			server.removeThread(this);
		}

	}

	/*
	 * OBS: denna ska flyttas...
	 */
	public void answerToFileRequest(String username) {
		while (true) {
			try {
				output.writeUTF(username + " wants to send a file to you. Do you accept? yes/no");
				String answer = input.readUTF();
				if (answer.compareTo("yes") == 0) {
					break;
				} else if (answer.compareTo("no") == 0) {
					break;
				} else {
					output.writeUTF("Please type 'yes' or 'no'.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/*
	 * Sends an arbitrary binary file on the output stream to a given client.
	 */
	public void sendBinaryFile(File file, String clientIP, int port) {
		try {
			Socket socket = new Socket(clientIP, port);
			BufferedOutputStream outStream = new BufferedOutputStream(socket.getOutputStream());
			BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[4096]; // 4096 is a common buffer size
			for (int nrOfBytesRead = fileInput.read(buffer); nrOfBytesRead >= 0; nrOfBytesRead = fileInput
					.read(buffer)) {
				output.write(buffer, 0, nrOfBytesRead);
			}
			fileInput.close();

		} catch (UnknownHostException e) {
			System.out.println("The host does not exist.");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}