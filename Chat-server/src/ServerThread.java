import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/*
 * Klass för en server-tråd som sköter kommunikationen med en klient.
 */
public class ServerThread extends Thread {

	private ChatServer server;
	Socket clientSocket;
	DataInputStream input;
	DataOutputStream output;
	public String username;
	JSONObject json;
	JSONParser parser = new JSONParser();
	String request;
	String content;
	String to;
	String from;
	

	public ServerThread(ChatServer server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
		try {
			input = new DataInputStream(clientSocket.getInputStream());
			output = new DataOutputStream(clientSocket.getOutputStream());

			// Vi har gjort så att ChatClient alltid skickar sitt username när
			// den connectar
			username = input.readUTF();
			
			//If the username already exists, it sends back false to the chatclient
			//It exits the loop when the user enters a valid username
			while(server.userExists(username)){
				output.writeUTF("false");
				username = input.readUTF();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Startar upp tråden genom att kalla på run()
		start();
	}
	
	public String getUsername(){
		return username;
	}
	

	
	private String getUser(String message){
		String user = "";
		
		message = message.substring(3,message.length());
		for(int i = 0;i<message.length();i++){
			String c = message.substring(i,i+1);
			if(c.compareTo(" ")==0){
				break;
			}
			else{
				user = user+c;
			}
		}
		return user;
	}



	public void run() {
		try {
			while (true) {
				// Läs klienternas meddelanden från input
				String message = input.readUTF();
				try{
					json = (JSONObject) parser.parse(message);
				} catch (org.json.simple.parser.ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				request = (String) json.get("REQUEST");
				content = (String) json.get("CONTENT");
				to = (String) json.get("TO");
				from = (String) json.get("from");
				
				
				
				
				//Tar bort klienten
				if(request.compareTo("leave")==0){
					server.removeThread(this);
					break;
				}
				
				else if (request.compareTo("send_to_all")==0){
					server.sendMsgToAll(json);
				}
				
				
				//Skicka privat meddelande om första tre i meddelandet = "/w "
				else if (request.compareTo("whisper")==0){
					if(server.userExists(to)){
						server.sendPrivateMessage(json);
					}
					else{
						JSONObject server_json = new JSONObject();
						server_json.put("REQUEST", "server_info");
						server_json.put("CONTENT", "That user does not exist");
						server_json.put("TO", username);
						server_json.put("FROM","server");
						String sendmessage = server_json.toJSONString();
						output.writeUTF(sendmessage);
					}
				}
				else if (request.compareTo("send_file")==0){
					
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