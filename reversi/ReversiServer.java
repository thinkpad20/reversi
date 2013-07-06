package reversi;
import reversi.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Collection;
import java.io.PrintWriter;


public class ReversiServer {
   private int port;
   Collection<ReversiGame> games;
   Collection<ReversiPlayer> players;
   Collection<ReversiTable> tables;
   int tableCount;
   ReversiServer(int port) { 
      this.games = new LinkedList<ReversiGame>();
      this.players = new LinkedList<ReversiPlayer>();
      this.tables = new LinkedList<ReversiTable>();
      this.port = port; 
      tableCount = 0;
   }

   public void start() throws Exception {
      ServerSocket ss = new ServerSocket(port);
      System.out.println("Server running on port " + port);
      Thread acceptThread = new Thread(new Acceptor(ss, this, players));
      acceptThread.start();
      acceptThread.join();
      ss.close();
   }

   public static void main(String[] args) throws Exception {
      ReversiServer serv = new ReversiServer(8189);
      serv.start();
   }

   public void removePlayer(ReversiPlayer player) {
      System.out.println("Attempting to remove player on sock " + player.getSocket());
      try {
         player.getSocket().close();
         players.remove(player);
         System.out.println("Successfully removed player.");
      } catch (Exception e) {
         System.out.println(e.getMessage());
         System.out.println("Failed to remove player");
      }
   }

   public int addTable() {
      ReversiTable t = new ReversiTable(tableCount++);
      tables.add(t);
      return tableCount - 1;
   }

   public String addPlayerToTable(ReversiPlayer player, int id) {
      for (ReversiTable table : tables) {
         if (table.getID() == id && table.addPlayer(player)) {
            return table.toString();
         }
      }
      return null;
   }

   public boolean isUniqueNick(String nick) {
      for (ReversiPlayer player : players) {
         if (player.getNick() != null && player.getNick().equals(nick))
            return false;
      }
      return true;
   }

   // public static void main(String[] args) {
   //    ReversiGame ReversiGame = new ReversiGame();
   //    Scanner instream = new Scanner(System.in);
   //    while (true) {
   //       System.out.print(ReversiGame);
   //       String player = (ReversiGame.getTurn() == 'W') ? "white" : "black";
   //       System.out.print(player + " move> ");
   //       String input = instream.nextLine();
   //       if (input.equals("exit")) break;
   //       int x = Integer.parseInt(Character.toString(input.charAt(0))),
   //           y = Integer.parseInt(Character.toString(input.charAt(2)));
   //       System.out.println(ReversiGame.respondToMessage("PLACE" + ReversiGame.getTurn() + x + y));
   //    }
   // }
}

// accepts new clients
class Acceptor implements Runnable {
   private ServerSocket ss;
   private ReversiServer serv;
   private Collection<ReversiPlayer> players;
   Acceptor(ServerSocket ss, ReversiServer serv, Collection<ReversiPlayer> players) {
      this.ss = ss;
      this.serv = serv;
      this.players = players;
   }
   public void run () {
      while(true) {
         try {
            Socket s = ss.accept();
            System.out.println("Connection received");
            ReversiPlayer p = new ReversiPlayer(serv, s);
            players.add(p);
            p.start();
         } catch (Exception e) {
            System.out.println(e.getMessage());
            break;
         }
      }
   }
}