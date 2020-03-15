/**
 * @(#) RCXCmd.java 0.1 98/03/12
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

/**
 * Build and manipulate RCX commands.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
public class RCXCmd {

    // Op codes
    public static final byte OutputMode = (byte)        0x21;
    public static final byte OutputPower = (byte)       0x13;
    public static final byte OutputDir = (byte)         0xe1;
// inputs
    public static final byte InputMode = (byte)         0x42;
    public static final byte InputType = (byte)         0x32;
// sound
    public static final byte PlaySound = (byte)         0x51;
    public static final byte PlayTone = (byte)          0x23;
// control flow
    public static final byte Test = (byte)              0x95;
    public static final byte Jump = (byte)              0x72;
    public static final byte SJump = (byte)             0x27;
    public static final byte SetLoop = (byte)           0x82;
    public static final byte CheckLoop = (byte)         0x92;
// misc
    public static final byte Delay = (byte)             0x43;
    public static final byte Display = (byte)           0x33;

    public static final byte SendMessage = (byte)       0xb2;
    public static final byte StartTask = (byte)         0x71;
    public static final byte StopTask = (byte)          0x81;
    public static final byte StopAll = (byte)           0x50;
    public static final byte ClearTimer = (byte)        0xa1;
    public static final byte ClearMsg = (byte)          0x90;
    public static final byte SendMsg = (byte)           0xb2;
    public static final byte ClearSensor = (byte)       0xd1;
    public static final byte GoSub = (byte)             0x17;
    public static final byte SetDatalog = (byte)        0x52;
    public static final byte Datalog = (byte)           0x62;
    public static final byte UploadDatalog = (byte)     0xa4;
// special
    public static final byte Read = (byte)              0x12;
    public static final byte Unlock = (byte)            0x1d;
    public static final byte BeginTask = (byte)         0x25;
    public static final byte BeginSub = (byte)          0x35;
    public static final byte Download = (byte)          0x45;
    public static final byte Message = (byte)           0xf7;
    public static final byte DeleteTasks = (byte)       0x40;
    public static final byte DeleteSubs = (byte)        0x70;
    public static final byte BootMode = (byte)          0x65;
    public static final byte BeginFirmware = (byte)     0x75;
    public static final byte EndFirmware = (byte)       0xad;
    public static final byte Ping = (byte)              0x10;
    public static final byte SelectProgram = (byte)     0x91;
    public static final byte BatteryLevel = (byte)      0x30;
    public static final byte SetWatch = (byte)          0x22;
    public static final byte IRMode = (byte)            0x31;
    public static final byte AutoOff = (byte)           0xb1;

    public static byte hibyte(int i)
    {
       return (byte) ((i >>> 8) & (byte) 0xff);
    }

    public static byte lobyte(int i)
    {
       return (byte) (i & (byte) 0xff);
    }

    public static byte [] copy(byte [] input, int start, int length)
    {
        byte [] newBytes = newBytes(length);
        System.arraycopy(input, start, newBytes, 0, length);
        return newBytes;
    }

    private static byte [] newBytes(int length)
    {
        return new byte[length];
    }

    public static byte [] set(byte aByte)
    {
        byte [] newBytes = newBytes(1);
        newBytes[0] = aByte;
        return newBytes;
    }

    public static byte [] set(byte one, byte two)
    {
        byte [] newBytes = newBytes(2);
        newBytes[0] = one;
        newBytes[1] = two;
        return newBytes;
    }

    public static byte [] set(byte one, byte two, byte three)
    {
        byte [] newBytes = newBytes(3);
        newBytes[0] = one;
        newBytes[1] = two;
        newBytes[2] = three;
        return newBytes;
    }

    public static byte [] set(byte one, byte two, byte three, byte four)
    {
        byte [] newBytes = newBytes(4);
        newBytes[0] = one;
        newBytes[1] = two;
        newBytes[2] = three;
        newBytes[3] = four;
        return newBytes;
    }

    public static byte [] set(byte one, byte two, byte three, byte four, byte five)
    {
        byte [] newBytes = newBytes(5);
        newBytes[0] = one;
        newBytes[1] = two;
        newBytes[2] = three;
        newBytes[3] = four;
        newBytes[4] = five;
        return newBytes;
    }

    public static byte [] set(byte one, byte two, byte three, byte four, byte five, byte six)
    {
        byte [] newBytes = newBytes(6);
        newBytes[0] = one;
        newBytes[1] = two;
        newBytes[2] = three;
        newBytes[3] = four;
        newBytes[4] = five;
        newBytes[5] = six;
        return newBytes;
    }

    public static byte [] startTask(byte task)
    {
        return set(StartTask, task);
    }

    public static byte [] stopTask(byte task)
    {
        return set(StopTask, task);
    }

    public static byte [] makeUnlock()
    {
	    return set(Unlock, (byte) 1, (byte) 3, (byte) 5, (byte) 7, (byte) 0xb);
    }

    public static byte [] makePlayTone(int freq, byte duration)
    {
        return set(PlayTone, lobyte(freq), hibyte(freq), duration);
    }

    public static byte [] makePlaySound(int sound)
    {
        return set(PlaySound, (byte)(sound & 7));
    }

    public static byte [] makeBeginTask(byte taskNum, int length)
    {
        return set(BeginTask, (byte) 0, taskNum, (byte) 0, lobyte(length), hibyte(length));
    }

    public static byte [] makeBeginSub(byte taskNum, int length)
    {
        return set(BeginSub, (byte) 0, taskNum, (byte) 0, lobyte(length), hibyte(length));
    }

    public static byte [] makeOutputPower(byte outputs, int val)
    {
        return set(OutputPower, outputs, getValueType(val), (byte) getValueData(val));
    }

    public static byte [] makeValue16(byte op, int val)
    {
        int data = getValueData(val);
        return set(op, getValueType(val), lobyte(data), hibyte(data));
    }

    public static byte [] makeValue8(byte op, int val)
    {
        return set(op, getValueType(val), (byte) getValueData(val));
    }

    public static byte [] makeDownload(int seq, byte [] data)
    {
        byte [] nb = newBytes(data.length+6);
        nb[0] = Download;
        nb[1] = lobyte(seq);
        nb[2] = hibyte(seq);
        nb[3] = lobyte(data.length);
        nb[4] = hibyte(data.length);

        byte checksum = 0;
        for(int i=0; i < data.length; i++) {
            byte d = data[i];
            checksum += d;
            nb[5+i] = d;
        }
        nb[data.length+5] = checksum;
        return nb;
    }

    public static void checkStartDownloadResult(boolean task, RCXResult res)
        throws IOException
    {
        byte [] results = res.getResult();
        if (results.length != 2) throw new IOException("Bad result");
        if (task) {
            if (results[0]==((byte) 0xd2)||results[0]==((byte) 0xda)) {
                if (results[1]==((byte) 0)) return;
                else {
                    if (results[1]==((byte) 1)) throw new IOException("Insufficient Memory");
                    else if (results[1]==((byte) 2)) throw new IOException("Task index invalid");
                }
            }
            throw new IOException("Error in response");
        } else {
            if (results[0]==((byte) 0xc2)||results[0]==((byte) 0xca)) {
                if (results[1]==0) return;
                else {
                    if (results[1]==((byte) 1)) throw new IOException("Insufficient Memory");
                    else if (results[1]==((byte) 2)) throw new IOException("Subroutine index invalid");
                }
                throw new IOException("Error in response");
            }
        }
    }

    public static void checkTransferDataResult(RCXResult res) throws IOException
    {
        byte [] results = res.getResult();
        if (results.length != 2||(results[0]!=((byte)0xb2)&&results[0]!=((byte)0xba))) throw new IOException("Bad result for transfer data");
        if (results[1]==((byte) 3)) throw new IOException("block checksum failure");
        if (results[1]==((byte) 4)) throw new IOException("firmware checksum error");
        if (results[1]==((byte) 6)) throw new IOException("invalid or missing download start");
    }

    public static String makeString(byte aByte)
    {
        StringBuffer tmp = new StringBuffer(Integer.toHexString(aByte & 0xff));
        if (tmp.length() == 1)
            tmp.insert (0, '0');
        return tmp.toString();
    }

    private static byte getValueType(int value)
    {
        return (byte) ((value >> 16) & 0xff);
    }

    private static int getValueData(int value)
    {
        return ((value) & 0xffff);
    }

}

