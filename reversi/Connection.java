package reversi;
import reversi.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Connection implements Runnable {
   Server serv; 
   Socket sock;
   private String nickPlz = "You must set a nickname first. Type nick <nickname>";
   private String helpString = "Reversi commands:\r\n" +
                               "join <opt table number>: join a table (def. first open)\r\n" +
                              "newtable: make a new table\r\n" +
                              "nick: set your nickname\r\n" +
                              "observe <table number>: observe a table\r\n" +
                              "show <opt table number>: show a table (def. current)\r\n" +
                              "move <0-7> <0-7>: make a move\r\n" +
                              "leave <opt table number>: leave a table (def. current)\r\n" +
                              "forfeit: forfeit current game\r\n" +
                              "list <opt tables|players>: list tables or players (def. tables)\r\n" +
                              "pass: pass your turn\r\n" +
                              "stats: get your stats\r\n" +
                              "mode <telnet|java>: change mode to telnet (pure text) or java mode\r\n" +
                              "exit: disconnect from server and quit\r\n" +
                              "help: show this menu\r\n";
   private Player player;
   Scanner in;
   PrintWriter out;

   public Connection(Player p) {
      this.player = p;
      p.setCon(this);
   }

   public Connection(Server serv, Socket s, Player p) {
      this(p);
      this.serv = serv;
      sock = s;
      try {
         in = new Scanner(sock.getInputStream());
         out = new PrintWriter(sock.getOutputStream(), true);
      } catch (Exception e) {
         System.out.println("" + e);
      }
   }
   
   public void run() {
      send("Welcome to Reversi! Type 'help' for a list of commands.");
      while (true) {
         try {
            String input = in.nextLine(); // get line from other side
            if (input.equalsIgnoreCase("exit")) break;
            System.out.println("Received from " + player.getNick() + ": " + input);
            String response = handle(input);
            System.out.println("Response: " + response);
            send(response);
         } catch (Exception e) {
            System.out.println("Client disconnected");
            break;
         }
      }
      serv.removeConnection(this);
   }

   public Socket getSocket() {
      return this.sock;
   }

   public Player getPlayer() {
      return this.player;
   }

   private String handleJoin(String[] words) {
      if (!player.hasNick())
         return nickPlz;

      if (player.getTable() != null)
         return "You are already at a table.";
      if (words.length == 1) {
         // then join any open table
         player.joinTable(serv.addPlayerToFirstOpenTable(player));
         return "Joined table\r\n" + player.getTable();
      }
      try {
         int id = Integer.parseInt(words[1]);
         Table res = serv.addPlayerToTable(player, id);
         if (res != null) {
            player.joinTable(res);
            return "Joined table " + words[1] + "\r\n" + res;
         }
         else
            return "That table is full or doesn't exist";
      } catch (Exception e) {
         return "Bad id";
      }      
   }

   private String handleNewTable(String[] words) {
      if (!player.hasNick())
         return nickPlz;
      return "Created table " + serv.addTable().getID();
   }

   private String handleNick(String[] words) {
      if (words.length > 1 
         && words[1].length() > 0 
         && words[1].length() <= 50) {
         if (serv.isUniqueNick(words[1])) {
            player.setNick(words[1]);
            return "Your nick is now " + player.getNick();
         } else
            return "Nick '" + words[1] + "' is in use.";
      } else {
         return "Please enter a nickname between 1 and 50 characters";
      }
   }
   private String handleObserve(String[] words) {
      try {
         int id = Integer.parseInt(words[1]);
         Table res = serv.addObserverToTable(player, id);
         if (res != null)
            return "Observing table " + words[1] + "\r\n" + res;
         else
            return "That table doesn't exist or you're already at that table";
      } catch (Exception e) {
         return "Bad id";
      }      
   }

   private String handleStats(String[] words) {
      int[] stats = player.getStats();
      return "You've played " + stats[0] + " games." + 
             " You have won " + stats[1] + " of them, for a percentage of " +
             (stats[1] * 100.0)/stats[0] + "%. You have scored " + 
             stats[2] + " points total.";
   }

   private String handleShow(String[] words) {
      if (words.length == 1) {
         if (player.getTable() != null) {
            return player.getTable().show();
         } else {
            return "You're not at any table";
         }
      }
      // if they supplied an id, check to see if there's a table with that id
      try {
         int id = Integer.parseInt(words[1]);
         if (player.getTable() != null && player.getTable().getID() == id) {
            return player.getTable().show();
         }
         for (Table obsTable : player.getObs()) {
            if (obsTable.getID() == id) {
               return obsTable.show();
            }
         }
         return "You're not watching that table";
      } catch (Exception e) {
         return "Bad id";
      }      
   }

   private String handleMove(String[] words) {
      if (player.getTable() == null) {
         return "You're not at any table";
      }
      try {
         int x = Integer.parseInt(words[1]),
             y = Integer.parseInt(words[2]);
         if (player.getTable().makeMove(player, x, y))
            return "";
         return "Bad move\r\n" + player.getTable().show();
      } catch (Exception e) {
         return "Bad x and/or y";
      }      
   }

   private String handleMode(String[] words) {
      if (words.length < 2)
         return "Please enter a valid mode";
      String mode = words[1];
      switch (mode) {
         case "telnet":
            player.setMode(Player.TELNET);
            break;
         case "java":
            player.setMode(Player.JAVA);
            break;
         default:
            return "Unknown mode";
      }
      return "Mode is now " + mode;
   }

   private String handleLeave(String[] words) {
      if (words.length == 1) {
         // then leave current table
         if (!player.hasTable()) {
            return "You're not at any table";
         }
         if (player.leaveTable()) {
            return "Left table";
         } else {
            return "It's not your turn. Type forfeit to forfeit the game.";
         }
      }
      try {
         int id = Integer.parseInt(words[1]);

         // see if this is their table
         if (player.getTable() != null && player.getTable().getID() == id) {
            player.leaveTable();
            return "Left table";
         }

         //else see if it's a table they're observing
         Table res = player.unObserveTable(id);
         if (res != null)
            return "No longer observing table " + words[1] + "\r\n" + res;
         return "You're not at that table";
      } catch (Exception e) {
         return "Bad id";
      }      
   }

   public String handleForfeit(String[] words) {
      if (!player.hasTable())
         return "You're not at a table.";
      player.forfeit();
      return "";
   }

   private String handleList(String[] words) {
      if (words.length == 1 || words[1].equals("tables"))
         return serv.listTables();
      return serv.listPlayers();      
   }

   private String handlePass(String[] words) {
      return player.pass();
   }

   private String handleHelp(String[] words) {
      return helpString;
   }

   private String handle(String message) {
      String[] words = message.trim().split(" ");
      String command = words[0];
      switch (command) {
         case "join":
            return handleJoin(words);
         case "newtable":
            return handleNewTable(words);
         case "nick":
            return handleNick(words);
         case "observe":
            return handleObserve(words);
         case "show":
            return handleShow(words);
         case "move":
            return handleMove(words);
         case "leave":
            return handleLeave(words);
         case "forfeit":
            return handleForfeit(words);
         case "list":
            return handleList(words);
         case "pass":
            return handlePass(words);
         case "stats":
            return handleStats(words);
         case "help":
            return handleHelp(words);
         case "mode":
            return handleMode(words);
         default:
            return "Unknown message: " + message;
      }
   }

   public void send(String msg) {
      if (msg != null && msg.length() > 0) {
         System.out.println("Sending to " + player.getNick() + ": '" + msg + "'");
         out.println(msg + "\r");
         if (player.getMode() == Player.JAVA)
            out.println("endmsg");
      }
   }
}