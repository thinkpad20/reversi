CLASSFILES = reversi/ReversiServlet.class reversi/Table.class reversi/Player.class\
				 reversi/Connection.class reversi/Server.class reversi/Game.class
SOURCES = reversi/ReversiServlet.java reversi/Table.java reversi/Player.java\
				 reversi/Connection.java reversi/Server.java

all: clean httpclient servlet restart

restart:
	-catalina stop
	catalina start

servlet: reversi/ReversiServlet.class
	cp $(CLASSFILES) /usr/local/Cellar/tomcat/7.0.42/libexec/webapps/ROOT/WEB-INF/classes/reversi

client: reversi/HttpClient.java
	javac reversi/HttpClient.java

run: all
	java reversi.ReversiClient

runtest: all
	sleep 2
	java reversi.ReversiClient test

reversi/ReversiServlet.class: reversi/ReversiServlet.java
	javac -cp /usr/local/Cellar/tomcat/7.0.42/libexec/lib/servlet-api.jar $(SOURCES)

httpclient: reversi/ReversiClient.java
	javac reversi/ReversiClient.java

# sockets: reversi/Server.class reversi/Player.class reversi/Connection.class reversi/Table.class reversi/Client.class
# 	javac reversi/Server.java reversi/Player.java reversi/Connection.java reversi/Table.java reversi/Client.java
	
clean:
	rm -f reversi/*.class

socketserver: sockets
	@java reversi.Server -p 7890

socketclient: sockets
	@java reversi.Client -p 7890