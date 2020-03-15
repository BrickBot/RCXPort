/**
 * @(#) RCXPacket.java 0.1 99/03/12
 *
 *  Copyright (C) 1998, Scott B. Lewis.  All Rights Reserved.
 *
 *  License to copy, use, and modify this software is granted provided that
 *  this notice is retained in any copies of any part of this software.
 *
 *  The author makes no guarantee that this software will compile or
 *  function correctly.  Also, if you use this software, you do so at your
 *  own risk.
 * 
 *  Scott B. Lewis.  slewis@teleport.com  http://www.slewis.com
 */
package rcxport;

import java.io.Serializable;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Packet for communication with RCX.  This class encodes a byte array
 * provided upon construction as an RCX packet according to the 
 * {@link <a href="http://www.crynwr.com/lego-robotics/">RCX Internals</a>} web page.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
class RCXPacket implements Serializable
{
    public static final byte PACKETHEADER1 = (byte) 0x55;
    public static final byte PACKETHEADER2 = (byte) 0xff;
    public static final byte PACKETHEADER3 = (byte) 0x00;
    
    /**
     * @serial myData the data to be in the packet
     */
    protected byte [] myData;
    /**
     * @serial mySendData the actual packet sent (including checksum and etc.)
     */
    protected byte [] mySendData;
    
    // Remember last command
    private static byte lastCommand=0;
    
    /**
     * Constructor.  Data provided should be a byte array of length >= 1.
     */
    protected RCXPacket(byte [] data)
    {
        myData = data;
    }
    /**
     * Writes this packet to the given output stream.
     *
     * @param os the OutputStream to write to
     * @exception IOException thrown if there is no data to write, or the write fails for
     * some reason
     */
    protected void writePacket(OutputStream os) throws IOException
    {
        if (myData == null || myData.length < 1) throw new IOException("No data in packet to send");
        mySendData = getBytes();
        // send it
        os.write(mySendData);
        os.flush();
        //System.out.println("Wrote packet: "+this);
    }
    /**
     * This gets the actual bytes for transmission over the comm port.
     *
     * @return byte[] the bytes to send
     */
    private byte[] getBytes()
    {
        byte [] sendData = new byte[myData.length*2+5];
        sendData[0] = PACKETHEADER1;
        sendData[1] = PACKETHEADER2;
        sendData[2] = PACKETHEADER3;
        
        int index = 3;
        int checkSum=0;
        
        // Correction for last command
        if (myData[0]==lastCommand) {
            myData[0] ^= 8;
        }
        lastCommand = myData[0];
        for(int i=0; i < myData.length; i++) {
            sendData[index]=myData[i];
            sendData[index+1]=(byte) ((~myData[i])&0xff);
            checkSum+=myData[i];
            index+=2;
        }
        sendData[index]=(byte) checkSum;
        sendData[index+1]=(byte) ~checkSum;
        return sendData;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer("RCXPacket[");
        if (myData == null) {
            sb.append(myData).append("]");
            return sb.toString();
        }
        
        for(int i=0; i < myData.length; i++) {
            if (i != 0) sb.append(" ");
            sb.append(RCXCmd.makeString(myData[i]));
        }
        sb.append("]");
        return sb.toString();
    }
}

