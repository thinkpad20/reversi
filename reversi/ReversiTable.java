package reversi;
import reversi.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Collection;

public class ReversiTable {
   private String name;
   private int id;
   private ReversiGame game;
   private ReversiPlayer[] players;
   private Collection<ReversiPlayer> observers;
   ReversiTable(int id) {
      this.id = id;
      players = new ReversiPlayer[2]; // 0 is black, 1 is white
      observers = new LinkedList<ReversiPlayer>();
   }

   public int getID() {
      return id;
   }

   public boolean addPlayer(ReversiPlayer player) {
      return addPlayer(player, 0) ? true : addPlayer(player, 1);
   }

   public boolean isReady() { 
      return (players[0] != null && players[1] != null); 
   }

   public boolean addPlayer(ReversiPlayer player, int posn) {
      if (players[posn] == null) {
         players[posn] = player;
         return true;
      }
      return false;
   }

   @Override public String toString() {
      String res = "Table " + id;
      res += "\n\tBlack: " + players[0];
      res += "\n\tWhite: " + players[1];
      res += "\n\tObservers:";
      for (ReversiPlayer player : observers)
         res += "\n\t\t" + player;
      return res;
   }
}