package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java.JNode;
import kiev.be.java.JDNode;
import kiev.be.java.JFileUnit;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public final class FileUnit extends DNode implements Constants, ScopeOfNames, ScopeOfMethods, ScopeOfOperators {

	@virtual typedef NImpl = FileUnitImpl;
	@virtual typedef VView = FileUnitView;
	@virtual typedef JView = JFileUnit;

	@nodeimpl
	public static class FileUnitImpl extends DNodeImpl {
		@virtual typedef ImplOf = FileUnit;
		FileUnitImpl() {}
		@att public KString			filename;
		@att public TypeNameRef		pkg;
		@att public NArr<DNode>		syntax;
		@att public NArr<DNode>		members;
		
		@ref public NArr<PrescannedBody>	bodies;
		     public final boolean[]		disabled_extensions = Kiev.getCmdLineExtSet();
		     public boolean				scanned_for_interface_only;
	}
	@nodeview
	public static final view FileUnitView of FileUnitImpl extends DNodeView {
		public				KString					filename;
		public				TypeNameRef				pkg;
		public access:ro	NArr<DNode>				syntax;
		public access:ro	NArr<DNode>				members;
		public access:ro	NArr<PrescannedBody>	bodies;
		public access:ro	boolean[]				disabled_extensions;
		public				boolean					scanned_for_interface_only;

		@getter public FileUnit get$ctx_file_unit() { return (FileUnit)this.getNode(); }
		@getter public Struct get$ctx_clazz() { return null; }
		@getter public Struct get$child_ctx_clazz() { return null; }
		@getter public Method get$ctx_method() { return null; }
		@getter public Method get$child_ctx_method() { return null; }

		public boolean preResolveIn() {
			for(int i=0; i < members.length; i++) {
				try {
					foreach (DNode dn; syntax; dn instanceof Import) {
						((Import)dn).resolveImports();
					}
				} catch(Exception e ) {
					Kiev.reportError/*Warning*/(members[i],e);
				}
			}
			return true;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public FileUnit() {
		this(KString.Empty, Env.root);
	}
	public FileUnit(KString name, Struct pkg) {
		super(new FileUnitImpl());
		this.filename = name;
		this.pkg = new TypeNameRef(pkg.name.name);
		this.pkg.lnk = pkg.ctype;
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies.append(b);
	}

	public KString getName() { return filename; }

	public String toString() { return /*getClass()+":="+*/filename.toString(); }

	public void resolveMetaDefaults() {
		trace(Kiev.debugResolve,"Resolving meta defaults in file "+filename);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(ASTNode n; members; n instanceof Struct) {
				try {
					((Struct)n).resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debugResolve,"Resolving meta values in file "+filename);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(ASTNode n; members; n instanceof Struct) {
				try {
					((Struct)n).resolveMetaValues();
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
	
	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving file "+filename);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			for(int i=0; i < members.length; i++) {
				try {
					members[i].resolveDecl();
				} catch(Exception e) {
					Kiev.reportError(members[i],e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	private boolean debugTryResolveIn(KString name, String msg) {
		trace(Kiev.debugResolve,"Resolving "+name+" in "+msg);
		return true;
	}

	public rule resolveOperatorR(Operator@ op)
		ASTNode@ syn;
	{
		trace( Kiev.debugResolve, "Resolving operator: "+op+" in file "+this),
		{
			syn @= syntax,
			syn instanceof Opdef && ((Opdef)syn).resolved != null,
			op ?= ((Opdef)syn).resolved,
			trace( Kiev.debugResolve, "Resolved operator: "+op+" in file "+this)
		;	syn @= syntax,
			syn instanceof Import && ((Import)syn).mode == Import.ImportMode.IMPORT_SYNTAX,
			((Struct)((Import)syn).resolved).resolveOperatorR(op)
		}
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		DNode@ syn;
	{
		syn @= syntax,
		{
			syn instanceof TypeDef,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			name.equals(((TypeDef)syn).name.name),
			node ?= syn
		;	syn instanceof Import && !((Import)syn).star,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			((Import)syn).resolveNameR(node,path,name)
		;	node ?= syn,
			syn instanceof Opdef && ((Opdef)syn).resolved != null,
			trace( Kiev.debugResolve, "Resolved operator: "+syn+" in file "+this)
		}
	;
		pkg != null,
		trace( Kiev.debugResolve, "In file package: "+pkg),
		((CompaundType)pkg.getType()).clazz.resolveNameR(node,path,name)
	;
		syn @= syntax,
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

	public rule resolveMethodR(DNode@ node, ResInfo path, KString name, CallType mt)
		ASTNode@ syn;
	{
		pkg != null,
		pkg.getType().resolveCallStaticR(node,path,name,mt)
	;	syn @= syntax,
		syn instanceof Import && ((Import)syn).mode == Import.ImportMode.IMPORT_STATIC,
		trace( Kiev.debugResolve, "In file syntax: "+syn),
		((Import)syn).resolveMethodR(node,path,name,mt)
	}

	public Dumper toJava(Dumper dmp) {
		for(int i=0; i < members.length; i++)
			toJava("classes", (Struct)members[i]);
		return dmp;
	}

	public void cleanup() {
        Kiev.parserAddresses.clear();
		Kiev.k.presc = null;
		foreach(DNode n; members; n instanceof Struct) ((Struct)n).getJView().cleanup();
		bodies.delAll();
	}

	public void toJava(String output_dir) {
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		try {
			for(int i=0; i < members.length; i++) {
				try {
					toJava(output_dir, (Struct)members[i]);
				} catch(Exception e) {
					Kiev.reportError(members[i],e);
				}
			}
		} finally { Kiev.curFile = curr_file; }
	}

	public void toJava(String output_dir, Struct cl) {
		if( output_dir ==null ) output_dir = "classes";
		Dumper dmp = new Dumper();
		if( cl.package_clazz != null && cl.package_clazz != Env.root ) {
			dmp.append("package ").append(cl.package_clazz.name).append(';').newLine();
		}
		for(int j=0; j < syntax.length; j++) {
			if (syntax[j] != null) dmp.append(syntax[j]);
		}

		cl.toJavaDecl(dmp);

		try {
			File f;
			Struct jcl = cl;
			String out_file = jcl.name.bytecode_name.replace('/',File.separatorChar).toString();
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


