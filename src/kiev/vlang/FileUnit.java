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
package kiev.vlang;

import kiev.fmt.ATextSyntax;

import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RFileUnit;
import kiev.be.java15.JFileUnit;
import kiev.ir.java15.RNameSpace;
import kiev.be.java15.JNameSpace;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="FileUnit", copyable=false)
public final class DirUnit extends SNode {

	@virtual typedef This  = DirUnit;

	public static final DirUnit[] emptyArray = new DirUnit[0];

	@att public String			name;
	@att public ASTNode[]		members;
	
	@setter public void set$name(String value) {
		this.name = (value == null) ? null : value.intern();
	}

	private DirUnit(String name) {
		this.name = name;
	}
	
	public String pname() {
		if (parent() instanceof DirUnit) {
			DirUnit p = (DirUnit)parent();
			if (p.name == ".")
				return name;
			return p.pname() + '/' + name;
		}
		return name;
	}
	
	private DirUnit addDir(String name) {
		DirUnit dir = new DirUnit(name);
		for (int i=0; i < members.length; i++) {
			SNode m = members[i];
			if (!(m instanceof DirUnit) || ((DirUnit)m).name.compareToIgnoreCase(name) > 0) {
				members.insert(i, dir);
				return dir;
			}
		}
		members.append(dir);
		return dir;
	}

	public FileUnit addFile(FileUnit fu) {
		for (int i=0; i < members.length; i++) {
			SNode m = members[i];
			if!(m instanceof FileUnit)
				continue;
			if (((FileUnit)m).fname.compareToIgnoreCase(fu.fname) > 0) {
				members.insert(i, fu);
				break;
			}
		}
		if (!fu.isAttached())
			members.append(fu);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			if (wt.fileEnumerator != null)
				wt.fileEnumerator.addNewFile(fu);
		}
		return fu;
	}

	public static DirUnit makeRootDir() {
		return new DirUnit(".");
	}
	public DirUnit makeDir(String qname) {
		qname = qname.replace(File.separatorChar, '/');
		DirUnit dir = this;
		int start = 0;
		int end = qname.indexOf('/', start);
		while (end > 0) {
			String nm = qname.substring(start, end).intern();
			if (nm != "") {
				DirUnit dd = null;
				foreach (DirUnit d; dir.members; d.name == nm) {
					dd = d;
					break;
				}
				if (dd == null)
					dd = dir.addDir(nm);
				dir = dd;
			}
			start = end+1;
			end = qname.indexOf('/', start);
		}
		String nm = qname.substring(start).intern();
		if (nm != "") {
			DirUnit dd = null;
			foreach (DirUnit d; dir.members; d.name == nm) {
				dd = d;
				break;
			}
			if (dd == null)
				dd = dir.addDir(nm);
			dir = dd;
		}
		return dir;
	}
	
	public Enumeration<FileUnit> enumerateAllFiles() {
		FileEnumerator fe = new FileEnumerator(this,true);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			assert (wt.fileEnumerator == null);
			wt.fileEnumerator = fe;
		}
		return fe;
	}
	
	public Enumeration<FileUnit> enumerateNewFiles() {
		FileEnumerator fe = new FileEnumerator(this,false);
		if (Thread.currentThread() instanceof WorkerThread) {
			WorkerThread wt = (WorkerThread)Thread.currentThread();
			assert (wt.fileEnumerator == null);
			wt.fileEnumerator = fe;
		}
		return fe;
	}
	
	public static class FileEnumerator implements Enumeration<FileUnit> {
		private Vector<FileUnit> files;
		private int idx;
		public boolean hasMoreElements() {
			return idx < files.length;
		}
		public FileUnit nextElement() {
			return files[idx++];
		}
		FileEnumerator(DirUnit dir, boolean all) {
			this.files = new Vector<FileUnit>();
			if (all)
				addFiles(dir);
		}
		private void addFiles(DirUnit dir) {
			foreach (FileUnit fu; dir.members)
				this.files.append(fu);
			foreach (DirUnit d; dir.members)
				addFiles(d);
		}
		void addNewFile(FileUnit fu) {
			this.files.append(fu);
		}
	}

}

@node(name="FileUnit", copyable=false)
public final class FileUnit extends NameSpace {

	@virtual typedef This  = FileUnit;
	@virtual typedef JView = JFileUnit;
	@virtual typedef RView = RFileUnit;

	public static final FileUnit[] emptyArray = new FileUnit[0];

	@att public String						fname;
	
	@ref public PrescannedBody[]			bodies;
	@ref public boolean						scanned_for_interface_only;

	public final boolean[]					disabled_extensions = Compiler.getCmdLineExtSet();
	public String							current_syntax;

	@getter public FileUnit get$ctx_file_unit() { return this; }
	@getter public NameSpace get$ctx_name_space() { return this; }
	@getter public TypeDecl get$ctx_tdecl() { return null; }
	@getter public TypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public String pname() {
		if!(parent() instanceof DirUnit)
			return fname;
		return ((DirUnit)parent()).pname() + '/' + fname;
	}
	
	public static FileUnit makeFile(String qname) {
		qname = qname.replace(File.separatorChar, '/');
		DirUnit dir;
		String name;
		int end = qname.lastIndexOf('/');
		if (end < 0) {
			dir = Env.getRoot().rdir;
			name = qname;
		} else {
			dir = Env.getRoot().rdir.makeDir(qname.substring(0,end));
			name = qname.substring(end+1);
		}
		foreach (FileUnit fu; dir.members; name.equals(fu.fname))
			return fu;
		FileUnit fu = new FileUnit(name);
		dir.addFile(fu); 
		return fu;
	}

	private FileUnit(String name) {
		this.fname = name;
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies.append(b);
	}

	public String toString() { return fname; }

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "fname")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	public void resolveMetaDefaults() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving meta defaults in file "+fname);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			super.resolveMetaDefaults();
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving meta values in file "+fname);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(pname());
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			super.resolveMetaValues();
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
	}

	public void setPragma(ASTPragma pr) {
		foreach (ConstStringExpr e; pr.options)
			setExtension(e,pr.enable,e.value.toString());
	}

	private void setExtension(ASTNode at, boolean enabled, String s) {
		KievExt ext;
		try {
			ext = KievExt.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(at,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (enabled && Compiler.getCmdLineExtSet()[i])
			Kiev.reportError(this,"Extension '"+s+"' was disabled from command line");
		disabled_extensions[i] = !enabled;
	}
	
	public rule resolveNameR(ASTNode@ node, ResInfo path)
	{
		super.resolveNameR(node, path)
	;
		srpkg.name != "",
		trace( Kiev.debug && Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		Env.getRoot().resolveNameR(node,path)
	}

	public boolean backendCleanup() {
        Kiev.parserAddresses.clear();
		Kiev.k.presc = null;
		return super.backendCleanup();
	}
}


@node(name="NameSpace")
public class NameSpace extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {

	@virtual typedef This  ≤ NameSpace;
	@virtual typedef RView ≤ RNameSpace;
	@virtual typedef JView ≤ JNameSpace;

	public static final NameSpace[] emptyArray = new NameSpace[0];

	@att public SymbolRef<TypeDecl>			srpkg;
	@att public ASTNode[]					members;
	
	@getter public FileUnit get$ctx_file_unit() { return (FileUnit)this; }
	@getter public NameSpace get$ctx_name_space() { return this; }
	@getter public TypeDecl get$ctx_tdecl() { return null; }
	@getter public TypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public NameSpace() {
		this.srpkg = new SymbolRef<KievPackage>(Env.getRoot());
	}
	
	public TypeDecl getPackage() {
		TypeDecl td = srpkg.dnode;
		if (td != null)
			return td;
		if (srpkg.name == "") {
			td = Env.getRoot();
			srpkg.open();
			srpkg.symbol = td;
		} else {
			if (parent() != null && parent().ctx_name_space != null)
				td = parent().ctx_name_space.getPackage();
			if (td == null || td instanceof Env) {
				td = Env.newPackage(srpkg.name);
				srpkg.symbol = td;
				srpkg.qualified = true;
			} else {
				td = Env.newPackage(td.qname() + "\u001f" + srpkg.name);
				srpkg.symbol = td;
				srpkg.qualified = false;
			}
		}
		return td;
	}

	public String toString() { return srpkg.name; }

	public void resolveMetaDefaults() {
		foreach(ASTNode n; members) {
			try {
				if (n instanceof NameSpace)
					n.resolveMetaDefaults();
				else if (n instanceof Struct)
					n.resolveMetaDefaults();
			} catch(Exception e) {
				Kiev.reportError(n,e);
			}
		}
	}

	public void resolveMetaValues() {
		foreach(Struct n; members) {
			try {
				if (n instanceof NameSpace)
					n.resolveMetaValues();
				else if (n instanceof Struct)
					n.resolveMetaValues();
			} catch(Exception e) {
				Kiev.reportError(n,e);
			}
		}
	}

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

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ASTNode@ syn;
	{
		syn @= members,
		{
			syn instanceof DNode && path.checkNodeName(syn),
			node ?= syn
		;	syn instanceof Import,
			trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
			((Import)syn).resolveNameR(node,path)
		;	syn instanceof Opdef && path.checkNodeName(syn),
			node ?= syn
		}
	;
		path.space_prev.pslot().name != "srpkg",
		trace( Kiev.debug && Kiev.debugResolve, "In namespace package: "+srpkg),
		getPackage().resolveNameR(node,path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveNameR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	;
		getPackage().resolveMethodR(node,path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	}

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "srpkg") {
			KievPackage scope = Env.getRoot();
			if (parent() != null && parent().ctx_name_space != null)
				scope = (KievPackage)parent().ctx_name_space.getPackage();
			int dot = -1;
			if (scope instanceof Env)
				dot = name.indexOf('\u001f');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					KievPackage@ node;
					ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
					if !(scope.resolveNameR(node,info))
						return new KievPackage[0];
					scope = (KievPackage)node;
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<KievPackage> vect = new Vector<KievPackage>();
					KievPackage@ node;
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports;
					if (!by_equals)
						flags |= ResInfo.noEquals;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(node,info)) {
						if (!vect.contains(node))
							vect.append(node);
					}
					return vect.toArray();
				}
			} while (dot > 0);
		}
		return super.findForResolve(name,slot,by_equals);
	}
}


