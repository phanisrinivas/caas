/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas.main;

import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.InteractiveConsole;
import org.python.util.PythonInterpreter;

public class JythonShell extends CommandBase {
	public JythonShell() { super("", "start a jython shell"); }

	private Cli cli=new Cli();
	
	@Override public void run(String[] args) {
	    console();
		//System.exit(code);
	}

	@Override public String getHelp() { return "\nOPTIONS\n"+cli.getSyntax("\t"); } 
	

    public void console() {
        InteractiveConsole interp;
        if (System.getProperty("python.home") == null) {
            System.setProperty("python.home", "");
        }
        InteractiveConsole.initialize(System.getProperties(), null, new String[0]);
        interp = new InteractiveConsole();
        //interp.push("print \"Starting\"");
        interp.exec("print \"hoi\"");
        interp.exec("print 1+3");
        interp.interact();
    }
    

    public void console2() {
        if (System.getProperty("python.home") == null) {
            System.setProperty("python.home", "");
        }
        PySystemState.initialize();
        PythonInterpreter pyi = new PythonInterpreter();   
        pyi.exec("import sys");    
        // you can pass the python.path to java to avoid hardcoding this
        // java -Dpython.path=/path/to/jythonconsole-0.0.6 EmbedExample
        pyi.exec("sys.path.append(r'./lib/jythonconsole-0.0.7/')");
        pyi.exec("from console import main");
        PyObject main = pyi.get("main");

        // stuff some objects into the namespace
        // this will probably be objects from your application
        pyi.set("embed_example", this);
        pyi.set("foo", "bar");
        pyi.set("n", 17);
        main.__call__(pyi.getLocals());
    }
}
