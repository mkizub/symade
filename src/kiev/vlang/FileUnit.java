package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RFileUnit;
import kiev.be.java15.JFileUnit;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class FileUnit extends DNode implements Constants, ScopeOfNames, ScopeOfMethods, ScopeOfOperators {

	@virtual typedef This  = FileUnit;
	@virtual typedef VView = VFileUnit;
	@virtual typedef JView = JFileUnit;
	@virtual typedef RView = RFileUnit;

	@att public TypeNameRef		pkg;
	@att public NArr<ASTNode>	members;
	
	@ref public NArr<PrescannedBody>	bodies;
		 public final boolean[]		disabled_extensions = Kiev.getCmdLineExtSet();
		 public boolean				scanned_for_interface_only;

	@getter public FileUnit get$ctx_file_unit() { return (FileUnit)this; }
	@getter public TypeDecl get$ctx_tdecl() { return null; }
	@getter public TypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	@nodeview
	public static final view VFileUnit of FileUnit extends VDNode {
		public		TypeNameRef				pkg;
		public:ro	NArr<ASTNode>			members;
		public:ro	NArr<PrescannedBody>	bodies;
		public:ro	boolean[]				disabled_extensions;
		public		boolean					scanned_for_interface_only;

		public boolean preResolveIn() {
			foreach (Import imp; members) {
				try {
					imp.resolveImports();
				} catch(Exception e ) {
					Kiev.reportError(imp,e);
				}
			}
			return true;
		}
	}

	public FileUnit() {
		this("", Env.root);
	}
	public FileUnit(String name, Struct pkg) {
		this.id = new Symbol(name);
		this.pkg = new TypeNameRef(pkg.qname());
		this.pkg.lnk = pkg.xtype;
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies.append(b);
	}

	public String toString() { return String.valueOf(id); }

	public void resolveMetaDefaults() {
		trace(Kiev.debugResolve,"Resolving meta defaults in file "+id);
		String curr_file = Kiev.curFile;
		Kiev.curFile = id.sname;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(Struct n; members) {
				try {
					n.resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debugResolve,"Resolving meta values in file "+id);
		String curr_file = Kiev.curFile;
		Kiev.curFile = id.sname;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(Struct n; members) {
				try {
					n.resolveMetaValues();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void setPragma(ASTPragma pr) {
		foreach (ConstStringExpr e; pr.options)
			setExtension(e,pr.enable,e.value.toString());
	}

	private void setExtension(ASTNode at, boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(at,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (enabled && Kiev.getCmdLineExtSet()[i])
			Kiev.reportError(this,"Extension '"+s+"' was disabled from command line");
		disabled_extensions[i] = !enabled;
	}
	
	private boolean debugTryResolveIn(String name, String msg) {
		trace(Kiev.debugResolve,"Resolving "+name+" in "+msg);
		return true;
	}

	public rule resolveOperatorR(Operator@ op)
		ASTNode@ syn;
	{
		trace( Kiev.debugResolve, "Resolving operator: "+op+" in file "+this),
		{
			syn @= members,
			syn instanceof Opdef && ((Opdef)syn).resolved != null,
			op ?= ((Opdef)syn).resolved,
			trace( Kiev.debugResolve, "Resolved operator: "+op+" in file "+this)
		;	syn @= members,
			syn instanceof Import && ((Import)syn).mode == Import.ImportMode.IMPORT_SYNTAX,
			((Struct)((Import)syn).resolved).resolveOperatorR(op)
		}
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
		ASTNode@ syn;
	{
		syn @= members,
		{
			syn instanceof TypeDef,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			((TypeDef)syn).id.equals(name),
			node ?= ((TypeDef)syn)
		;	syn instanceof Import && !((Import)syn).star,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			((Import)syn).resolveNameR(node,path,name)
		;	syn instanceof Opdef && ((Opdef)syn).resolved != null,
			((Opdef)syn).image == name,
			node ?= ((Opdef)syn),
			trace( Kiev.debugResolve, "Resolved operator: "+syn+" in file "+this)
		}
	;
		pkg != null,
		trace( Kiev.debugResolve, "In file package: "+pkg),
		((CompaundType)pkg.getType()).clazz.resolveNameR(node,path,name)
	;
		syn @= members,
		syn instanceof Import,
		{
			((Import)syn).star,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			((Import)syn).resolveNameR(node,path,name)
		;	((Import)syn).mode == Import.ImportMode.IMPORT_SYNTAX,
			((Import)syn).resolved != null,
			((Struct)((Import)syn).resolved).resolveNameR(node,path,name)
		}
	;
		trace( Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		Env.root.resolveNameR(node,path,name)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, String name, CallType mt)
		ASTNode@ syn;
	{
		pkg != null,
		pkg.getStruct().resolveMethodR(node,path,name,mt)
	;	syn @= members,
		syn instanceof Import && ((Import)syn).mode == Import.ImportMode.IMPORT_STATIC,
		trace( Kiev.debugResolve, "In file syntax: "+syn),
		((Import)syn).resolveMethodR(node,path,name,mt)
	}

	public Dumper toJava(Dumper dmp) {
		foreach (Struct s; members)
			toJava("classes", s);
		return dmp;
	}

	public void cleanup() {
        Kiev.parserAddresses.clear();
		Kiev.k.presc = null;
		foreach(Struct n; members) ((JStruct)n).cleanup();
		bodies.delAll();
	}

	public void toJava(String output_dir) {
		String curr_file = Kiev.curFile;
		Kiev.curFile = id.sname;
		try {
			foreach (Struct n; members) {
				try {
					toJava(output_dir, n);
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; }
	}

	public void toJava(String output_dir, Struct cl) {
		if( output_dir ==null ) output_dir = "classes";
		Dumper dmp = new Dumper();
		if( cl.package_clazz != null && cl.package_clazz != Env.root ) {
			dmp.append("package ").append(cl.package_clazz.qname()).append(';').newLine();
		}
		foreach (SNode syn; members)
			dmp.append(syn);

		cl.toJavaDecl(dmp);

		try {
			File f;
			Struct jcl = cl;
			String out_file = jcl.qname().replace('.',File.separatorChar).toString();
			make_output_dir(output_dir,out_file);
			f = new File(output_dir,out_file+".java");
			FileOutputStream out;
			try {
				out = new FileOutputStream(f);
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new FileOutputStream(f);
			}
			out.write(dmp.toString().getBytes());
			out.close();
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-Java exporting: "+e);
		}
	}

	private static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}
}


