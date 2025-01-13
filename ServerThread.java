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
    private String groupName = "Unnamed group";

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
            String sendernNumber = in.readUTF();
            System.out.println("Received number: " + sendernNumber); // Log del numero ricevuto

            // Leggi il nome del client
            String senderName = in.readUTF();
            System.out.println("Received name: " + senderName); // Log del nome ricevuto


            if (clientNames.isEmpty()) {
                groupName = senderName + "'s group";
            } else {
                if(!getCountryFromPhoneNumber(clientNumbers.get(0), sendernNumber)){
                    throw new PhoneNumberMismatchException("A suspicious number tried to access \uD83D\uDE31");
                }
                groupName = clientNames.get(0);
            }

            // Controlla se il client è già registrato
            if (clientNumbers.contains(sendernNumber)) {
                // Logica per il client che ritorna
                handleReturningClient(sendernNumber, senderName);
            } else {
                // Logica per il primo accesso del client
                handleNewClient(sendernNumber, senderName);
            }

            while (true) {
                try {
                    String str = in.readUTF(); // Leggi il messaggio dal client

                    if(str.equals("/exit")){
                        messageSender(senderName, "quit.");
                    }
                    else {

                        System.out.println(senderName + ": " + str); // Log del messaggio ricevuto

                        if (str == null || str.trim().isEmpty()) {
                            continue; // Evita di inviare messaggi vuoti
                        }


                        // Aggiungi il messaggio alla lista dei messaggi con timestamp
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        messages.add(timestamp + " - " + senderName + ": " + str);

                        // Invia il messaggio a tutti gli altri client
                        messageSender(senderName, str);
                    }
                }
                catch (Exception e) {
                    // Gestione dell'errore durante la lettura di un messaggio
                    System.out.println(senderName + " quit.");
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
                System.err.println("Error during closure: " + e.getMessage());
            }
        }
    }

    private void messageSender(String senderName, String message){
        synchronized (threads) {
            for (ServerThread thread : threads) {
                if (thread != this) {
                    thread.sendMessage(senderName + " " + message);
                }
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
    private void handleReturningClient(String sendernNumber, String senderName) throws IOException {
        if (!clientNames.contains(senderName) && clientNumbers.contains(sendernNumber)) {
            boolean ver = false;
            int i = 0;
            while(!ver){
                if(sendernNumber.equals(clientNumbers.get(i))){
                    ver = true;
                }else{
                    i++;
                }
            }
            clientNumbers.set(i, senderName);
            if (i==0){
                groupName = senderName;
                System.out.println("New name for the group!!!: "+ groupName +"'s group!!!");
            }


            String strServer = sendernNumber + " is back in " + groupName + "'s group with a new name, welcome back " + senderName + "!";

            messageSender(senderName, "joined the chat!");

            System.out.println(strServer);
            out.writeUTF(strServer);
        } else {
            String strServer = sendernNumber + ": " + senderName + " is back in " + groupName + "'group!";
            System.out.println(strServer);
            out.writeUTF(strServer);
        }

        // Invia i messaggi precedenti a questo client
        String firstAccessTime = firstAccessTimestamps.get(sendernNumber);
        for (String msg : messages) {
            String[] parts = msg.split(" - ", 2);
            if (parts.length == 2 && parts[0].compareTo(firstAccessTime) > 0) {
                out.writeUTF(parts[1]); // Invia solo i messaggi successivi al primo accesso
            }
        }
    }

    //Gestione della logica del primo accesso del client
    private void handleNewClient(String senderNumber, String senderName) throws IOException {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        firstAccessTimestamps.put(senderNumber, timestamp);

        // Messaggio di benvenuto
        out.writeUTF("Welcome " + senderNumber + " -> " + senderName + " in " + groupName + "'s group! First access in " + groupName + " registered at: " + timestamp);
        System.out.println(senderNumber + ": " + senderName + " joined the chat at " + timestamp);

        messageSender(senderName, "joined the chat!");

        // Aggiungi numero e nome
        clientNumbers.add(senderNumber);
        clientNames.add(senderName);
    }

    public static boolean getCountryFromPhoneNumber(String adminPhoneNumber, String phoneNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            // Analizza il numero specificando la regione
            Phonenumber.PhoneNumber parsedNumberUser = phoneNumberUtil.parse(phoneNumber, "IT");
            Phonenumber.PhoneNumber parsedNumberAdmin = phoneNumberUtil.parse(adminPhoneNumber, "IT");

            // Verifica se il numero è valido
            if (phoneNumberUtil.isValidNumber(parsedNumberAdmin) && phoneNumberUtil.isValidNumber(parsedNumberUser)) {
                // Ricava il codice della regione dal numero
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