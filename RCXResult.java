/**
 * @(#) RCXResult.java 0.1 98/03/12
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

import java.io.IOException;
import java.io.InputStream;

/**
 * Holds result information in response to a an RCXPacket.  
 * This class gets
 * the raw bytes from the InputStream provided in the constructor, reads 
 * them in and checks them as dictated by the 
 * {@link <a href="http://www.crynwr.com/lego-robotics/">RCX Internals</a>} web page.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
class RCXResult 
{
    public static final int BUFFSIZE = 4096;
    
    private byte [] myBuffer = new byte[BUFFSIZE];
    private int myNumRead;
    private byte [] myReturnBuffer = new byte[BUFFSIZE];
    private int myReturnBufferLength;
    
    /**
     * Protected constructor so instances can only be made by RCXPort class.
     *
     * @param ins the InputStream to read the result data from
     * @param aPacket the RCXPacket sent (so validity checks can be performed
     * on the response data.
     */
    protected RCXResult(InputStream ins, RCXPacket aPacket) throws IOException
    {
        while (true) {
            int read = ins.read(myBuffer, myNumRead, BUFFSIZE - myNumRead);
            if (read == 0) throw new IOException("No response. Packet: "+this);
            myNumRead += read;
            if (checkData(aPacket)) {
                //System.out.println("Got result "+this);
                break;
            }
        }
    }
    /**
     * Check the result data for validity against the packet sent.  This does 
     * basic checksum checks and throws an IOException if problems are found.  
     * This is 
     *
     * @param packet the RCXPacket sent to the RCX
     * @exception IOException thrown if there are problems with the result
     * data
     */
    private boolean checkData(RCXPacket packet) throws IOException
    {
        // check that we've received enough bytes.  The minimum number is
        // the echo length, plus 3 header bytes, plus 1 response/echo, and the checksum 
        // and checksum echo = 7.
        int rcvLength = packet.mySendData.length + 7;
        if (myNumRead < rcvLength) {
            // If not, then return false and read some more
            return false;
        }
        // Read enough bytes, now check echo for validity
        int i=0;
        for(; i < packet.mySendData.length; i++) {
            if (packet.mySendData[i] != myBuffer[i]) {
                throw new IOException("RCX echo not valid.  Packet: "+this);
            }
        }
        
        // Check headers
        if (myBuffer[i++]!= RCXPacket.PACKETHEADER1 ||
            myBuffer[i++]!= RCXPacket.PACKETHEADER2 ||
            myBuffer[i++]!= RCXPacket.PACKETHEADER3) throw new IOException("RCX response had bad header.  Packet: "+this);
        
        int retPos = 0;
        int sum = 0;
        myReturnBufferLength = 0;
        // Check for shadow and get checksum
        for(; i < myNumRead - 2; i+=2) {
            if (myBuffer[i] != (~myBuffer[i+1]&(byte)0xff)) {
                throw new IOException("RCX corrupt response.  Packet: "+this);
            }
            sum+=myBuffer[i];
            if (retPos < BUFFSIZE) {
                myReturnBuffer[retPos++] = myBuffer[i];
                myReturnBufferLength++;
            }
        }
        // Verify length and checksum shadow
        if (i != myNumRead -2) throw new IOException("RCX bad response.  Packet: "+this);
        if (myBuffer[i] != (~myBuffer[i+1]&((byte)0xff))) throw new IOException("RCX bad sum/checksum compare.  Packet: "+this);
        // Verify checksum
        if (sum != myBuffer[i]) throw new IOException("RCX bad checksum.  Packet: "+this);
        return true;
    }
    /**
     * Gets the entire packet received from the RCX.  This includes all of the
     * original packet sent, checksums, and all the other stuff as described in the
     * {@link <a href="http://www.crynwr.com/lego-robotics/">RCX Internals</a>} web page.
     * 
     * @return byte[] bytes representing full packet from RCX
     */
    public byte[] getFullPacket()
    {
        return copyBytes(myBuffer, myNumRead);
    }
    /**
     * Gets the actual result from packet received from RCX.  These are just the
     * important bytes in the result (after removing shadowing, checksums, etc.).
     *
     * @return byte[] bytes representing just the result from RCX
     */
    public byte[] getResult()
    {
        return copyBytes(myReturnBuffer, myReturnBufferLength);
    }
    /**
     * Create a copy of the first num bytes from given byte array.
     *
     * @param arr the src array
     * @param num the number of elements to copy (starting from 0).
     * @return byte[] the new bytes
     */
    private byte [] copyBytes(byte [] arr, int num)
    {
        byte [] retBytes = new byte[num];
        System.arraycopy(arr, 0, retBytes, 0, num);
        return retBytes;
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer("RCXResult[");
        for(int i=0; i < myNumRead; i++) {
            sb.append(RCXCmd.makeString(myBuffer[i])).append(" ");
        }
        sb.append("return(");
        for(int i=0; i < myReturnBufferLength; i++) {
            if (i > 0) sb.append(" ");
            sb.append(RCXCmd.makeString(myReturnBuffer[i]));
        }
        sb.append(")]");
        return sb.toString();
    }
}