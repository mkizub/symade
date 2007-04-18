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

import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RFileUnit;
import kiev.be.java15.JFileUnit;
import kiev.be.java15.JStruct;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="FileUnit")
public final class FileUnit extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {

	@virtual typedef This  = FileUnit;
	@virtual typedef JView = JFileUnit;
	@virtual typedef RView = RFileUnit;

	public static final FileUnit[] emptyArray = new FileUnit[0];

	@att public String			name;
	@att public TypeNameRef		pkg;
	@att public ASTNode[]		members;
	
	@ref public PrescannedBody[]	bodies;
		 public final boolean[]		disabled_extensions = Compiler.getCmdLineExtSet();
		 public boolean				scanned_for_interface_only;

	@getter public FileUnit get$ctx_file_unit() { return (FileUnit)this; }
	@getter public TypeDecl get$ctx_tdecl() { return null; }
	@getter public TypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public FileUnit() {
		this("", null);
	}
	public FileUnit(String name, Struct pkg) {
		this.name = name;
		if (pkg != null) {
			this.pkg = new TypeNameRef(pkg.qname());
			this.pkg.lnk = pkg.xtype;
		}
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies.append(b);
	}

	public String toString() { return name; }

	public void resolveMetaDefaults() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving meta defaults in file "+name);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(name);
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
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving meta values in file "+name);
		String curr_file = Kiev.getCurFile();
		Kiev.setCurFile(name);
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
		} finally { Kiev.setCurFile(curr_file); Kiev.setExtSet(exts); }
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
	
	private boolean debugTryResolveIn(String name, String msg) {
		trace(Kiev.debug && Kiev.debugResolve,"Resolving "+name+" in "+msg);
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
		pkg != null && path.space_prev.pslot().name != "pkg",
		trace( Kiev.debug && Kiev.debugResolve, "In file package: "+pkg),
		((CompaundType)pkg.getType()).tdecl.resolveNameR(node,path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveNameR(node,path)
	;
		trace( Kiev.debug && Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		Env.root.resolveNameR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	;	pkg != null,
		pkg.getStruct().resolveMethodR(node,path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	}

	public boolean backendCleanup() {
        Kiev.parserAddresses.clear();
		Kiev.k.presc = null;
		return super.backendCleanup();
	}
}


