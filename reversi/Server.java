package reversi;
import reversi.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Collection;
import java.io.PrintWriter;


public class Server {
   private int port;
   Collection<Game> games;
   Collection<Connection> cons;
   Collection<Table> tables;
   int tableCount;
   public Server(int port) { 
      games = new LinkedList<Game>();
      cons = new LinkedList<Connection>();
      tables = new LinkedList<Table>();
      this.port = port;
      tableCount = 0;
   }

   public void start() throws Exception {
      ServerSocket ss = new ServerSocket(port);
      System.out.println("Server running on port " + port);
      Thread acceptThread = new Thread(new Acceptor(ss, this, cons));
      acceptThread.start();
      acceptThread.join();
      ss.close();
   }

   public void removeConnection(Connection con) {
      System.out.println("Attempting to remove connection on sock " + con.getSocket());
      try {
         con.getSocket().close();
         cons.remove(con);
         System.out.println("Successfully removed player.");
      } catch (Exception e) {
         System.out.println(e.getMessage());
         System.out.println("Failed to remove player");
      }
   }

   public Table addTable() {
      Table t = new Table(tableCount++);
      tables.add(t);
      return t;
   }

   public Table addPlayerToFirstOpenTable(Player player) {
      for (Table table : tables) {
         if (table.addPlayer(player)) {
            return table;
         }
      }

      // if no open table found, create one
      Table t = addTable();
      t.addPlayer(player);
      return t;
   }

   public Table addPlayerToTable(Player player, int id) {
      for (Table table : tables) {
         if (table.getID() == id && table.addPlayer(player)) {
            return table;
         }
      }
      return null;
   }

   public Table addObserverToTable(Player player, int id) {
      for (Table table : tables) {
         if (table.getID() == id && table.addObserver(player)) {
            return table;
         }
      }
      return null;
   }


   public boolean isUniqueNick(String nick) {
      for (Connection con : cons) {
         Player player = con.getPlayer();
         if (player.getNick() != null && player.getNick().equals(nick))
            return false;
      }
      return true;
   }

   public String listTables() {
      StringBuffer res = new StringBuffer("Current tables:\n");
      for (Table table : tables)
         res.append("\t" + table.toString() + "\n");
      return res.toString();
   }

   public String listPlayers() {
      StringBuffer res = new StringBuffer("Current tables:\n");
      for (Connection con : cons)
         res.append("\t" + con.getPlayer() + "\n");
      return res.toString();
   }

   public static void main(String[] args) throws Exception {
      int port = 8189;
      for (int i=0; i<args.length; ++i) {
         if (args[i].equals("-p") && i < args.length - 1) {
            try {
               port = Integer.parseInt(args[++i]);
            } catch (Exception e) {
               System.out.println("Please enter a valid port, or none");
               System.exit(0);
            }
         }
      }
      Server serv = new Server(port);
      serv.start();
   }
}

// accepts new clients
class Acceptor implements Runnable {
   private ServerSocket ss;
   private Server serv;
   private Collection<Connection> cons;

   public Acceptor(ServerSocket ss, Server serv, Collection<Connection> cons) {
      this.ss = ss;
      this.serv = serv;
      this.cons = cons;
   }

   public void run () {
      while(true) {
         try {
            Socket s = ss.accept();
            System.out.println("Connection received");
            Connection c = new Connection (serv, s, new Player());
            Thread t = new Thread(c);
            cons.add(c);
            t.start();
         } catch (Exception e) {
            System.out.println("Exception server run: " + e.getMessage());
            break;
         }
      }
   }
}