package reversi;
import reversi.*;
import java.net.Socket;
import java.util.Scanner;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.LinkedList;

public class Client {
   InetAddress addr;
   int port;
   public Client(InetAddress addr, int port) {
      this.addr = addr;
      this.port = port;
   }

   public void start() throws Exception {
      Socket sock;
      try {
         sock = new Socket(this.addr, this.port);
         Scanner keyIn = new Scanner(System.in);
         PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
         Thread listenThread = new Thread(new ResponseListener(sock));
         listenThread.start();
         while (true) {
            String input = keyIn.nextLine();
            out.println(input);
            if (input.equalsIgnoreCase("exit")) break;
            if (!listenThread.isAlive()) break;
         }
         sock.close();
      } catch (Exception e) {
         e.printStackTrace();
         return;
      }
   }

   public static void main(String[] args) {
      try {
         int port = 8189;
         String hostname = "localhost";
         for (int i=0; i<args.length; ++i) {
            if (args[i].equals("-p") && i < args.length - 1) {
               try {
                  port = Integer.parseInt(args[++i]);
               } catch (Exception e) {
                  System.out.println("Please enter a valid port, or none");
                  System.exit(0);
               }
            }
            if (args[i].equals("-s") && i < args.length - 1) {
               hostname = args[++i];
            }
         }
         Client client = new Client(InetAddress.getByName(hostname), port);
         client.start();
      } catch (Exception e) {
         System.out.println("" + e);
      }
   }
}

class ResponseListener implements Runnable {
   Socket sock;
   Scanner in;
   LinkedList<String> lines;

   ResponseListener(Socket sock) throws Exception { 
      this.sock = sock; 
      in = new Scanner(sock.getInputStream());
      lines = new LinkedList<String>();
   }

   private void printLineList() {
      if (lines.getFirst().equals("board")) {
         lines.removeFirst();
         System.out.println(Renderer.render(lines.removeFirst()));
      }
      for (String line : lines)
         if (!line.equals("endmsg"))
            System.out.println("--> " + line);
      lines.clear();
   }

   public void run() {
      while (true) {
         String fromServer;
         try {
            fromServer = in.nextLine();
         } catch (Exception e) {
            System.out.println("Server disconnected");
            System.exit(0);
            break;
         }
         lines.add(fromServer.trim());
         if (lines.getLast().equals("endmsg")) {
            printLineList();
         }
      }
   }
}

class Renderer {
   public static String makeLine(String cs) {
      StringBuffer res = new StringBuffer();
      for (int i=0; i<cs.length(); ++i) {
         if (i == 0) res.append("|");
         res.append(" " + cs.charAt(i) + " |");
      }
      res.append("\n");
      return res.toString();
   }
   public static String makePlusLine(String cs) {
      StringBuffer res = new StringBuffer();
      for (int i=0; i<cs.length(); ++i) {
         if (i == 0) res.append("+");
         res.append("---+");
      }
      res.append("\n");
      return res.toString();
   }
   public static String render(String board) {
      String[] words = board.trim().split(" ");
      StringBuffer res = new StringBuffer();
      int dim = Integer.parseInt(words[0]);
      int k = 0;
      for (int i=0; i<dim; ++i) {
         String line = words[1].substring(i * dim, i * dim + dim);
         if (i == 0) 
            res.append(makePlusLine(line));
         res.append(makeLine(line));
         res.append(makePlusLine(line));
      }
      return res.toString();
   }
}