package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;


/*
 * Implemention for the generic Connections<T>
 */
public class TftpConnections<T> implements Connections<T> {
    /*
     * We use connectionId as the key for the ConcurrentHashMap
     */
    ConcurrentHashMap<Integer, BlockingConnectionHandler<T>> connections;

    public TftpConnections(){
        connections = new ConcurrentHashMap<>();
    }

    public void connect(int connectionId, BlockingConnectionHandler<T> handler){
        if(!connections.containsKey(connectionId))
            connections.put(connectionId, handler);
    }

    @Override
    public boolean send(int connectionId, T msg) {
        //check if client is exist
        BlockingConnectionHandler<T> handler = connections.get(connectionId);
        System.out.println("send connection");
        handler.send(msg);
        return true;
        
    }

    public void sendToAll(T msg) {
        for (BlockingConnectionHandler<T> handler : connections.values()) {
                handler.send(msg);
            }
    }

    @Override
    public void disconnect(int connectionId) {
        BlockingConnectionHandler<T> handler = connections.get(connectionId);
        try {
            handler.close();
            connections.remove(connectionId);
        } catch (Exception e) {
            // do nothing?
        }
    }




public static String byteToString(byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
}
}
