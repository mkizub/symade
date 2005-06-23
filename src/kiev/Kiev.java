/*
 Copyright (C) 1997-2000, Forestro, http://forestro.com

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
import java.io.*;
import kiev.vlang.*;
import kiev.parser.*;
import kiev.tree.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/Kiev.java,v 1.5.2.1.2.2 1999/05/29 21:03:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.2 $
 *
 */

public final class Kiev {

	private Kiev() {}

	// Error section
	public static long		programm_start;
	public static long		programm_end;
	public static long		programm_mem = 0;
	public static int		errCount = 0;
	public static int		warnCount = 0;
	public static KString	curFile = KString.Empty;
	public static KString	maybeCurFile = KString.Empty;

   	public static void reportError(int pos, Throwable e) {
		if( PassInfo.method != null ) PassInfo.method.setBad(true);
		if( PassInfo.clazz != null ) PassInfo.clazz.setBad(true);
		if( debug ) e.printStackTrace( /* */System.out /* */ );
		int p = pos;
		if( e instanceof CompilerException  ) {
			if( ((CompilerException)e).pos != 0 )
				p = ((CompilerException)e).pos;
			if( ((CompilerException)e).clazz != null ) {
				SourceFileAttr sfa = (SourceFileAttr)((CompilerException)e).clazz.getAttr(Constants.attrSourceFile);
				if( sfa != null ) maybeCurFile = sfa.filename;
			}
		}
		errCount++;
		if( e.getMessage() == null )
			reportError(p,"Error",e.getClass().toString());
		else
			reportError(p,"Error",e.getMessage());
	}

   	public static void reportParserError(int pos, String msg) {
		if( PassInfo.method != null ) PassInfo.method.setBad(true);
		if( PassInfo.clazz != null ) PassInfo.clazz.setBad(true);
        errorPrompt = false;
		errCount++;
		reportError( pos, "Error", msg);
//        System.exit(1);
	}

   	public static void reportParserError(int pos, String msg, Throwable e) {
		if( PassInfo.method != null ) PassInfo.method.setBad(true);
		if( PassInfo.clazz != null ) PassInfo.clazz.setBad(true);
        errorPrompt = false;
		if( debug ) e.printStackTrace( /* */System.out /* */ );
		errCount++;
		reportError( pos, "Error", msg);
//        System.exit(1);
	}

   	public static void reportParserError(int pos, Throwable e) {
		if( PassInfo.method != null ) PassInfo.method.setBad(true);
		if( PassInfo.clazz != null ) PassInfo.clazz.setBad(true);
        errorPrompt = false;
		if( debug ) e.printStackTrace( /* */System.out /* */ );
		int p = pos;
		if( e instanceof CompilerException  ) {
			if( ((CompilerException)e).pos != 0 )
				p = ((CompilerException)e).pos;
			if( ((CompilerException)e).clazz != null ) {
				SourceFileAttr sfa = (SourceFileAttr)((CompilerException)e).clazz.getAttr(Constants.attrSourceFile);
				if( sfa != null ) maybeCurFile = sfa.filename;
			}
		}
		reportError( p,e );
	}

	public static void reportError(int pos, String msg) {
		if( PassInfo.method != null ) PassInfo.method.setBad(true);
		if( PassInfo.clazz != null ) PassInfo.clazz.setBad(true);
		errCount++;
		reportError(pos,"Error",msg);
	}

	private static int getPossiblePos(int i) {
		if( i < 0 ) return 0;
		int pos = PassInfo.path[i].getPos();
		if( (pos >>> 11) == 0 ) return getPossiblePos(i-1);
		return pos;
	}

	public static void reportError(int pos, String err, String msg) {
		if( (pos >>> 11) == 0 ) pos = getPossiblePos(PassInfo.pathTop-1);
		KString cf = KString.Empty;
		if( curFile == null || curFile.equals(KString.Empty) ) {
			if( maybeCurFile != null && !maybeCurFile.equals(KString.Empty) )
				cf = maybeCurFile;
			else if( PassInfo.clazz != null ) {
				SourceFileAttr sfa = (SourceFileAttr)PassInfo.clazz.getAttr(Constants.attrSourceFile);
				if( sfa != null ) cf = sfa.filename;
			}
		} else {
			cf = curFile;
		}
		maybeCurFile = KString.Empty;
		if (javacerrors) {
			String fn = new File(cf.toString()).getAbsolutePath();
			System./*err*/out.println(fn+":"+(pos>>>11)+": "+err+": "+msg);
		} else {
			System./*err*/out.println(cf+":"+(pos>>>11)+":"+(pos & 0x3FF)+": "+err+": "+msg);
		}
		if( (verbose || Kiev.javaMode) && (pos >>> 11) != 0 ) {
			File f = new File(cf.toString());
			if( f.exists() && f.canRead() ) {
				int lineno = pos>>>11;
				int colno = pos & 0x3FF;
				String ln = null;
				try {
					LineNumberReader rd = new LineNumberReader(new FileReader(cf.toString()));
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
					throw new Error("Compilation terminated");
				case ' ': case '\t': case '\r': case '\n':
					continue;
				}
				System.out.println("respond with 'r' or 'a'");
				System.out.println("R)esume, A)bort:");
			}
		}
	}

	public static void reportWarning(int pos, String msg) {
		warnCount++;
		if( debug ) {
			try {
				throw new Exception();
			} catch( Exception e ) {
				e.printStackTrace( /* */System.out /* */ );
			}
		}
		if (!nowarn)
			reportError( pos,"Warning",msg );
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

			static class profInfo {
				Class cl;
				int max_instances;
				int curr_instances;
				int total_instances;
				profInfo(Class cl, int mi, int ci, int ti) {
					this.cl = cl;
					max_instances = mi;
					curr_instances = ci;
					total_instances = ti;
				}
				public String toString() {
					return cl+"\t"+curr_instances+"\t"+max_instances+"\t"+total_instances;
				}
			}

	public static void reportTotals() {
		if( errCount > 0 )
			System./*err*/out.println(errCount+" errors");
		if( warnCount > 0 )
			System./*err*/out.println(warnCount+" warnings");
		programm_end = System.currentTimeMillis();
		System./*err*/out.println("total "+(programm_end-programm_start)+"ms, max memory used = "+programm_mem+" Kb");
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
	public static String version = "Kiev compiler (v 0.20), (C) Forestro, 1997-2003, http://forestro.com";
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

	public static boolean gen_resolve		= false;

	public static boolean javacerrors		= false;
	public static boolean nowarn			= false;

    // New primitive objects section
	public static final Byte[]		byteArray	= new Byte[256];
	public static final java.lang.Character[]	charArray	= new java.lang.Character[128];
	public static final Short[]		shortArray	= new Short[256+1];
	public static final Integer[]	intArray	= new Integer[256*2+1];
	public static final Long[]		longArray	= new Long[128+1];
	public static final Float[]		floatArray	= new Float[4];
	public static final Double[]	doubleArray = new Double[4];

	static {
		// Fill hash arrays
		for(int i=0; i < byteArray.length; i++) {
			byteArray[i] = new Byte((byte)(i-128));
		}
		for(int i=0; i < charArray.length; i++) {
			charArray[i] = new java.lang.Character((char)i);
		}
		for(int i=0; i < shortArray.length; i++) {
			shortArray[i] = new Short((short)(i-128));
		}
		for(int i=0; i < intArray.length; i++) {
			intArray[i] = new Integer(i-256);
		}
		for(int i=0; i < longArray.length; i++) {
			longArray[i] = new Long((long)(i-64));
		}
		for(int i=0; i < floatArray.length; i++) {
			floatArray[i] = new Float((float)(i-1));
		}
		for(int i=0; i < doubleArray.length; i++) {
			doubleArray[i] = new Double((double)(i-1));
		}
	}

	public static Byte newByte(int n) {
		byte b = (byte)n;
		n = (int)b;
		return byteArray[n+128];
	}
	public static java.lang.Character newCharacter(int n) {
		if( n >= 0 && n <= 127 )
			return charArray[n];
		else
			return new java.lang.Character((char)n);
	}
	public static Short newShort(int n) {
		int i = n+128;
		if( i >= 0 && i <= 256 )
			return shortArray[i];
		else
			return new Short((short)n);
	}
	public static Integer newInteger(int n) {
		if( n >= -256 && n <= 256 )
			return intArray[n+256];
		else
			return new Integer(n);
	}
	public static Long newLong(long n) {
		long i = n+64;
		if( i >= 0 && i <= 128 )
			return longArray[(int)i];
		else
			return new Long(n);
	}
	public static Float newFloat(float n) {
		if( n == -1.f ) return floatArray[0];
		if( n == 0.f ) return floatArray[1];
		if( n == 1.f ) return floatArray[2];
		if( n == 2.f ) return floatArray[3];
		return new Float(n);
	}
	public static Double newDouble(double n) {
		if( n == -1.d ) return doubleArray[0];
		if( n == 0.d ) return doubleArray[1];
		if( n == 1.d ) return doubleArray[2];
		if( n == 2.d ) return doubleArray[3];
		return new Double(n);
	}

	// Scanning & parsing
	public static kiev020				k;

	public static Tree                  astTree = new Tree();
	public static Tree					mainTree = new Tree();

	public static ASTFileUnit			curASTFileUnit;
	public static Vector<Struct>		packages_scanned = new Vector<Struct>();
	public static Vector<Node>		files_scanned = new Vector<Node>();
	public static TopLevelPass			pass_no = TopLevelPass.passStartCleanup;
	public static Type					argtype = null;

	public static Hashtable<String,Node> parserAddresses = new Hashtable<String,Node>();
	private static int						parserAddrIdx;
	
	public static boolean passLessThen(TopLevelPass p) {
		return ((int)pass_no) < ((int)p);
	}
	public static boolean passGreaterEquals(TopLevelPass p) {
		return ((int)pass_no) >= ((int)p);
	}

	public static String parserAddr(Node node) {
		String addr = Integer.toHexString(++parserAddrIdx);
		while( addr.length() < 8 ) {
			addr = '0'+addr;
		}
		Kiev.parserAddresses.put(addr,node);
		return addr;
	}

	public static Node parseBlock(StringBuffer sb, int begLine, int begCol) {
		StringBufferInputStream is = new StringBufferInputStream(sb.toString());
		k.ReInit(is);
		k.reparse_body = true;
		k.reparse_pos = (begLine << 11) | (begCol & 0x3FF);
		ASTBlock body = null;
		try {
			body = k.Block();
		} finally { k.reparse_body = false; }
		return body;
	}

	public static void parseFile(FileUnit f) {
		RandomAccessFile file_input_stream;
		byte[] file_bytes;
		curFile = f.filename;
		try {
			file_input_stream = new RandomAccessFile(curFile.toString(),"r");
			file_input_stream.readFully(file_bytes = new byte[(int)file_input_stream.length()]);
		} catch( Exception e ) {
			reportError(0,e);
			return;
		}
		kiev020.interface_only = false;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(file_bytes);
			Kiev.k.ReInit(bis);
			foreach(PrescannedBody b; f.bodies; b != null ) {
				// callect parents of this block
				List<Node> pl = List.Nil;
				Node n = (Node)b.sb;
				while( n != null ) {
					pl = new List.Cons<Node>(n,pl);
					n = (Node)n.parent;
				}
				if( pl.head() != f ) {
					reportError((b.lineno <<11) | (b.columnno & 0x3FF),"Prescanned body highest parent is "+pl.head()+" but "+f+" is expected");
					continue;
				}
				foreach(Node nn; pl) {
					PassInfo.push(nn);
				}
				ASTBlock bl;
				switch(b.mode) {
				case PrescannedBody.BlockMode:
					bl = Kiev.k.PrescannedBlock(b);
					break;
				case PrescannedBody.RuleBlockMode:
					bl = Kiev.k.PrescannedRuleBlock(b);
					break;
				case PrescannedBody.CondBlockMode:
					bl = Kiev.k.PrescannedCondBlock(b);
					break;
				default:
					throw new RuntimeException("Unknown mode of prescanned block: "+b.mode);
				}
				if( !b.sb.setBody(bl) ) {
					reportError((b.lineno <<11) | (b.columnno & 0x3FF),"Prescanned body does not math");
				}
				pl = pl.reverse();
				foreach(Node nn; pl) {
					PassInfo.pop(nn);
				}
			}
		} finally {
			Kiev.k.reset();
			file_input_stream.close();
			f.bodies = PrescannedBody.emptyArray;
		}
	}


	// Extensions settings
	public static boolean javaMode			= false;
		//System.getProperties().get("java.ext.version") != null;
	static public enum Ext {
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
		EnumInt					: "enum int"		,
		Contract				: "contract"		,
		Generics				: "generics"		,
		Templates				: "templates"		,
		Wrappers				: "wrappers"		,
		Access					: "access"
	};
	
	private static boolean[] command_line_disabled_extensions	= new boolean[20];
	private static boolean[] disabled_extensions				= new boolean[20];
	
	public static boolean disabled(Ext ext) {
		int idx = (int)ext;
		return disabled_extensions[idx-1];
	}
	
	public static boolean enabled(Ext ext) {
		int idx = (int)ext;
		return !disabled_extensions[idx-1];
	}
	
	public static void check(int pos, Ext ext) {
		if (!enabled(ext))
			throw new CompilerException(pos,"Error: extension disabled, to enable use: pragma enable \""+ext+"\";");
	}
	
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
		disabled_extensions[((int)ext)-1] = false;
	}
	
	static void setExtension(boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(0,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (i < 0) {
			for (int i=0; i < disabled_extensions.length; i++) {
				command_line_disabled_extensions[i] = !enabled;
				disabled_extensions[i] = !enabled;
			}
		} else {
			command_line_disabled_extensions[i] = !enabled;
			disabled_extensions[i] = !enabled;
		}
	}
}
