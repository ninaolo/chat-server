
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

3. The `java.lang.Runnable` is an interface in java which a class can implement. To use it, one can create a java `Thread` and pass the Runnable to it. When calling `Thread.start()` it calls the interface's `run()` method which executes a separate thread (an independent path of execution) which can be used to do things in parallel. One can also extend the class `java.lang.Thread` instead of using the interface, but this is basically the same thing. There are some differences (for example, multiple inheritance is not allowed in java and you can therefore not inherit from any other class if you inherit from Thread) but we will not discuss this further.

4. The TCP/IP protocol stack has four layers:
  * **Application:** This layer defines the application protocols and how host programs create user data and communicate this to other applications by making use of the services in the lower layers (especially the transport layer). Examples: FTP, HTTP and SSH.
  * **Transport:** Performs communication between hosts on either local or remote networks. Examples: TCP and UDP (see description in question 6).
  * **Internet:** Exchanges data across network boundaries and packages the data into IP datagrams/packets, containing source and destination address. The primary protocol is the Internet Protocol (IP). It performs routing by transporting datagrams to the next IP router closer to the final destination.
  * **Network Access:** This layer has details of how data is physically sent through a network. This includes mapping IP addresses to physical hardware addresses (MAC) and protocols for the physical data transfer to coaxial cables, optical fiber and so on. Example: ethernet.

5. These belong to the TCP protocol. ACK and SYN are flags which can be set in the TCP header. There are also two important values in the header: acknowledgement number and sequence (SEQ) number, and these are not flags. To establish a connection, TCP uses a so-called `three-way handshake`.
  * **SYN:** The client wants to connect to the server and sets the sequence number to a random value (A).
  * **SYN-ACK:** The server replies and sets the acknowledgement number to A+1 and SEQ to a random number (B).
  * **ACK:** The client sends ACK back to the server with an acknowledgement number of B+1 and a SEQ of A+1.

6. They are both protocols which work at the transport layer, but differ on a few points:
  * **Reliability:** If connection fails, TCP will retry to send the packages to get them delivered, whereas with UDP data could get lost on the way. TCP does error checking and UDP does not.
  * **Handshake:** TCP does the SYN, SYN-ACK, ACK (described above) but UDP has no handshake.
  * **Order:** TCP keeps track of the order in which data is sent, but UDP doesn't.
  * **Streaming:** TCP is sent as a stream and UDP is sent package by package.
  * **Size:** TCP header size is big (20 bytes) and UDP is small (8 bytes).
  * **Usage:** Because of the above, TCP should be used in cases where high reliability is required and transmission time is less important (for example email). UDP should on the other hand be used when you need something lighweight which transports data efficiently, like in games or when streaming videos (and it isn't the end of the world if one bit here and there is in the wrong order or gets lost on the way...)



