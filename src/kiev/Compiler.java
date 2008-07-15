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
import syntax kiev.Syntax;

import kiev.stdlib.Arrays;

import java.util.IdentityHashMap;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@singleton
public class CompilerThread extends WorkerThread {
	private CompilerThread() { super("compiler"); }
	public static CompilerThread getInst() { return CompilerThread; } 
}

@singleton
public class EditorThread extends WorkerThread {
	private EditorThread() { super("editor"); }
	public static EditorThread getInst() { return EditorThread; } 
}

public final class CompilerParseInfo {
	// either of file or fname (with optional fdata) must be specified
	final String	fname;
	final byte[]	fdata;
	// add or not add to the project file
	final boolean	add_to_project;
	// resulting FileUnit
	public FileUnit	fu;
	
	public CompilerParseInfo(File file, boolean add_to_project) {
		this.fname = file.getPath().replace(File.separatorChar, '/').intern();
		this.fdata = null;
		this.add_to_project = add_to_project;
	}
	public CompilerParseInfo(String fname, byte[] fdata, boolean add_to_project) {
		if (fname != null)
			this.fname = fname.replace(File.separatorChar, '/').intern();
		this.fdata = fdata;
		this.add_to_project = add_to_project;
	}
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
	public Project.FileEnumerator	fileEnumerator;

	public IdentityHashMap			dataFlowInfos = new IdentityHashMap(1023);

	private boolean					busy;
	private boolean					run_fe;
	private boolean					run_be;
	private CompilerParseInfo[]		args;
	private ANode					root;

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
					runFrontEndParse();
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
				fileEnumerator = null;
			}
		}
	}
	public boolean isBusy() {
		return busy;
	}
	public boolean setTask(boolean run_fe, boolean run_be, CompilerParseInfo[] args, ANode root) {
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
	private void runFrontEndParse() {
		this.programm_start = this.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		try {
//			if( Kiev.verbose ) System.out.println(Compiler.version);
//			Runtime.getRuntime().traceMethodCalls(Compiler.methodTrace);

			if( !Kiev.initialized ) {
				if (args == null)
					args = new CompilerParseInfo[0];
				Env.getRoot().InitializeEnv(Kiev.compiler_classpath);
				foreach (CompilerParseInfo cpi; args; cpi.add_to_project && cpi.fname != null)
					Env.getProject().addProjectFile(cpi.fname);
				addRequaredToMake();
			}


			if( args == null || args.length == 0 )
				return;

			if( !Kiev.initialized ) {
				Class force_init = Class.forName(StdTypes.class.getName());
				Kiev.initialized = (force_init != null);
			}

			Kiev.resetFrontEndPass();
			
			Kiev.k = new Parser(new StringReader(""));
			for(int i=0; i < args.length; i++) {
				CompilerParseInfo cpi = args[i];
				try {
					this.curFile = cpi.fname.replace('/', File.separatorChar);
					if (this.curFile.toLowerCase().endsWith(".xml")) {
						cpi.fu = DumpUtils.loadFromXmlFile(new File(this.curFile), cpi.fdata);
						Kiev.runProcessorsOn(cpi.fu);
					} else {
						java.io.InputStreamReader file_reader = null;
						char[] file_chars = new char[8196];
						int file_sz = 0;
						try {
							if (cpi.fdata != null)
								file_reader = new InputStreamReader(new ByteArrayInputStream(cpi.fdata), "UTF-8");
							else
								file_reader = new InputStreamReader(new FileInputStream(this.curFile), "UTF-8");
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
						Compiler.runGC(this);
						diff_time = curr_time = System.currentTimeMillis();
						Kiev.k.ReInit(bis);
						cpi.fu = Kiev.k.FileUnit(cpi.fname);
						cpi.fu.current_syntax = "stx-fmt\u001fsyntax-for-java";
						diff_time = System.currentTimeMillis() - curr_time;
						bis.close();
					}
					Compiler.runGC(this);
					this.curFile = "";
					if( Kiev.verbose )
						Kiev.reportInfo("Parsed  file   "+cpi.fname,diff_time);
					System.out.flush();
				} catch (Exception e) {
					Kiev.reportParserError(0,e);
				}
			}
			Compiler.runGC(this);

			this.root = Env.getRoot();

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		dataFlowInfos.clear();
		Env.getRoot().dumpProjectFile();
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
				Compiler.runGC(this);
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && msg != null) Kiev.reportInfo(msg,diff_time);
				if( this.errCount > 0 ) goto stop;
			} while (Kiev.nextFrontEndPass());

			{
				diff_time = curr_time = System.currentTimeMillis();
				Kiev.runVerifyProcessors(root);
				Compiler.runGC(this);
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose) Kiev.reportInfo("Tree verification",diff_time);
				if( this.errCount > 0 ) goto stop;
			}

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		dataFlowInfos.clear();
		Kiev.lockNodeTree(root);
		Env.classHashOfFails.clear();
		Env.getRoot().dumpProjectFile();
		Compiler.runGC(this);
		if (this.errCount > 0)
			run_be = false;
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Frontend");
	}

	private void runBackEnd() {
		long curr_time = 0L, diff_time = 0L;
		Transaction tr_me = Transaction.open("Compiler.java:runBackEnd(1)");
		try {
			////////////////////////////////////////////////////
			//	                  Midend                      //
			////////////////////////////////////////////////////

			if (!ASTNode.EXECUTE_UNVERSIONED) {
				Env.getRoot().walkTree(new TreeWalker() {
					public boolean pre_exec(ANode n) {
						if (n instanceof ASTNode) {
							ASTNode astn = (ASTNode)n;
							if (!astn.locked)
								System.out.println("Unlocked node "+n);
							astn.compileflags = 3; // locked & versioned
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
			Env.classHashOfFails.clear();

			foreach(FileUnit fu; Env.getProject().enumerateAllFiles(); !fu.isAutoGenerated()) {
				final int errCount = this.errCount;
				if (fu.scanned_for_interface_only)
					continue; // don't run back-end on interface (API) files
				Transaction tr = Transaction.open("Compiler.java:runBackEnd(2) on "+fu);
				try {
					Kiev.resetBackEndPass();
					Kiev.openBackEndFileUnit(fu);
					do {
						diff_time = curr_time = System.currentTimeMillis();
						String msg = Kiev.runCurrentBackEndProcessor(fu);
						diff_time = System.currentTimeMillis() - curr_time;
						if( Kiev.verbose && msg != null) Kiev.reportInfo(msg,diff_time);
					} while (Kiev.nextBackEndPass() && this.errCount == errCount);
					Kiev.closeBackEndFileUnit();
				} finally { tr.rollback(false); }
				Compiler.runGC(this);
			}
			//Env.classHashOfFails.clear();
		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				return;
			Kiev.reportError(e);
		} finally {
			dataFlowInfos.clear();
			tr_me.rollback(false);
			((WorkerThread)Thread.currentThread()).fileEnumerator = null;
		}
		Env.getRoot().dumpProjectFile();
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Backend");
	}

	
	private boolean containsFileName(String fname) {
		foreach (CompilerParseInfo cpi; args; cpi.fname.equals(fname))
			return true;
		return false;
	}
	
	/** add all files from project file if need to rebuild
	*/
	private void addRequaredToMake() {
		foreach (FileUnit fu; Env.getProject().enumerateAllFiles(); !containsFileName(fu.pname())) {
			if( Kiev.verbose ) System.out.println("File "+fu.pname());
			args = (CompilerParseInfo[])Arrays.appendUniq(args,new CompilerParseInfo(fu.pname(),null,true));
		}
		fileEnumerator = null;
	}

	public void reportTotals(String src) {
		if( errCount > 0 )
			System.out.println(errCount+" errors");
		if( warnCount > 0 )
			System.out.println(warnCount+" warnings");
		programm_end = System.currentTimeMillis();
//		System.out.println(src+": total "+(programm_end-programm_start)+"ms, max memory used = "+programm_mem+" Kb\007\007");
		System.out.println(src+": total "+(programm_end-programm_start)+"ms, max memory used = "+programm_mem+" Kb");
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

	public static final String version = "SymADE (v 0.5a), (C) UAB MAKSINETA, 1997-2008, http://symade.com";

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
	public static boolean interface_only		= false;
	public static boolean initialized			= false;

	public static String project_file			= null;
	public static String output_dir				= "classes";
	public static String compiler_classpath	= null;
	public static boolean javacerrors			= false;
	public static boolean nowarn				= false;
	public static boolean code_nowarn			= true;

	public static KievBackend useBackend = KievBackend.Java15;

	public static CError testError				= null;
	public static int    testErrorLine			= 0;
	public static int    testErrorOffs			= 0;

	public static long gc_mem				= (long)(1024*1024);

	private static boolean[] command_line_disabled_extensions	= new boolean[KievExt.values().length];

	static {
		Compiler.setExtension(false, "view");
		Compiler.setExtension(false, "vnode");
		Compiler.setExtension(false, "dflow");
		Compiler.setExtension(false, "xpath");
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

				if( args[a].equals("-disable") ) {
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
						System.setProperty("symade.unversioned","false");
					continue;
				}
				else if( args[a].equals("-batch") ) {
					args[a] = null;
					System.setProperty("symade.unversioned",String.valueOf(!onoff));
					if (onoff)
						Compiler.run_gui = false;
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
				else if( args[a].equals("-codewarn")) {
					Compiler.code_nowarn = !onoff;
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
						System.getProperties().setProperty(prop,value);
					} else {
						System.getProperties().remove(prop);
					}
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-prop")) {
					args[a] = null;
					String fname = args[++a];
					args[a] = null;
					try {
						InputStream inp = new FileInputStream(fname);
						//System.getProperties().load(new InputStreamReader(inp, "UTF-8"));
						System.getProperties().load(inp);
					} catch( IOException e ) {
						Kiev.reportError("Error opening property file: "+e);
					}
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
				String fn = args[i].replace('/', File.separatorChar).replace('\\', File.separatorChar);
				args1 = (String[])Arrays.appendUniq(args1,fn);
			}
			args = args1;
		} catch( ArrayIndexOutOfBoundsException e) {
			args = new String[0];
		}
		return args;
	}
	
	public static void run(String[] args) {
		Compiler.interface_only = false;

		try {
			args = parseArgs(args);
		} catch( Exception e) {
			Kiev.reportError(e);
			return;
		}
		
		CompilerThread thr = CompilerThread;
		Vector cargs = new Vector();
		foreach (String arg; args)
			cargs.add(new CompilerParseInfo(arg, null, true));
		runFrontEnd(thr, (CompilerParseInfo[])cargs.toArray(new CompilerParseInfo[cargs.size()]), null, true);

		if (Kiev.run_gui) {
			//kiev.gui.Window wnd = new kiev.gui.Window();
			Object wnd = Class.forName("kiev.gui.Main").newInstance();
			for(;;) Thread.sleep(10*1000);
		} else {
			if (thr.errCount == 0)
				runBackEnd(thr, Env.getRoot(), Compiler.useBackend, true);
			System.exit(thr.errCount > 0 ? 1 : 0);
		}
	}
	
	public static void runFrontEnd(WorkerThread thr, CompilerParseInfo[] args, ANode root, boolean sync) {
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
+" -warn                  Show warnings.\n"
+" -codewarn              Show codegeneration warnings.\n"
+" -prompt                Interactive compilation.\n"
+" -pipe [host][:port]    Connect to server at host:port\n"
+" -server [host][:port]  Kiev compiler is server at host:port;\n"
+"                        default is 127.0.0.1:1966\n"
+"\n"
+" -p or -project xxx     Sets the project file.  Default is no project.\n"
+"\n"
+" -classpath xxx   Sets the CLASSPATH; if not specified, the environment\n"
+"                  variable is used.\n"
+" -d               Output root directory.  Default: \"classes\".\n"
+"\n"
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
		String s = args[pos].replace('/', File.separatorChar).replace('\\', File.separatorChar);
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
}

