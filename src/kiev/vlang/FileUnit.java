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
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/FileUnit.java,v 1.5.2.1.2.1 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

@node
public class FileUnit extends ASTNode implements Constants, ScopeOfNames, ScopeOfMethods, ScopeOfOperators {
	@att public KString					filename = KString.Empty;
	@att public StructRef				pkg;
	@att public final NArr<ASTNode>		syntax;
	@att public final NArr<ASTNode>		members;
	
	public PrescannedBody[]				bodies = PrescannedBody.emptyArray;
	public boolean[]					disabled_extensions;

	public FileUnit() {
		this(KString.Empty, Env.root);
	}
	public FileUnit(KString name, Struct pkg) {
		super(0);
		this.filename = name;
		this.pkg = new StructRef(pkg);
		disabled_extensions = Kiev.getCmdLineExtSet();
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies = (PrescannedBody[])Arrays.append(bodies,b);
	}

	public KString getName() { return filename; }

	public String toString() { return /*getClass()+":="+*/filename.toString(); }

	public void resolveMetaDefaults() {
		trace(Kiev.debugResolve,"Resolving meta defaults in file "+filename);
		PassInfo.push(this);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(ASTNode n; members; n instanceof Struct) {
				try {
					((Struct)n).resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(n.pos,e);
				}
			}
		} finally { PassInfo.pop(this); Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debugResolve,"Resolving meta values in file "+filename);
		PassInfo.push(this);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(ASTNode n; members; n instanceof Struct) {
				try {
					((Struct)n).resolveMetaValues();
				} catch(Exception e) {
					Kiev.reportError(n.pos,e);
				}
			}
		} finally { PassInfo.pop(this); Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void setPragma(ASTPragma pr) {
		foreach (ConstStringExpr e; pr.options)
			setExtension(e.pos,pr.enable,e.value.toString());
	}

	private void setExtension(int pos, boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(pos,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (enabled && Kiev.getCmdLineExtSet()[i])
			Kiev.reportError(pos,"Extension '"+s+"' was disabled from command line");
		disabled_extensions[i] = !enabled;
	}

	public ASTNode autoProxyMethods() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(this);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - pass3()
			for(int i=0; i < members.length; i++) {
				members[i].autoProxyMethods();
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(this); Kiev.curFile = oldfn; }
		return this;
	}

	public ASTNode resolveImports() {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(this);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			for(int i=0; i < members.length; i++) {
				try {
					members[i].resolveImports();
				} catch(Exception e ) {
					Kiev.reportError/*Warning*/(members[i].getPos(),e);
				}
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(this); Kiev.curFile = oldfn; }
		return this;
	}

	public ASTNode resolveFinalFields(boolean cleanup) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = filename;
		PassInfo.push(this);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			// Process members - resolveFinalFields()
			for(int i=0; i < members.length; i++) {
				members[i].resolveFinalFields(cleanup);
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(this); Kiev.curFile = oldfn; }
		return this;
	}

	public ASTNode resolve() throws RuntimeException {
		trace(Kiev.debugResolve,"Resolving file "+filename);
		PassInfo.push(this);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			for(int i=0; i < members.length; i++) {
				try {
					members[i] = (Struct)((TopLevelDecl)members[i]).resolve(null);
				} catch(Exception e) {
					Kiev.reportError(members[i].pos,e);
				}
			}
		} finally { PassInfo.pop(this); Kiev.curFile = curr_file; Kiev.setExtSet(exts); /*RuleNode.curr = RuleNode.curr.joinUp();*/ }
		return this;
	}

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		PassInfo.push(this);
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
				if( members[i] instanceof Struct ) {
					Struct s = (Struct)members[i];
					foreach(TypeRef gen; s.gens) {
						Type t = gen.lnk;
						Type oldargtype = Kiev.argtype;
						Kiev.argtype = t;
						try {
							diff_time = curr_time = System.currentTimeMillis();
							s.generate();
							diff_time = System.currentTimeMillis() - curr_time;
							if( Kiev.verbose )
								Kiev.reportInfo("Generated clas "+t.clazz,diff_time);
						} finally {
							Kiev.argtype = oldargtype;
						}
					}
				}
			}
		} finally { PassInfo.pop(this); Kiev.curFile = cur_file; Kiev.setExtSet(exts); }
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

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name)
		ASTNode@ syn;
	{
		syn @= syntax,
		{
			syn instanceof Typedef,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			name.equals(((Typedef)syn).name),
			node ?= ((Typedef)syn).type
		;	syn instanceof Import && !((Import)syn).star,
			trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
			((Import)syn).resolveNameR(node,path,name)
		}
	;
		pkg != null,
		trace( Kiev.debugResolve, "In file package: "+pkg),
		pkg.clazz.resolveNameR(node,path,name)
	;
		syn @= syntax,
		syn instanceof Import && ((Import)syn).star,
		trace( Kiev.debugResolve, "In file syntax: "+name+" with "+syn),
		((Import)syn).resolveNameR(node,path,name)
	;
		trace( Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		Env.root.resolveNameR(node,path,name)
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, MethodType mt)
		ASTNode@ syn;
	{
		pkg != null && pkg != Env.root,
		pkg.resolveMethodR(node,path,name,mt)
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
		parent=null;
        Kiev.parserAddresses.clear();
		Kiev.curFileUnit = null;
		Kiev.k.presc = null;
		for(int i=0; i < members.length; i++)
			members[i].cleanup();
		foreach(ASTNode n; syntax) n.cleanup();
		syntax = null;
		foreach(ASTNode n; members) n.cleanup();
		members = null;
		foreach(PrescannedBody n; bodies)
			n.replaceWith(null);
		bodies = null;
	}

	public void toJava(String output_dir) {
		PassInfo.push(this);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		try {
			for(int i=0; i < members.length; i++) {
				try {
					toJava(output_dir, (Struct)members[i]);
					if( members[i] instanceof Struct ) {
						Struct s = (Struct)members[i];
						foreach(TypeRef gen; s.gens) {
							Type t = gen.lnk;
							Type oldargtype = Kiev.argtype;
							Kiev.argtype = t;
							try {
								toJava(output_dir, (Struct)members[i]);
							} finally {
								Kiev.argtype = oldargtype;
							}
						}
					}
				} catch(Exception e) {
					Kiev.reportError(members[i].pos,e);
				}
			}
		} finally { PassInfo.pop(this); Kiev.curFile = curr_file; }
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

		PassInfo.push(this);
		try {
			cl.toJavaDecl(dmp);
		} finally { PassInfo.pop(this); }

		try {
			File f;
			Struct jcl = Kiev.argtype == null? cl : (Struct)Kiev.argtype.clazz;
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

	private void dumpFile(String output_dir, String out_file, String ext, Dumper dmp, boolean patch) {
		try {
			make_output_dir(output_dir,out_file);
			File f = new File(output_dir,out_file+ext);
			File bk = new File(output_dir,out_file+ext+".bak");
			if (patch && f.exists()) {
				if (bk.exists())
					bk.delete();
				f.renameTo(bk);
			}
			BufferedWriter out;
			BufferedReader in = null;
			try {
				out = new BufferedWriter(new FileWriter(f));
				if (patch && bk.exists())
					in = new BufferedReader(new FileReader(bk));
			} catch( java.io.FileNotFoundException e ) {
				System.gc();
				System.runFinalization();
				System.gc();
				System.runFinalization();
				out = new BufferedWriter(new FileWriter(f));
				if (patch && bk.exists())
					in = new BufferedReader(new FileReader(bk));
			}
			if (patch && in != null) {
				write_patched(out,new BufferedReader(new StringReader(dmp.toString())),in);
			} else {
				out.write(dmp.toString());
			}
			out.close();
		} catch( IOException e ) {
			System.out.println("Create/write error while Kiev-to-"+ext+" exporting: "+e);
		}
	}

	private void write_patched(BufferedWriter out, BufferedReader in, BufferedReader pin) {
		Vector<String> src = new Vector<String>();
		Vector<String> patch = new Vector<String>();
		// analyze source and patch
		for (;;) {
			String line = in.readLine();
			if (line == null)
				break;
			src.append(line);
		}
		for (;;) {
			String ptch = pin.readLine();
			if (ptch == null)
				break;
			patch.append(ptch);
		}
		for (int i=0; i < src.size(); i++) {
			String line = src[i];
			out.write(line);
			out.newLine();
			if (line.trim().startsWith("//{")) {
				// need to patch!
				for (int j=0; j < patch.size(); j++) {
					if (line.equals(patch[j])) {
						patch[j] = "!!!moved";
						for(j++; !patch[j].trim().startsWith("//} end of your code"); j++) {
							out.write(patch[j]);
							out.newLine();
							patch[j] = "!!!moved";
						}
						patch[j] = "!!!moved";
					}
				}
			}
		}
		// check that we've moved all parts
		for (int i=0; i < patch.size(); i++) {
			if (patch[i].trim().startsWith("//{")) {
				if (!patch[i+1].trim().startsWith("//} end of your code")) {
					Kiev.reportWarning(0,"C++ patching - not moved chank started with\n"+patch[i]);
				}
				for (; i < patch.size(); i++) {
					if (patch[i].trim().startsWith("//} end of your code"))
						break;
				}
			}
		}
	}


	public static void toBytecode(Struct cl) {
		Struct jcl = Kiev.argtype == null? cl : (Struct)Kiev.argtype.clazz;
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
			byte[] dump = new Bytecoder(cl,null).writeClazz();
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
		if( !dir.exists() || !dir.isDirectory() ) throw new Error("Can't create output dir "+dir);
	}
}


