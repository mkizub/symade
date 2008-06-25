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

import java.io.*;

import kiev.be.java15.Code;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

	
// Backends
static public enum KievBackend {
	Generic					: "generic",
	Java15					: "java15",
	VSrc					: "vsrc"
}

static public enum KievExt {
	JavaOnly				: "java-only"		,
	DumpAPI					: "dump-api"		,
	Rewrite					: "rewrite"			,
	GotoCase				: "goto-case"		,
	Goto					: "goto"			,
	With					: "with"			,
	Closures				: "closures"		,
	VirtualFields			: "virtual-fields"	,
	PackedFields			: "packed-fields"	,
	VarArgs					: "varargs"			,
	Forward					: "forward"			,
	Logic					: "logic"			,
	Alias					: "alias"			,
	Operator				: "operators"		,
	Typedef					: "typedef"			,
	Enum					: "enum"			,
	View					: "view"			,
	PizzaCase				: "pizza-case"		,
	Contract				: "contract"		,
	Generics				: "generics"		,
	Templates				: "templates"		,
	Wrappers				: "wrappers"		,
	Access					: "access"			,
	VNode					: "vnode"			,
	DFlow					: "dflow"			,
	XPath					: "xpath"
}
	
public final class Kiev {

	private Kiev() {}
	
	static class CompilationAbortError extends java.lang.Error {
		CompilationAbortError() { super("Compilation terminated"); }
	}
	
	public static String getCurFile() {
		if (Thread.currentThread() instanceof WorkerThread)
			return ((WorkerThread)Thread.currentThread()).curFile;
		return null;
	}
	
	public static void setCurFile(String cf) {
		if (Thread.currentThread() instanceof WorkerThread)
			((WorkerThread)Thread.currentThread()).curFile = cf;
	}
	
   	public static void reportError(Throwable e) {
		if (e instanceof CompilationAbortError)
			throw (CompilationAbortError)e;
		ASTNode dummy = null;
		reportError(dummy, e);
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
			TypeDecl clazz = null;
			Method method = null;
			try {
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = (ASTNode)from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_tdecl;
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
		report( pos, k.curFileUnit, null, null, SeverError.Error, msg);
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
		report( pos, k.curFileUnit, null, null, SeverError.Error, msg);
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
			report(pos, k.curFileUnit, null, null, SeverError.Error,e.getClass().getName());
		else
			report(pos, k.curFileUnit, null, null, SeverError.Error,e.getMessage());
	}

   	public static void reportError(String msg) {
		ASTNode dummy = null;
		reportError(dummy, msg);
	}
	
	public static void reportError(ASTNode from, String msg) {
		if( debug ) new Exception().printStackTrace(System.out);
		if (from != null) {
			int pos = from.pos;
			FileUnit fu = null;
			TypeDecl clazz = null;
			Method method = null;
			try {
				ASTNode f = from;
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = (ASTNode)from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_tdecl;
				fu = from.ctx_file_unit;
			} catch (Exception e) { /*ignore*/}
			report(pos,fu,clazz,method,SeverError.Error,msg);
		} else {
			report(0,null,null,null,SeverError.Error,msg);
		}
	}

	private static void report(int pos, FileUnit file_unit, TypeDecl clazz, Method method, SeverError err, String msg) {
		WorkerThread thr = null;
		if (Thread.currentThread() == CompilerThread)
			thr = CompilerThread;
		else if (Thread.currentThread() == EditorThread)
			thr = EditorThread;
		if (err == SeverError.Warning) {
			if (thr != null)
				thr.warnCount++;
		} else {
			if (thr != null)
				thr.errCount++;
			if( method != null ) method.setBad(true);
			if( clazz != null ) clazz.setBad(true);
		}
		String cf = null;
		if (file_unit != null) {
			cf = file_unit.pname();
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
		if( cf != null && verbose && (pos >>> 11) != 0 ) {
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
		if (Kiev.code_nowarn)
			return;
		if( debug && verbose) new Exception().printStackTrace(System.out);
		report(code.last_lineno<<11, (FileUnit)code.clazz.jctx_file_unit, (TypeDecl)code.clazz, (Method)code.method, SeverError.Warning, msg);
	}
	
	public static void reportWarning(String msg) {
		reportWarning(null, msg);
	}
	
	public static void reportWarning(ASTNode from, String msg) {
		if (nowarn)
			return;
		if( debug && verbose) new Exception().printStackTrace(System.out);
		if (from != null) {
			int pos = from.pos;
			int pos = from.pos;
			FileUnit fu = null;
			TypeDecl clazz = null;
			Method method = null;
			try {
				ASTNode f = from;
				for (int i=0; i < 3 && f != null && pos == 0; i++, f = (ASTNode)from.parent())
					pos = f.pos;
				method = from.ctx_method;
				clazz = from.ctx_tdecl;
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
	public static final boolean debug			= Compiler.debug;
	public static boolean debugStatGen			= Compiler.debugStatGen;
	public static boolean debugInstrGen		= Compiler.debugInstrGen;
	public static boolean debugBytecodeRead	= Compiler.debugBytecodeRead;
	public static boolean debugResolve			= Compiler.debugResolve;
	public static boolean debugOperators		= Compiler.debugOperators;
	public static boolean debugMembers			= Compiler.debugMembers;
	public static boolean debugCreation		= Compiler.debugCreation;
	public static boolean debugRules			= Compiler.debugRules;
	public static boolean debugMultiMethod		= Compiler.debugMultiMethod;
	public static boolean debugNodeTypes		= Compiler.debugNodeTypes;

	public static boolean verbose				= Compiler.verbose;
	public static boolean verify				= Compiler.verify;
	public static boolean safe					= Compiler.safe;
	public static boolean debugOutputA			= Compiler.debugOutputA;
	public static boolean debugOutputT			= Compiler.debugOutputT;
	public static boolean debugOutputC			= Compiler.debugOutputC;
	public static boolean debugOutputL			= Compiler.debugOutputL;
	public static boolean debugOutputV			= Compiler.debugOutputV;
	public static boolean debugOutputR			= Compiler.debugOutputR;

	public static boolean errorPrompt			= Compiler.errorPrompt;

	public static final boolean run_gui		= Compiler.run_gui;

	public static String output_dir				= Compiler.output_dir;
	public static String compiler_classpath	= Compiler.compiler_classpath;

	public static boolean javacerrors			= Compiler.javacerrors;
	public static boolean nowarn				= Compiler.nowarn;
	public static boolean code_nowarn			= Compiler.code_nowarn;
	
	public static CError testError				= Compiler.testError;
	public static int    testErrorLine			= Compiler.testErrorLine;
	public static int    testErrorOffs			= Compiler.testErrorOffs;

	public static boolean interface_only		= Compiler.interface_only;
	public static boolean initialized			= false;

	public static File project_file				= Compiler.project_file==null? null : new File(Compiler.project_file);

	// Scanning & parsing
	public static Parser				k;

	private static int		parserAddrIdx;

	public static KievBackend useBackend = Compiler.useBackend;

	private static int					fe_pass_no;
	private static int					me_pass_no;
	private static int					be_pass_no;
	private static TransfProcessor[]	feProcessors;
	private static VerifyProcessor[]	vfProcessors;
	private static BackendProcessor[]	meProcessors;
	private static BackendProcessor[]	beProcessors;
	static {
		{
			Vector<TransfProcessor> processors = new Vector<TransfProcessor>();
			processors.append(KievFE_Pass1);
			processors.append(KievFE_Pass2);
			processors.append(KievFE_CheckStdTypes);
			processors.append(KievFE_MetaDecls);
			processors.append(KievFE_MetaDefaults);
			processors.append(KievFE_MetaValues);
			processors.append(KievFE_Pass3);
			processors.append(PizzaFE_Pass3);
			processors.append(VNodeFE_Pass3);
			processors.append(VirtFldFE_GenMembers);
			processors.append(EnumFE_GenMembers);
			processors.append(ViewFE_GenMembers);
			processors.append(VNodeFE_GenMembers);
			processors.append(KievFE_PreResolve);
			processors.append(KievFE_MainResolve);
			feProcessors = processors.toArray();
		}
		
		{
			Vector<VerifyProcessor> processors = new Vector<VerifyProcessor>();
			processors.append(VNodeFE_Verify);
			processors.append(PackedFldFE_Verify);
			vfProcessors = processors.toArray();
		}
		
		{
			Vector<BackendProcessor> processors = new Vector<BackendProcessor>();
			processors.append(KievME_DumpAPI);
			processors.append(RewriteME_PreGenerate);
			processors.append(XPathME_PreGenerate);
			processors.append(KievME_PreGenartion);
			processors.append(PackedFldME_PreGenerate);
			processors.append(VirtFldME_PreGenerate);
			processors.append(PizzaME_PreGenerate);
			processors.append(ViewME_PreGenerate);
			processors.append(VNodeME_PreGenerate);
			processors.append(InnerBE_Rewrite);
			meProcessors = processors.toArray();
		}

		{
			Vector<BackendProcessor> processors = new Vector<BackendProcessor>();
			processors.append(KievBE_Resolve);
			processors.append(VirtFldBE_Rewrite);
			processors.append(KievBE_Generate);
			//processors.append(ExportBE_Generate);
			processors.append(KievBE_Cleanup);
			beProcessors = processors.toArray();
		}
	}
	
	private static boolean[] disabled_extensions = Compiler.getCmdLineExtSet();
	
	public static void resetFrontEndPass() {
		fe_pass_no = 0;
		me_pass_no = 0;
		be_pass_no = 0;
	}
	public static boolean nextFrontEndPass() {
		fe_pass_no += 1;
		return fe_pass_no < feProcessors.length;
	}
	public static void resetMidEndPass() {
		me_pass_no = 0;
		be_pass_no = 0;
	}
	public static boolean nextMidEndPass() {
		me_pass_no += 1;
		return me_pass_no < meProcessors.length;
	}
	public static void resetBackEndPass() {
		be_pass_no = 0;
	}
	public static boolean nextBackEndPass() {
		be_pass_no += 1;
		return be_pass_no < beProcessors.length;
	}
	
	public static boolean disabled(KievExt ext) {
		int idx = (int)ext;
		return disabled_extensions[idx];
	}
	
	public static boolean enabled(KievExt ext) {
		int idx = (int)ext;
		return !disabled_extensions[idx];
	}
	
	public static boolean[] getExtSet() {
		return (boolean[])disabled_extensions.clone();
	}
	
	public static void setExtSet(boolean[] set) {
		disabled_extensions = set;
	}
	
	public static void enable(KievExt ext) {
		disabled_extensions[((int)ext)] = false;
	}
	
	public static void lockNodeTree(ANode node) {
		if (ASTNode.EXECUTE_UNVERSIONED)
			return;
		node.walkTree(new TreeWalker() {
			public boolean pre_exec(ANode n) {
				if (n instanceof ASTNode) {
					ASTNode astn = (ASTNode)n;
					astn.compileflags = 3; // locked & versioned
				}
				return true;
			}
		});
	}
	
	public static String runCurrentFrontEndProcessor(ANode root) {
		TransfProcessor tp = feProcessors[fe_pass_no];
		if (!tp.isEnabled())
			return null;
		try {
			if (root == Env.getRoot()) {
				foreach(FileUnit fu; Env.getProject().enumerateAllFiles(); !fu.isAutoGenerated())
					runCurrentFEP(tp, fu);
			} else {
				if !(root instanceof FileUnit)
					root = root.ctx_file_unit;
				Enumeration<FileUnit> new_files = Env.getProject().enumerateNewFiles(); 
				runCurrentFEP(tp, (FileUnit)root);
				foreach(FileUnit fu; new_files; !fu.isAutoGenerated())
					runCurrentFEP(tp, fu);
			}
		} finally {
			((WorkerThread)Thread.currentThread()).fileEnumerator = null;
		}
		return tp.getDescr();
	}

	private static void runCurrentFEP(TransfProcessor tp, FileUnit fu) {
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(fu.pname());
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(fu.disabled_extensions);
			try {
				tp.process(fu,Transaction.get());
			}
			catch (Exception e) {
				Kiev.reportError(fu,e);
			}
		}
		finally {
			Kiev.setCurFile(curr_file);
			Kiev.setExtSet(exts);
		}
	}

	public static void runVerifyProcessors(ANode root) {
		Transaction tr = Transaction.enter(Transaction.get(),"Verification");
		try {
			root.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if !(n instanceof ASTNode)
						return false;
					ASTNode astn = (ASTNode)n;
					foreach (VerifyProcessor vp; vfProcessors; vp.isEnabled())
						vp.verify(astn);
					return n.preVerify();
				}
				public void post_exec(ANode n) {
					if (n instanceof ASTNode)
						n.postVerify();
				}
			});
		} finally { tr.leave(); }
	}

	public static String runCurrentMidEndProcessor() {
		BackendProcessor bp = meProcessors[me_pass_no];
		if (!bp.isEnabled())
			return null;
		try {
			foreach(FileUnit fu; Env.getProject().enumerateAllFiles(); !fu.isAutoGenerated()) {
				String curr_file = Kiev.getCurFile();
				Kiev.setCurFile(fu.pname());
				boolean[] exts = Kiev.getExtSet();
				try {
					Kiev.setExtSet(fu.disabled_extensions);
					try {
						bp.process(fu,Transaction.get());
					}
					catch (Exception e) {
						Kiev.reportError(fu,e);
					}
				}
				finally {
					Kiev.setCurFile(curr_file);
					Kiev.setExtSet(exts);
				}
			}
		} finally {
			((WorkerThread)Thread.currentThread()).fileEnumerator = null;
		}
		return bp.getDescr();
	}

	public static void openBackEndFileUnit(FileUnit fu) {
		Kiev.setCurFile(fu.pname());
		Kiev.setExtSet(fu.disabled_extensions);
	}
	public static void closeBackEndFileUnit() {
		Kiev.setCurFile("");
		Kiev.setExtSet(Compiler.getCmdLineExtSet());
	}
	
	public static String runCurrentBackEndProcessor(FileUnit fu) {
		BackendProcessor bp = beProcessors[be_pass_no];
		if (!bp.isEnabled())
			return null;
		try {
			bp.process(fu,Transaction.get());
		} catch (Exception e) {
			Kiev.reportError(e);
		}
		return bp.getDescr();
	}
	
	public static void runFrontEndProcessorsOn(ASTNode node) {
		for (int i=fe_pass_no; i < feProcessors.length; i++) {
			TransfProcessor tp = feProcessors[i];
			if (tp.isEnabled())
				tp.process(node,Transaction.get());
		}
	}

	public static void runProcessorsOn(ASTNode node) {
		int N = fe_pass_no;
		for (int i=0; i < N; i++) {
			TransfProcessor tp = feProcessors[i];
			if (tp.isEnabled())
				tp.process(node,Transaction.get());
		}
		if (N < feProcessors.length)
			return;
		Transaction tr = Transaction.enter(Transaction.get(),"Kiev.java:runProcessorsOn()");
		try {
			N = me_pass_no;
			for (int i=0; i < N; i++) {
				BackendProcessor mp = meProcessors[i];
				if (mp.isEnabled())
					mp.process(node,tr);
			}
			if (N < meProcessors.length)
				return;
			N = be_pass_no;
			for (int i=0; i < N; i++) {
				BackendProcessor bp = beProcessors[i];
				if (bp.isEnabled())
					bp.process(node,tr);
			}
		} finally { tr.leave(); }
	}

	public static void runProcessorsWithRewalk(ASTNode node) {
		assert(node.isAttached());
		try {
			Kiev.runProcessorsOn(node);
		} catch (ReWalkNodeException e) {
			runProcessorsWithRewalk((ASTNode)e.replacer);
			return;
		}
	}

}

