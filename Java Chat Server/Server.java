package exercise1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import formats.MsgFormatWithHandshake;

public class Server {
    private ServerSocket server = null;
    private int clientCount = 0;
    private Vector<Server.SocketHandler> clients = new Vector<Server.SocketHandler>();

    class SocketHandler implements Runnable {
        private Socket socket;

        private InputStream in = null;
        private OutputStream out = null;

        private int clientId;
        private String prefix;

        public SocketHandler(Socket socket, int clientId) throws IOException {
            this.socket = socket;
            this.clientId = clientId;
            this.prefix = "Client[" + clientId + "]: ";

            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        @Override
        public void run() {
            try {
                clients.add(this);
                int serverId = 1;

                MsgFormatWithHandshake.Handshake handshakeIn = MsgFormatWithHandshake.Handshake.parseDelimitedFrom(in);

                boolean someCondition = false; // Here we would do some interesting checking, for now we just use "false"

                MsgFormatWithHandshake.Handshake.Builder handshakeOut = MsgFormatWithHandshake.Handshake.newBuilder();
                handshakeOut.setId(handshakeIn.getId());
                handshakeOut.setError(false);
                handshakeOut.build().writeDelimitedTo(out);

                if(someCondition){
                    System.out.println("Error while connecting with " + handshakeIn.getId());
                } else {
                    System.out.println("Connection with " + handshakeIn.getId() + " established");

                    String msg = "";
                    while (!msg.equals("end")) {
                        try {
                            MsgFormatWithHandshake.Message fromClient = MsgFormatWithHandshake.Message.parseDelimitedFrom(in);
                            msg = fromClient.getMsg();
                            System.out.println(prefix + msg + ", Id: " + fromClient.getFr());
                            
                            MsgFormatWithHandshake.Message.Builder toClient = MsgFormatWithHandshake.Message.newBuilder();
                            toClient.setFr(serverId);
                            toClient.setTo(fromClient.getFr());
                            toClient.setMsg("Received the message " + msg);
                            toClient.build().writeDelimitedTo(out);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                System.out.println("Closing connection with " + prefix);
                clients.remove(this);

                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerOperator implements Runnable {
        private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        @Override
        public void run() {
            String command = "";
            while (true) {
                try {
                    command = input.readLine();
                    if (command.equals("num_users")) {
                        System.out.println("Users Connected: " + clients.size());
                    } else {
                        System.out.println("Invalid Command");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started");

            Server.ServerOperator operator = new Server.ServerOperator();
            new Thread(operator).start();

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client accepted");
                Server.SocketHandler handler = new Server.SocketHandler(socket, ++clientCount);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: server [port]");
            System.exit(0);
        }
        Server server = new Server(Integer.parseInt(args[0]));
    }
}