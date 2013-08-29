package reversi;
import reversi.*;
import java.util.LinkedList;
import java.util.Collection;

public class Table {
   private String name;
   private static int numTables = 0;
   private int id, current;
   private int[] recentMove;
   private boolean[] passedLastTurn;
   private Game game;
   private Player[] players;
   private Collection<Player> observers;
   private boolean recentPass; // true if last turn, player passed

   public Table() {
      this.id = numTables++;
      game = new Game();
      players = new Player[2]; // 0 is black, 1 is white
      observers = new LinkedList<Player>();
      recentPass = false;
      current = -1;
      recentMove = new int[2];
      passedLastTurn = new boolean[2]; // true if player passed last turn 
   }

   public Table(Player p) {
      this();
      addPlayer(p);
   }

   public int getID() {
      return id;
   }

   public synchronized boolean addPlayer(Player player) {
      if (isReady())
         return false;
      for (int i=0; i<2; ++i)
         if (players[i] == null) {
            players[i] = player;
            break;
         }
      player.setTable(this);
      return true;
   }

   public String getBoardXML() {
      StringBuffer buf = new StringBuffer("<board dim=\"8\">");
      for (int i = 0; i < 8; ++i) {
         buf.append("<row num=\"" + i + "\">");
         buf.append(new String(game.getRow(i)));
         buf.append("</row>");
      }
      buf.append("</board>");
      return buf.toString();
   }

   public String getInfoXML() {
      // return tableInfo after a join; might include the board if
      // table is ready
      StringBuffer buf = new StringBuffer("<tableInfo>");
      buf.append("<tableid>" + getID() + "</tableid>");
      buf.append("<ready>" + (isReady() ? "true" : "false") + "</ready>");
      buf.append("<blackPlayer>" + getBlackNick() + "</blackPlayer>");
      buf.append("<whitePlayer>" + getWhiteNick() + "</whitePlayer>");
      // if two players are at the table, return the board, score and turn
      if (isReady()) {
         buf.append(getBoardXML());
         int[] scores = game.getScores();
         buf.append("<blackScore>" + scores[0] + "</blackScore>");
         buf.append("<whiteScore>" + scores[1] + "</whiteScore>");
         buf.append("<turn>" + (current == 1 ? "white" : "black") + "</turn>");
      }
      buf.append("</tableInfo>");
      return buf.toString();
   }

   public synchronized boolean addPlayer(Player player, int posn) {
      if (players[posn] == null) {
         players[posn] = player;
         return true;
      }
      return false;
   }

   public synchronized boolean removePlayer(Player player) {
      endGame();
      for (int i=0; i<2; ++i) 
         if (players[i] == player) {
            players[i] = null;
            return true;
         }
      return false;
   }

   private String getBlackNick() {
      if (players[0] == null) return "none";
      return players[0].getNick();
   }

   private String getWhiteNick() {
      if (players[1] == null) return "none";
      return players[1].getNick();
   }

   public synchronized void forfeitPlayer(Player player) {
      if (!isReady()) {
         notify("Attempt to forfeit but no game is active.");
         return;
      }
      if (player == players[0] && players[1] != null) {
         players[1].incGamesWon(game.getScores()[1]);
      } else if (player == players[1] && players[0] != null) {
         players[0].incGamesWon(game.getScores()[0]);
      } else {
         return;
      }
      notify("Player " + player.getNick() + " has forfeited.");
      game.reset();
   }

   public synchronized boolean tryLeave(Player player) {
      if (!isReady() || currentPlayer() == player) {
         return removePlayer(player);
      }
      return false;
   }

   public boolean addObserver(Player player) {
      if (observers.contains(player)) return false;
      observers.add(player);
      return true;
   }

   public boolean removeObserver(Player player) {
      if (observers.contains(player)) {
         observers.remove(player);
         return true;
      }
      return false;
   }

   public synchronized boolean isReady() { 
      return (players[0] != null && players[1] != null); 
   }

   public String show() {
      if (!isReady())
         return "Waiting for another player";
      return game.toString();
   }

   private void toggleCurrent() {
      // toggle current player
      current = (current == 1) ? 0 : 1;
   }

   private void updatePlayers() {
      // for (Player player : players) {
      //    if (player != null)
      //       player.send(getRecentMoveString());
      //       if (player.getMode() == Player.JAVA)
      //          player.send(game.toString());
      //       else
      //          player.send(game.prettyPrint());
      //       player.send(turnAnnouncement(player));
      // }
      // for (Player player : observers) {
      //    if (player != null)
      //       player.send(getRecentMoveString());
      //       player.send(game.prettyPrint());
      //       player.send(turnAnnouncement(player));
      // }
   }

   private void notify(String msg) {
      // for (Player player : players) {
      //    if (player != null)
      //       player.send(msg);
      // }
      // for (Player player : observers) {
      //    if (player != null)
      //       player.send(msg);
      // }
   }

   private void endGame() {
      int[] scores = game.getScores();
      if (isReady()) {
         if (scores[0] > scores[1])
            players[0].incGamesWon(scores[0]);
         else if (scores[1] > scores[0]) 
            players[1].incGamesWon(scores[1]);
         else {
            players[0].addPoints(scores[0]);
            players[1].addPoints(scores[1]);
         }
      }
      notify("Game has ended. Score was black " 
               + scores[0] + ", white " + scores[1]);
      game.reset();
   }



   public boolean passTurn(Player player) {
      if (currentPlayer() != player)
         return false;
      if (passedLastTurn[current]) {
         endGame();
      } else {
         passedLastTurn[current] = true;
      }
      toggleCurrent();
      return true;
   }

   public Player currentPlayer() {
      if (current == 0 || current == 1)
         return players[current];
      return players[0];
   }

   private Player prevPlayer() {
      return players[(current == 1) ? 0 : 1];
   }

   private String currentColor() {
      return (current == 0) ? "Black" : 
             (current == 1) ? "White" :
             "None";
   }

   private String getRecentMoveString() {
      if (currentColor().equals("None"))
         return "New game\r\n";
      String move = "moved to " + recentMove[0] + ", " + recentMove[1];
      if (recentPass)
         move = "passed their turn";
      return currentColor() + " (" + currentPlayer().getNick() + ") " + move + "\r\n";
   }

   private String turnAnnouncement(Player player) {
      if (currentPlayer() == player)
         return "Your turn to play";
      return currentPlayer().getNick() + "'s turn to play";
   }

   // used when someone successfully places a piece
   private void update(int x, int y) {
      toggleCurrent();
      recentMove[0] = x;
      recentMove[1] = y;
      updatePlayers();
      recentPass = false;
   }

   public boolean makeMove(Player player, int x, int y) {
      char toPlay = (players[0] == player) ? 'B' :
                    (players[1] == player) ? 'W' :
                    '.';
      if (toPlay != '.' && game.placePiece(toPlay, x, y)) {
         update(x, y);
         // update passedLastTurn to indicate was not a pass
         passedLastTurn[toPlay == 'B' ? 0 : 1] = false;
         return true;
      }
      return false;
   }

   @Override public String toString() {
      String res = "Table " + id;
      res += "\r\n\tBlack: " + ((players[0] != null) ? players[0].getNick() : "none");
      res += "\r\n\tWhite: " + ((players[1] != null) ? players[1].getNick() : "none");
      if (observers.size() > 0) {
         res += "\r\n\tObservers:";
         for (Player player : observers)
            res += "\r\n\t\t" + player;
      }
      return res;
   }
}

class Game {
   private char board[][];
   public Game() {
      board = new char[8][8];
      reset();
   }

   public char[] getRow (int row) {
      return board[row];
   }

   public boolean hasValidMove(char color) {
      for (int i=0; i<8; ++i) {
         for (int j=0; j<8; ++j) {
            if (exploreAndSet(color, i, j) > 0)
               return true;
         }
      }
      return false;
   }

   public boolean placePiece(char color, int x, int y) {
      //space must not be occupied
      if (x > 7 || y > 7 || board[x][y] != '.') {
         return false;
      }

      if (exploreAndSet(color, x, y) > 0) {
         board[x][y] = color;
         return true;
      } else {
         // if no score, revert back and return false
         return false;
      }
   }

   public int[] getScores() {
      int[] scores = new int[2];
      for (int i=0; i<8; ++i) {
         for (int j=0; j<8; ++j) {
            if (board[i][j] == 'B')
               scores[0]++;
            if (board[i][j] == 'W')
               scores[1]++;
         }
      }
      return scores;
   }

   private int exploreAndSet(char color, int x, int y) {
      int colors = 0;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            int ex = explore(color, x + dx, y + dy, dx, dy, 0);
            if (ex > 0) set(color, x + dx, y + dy, dx, dy);
            colors += ex;
         }
      }
      return colors;
   }

   // recursively check to see how many colors would be flipped in some given direction
   private int explore(char color, int x, int y, int deltax, int deltay, int points) {
      if (x >= 8 || x < 0 || y >= 8 || y < 0 || board[x][y] == '.') return 0;
      if (board[x][y] == color) return points;
      return explore(color, x + deltax, y + deltay, deltax, deltay, points + 1);
   }

   // recursively flip colors in some direction
   private void set(char color, int x, int y, int deltax, int deltay) {
      if (board[x][y] == color) return;
      board[x][y] = color;
      set(color, x + deltax, y + deltay, deltax, deltay);
   }

   public String prettyPrint() {
      StringBuffer ret = new StringBuffer(" ");
      for (int i=0; i<9; ++i) {
         if (i > 0)
            ret.append("" + (i-1));
      }
      ret.append("\r\n");
      for (int i=0; i<8; ++i) {
         ret.append("" + i);
         for (int j=0; j<8; ++j) {
            ret.append(board[i][j]);
         }
         ret.append("\r\n");
      }
      return ret.toString();
   }

   @Override public String toString() {
      StringBuffer ret = new StringBuffer("board\r\n" + 8 + " ");
      for (int i=0; i<8; ++i) {
         for (int j=0; j<8; ++j) {
            ret.append(board[i][j]);
         }
      }
      return ret.toString();
   }

   public void reset() {
      for (int i=0; i<8; ++i) {
         for (int j=0; j<8; ++j) {
            board[i][j] = '.';
         }
      }
      board[3][3] = board[4][4] = 'W';
      board[4][3] = board[3][4] = 'B';
   }
}