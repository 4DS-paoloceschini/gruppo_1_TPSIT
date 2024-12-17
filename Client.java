import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Indirizzo del server
    private static final int SERVER_PORT = 5678; // Porta del server
    private static final String REGION_CODE = "IT"; // Codice del Paese (Italia in questo caso)

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            // Controllo del numero di telefono
            String numero = "";
            while (true) {
                System.out.print("Inserisci il tuo numero di telefono (es. +39...): ");
                numero = scanner.nextLine();
                if (isValidPhoneNumber(numero)) {
                    break; // Esci dal ciclo se il numero è valido
                }
                System.out.println("Numero non valido. Riprova.");
            }
            out.writeUTF(numero);

            // Chiede e invia il nome
            System.out.print("Inserisci il tuo nome: ");
            String nome = scanner.nextLine();
            out.writeUTF(nome);

            // Riceve il messaggio di benvenuto dal server
            System.out.println(in.readUTF());

            // Thread per ricevere messaggi dal server
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        String serverMessage = in.readUTF();
                        System.out.println(serverMessage);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            // Ciclo per inviare messaggi al server
            System.out.print("Inserisci il messaggio (o 'exit' per uscire): \n");
            while (true) {
                String message = scanner.nextLine();
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
                out.writeUTF(message);
            }
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Metodo per validare il numero di telefono utilizzando libphonenumber
    private static boolean isValidPhoneNumber(String phoneNumber) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber parsedNumber = phoneUtil.parse(phoneNumber, REGION_CODE);
            return phoneUtil.isValidNumber(parsedNumber);
        } catch (NumberParseException e) {
            return false; // Numero non valido
        }
    }
}
