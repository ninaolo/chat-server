import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/*
	 * Privat klass för en server-tråd som hanterar varje separat klient i
	 * chat-programmet.
	 */
public class ServerThread extends Thread {

	private ChatServer server;
	Socket clientSocket;
	DataInputStream input;
	DataOutputStream output;

	public ServerThread(ChatServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
		try {
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Startar upp tråden genom att kalla på run()
		start();
	}

	public void run() {

		try {

			while (true) {

				// Läs meddelanden från input
				String message = input.readUTF();

				System.out.println("Sending your message to all other clients...");

				// Skickar ut meddelandet till alla klienter
				server.sendMsgToAll(message);

				if (message.compareTo("leave") == 0) {
					server.removeThread(this);
					break;
				}
			}

		} catch (IOException ioe) {
			ioe.printStackTrace();

		} finally {
			// Om inte detta görs kan det bli massa döda trådar kvar
			server.removeThread(this);
		}

	}
}