package kiev;

import kiev.stdlib.*;
import java.io.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;
import kiev.transf.*;
import kiev.be.java15.Code;
import kiev.be.java15.JNode;


/**
 * @author Maxim Kizub
 *
 */

public final class Kiev {

	private Kiev() {}
	
	static class CompilationAbortError extends java.lang.Error {
		CompilationAbortError() { super("Compilation terminated"); }
	}
	
	// Error section
	public static long		programm_start;
	public static long		programm_end;
	public static long		programm_mem = 0;
	public static int		errCount = 0;
	public static int		warnCount = 0;
	public static String	curFile = "";

   	public static void reportError(Throwable e) {
		ASTNode dummy = null;
		reportError(dummy, e);
	}
	
   	public static void reportError(ASTNode.NodeView from, Throwable e) {
		reportError((ASTNode)from,e);
	}
   	public static void reportError(JNode from, Throwable e) {
		reportError((ASTNode)from,e);
	}
	
   	public static void reportError(ASTNode from, Throwable e) {
		if (e instanceof CompilationAbortError)
			throw (CompilationAbortError)e;
		if (e instanceof CompilerException) {
			if (e.from != null)
				from = e.from;
		}
		if( debug ) e.printStackTrace(System.out);
		int pos = 0;
		if (from != null && from.isAttached()) {
			pos = from.pos;
			ASTNode f = from;
			FileUnit fu = null;
			Struct clazz = null;
			Method method = null;
			try {
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_clazz;
				fu = from.ctx_file_unit;
			} catch (Exception e) { /*ignore*/}
			if( e.getMessage() == null )
				report(pos,fu,clazz,method,SeverError.Error,e.getClass().getName());
			else
				report(pos,fu,clazz,method,SeverError.Error,e.getMessage());
		} else {
			if( e.getMessage() == null )
				report(0,null,null,null,SeverError.Error,e.getClass().getName());
			else
				report(0,null,null,null,SeverError.Error,e.getMessage());
		}
		if (testError != null) {
			if !(e instanceof CompilerException) {
				System.out.println("FAILED: expected CompilerException");
				System.exit(1);
			}
			else if ((pos>>>11) != testErrorLine || (pos&0x3FF) != testErrorOffs) {
				System.out.println("FAILED: expected position "+(pos>>>11)+":"+(pos&0x3FF));
				System.exit(1);
			}
			else if (((CompilerException)e).err_id != testError) {
				System.out.println("FAILED: expected error "+testError);
				System.exit(1);
			}
			System.out.println("SUCCESS: found expected error "+testError+" at "+(pos>>>11)+":"+(pos&0x3FF));
			System.exit(0);
		}
	}

   	public static void reportParserError(int pos, String msg) {
        errorPrompt = false;
		if( debug ) new Exception().printStackTrace(System.out);
		report( pos, k.curFileUnit, k.curClazz, k.curMethod, SeverError.Error, msg);
	}

   	public static void reportParserError(int pos, String msg, Throwable e) {
		if (e instanceof CompilationAbortError)
			throw (CompilationAbortError)e;
		if (e instanceof ParseException) {
			if (e.currentToken == null)
				;
			else if (e.currentToken.next == null)
				pos = e.currentToken.getPos();
			else
				pos = e.currentToken.next.getPos();
		}
        errorPrompt = false;
		if( debug ) e.printStackTrace(System.out);
		report( pos, k.curFileUnit, k.curClazz, k.curMethod, SeverError.Error, msg);
	}

   	public static void reportParserError(int pos, Throwable e) {
		if (e instanceof CompilationAbortError)
			throw (CompilationAbortError)e;
		if (e instanceof ParseException) {
			if (e.currentToken == null)
				pos = 0;
			else if (e.currentToken.next == null)
				pos = e.currentToken.getPos();
			else
				pos = e.currentToken.next.getPos();
		}
        errorPrompt = false;
		if( debug ) e.printStackTrace(System.out);
		if( e.getMessage() == null )
			report(pos, k.curFileUnit, k.curClazz, k.curMethod, SeverError.Error,e.getClass().getName());
		else
			report(pos, k.curFileUnit, k.curClazz, k.curMethod, SeverError.Error,e.getMessage());
	}

   	public static void reportError(String msg) {
		ASTNode dummy = null;
		reportError(dummy, msg);
	}
	
   	public static void reportError(ASTNode.NodeView from, String msg) {
		reportError((ASTNode)from,msg);
	}
   	public static void reportError(JNode from, String msg) {
		reportError((ASTNode)from,msg);
	}
	
	public static void reportError(ASTNode from, String msg) {
		if( debug ) new Exception().printStackTrace(System.out);
		if (from != null) {
			int pos = from.pos;
			FileUnit fu = null;
			Struct clazz = null;
			Method method = null;
			try {
				ASTNode f = from;
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_clazz;
				fu = from.ctx_file_unit;
			} catch (Exception e) { /*ignore*/}
			report(pos,fu,clazz,method,SeverError.Error,msg);
		} else {
			report(0,null,null,null,SeverError.Error,msg);
		}
	}

	private static void report(int pos, FileUnit file_unit, Struct clazz, Method method, SeverError err, String msg) {
		if (err == SeverError.Warning) {
			warnCount++;
		} else {
			errCount++;
			if( method != null ) method.setBad(true);
			if( clazz != null ) clazz.setBad(true);
		}
		String cf = null;
		if (file_unit != null) {
			cf = file_unit.id.sname;
			if (javacerrors) {
				String fn = new File(cf.toString()).getAbsolutePath();
				System.out.println(fn+":"+(pos>>>11)+": "+err+": "+msg);
			}
			else if (pos > 0) {
				System.out.println(cf+":"+(pos>>>11)+":"+(pos & 0x3FF)+": "+err+": "+msg);
			}
			else {
				System.out.println(err+": "+msg);
			}
		} else {
			System.out.println(err+": "+msg);
		}
		if( cf != null && (verbose || Kiev.javaMode) && (pos >>> 11) != 0 ) {
			File f = new File(cf.toString());
			if( f.exists() && f.canRead() ) {
				int lineno = pos>>>11;
				int colno = pos & 0x3FF;
				String ln = null;
				try {
					LineNumberReader rd = new LineNumberReader(new InputStreamReader(new FileInputStream(cf.toString()), "UTF-8"));
					while( rd.getLineNumber() != lineno ) ln = rd.readLine();
					rd.close();
//					ln = ln.replace('\t',' ');
					System.out.println(ln);
					StringBuffer sb = new StringBuffer(colno+5);
					for(int i=0; i < colno-1; i++) sb.append(' ');
					sb.append('^');
					System.out.println(sb.toString());
				} catch (IOException e){}
			}
		}
		if( errorPrompt && !err.equals("Warning") ) {
			System.out.println("R)esume, C)ontinue, A)bort:");
			int ch;
			while( true ) {
				try {
					ch=System.in.read();
				} catch( java.io.IOException e ) {
					ch = -1;
				}
				switch(ch) {
				case 'C':
				case 'c':
					errorPrompt = false;
					return;
				case 'R':
				case 'r':
					return;
				case -1:
				case 'a':
				case 'A':
					throw new CompilationAbortError();
				case ' ': case '\t': case '\r': case '\n':
					continue;
				}
				System.out.println("respond with 'r' or 'a'");
				System.out.println("R)esume, A)bort:");
			}
		}
	}

	public static void reportCodeWarning(Code code, String msg) {
		if (nowarn)
			return;
		if( debug && verbose) new Exception().printStackTrace(System.out);
		report(code.last_lineno<<11, (FileUnit)code.clazz.jctx_file_unit, (Struct)code.clazz, (Method)code.method, SeverError.Warning, msg);
	}
	
	public static void reportWarning(String msg) {
		reportWarning(null, msg);
	}
	
	public static void reportWarning(ASTNode.NodeView from, String msg) {
		reportWarning((ASTNode)from, msg);
	}
	
	public static void reportWarning(ASTNode from, String msg) {
		if (nowarn)
			return;
		if( debug && verbose) new Exception().printStackTrace(System.out);
		if (from != null) {
			int pos = from.pos;
			int pos = from.pos;
			FileUnit fu = null;
			Struct clazz = null;
			Method method = null;
			try {
				ASTNode f = from;
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_clazz;
				fu = from.ctx_file_unit;
			} catch (Exception e) { /*ignore*/}
			report(pos,fu,clazz,method,SeverError.Warning,msg);
		} else {
			report(0,null,null,null,SeverError.Warning,msg);
		}
	}

    private static char[] emptyString = new char[80];
    static { for(int i=0; i < 80; i++) emptyString[i] = ' '; }
	public static void reportInfo(String msg, long diff_time) {
    	StringBuffer sb = new StringBuffer(79);
        sb.append("[ ");
        sb.append(msg);
        String tm = String.valueOf(diff_time);
        int i = 73 - msg.length() - tm.length();
        while( i < 0 ) i+=80;
        sb.append(emptyString,0,i);
        sb.append(tm).append("ms ]");
		System./*err*/out.println(sb.toString());
		System./*err*/out.flush();
	}

	public static void reportTotals() {
		if( errCount > 0 )
			System.out.println(errCount+" errors");
		if( warnCount > 0 )
			System.out.println(warnCount+" warnings");
		programm_end = System.currentTimeMillis();
		System.out.println("total "+(programm_end-programm_start)+"ms, max memory used = "+programm_mem+" Kb");
		if (testError != null) {
			System.out.println("FAILED: there was no expected error "+testError+" at "+testErrorLine+":"+testErrorOffs);
			System.exit(1);
		}
		System.out.println("\007\007");
	}

/** sort (int) arrays of keys and values
 */
    static void qsort2(int[] keys, Object[] values, int lo, int hi) {
        int i = lo;
        int j = hi;
        int pivot = keys[(i+j)/2];
        do {
            while (keys[i] < pivot) i++;
            while (pivot < keys[j]) j--;
            if (i <= j) {
                int temp1 = keys[i];
                keys[i] = keys[j];
                keys[j] = temp1;
                Object temp2 = values[i];
                values[i] = values[j];
                values[j] = temp2;
                i++;
                j--;
            }
        } while (i <= j);
        if (lo < j) qsort2(keys, values, lo, j);
        if (i < hi) qsort2(keys, values, i, hi);
    }

	// Global flags and objects
	public static String version = "Kiev compiler (v 0.40), (C) Forestro, 1997-2005, http://forestro.com";
	//public static int revision = 000;

	public static boolean debug				= false;
	public static boolean debugStatGen		= false;
	public static boolean debugAsmGen		= false;
	public static boolean debugInstrGen		= false;
	public static boolean debugBytecodeGen	= false;
	public static boolean debugBytecodeRead	= false;
	public static boolean debugResolve		= false;
	public static boolean debugOperators	= false;
	public static boolean debugFlags		= false;
	public static boolean debugMembers		= false;
	public static boolean debugCreation		= false;
	public static boolean debugAST			= false;
	public static boolean debugMethodTrace	= false;
	public static boolean debugRules		= false;
	public static boolean traceRules		= false;
	public static boolean debugMultiMethod	= false;
	public static boolean debugNodeTypes	= false;

	public static boolean verbose			= false;
	public static boolean verify			= true;
	public static boolean safe				= true;
	public static boolean debugOutputA		= false;
	public static boolean debugOutputT		= false;
	public static boolean debugOutputC		= false;
	public static boolean debugOutputL		= false;
	public static boolean debugOutputV		= false;
	public static boolean debugOutputR		= false;

	public static boolean errorPrompt		= false;

	public static boolean run_gui			= false;
	public static boolean source_only		= false;
	public static boolean make_project		= false;
	public static boolean makeall_project	= false;
	public static boolean interactive		= false;
	public static boolean initialized		= false;

	public static String output_dir			= "classes";

	public static String compiler_classpath = null;

	public static File project_file			= new File("project");
	public static boolean interface_only	= false;

	public static long gc_mem				= (long)(1024*1024);

	public static boolean javacerrors		= false;
	public static boolean nowarn			= false;
	
	public static CError testError			= null;
	public static int    testErrorLine		= 0;
	public static int    testErrorOffs		= 0;

	// Scanning & parsing
	public static Parser				k;
	public static Vector<FileUnit>		files = new Vector<FileUnit>();
	public static TopLevelPass			pass_no = TopLevelPass.passStartCleanup;

	public static Hashtable<String,Object> parserAddresses = new Hashtable<String,Object>();
	private static int		parserAddrIdx;

	private static String parserAddr(Object obj) {
		String addr = Integer.toHexString(++parserAddrIdx);
		while( addr.length() < 8 ) {
			addr = '0'+addr;
		}
		Kiev.parserAddresses.put(addr,obj);
		return addr;
	}

	public static String reparseExpr(ENode e, boolean copy) {
		if (copy)
			return "#expr"+parserAddr(e.ncopy())+"#".toLowerCase();
		else
			return "#expr"+parserAddr(e)+"#".toLowerCase();
	}

	public static String reparseStat(ENode s, boolean copy) {
		if (copy)
			return "#stat"+parserAddr(s.ncopy())+"#".toLowerCase();
		else
			return "#stat"+parserAddr(s)+"#".toLowerCase();
	}

	public static String reparseType(Type tp) {
		return "#type"+parserAddr(tp)+"#".toLowerCase();
	}

	public static boolean passLessThen(TopLevelPass p) {
		return ((int)pass_no) < ((int)p);
	}
	public static boolean passGreaterEquals(TopLevelPass p) {
		return ((int)pass_no) >= ((int)p);
	}

	public static Block parseBlock(ASTNode from, StringBuffer sb) {
		StringBufferInputStream is = new StringBufferInputStream(sb.toString());
		FileUnit oldFileUnit = k.curFileUnit;
		Struct oldClazz = k.curClazz;
		Method oldMethod = k.curMethod;
		k.ReInit(is);
		k.reparse_body = true;
		k.reparse_pos = from.pos;
		k.curFileUnit = from.ctx_file_unit;
		k.curClazz = from.ctx_clazz;
		k.curMethod = from.ctx_method;
		Block body = null;
		try {
			body = k.Block();
		} finally {
			k.reparse_body = false;
			k.curFileUnit = oldFileUnit;
			k.curClazz = oldClazz;
			k.curMethod = oldMethod;
		}
		return body;
	}

	public static void parseFile(FileUnit f) {
		InputStreamReader file_reader = null;
		char[] file_chars = new char[8196];
		int file_sz = 0;
		curFile = f.id.sname;
		try {
			file_reader = new InputStreamReader(new FileInputStream(curFile.toString()), "UTF-8");
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
		} catch( Exception e ) {
			reportError(f,e);
			return;
		} finally {
			file_reader.close();
		}
		Kiev.k.interface_only = false;
		try {
			CharArrayReader bis = new CharArrayReader(file_chars, 0, file_sz);
			Kiev.k.ReInit(bis);
			foreach(PrescannedBody b; f.bodies; b != null ) {
				Kiev.k.curFileUnit = null;
				Kiev.k.curClazz = null;
				Kiev.k.curMethod = null;
				// callect parents of this block
				List<ASTNode> pl = List.Nil;
				ASTNode n = (ASTNode)b;
				while( n != null ) {
					pl = new List.Cons<ASTNode>(n,pl);
					n = n.parent();
				}
				if( pl.head() != f ) {
					reportError(b,"Prescanned body highest parent is "+pl.head()+" but "+f+" is expected");
					continue;
				}
				foreach(ASTNode nn; pl) {
					if (nn instanceof FileUnit)
						Kiev.k.curFileUnit = (FileUnit)nn;
					else if (nn instanceof Struct)
						Kiev.k.curClazz = (Struct)nn;
					else if (nn instanceof Method)
						Kiev.k.curMethod = (Method)nn;
				}
				Block bl;
				switch(b.mode) {
				case PrescannedBody.BlockMode:
					bl = Kiev.k.PrescannedBlock(b);
					break;
				case PrescannedBody.RuleBlockMode:
					bl = Kiev.k.PrescannedRuleBlock(b);
					break;
				default:
					throw new RuntimeException("Unknown mode of prescanned block: "+b.mode);
				}
				if( !((SetBody)b.parent()).setBody(bl) ) {
					reportError(b,"Prescanned body does not math");
				}
				b.replaceWithNode(null);
				pl = pl.reverse();
			}
		} finally {
			Kiev.k.curFileUnit = null;
			Kiev.k.curClazz = null;
			f.bodies.delAll();
		}
	}

	
	// Backends
	static public enum Backend {
		Generic					: "generic",
		Java15					: "java15",
		VSrc					: "vsrc"
	};
	public static Backend useBackend = Backend.Java15;

	// Extensions settings
	public static boolean javaMode			= false;
		//System.getProperties().get("java.ext.version") != null;
	static public enum Ext {
		Rewrite					: "rewrite"			,
		JavaOnly				: "java only"		,
		GotoCase				: "goto case"		,
		Goto					: "goto"			,
		With					: "with"			,
		Closures				: "closures"		,
		VirtualFields			: "virtual fields"	,
		PackedFields			: "packed fields"	,
		VarArgs					: "varargs"			,
		Forward					: "forward"			,
		Logic					: "logic"			,
		Alias					: "alias"			,
		Operator				: "operators"		,
		Typedef					: "typedef"			,
		Enum					: "enum"			,
		View					: "view"			,
		PizzaCase				: "pizza case"		,
		Contract				: "contract"		,
		Generics				: "generics"		,
		Templates				: "templates"		,
		Wrappers				: "wrappers"		,
		Access					: "access"			,
		VNode					: "vnode"			,
		DFlow					: "dflow"
	};
	
	private static boolean[] command_line_disabled_extensions	= new boolean[Ext.values().length];
	private static boolean[] disabled_extensions				= new boolean[Ext.values().length];
	
	public static TransfProcessor[] transfProcessors			= new TransfProcessor[Ext.values().length];
	static {
		transfProcessors[(int)Ext.Rewrite]			= ProcessRewrite;
		transfProcessors[(int)Ext.JavaOnly]		= ImportKievSrc;
		transfProcessors[(int)Ext.VirtualFields]	= ProcessVirtFld;
		transfProcessors[(int)Ext.PackedFields]	= ProcessPackedFld;
		transfProcessors[(int)Ext.Enum]				= ProcessEnum;
		transfProcessors[(int)Ext.View]				= ProcessView;
		transfProcessors[(int)Ext.PizzaCase]		= ProcessPizzaCase;
		transfProcessors[(int)Ext.VNode]			= ProcessVNode;
		transfProcessors[(int)Ext.DFlow]			= ProcessDFlow;
		setExtension(false, "vnode");
		setExtension(false, "dflow");
	}
	
	public static TransfProcessor getProcessor(Ext ext) {
		TransfProcessor tp = transfProcessors[(int)ext];
		if (tp != null && !tp.isDisabled())
			return tp;
		return null;
	}
	
	public static boolean disabled(Ext ext) {
		int idx = (int)ext;
		return disabled_extensions[idx];
	}
	
	public static boolean enabled(Ext ext) {
		int idx = (int)ext;
		return !disabled_extensions[idx];
	}
	
//	public static void check(int pos, Ext ext) {
//		if (!enabled(ext))
//			throw new CompilerException(pos,"Error: extension disabled, to enable use: pragma enable \""+ext+"\";");
//	}
	
	public static boolean[] getCmdLineExtSet() {
		return (boolean[])command_line_disabled_extensions.clone();
	}
	
	public static boolean[] getExtSet() {
		return (boolean[])disabled_extensions.clone();
	}
	
	public static void setExtSet(boolean[] set) {
		disabled_extensions = set;
	}
	
	public static void enable(Ext ext) {
		disabled_extensions[((int)ext)] = false;
	}
	
	static void setExtension(boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning("Unknown pragma '"+s+"'");
			return;
		}
		int i = (int)ext;
		if (i == 0) {
			for (int i=1; i < disabled_extensions.length; i++) {
				command_line_disabled_extensions[i] = !enabled;
				disabled_extensions[i] = !enabled;
			}
		} else {
			command_line_disabled_extensions[i] = !enabled;
			disabled_extensions[i] = !enabled;
		}
	}
	
	public static boolean runBackends(Kiev.Backend be, (BackendProcessor)->void step) {
		foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null) {
			if (!tp.isEnabled() )
				continue;
			BackendProcessor bep = tp.getBackend(be);
			if (bep != null)
				try { step(bep); } catch (Exception e) { Kiev.reportError(e); }
		}
		return (Kiev.errCount > 0); // true if failed
	}
	
	public static boolean runProcessors((TransfProcessor, FileUnit)->void step) {
		foreach (FileUnit fu; Kiev.files) {
			String curr_file = Kiev.curFile;
			Kiev.curFile = fu.id.sname;
			boolean[] exts = Kiev.getExtSet();
			try {
				Kiev.setExtSet(fu.disabled_extensions);
				foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null) {
					try {
						if (tp.isEnabled() )
							step(tp,fu);
					}
					catch (Exception e) {
						Kiev.reportError(fu,e);
					}
				}
			}
			finally {
				Kiev.curFile = curr_file;
				Kiev.setExtSet(exts);
			}
		}
		return (Kiev.errCount > 0); // true if failed
	}
	
	public static void runProcessorsOn(ASTNode node) {
		if ( Kiev.passGreaterEquals(TopLevelPass.passProcessSyntax) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.pass1(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passStructTypes) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.pass2(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passResolveMetaDecls) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.resolveMetaDecl(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passResolveMetaDefaults) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.resolveMetaDefaults(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passResolveMetaValues) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.resolveMetaValues(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passCreateMembers) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.pass3(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passAutoGenerateMembers) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.autoGenerateMembers(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passPreResolve) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.preResolve(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passMainResolve) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.mainResolve(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passVerify) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null)
				if (tp.isEnabled()) tp.verify(node);
		}
		if ( Kiev.passGreaterEquals(TopLevelPass.passPreGenerate) ) {
			foreach (TransfProcessor tp; Kiev.transfProcessors; tp != null) {
				if !(tp.isEnabled())
					continue;
				BackendProcessor bep = tp.getBackend(Kiev.useBackend);
				if (bep == null)
					continue;
				bep.preGenerate(node);
			}
		}
	}

}

