package de.unijena;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Main {
    public static void main(String[] args) throws Exception {
        // Create a map of all available commands
        Map<String, Function> commands = new HashMap<>();
        commands.put("SocketClientReadV1", (bool) -> { // Command for the first version of the socket client to read emails from the server
            try {
                SocketClientReadV1.main();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });

        System.out.println("Enter the name of the class you want to run: "); // Ask the user for the name of the class he wants to run
        System.out.println("Available classes: " + commands.keySet()); // Print all available classes
        String command = new BufferedReader(new InputStreamReader(System.in)).readLine(); // Read the user input

        if (commands.containsKey(command)) { // Check if the user input is a valid command
            if ((Boolean) commands.get(command).apply(true)) { // Execute the command
                System.out.println("Successfully executed " + command); // Print a success message
            } else {
                System.out.println("Failed to execute " + command); // Print a failure message
            }
        } else {
            System.out.println("Unknown command: " + command); // Print an error message
        }
    }
}