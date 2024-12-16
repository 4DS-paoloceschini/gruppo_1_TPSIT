import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // Indirizzo del server (assumendo che sia in locale)
    private static final int SERVER_PORT = 5678; // Porta del server

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             Scanner scanner = new Scanner(System.in)) {

            // Invia il nome del client
            System.out.print("Inserisci il tuo numero di telefono: ");
            String numero = scanner.nextLine();
            out.writeUTF(numero);

            System.out.print("Inserisci il tuo nome: ");
            String nome = scanner.nextLine();
            out.writeUTF(nome);

            // Riceve il messaggio di benvenuto dal server
            System.out.println(in.readUTF());

            // Thread per ricevere messaggi dal server
            Thread receiveThread = new Thread(() -> {
                try {
                    while (true) {
                        // Leggi e stampa i messaggi ricevuti dal server
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
                    break;  // Uscita dal client
                }

                // Invia il messaggio al server
                out.writeUTF(message);
            }
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}