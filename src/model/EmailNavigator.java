package model;

import java.util.Scanner;

public class EmailNavigator {
    public static void navigateFolder(MailFolder folder) {
        EmailIterator iterator = new EmailIterator(folder);
        Scanner scanner = new Scanner(System.in);
        String command;

        System.out.println("\n--- Navigating folder: " + folder.getName() + " ---");
        System.out.println("Commands: n = next, p = previous, r = reset, q = quit");

        while (true) {
            System.out.print("Enter command: ");
            command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "n":
                    if (iterator.hasNext()) {
                        EmailComponent email = iterator.next();
                        email.display();
                    } else {
                        System.out.println("Reached the end of folder.");
                    }
                    break;

                case "p":
                    if (iterator.hasPrevious()) {
                        EmailComponent email = iterator.previous();
                        email.display();
                    } else {
                        System.out.println("At the beginning of folder.");
                    }
                    break;

                case "r":
                    iterator.reset();
                    System.out.println("Iterator reset to start.");
                    break;

                case "q":
                    System.out.println("Exiting folder navigation.");
                    return;

                default:
                    System.out.println("Invalid command. Use n/p/r/q.");
            }
        }
    }
}

