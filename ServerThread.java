import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerThread extends Thread {
    private static final List<ServerThread> threads = new CopyOnWriteArrayList<>();
    private static final List<String> messages = new CopyOnWriteArrayList<>(); // Lista dei messaggi inviati
    private static final List<String> clientNumbers = new CopyOnWriteArrayList<>(); // Lista dei numeri dei client
    private static final List<String> clientNames = new CopyOnWriteArrayList<>();   // Lista dei nomi dei client
    private static final HashMap<String, String> firstAccessTimestamps = new HashMap<>(); // Mappa dei primi accessi

    private Socket client;
    private DataOutputStream out;
    private DataInputStream in;
    private String nomeGruppo = "Unnamed group";

    public ServerThread(Socket c) {
        client = c;
    }
    @Override
    public void run() {
        try {
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());

            synchronized (threads) {
                threads.add(this); // Aggiungi il nuovo thread alla lista dei client connessi
            }

            comunica();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void comunica() {
        try {

            // Leggi il numero del client
            String numeroMittente = in.readUTF();
            System.out.println("Received number: " + numeroMittente); // Log del numero ricevuto

            // Leggi il nome del client
            String nomeMittente = in.readUTF();
            System.out.println("Received name: " + nomeMittente); // Log del nome ricevuto



            if(clientNames.isEmpty()){
                nomeGruppo = nomeMittente + "'s group";
            }

            // Controlla se il client è già registrato
            if (clientNames.contains(nomeMittente)) {
                String strServer = nomeMittente + " is back in "+nomeGruppo+"!";
                System.out.println(strServer);
                out.writeUTF(strServer);

                // Invia i messaggi precedenti a questo client
                String firstAccessTime = firstAccessTimestamps.get(nomeMittente);
                for (String msg : messages) {
                    String[] parts = msg.split(" - ", 2); // Assumendo formato: "timestamp - messaggio"
                    if (parts.length == 2 && parts[0].compareTo(firstAccessTime) > 0) {
                        out.writeUTF(parts[1]); // Invia solo i messaggi successivi al primo accesso
                    }
                }
            } else {
                // Registra il primo accesso con data e ora
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                firstAccessTimestamps.put(nomeMittente, timestamp);

                // Messaggio di benvenuto
                out.writeUTF("Welcome " + numeroMittente + " " + nomeMittente + "! First access in "+nomeGruppo+" registered at: " + timestamp);
                System.out.println(nomeMittente + " joined the chat at " + timestamp);

                // Aggiungi numero e nome ai rispettivi array
                clientNumbers.add(numeroMittente);
                clientNames.add(nomeMittente);
            }

            // Leggi i messaggi dal client
            String str;
            while (true) {
                str = in.readUTF(); // Leggi il messaggio dal client
                System.out.println(nomeMittente + ": " + str); // Log del messaggio ricevuto
                if (str == null || str.trim().isEmpty()) {
                    continue; // Evita di inviare messaggi vuoti
                }

                // Aggiungi il messaggio alla lista dei messaggi con timestamp
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                messages.add(timestamp + " - " + nomeMittente + ": " + str);

                // Invia il messaggio a tutti gli altri client
                synchronized (threads) {
                    for (ServerThread thread : threads) {
                        if (thread != this) { // Evita di inviare il messaggio al client che lo ha inviato
                            thread.sendMessage(nomeMittente + ": " + str);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (client != null) client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (threads) {
                threads.remove(this); // Rimuovi il thread dalla lista quando si disconnette
            }
        }
    }

    private void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
