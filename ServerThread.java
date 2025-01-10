import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class PhoneNumberMismatchException extends Exception {
    public PhoneNumberMismatchException(String message) {
        super(message);
    }
}

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


            if (clientNames.isEmpty()) {
                nomeGruppo = nomeMittente + "'s group";
            } else {
                if(!getCountryFromPhoneNumber(clientNumbers.get(0), numeroMittente)){
                    throw new PhoneNumberMismatchException("Un numero sospetto ha provato ad accedere");
                }
                nomeGruppo = clientNames.get(0);
            }

            // Controlla se il client è già registrato
            if (clientNumbers.contains(numeroMittente)) {
                // Logica per il client che ritorna
                handleReturningClient(numeroMittente, nomeMittente);
            } else {
                // Logica per il primo accesso del client
                handleNewClient(numeroMittente, nomeMittente);
            }

            // Leggi i messaggi dal client
            while (true) {
                try {
                    String str = in.readUTF(); // Leggi il messaggio dal client
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
                catch (Exception e) {
                    // Gestione dell'errore durante la lettura di un messaggio
                    System.out.println(nomeMittente + " quit.");
                    break; // Esci dal ciclo quando il client si disconnette
                }
            }
        }catch (PhoneNumberMismatchException e) {
            // Gestisce l'eccezione e stampa il messaggio
            System.out.println(e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            closeQuietly(client);
            synchronized (threads) {
                threads.remove(this); // Rimuovi il thread dalla lista quando si disconnette
            }
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log a livello debug o ignora l'eccezione
                System.err.println("Error during closure: " + e.getMessage());
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

    // Metodo per gestire la logica del client che ritorna
    private void handleReturningClient(String numeroMittente, String nomeMittente) throws IOException {
        if (!clientNames.contains(nomeMittente) && clientNumbers.contains(numeroMittente)) {
            boolean ver = false;
            int i = 0;
            while(!ver){
                if(numeroMittente.equals(clientNumbers.get(i))){
                    ver = true;
                }else{
                    i++;
                }
            }
            clientNumbers.set(i, nomeMittente);
            if (i==0){
                nomeGruppo = nomeMittente;
                System.out.println("New name for the group!!!: "+nomeGruppo+"'s group!!!");
            }


            String strServer = numeroMittente + " is back in " + nomeGruppo + "'s group with a new name, welcome back " + nomeMittente + "!";

            System.out.println(strServer);
            out.writeUTF(strServer);
        } else {
            String strServer = numeroMittente + ": " + nomeMittente + " is back in " + nomeGruppo + "'group!";
            System.out.println(strServer);
            out.writeUTF(strServer);
        }

        // Invia i messaggi precedenti a questo client
        String firstAccessTime = firstAccessTimestamps.get(numeroMittente);
        for (String msg : messages) {
            String[] parts = msg.split(" - ", 2); // Assumendo formato: "timestamp - messaggio"
            if (parts.length == 2 && parts[0].compareTo(firstAccessTime) > 0) {
                out.writeUTF(parts[1]); // Invia solo i messaggi successivi al primo accesso
            }
        }
    }

    //Gestione della logica del primo accesso del client
    private void handleNewClient(String numeroMittente, String nomeMittente) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        firstAccessTimestamps.put(numeroMittente, timestamp);

        // Messaggio di benvenuto
        out.writeUTF("Welcome " + numeroMittente + " -> " + nomeMittente + " in " + nomeGruppo + "'s group! First access in " + nomeGruppo + " registered at: " + timestamp);
        System.out.println(numeroMittente + ": " + nomeMittente + " joined the chat at " + timestamp);

        // Aggiungi numero e nome ai rispettivi array
        clientNumbers.add(numeroMittente);
        clientNames.add(nomeMittente);
    }

    public static boolean getCountryFromPhoneNumber(String adminPhoneNumber, String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            // Analizza il numero specificando la regione
            Phonenumber.PhoneNumber parsedNumberUser = phoneNumberUtil.parse(phoneNumber, "IT");
            Phonenumber.PhoneNumber parsedNumberAdmin = phoneNumberUtil.parse(adminPhoneNumber, "IT");

            // Verifica se il numero è valido per lo stato corrente
            if (phoneNumberUtil.isValidNumber(parsedNumberAdmin) && phoneNumberUtil.isValidNumber(parsedNumberUser)) {
                // Ricava il codice della regione (stato) dal numero
                String regionUser = phoneNumberUtil.getRegionCodeForNumber(parsedNumberUser);
                String regionAdmin = phoneNumberUtil.getRegionCodeForNumber(parsedNumberAdmin);
                if (!regionUser.equals(regionAdmin)) {
                    return false;
                }
            }
        } catch (NumberParseException ignored) {

        }
        return true;
    }
}