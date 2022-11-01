package de.unijena;

import com.sun.mail.pop3.POP3SSLStore;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public abstract class APIClientReadV1 {

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

        // Create a new Properties object
        java.util.Properties properties = new java.util.Properties();

        // Set the host and port
        properties.setProperty("mail.pop3.host", host);
        properties.setProperty("mail.pop3.port", String.valueOf(portNumber));

        // Set the SSL property if SSL is used
        if (secure) {
            properties.setProperty("mail.pop3.ssl.enable", "true");
        }

        // Create a new Session object
        Session session = Session.getInstance(properties);

        // Create a new Store object
        POP3SSLStore sslStore = null;
        Store store;
        if (secure) {
            sslStore = new POP3SSLStore(session, null);
            store = sslStore;
        } else {
            store = session.getStore("pop3");
        }

        // Connect to the server
        if (secure) {
            sslStore.connect(host, portNumber, email, password);
        } else {
            store.connect(host, portNumber, email, password);
        }

        // Get the inbox folder
        Folder inbox = secure ? sslStore.getFolder("INBOX") : store.getFolder("INBOX");

        // Open the inbox folder
        inbox.open(Folder.READ_ONLY);

        // Get the messages in the inbox folder
        Message[] messages = inbox.getMessages();

        // print all messages (Format: "[<index>] Date: <date>, Subject: <subject>")
        System.out.println("================================================================================");
        for (int i = 0; i < messages.length; i++) {
            System.out.println("[" + i + "] Date: " + messages[i].getSentDate() + ", Subject: " + messages[i].getSubject());
            System.out.println(); // Print a new line
        }
        System.out.println("================================================================================");
        System.out.println("Total amount of messages: " + messages.length);


        // Listen for commands from the user
        while (true) {
            System.out.println("Enter the number of the message you want to read or close to exit: ");

            // Get the command from the user
            String command = br.readLine();

            if ("close".equals(command)) {// Close the connection to the server
                System.out.println("================================================================================");
                break;
            } else {// if the input is not a number, it is invalid, else it is valid
                try {
                    int index = Integer.parseInt(command);

                    // if the index is out of bounds, it is invalid, else it is valid
                    if (index < 0 || index >= messages.length) {
                        System.out.println("Invalid index. Please enter a valid index or 'close' to exit: ");
                        System.out.println("================================================================================");
                    } else {
                        String sender = messages[index].getFrom()[0].toString();
                        sender = sender.substring(sender.indexOf("<") + 1, sender.indexOf(">"));
                        String receiver = messages[index].getAllRecipients()[0].toString();
                        receiver = receiver.substring(receiver.indexOf("<") + 1, receiver.indexOf(">"));

                        // print the message
                        System.out.println("================================================================================");
                        System.out.println("Date: " + messages[index].getSentDate());
                        System.out.println("Sender: " + sender);
                        System.out.println("Receiver: " + receiver);
                        System.out.println("Subject: " + messages[index].getSubject());
                        System.out.println("======================== Body =============================");
                        if (messages[index].getContent() instanceof MimeMultipart mimeMultipart) {
                            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                                System.out.println(bodyPart.getContent());
                            }
                        } else {
                            System.out.println(messages[index].getContent());
                        }
                        System.out.println("================================================================================");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input!");
                    System.out.println("================================================================================");
                }
            }
        }

        System.out.println("Closing connection..."); // tell the user that the connection is closing

        inbox.close(false); // close the inbox folder without expunging the messages

        // Close the BufferedReader
        br.close();

        // Close the connection to the server
        if (secure) {
            sslStore.close();
        } else {
            store.close();
        }
    }
}
