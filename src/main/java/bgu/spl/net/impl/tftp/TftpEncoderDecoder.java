
package bgu.spl.net.impl.tftp;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    private byte[] bytes = new byte[1 << 9]; 
    private int len = 0;

    public byte[] shortTobyte(short a){
		return new byte []{(byte)(a >> 8) , (byte)(a & 0xff)};
	}
	
    public short byteToShort(byte[] byteArr, int fromIndex, int toIndex){
        return (short) ((((short)(byteArr[fromIndex]) )) << 8 | (short)(byteArr[toIndex] & 0X00FF)); 
    }


    @Override
public byte[] decodeNextByte(byte nextByte) {
    Byte nexByteWrap = nextByte;
    pushByte(nextByte);

    if (len >= 2) {
        // Get packet type according to first 2 bytes
        String packetType = getPacketType(bytes);

        // Decode according to packet type
        if (packetType == "LOGRQ" || packetType == "DELRQ"|| packetType == "RRQ"|| packetType == "WRQ"|| packetType =="BCAST"|| packetType =="ERROR"){
            if (nexByteWrap.intValue() == 0) {
                return popArray();
            }
            return null;
        }


        else if (packetType =="DATA"){      
            if (len >= 4) {
            int size = byteToShort(bytes, 2,3) + 6;
            if (len == size) {
                return popArray();
            }
        }
        return null;
    }

        else if (packetType =="ACK"){
            if (len == 4) {
                return popArray();
            }
            return null;
        }


        else if (packetType =="DIRQ" || packetType =="DISC"){
            System.out.println("case disc");
            return popArray();
        }

        else{
            System.out.println("uknown packet type");
                // Unknown packet type, handle accordingly or return an error
        }
    }

    return null;
}

        

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private byte[] popArray(){
        byte[] output = Arrays.copyOf(bytes, len);  //cut the array to the message size
        len = 0;
        return output;
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len] = nextByte;
        len++;
}

public String getPacketType(byte[] message) {
       short value =  byteToShort(message, 0, 1);
        try {
            if(value == (short)1){
                return "RRQ";
            }
            if(value == (short)2){
                return "WRQ";
            }
            if(value == (short)3){
                return "DATA";
            }
            if(value == (short)4){
                return "ACK";
            }
            if(value == (short)5){
                return "ERROR";
            }
            if(value == (short)6){
                return "DIRQ";
            }
            if(value == (short)7){
                return "LOGRQ";
            }
            if(value == (short)8){
                return "DELRQ";
            }
            if(value == (short)9){
                return "BCAST";
            }
            if(value == (short)10){
                return "DISC";
            }
            
        } catch (Exception e) {
            System.out.println("cant find packet type");

        }
        return null;

    }


}