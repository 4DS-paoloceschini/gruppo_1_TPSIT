import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {

    public void attendi() {
        ServerSocket server;
        try {
            server = new ServerSocket(1234);
            while (true) {
                try {
                    Socket client = server.accept();
                    ServerThread s1 = new ServerThread(client);
                    s1.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String args[]) {
        MultiServer s = new MultiServer();
        s.attendi();
    }
}