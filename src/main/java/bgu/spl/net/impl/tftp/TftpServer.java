package bgu.spl.net.impl.tftp;
import bgu.spl.net.srv.Server;



public class TftpServer {
    public static void main(String[] args) {
        int port = 7777;
        Server.threadPerClient(
			port, //port
			() -> new TftpProtocol(), //protocol factory
			TftpEncoderDecoder::new).serve();
    }
}
