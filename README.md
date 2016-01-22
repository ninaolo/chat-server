# labs-DD2390

## How to run the chat server
1. Open three terminal windows
2. Go to `labs-DD2390/Chat-server/bin`
3. In the first terminal window, type `java ChatServer`
4. In the second and third window, type `java ChatClient` followed by a username of your choice
5. Done! Whenever you type messages from one of the users, the others will see it together with the sender's username.

##How to import into Eclipse
1. To clone the repository from GitHub, run this in a terminal:
   `git clone git@github.com:ninaolo/labs-DD2390.git`
2. In Eclipse, import the project by first doing
`File -> New -> Java Project`

3. Deselect "Use default location" and find the cloned repository.
4. Click "Next" and under "Libraries", do:
  * Click "Add Library..."
  * Select JUnit
  * Change version to JUnit 4 in the dropdown list.
5. Done!
