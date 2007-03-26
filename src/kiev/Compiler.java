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
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.ATextSyntax;

import java.io.*;
import java.net.*;
import java.util.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@singleton
public class CompilerThread extends WorkerThread {
	private CompilerThread() { super("compiler"); }
}

@singleton
public class EditorThread extends WorkerThread {
	private EditorThread() { super("editor"); }
}

public abstract class WorkerThread extends Thread {
	// Error section
	public long		programm_start;
	public long		programm_end;
	public long		programm_mem;
	public int		errCount;
	public int		warnCount;
	public boolean	reportTotals;
	public String	curFile = "";

	private boolean			busy;
	private boolean			run_fe;
	private boolean			run_be;
	private String[]		args;
	private ANode			root;

	WorkerThread(String name) {
		super(name);
		try {
			setDaemon(true);
			setPriority((NORM_PRIORITY+MIN_PRIORITY)/2);
		} catch (Exception e) { e.printStackTrace(); }
	}
	public void run() {
		for(;;) {
			synchronized(this) {
				if (!busy) {
					notifyAll();
					wait();
				}
			}
			if (busy) {
				//System.out.println("task run: "+run_fe+" "+run_be);
				this.errCount = 0;
				this.warnCount = 0;
				if (run_fe) {
					if (args != null)
						runFrontEnd(args);
					if (root != null)
						runFrontEnd(root);
				}
				if (run_be)
					runBackEnd();
			}
			synchronized(this) {
				run_fe = false;
				run_be = false;
				args = null;
				root = null;
				busy = false;
			}
		}
	}
	public boolean isBusy() {
		return busy;
	}
	public boolean setTask(boolean run_fe, boolean run_be, String[] args, ANode root) {
		if (busy) return false;
		synchronized(this) {
			//System.out.println("task set: "+run_fe+" "+run_be);
			this.run_fe = run_fe;
			this.run_be = run_be;
			this.args = args;
			this.root = root;
			this.busy = true;
			notifyAll();
		}
		return true;
	}
	private void runFrontEnd(String[] args) {
		this.programm_start = this.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		try {
//			if( Kiev.verbose ) System.out.println(Compiler.version);
//			Runtime.getRuntime().traceMethodCalls(Compiler.methodTrace);

			if( !Kiev.initialized )
				Env.InitializeEnv(Kiev.compiler_classpath);

			if( Compiler.make_project || Compiler.safe )
				args = Compiler.addRequaredToMake(args);

			if( args.length == 0 ) {
				trace(Kiev.verbose,"All files are up to date\007\007");
				return;
			}

			if( !Kiev.initialized ) {
				Class force_init = Class.forName(StdTypes.class.getName());
				Kiev.initialized = (force_init != null);
			}

			Kiev.resetFrontEndPass();
			Env.root.files.delAll();
			
			Kiev.k = new Parser(new StringReader(""));
			for(int i=0; i < args.length; i++) {
				try {
					this.curFile = args[i].intern();
					if (this.curFile.toLowerCase().endsWith(".xml")) {
						FileUnit fu = Env.loadFromXmlFile(new File(this.curFile));
						Env.root.files += fu;
						foreach (ATextSyntax ts; fu.members)
							Env.createProjectInfo(ts,this.curFile);
					} else {
						java.io.InputStreamReader file_reader = null;
						char[] file_chars = new char[8196];
						int file_sz = 0;
						try {
							file_reader = new InputStreamReader(new FileInputStream(args[i]), "UTF-8");
							for (;;) {
								int r = file_reader.read(file_chars, file_sz, file_chars.length-file_sz);
								if (r < 0)
									break;
								file_sz += r;
								if (file_sz >= file_chars.length) {
									char[] tmp = new char[file_chars.length + 8196];
									System.arraycopy(file_chars, 0, tmp, 0, file_chars.length);
									file_chars = tmp;
								}
							}
						} finally {
							if (file_reader != null) file_reader.close();
						}
						java.io.CharArrayReader bis = new java.io.CharArrayReader(file_chars, 0, file_sz);
						Kiev.k.interface_only = true;
						Compiler.runGC(this);
						diff_time = curr_time = System.currentTimeMillis();
						Kiev.k.ReInit(bis);
						FileUnit fu = Kiev.k.FileUnit(args[i]);
						Env.root.files += fu;
						diff_time = System.currentTimeMillis() - curr_time;
						bis.close();
					}
					Compiler.runGC(this);
					this.curFile = "";
					if( Kiev.verbose )
						Kiev.reportInfo("Scanned file   "+args[i],diff_time);
					System.out.flush();
				} catch (Exception e) {
					Kiev.reportParserError(0,e);
				}
			}
			Compiler.runGC(this);

			this.root = Env.root;

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		Env.dumpProjectFile();
		if (this.errCount > 0) {
			run_be = false;
			this.root = null;
		}
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Frontend");
	}

	private void runFrontEnd(ANode root) {
		if (root == null)
			return;

		this.programm_start = this.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		try {
//			if( Kiev.verbose ) System.out.println(Compiler.version);
//			Runtime.getRuntime().traceMethodCalls(Compiler.methodTrace);


			Kiev.resetFrontEndPass();
			
			////////////////////////////////////////////////////
			//	                  Frontend                    //
			////////////////////////////////////////////////////

			do {
				diff_time = curr_time = System.currentTimeMillis();
				String msg = Kiev.runCurrentFrontEndProcessor(root);
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && msg != null) Kiev.reportInfo(msg,diff_time);
				if( this.errCount > 0 ) goto stop;
			} while (Kiev.nextFrontEndPass());

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		Kiev.lockNodeTree(root);
		Env.dumpProjectFile();
		if (this.errCount > 0)
			run_be = false;
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Frontend");
	}

	private void runBackEnd() {
		long curr_time = 0L, diff_time = 0L;
		Transaction tr_me = Transaction.open();
		try {
			////////////////////////////////////////////////////
			//	                  Midend                      //
			////////////////////////////////////////////////////

			if (!Kiev.run_batch) {
				Env.root.walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if (n instanceof ASTNode) {
							ASTNode astn = (ASTNode)n;
							if (!astn.locked)
								System.out.println("Unlocked node "+n);
							astn.compileflags &= 0xFFFF0001;
						}
						return true;
					}
				});
			}

			Kiev.resetMidEndPass();
			do {
				diff_time = curr_time = System.currentTimeMillis();
				String msg = Kiev.runCurrentMidEndProcessor();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && msg != null) Kiev.reportInfo(msg,diff_time);
				if( this.errCount > 0 ) throw new Kiev.CompilationAbortError();
			} while (Kiev.nextMidEndPass());
			
			tr_me.close();

			for(int i=0; i < Env.root.files.length; i++) {
				final int errCount = this.errCount;
				final FileUnit fu = Env.root.files[i];
				Transaction tr = Transaction.open();
				try {
					Kiev.resetBackEndPass();
					Kiev.openBackEndFileUnit(fu);
					do {
						diff_time = curr_time = System.currentTimeMillis();
						String msg = Kiev.runCurrentBackEndProcessor(fu);
						diff_time = System.currentTimeMillis() - curr_time;
						if( Kiev.verbose && msg != null) Kiev.reportInfo(msg,diff_time);
					} while (Kiev.nextBackEndPass() && this.errCount == errCount);
					fu.cleanup();
					Kiev.closeBackEndFileUnit();
				} finally { tr.rollback(false); }
				Compiler.runGC(this);
			}
		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				return;
			Kiev.reportError(e);
		} finally {
			tr_me.rollback(false);
		}
		Env.dumpProjectFile();
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Backend");
	}

	public void reportTotals(String src) {
		if( errCount > 0 )
			System.out.println(errCount+" errors");
		if( warnCount > 0 )
			System.out.println(warnCount+" warnings");
		programm_end = System.currentTimeMillis();
		System.out.println(src+": total "+(programm_end-programm_start)+"ms, max memory used = "+programm_mem+" Kb\007\007");
		if (Kiev.testError != null) {
			System.out.println("FAILED: there was no expected error "+Kiev.testError+" at "+Kiev.testErrorLine+":"+Kiev.testErrorOffs);
			System.exit(1);
		}
		this.reportTotals = true;
	}

}

public class Compiler {
	public static ServerSocket		server;
	public static Socket			socket;
	public static InputStream		system_in = System.in;
	public static PrintStream		system_out = System.out;
	public static PrintStream		system_err = System.err;

	public static final String version = "SymADE (v 0.40), (C) UAB MAKSINETA, 1997-2007, http://symade.com";

	public static boolean debug					= false;
	public static boolean debugStatGen			= false;
	public static boolean debugInstrGen		= false;
	public static boolean debugBytecodeRead	= false;
	public static boolean debugResolve			= false;
	public static boolean debugOperators		= false;
	public static boolean debugMembers			= false;
	public static boolean debugCreation		= false;
	public static boolean debugRules			= false;
	public static boolean debugMultiMethod		= false;
	public static boolean debugNodeTypes		= false;

	public static boolean methodTrace			= false;

	public static boolean verbose				= false;
	public static boolean verify				= true;
	public static boolean safe					= true;
	public static boolean debugOutputA			= false;
	public static boolean debugOutputT			= false;
	public static boolean debugOutputC			= false;
	public static boolean debugOutputL			= false;
	public static boolean debugOutputV			= false;
	public static boolean debugOutputR			= false;

	public static boolean errorPrompt			= false;

	public static boolean run_gui				= false;
	public static boolean make_project			= false;
	public static boolean makeall_project		= false;
	public static boolean run_batch				= true;
	public static boolean interactive			= false;
	public static boolean interface_only		= false;
	public static boolean initialized			= false;

	public static String project_file			= "project";
	public static String output_dir				= "classes";
	public static String compiler_classpath	= null;
	public static boolean javaMode				= false;
	public static boolean javacerrors			= false;
	public static boolean nowarn				= false;

	public static KievBackend useBackend = KievBackend.Java15;

	public static CError testError				= null;
	public static int    testErrorLine			= 0;
	public static int    testErrorOffs			= 0;

	public static long gc_mem				= (long)(1024*1024);

	private static boolean[] command_line_disabled_extensions	= new boolean[KievExt.values().length];

	{
		Compiler.setExtension(false, "vnode");
		Compiler.setExtension(false, "dflow");
	}
	
	public static boolean[] getCmdLineExtSet() {
		return (boolean[])command_line_disabled_extensions.clone();
	}
	
	static void setExtension(boolean enabled, String s) {
		KievExt ext;
		try {
			ext = KievExt.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning("Unknown pragma '"+s+"'");
			return;
		}
		int i = (int)ext;
		if (i == 0) {
			for (int i=1; i < command_line_disabled_extensions.length; i++)
				command_line_disabled_extensions[i] = !enabled;
		} else {
			command_line_disabled_extensions[i] = !enabled;
		}
	}
	
	public static void runServer(InetAddress addr, int port) {
		server = new ServerSocket(port,2,addr);
		System.out.println("Server mode: "+server);
		for(;;) {
			socket = server.accept();
			System.out.println("Incoming connection "+socket);
			System.setIn(new DataInputStream(socket.getInputStream()));
			System.setOut(new PrintStream(socket.getOutputStream()));
			System.setErr(new PrintStream(socket.getOutputStream()));
			String[] args = getCommandLine();
			run(args);
			socket.close();
			System.setIn(system_in);
			System.setOut(system_out);
			System.setErr(system_err);
			System.out.println("Done.");
		}
	}

	public static void runIncremental(String[] args) {
		System.out.println("Incremental mode");
		System.setIn(new DataInputStream(System.in));
		for(;;) {
			args = getCommandLine();
			printargs(args);
			run(args);
		}
	}

	public static void printargs(String[] args) {
		StringBuffer sb = new StringBuffer();
		foreach(String arg; args; arg != null) {
			sb.append(arg).append(' ');
		}
		System.out.println(sb.toString());
	}

	public static String[] getCommandLine() {
	start:;
		String[] args;
		System.out.print("kiev> ");
		System.out.flush();
		System.gc();
		String cmd = ((DataInputStream)System.in).readLine();
		System.out.println("Command "+cmd);
		if( cmd == null ) {
			args = new String[]{"-q"};
			return args;
		} else if( cmd.length() == 0 ) {
			args = new String[0];
			return args;
		}
		StringTokenizer st = new StringTokenizer(cmd);
		args = new String[st.countTokens()];
		int i=0;
		foreach(Object s; st) args[i++] = (String)s;
		if( args[0].startsWith("-q") ) {
			System.out.println("Bye.");
			if( server != null ) server.close();
			if( socket != null ) socket.close();
			System.exit(0);
		}
		else if( args[0].equals("?") ) {
			System.out.println(
				"[options] file.java ...\t - set options and compile file(s)\n"+
				"-q\t\t - quit\n"+
				"?\t\t - this help\n"+
				"{Enter}\t\t - make project\n"
			);
			goto start;
		}
		return args;
	}

	private static String[] parseArgs(String[] args) throws Exception {
		int a = 0;
		if( args==null ) args = new String[0];
		int alen = args.length;
		for(a=0; a < alen ;a++) {
			if( args[a].equals("-debug"))
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
		}
		try {
			for(a=0; a < alen ;a++) {
				boolean onoff = true;
				if( args[a] == null ) continue;
				if( args[a].equals("--") ) {
					args[a] == null;
					break;
				}
				if( args[a].equals("-pipe") || args[a].equals("-server") ) {
					args[a] = null;
					if( args[a+1].charAt(0) == '-' ) continue;
					if( args[a+1].endsWith(".java") ) continue;
					args[++a] = null;
					continue;
				}
				else if( args[a].equals("-?") || args[a].equals("-h") || args[a].equals("-help") ) {
					printHelp();
					return new String[0];
				}

				if( args[a].startsWith("-no-") ) {
					args[a] = args[a].substring(3);
					onoff = false;
				}

				if( args[a].equals("-java")) {
					Compiler.javaMode = onoff;
					if (onoff) {
						Compiler.verify = true;
						Compiler.safe = true;
						Compiler.project_file = null;
						Compiler.output_dir = null;
					}
					args[a] = null;
				}
				else if( args[a].equals("-disable") ) {
					args[a] = null;
					Compiler.setExtension(!onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-enable") ) {
					args[a] = null;
					Compiler.setExtension(onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-prompt")) {
					Compiler.errorPrompt = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-g") ) {
					Compiler.debugOutputA = onoff;
					Compiler.debugOutputT = onoff;
					Compiler.debugOutputC = onoff;
					Compiler.debugOutputL = onoff;
					Compiler.debugOutputV = onoff;
					Compiler.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].startsWith("-g:")) {
					if( args[a].indexOf('a') > 0 )
						Compiler.debugOutputA = onoff;
					if( args[a].indexOf('t') > 0 )
						Compiler.debugOutputT = onoff;
					if( args[a].indexOf('c') > 0 )
						Compiler.debugOutputC = onoff;
					if( args[a].indexOf('l') > 0 )
						Compiler.debugOutputL = onoff;
					if( args[a].indexOf('v') > 0 )
						Compiler.debugOutputV = onoff;
					if( args[a].indexOf('r') > 0 )
						Compiler.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-debug")) {
					args[a] = null;
					String dbg = args[++a];
					args[a] = null;
					System.out.println("Debugging: "+onoff);
					if( dbg.indexOf("stat",0) >= 0 ) {
						System.out.println("\tstatement generation");
						Compiler.debugStatGen	= onoff;
					}
					if( dbg.indexOf("asm",0) >= 0 ) {
						System.out.println("\tassembler generation");
						Compiler.debugInstrGen	= onoff;
					}
					if( dbg.indexOf("instr",0) >= 0 ) {
						System.out.println("\tinstractions generation");
						Compiler.debugInstrGen	= onoff;
					}
					if( dbg.indexOf("bcread",0) >= 0 ) {
						System.out.println("\tbytecode .class reading");
						Compiler.debugBytecodeRead = onoff;
						kiev.bytecode.Clazz.traceRead = onoff;
					}
					if( dbg.indexOf("bcpatch",0) >= 0 ) {
						System.out.println("\tbytecode .class patching and rules");
						kiev.bytecode.Clazz.traceRules = onoff;
					}
					if( dbg.indexOf("resolv",0) >= 0 ) {
						System.out.println("\tidentifier resolving");
						Compiler.debugResolve	= onoff;
					}
					if( dbg.indexOf("operat",0) >= 0 ) {
						System.out.println("\toperator resolving");
						Compiler.debugOperators	= onoff;
					}
					if( dbg.indexOf("member",0) >= 0 ) {
						System.out.println("\tmembers attaching (AST generation)");
						Compiler.debugMembers	= onoff;
					}
					if( dbg.indexOf("create",0) >= 0 ) {
						System.out.println("\tmembers creation");
						Compiler.debugCreation	= onoff;
					}
					if( dbg.indexOf("methodtrace",0) >= 0 ) {
						System.out.println("\tMethod tracing");
						Compiler.methodTrace	= onoff;
					}
					if( dbg.indexOf("multimethod",0) >= 0 ) {
						System.out.println("\tMultiMethod generation");
						Compiler.debugMultiMethod	= onoff;
					}
					if( dbg.indexOf("rule",0) >= 0 ) {
						System.out.println("\trules");
						Compiler.debugRules		= onoff;
					}
					if( dbg.indexOf("types",0) >= 0 ) {
						System.out.println("\tvar/field types");
						Compiler.debugNodeTypes		= onoff;
					}
					Compiler.debug = onoff;
					continue;
				}
				else if( args[a].equals("-trace")) {
					args[a] = null;
					String dbg = args[++a];
					args[a] = null;
					System.out.println("Tracing: "+onoff);
					if( dbg.indexOf("rules",0) >= 0 ) {
						System.out.println("\tprolog rules");
						kiev.stdlib.PEnv.debug = onoff;
					}
					Compiler.debug = onoff;
					continue;
				}
				else if( args[a].equals("-verbose") || args[a].equals("-v") ) {
					Compiler.verbose = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-verify") ) {
					Compiler.verify = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-safe") ) {
					Compiler.safe = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-d")) {
					args[a] = null;
					if( onoff ) {
						Compiler.output_dir = args[++a];
						args[a] = null;
					} else {
						Compiler.output_dir = null;
					}
					continue;
				}
				else if( args[a].equals("-p") || args[a].equals("-project") ) {
					args[a] = null;
					if( onoff ) {
						Compiler.project_file = args[++a];
						args[a] = null;
					} else
						Compiler.project_file = null;
					continue;
				}
				else if( args[a].equals("-make") ) {
					args[a] = null;
					Compiler.make_project = onoff;
					continue;
				}
				else if( args[a].equals("-makeall") ) {
					args[a] = null;
					if( onoff )
						Compiler.make_project = true;
					Compiler.makeall_project = onoff;
					continue;
				}
				else if( args[a].equals("-classpath")) {
					args[a] = null;
					if( onoff ) {
						Compiler.compiler_classpath = args[++a];
						args[a] = null;
					} else
						Compiler.compiler_classpath = null;
					Compiler.initialized = false;
					continue;
				}
				else if( args[a].equals("-gc")) {
					args[a] = null;
					if( onoff ) {
						Compiler.gc_mem = (long)(Double.valueOf(args[++a]).doubleValue()*1024.D);
						args[a] = null;
					} else
						Compiler.gc_mem = (long)(1024*1024.D);
					continue;
				}
				else if( args[a].equals("-vsrc")) {
					Compiler.useBackend = KievBackend.VSrc;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-gui")) {
					args[a] = null;
					Compiler.run_gui   = onoff;
					if (onoff)
						Compiler.run_batch = false;
					continue;
				}
				else if( args[a].equals("-batch") ) {
					args[a] = null;
					Compiler.run_batch = onoff;
					if (onoff)
						Compiler.run_gui = false;
					continue;
				}
				else if( args[a].equals("-i") || args[a].equals("-incremental")) {
					Compiler.interactive = onoff;
					if( onoff )
						Compiler.make_project = true;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-javacerrors")) {
					Compiler.javacerrors = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-warn")) {
					Compiler.nowarn = !onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-test")) {
					args[a++] = null;
					String[] errs = args[a].split(":");
					args[a] = null;
					Compiler.testError = CError.valueOf(errs[0]);
					Compiler.testErrorLine = Integer.parseInt(errs[1]);
					Compiler.testErrorOffs = Integer.parseInt(errs[2]);
					continue;
				}
				else if( args[a].startsWith("-D") ) {
					Properties ps = System.getProperties();
					String prop, value;
					args[a] = args[a].substring(2);
					int indx = args[a].indexOf("=");
					if( indx < 0 ) {
						prop = args[a];
						value = "";
					} else {
						prop = args[a].substring(0,indx);
						value = args[a].substring(indx+1);
					}
					if( onoff ) {
						Env.setProperty(prop,value);
					} else {
						Env.removeProperty(prop);
					}
					args[a] = null;
					continue;
				}
				else if( args[a].startsWith("@") ) {
					String fname = args[a].substring(1);
					args[a] = null;
					RandomAccessFile f = new RandomAccessFile(fname, "r");
					byte[] buf = new byte[(int)f.length()];
					f.readFully(buf);
					f.close();
					StringTokenizer st = new StringTokenizer(new String(buf));
					String[] arr = new String[st.countTokens()];
					for (int i=0; i < arr.length; i++)
						arr[i] = st.nextToken();
					arr = parseArgs(arr);
					for (int i=0; i < arr.length; i++)
						args = (String[])Arrays.insert(args,arr[i],a+i+1);
					alen = args.length;
					continue;
				}
				else if( hasWildcards(args[a]) ) {
					try {
						args = addExpansion(args,a);
					} catch( IOException e ) {
						Kiev.reportError("Error in arguments: "+e);
					}
				}
			}

			String[] args1 = new String[0];
			for(int i=0; i < args.length; i++) {
				if( args[i] == null ) continue;
				args1 = (String[])Arrays.appendUniq(args1,args[i]);
			}
			args = args1;
		} catch( ArrayIndexOutOfBoundsException e) {
			args = new String[0];
		}
		return args;
	}
	
	public static void run(String[] args) {
		Compiler.interface_only = false;
		Compiler.makeall_project = false;

		try {
			args = parseArgs(args);
		} catch( Exception e) {
			Kiev.reportError(e);
			return;
		}
		
		CompilerThread thr = CompilerThread;
		runFrontEnd(thr, args, null, true);

		if (Kiev.run_gui) {
			kiev.gui.Window wnd = new kiev.gui.Window();
			if (Env.root.files.length > 0)
				wnd.openEditor(Env.root.files[0]);
			for(;;) Thread.sleep(10*1000);
		} else {
			if (thr.errCount == 0)
				runBackEnd(thr, Env.root, Compiler.useBackend, true);
			if !(Kiev.interactive) {
				System.exit(thr.errCount > 0 ? 1 : 0);
			}
		}
	}
	
	public static void runFrontEnd(WorkerThread thr, String[] args, ANode root, boolean sync) {
		if (!thr.isAlive())
			thr.start();
		
		while (!thr.setTask(true, false, args, root)) {
			synchronized(thr) {
				if (thr.isBusy())
					thr.wait();
			}
		}
		if (!sync)
			return;
		while (thr.isBusy()) {
			synchronized(thr) {
				if (thr.isBusy())
					thr.wait();
			}
		}
	}
	
	public static void runBackEnd(WorkerThread thr, ASTNode root, KievBackend be, boolean sync) {
		if (be != null)
			Kiev.useBackend = be;
		else
			be = Kiev.useBackend;
		if( Kiev.verbose ) Kiev.reportInfo("Running back-end "+be,0);
//		long be_start_time = System.currentTimeMillis();

		while (!thr.setTask(false, true, null, root)) {
			synchronized(thr) {
				if (thr.isBusy())
					thr.wait();
			}
		}
		if (!sync)
			return;
		while (thr.isBusy()) {
			synchronized(thr) {
				if (thr.isBusy())
					thr.wait();
			}
		}

//		boolean ret = (thr.errCount == 0);
//		long diff_time = System.currentTimeMillis() - be_start_time;
//		if( Kiev.verbose ) Kiev.reportInfo("Back-end "+be+" completed: "+(ret?"OK":"FAIL"),diff_time);
//		return ret;
	}

	public static void runGC(WorkerThread thr) {
		java.lang.Runtime rt = java.lang.Runtime.getRuntime();
		long old_free = rt.freeMemory()/1024;
		long old_total = rt.totalMemory()/1024;
		long old_busy = old_total - old_free;
		if( thr.programm_mem < old_busy ) thr.programm_mem = old_busy;
		if( (old_total - old_free) > Compiler.gc_mem ) {
			if( Kiev.verbose ) {
				rt.gc();
				long new_free = rt.freeMemory()/1024;
				long new_total = rt.totalMemory()/1024;
				long new_busy = new_total - new_free;
				System.out.println("[GC (busy/free/total):"+old_busy+"/"+old_free+"/"+old_total+" -> "+new_busy+"/"+new_free+"/"+new_total+" = +"+(new_free-old_free)+" Kb ]");
			} else {
				rt.gc();
			}
		} else {
//			if( Kiev.debug )
//				System.out.println("[Garbage collection skipped: "+(old_total-old_free)+"/"+gc_mem+" Kb ]");
		}
	}

	public static void printHelp() {
		StringBuffer exts = new StringBuffer();
		int N = Compiler.getCmdLineExtSet().length;
		for (int i=0, sz=0; i < N; i++) {
			try {
				KievExt ext = (KievExt)i;
				String s = ext.toString();
				if (sz + s.length() + 3 >= 70) {
					exts.append("\n\t");
					sz = 0;
				}
				sz += s.length() + 3;
				exts.append('\"').append(s).append("\" ");
			} catch (ClassCastException e) {}
		}
		System.out.println(
 "Usage:  java kiev.Main [-g] [-debug x,y,z,..] [-prompt] [-i]\n"
+"           [-pipe] [-server]\n"
+"           [-gc N] [-d output_dir] [-verbose] [-classpath search_path]\n"
+"           [-D=var] [-verify] file.java ...\n"
+"\n"
+"  Each switch may be prepended by '-no' to change the default value.\n"
+"      For example, -no-project defeats the project feature.\n"
+"\n"
+" --  End of switches.  The allows the compiler to treat the\n"
+"     remainder of the command line as input files to compile.\n"
+"     This is necessary due to the syntax of certain options.\n"
+"\n"
+" -gc N  Invoke garbage collection if allocated memory exceeds N megabytes.\n"
+"        Default: no upper limit.\n"
+"\n"
+" -g[:atclvr] Generate classes with debug info and other checks.  Parameters:\n"
+"		       Assert, Trace, Condition, Linetable, Vartable, Rule tracing\n"
+" -debug opt1,opt2,...	- debug output, options are:\n"
+"    stat     statement generation\n"
+"    asm      assembler generation\n"
+"    instr    instractions generation\n"
+"    bcread   bytecode .class reading\n"
+"    bcwrite  bytecode .class generation\n"
+"    bcrules  bytecode .class patching and rules\n"
+"    resolv   identifier resolving\n"
+"    flags    flags change for members\n"
+"    member   members attaching (AST generation)\n"
+"    create   members creation\n"
+"    ast      AST tree\n"
+"    types    var/field types\n"
+"\n"
+" -v or -verbose         Verbose operation.\n"
+" -prompt                Interactive compilation.\n"
+" -i or -incremental     Set incremental mode (autoset -make)\n"
+" -pipe [host][:port]    Connect to server at host:port\n"
+" -server [host][:port]  Kiev compiler is server at host:port;\n"
+"                        default is 127.0.0.1:1966\n"
+"\n"
+" -p or -project xxx     Sets the project file.  Default:  \"project\".\n"
+" -makeall               Remake all project files.\n"
+" -make                  Make project files.\n"
+"\n"
+" -classpath xxx   Sets the CLASSPATH; if not specified, the environment\n"
+"                  variable is used.\n"
+" -d               Output root directory.  Default: \"classes\".\n"
+"\n"
+" -java            Java source code input; Kiev-specific keywords\n"
+"                  are treated as identifiers.\n"
+" -verify          Generate verifiable code.\n"
+" -safe            Do not generate class files with compiler errors.\n"
+"\n"
+" -Dvar[=value]    Set compile-time constant.  Default value is \"\" or true.\n"
+"      To unset the value, use -no-Dvar (sets to null or false).\n"
+"      All periods ('.') in $Dvar are replaced with underscores ('_').\n"
+"      System properties provide predefined variables,	as supported by\n"
+"      (System.getProperties()).\n"
+"      The compile-time constants $FILE, $METHOD, $LINENO, and $DEBUG\n"
+"      are also defined.\n"
+" -disable \"extension\" completly disable the specified extension,\n"
+"      also you can manage extensions by pragmas in file headers,\n"
+"      complete list of extensions to enable/disable is:\n\t"
+       exts.toString()
			);
	}

/** is string not an option and has it embedded '*' or '?' characters?
 */
    static boolean hasWildcards(String s) {
        return
            s.length() > 0 &&
            s.charAt(0) != '-' &&
            (s.indexOf('*') >= 0 || s.indexOf("?") >= 0);
    }

/** does string s[soffset..] match pattern p[poffset..]?
 *  p can contain wildcards.
 */
    static boolean matches(String s, int soffset,
                           String p, int poffset) {
        if (poffset >= p.length())
            return soffset >= s.length();
        else if (p.charAt(poffset) == '*')
            return
                matches(s, soffset, p, poffset + 1) ||
                (soffset < s.length() &&
                 matches(s, soffset + 1, p, poffset));
        else
            return
                soffset < s.length() &&
                (p.charAt(poffset) == '?' ||
                 Character.toUpperCase(s.charAt(soffset)) ==
                 Character.toUpperCase(p.charAt(poffset))) &&
                matches(s, soffset + 1, p, poffset + 1);
    }

	/** add all files matching pattern string s to buffer expargs.
	*/
	static String[] addExpansion(String[] args, int pos) throws IOException {
		String s = args[pos];
		File f = new File(s);
		String path = f.getParent();
		String name = f.getName();
		if (path == null)
			if (s.indexOf(File.separatorChar) == 0) path = "";
			else path = ".";
		File dir = new File(path + File.separatorChar);
		if (dir == null) return args;
		String[] files = dir.list();
		args[pos] = null;
		if (files != null) {
			for (int j = 0; j < files.length; j++) {
				if (matches(files[j], 0, name, 0)) {
					args = (String[])Arrays.appendUniq(args,path + File.separator + files[j]);
				}
			}
		}
		return args;
	}

	/** add all files from project file if need to rebuild
	*/
	static String[] addRequaredToMake(String[] args) {
		for(Enumeration<String> e=Env.projectHash.keys(); e.hasMoreElements();) {
			try {
				String key = e.nextElement();
				ProjectFile value = Env.projectHash.get(key);
				if (value.type == ProjectFileType.FORMAT) {
					String nm = value.file.toString();
					if( !Arrays.contains(args,nm) ) {
						if( Kiev.verbose ) System.out.println("File "+nm+" - format");
						args = (String[])Arrays.append(args,nm);
					}
					continue;
				}
				if (value.type == ProjectFileType.METATYPE) {
					String nm = value.file.toString();
					if( !Arrays.contains(args,nm) ) {
						if( Kiev.verbose ) System.out.println("File "+nm+" - metatype");
						args = (String[])Arrays.append(args,nm);
					}
					continue;
				}
				File fclass = new File(Kiev.output_dir,value.bname.toString());
				if( fclass.exists() && fclass.isDirectory() ) {
					fclass = new File(Kiev.output_dir,value.bname+"/package.class");
				} else {
					fclass = new File(Kiev.output_dir,value.bname+".class");
				}
				File fjava = value.file;
				if( !fjava.exists() ) continue;
				if( value.bad || !fclass.exists() ) {
					String nm = fjava.toString();
					if( Kiev.verbose ) System.out.println("File "+nm+" - "+value.bname+" "+(value.bad?"is bad":"does not exists"));
					args = (String[])Arrays.appendUniq(args,nm);
					continue;
				}
				long fclass_modified = fclass.lastModified();
				long fjava_modified = fjava.lastModified();
				if( fclass_modified < fjava_modified || Compiler.makeall_project ) {
					String nm = fjava.toString();
					if( !Arrays.contains(args,nm) ) {
						if( Kiev.verbose ) System.out.println("File "+nm+" - outdated");
						args = (String[])Arrays.append(args,nm);
					}
				}
			} catch ( IOException exc ) {
				continue;
			}
		}
		return args;
	}

}

