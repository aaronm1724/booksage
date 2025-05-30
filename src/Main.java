import java.util.Scanner;

public class Main {
    // Scanner object for user input
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        // Display welcome message
        System.out.println("Welcome to BookSage!");
        System.out.println("Your personal book discovery assistant\n");

        // Main program loop
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getUserChoice();
            running = handleUserChoice(choice);
        }

        // Clean up resources
        scanner.close();
        System.out.println("Thank you for using BookSage. Goodbye!");
    }

    // Display the main menu options
    private static void displayMenu() {
        System.out.println("\n" + "-".repeat(40));
        System.out.println("Please select an option:");
        System.out.println("1. Search by genre");
        System.out.println("2. Search by author");
        System.out.println("3. Search by book title");
        System.out.println("4. Exit");
        System.out.println("-".repeat(40));
    }

    // Get and validate user input
    private static int getUserChoice() {
        int choice = 0;
        boolean validInput = false;

        while (!validInput) {
            System.out.print("Enter your choice (1-4): ");
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice >= 1 && choice <= 4) {
                    validInput = true;
                } else {
                    System.out.println("Invalid selection. Please enter a number between 1 and 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }

        return choice;
    }

    // Process user's menu selection
    private static boolean handleUserChoice(int choice) {
        switch (choice) {
            case 1:
                searchByGenre();
                return true;
            case 2:
                searchByAuthor();
                return true;
            case 3:
                searchByTitle();
                return true;
            case 4:
                return false;
            default:
                return true;
        }
    }

    // Placeholder methods for search functionality
    private static void searchByGenre() {
        System.out.println("\nSearching by genre... (Feature coming soon)");
    }

    private static void searchByAuthor() {
        System.out.println("\nSearching by author... (Feature coming soon)");
    }

    private static void searchByTitle() {
        System.out.println("\nSearching by title... (Feature coming soon)");
    }
}

