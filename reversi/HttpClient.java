package reversi;
import  reversi.*;
import  java.net.*;
import  java.io.*;
import  java.util.Scanner;
import  javax.xml.parsers.DocumentBuilderFactory;
import  javax.xml.parsers.DocumentBuilder;
import  org.w3c.dom.Document;
import  javax.xml.transform.TransformerFactory;
import  javax.xml.transform.Transformer;
import  javax.xml.transform.dom.DOMSource;
import  javax.xml.transform.stream.StreamResult;
import  org.w3c.dom.Element;
import  org.w3c.dom.Text;
import  java.io.StringWriter;


//java HWClientExample 

public class HttpClient {

   private static int id = 200;
   private static String color = "white";

   public static void main(String[] args) throws Exception{
      Scanner keyInput = new Scanner(System.in);
      boolean gameOver = false;
      while (!gameOver){
         System.out.print(">>");
         int move    = Integer.parseInt(keyInput.next());
         String xmlMove = marshalMoveToXML(move);
         //send xml to servlet
         System.out.println("sending input XML to server");
         String reply = postToServlet(xmlMove);
         System.out.println(reply);
         //Get Response
      }
   }

   /*
   take a move int and create corresponding XML representation to 
   send to server.ex
   <move id="100">
   <location>22</location>
   <color>white</color>
   </move>
   */
   private static String marshalMoveToXML(int loc) throws Exception{
   //obviously this first part should be done once per game, not for each move
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.newDocument();
      Element root = document.createElement("move");
      document.appendChild(root);

      root.setAttribute("id",Integer.toString(id));


      Element locEl   = document.createElement("location");
      Element colorEl = document.createElement("color");
      root.appendChild(locEl);
      root.appendChild(colorEl);

      Text text = document.createTextNode(Integer.toString(loc));
      locEl.appendChild(text);

      text = document.createTextNode(color);
      colorEl.appendChild(text);
      //now that I have a DOM representation of the requested move,
      //convert the DOM into a XML String to be sent to server
      StringWriter sw = new StringWriter();
      TransformerFactory tFactory =
      TransformerFactory.newInstance();
      Transformer transformer = tFactory.newTransformer();
      DOMSource source = new DOMSource(document);
      StreamResult result = new StreamResult(sw);
      transformer.transform(source, result);
      return sw.toString();
   }

   private static String postToServlet(String xmlMove) throws Exception{
      URL url = new URL("http://localhost:8080/reversi");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setRequestProperty("Content-Type", "text/xml");
      //conn.setRequestProperty("Content-Length", "" +  8);
      conn.setRequestProperty("Content-Language", "en-US");  
      conn.setDoInput(true);
      conn.setDoOutput(true);
      PrintWriter pw = new PrintWriter(conn.getOutputStream());
      pw.write(xmlMove);
      pw.close();

      Scanner in = new Scanner(conn.getInputStream());
      StringBuffer retVal = new StringBuffer();
      while (in.hasNext())
         retVal.append(in.next());

      conn.disconnect();

      return (retVal.toString());

   }
}