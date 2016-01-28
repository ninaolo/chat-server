
# lab 1

## How to run the chat server
1. Open three terminal windows
2. Go to `labs-DD2390/Chat-server/bin`
3. In the first terminal window, type `java ChatServer`
4. In the second and third window, type `java ChatClient` followed by a username of your choice
5. Done! Whenever you type messages from one of the users, the others will see it together with the sender's username

##Questions
1. If you have n connected clients, how many instances of thread are needed by the: Server? Client?
2. What does the Java keyword synchronized do?
3. What is a runnable in Java?
4. Describe the four layers in the TCP/IP protocol stack.
5. What does the flags, ACK, SYN and SEQ mean and what protocol do
they belong to?
6. What is the difference between TCP and UDP?


##Answers
1. The `ChatServer` has one main thread waiting for clients to connect and then creates one thread for every new client. This results in n+1 threads. The `ChatClient` has one main thread waiting for input by the user, and one thread (which we have called `ChatListener`) waiting for messages from other clients or the server.
2. ...
3. ...
4. ...
5. These belong to the TCP protocol. ACK and SYN are flags which can be set in the TCP header. There are also two important values in the header: acknowledgement number and sequence (SEQ) number, and these are not flags. To establish a connection, TCP uses a so-called `three-way handshake`.
.. * SYN: The client wants to connect to the server and sets the sequence number to a random value (A).
.. * SYN-ACK: The server replies and sets the acknowledgement number to A+1 and SEQ to another random number (B).
.. * ACK: The client sends ACK back to the server with an acknowledgement number of B+1 and a SEQ of A+1.
6. ...



