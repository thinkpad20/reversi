package reversi;

public class ReversiGame {
   private char board[][];
   private char turn = 'B';
   ReversiGame() {
      board = new char[8][8];
      reset();
   }

   char[][] copyBoard(char[][] board) {
      // later add thread safety
      char boardCopy[][] = new char[8][8];
      for (int i=0; i<8; ++i)
         for (int j=0; j<8; ++j)
            boardCopy[i][j] = board[i][j];
      return boardCopy;
   }

   void toggleTurn() {
      turn = (turn == 'B') ? 'W' : 'B';
   }

   public char getTurn() {
      return turn;
   }

   boolean placePiece(char piece, int x, int y) {
      // save a copy of the board to revert if the move doesn't surround anything
      char[][] boardCopy = copyBoard(board);

      //space must not be occupied
      if (x > 7 || 
          y > 7 || 
          piece != getTurn() || 
          board[x][y] != '.') {
         System.out.println("illegal space");
         return false;
      }

      board[x][y] = piece;
      if (exploreAndSet(piece, x, y) > 0) {
         toggleTurn();
         return true;
      } else {
         // if no score, revert back and return false
         board[x][y] = '.';
         System.out.println("no points scored");
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

   int exploreAndSet(char piece, int x, int y) {
      int pieces = 0;
      for (int dx = -1; dx <= 1; dx++) {
         for (int dy = -1; dy <= 1; dy++) {
            int ex = explore(piece, x + dx, y + dy, dx, dy, 0);
            if (ex > 0) set(piece, x + dx, y + dy, dx, dy);
            pieces += ex;
         }
      }
      return pieces;
   }

   int explore(char piece, int x, int y, int deltax, int deltay, int points) {
      if (x >= 8 || x < 0 || y >= 8 || y < 0 || board[x][y] == '.') return 0;
      if (board[x][y] == piece) return points;
      return explore(piece, x + deltax, y + deltay, deltax, deltay, points + 1);
   }

   void set(char piece, int x, int y, int deltax, int deltay) {
      if (board[x][y] == piece) return;
      board[x][y] = piece;
      set(piece, x + deltax, y + deltay, deltax, deltay);
   }

   @Override public String toString() {
      String ret = " ";
      for (int i=0; i<9; ++i) {
         if (i > 0)
            ret += "" + (i-1);
      }
      ret += "\n";
      for (int i=0; i<8; ++i) {
         ret += "" + i;
         for (int j=0; j<8; ++j) {
            switch(this.board[i][j]) {
               case '.': ret += "."; break;
               case 'B': ret += "B"; break;
               default: ret += "W"; break; 
            }
         }
         if (i == 0) ret += " Black: " + getScores()[0] + " points";
         if (i == 1) ret += " White: " + getScores()[1] + " points";
         ret += "\n";
      }
      return ret;
   }

   private void reset() {
      for (int i=0; i<8; ++i) {
         for (int j=0; j<8; ++j) {
            board[i][j] = '.';
         }
      }
      board[3][3] = board[4][4] = 'W';
      board[4][3] = board[3][4] = 'B';
   }

   public String respondToMessage(String msg) {
      if (msg.startsWith("PLACE")) {
         char piece = msg.charAt(5);
         int x = Integer.parseInt(Character.toString(msg.charAt(6)));
         int y = Integer.parseInt(Character.toString(msg.charAt(7)));
         if (placePiece(piece, x, y))
            return "OK";
         return "Error";
      }
      if (msg.startsWith("RESET")) {
         reset();
         return "OK";
      }
      return "UNKNOWN";
   }
}
