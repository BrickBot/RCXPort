/**
 * @(#) RCXTask.java 0.1 98/03/12
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
/**
 * Representation of RCX Task.
 *
 * @author Scott B. Lewis, slewis@teleport.com
 */
public class RCXTask implements Serializable
{
    /**
     * @serial myBytes the actual byte codes for this task.
     */
    byte [] myBytes;
    
    public RCXTask(byte [] bytes)
    {
        myBytes = bytes;
    }
    
    protected byte [] getBytes()
    {
        return myBytes;
    }
}