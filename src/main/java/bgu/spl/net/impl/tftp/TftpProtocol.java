package bgu.spl.net.impl.tftp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.impl.packets.PacketType;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.TftpConnections;
import java.util.concurrent.ConcurrentHashMap;


class holder{
    static ConcurrentHashMap<Integer, String> usersLoggedIn = new ConcurrentHashMap<Integer, String>(); 
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]> {

    private boolean shouldTerminate = false;
    private int connectionId;
    private short lastSentBlockNumber = (short)0;
    private FileOutputStream fos;
    private FileInputStream fis;
    private byte[] fileToWrite;
    private File fileToRead; 
    String folderPath = "Files" + File.separator;
    private TftpConnections<byte[]> connections;
    private short LastRecievedBlockNumberData = (short)0;
    private byte[] dirqToSend;
    private String filename;
    



    public TftpProtocol() {
		
    }


    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.connectionId = connectionId;
        this.connections = (TftpConnections<byte[]>)connections;
    }

    @Override
    public void process(byte[] msg) {
        printByteArrayInHex(msg); 
        
        PacketType pt = getPacketType(msg);
        if (pt != null)
            handleByType(pt, msg);

        else connections.send(connectionId,createError(0));
    }

    @Override
    public boolean shouldTerminate() {
        if(shouldTerminate){
            holder.usersLoggedIn.remove(connectionId);
            connections.disconnect(connectionId);        
        }
        return shouldTerminate;
    }


    private void handleWRQ(byte[] message) {

        String currfileName = new String(Arrays.copyOfRange(message, 2, message.length-1), StandardCharsets.UTF_8);
        System.out.println("created file: "+ currfileName+ "end");
        File currFileToWrite = new File(folderPath + File.separator + currfileName);

        // Check if the filename contains a 0 byte
        if (currfileName.contains("\0")) {
            connections.send(connectionId,createError(2));
            return;
        }

        if(!isLoggedIn()){
            connections.send(connectionId,createError(6));
            return;
            
        }

        else{
            try {
            if(!currFileToWrite.exists()){
                fileToWrite = new byte[0];
                filename = currfileName;
                LastRecievedBlockNumberData = (short)0;
                System.out.println("ack sent");
                printByteArrayInHex(createACK((short)0));
                connections.send(connectionId,createACK((short)0));
                return;

            }

            else{
                fileToWrite = null;
                filename = null;
                connections.send(connectionId,createError(5));
                return;

            }
            
            
        } catch (Exception e) {
            connections.send(connectionId,createError(2));
        }
    }
}

     

    public String byteToString(byte[] bytes) {
        // Assuming UTF-8 encoding, you can change it based on your requirements
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void printByteArrayInHex(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input byte array cannot be null");
        }
    
        for (byte b : data) {
            System.out.printf("%02X ", b); // Format as 2-digit hex with a space
        }
        System.out.println(); // Add a newline at the end
    }

    public static byte[] mergeArrays(byte[] arr1, byte[] arr2, byte[] arr3, byte[] arr4) {
        int totalLength = arr1.length + arr2.length + arr3.length + arr4.length;
        byte[] mergedArray = new byte[totalLength];
    
        System.arraycopy(arr1, 0, mergedArray, 0, arr1.length);
        System.arraycopy(arr2, 0, mergedArray, arr1.length, arr2.length);
        System.arraycopy(arr3, 0, mergedArray, arr1.length + arr2.length, arr3.length);
        System.arraycopy(arr4, 0, mergedArray, arr1.length + arr2.length + arr3.length, arr4.length);
    
        return mergedArray;
    }

    public static byte[] mergeArrays(byte[] arr1, byte[] arr2) {
        int totalLength = arr1.length + arr2.length;
        byte[] mergedArray = new byte[totalLength];
        System.arraycopy(arr1, 0, mergedArray, 0, arr1.length);
        System.arraycopy(arr2, 0, mergedArray, arr1.length, arr2.length);
        return mergedArray;
      }

    private void handleDATA(byte[] messageOfData) {
        
        printByteArrayInHex(messageOfData); 
        byte[] dataToWrite = Arrays.copyOfRange(messageOfData, 6, messageOfData.length);
        short currBlockNum = getBlockNum(messageOfData);
        if(currBlockNum == LastRecievedBlockNumberData + (short)1){
            try {
                fileToWrite = mergeArrays(fileToWrite, dataToWrite);
                // Check if the data array is long enough to create the ack
                if (messageOfData.length >= 6) {
                    byte[] ack = new byte[] { 0, 4, messageOfData[4], messageOfData[5]};
                    LastRecievedBlockNumberData = currBlockNum;
                    if(dataToWrite.length < 512){
                        System.out.println("finished upload");
                        System.out.println(filename);
                        
                        File currFileToWrite = new File(folderPath + File.separator + filename);
                        if(dataToWrite[dataToWrite.length-1] !=0){
                            fos = new FileOutputStream(currFileToWrite,true);
                            fos.write(fileToWrite);
                            fos.close();

                        }
                        currFileToWrite.createNewFile();
                        String name= currFileToWrite.getName();
                        connections.send(connectionId, ack);
                        fileToWrite = null;
                        filename = null;
                        currFileToWrite = null;
                        sendBCAST(name,"WRQ");
                        
                        return;
                    }
                    else{
                        connections.send(connectionId, ack);
                        return;

                    }
                    }
                    
                    else {
                    // Handle the case where the data array is too short
                    System.err.println("Invalid data format. Unable to create ACK.");
                    return;
                }
        } catch (IOException e) {
            // Handle the IOException (e.g., if there is an issue with file writing)
            connections.send(connectionId, createError(0));
        }
        
    }
    else
        connections.send(connectionId, createError(0));
        return;
}

    private void handleDISC(byte[] message){
        if(!isLoggedIn()){
            connections.send(connectionId,createError(6));
            return;
            }
            else{
                shouldTerminate = true;
                connections.send(connectionId, createACK((short)0));
                shouldTerminate();
            }
        }
    private byte[] createError(int value) {

        String ans = "";

        if (value == 0) {
            ans = "Not defined, see error message (if any).";
        }

        else if (value == 1) {
            ans = "File not found - RRQ DELRQ of non-existing file";
            System.out.println(ans);
        }

        else if (value == 2) {
            ans = "Access violation - File cannot be written, read or deleted.";
        }

        else if (value == 3) {
            ans = "Disk full or allocation exceeded - No room in disk.";
        }

        else if (value == 4) {
            ans = "Illegal TFTP operation - Unknown Opcode.";
        }

        else if (value == 5) {
            ans = "File already exists - File name exists on WRQ.";
        }

        else if (value == 6) {
            ans = "User not logged in - Any opcode received before Login completes.";
        }

        else {
            ans = "User already logged in - Login username already connected.";
        }

        byte[] ErrMsg = ans.getBytes();

        byte[] op = shortTobyte((short)5);

        byte[] errorType = shortTobyte((short)value);

        byte[] zero = new byte[]{0};

        byte[] errorMsg = mergeArrays(op, errorType, ErrMsg, zero);
        printByteArrayInHex(errorMsg);

        return errorMsg;

    }

    public short getBlockNum(byte[] bytes){
        return byteToShort(bytes, 4, 5);
    }

    public static byte[] addArrays(byte[] array1, byte[] array2, byte[] array3, byte[] array4) {
        int length = Math.min(Math.min(Math.min(array1.length, array2.length), array3.length), array4.length);
        byte[] result = new byte[length];

        for (int i = 0; i < length; i++) {
            result[i] = (byte) (array1[i] + array2[i] + array3[i] + array4[i]);
        }

        return result;
    }

    

    public byte[] shortTobyte(short a){
		return new byte []{(byte)(a >> 8) , (byte)(a & 0xff)};
	} 
    
	
    public short byteToShort(byte[] byteArr, int fromIndex, int toIndex){
        return (short) ((((short)(byteArr[fromIndex]) )) << 8 | (short)(byteArr[toIndex] & 0X00FF)); 
    }

    private void handleByType(PacketType pt, byte[] message) {
        System.out.println("message: "+ byteToString(message));
        switch (pt) {
            case RRQ:
            System.out.println("case rrq");
                handleRRQ(message);
                break;
            case WRQ:
            System.out.println("case wrq");
                handleWRQ(message);
                break;
            case DATA:
            System.out.println("case data");
                handleDATA(message);
                break;
            case ACK:
                handleACK(message);
                break;
            case ERROR:
                handleERROR(message);
                break;
            case DIRQ:
                handleDIRQ();
                break;
            case LOGRQ:
            System.out.println("case log");
                handleLOGRQ(message);
                break;
            case DELRQ:
                handleDERLQ(message);
                break;
            case DISC:
            System.out.println("case disc");
                handleDISC(message);
                break;
            default:
                connections.send(connectionId, createError(0));
                break;
        }
    }

    public PacketType getPacketType(byte[] message) {
        short b_short = byteToShort(message, 0, 1);
        return PacketType.valueOf(b_short);
    }


    private void handleLOGRQ(byte[] message) {
        byteToString(message);
        String name = getUsername(message);
        if(isLoggedIn()){
            System.out.println("logged in");
            connections.send(connectionId, createError(7));
            return;
        }

        else{
            holder.usersLoggedIn.put(connectionId,name);
            byte[] ack = createACK((short)0);
            connections.send(connectionId, ack);
            return;
        }
    }


    // Maybe should be part of encoder/decoder
    private String getUsername(byte[] arg) {

        String name = byteToString(Arrays.copyOfRange(arg, 2, arg.length));

        return name;

    }


    private void handleACK(byte[] message) {
        short ackBlockNumber = ByteBuffer.wrap(Arrays.copyOfRange(message, 2, 4)).getShort();
        if(ackBlockNumber == 0){
            return;
        }
        if (ackBlockNumber != lastSentBlockNumber) {
            connections.send(connectionId, createError(4));
            return;
        }
        try {
            if(fileToRead != null) {
                sendData();
                return;
            }
            else if(dirqToSend != null){
                sendDirqDataPacket();
                return;
            }
        }
        catch (Exception e) {
            connections.send(connectionId, createError(2));
        }
    }


    private void sendData() {

        try (FileInputStream fis = new FileInputStream(fileToRead);
            DataInputStream din = new DataInputStream(fis)){
            long start = (lastSentBlockNumber) * 512;
            fis.skip(start);
            long bytesRemainToRead = fileToRead.length() - start;
                if (bytesRemainToRead >= 0) {
                    byte[] onlyData = new byte[(int) Math.min(512, bytesRemainToRead)];
                    int bytesRead = din.read(onlyData);
                    if (bytesRead >= 0) {
                        System.out.println("sent" + lastSentBlockNumber);
                        sendDataPacket(onlyData, lastSentBlockNumber);
                    }
                    if(bytesRemainToRead<512){
                        fileToRead = null;
                        return;
                    }
                }
            }
        catch (IOException e) {
                connections.send(connectionId, createError(2));
            }

    }

    private void sendDataPacket(byte[] data, short blockNumber) { 
        lastSentBlockNumber++;
        printByteArrayInHex(data);
        byte[] op = new byte[]{0,3};
        byte[] size = shortTobyte((short) data.length);
        byte[] blockNum = shortTobyte(lastSentBlockNumber);
        byte[] dataPacket = mergeArrays(op,size,blockNum, data);
        connections.send(connectionId, dataPacket);
        
    }

    private void handleERROR(byte[] message) {
        System.out.println(byteToString(message));
    }


    private void handleRRQ(byte[] message) {
        if(!isLoggedIn()){
            connections.send(connectionId,createError(6));
            return;
        }

        String fileName = new String(Arrays.copyOfRange(message, 2, message.length-1), StandardCharsets.UTF_8);
        fileToRead = new File(folderPath + File.separator + fileName);

        if (!fileToRead.exists()) {
            System.out.println("exists" + fileToRead.exists());
            connections.send(connectionId,createError(1));
            return;
        }

        else{        
            try(FileInputStream fis = new FileInputStream(fileToRead)) {
                lastSentBlockNumber = 0;
                sendData();
        } catch (IOException e) {
            System.out.println("faied in try");
                

        }


        }
    }

    private byte[] createACK(short blockNum) {
        byte[] ack = new byte[4];
        ack[0] = (byte)0; 
        ack[1] = (byte)4;
        ack[2] = shortTobyte(blockNum)[0];
        ack[3] = shortTobyte(blockNum)[1];
        return ack;
    }

    public static byte[] mergeArrays(byte[] arr1, byte[] arr2, byte[] arr3) {
        int totalLength = arr1.length + arr2.length + arr3.length;
        byte[] mergedArray = new byte[totalLength];
        
        System.arraycopy(arr1, 0, mergedArray, 0, arr1.length);
        System.arraycopy(arr2, 0, mergedArray, arr1.length, arr2.length);
        System.arraycopy(arr3, 0, mergedArray, arr1.length + arr2.length, arr3.length);
        
        return mergedArray;
    }


    private void sendBCAST(String filename, String packString) {
        byte[] filenameBytes = filename.getBytes(StandardCharsets.UTF_8);
        byte[] op = shortTobyte((short)9);
        byte[] pt = new byte[1];
        if(packString == "DELRQ"){
            pt[0] = (byte)0;
        }
        else
            pt[0] = (byte)1;
        byte[] msg1 = mergeArrays(op, pt, filenameBytes);
        byte[] zero = new byte[]{(byte)0};
        byte[] msg2 = mergeArrays(msg1, zero);

        printByteArrayInHex(msg2);

        for(Integer id : holder.usersLoggedIn.keySet()){
            connections.send(id, msg2);
        }

    }



    private void handleDIRQ() {

        if(!isLoggedIn()){
            connections.send(connectionId,createError(6));
            return;
        }
        else{
            File dir = new File(folderPath);
            File[] listOfFiles = dir.listFiles();
            String allFiles= "";
            if (listOfFiles != null) {
                for (File f : listOfFiles) {
                    allFiles += f.getName() + '\0';
                }
                dirqToSend = Arrays.copyOf(allFiles.getBytes(), allFiles.getBytes().length-1); 
                printByteArrayInHex(dirqToSend);
                lastSentBlockNumber = 0;
                sendDirqDataPacket();
            } 
        }
    }

    private void sendDirqDataPacket() {
        if(dirqToSend!=null){
            int start = (lastSentBlockNumber) * 512;
            long bytesRemainToRead = dirqToSend.length - start;
                if (bytesRemainToRead >= 0) {
                    try(ByteArrayInputStream bais = new ByteArrayInputStream(dirqToSend)){
                        byte[] onlyData = new byte[(int) Math.min(512, bytesRemainToRead)];
                        if (bytesRemainToRead > 0) {
                            bais.read(onlyData, start, onlyData.length);
                            bais.close();
                            System.out.println("sent" + lastSentBlockNumber);
                            sendDataPacket(onlyData, lastSentBlockNumber);
                        } 
                        
                        
                    if(bytesRemainToRead < 512){
                        dirqToSend = null;
                    }
                        
                    } catch (Exception e) {
                        connections.send(connectionId, createError(0));
                    }
                
            }
        }
    }



    private void handleDERLQ(byte[] message) {
        if(!isLoggedIn()){
            connections.send(connectionId,createError(6));
            return;
        }

        String filename = new String(Arrays.copyOfRange(message, 2, message.length-1), StandardCharsets.UTF_8);
        System.out.println("DELETE file: "+ filename+ "end");
        File file = new File(folderPath + File.separator + filename);

        if (!file.exists()) {
            connections.send(connectionId,createError(1));
            return;
        }

        if (file.delete()) {
            byte[] ack = createACK((short) 0);
            connections.send(connectionId, ack);
            sendBCAST(filename, "DELRQ"); // Broadcast file deletion
            return;
        } else {
            connections.send(connectionId,createError(2));
            return;

        }
    }

    public boolean isLoggedIn(){
        if(holder.usersLoggedIn.containsKey(this.connectionId)){
            return true;
        }
        else return false;
    }

    
}
