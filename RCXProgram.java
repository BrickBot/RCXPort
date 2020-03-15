/**
 * @(#) RCXProgram.java 0.1 98/03/12
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
import java.util.Vector;
import java.util.Enumeration;

import java.io.IOException;

/**
 * Representation of an RCX program.  Includes both a set of tasks and
 * a set of subroutines.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
public class RCXProgram implements Serializable
{
    public static final int MAX_TASKS = 10;
    public static final int MAX_PROGS = 5;

    // Set of tasks
    /**
     * @serial myTasks the byte code tasks for this program.
     */
    Vector myTasks;
    /**
     * @serial mySubs the byte code subroutines for this program.
     */
    Vector mySubs;

    /**
     * @serial myProgNum the program number (0..4)
     */
    byte myProgNum;

    public RCXProgram(byte prog)
    {
        if (prog < (byte) 0 || prog >= MAX_PROGS) prog = MAX_PROGS-1;
        myProgNum = prog;
        // Defer creation of Vectors until needed.
    }

    public RCXProgram(byte prog, RCXTask aTask)
    {
        this(prog);
        addTask(aTask);
    }

    public RCXProgram(byte prog, byte [] aTask)
    {
        this(prog);
        addTask(new RCXTask(aTask));
    }

    public RCXProgram addTask(RCXTask task)
    {
        // If invalid task number just ignore
        if (myTasks == null) myTasks = new Vector(MAX_TASKS);
        // If we already have our allotment of tasks, ignore.
        if (myTasks.size()==MAX_TASKS) return this;
        myTasks.addElement(task);
        return this;
    }

    public RCXProgram addSub(RCXSub aSub)
    {
        if (mySubs == null) mySubs = new Vector();
        mySubs.addElement(aSub);
        return this;
    }

    protected void downloadTasks(RCXPort aPort) throws IOException
    {
        if (myTasks != null) {
            int i= 0;
            for(Enumeration e=myTasks.elements(); e.hasMoreElements(); ) {
                RCXTask aTask = (RCXTask) e.nextElement();
                writeBytes(aPort, true, (byte) i++, aTask.getBytes());
            }
        }
    }

    protected void downloadSubroutines(RCXPort aPort) throws IOException
    {
        if (mySubs != null) {
            int i = 0;
            for(Enumeration e=mySubs.elements(); e.hasMoreElements(); ) {
                RCXSub aSub = (RCXSub) e.nextElement();
                writeBytes(aPort, false, (byte) i++, aSub.getBytes());
            }
        }
    }

    protected int getProgramNum()
    {
        return myProgNum;
    }

    private void writeBytes(RCXPort aPort, boolean task, byte index, byte [] bytes)
        throws IOException
    {
        if (bytes != null) aPort.downloadFragment(task, index, bytes);
    }
}