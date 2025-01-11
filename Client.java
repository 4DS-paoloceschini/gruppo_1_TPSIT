import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Indirizzo del server
    private static final int SERVER_PORT = 1234; // Porta del server

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String number;

        while (true) {
            System.out.print("Insert your phone number (must put the prefix): ");
            number = scanner.nextLine();

            if (number.charAt(0)=='+' && isValidPhoneNumber(number)) {
                break;
            } else {
                System.out.println("Invalid phone number. Please try again.");
            }
        }

        System.out.print("Insert your username: ");
        String name = scanner.nextLine();

        System.out.println("Write '/join' to access the group, until that message, be free to write whatever you want <3");

        String instruction = "";

        while (!instruction.equals("/join")) {
            instruction = scanner.nextLine();
        }
        connection(number, name);


        do {
            System.out.println("Want to let your device waiting to rejoin or shut it down? ('/wait' or '/shut')");
            while (!instruction.equals("/wait") && !instruction.equals("/shut")) {
                instruction = scanner.nextLine();
            }
            if (instruction.equals("/wait")) {
                while (!instruction.equals("/join")) {
                    instruction = scanner.nextLine();
                }
                connection(number, name);}
            }while (!instruction.equals("/shut")) ;

        System.out.println("Thanks for using our product! <3");
    }

    public static void connection(String number, String name) {
        Scanner scanner = new Scanner(System.in);
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                DataInputStream in = new DataInputStream(socket.getInputStream());) {


            // Invia il nome del client
            out.writeUTF(number);

            out.writeUTF(name);

            // Riceve il messaggio di benvenuto dal server
            System.out.println(in.readUTF());

            // Thread per ricevere messaggi dal server
            Thread receiveThread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        String serverMessage = in.readUTF();
                        System.out.println(serverMessage);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) {
                        System.out.println("Server connection lost.");
                    }
                }
            });
            receiveThread.start();

            // Ciclo per inviare messaggi al server
            System.out.println("Insert a message (or '/exit' to quit):");
            while (true) {
                String message = scanner.nextLine();

                if ("/exit".equals(message)) {
                    System.out.println("Goodbye!");
                    break;  // Uscita dal client
                }

                if(!(message.charAt(0) == '/')) {
                    out.writeUTF(message); // Invia il messaggio al server
                }
            }

            socket.close(); // Chiudi il socket
        } catch (Exception e) {
            System.out.println("<3 Error: " + e.getMessage());
        }
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            // Analizza il numero specificando la regione
            Phonenumber.PhoneNumber parsedNumber = phoneNumberUtil.parse(phoneNumber, "IT");

            // Verifica se il numero è valido per lo stato corrente
            if (phoneNumberUtil.isValidNumber(parsedNumber)) {
                // Ricava il codice della regione (stato) dal numero
                String region = phoneNumberUtil.getRegionCodeForNumber(parsedNumber);
                //System.out.println("Valid phone number from country: " + region);
                return true;
            }
        } catch (NumberParseException ignored) {

        }
        return false;
    }

}