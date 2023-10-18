import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class represents a client for the GoFundMe application. It allows users
 * to create new fundraising events, list fundraising events, donate to an
 * event, check event details, and exit the application. The client communicates
 * with the server using sockets and sends/receives data using DataInputStream
 * and DataOutputStream.
 * The client prompts the user for input using Scanner and validates the input
 * before sending it to the server.
 * The client runs in an infinite loop to allow for reconnect attempts in case
 * the connection to the server is lost.
 * The server address and port are set as constants in the class.
 */
public class GoFundMeClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    /**
     * This class represents a client for the GoFundMe fundraising platform. It
     * allows users to create new fundraising events, list existing events, donate
     * to events, and check event details. The client communicates with a server
     * using sockets and sends/receives data using DataInputStream and
     * DataOutputStream. The user interface is implemented using a Scanner object to
     * read user input from the console.
     * 
     * The main method contains a loop that allows for reconnect attempts in case
     * the connection to the server is lost. Within the loop, the user is presented
     * with a menu of options to choose from. The user's choice is sent to the
     * server, which responds with the appropriate data or confirmation message.
     * 
     * The getIntInput, getStringInput, and getDateInput methods are helper methods
     * that validate user input and ensure that it is of the correct type/format.
     */
    public static void main(String[] args) throws InterruptedException {
        while (true) { // Loop to allow for reconnect attempts
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    Scanner scanner = new Scanner(System.in)) {

                int totalEventCountForList = 0;

                while (true) {
                    System.out.println("---------------------------------");
                    System.out.println("Choose an option:");
                    System.out.println("1. Create a new fundraising event");
                    System.out.println("2. List fundraising events");
                    System.out.println("3. Donate to an event");
                    System.out.println("4. Check event details");
                    System.out.println("5. Exit");

                    int choice = getIntInput(scanner, "Enter your choice: ", 1, 5);

                    switch (choice) {
                        case 1:
                            System.out.println("---------------------------------");
                            String eventName = getStringInput(scanner, "Enter event name: ");
                            double targetAmount = getDoubleInput(scanner, "Enter target amount: ", 0);
                            Date deadline = getDateInput(scanner, "Enter deadline (in format MM-dd-yyyy): ");

                            out.writeUTF("CREATE_EVENT");
                            out.writeUTF(eventName);
                            out.writeDouble(targetAmount);
                            out.writeLong(deadline.getTime());

                            String response = in.readUTF();
                            System.out.println(response);
                            break;

                        case 2:
                            System.out.println("---------------------------------");
                            out.writeUTF("LIST_EVENTS");

                            List<String> pastEventsOutput = new ArrayList<>();
                            totalEventCountForList = in.readInt();

                            System.out.println("Current Fundraising Events:");
                            for (int i = 1; i <= totalEventCountForList; i++) {
                                boolean isCurrent = in.readBoolean();
                                eventName = in.readUTF();
                                double eventTargetAmount = in.readDouble();
                                double eventCurrentAmount = in.readDouble();
                                Date eventDeadline = new Date(in.readLong());

                                String output = String.format("%d. %s (Target: $%.2f, Raised: $%.2f, Deadline: %s)\n",
                                        i, eventName, eventTargetAmount, eventCurrentAmount,
                                        new SimpleDateFormat("MM-dd-yyyy").format(eventDeadline));

                                if (isCurrent) {
                                    System.out.print(output);
                                } else {
                                    pastEventsOutput.add(output);
                                }
                            }

                            System.out.println("\nPast Fundraising Events:");
                            for (String pastEvent : pastEventsOutput) {
                                System.out.print(pastEvent);
                            }
                            break;

                        case 3:
                            System.out.println("---------------------------------");
                            int eventIndexForDonate = getIntInput(scanner, "Enter event index: ", 1,
                                    totalEventCountForList) - 1; // Use the totalEventCountForList
                            double donationAmount = getDoubleInput(scanner, "Enter donation amount: ", 0);

                            out.writeUTF("DONATE");
                            out.writeInt(eventIndexForDonate);
                            out.writeDouble(donationAmount);

                            String responseForDonate = in.readUTF();
                            System.out.println(responseForDonate);
                            break;

                        case 4:
                            System.out.println("---------------------------------");
                            int eventIndex4 = getIntInput(scanner, "Enter event index: ", 1, totalEventCountForList)
                                    - 1; // Use the totalEventCountForList

                            out.writeUTF("CHECK_DETAILS");
                            out.writeInt(eventIndex4);

                            String checkEventName = in.readUTF(); // Renamed to avoid conflict
                            double checkTargetAmount = in.readDouble();
                            double checkCurrentAmount = in.readDouble();
                            Date checkDeadline = new Date(in.readLong());
                            System.out.printf(
                                    "Event Details:\nName: %s\nTarget Amount: $%.2f\nAmount Raised: $%.2f\nDeadline: %s\n",
                                    checkEventName, checkTargetAmount, checkCurrentAmount,
                                    new SimpleDateFormat("MM-dd-yyyy").format(checkDeadline));
                            break;

                        case 5:
                            System.out.println("Exiting...");
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                }

            } catch (SocketException se) {
                System.out.println("Lost connection to server. Trying to reconnect...");
                // Add a sleep here if you want to introduce a delay before retrying
                Thread.sleep(2000); // 2 seconds delay
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method prompts the user for input using the provided prompt string and
     * returns the input as a trimmed string.
     * If the input is empty, the method will continue to prompt the user until a
     * non-empty input is provided.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the prompt string to display to the user
     * @return the user's input as a trimmed string
     */
    private static String getStringInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        while (input.isEmpty()) {
            System.out.println("Input cannot be empty. Please try again.");
            System.out.print(prompt);
            input = scanner.nextLine().trim();
        }
        return input;
    }

    /**
     * Prompts the user for a double input and returns the value if it is greater
     * than the specified minimum value.
     * If the user enters an invalid input, the method will prompt the user to enter
     * a positive number.
     * 
     * @param scanner  the Scanner object used to read user input
     * @param prompt   the message to display to the user when prompting for input
     * @param minValue the minimum value that the input must be greater than
     * @return the double value entered by the user
     */
    private static double getDoubleInput(Scanner scanner, String prompt, double minValue) {
        double value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextDouble()) {
                value = scanner.nextDouble();
                if (value > minValue) {
                    break;
                }
            }
            System.out.println("Invalid input. Please enter a positive number.");
            scanner.nextLine(); // Clear the invalid input
        }
        scanner.nextLine(); // Consume the newline character
        return value;
    }

    /**
     * Prompts the user to enter a date in the format "MM-dd-yyyy" and returns a
     * Date object.
     * If the user enters an invalid date format, the method will prompt the user
     * again until a valid date is entered.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the prompt message to display to the user
     * @return a Date object representing the user's input in the format
     *         "MM-dd-yyyy"
     */
    private static Date getDateInput(Scanner scanner, String prompt) {
        System.out.print(prompt);
        Date date = null;
        while (date == null) {
            String input = scanner.nextLine();
            try {
                date = new SimpleDateFormat("MM-dd-yyyy").parse(input);
            } catch (ParseException e) {
                System.out.println("Invalid date format. Please enter the date in MM-dd-yyyy format.");
            }
            if (date == null) {
                System.out.print(prompt);
            }
        }
        return date;
    }

    /**
     * This method prompts the user to enter an integer value within a specified
     * range and returns the value.
     * If the user enters an invalid input, the method will prompt the user to enter
     * a number between the specified range.
     * 
     * @param scanner the Scanner object used to read user input
     * @param prompt  the message to prompt the user for input
     * @param min     the minimum value of the range (inclusive)
     * @param max     the maximum value of the range (inclusive)
     * @return the integer value entered by the user within the specified range
     */
    private static int getIntInput(Scanner scanner, String prompt, int min, int max) {
        int value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                if (value >= min && value <= max) {
                    break;
                }
            }
            System.out.println("Invalid input. Please enter a number between " + min + " and " + max + ".");
            scanner.nextLine(); // Clear the invalid input
        }
        scanner.nextLine(); // Consume the newline character
        return value;
    }
}
