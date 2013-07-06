package reversi;
import reversi.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Collection;
import java.io.PrintWriter;

public class ReversiPlayer extends Thread {
   private Socket sock;
   private ReversiServer serv;
   private int gamesPlayed, gamesWon;
   private ReversiTable currentTable;
   private Collection<ReversiTable> currentObservations;
   Scanner in;
   PrintWriter out;
   String nick;
   ReversiPlayer(ReversiServer serv, Socket s) {
      this.serv = serv;
      sock = s;
      gamesWon = gamesPlayed = 0;
      currentTable = null;
      currentObservations = new LinkedList<ReversiTable>();
      System.out.println("Created new player on socket " + s);
   }

   private void setNick(String nick) {
      this.nick = nick;
   }

   public String getNick() {
      return nick;
   }

   public void leaveTable() {
      currentTable = null;
   }
   public void joinTable(ReversiTable table) {
      if (currentTable == null)
         currentTable = table;
   }

   public void playGame() { gamesPlayed++; }

   public void winGame() { gamesWon++; }

   public Socket getSocket() {
      return this.sock;
   }

   public void run() {
      try {
         in = new Scanner(sock.getInputStream());
         out = new PrintWriter(sock.getOutputStream(), true);
      } catch (Exception e) {
         System.out.println(e.getMessage());
         return;
      }
      while (true) {
         String input = in.nextLine(); // get line from other side
         if (input.equalsIgnoreCase("exit")) break;
         System.out.println("hey yo!");
         System.out.println("Client sent: " + input);
         String response = handle(input);
         out.println(response);
      }
      serv.removePlayer(this);
   }
   private String handle(String message) {
      String[] words = message.trim().split(" ");
      String command = words[0];
      System.out.println("Command: " + command);
      switch (command) {
         case "join":
            try {
               int id = Integer.parseInt(words[1]);
               String res = serv.addPlayerToTable(this, id);
               if (res != null)
                  return "Joined table " + words[1] + "\n" + res;
               else
                  return "That table is full or doesn't exist";
            } catch (Exception e) {
               return "Bad id";
            }
         case "newtable":
            return "Created table " + serv.addTable();
         case "nick":
            if (words.length > 1) {
               if (serv.isUniqueNick(words[1]))
                  setNick(words[1]);
               else
                  return "Nick '" + words[1] + "' is in use.";
            } else {
               setNick(null);
            }
            return "Your nick is now " + nick;
         default:
            break;
      }
      return message;
   }

   @Override public String toString() {
      String res = nick + " (" + sock + ")";
      return res;
   }
}