package exercise1;

import java.io.*;
import java.net.Socket;
import java.util.Random;

import formats.MsgFormatWithHandshake;

public class Client {
    private Socket socket = null;

    private InputStream in = null;
    private OutputStream out = null;

    private BufferedReader userInput = null;

    public Client(String address, int port) {
        try {
            socket = new Socket(address, port);
            System.out.println("Client started");

            in = socket.getInputStream();
            out = socket.getOutputStream();

            userInput = new BufferedReader(new InputStreamReader(System.in));

            int serverId = 1;
            int myId = new Random().nextInt(98) + 2;

            MsgFormatWithHandshake.Handshake.Builder handshakeOut = MsgFormatWithHandshake.Handshake.newBuilder();
            handshakeOut.setId(myId);
            handshakeOut.setError(false);
            handshakeOut.build().writeDelimitedTo(out);

            MsgFormatWithHandshake.Handshake handshakeIn = MsgFormatWithHandshake.Handshake.parseDelimitedFrom(in);

            if(handshakeIn.getError()){
                System.out.println("Could not connect with the server");
            } else {
                System.out.println("Connection with the server established");

                String msg = "";
                while (!msg.equals("end")) {
                    msg = userInput.readLine();
                    MsgFormatWithHandshake.Message.Builder toServer = MsgFormatWithHandshake.Message.newBuilder();
                    toServer.setFr(myId);
                    toServer.setTo(serverId);
                    toServer.setMsg(msg);
                    toServer.build().writeDelimitedTo(out);

                    MsgFormatWithHandshake.Message fromServer = MsgFormatWithHandshake.Message.parseDelimitedFrom(in);
                    System.out.println("Server Message: " + fromServer.getMsg() + ", Server Id: " + fromServer.getFr());
                }
            }

            userInput.close();
            out.close();
            in.close();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        Client client = new Client("127.0.0.1", 8080);
    }
}