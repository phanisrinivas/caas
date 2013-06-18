package org.kisst.cordys.caas.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This calass contains utility methods to work with Exceptions.
 * 
 * @author pgussow
 */
public class ExceptionUtil
{
    /**
     * This method gets the stacktrace from the given throwable as a string.
     * 
     * @param t The throwable to get the stacktrace for.
     * @return The string version of the full stacktrace
     */
    public static String getStacktrace(Throwable t)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();

        return sw.getBuffer().toString();
    }
}
