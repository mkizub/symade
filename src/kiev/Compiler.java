/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev;

import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import java.io.*;
import java.net.*;
import java.util.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Compiler {
	public static ServerSocket		server;
	public static Socket			socket;
	public static InputStream		system_in = System.in;
	public static PrintStream		system_out = System.out;
	public static PrintStream		system_err = System.err;

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
		try {
			for(; a < alen ;a++) {
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
					Kiev.javaMode = onoff;
					if (onoff) {
						Kiev.verify = true;
						Kiev.safe = true;
						Kiev.project_file = null;
						Kiev.output_dir = null;
					}
					args[a] = null;
				}
				else if( args[a].equals("-disable") ) {
					args[a] = null;
					Kiev.setExtension(!onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-enable") ) {
					args[a] = null;
					Kiev.setExtension(onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-prompt")) {
					Kiev.errorPrompt = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-g") ) {
					Kiev.debugOutputA = onoff;
					Kiev.debugOutputT = onoff;
					Kiev.debugOutputC = onoff;
					Kiev.debugOutputL = onoff;
					Kiev.debugOutputV = onoff;
					Kiev.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].startsWith("-g:")) {
					if( args[a].indexOf('a') > 0 )
						Kiev.debugOutputA = onoff;
					if( args[a].indexOf('t') > 0 )
						Kiev.debugOutputT = onoff;
					if( args[a].indexOf('c') > 0 )
						Kiev.debugOutputC = onoff;
					if( args[a].indexOf('l') > 0 )
						Kiev.debugOutputL = onoff;
					if( args[a].indexOf('v') > 0 )
						Kiev.debugOutputV = onoff;
					if( args[a].indexOf('r') > 0 )
						Kiev.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-debug")) {
					args[a] = null;
					String dbg = args[++a];
					args[a] = null;
					System.out.println("Debugging: "+onoff);
					if( dbg.indexOf("stat",0) >= 0 ) {
						System.out.println("\tstatement generation");
						Kiev.debugStatGen	= onoff;
					}
					if( dbg.indexOf("asm",0) >= 0 ) {
						System.out.println("\tassembler generation");
						Kiev.debugAsmGen		= onoff;
					}
					if( dbg.indexOf("instr",0) >= 0 ) {
						System.out.println("\tinstractions generation");
						Kiev.debugInstrGen	= onoff;
					}
					if( dbg.indexOf("bcwrite",0) >= 0 ) {
						System.out.println("\tbytecode .class generation");
						Kiev.debugBytecodeGen = onoff;
						kiev.bytecode.Clazz.traceWrite = onoff;
					}
					if( dbg.indexOf("bcread",0) >= 0 ) {
						System.out.println("\tbytecode .class reading");
						Kiev.debugBytecodeRead = onoff;
						kiev.bytecode.Clazz.traceRead = onoff;
					}
					if( dbg.indexOf("bcpatch",0) >= 0 ) {
						System.out.println("\tbytecode .class patching and rules");
						kiev.bytecode.Clazz.traceRules = onoff;
					}
					if( dbg.indexOf("resolv",0) >= 0 ) {
						System.out.println("\tidentifier resolving");
						Kiev.debugResolve	= onoff;
					}
					if( dbg.indexOf("operat",0) >= 0 ) {
						System.out.println("\toperator resolving");
						Kiev.debugOperators	= onoff;
					}
					if( dbg.indexOf("flags",0) >= 0 ) {
						System.out.println("\tflags change for members");
						Kiev.debugFlags		= onoff;
					}
					if( dbg.indexOf("member",0) >= 0 ) {
						System.out.println("\tmembers attaching (AST generation)");
						Kiev.debugMembers	= onoff;
					}
					if( dbg.indexOf("create",0) >= 0 ) {
						System.out.println("\tmembers creation");
						Kiev.debugCreation	= onoff;
					}
					if( dbg.indexOf("ast",0) >= 0 ) {
						System.out.println("\tAST tree");
						Kiev.debugAST		= onoff;
					}
					if( dbg.indexOf("methodtrace",0) >= 0 ) {
						System.out.println("\tMethod tracing");
						Kiev.debugMethodTrace	= onoff;
					}
					if( dbg.indexOf("multimethod",0) >= 0 ) {
						System.out.println("\tMultiMethod generation");
						Kiev.debugMultiMethod	= onoff;
					}
					if( dbg.indexOf("rule",0) >= 0 ) {
						System.out.println("\trules");
						Kiev.debugRules		= onoff;
					}
					if( dbg.indexOf("types",0) >= 0 ) {
						System.out.println("\tvar/field types");
						Kiev.debugNodeTypes		= onoff;
					}
					Kiev.debug = onoff;
					continue;
				}
				else if( args[a].equals("-trace")) {
					args[a] = null;
					String dbg = args[++a];
					args[a] = null;
					System.out.println("Tracing: "+onoff);
					if( dbg.indexOf("rules",0) >= 0 ) {
						System.out.println("\tprolog rules");
						Kiev.traceRules		= onoff;
						kiev.stdlib.PEnv.debug = onoff;
					}
					Kiev.debug = onoff;
					continue;
				}
				else if( args[a].equals("-verbose") || args[a].equals("-v") ) {
					Kiev.verbose = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-verify") ) {
					Kiev.verify = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-safe") ) {
					Kiev.safe = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-d")) {
					args[a] = null;
					if( onoff ) {
						Kiev.output_dir = args[++a];
						args[a] = null;
					} else {
						Kiev.output_dir = null;
					}
					continue;
				}
				else if( args[a].equals("-p") || args[a].equals("-project") ) {
					args[a] = null;
					if( onoff ) {
						Kiev.project_file = new File(args[++a]);
						args[a] = null;
					} else
						Kiev.project_file = null;
					continue;
				}
				else if( args[a].equals("-makedep") ) {
					args[a] = null;
					Kiev.interface_only = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-make") ) {
					args[a] = null;
					Kiev.make_project = onoff;
					continue;
				}
				else if( args[a].equals("-makeall") ) {
					args[a] = null;
					if( onoff )
						Kiev.make_project = true;
					Kiev.makeall_project = onoff;
					continue;
				}
				else if( args[a].equals("-classpath")) {
					args[a] = null;
					if( onoff ) {
						Kiev.compiler_classpath = args[++a];
						args[a] = null;
					} else
						Kiev.compiler_classpath = null;
					Kiev.initialized = false;
					continue;
				}
				else if( args[a].equals("-gc")) {
					args[a] = null;
					if( onoff ) {
						Kiev.gc_mem = (long)(Double.valueOf(args[++a]).doubleValue()*1024.D);
						args[a] = null;
					} else
						Kiev.gc_mem = (long)(1024*1024.D);
					continue;
				}
				else if( args[a].equals("-s")) {
					Kiev.source_only = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-i") || args[a].equals("-incremental")) {
					Kiev.interactive = onoff;
					if( onoff )
						Kiev.make_project = true;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-javacerrors")) {
					Kiev.javacerrors = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-warn")) {
					Kiev.nowarn = !onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-test")) {
					args[a++] = null;
					String[] errs = args[a].split(":");
					args[a] = null;
					Kiev.testError = CError.valueOf(errs[0]);
					Kiev.testErrorLine = Integer.parseInt(errs[1]);
					Kiev.testErrorOffs = Integer.parseInt(errs[2]);
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
						Kiev.reportError(0,"Error in arguments: "+e);
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
		Kiev.programm_start = Kiev.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		Kiev.interface_only = false;
		Kiev.makeall_project = false;

		try {
			args = parseArgs(args);
		} catch( Exception e) {
			Kiev.reportError(0,e);
			goto stop;
		}
		
		try {
			if( Kiev.debugMethodTrace ) {
				Runtime.getRuntime().traceMethodCalls(true);
			} else {
				Runtime.getRuntime().traceMethodCalls(false);
			}

			if( !Kiev.initialized )
				Env.InitializeEnv(Kiev.compiler_classpath);

			if( Kiev.make_project || Kiev.safe )
				args = addRequaredToMake(args);

			if( args.length == 0 ) {
				trace(Kiev.verbose,"All files are up to date\007\007");
				goto stop;
			}

			if( !Kiev.initialized ) {
				Class force_init = StdTypes.class;
				Kiev.initialized = (force_init != null);
			}

		} catch( Exception e) {
			Kiev.reportError(0,e);
			goto stop;
		}

		try {
//			if( Kiev.verbose ) System.out.println(Kiev.version);


			Kiev.pass_no = TopLevelPass.passStartCleanup;
			Kiev.files.cleanup();
			
			ExportJavaTop exporter = (ExportJavaTop)Kiev.getProcessor(Kiev.Ext.JavaOnly);

			Kiev.pass_no = TopLevelPass.passCreateTopStruct;
			for(int i=0; i < args.length; i++) {
				try {
					Kiev.curFile = KString.from(args[i]);
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
						file_reader.close();
					}
					java.io.CharArrayReader bis = new java.io.CharArrayReader(file_chars, 0, file_sz);
					kiev040.interface_only = true;
					runGC();
					diff_time = curr_time = System.currentTimeMillis();
					if( Kiev.k == null )
						Kiev.k = new kiev040(bis);
					else
						Kiev.k.ReInit(bis);
					FileUnit fu = Kiev.k.FileUnit(args[i]);
					Kiev.files.append(fu);
					diff_time = System.currentTimeMillis() - curr_time;
					bis.close();
					runGC();
					Kiev.curFile = KString.Empty;
					if( Kiev.verbose )
						Kiev.reportInfo("Scanned file   "+args[i],diff_time);
					System.out.flush();
				} catch (Exception e) {
					Kiev.reportParserError(0,e);
				}
			}
			runGC();


			////////////////////////////////////////////////////
			//		   PASS 1,2 - create top structures	   //
			////////////////////////////////////////////////////

			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.pass1(fu); });
			if( Kiev.errCount > 0 ) goto stop;
			if( Kiev.project_file != null ) {
				diff_time = curr_time = System.currentTimeMillis();
				Env.dumpProjectFile();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose ) Kiev.reportInfo("Dumped project file \'"+Kiev.project_file+'\'',diff_time);
			}
			if( Kiev.interface_only ) goto stop;
			diff_time = curr_time = System.currentTimeMillis();
			runGC();


			Kiev.pass_no = TopLevelPass.passProcessSyntax;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.pass1_1(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passArgumentInheritance;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.pass2(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passStructInheritance;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.pass2_2(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Class's declarations passed",diff_time);
			runGC();

			////////////////////////////////////////////////////
			//		   PASS Meta - resolve meta-info		   //
			////////////////////////////////////////////////////

			diff_time = curr_time = System.currentTimeMillis();
			Kiev.pass_no = TopLevelPass.passResolveMetaDecls;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.resolveMetaDecl(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passResolveMetaDefaults;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.resolveMetaDefaults(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passResolveMetaValues;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.resolveMetaValues(fu); });
			if( Kiev.errCount > 0 ) goto stop;

			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Meta information resolved",diff_time);
			runGC();


			////////////////////////////////////////////////////
			//		   PASS 3 - class' members					//
			////////////////////////////////////////////////////

			Kiev.pass_no = TopLevelPass.passCreateMembers;
			diff_time = curr_time = System.currentTimeMillis();
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.pass3(fu); });
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Class's pure interface declarations passed",diff_time);
			if( Kiev.errCount > 0) goto stop;
			runGC();

			///////////////////////////////////////////////////////////////////////
			///////////////////////    Parse bodies       /////////////////////////
			///////////////////////////////////////////////////////////////////////
			
			foreach (FileUnit fu; Kiev.files; !fu.scanned_for_interface_only) {
				try {
					runGC();
					diff_time = curr_time = System.currentTimeMillis();
					Kiev.parseFile(fu);
					diff_time = System.currentTimeMillis() - curr_time;
					Kiev.curFile = KString.Empty;
				} catch (Exception ioe) {
					Kiev.reportParserError(0,ioe);
				}
				if( Kiev.verbose )
					Kiev.reportInfo("Parsed file    "+fu,diff_time);
			}
			runGC();
				
			///////////////////////////////////////////////////////////////////////
			///////////////////////    VNode language     /////////////////////////
			///////////////////////////////////////////////////////////////////////
			
			Kiev.pass_no = TopLevelPass.passAutoGenerateMembers;
			diff_time = curr_time = System.currentTimeMillis();
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.autoGenerateMembers(fu); });
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Class's members created",diff_time);
			if( Kiev.errCount > 0 ) goto stop;
			runGC();

			Kiev.pass_no = TopLevelPass.passResolveImports;
			diff_time = curr_time = System.currentTimeMillis();
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.preResolve(fu); });
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.mainResolve(fu); });
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Class's members resolved",diff_time);
			if( Kiev.errCount > 0 ) goto stop;
			runGC();

			Kiev.pass_no = TopLevelPass.passVerify;
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.verify(fu); });
			if( Kiev.errCount > 0 ) goto stop;
			runGC();
			
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Semantic tree verification passed",diff_time);
	
			///////////////////////////////////////////////////////////////////////
			///////////////////////    Back-end    ////////////////////////////////
			///////////////////////////////////////////////////////////////////////

			Kiev.pass_no = TopLevelPass.passPreGenerate;
			diff_time = curr_time = System.currentTimeMillis();
			Kiev.runProcessors(fun (TransfProcessor tp, FileUnit fu)->void { tp.preGenerate(fu); });
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose ) Kiev.reportInfo("Class's members pre-generated",diff_time);
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passGenerate;
			for(int i=0; i < Kiev.files.length; i++) {
				runGC();
				try {
					Kiev.files[i].resolveDecl();
				} catch (Exception rte) {
					Kiev.reportError(0,rte);
				}
				runGC();
				try {
					ProcessVirtFld tp = (ProcessVirtFld)Kiev.getProcessor(Kiev.Ext.VirtualFields);
					if (tp != null)
						tp.rewriteNode(Kiev.files[i]);
				} catch (Exception rte) {
					Kiev.reportError(0,rte);
				}
				try {
					ProcessPackedFld tp = (ProcessPackedFld)Kiev.getProcessor(Kiev.Ext.PackedFields);
					if (tp != null)
						tp.rewriteNode(Kiev.files[i]);
				} catch (Exception rte) {
					Kiev.reportError(0,rte);
				}
				runGC();
				if( Kiev.source_only ) {
					if( Kiev.output_dir == null )
						if( Kiev.verbose ) System.out.println("Dumping to Java source file "+args[i]);
					else
						if( Kiev.verbose ) System.out.println("Dumping to Java source file "+args[i]+" into "+Kiev.output_dir+" dir");
					try {
						Kiev.files[i].toJava(Kiev.output_dir);
					} catch (Exception rte) { Kiev.reportError(0,rte); }
				} else {
					try {
						Kiev.files[i].generate();
					} catch (Exception rte) { Kiev.reportError(0,rte); }
				}
				Kiev.files[i].cleanup();
				Kiev.files[i] = null;
				runGC();
			}
		} catch( Throwable e) {
			if( e.getMessage() != null && e.getMessage().equals("Compilation terminated") ) {
				Env.dumpProjectFile();
			}
			Kiev.reportError(0,e);
			goto stop;
		}


stop:;
		Env.dumpProjectFile();
		if( Kiev.verbose || Kiev.errCount > 0 )
			Kiev.reportTotals();
		if !(Kiev.interactive) {
			System.exit(Kiev.errCount > 0 ? 1 : 0);
		}
		Kiev.errCount = 0;
		Kiev.warnCount = 0;
	}

	public static void runGC() {
		java.lang.Runtime rt = java.lang.Runtime.getRuntime();
		long old_free = rt.freeMemory()/1024;
		long old_total = rt.totalMemory()/1024;
		long old_busy = old_total - old_free;
		if( Kiev.programm_mem < old_busy ) Kiev.programm_mem = old_busy;
		if( (old_total - old_free) > Kiev.gc_mem ) {
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
		int N = Kiev.getCmdLineExtSet().length;
		for (int i=0, sz=0; i < N; i++) {
			try {
				Kiev.Ext ext = (Kiev.Ext)i;
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
+"           [-D=var] [-s] [-verify] file.java ...\n"
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
+" -makedep               Make depend, i.e. only scan files and update\n"
+"                        project file.\n"
+" -makeall               Remake all project files.\n"
+" -make                  Make project files.\n"
+"\n"
+" -classpath xxx   Sets the CLASSPATH; if not specified, the environment\n"
+"                  variable is used.\n"
+" -d               Output root directory.  Default: \"classes\".\n"
+"\n"
+" -java            Java source code input; Kiev-specific keywords\n"
+"                  are treated as identifiers.\n"
+" -s or -src       Generate Java source code instead of bytecode.\n"
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
		for(Enumeration<KString> e=Env.projectHash.keys(); e.hasMoreElements();) {
			try {
				KString key = (KString)e.nextElement();
				ProjectFile value = Env.projectHash.get(key);
				File fclass = new File(Kiev.output_dir,value.name.bytecode_name.toString());
				if( fclass.exists() && fclass.isDirectory() ) {
					fclass = new File(Kiev.output_dir,value.name.bytecode_name+"/package.class");
				} else {
					fclass = new File(Kiev.output_dir,value.name.bytecode_name+".class");
				}
				File fjava = value.file;
				if( !fjava.exists() ) continue;
				if( value.bad || !fclass.exists() ) {
					String nm = fjava.toString();
					if( Kiev.verbose ) System.out.println("File "+nm+" - "+value.name.bytecode_name+" "+(value.bad?"is bad":"does not exists"));
					args = (String[])Arrays.appendUniq(args,nm);
					continue;
				}
				long fclass_modified = fclass.lastModified();
				long fjava_modified = fjava.lastModified();
				if( fclass_modified < fjava_modified || Kiev.makeall_project ) {
					String nm = fjava.toString();
					if( !Arrays.contains((Object)args,(Object)nm) ) {
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

