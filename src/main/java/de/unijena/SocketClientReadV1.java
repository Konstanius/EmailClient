package de.unijena;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Base64;

public abstract class SocketClientReadV1 {

    public static void main() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // Get the host that the user wants to connect to, default to pop3.uni-jena.de
        System.out.println("[OPTIONAL] Enter the host you want to connect to ('pop3.uni-jena.de'): ");
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

        // Check whether the user wants to connect with or without SSL
        System.out.println("[REQUIRED] Do you want to connect with SSL? (y/n): ");
        boolean secure = false;
        while (true) {
            String secureInput = br.readLine();

            // if the input is not a number, it is invalid, else it is valid
            if (secureInput.equalsIgnoreCase("y")) {
                secure = true;
                break;
            } else if (secureInput.equalsIgnoreCase("n")) {
                break;
            } else {
                System.out.println("Invalid input. Please enter 'y' or 'n': ");
            }
        }

        // Get the port that the user wants to connect to, default to 110 / 995 (depending on whether SSL is used)
        System.out.println("[OPTIONAL] Enter the port you want to connect to ('" + (secure ? "995" : "110") + "'): ");
        int portNumber = secure ? 995 : 110;
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
                System.out.println("Invalid port number. Please enter a valid port number or leave the field empty to use the default port (" + (secure ? "995" : "110") + "): ");
            }
        }

        // Get the username that the user wants to use
        System.out.println("[REQUIRED] Enter your username ('max.mustermann'): ");
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
        System.out.println("[REQUIRED] Enter your password ('password'): ");
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
        client.connect(host, portNumber, secure);
        client.authenticate(email, password);
        System.out.println("Connected to " + host + " on port " + portNumber + " as " + email);

        // Print all message indexes, their date and subject
        System.out.println("================================================================================");
        client.printAllMails();
        System.out.println("================================================================================");

        // Tell the user how many messages are in the inbox
        int totalAmount = client.getMailAmount();
        System.out.println("Total amount of messages: " + totalAmount);

        // Listen for commands from the user
        while (true) {
            System.out.println("Enter the number of the message you want to read or close to exit: ");
            String command = br.readLine(); // read the command from the user

            // Check the command against known commands
            try {
                if (command.equals("close")) { // if the command is close, close the connection
                    System.out.println("================================================================================");
                    break;
                } else { // if the command is not close, try to parse it as an integer
                    System.out.println("================================================================================");
                    int messageNumber = Integer.parseInt(command); // parse the command as an integer
                    client.printMail(messageNumber); // print the message with the given number
                    System.out.println("================================================================================");
                }
            } catch (NumberFormatException e) { // if the command is not an integer, print an error message
                System.out.println("Invalid input!");
                System.out.println("================================================================================");
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
         * The secure socket that is used to connect to the server
         */
        SSLSocket sslSocket;

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
         * Whether to use SSL or not
         */
        boolean secure;

        /**
         * The constructor of the client
         */
        public Client() {}

        /**
         * Connects to the server
         * @param host The host that the client should connect to
         * @param port The port that the client should connect to
         * @throws IOException If the connection fails
         */
        public void connect(String host, int port, boolean secure) throws IOException {
            this.secure = secure;
            if (secure) {
                SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                sslSocket = (SSLSocket) factory.createSocket(host, port);
                sslSocket.setKeepAlive(true);
                reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                writer = new PrintWriter(sslSocket.getOutputStream(), true);
            } else {
                socket = new Socket(host, port);
                socket.setKeepAlive(true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
            }
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
            if (!line.startsWith("+OK")) {
                throw new IOException("Authentication failed!");
            }
        }

        /**
         * Prints all emails that are found in the inbox of the user
         * @throws IOException If the reading of the emails fails
         */
        public void printAllMails() throws IOException {
            int numberOfMessages; // Amount of total messages

            writer.println("STAT"); // Get the amount of total messages (Returns: +OK <number of messages> <total size of messages>)
            line = reader.readLine(); // Read the response
            numberOfMessages = Integer.parseInt(line.split(" ")[1]); // Get the amount of total messages

            System.out.println(); // Print a new line
            for (int i = 1; i <= numberOfMessages; i++) { // Loop through all messages
                System.out.print("[" + i + "] "); // Print the message number
                writer.println("RETR " + i); // Get the message (Returns: +OK message follows, <message>, .) (see https://de.wikipedia.org/wiki/Post_Office_Protocol)
                line = reader.readLine(); // Read the response

                boolean foundDate = false; // If the date has already been printed
                boolean foundSubject = false; // If the subject has already been printed

                String date = ""; // The date of the message
                StringBuilder subject = new StringBuilder(); // The subject of the message

                while (!line.equals(".")) { // Loop through all lines of the message
                    if (line.toLowerCase().startsWith("date: ") && !foundDate) { // If the line starts with "Date: " and the date has not been printed yet
                        date = line.substring(6); // Get the date
                        foundDate = true; // Set the date to printed
                    }

                    if (line.startsWith("Subject: ") && !foundSubject) { // If the line starts with "Subject: " and the subject has not been printed yet
                        subject = new StringBuilder(line.substring(9)); // Get the subject
                        foundSubject = true; // Set the subject to printed

                        // while the line starts with " " it still belongs to the subject
                        do {
                            line = reader.readLine(); // Read the next line
                            if (line.startsWith(" ")) { // If the line starts with " "
                                subject.append(line.substring(1)); // Append the line to the subject
                            }
                        } while (line.startsWith(" ")); // If the next line starts with "Subject: ", read the next line
                        continue;
                    }

                    line = reader.readLine(); // Read the next line
                }

                // date is of format: "Wed, 21 Oct 2015 12:34:56 +0200 (CEST)"
                // remove everything after 5th space
                String[] dateParts = date.split(" "); // split the date into parts
                date = dateParts[0] + " " + dateParts[1] + " " + dateParts[2] + " " + dateParts[3] + " " + dateParts[4]; // get the first 5 parts of the date

                System.out.print("Date: " + date + ", "); // Print the date
                System.out.println("Subject: " + anyDecode(subject.toString())); // Print the subject
                System.out.println(); // Print a new line
            }
        }

        /**
         * Gets the total amount of messages in the inbox of the user
         * @return The total amount of messages in the inbox of the user
         * @throws IOException If the reading of the emails fails
         */
        int getMailAmount() throws IOException {
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
        public void printMail(int messageNumber) throws IOException {
            writer.println("RETR " + messageNumber); // Get the message (Returns: +OK message follows, <message>, .) (see https://de.wikipedia.org/wiki/Post_Office_Protocol)

            String sender = ""; // The sender of the message
            String date = ""; // The date of the message
            String receiver = ""; // The receiver of the message
            String subject = ""; // The subject of the message
            StringBuilder body = new StringBuilder(); // The body of the message

            boolean bodyMode = false; // If the body of the message is being read
            while (true) { // Loop through all lines of the message
                String newLine = reader.readLine(); // Read the next line

                if (newLine.equals(".")) { // If the line is a dot, break the loop
                    break;
                }

                // if the newLine starts with "    " or " ", add newLine to line
                if ((newLine.startsWith("   ") || newLine.startsWith(" ") || newLine.startsWith("\t")) && !bodyMode) {
                    line += newLine;
                } else {
                    if (line.startsWith("-ERR")) { // If the line starts with "-ERR"
                        System.out.println("Message not found!"); // Print an error message
                        break; // Break the loop
                    } else if (line.equals(".")) { // If the line is "."
                        break; // Break the loop
                    }

                    // avoid image base64 blocks because of overflows
                    if (line.toLowerCase().startsWith("content-transfer-encoding: base64") || line.toLowerCase().startsWith("content-type: image/")) {
                        while (!line.equals(".")) { // Loop through all lines of the message
                            line = reader.readLine(); // Read the next line
                        }
                        break;
                    }

                    if (line.toLowerCase().startsWith("from: ")) { // If the line starts with "From: ", split by <
                        sender = line.substring(6).trim(); // Get the sender
                        if (sender.contains("<")) {
                            sender = sender.substring(sender.indexOf("<") + 1, sender.indexOf(">"));
                        }
                    }

                    else if (line.toLowerCase().startsWith("date: ")) { // If the line starts with "Date: "
                        String[] dateParts = line.substring(6).split(" "); // split the date into parts
                        date = dateParts[0] + " " + dateParts[1] + " " + dateParts[2] + " " + dateParts[3] + " " + dateParts[4]; // get the first 5 parts of the date
                    }

                    else if (line.toLowerCase().startsWith("to: ")) { // If the line starts with "To: ", split by <
                        receiver = line.substring(4).trim(); // Get the receiver
                        if (receiver.contains("<")) {
                            receiver = receiver.substring(receiver.indexOf("<") + 1, receiver.indexOf(">"));
                        }
                    }

                    else if (line.toLowerCase().startsWith("subject: ")) { // If the line starts with "Subject: "
                        subject = anyDecode(line.substring(9)); // Get the subject
                    }

                    else if (line.toLowerCase().startsWith("content-type: text/")) { // If the line starts with "Content-Type: text/plain; charset="
                        bodyMode = true; // Set the body mode to true
                    }

                    if (bodyMode) { // If the body mode is true
                        body.append(line).append("\n"); // Add the line to the body
                    }

                    line = newLine;
                }
            }

            // Print the sender, date, receiver, subject and body
            System.out.println("Date: " + date);
            System.out.println("Sender: " + sender);
            System.out.println("Receiver: " + receiver);
            System.out.println("Subject: " + subject);
            System.out.println("======================== Body =============================");
            System.out.println(body);
        }

        /**
         * Closes the connection to the server
         * @throws IOException If the closing of the connection fails
         */
        public void close() throws IOException {
            writer.println("QUIT"); // Close the connection (Returns: +OK POP3 server signing off)
            line = reader.readLine(); // Read the response
            if (secure) { // If the connection is secure
                sslSocket.close(); // Close the socket
            } else { // If the connection is not secure
                socket.close(); // Close the socket
            }
        }
    }

    /**
     * Decodes a given "Subject" String, based on the format that is provided (UTF-8 / utf-8,  iso-8859-1, plaintext), as denoted by "=?(charset)?(encoding)?(encoded text)?="<br>
     * Cases are: <br>
     * <ul>
     *     <li>UTF-8 / utf-8: =?UTF-8?Q?Subject?=</li>
     *     <li>iso-8859-1: =?iso-8859-1?Q?Subject?=</li>
     *     <li>Subject (Plaintext)</li>
     * </ul>
     * [GitHub] Please verify your email address.
     * <p>
     * =?iso-8859-1?Q?Mentor*innen_f=FCr_internationale_Studierende_gesucht!?=
     * <p>
     * =?UTF-8?Q?[Friedolin]_-_PR=C3=84SENZ_im_WiSe_22?=
     * <p>
     * =?utf-8?q?Willkommen_bei_der_=22FSRInfo-News=22_Mailingliste__?=
     * <p>
     * =?utf-8?B?TmV1ZXMgYXVzIGRlbSBJbnRlcm5hdGlvbmFsZW4gQsO8cm8gLyBOZXdzIGZy?=
     * <p>
     * =?utf-8?B?TGluQWxnIGbDvHIgSW5mbyAoMjAyMik6IExlc2VhdWZnYWJlIGbDvHIgZGk=?=  =?utf-8?B?ZSBMaW5lYXJlIEFsZ2VicmE=?=
     */
    private static String anyDecode(String subject) {
        if (subject.startsWith("=?")) { // If the subject starts with "=?"
            String[] splits = subject.split("=\\?"); // Split the subject by "=?"
            StringBuilder decoded = new StringBuilder(); // The decoded subject

            for (String split : splits) {
                try {
                    if (split.isBlank()) { // If the split is blank, continue
                        continue;
                    }

                    String[] parts = split.split("\\?"); // Split the subject into parts
                    String charset = parts[0]; // Get the charset
                    String encoding = parts[1].toUpperCase(); // Get the encoding
                    String encodedText = parts[2]; // Get the encoded text

                    if (encoding.equals("Q")) { // If the encoding is "Q"
                        // use regex
                        encodedText = encodedText.replaceAll("=([0-9A-Fa-f]{2})", "%$1"); // Replace all "=XX" with "%XX"

                        try {
                            decoded.append(URLDecoder.decode(encodedText, charset)); // Decode the encoded text
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            System.out.println("Unsupported encoding: " + charset);
                        }
                    } else if (encoding.equals("B")) { // If the encoding is "B"
                        byte[] bytes = Base64.getDecoder().decode(encodedText); // Decode the encoded text
                        try {
                            decoded.append(new String(bytes, charset)); // Decode the bytes
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            System.out.println("Unsupported encoding: " + charset);
                        }
                    }
                } catch (Exception ignored) {}
            }

            return decoded.toString(); // Return the subject
        } else { // If the subject does not start with "=?"
            return subject; // Return the subject
        }
    }
}