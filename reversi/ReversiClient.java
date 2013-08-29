package reversi;
import reversi.*;
import java.io.*;
import  java.net.*;
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
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.ArrayList;
import javax.xml.transform.dom.DOMSource;
import java.util.UUID;
import java.util.HashMap;

public class ReversiClient {
    private String nick, uuid, color, tableid;
    private boolean playing;
    private int wins, losses, points;
    private double ratio;
    private URL url;
    private static boolean verbose = true;
    private Scanner in = new Scanner(System.in);

    private String makeXML(String type, String content) {
        String base = "<?xml version=\"1.0\" ?><request "+
                        "type=\"%s\"><nick>%s</nick><uuid>%s"+
                        "</uuid>%s</request>";
        return String.format(base, type, nick, uuid, content);
    }

    // convenience method
    private String makeXML(String type) {
        return makeXML(type, "");
    }

    public ReversiClient(String url) throws MalformedURLException {
        this.url = new URL(url);
        // keep looping until a valid username is found
        while (true) {
            System.out.print("Please enter a nick, or 'exit' to quit: ");
            String input = in.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                System.exit(0);
            }
            nick = input;
            if (register()) break;
        }
        System.out.println("Successfully registered as " + nick + ".");
    }

    public ReversiClient(String url, String nick) 
                                   throws MalformedURLException {
        this.url = new URL(url);
        this.nick = nick;
        if (!register()) {
            throw new RuntimeException("Failed to register nick " + nick);
        }
    }

    private Element findSubelem(Element elem, String name) {
        // Given an element, finds first subelement with given name
        NodeList nodes = elem.getElementsByTagName(name);
        if (nodes.getLength() > 0)
            return (Element) nodes.item(0);
        return null;
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

    // convenience method
    private Element makeRequest(String xml) {
        return makeRequest(xml, "POST");
    }

    // tells us if response is confirm, update or error
    private String responseType(Element resp) {
        return resp.getAttribute("type");
    }

    // tells us which request the server is responding to
    private String responseRequest(Element resp) {
        return resp.getAttribute("request");
    }

    private Element makeRequest(String xml, String method) {
        if (verbose) System.out.println("Request:\n" + xml);
        try {
            // open a connection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // set HTTP properties
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("Content-Language", "en-US");  
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // set up output stream
            PrintWriter pw = new PrintWriter(conn.getOutputStream());
            pw.write(xml);
            pw.close();

            // get response
            BufferedReader in = 
                new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                    );
            StringBuffer retVal = new StringBuffer();
            String line;
            while ((line = in.readLine()) != null) {
                retVal.append(line + "\n");
            }

            // close connection
            conn.disconnect();

            String resp = retVal.toString();
            if (verbose) System.out.println("Response: \n" + resp);

            // convert to Document
            try {
                Document document = getDOM(resp);

                if (document == null) {
                    throw new RuntimeException("Malformed XML:" + resp);
                }
                // return root element
                return document.getDocumentElement();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not connect to server");
            System.exit(0);
            return null;
        }
    }

    private boolean register() {
        String reg = 
            "<?xml version=\"1.0\" ?><request "+
            "type=\"register\"><nick>" + nick + "</nick></request>";
        Element resp = makeRequest(reg);
        if (resp.getAttribute("type").equals("confirm")) {
            Element uiElem = findSubelem(resp, "regInfo");
            if (uiElem == null)
                throw new RuntimeException("Didn't get a ui from the server");
            
            uuid = findSubelem(uiElem, "uuid").getTextContent();
            return true;
        } else if (resp.getAttribute("type").equals("error")) {
            System.out.println("Error: ");
            Element message = findSubelem(resp, "message");
            System.out.println(message.getTextContent());
        }
        return false;
    }

    private void join(String color, String tableid) {
        String contents = "";
        if (color.length() > 0) {
            contents = "<color>" + color + "</color>";
        }
        if (tableid.length() > 0) {
            contents += "<tableid>" + tableid + "</tableid>";
        }
        // System.out.println("Sending join request to the server");
        update(makeRequest(makeXML("join", contents)));
    }

    private void join() {
        join("", "");
    }

    private void move(int row, int col) {
        System.out.printf("%s moves to %d, %d\n", nick, row, col);
        String posn = "<position><row>" + row +
                      "</row><col>" + col + "</col></position>";
        update(makeRequest(makeXML("move", posn)));
    }

    private void pass() {
        System.out.printf("%s passes turn\n", this.nick);
        update(makeRequest(makeXML("pass")));
    }

    // updates by sending an update request to server
    private void update() {
        String tbl = "";
        // if we have a table that we're at, add it to the request
        if (tableid != null) {
            tbl = "<tableid>" + tableid + "</tableid>";
        }
        update(makeRequest(makeXML("update", tbl), "GET"));
    }

    // update win/loss/points stats
    private void stats() {
        Element resp = makeRequest(makeXML("stats"), "GET");
    }

    private void listPlayers() {
        Element resp = makeRequest(makeXML("listPlayers"), "GET");
    }

    private void listTables() {
        Element resp = makeRequest(makeXML("listTables"), "GET");
    }

    // updates based on the response to some request we sent
    private void update(Element resp) {
        if (resp == null) {
            throw new RuntimeException("Malformed XML in response?");
        }

        if (resp.getAttribute("type").equals("error")) {
            System.out.println("Looks like we've got an error:");
            Element message = findSubelem(resp, "message");
            System.out.println(message.getTextContent());
            return;
        }

        if (resp.getAttribute("type").equals("gameover")) {
            System.out.println("Game over!");
            return;
        }

        Element tableInfo = findSubelem(resp, "tableInfo");
        if (tableInfo == null) {
            throw new RuntimeException("tableInfo element not found");
        }

        if (tableid == null)
            tableid = findSubelem(tableInfo, "tableid").getTextContent();

        // see if we're the black player or the white player
        String black = 
            findSubelem(tableInfo, "blackPlayer").getTextContent();
        String white = 
            findSubelem(tableInfo, "whitePlayer").getTextContent();
        if (nick.equals(black))
            color = "black";
        else if (nick.equals(white))
            color = "white";
        // otherwise, this isn't our table that we're looking at...

        // check if they've sent a board
        Element board = findSubelem(tableInfo, "board");
        if (board != null) {
            printBoard(board);
            String blackPlayer = 
                findSubelem(tableInfo, "blackPlayer").getTextContent();
            String whitePlayer = 
                findSubelem(tableInfo, "whitePlayer").getTextContent();
            int blackScore = Integer.parseInt(
                findSubelem(tableInfo, "blackScore").getTextContent());
            int whiteScore = Integer.parseInt(
                findSubelem(tableInfo, "whiteScore").getTextContent());
            // see whose turn it is
            String turn = findSubelem(tableInfo, "turn").getTextContent();
            System.out.printf("Black: %s, %d points\n"+
                              "White: %s, %d points\n%s's turn\n",
                              blackPlayer, blackScore,
                              whitePlayer, whiteScore, turn);
        }

        Element userInfo = findSubelem(resp, "userInfo");
        if (userInfo != null) {
            ratio = Double.parseDouble(
                        findSubelem(userInfo, "ratio").getTextContent()
                    );
            points = Integer.parseInt(
                        findSubelem(userInfo, "points").getTextContent()
                    );
        }
    }

    private void help() {
        System.out.println("Available commands:\n"+
                           "\thelp: to see this menu\n"+
                           "\tjoin: to join a table\n"+
                           "\tmove: to make a move\n"+
                           "\tpass: to pass your turn\n"+
                           "\tleave: to exit a table\n"+
                           "\tupdate: to get the latest at your table\n"+
                           "\tobserve: see the state of a table\n" +
                           "\ttables: to see all tables on the server\n" +
                           "\tplayers: to see all players on the server"
            );          
    }

    private int[] getMove() {
        // get row and column from user input
        System.out.print("Enter row: ");
        int row = Integer.parseInt(in.nextLine());
        System.out.print("Enter column: ");
        int column = Integer.parseInt(in.nextLine());
        // package row and column in array and return
        int[] move = {row, column};
        return move;
    }

    private void joinTable() {
        System.out.print("Enter a color (press enter if no preference): ");
        String color = in.nextLine();
        if (!color.equals("black") && !color.equals("white")) {
            System.out.println("Error: invalid color selected. No preference");
            color = "";
        }
        // make this a loop so that if the user wants, they can see all of
        // the tables available to them
        while(true) {
            System.out.print("Enter table id (type list for list, "+
                             "or enter for the first open table): ");
            
            String input = in.nextLine();
            if (input.equals("list")) {
                // send a list tables request
                listTables();
                // loop will now repeat
            } else if (input.length() == 0) {
                // if 
                join();
                break;
            } else {
                join(input, color);
                break;
            }
        }
    }

    public void printBoard(Element board) {
        // build arraylist of rows
        NodeList rows = board.getElementsByTagName("row");
        ArrayList<String> rowStrings = new ArrayList<String>();
        for (int i = 0; i < rows.getLength(); ++i)
            rowStrings.add(((Element)rows.item(i)).getTextContent());
        // render the board
        System.out.println(Renderer.render(rowStrings));
    }

    public void run() {
        while (true) {
            System.out.print("Enter command (type help for list): ");
            String input = in.nextLine();
            if (input.equalsIgnoreCase("exit"))
                return;
            else if (input.equalsIgnoreCase("join")) {
                joinTable();
            }
            else if (input.equalsIgnoreCase("move")) {
                int[] move = getMove();
                move(move[0], move[1]);
            }
            else if (input.equalsIgnoreCase("pass")) {
                pass();
            }
            else if (input.equalsIgnoreCase("update")) {
                update();
            }
            else if (input.equalsIgnoreCase("help")) {
                help();
            }
            else if (input.equalsIgnoreCase("tables")) {
                listTables();
            }
            else if (input.equalsIgnoreCase("players")) {
                listPlayers();
            }
            else {
                System.out.println("Unrecognized command '" + input + "'");
            }
        }
    }

    public static void test(String nick1, String nick2) 
                        throws MalformedURLException {
        String url = "http://localhost:8080/reversi";
        ReversiClient cli1 = new ReversiClient(url, nick1);
        ReversiClient cli2 = new ReversiClient(url, nick2);
        cli1.join();
        cli2.join();
        cli1.update();
        cli1.move(2, 3);
        cli1.move(2, 3);
        cli2.update();
        cli2.move(2, 2);
        cli1.update();
        cli1.move(3,2);
        cli2.update();
        cli2.move(2, 4);
        cli1.update();
        cli1.move(3, 5);
        cli2.move(4,2);
        cli1.update();
        cli1.pass();
        cli2.move(3, 6);
        cli1.pass();
        cli1.update();
        cli2.update();
        // cli2.move
    }

    public static void main(String[] args) throws MalformedURLException {
        if (args.length > 0 && args[0].equals("test"))
            test("foo", "bar");
        else {
            String url = "http://localhost:8080/reversi";
            //initialize the client
            ReversiClient cli = new ReversiClient(url);
            cli.run();
        }
    }
}

class Renderer {
    public static String makeTop(int n) {
        StringBuffer res = new StringBuffer("    ");
        for (int i = 0; i < n; ++i) {
            res.append("  " + i + " ");
        }
        return res.toString() + "\n";
    }
    public static String makeLine(String cs, int row) {
        StringBuffer res = new StringBuffer("  " + row + " ");
        for (int i=0; i<cs.length(); ++i) {
            if (i == 0) res.append("|");
            res.append(" " + cs.charAt(i) + " |");
        }
        res.append("\n");
        return res.toString();
    }
    public static String makePlusLine(String cs) {
        StringBuffer res = new StringBuffer("    ");
        for (int i=0; i<cs.length(); ++i) {
            if (i == 0) res.append("+");
            res.append("---+");
        }
        res.append("\n");
        return res.toString();
    }
    public static String render(ArrayList<String> rows) {
        StringBuffer res = new StringBuffer(makeTop(rows.size()));
        int mid = rows.size() / 2;
        for (int i=0; i<rows.size(); ++i) {
            String row = rows.get(i);
            if (i == 0) 
                res.append(makePlusLine(row));
            res.append(makeLine(row, i));
            res.append(makePlusLine(row));
        }
        return res.toString();
    }
}