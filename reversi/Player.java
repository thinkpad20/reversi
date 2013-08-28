package reversi;
import reversi.*;
import java.util.LinkedList;
import java.util.Collection;

public class Player extends Thread {
   private int gamesPlayed, gamesWon, totalPoints;
   private Table table;
   private Collection<Table> currentObservations;
   private String nick, uuid;
   private Connection con;
   public static final int JAVA = 0, TELNET = 1;
   private int mode;

   public Player() {
      gamesWon = gamesPlayed = 0;
      table = null;
      currentObservations = new LinkedList<Table>();
      System.out.println("Created new player");
      mode = JAVA;
   }

   public Player(String nick, String uuid) {
      this();
      setNick(nick);
      setUuid(uuid);
   }

   private void setUuid(String uuid) {
      this.uuid = uuid;
   }

   public String getUuid() {
      return uuid;
   }

   public int[] getStats() {
      int[] stats = { gamesPlayed, gamesWon, totalPoints };
      return stats;
   }

   public void setCon(Connection con) {
      // not relevant in HTTP, only for sockets
      this.con = con;
   }

   public void setNick(String nick) {
      this.nick = nick;
   }

   public String getNick() {
      return hasNick() ? nick : "(no nick)";
   }

   public boolean hasNick() {
      return nick != null;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   public int getMode() {
      return mode;
   }

   public boolean leaveTable() {
      if (table != null && table.tryLeave(this)) {
         table = null;
         return true;
      }
      return false;
   }

   public boolean hasTable() {
      return table != null;
   }

   public boolean joinTable(Table table) {
      if (this.table == null) {
         this.table = table;
         return true;
      }
      return false;
   }

   public Table getTable() {
      return table;
   }

   public void setTable(Table t) {
      this.table = t;
   }

   public void observeTable(Table t) {
      currentObservations.add(t);
   }

   public Table unObserveTable(int id) {
      for (Table obsTable : currentObservations) {
         if (obsTable.getID() == id) {
            obsTable.removeObserver(this);
            currentObservations.remove(obsTable);
            return obsTable;
         }
      }
      return null;
   }

   public Collection<Table> getObs() {
      return currentObservations;
   }

   public String pass() {
      if (table != null) {
         if (table.passTurn(this))
            return "OK";
         else
            return "It's not your turn";
      } else {
         return "You're not at any table";
      }      
   }

   public void forfeit() {
      table.forfeitPlayer(this);
   }

   public void incGamesPlayed() { gamesPlayed++; }

   public void incGamesWon(int pts) { gamesWon++; addPoints(pts); }

   public void addPoints(int pts) { totalPoints += pts; }

   public void send(String msg) {
      con.send(msg);
   }

   @Override public String toString() {
      String tbl = (table == null) ? "(no table)" : "" + table.getID();
      StringBuffer res = new StringBuffer(getNick() + " on table: " + tbl);
      if (currentObservations.size() > 0) {
         res.append("Observing:\r\n");
         for (Table table : currentObservations)
            res.append("" + table.getID() + "\r\n");
      }
      return res.toString();
   }

   public String getInfoXML() {
      StringBuffer res = new StringBuffer("<userInfo>");
      if (getNick() != null)
         res.append("<nick>" + getNick() + "</nick>");
      if (getUuid() != null)
         res.append("<uuid>" + getUuid() + "</uuid>");
      if (getTable() != null)
         res.append("<table>" + getTable().getID() + "</table>");
      res.append("<points>" + totalPoints + "</points>");
      res.append("<ratio>" + ((double)gamesWon/gamesPlayed) + "</ratio>");
      res.append("</userInfo>");
      return res.toString();
   }
}