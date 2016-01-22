import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * En klass för ett användargränssnitt som beskriver en klient till chatt-programmet.
 */
public class ChatClient extends Thread {

	Socket clientSocket;
	DataInputStream input;
	DataOutputStream output;

	public ChatClient(String serverIP, int port) {

		try {

			// Starta upp vår connection
			System.out.println("Trying to connect...");
			clientSocket = new Socket(serverIP, port);
			System.out.println("Successfully connected to " + clientSocket);

			// Hämta input och output-strömmar
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());

			// Starta upp client-tråden
			new Thread(this).start();

		} catch (UnknownHostException e) {
			System.out.println("Unknown host");
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {

		try {

			while (true) {
				// Hämta meddelanden hela tiden och printa ut
				String message = input.readUTF();
				System.out.println(message);
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new ChatClient("127.0.0.1", 1337);
	}

}
