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
      tables.add(new Table());
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
      String response = processInput(inputAsString, "get");
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
      // goal is to convert XML representation, embedded
      // in xmlString, to instance of move object. Ideally
      // this could be done auto-magically, but these technologies
      // are heavyweight and not particularly robust. A nice 
      // compromise is to use a generic tree parsing methodology.
      // such exists for XML -- it is called DOM. DOM is mapped
      // to many languages and is robust and simple (if a bit
      // of a hack). JDOM is superior but not as uniformly adopted.

      // parse XML into DOM tree
      // getting parsers is longwinded but straightforward
      try {
         // get the document tree (will also test validation)
         Document document = getDOM(xmlString);
         // get root element
         Element root = document.getDocumentElement();
         // get the value of the type attribute
         String type = root.getAttribute("type");
         Element nickElt = 
             (Element) root.getElementsByTagName("nick").item(0);
         Element uuidElt = 
             (Element) root.getElementsByTagName("uuid").item(0);
         String nick = nickElt.getTextContent();
         if (nick == null) {
            return makeErrorResponse("You must provide a username.");
         }
         if (type.equals("register")) {
            /* will check if username exists and if not, give a uuid back */
            return makeRegistrationResponse(nick);
         }
         else if (type.equals("join")) {
            if (uuidElt == null) {
               return makeErrorResponse("No uuid provided.");
            } else {
               String uuid = uuidElt.getTextContent();
               Player p = players.get(nick);
               if (!uuid.equals(p.getUuid())) {
                  return makeErrorResponse("Invalid uuid given., '" + nick +
                                           "' has uuid '" + p.getUuid() + "'");
               }
               // look at all current tables and see if any have seats open,
               // if not then add player to that table. If all full, make
               // a new table
               for (Table t : tables) {
                  if (!t.isReady()) {
                     t.addPlayer(p);
                     return makeJoinResponse(t);
                  }
               }
               Table t = new Table(p);
               tables.add(t);
               return makeJoinResponse(t);
            }
         }
         else {
            return makeErrorResponse("As-yet unknown request type '" 
                                      + type + "'");
         }
      }
      catch (Exception e) {
         StringWriter sw = new StringWriter();
         PrintWriter pw = new PrintWriter(sw);
         e.printStackTrace(pw);
         return makeErrorResponse(sw.toString() + ". Original message: <!-- " + 
                                    xmlString + " -->");
      }
   }

   private String makeJoinResponse(Table t) {
      return "<?xml version=\"1.0\"?>" +
             "<response type=\"confirm\" request=\"join\">" +
             t.getInfoXML() + "</response>";
   }

   private String makeErrorResponse(String message) {
      return 
         "<?xml version=\"1.0\"?>" +
         "<response type=\"error\">" +
         "<message>" + message + "</message>\r\n" +
         "</response>\r\n";
   }

   private Player register(String nick) {
      if (players.containsKey(nick)) {
         return null;
      }
      String uuid = UUID.randomUUID().toString();
      /* store the generated nick/uuid combo */
      Player p = new Player(nick, uuid);
      players.put(nick, p);
      return p;
   }

   private String makeRegistrationResponse(String nick) {
      Player newPlayer = register(nick);
      if (newPlayer == null) {
         return makeErrorResponse("Username '" + nick + "' is already taken.");
      }
      return
         "<?xml version=\"1.0\"?>" +
         "<response type=\"confirm\" request=\"register\">" +
         "<nick>" + newPlayer.getNick() + "</nick>" +
         "<uuid>" + newPlayer.getUuid() + "</uuid>" + 
         "</response>";
   }

   private void validateXML(Document document) throws Exception {
      validator.validate(new DOMSource(document));
   }
}
