/**
 * @(#) RCXPort.java 0.1 98/03/12
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
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileReader;
import java.io.Reader;
import java.io.BufferedReader;

// Classes needed from Java Comm API for communicating over the serial port.
import javax.comm.CommPortIdentifier;
import javax.comm.SerialPort;

/**
 * Top-level interface to the RCX.  This is the top-level interface for interacting
 * with the Lego Mindstorms Robotics Kit RCX.  It uses the communication protocol described 
 * and documented in the
 * {@link <a href="http://www.crynwr.com/lego-robotics/">RCX Internals</a>} web page.
 * The primary method for sending RCX byte codes to the RCX is the </b>downloadProgram</b>
 * method in this class.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
public class RCXPort
{
    public static final int INSBUFF = 4096;
    public static final int OUTBUFF = 4096;
    public static final String RCXPORTNAME = "RCXPort";
    public static final int PORTOPENTIMEOUT = 1000;
    public static final int PORTREADTIMEOUT = 1000;
    
    public static final String FILE_DELIMITERS = " ,\t";
    // Default radix for reading byte codes from file is hex
    public static final int FILE_RADIX = 16;
    
    public static final byte DOWNLOADCHUNK = 20;
    
    public static final byte DEFAULTRETRYCOUNT = 3;
    public static final byte DOWNLOAD_SOUND = 5;
    
    private String myPortName;
    private OutputStream myOutputStream;
    private InputStream myInputStream;
    private CommPortIdentifier myPortIdentifier;
    private SerialPort myPort;
    
    private boolean mySynched = false;
    
    /**
     * Create an interface to the RCX.  Opens and prepares comm port for use.  Will
     * throw an exception if comm port is not available.
     *
     * @param port the name of the system port to use.  This name will be system
     * dependent (e.g. "COM1").
     * @exception Exception thrown if an identifier cannot be gotten from
     * CommPortIdentifier.getPortIdentifier(), or if other comm port 
     * problems occur during initialization.
     */
    public RCXPort(String port) throws Exception
    {
        myPortName = port;
        myPortIdentifier = CommPortIdentifier.getPortIdentifier(myPortName);
        try {
            myPort = (SerialPort) myPortIdentifier.open(RCXPORTNAME, PORTOPENTIMEOUT);
    	    myPort.setSerialPortParams(2400,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,
                                	    SerialPort.PARITY_ODD);
            myPort.enableReceiveTimeout(PORTREADTIMEOUT);
            //myPort.enableReceiveThreshold(1);
            myOutputStream = new BufferedOutputStream(myPort.getOutputStream(), INSBUFF);
            myInputStream = myPort.getInputStream();
        } catch (Exception e) {
            // problem so close
            if (myPort != null) myPort.close();
            throw e;
        }
    }
    /**
     * Send data to the RCX, and get an RCXResult back.  This is the primary
     * interface for communicating with the RCX.
     *
     * @param data the byte array to send.  Must not be null and should have length longer than 0.
     * @return res a valid RCXResult returned from the RCX.
     * @exception IOException thrown if port has previously been closed, the packet
     * provided is null, or some problem sending packet or receiving result from RCX
     */
    public synchronized RCXResult sendData(byte [] data, boolean retry) throws IOException
    {
        if (myPort==null) throw new IOException("Port closed");
        // Check that we haven't been given bogus data
        if (data==null || data.length == 0) throw new IOException("Null data");
        int retries = (retry)?DEFAULTRETRYCOUNT:1;
        IOException last = null;
        // Read anything from input buffer
        if (myInputStream.available() != 0) while (myInputStream.read() != -1) ;
        for(int i=0; i < retries; i++) {
            try {    
                // Send packet
                RCXPacket p = new RCXPacket(data);
                p.writePacket(myOutputStream);
                return new RCXResult(myInputStream, p);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }
    /**
     * Alternative to sendData/2 that has a retry by default.
     *
     * @param data the data to send
     * @exception IOException thrown if port has previously been closed, the packet
     * provided is null, or some problem sending packet or receiving result from RCX
     */
    public RCXResult sendData(byte [] data) throws IOException
    {
        return sendData(data, true);
    }
    /**
     * Get port name.
     *
     * @return String name of port being used.  System dependent.
     */
    public String getPortName()
    {
        return myPortName;
    }
    /**
     * Close interaction with this port.
     */
    public synchronized void close()
    {
        if (myPort != null) {
            myPort.close();
            myPort = null;
        }
    }
    
    public void sync() throws IOException
    {
        if (mySynched) return;
        ping();
        mySynched = true;
    }
    
    public void ping() throws IOException
    {
        sendData(RCXCmd.set(RCXCmd.Ping));
    }
    
    public void deleteSubs() throws IOException
    {
        sendData(RCXCmd.set(RCXCmd.DeleteSubs));
    }
    public void deleteTasks() throws IOException
    {
        sendData(RCXCmd.set(RCXCmd.DeleteTasks));
    }
    public void selectProgram(byte prog) throws IOException
    {
        sendData(RCXCmd.set(RCXCmd.SelectProgram, prog));
    }
    public void playSound(byte sound) throws IOException
    {
        ping();
        sendData(RCXCmd.makePlaySound(sound));
    }
    
    /**
     * Download given program and optionally run it.
     *
     * @param aProg the RCXProgram to download to the RCX.
     * @param run if true, immediately run the downloaded program.  If false, just
     * do the download
     * @exception IOException thrown if some problem communicating with the RCX.
     */
    public void downloadProgram(RCXProgram aProg, boolean run) throws IOException
    {
        if (aProg == null) return;
        sync();
        RCXResult res = sendData(RCXCmd.set(RCXCmd.StopAll));
        selectProgram((byte) aProg.getProgramNum());
        deleteTasks();
        deleteSubs();
        // Download subroutines
        aProg.downloadSubroutines(this);
        // Download tasks
        aProg.downloadTasks(this);
        // Play sound when done with download
        playSound(DOWNLOAD_SOUND);
        if (run) {
            // start program
            startTask((byte) 0);
        }
    }
    
    public void startTask(byte task) throws IOException
    {
        ping();
        sendData(RCXCmd.startTask(task));
    }
    
    public void stopTask(byte task) throws IOException
    {
        ping();
        sendData(RCXCmd.stopTask(task));
    }
    
    public void downloadFragment(boolean type, byte num, byte [] data) throws IOException
    {
        // Clear existing tasks
        sync();
        byte [] send = (type)?RCXCmd.makeBeginTask(num, data.length):RCXCmd.makeBeginSub(num, data.length);
        RCXCmd.checkStartDownloadResult(type, sendData(send));
        // Download data
        download(data);
    }
    
    private void download(byte [] data) throws IOException
    {
        int seq = 1;
        int remain = data.length;
        int n = 0;
        int start = 0;
        while (remain > 0) {
            if (remain <= DOWNLOADCHUNK) {
                seq = 0;
                n = remain;
            } else {
                n = DOWNLOADCHUNK;
            }
            byte out[] = RCXCmd.copy(data, start, n);
            // send data and check result
            RCXCmd.checkTransferDataResult(sendData(RCXCmd.makeDownload(seq++, out)));
            remain -= n;
            start += n;
        }
    }
    /**
     * Get byte codes from a BufferedReader.
     *
     * @param r the BufferedReader to read from
     * @return the byte codes read from the reader.  Uses FILE_DELIMITERS to separate
     * byte codes, and FILE_RADIX to determine the radix used int he file
     */
    public byte [] getByteCodesFromReader(BufferedReader r) throws IOException
    {
        return getByteCodesFromString(loadStringFromFile(r), FILE_DELIMITERS, FILE_RADIX);
    }
    /**
     * Create byte array from string of byte codes.  Uses second parameter 
     * to parse byte codes from String, and uses the radix provided (e.g. hex=16).
     *
     * @param aString the string to read.  Must not be null.
     * @param delimiters the delimiters used by the StringTokenizer to parse the given String.
     * @param radix the radix of the numbers given in the first parameter.
     * @return byte [] that holds the byte codes parsed from the provided String.
     */
    public byte [] getByteCodesFromString(String aString, String delimiters, int radix)
    {
        java.util.StringTokenizer st = new java.util.StringTokenizer(aString, delimiters);
        byte bytes [] = new byte[st.countTokens()];
        int count = 0;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            bytes[count] = makeByte(tok, radix);
            count++;
        }
        return bytes;
    }
    
    private byte makeByte(String code, int radix)
    {
        return Integer.valueOf(code, radix).byteValue();
    }
    
    protected String loadStringFromFile(BufferedReader r) throws IOException
    {
        String line;
        String res = new String("");
        try {
            while ((line = r.readLine()) != null) {
                res += line;
            }
        } finally {
            r.close();
        }
        return res;
    }
    
    /**
     * Test program for Java code to communicate with the {@link <a href="http://www.legomindstorms.com">Lego Mindstorms RCX</a>}.
     * See the {@link <a href="http://www.slewis.com/rcxport">RCXPort home page</a>} for more information.
     * <p><p>
     * Usage:  java rcxport.RCXPort -p &lt;comm port&gt; -n &lt;prog num&gt; [-f &lt;filename&gt;] | [-raw &lt;byte codes&gt;]
     * <p>
     * Options:
     * <p>
     *   -p: serial port (e.g. COM1).  Defaults to COM1.
     * <p>
     *   -n: RCX program number (in range 1-5 inclusive).  Defaults to 5.
     * <p>
     *   -f: file of byte codes to read.  In hex.
     * <p>
     *   -raw: raw byte codes to send to RCX (e.g. 51 3).  In hex.
     * <p><p>
     * Either the -f param or the -raw param should be provided.  If the -f parameter is specified, 
     * the file is read for byte codes (in hex).  If -raw is specified, byte codes 
     * are specified directly on the command line (also in hex):
     * <p><p>
     * java rcxport.RCXPort -p COM1 -n 1 -f filename.lis
     * <p><p>
     * <p>to read the byte codes from filename.lis, or
     * <p><p>
     * java rcxport.RCXPort -p COM1 -n 1 -raw 51 3
     * <p><p>
     * <p>to send 51 3 (play sound '3') to the RCX.
     *
     * @author Scott B. Lewis, slewis@teleport.com
     */
    public static void main(String args[]) throws Exception
    {
        String DEFAULT_PORT = "COM1";
        int DEFAULT_PROGRAM = 5;
        
        String commport = DEFAULT_PORT;
        int prognum = DEFAULT_PROGRAM;
        String fileName = null;
        String [] codes = null;
        
        byte [] data = null;
        RCXPort aPort = null;
        
        try {
            int i=0;
            
            while (i < args.length) {
                if (args[i].equals("-p")) {
                    commport = args[i+1];
                    i++;
                } else if (args[i].equals("-n")) {
                    prognum = Integer.parseInt(args[i+1]);
                    if (prognum < 1 || prognum > 5) throw new Exception("Program number out of range 1-5.");
                    i++;
                } else if (args[i].equals("-f")) {
                    fileName = args[i+1];
                    i++;
                } else if (args[i].equals("-raw")) {
                    i++;
                    codes = new String[args.length - i];
                    for(int j=0; j < codes.length; j++) {
                        codes[j] = args[i++];
                    }
                } else {
                    throw new Exception("Invalid parameter: "+args[i]);
                }
                i++;
            }
        
        } catch (Exception e) {
            System.out.println(e.getMessage());
            usage();
            return;
        }
        
        System.out.print("Opening port "+commport+"...");
        aPort = new RCXPort(commport);
        System.out.println("done.");
        
        if (fileName != null) {
            System.out.print("Reading byte codes from file: "+fileName+"...");
            data = aPort.getByteCodesFromReader(new BufferedReader(new FileReader(fileName)));
            System.out.println("Done.");
        } else if (codes != null && codes.length > 0) {
            System.out.print("Raw codes: ");
            data= new byte[codes.length];
            for(int j=0; j < codes.length; j++) {
                data[j] = Integer.valueOf(codes[j], 16).byteValue();
                System.out.print(codes[j]+" ");
            }
            System.out.println();
        } else throw new Exception("Must specify either -f or -raw");
        
        // Test by creating and downloading an RCXProgram instance
        System.out.print("Downloading program "+prognum+" to RCX...");
        aPort.downloadProgram(new RCXProgram((byte) (prognum-1), data), false);
        System.out.println("done.");
    }
    
    static void usage()
    {
        System.out.println("Usage: java rcxport.RCXPort -p <comm port> -n <prog num>; [-f <filename>] | [-raw <byte codes>]");
        System.out.println("Options:");
        System.out.println("    -p: serial port (e.g. COM1).  Defaults to COM1.");
        System.out.println("    -n: RCX program number (in range 1-5 inclusive).  Defaults to 5.");
        System.out.println("    -f: file of byte codes to read.  In hex.");
        System.out.println("    -raw: raw byte codes to send to RCX (e.g. 51 3).  In hex.");
    }
}
    