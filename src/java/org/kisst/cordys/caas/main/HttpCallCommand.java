package org.kisst.cordys.caas.main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.util.XmlNode;
import org.python.core.util.FileUtil;

public class HttpCallCommand extends SysCommand
{
    private final Cli.StringOption input = cli.stringOption("i", "input", "inputfile (- for stdin)", "-");
    private final Cli.StringOption output = cli.stringOption("o", "output", "outputfile (- for stdout)", "-");
    private final Cli.Flag pretty = cli.flag("p", "pretty", "pretty print xml output");
    public HttpCallCommand() { 
        super("[-i inputfile] [-o outputfile]", "do a http call to a certain system");
    }
    @Override public void run(String[] args) {
        args = checkArgs(args);
        String ifile=input.get();
        String ofile=output.get();
        InputStream inp = System.in;
        OutputStream out = System.out;
        try {
            if (ifile!=null && ! "-".equals(ifile))
                inp=new FileInputStream(ifile);
            if (ofile!=null && ! "-".equals(ofile))
                out=new FileOutputStream(ofile);
            CordysSystem sys = getSystem();
            String request=new String(FileUtil.readBytes(inp));
            String result=sys.getSoapCaller().httpCall(request);
            if (pretty.isSet()) {
                try {
                    XmlNode xml=new XmlNode(result);
                    result=xml.getPretty();
                }
                catch (RuntimeException e) { /* ignore, show result as text, since Xml parsing did not succeed */ }
            }
            out.write(result.getBytes());
        }
        catch (IOException e) { throw new RuntimeException(e); }
    }
    
}
