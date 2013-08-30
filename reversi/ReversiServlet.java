package reversi;
import reversi.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.*;
import javax.xml.XMLConstants;
import java.util.Scanner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.DOMException;
import java.util.ArrayList;
import javax.xml.transform.dom.DOMSource;
import java.util.UUID;
import java.util.HashMap;

//a proof-of-concept servlet to play the role of reversi server
//based on XML tunneled over http
public class ReversiServlet extends HttpServlet {
   // this method is called once when the servlet is first loaded
   // board object here
   ArrayList<Table> tables = new ArrayList<Table>();
   Schema requestSchema;
   Validator validator;
   HashMap<String, Player> players;
   public void init() {
      // initialize board
      players = new HashMap<String, Player>();
      // initialize schema
      SchemaFactory sf = 
         SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      // load schema into memory
      try {
         File schemaFile = new File("request.xml");
         requestSchema = sf.newSchema(schemaFile);
         validator = requestSchema.newValidator();
      } catch (Exception e) {
         System.out.println("Error loading schema: ");
         e.printStackTrace();
      }
   }

   static String message;

   public void doPost(HttpServletRequest req,
                      HttpServletResponse res)
                      throws IOException, ServletException {
      Scanner in = new Scanner(req.getInputStream());
      in.useDelimiter("\n"); // set so that it doesn't eliminate whitespace
      StringBuffer input = new StringBuffer();
      // get everything available in the input buffer
      while (in.hasNext())
          input.append(in.next());
      // put to string
      String inputAsString = input.toString();
      // process and get response
      String response = processInput(inputAsString);
      // write response to output
      PrintWriter out = res.getWriter();
      out.print(response);
   }

   public void doGet(HttpServletRequest req,
                      HttpServletResponse res)
                      throws IOException, ServletException {
      Scanner in = new Scanner(req.getInputStream());
      in.useDelimiter("\n"); // set so that it doesn't eliminate whitespace
      StringBuffer input = new StringBuffer();
      // get everything available in the input buffer
      while (in.hasNext())
          input.append(in.next());
      // put to string
      String inputAsString = input.toString();
      // process and get response
      String response = processInput(inputAsString);
      // write response to output
      PrintWriter out = res.getWriter();
      out.print(response);
   }

   private Document getDOM(String xml) throws Exception {
      // parses into a DOM and validates at the same time. If not
      // valid, throws an exception.
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();

      Document document =
         builder.parse(new InputSource(new StringReader(xml)));
      // validateXML(document);
      return document;
   }

   private String processInput(String xmlString) {
      try {
         // get the document tree (will also test validation)
         Document document = getDOM(xmlString);
         if (document == null) {
            return makeErrorResponse("Malformed XML", "unknown");
         }
         // get root element
         Element root = document.getDocumentElement();
         // get the value of the type attribute
         String type = root.getAttribute("type");
         Element nickElt = 
             (Element) root.getElementsByTagName("nick").item(0);
         Element uuidElt = 
             (Element) root.getElementsByTagName("uuid").item(0);

         // make sure user has provided a nick
         if (nickElt == null) {
            return makeErrorResponse("You must provide a username.", type);
         }
         String nick = nickElt.getTextContent();
          
         // ensure this player exists  
         Player p = players.get(nick);
         if (p == null && !type.equals("register")) {
            return makeErrorResponse("That nick doesn't exist.", type);
         }
         // otherwise, generate a new player (not registered yet)
         else if (type.equals("register")) {
            p = new Player(nick, UUID.randomUUID().toString());
         }
         // ensure user has provided a uuid
         if (uuidElt == null && !type.equals("register")) {
            return makeErrorResponse("You must provide a uuid", type);
         }
         // ensure that the uuid provided is correct
         if (uuidElt != null && 
               !uuidElt.getTextContent().equals(p.getUuid())) {
            return makeErrorResponse("Incorrect uuid, can't verify", type);
         }
         return handle(root, type, p);
      } catch (Exception e) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         return makeErrorResponse(sw.toString() + ". Original message: <!-- " + 
                                    xmlString + " -->", "unknown");
      }
   }

   private String handle(Element root, String type, Player p) {
      StringBuffer res = new StringBuffer();
      Table t = p.getTable();
      if (type.equals("register")) {
         /* will check if username exists and if not, give a uuid back */
         if (!register(p)) {
            return makeErrorResponse("Username '" + p.getNick() + 
                                     "' is already taken.", type);
         }
         return makeRegInfoResponse(p);
      }
      else if (type.equals("join")) {
         // check if player is already at a table: if so, error.
         if (p.getTable() != null) {
            return makeErrorResponse("You're already at a table." +
               " If you want to leave this table, " +
               "try the 'leave' command.", type);
         }
         // look at all current tables and see if any have seats open,
         // if so then add player to that table. If all full, make
         // a new table
         for (Table tbl : tables) {
            if (!tbl.isReady()) {
               tbl.addPlayer(p);
               return makeTableInfoResponse(tbl, type);
            }
         }
         Table newTable = new Table(p);
         tables.add(newTable);
         return makeTableInfoResponse(newTable, type);
      }
      else if (type.equals("update")) {
         res.append("<?xml version=\"1.0\"?>" +
                    "<response type=\"confirm\" request=\"update\">");
         // first send their user info
         res.append(p.getInfoXML());
         // find the table this player is at, and send its info back.
         if (p.getTable() != null) {
            res.append(p.getTable().getInfoXML());
         }
         res.append("</response>");
         return res.toString();
      }
      else if (type.equals("listPlayers")) {
         res.append("<?xml version=\"1.0\"?>" +
                       "<response type=\"confirm\" "+
                       "request=\"listPlayers\">");
         for (Player pl : players.values()) {
            res.append(pl.getInfoXML());
         }
         res.append("</response>");
         return res.toString();
      }
      else if (type.equals("listTables")) {
         res.append("<?xml version=\"1.0\"?>" +
                       "<response type=\"confirm\" "+
                       "request=\"listTables\">");
         for (Table tbl : tables) {
            res.append(tbl.getInfoXML());
         }
         res.append("</response>");
         return res.toString();
      }
      else if (type.equals("observe")) {
         Element tableid = 
            (Element) root.getElementsByTagName("tableid").item(0);
         if (tableid == null) {
            return makeErrorResponse("No tableid specified", type);
         }
         try {
            int tid = Integer.parseInt(tableid.getTextContent());
            return makeTableInfoResponse(tables.get(tid),type);
         } catch (Exception e) {
            return makeErrorResponse("Invalid tableid", type);
         }
      }
      else if (type.equals("move")) {
         // make sure the player is at a table
         if (t == null) {
            return makeErrorResponse("You aren't at any table", type);
         }
         // make sure it's that player's turn
         if (t.currentPlayer() != p) {
            return makeErrorResponse("It's not your turn", type);
         }
         try {
            // get the row and column
            int row = Integer.parseInt(
               ((Element) root.getElementsByTagName("row").
                              item(0)).getTextContent()
            );
            int col = Integer.parseInt(
               ((Element) root.getElementsByTagName("col").
                              item(0)).getTextContent()
            );
            // ensure the player's table is ready
            if (!t.isReady()) {
               return makeErrorResponse("Still waiting for an opponent", type);
            }
            // try to make the move; if successful send the new table back,
            // else error
            if (t.makeMove(p, row, col)) {
               return makeTableInfoResponse(t, type);
            } else {
               return makeErrorResponse("Invalid move", type);
            }
         } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return makeErrorResponse(sw.toString() + " (sorry)", type);
         }
      }
      else if (type.equals("pass")) {
         // make sure the player is at a table
         if (t == null) {
            return makeErrorResponse("You aren't at any table", type);
         }
         // make sure it's that player's turn
         if (t.currentPlayer() != p) {
            return makeErrorResponse("It's not your turn", type);
         }
         try {
            // try to pass; if successful send the new table back,
            // else error
            if (t.passTurn(p)) {
               return makeTableInfoResponse(t, type);
            } else {
               return makeErrorResponse("Invalid move", type);
            }
         } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return makeErrorResponse(sw.toString() + " (sorry)", type);
         }
      }
      else if (type.equals("stats")) {
         // request for a player's stats. Could be this player, or another
         // assume we want this player's stats
         Player target = p;
         // then see if he's asking for someone else instead
         Element playerElem = 
            (Element) root.getElementsByTagName("player").item(0);
         if (playerElem != null) {
            String targetNick = playerElem.getTextContent();
            // if so, look that player up
            target = players.get(targetNick);
            // make sure they exist
            if (target == null)
               return makeErrorResponse("Player " + targetNick + " does not " +
                                        "exist on server.", type);
         }
         return makePlayerInfoResponse(target, type);
      }
      else {
         return makeErrorResponse("As-yet unknown request type", type);
      }
   }

   private String makeTableInfoResponse(Table t, String request) {
      return "<?xml version=\"1.0\"?>" +
             "<response type=\"confirm\" request=\"" + request + "\">" +
             t.getInfoXML() + "</response>";
   }

   private String makePlayerInfoResponse(Player p, String request) {
      return "<?xml version=\"1.0\"?>" +
             "<response type=\"confirm\" request=\"" + request + "\">" +
             p.getInfoXML() + "</response>";
   }

   private String makeErrorResponse(String message, String request) {
      return 
         "<?xml version=\"1.0\"?>" +
         "<response type=\"error\" request=\"" + request + "\">" +
         "<message>" + message + "</message>\r\n" +
         "</response>\r\n";
   }

   private boolean register(Player p) {
      // don't allow duplicate nicks, and "none" is reserved
      String nick = p.getNick();
      if (players.containsKey(nick) || nick.equalsIgnoreCase("none")) {
         return false;
      }
      /* store the generated nick/uuid combo */
      players.put(nick, p);
      return true;
   }

   private String makeRegInfoResponse(Player p) {
      return
         "<?xml version=\"1.0\"?>" +
         "<response type=\"confirm\" request=\"register\">" +
         "<regInfo><nick>" + p.getNick() + "</nick>" +
         "<uuid>" + p.getUuid() + "</uuid></regInfo></response>";
   }

   private void validateXML(Document document) throws Exception {
      validator.validate(new DOMSource(document));
   }
}
