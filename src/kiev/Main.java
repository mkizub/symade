/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev;

import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import java.io.*;
import java.net.*;
import java.util.*;

import syntax kiev.Syntax;
/**
 * @author Maxim Kizub
 * @version $Revision: 703 $
 *
 */

public class Main {

	public static InputStream	input;
	public static OutputStream	output;

	public static void main(String[] args) {
		System.out.println(Compiler.version);	
		String addr = null;
		int port = 1966;
		for(int i=0; i < args.length; i++) {
			if( args[i].equals("-pipe") || args[i].equals("-server")) {
				if( i+1 >= args.length ) {
					addr = "127.0.0.1";
					port = 1966;
				} else {
					addr = args[i+1];
					if( addr.charAt(0) == '-' || addr.endsWith(".java") ) {
						addr = "127.0.0.1";
						port = 1966;
					} else {
						args[i+1] = null;
					}
				}
				int index;
				if( (index=addr.indexOf(':')) >= 0 ) {
					String sport = null;
					try {
						if( index == 0 ) {
							sport = addr.substring(1);
							port = Integer.valueOf(sport).intValue();
							addr = "127.0.0.1";
						} else {
							sport = addr.substring(index+1);
							port = Integer.valueOf(sport).intValue();
							addr = addr.substring(0,index);
						}
					} catch ( NumberFormatException e ) {
						System.err.println("Invalid port "+sport);
					}
					if( port == 0 ) port = 1966;
					if( port < 1024 ) {
						System.err.println("Port "+port+" is invalid, use port > 1024");
						return;
					}
				} else {
					port = 1966;
				}
				if( args[i].equals("-pipe") ) {
					args[i] = null;
					setupStreams(addr,port);
					StringBuffer sb = new StringBuffer();
					foreach(String arg; args; arg != null) {
						sb.append(arg).append(' ');
					}
					sb.append('\n');
					String cmd = sb.toString();
					output.write(cmd.getBytes());
					byte[] buf = new byte[2048];
					for(int r; (r=input.read()) >= 0; System.out.write(r));
					return;
				} else {
					args[i] = null;
					Compiler.printargs(args);
					Compiler.run(args);
					Compiler.runServer(InetAddress.getByName(addr),port);
					return;
				}
			}
			else if( args[i].equals("-i") || args[i].equals("-incremental")) {
				Compiler.printargs(args);
				Compiler.run(args);
				Compiler.runIncremental(args);
				return;
			}
		}
		Compiler.run(args);
	}
	
	public static void setupStreams(String addr, int port) {
		Socket s = null;
		try {
			s = new Socket(addr,port);
		} catch( ConnectException e ) {
			System.err.println("Kiev compiler server not found at "+addr+":"+port+" : "+e);
			System.exit(0);
		} catch( Exception e ) {
			e.printStackTrace();
			System.exit(0);
		}
		System.out.println("Conected to "+s);
		input = s.getInputStream();
		output = s.getOutputStream();
	}
}

