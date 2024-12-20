import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Indirizzo del server
    private static final int SERVER_PORT = 1234; // Porta del server

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {


            // Invia il nome del client
            System.out.print("Insert your phone number: ");
            String numero = scanner.nextLine();
            out.writeUTF(numero);

            System.out.print("Insert your username: ");
            String nome = scanner.nextLine();
            out.writeUTF(nome);

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
            System.out.println("Insert a message (or 'exit' to quit):");
            while (true) {
                String message = scanner.nextLine();

                if ("exit".equals(message)) {
                    System.out.println("Goodbye!");
                    break;  // Uscita dal client
                }

                out.writeUTF(message); // Invia il messaggio al server
            }

            socket.close(); // Chiudi il socket
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
