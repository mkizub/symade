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

package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class FileUnit extends DNode implements Constants, ScopeOfNames, ScopeOfMethods, ScopeOfOperators {

	@node
	static class FileUnitImpl extends DNodeImpl {
		FileUnitImpl() {}
		@att KString			filename;
		@att TypeNameRef		pkg;
		@att NArr<DNode>		syntax;
		@att NArr<DNode>		members;
		
		@ref NArr<PrescannedBody>	bodies;
		     final boolean[]		disabled_extensions = Kiev.getCmdLineExtSet();
		     boolean				scanned_for_interface_only;
	}
	@nodeview
	static class FileUnitView extends DNodeView {
		final FileUnitImpl impl;
		FileUnitView(FileUnitImpl impl) {
			super(impl);
			this.impl = impl;
		}
		@getter public final KString				get$filename()	{ return this.impl.filename; }
		@getter public final TypeNameRef			get$pkg()		{ return this.impl.pkg; }
		@getter public final NArr<DNode>			get$syntax()	{ return this.impl.syntax; }
		@getter public final NArr<DNode>			get$members()	{ return this.impl.members; }
		@getter public final NArr<PrescannedBody>	get$bodies()	{ return this.impl.bodies; }
		@getter public final boolean[]				get$disabled_extensions()			{ return this.impl.disabled_extensions; }
		@getter public final boolean				get$scanned_for_interface_only()	{ return this.impl.scanned_for_interface_only; }

		@setter public final void set$filename(KString val)					{ this.impl.filename = val; }
		@setter public final void set$pkg(TypeNameRef val)						{ this.impl.pkg = val; }
		@setter public final void set$scanned_for_interface_only(boolean val)	{ this.impl.scanned_for_interface_only = val; }
	}
	public NodeView			getNodeView()		{ return new FileUnitView((FileUnitImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new FileUnitView((FileUnitImpl)this.$v_impl); }
	public FileUnitView		getFileUnitView()	{ return new FileUnitView((FileUnitImpl)this.$v_impl); }

	@att public abstract virtual 				KString					filename;
	@att public abstract virtual				TypeNameRef				pkg;
	@att public abstract virtual access:ro		NArr<DNode>				syntax;
	@att public abstract virtual access:ro		NArr<DNode>				members;
	
	@ref public abstract virtual access:ro		NArr<PrescannedBody>	bodies;
	     public abstract virtual access:ro		boolean[]				disabled_extensions;
	     public abstract virtual				boolean					scanned_for_interface_only;

	@getter public KString					get$filename()	{ return this.getFileUnitView().get$filename(); }
	@getter public TypeNameRef				get$pkg()		{ return this.getFileUnitView().get$pkg(); }
	@getter public NArr<DNode>				get$syntax()	{ return this.getFileUnitView().get$syntax(); }
	@getter public NArr<DNode>				get$members()	{ return this.getFileUnitView().get$members(); }
	@getter public NArr<PrescannedBody>	get$bodies()	{ return this.getFileUnitView().get$bodies(); }
	@getter public boolean[]				get$disabled_extensions()			{ return this.getFileUnitView().get$disabled_extensions(); }
	@getter public boolean					get$scanned_for_interface_only()	{ return this.getFileUnitView().get$scanned_for_interface_only(); }

	@setter public void set$filename(KString val)						{ this.getFileUnitView().set$filename(val); }
	@setter public void set$pkg(TypeNameRef val)						{ this.getFileUnitView().set$pkg(val); }
	@setter public void set$scanned_for_interface_only(boolean val)	{ this.getFileUnitView().set$scanned_for_interface_only(val); }

	public FileUnit() {
		this(KString.Empty, Env.root);
	}
	public FileUnit(KString name, Struct pkg) {
		super(new FileUnitImpl());
		this.filename = name;
		this.pkg = new TypeNameRef(pkg.name.name);
		this.pkg.lnk = pkg.type;
	}

	public void setupContext() {
		pctx = new NodeContext(this);
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

	public boolean preResolveIn(TransfProcessor proc) {
		this.resolveImports();
		return true;
	}
	
	private void resolveImports() {
		for(int i=0; i < members.length; i++) {
			try {
				foreach (DNode dn; syntax; dn instanceof Import) {
					((Import)dn).resolveImports();
				}
				foreach (DNode dn; members; dn instanceof Struct) {
					((Struct)dn).resolveImports();
				}
			} catch(Exception e ) {
				Kiev.reportError/*Warning*/(members[i],e);
			}
		}
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

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		KString cur_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			for(int i=0; i < members.length; i++) {
				diff_time = curr_time = System.currentTimeMillis();
				((Struct)members[i]).generate();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose )
					Kiev.reportInfo("Generated clas "+members[i],diff_time);
			}
		} finally { Kiev.curFile = cur_file; Kiev.setExtSet(exts); }
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
			syn instanceof TypeDefOp,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			name.equals(((TypeDefOp)syn).name),
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
		((BaseType)pkg.getType()).clazz.resolveNameR(node,path,name)
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

	public rule resolveMethodR(DNode@ node, ResInfo path, KString name, MethodType mt)
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
		foreach(DNode n; members; n instanceof Struct) ((Struct)n).cleanup();
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

	public static void toBytecode(Struct cl, ConstPool constPool) {
		Struct jcl = cl;
		String output_dir = Kiev.output_dir;
		if( output_dir == null ) output_dir = Kiev.javaMode ? "." : "classes";
		String out_file;
		if( Kiev.javaMode && output_dir == null )
			out_file = cl.name.short_name.toString();
		else if( cl.isPackage() )
			out_file = (cl.name.bytecode_name+"/package").replace('/',File.separatorChar);
		else
			out_file = jcl.name.bytecode_name.replace('/',File.separatorChar).toString();
		try {
			DataOutputStream out;
			make_output_dir(output_dir,out_file);
			try {
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new DataOutputStream(new FileOutputStream(new File(output_dir,out_file+".class")));
			}
			byte[] dump = new Bytecoder(cl,null,constPool).writeClazz();
			out.write(dump);
			out.close();
//			if( Kiev.verbose ) System.out.println("[Wrote bytecode for class "+cl+"]");
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-JavaBytecode exporting: "+e);
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


