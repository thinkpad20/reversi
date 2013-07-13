package reversi;
import reversi.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Sender {
   private enum Type {SOCKET, RMI} t;
   private Socket sock;
   private PrintWriter out;
   public Sender (Socket sock) {
      t = Type.SOCKET;
      this.sock = sock;
      try {
         out = new PrintWriter(sock.getOutputStream(), true);
      } catch (Exception e) {
         System.out.println("Error initializing printwriter");
         System.exit(1);
      }
   }
   public void send(String msg) {
      if ()
   }
}