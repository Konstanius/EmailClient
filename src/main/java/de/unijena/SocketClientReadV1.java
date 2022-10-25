package de.unijena;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public abstract class SocketClientReadV1 {

    public static void main() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // Get the host that the user wants to connect to, default to pop3.uni-jena.de
        System.out.println("Enter the host you want to connect to ('pop3.uni-jena.de'): ");
        String host;
        while (true) {
            host = br.readLine();

            // if the input was empty, use the default value
            if (host.equals("")) {
                host = "pop3.uni-jena.de";
                break;
            }

            // if the input contains a space, it is invalid, else it is valid
            if (host.contains(" ")) {
                System.out.println("Host cannot contain spaces!");
            } else {
                break;
            }
        }

        // Get the port that the user wants to connect to, default to 110
        System.out.println("Enter the port you want to connect to (110): ");
        int portNumber = 110;
        while (true) {
            String port = br.readLine();

            // if the input was empty, use the default value
            if (port.equals("")) {
                break;
            }

            // if the input is not a number, it is invalid, else it is valid
            try {
                portNumber = Integer.parseInt(port);
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number. Please enter a valid port number or leave the field empty to use the default port (110): ");
            }
        }

        // Get the username that the user wants to use
        System.out.println("Enter your username ('max.mustermann'): ");
        String email;
        while (true) {
            email = br.readLine();

            // if the input is empty, it is invalid, else it is valid
            if (email.equals("")) {
                System.out.println("No username entered! Please enter your username ('max.mustermann'): ");
            } else {
                // if the username already contains @uni-jena.de, remove it
                email = email.replace("@uni-jena.de", "");
                break;
            }
        }
        // append the @uni-jena.de to the username, so that it is a valid email address
        email = email + "@uni-jena.de";

        // Get the password that the user wants to use
        System.out.println("Enter your password ('password'): ");
        String password;
        while (true) {
            password = br.readLine();

            // if the input is empty, it is invalid, else it is valid
            if (password.equals("")) {
                System.out.println("No password entered! Please enter your password: ");
            } else {
                break;
            }
        }

        // Initiate, connect and authenticate the client
        Client client = new Client();
        client.connect(host, portNumber);
        client.authenticate(email, password);
        System.out.println("Connected to " + host + " on port " + portNumber + " as " + email);

        // Print all message indexes, their date and subject
        System.out.println("========================================");
        client.printAllMessages();
        System.out.println("========================================");

        // Tell the user how many messages are in the inbox
        int totalAmount = client.getMessageAmount();
        System.out.println("Total amount of messages: " + totalAmount);

        // Listen for commands from the user
        while (true) {
            System.out.println("Enter the number of the message you want to read or close to exit: ");
            String command = br.readLine(); // read the command from the user

            // Check the command against known commands
            try {
                if (command.equals("close")) { // if the command is close, close the connection
                    System.out.println("========================================");
                    break;
                } else { // if the command is not close, try to parse it as an integer
                    System.out.println("========================================");
                    int messageNumber = Integer.parseInt(command); // parse the command as an integer
                    client.printMessage(messageNumber); // print the message with the given number
                    System.out.println("========================================");
                }
            } catch (Exception e) { // if the command is not an integer, print an error message
                System.out.println("Invalid input!");
                System.out.println("========================================");
            }
        }


        System.out.println("Closing connection..."); // tell the user that the connection is closing
        client.close(); // close the connection
    }

    private static class Client {
        /**
         * The socket that is used to connect to the server
         */
        Socket socket;

        /**
         * The buffered reader that is used to read the input from the server
         */
        BufferedReader reader;

        /**
         * The print writer that is used to write to the server
         */
        PrintWriter writer;

        /**
         * The last read line from the server
         */
        String line;

        /**
         * The constructor of the client
         */
        public Client() {}

        /**
         * Connects to the server
         * @param host The host that the client should connect to
         * @param port The port that the client should connect to
         * @throws Exception If the connection fails
         */
        public void connect(String host, int port) throws Exception {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            line = reader.readLine();
        }

        /**
         * Authenticates the client
         * @param email The email address of the user
         * @param password The password of the user
         * @throws IOException If the authentication fails
         */
        public void authenticate(String email, String password) throws IOException {
            writer.println("USER " + email);
            line = reader.readLine();
            writer.println("PASS " + password);
            line = reader.readLine();
        }

        /**
         * Prints all emails that are found in the inbox of the user
         * @throws IOException If the reading of the emails fails
         */
        public void printAllMessages() throws IOException {
            int numberOfMessages; // Amount of total messages

            writer.println("STAT"); // Get the amount of total messages (Returns: +OK <number of messages> <total size of messages>)
            line = reader.readLine(); // Read the response
            numberOfMessages = Integer.parseInt(line.split(" ")[1]); // Get the amount of total messages

            for (int i = 1; i <= numberOfMessages; i++) { // Loop through all messages
                System.out.print("[" + i + "] "); // Print the message number
                writer.println("RETR " + i); // Get the message (Returns: +OK message follows, <message>, .) (see https://de.wikipedia.org/wiki/Post_Office_Protocol)
                line = reader.readLine(); // Read the response

                boolean foundDate = false; // If the date has already been printed
                boolean foundSubject = false; // If the subject has already been printed

                String date = ""; // The date of the message
                String subject = ""; // The subject of the message

                while (!line.equals(".")) { // Loop through all lines of the message
                    if (line.startsWith("Date: ") && !foundDate) { // If the line starts with "Date: " and the date has not been printed yet
                        date = line.substring(6); // Get the date
                        foundDate = true; // Set the date to printed
                    }

                    if (line.startsWith("Subject: ") && !foundSubject) { // If the line starts with "Subject: " and the subject has not been printed yet
                        subject = line.substring(9); // Get the subject
                        foundSubject = true; // Set the subject to printed
                    }

                    line = reader.readLine(); // Read the next line
                }

                System.out.print("Date: " + date + ", "); // Print the date
                System.out.println("Subject: " + subject); // Print the subject
                System.out.println(); // Print a new line
            }
        }

        /**
         * Gets the total amount of messages in the inbox of the user
         * @return The total amount of messages in the inbox of the user
         * @throws IOException If the reading of the emails fails
         */
        int getMessageAmount() throws IOException {
            int numberOfMessages; // Amount of total messages
            writer.println("STAT"); // Get the amount of total messages (Returns: +OK <number of messages> <total size of messages>)
            line = reader.readLine(); // Read the response
            numberOfMessages = Integer.parseInt(line.split(" ")[1]); // Get the amount of total messages
            return numberOfMessages; // Return the amount of total messages
        }

        /**
         * Prints the message with the given number
         * @param messageNumber The number of the message that should be printed
         * @throws IOException If the reading of the emails fails
         */
        public void printMessage(int messageNumber) throws IOException {
            writer.println("RETR " + messageNumber); // Get the message (Returns: +OK message follows, <message>, .) (see https://de.wikipedia.org/wiki/Post_Office_Protocol)
            line = reader.readLine(); // Read the response
            while (!line.equals(".")) { // Loop through all lines of the message
                System.out.println(line); // Print the line
                line = reader.readLine(); // Read the next line
            }
        }

        /**
         * Closes the connection to the server
         * @throws IOException If the closing of the connection fails
         */
        public void close() throws IOException {
            writer.println("QUIT"); // Close the connection (Returns: +OK POP3 server signing off)
            line = reader.readLine(); // Read the response
            socket.close(); // Close the socket
        }
    }
}