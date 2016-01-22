import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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

	public void run() {

		try {

			while (true) {

				// Läs klienternas meddelanden från input
				String message = input.readUTF();

				// Skickar ut meddelandet till alla klienter
				server.sendMsgToAll(username + ": " + message);

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