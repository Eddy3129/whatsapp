# Chat Application Setup Guide

This guide provides step-by-step instructions to set up and run the chat application, including the server and multiple client instances. Follow the steps below to get started.

---

## 1. Clone the Repository

First, clone the repository to your local machine using the following command:

```bash
git clone https://github.com/Eddy3129/whatsapp.git
```

## 2. Open in IntelliJ IDEA

1. Open IntelliJ IDEA.
2. Click on **File > Open** and navigate to the folder where you cloned the repository.
3. Select the folder and click **OK** to open the project.
4. Ensure that all dependencies are resolved by letting IntelliJ sync the project (e.g., Maven or Gradle).

---

## 3. Enable Multiple Instances for ClientApp

To allow multiple instances of the `ClientApp` to run simultaneously:

1. In IntelliJ IDEA, go to **Run > Edit Configurations**.
2. Locate the **ClientApp** configuration.
3. Modify options -> Check the box **"Allow multiple instances"**.
4. Click **Apply** and **OK** to save the changes.

---

## 4. Run the Server

1. Locate the `ServerApp` class in the `org.example` package.
2. Right-click on the `ServerApp` class and select **Run 'ServerApp'**.
3. The server will start on port `25520` with the default configuration.

If needed, verify the server is running by checking the console output. You should see:

```
Chat server started on port 25520
Waiting for clients...
```

---

## 5. Change Client Application Configuration for Multiple Instances

Each client instance must have a unique configuration. To set this up:

1. Locate the `application.conf` file used by the client in the `resources` folder.
2. Modify the `canonical.port` value to `0`:

   ```hocon
   akka.remote.artery.canonical.port = 0
   ```
   This allows the client to use a random available port when connecting to the server.

3. Save the changes.

---

## 6. Run Multiple Client Instances

1. Locate the `ClientApp` class in the `org.example` package.
2. Right-click on the `ClientApp` class and select **Run 'ClientApp'**.
3. Repeat this step to launch as many client instances as you need.

When prompted, enter a unique username for each client.

---

## 7. Start Chatting

## 8. Group Chatting
For group chat,
1. Use the client UI to create groups, invite members, and send messages.
2. Available commands are displayed in the client interface, including:
   - `/create <group_name>`: Create a new group.
   - `/join <group_name>`: Join an existing group.
   - `/invite <username>`: Invite a user to the group.
   - `/members`: View the current group members.
   - `/leave`: Leave the group.
   - `/help`: Display available commands.

---

## Example Workflow

1. Start the server.
2. Run multiple client instances.
3. In one client instance, create a group:

   ```
   /create Malaysia
   ```

4. Invite another user:

   ```
   /invite Akao
   ```

5. Other users can join the group with:

   ```
   /join Malaysia
   ```

6. Start chatting in the group or send direct messages.

---

## Notes

- Ensure that the server is running before starting any client instances.
- Each client must have a unique username.
- The `application.conf` file for the client must have the `canonical.port` set to `0` to allow multiple instances.
- For troubleshooting, check the logs in the IntelliJ console for any errors or warnings.

---

Enjoy using your chat application!

