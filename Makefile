# yes, this makefile is lame... oh well

all: reversi/Server.class reversi/Player.class reversi/Connection.class reversi/Table.class reversi/Client.class
	javac reversi/Server.java reversi/Player.java reversi/Connection.java reversi/Table.java reversi/Client.java
	
clean:
	rm reversi/*.class

server: all
	@java reversi.Server -p 7890

client: all
	@java reversi.Client -p 7890