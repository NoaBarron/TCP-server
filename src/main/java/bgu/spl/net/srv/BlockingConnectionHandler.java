package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;



public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    public final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;


    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }


    @Override
    
    public void run() {

        System.out.println("connection handler run");

        try (Socket sock = this.sock) { 
            int read;
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    System.out.println("got a message");
                    protocol.process(nextMessage);
                }
            }

            System.out.println("im hereeeeeeee");

        } catch (IOException ex) {
            System.out.println("im hereeeeeeee");
            ex.printStackTrace();
        }
        System.out.println("im hereeeeeeee");
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public synchronized void send(T msg) {
        System.out.println("send handler");
        try{
            System.out.println(msg == null);
            if (msg != null) {
                 //maybe a bug here if "out" is not initialized
                out.write(encdec.encode(msg));
                out.flush();
            }
        }catch (IOException e){
            e.printStackTrace();
            System.out.println(System.err);
        }
    }


    public static String byteToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
