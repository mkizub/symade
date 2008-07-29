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
import syntax kiev.Syntax;

import java.io.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(name="FileUnit", lang=CoreLang)
public final class FileUnit extends NameSpace {

	public static final FileUnit[] emptyArray = new FileUnit[0];

	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String					fname;
	@nodeAttr public ImportSyntax∅			syntaxes;
	
	@nodeData public boolean				scanned_for_interface_only;

	public final boolean[]					disabled_extensions = Compiler.getCmdLineExtSet();
	public String							current_syntax;
	@UnVersioned
	public boolean							is_project_file;

	@getter public ComplexTypeDecl get$ctx_tdecl() { return null; }
	@getter public ComplexTypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public String pname() {
		if!(parent() instanceof DirUnit)
			return fname;
		return ((DirUnit)parent()).pname() + '/' + fname;
	}
	
	// for GUI
	public String getCurrentSyntax() { this.current_syntax }
	// for GUI
	public void setCurrentSyntax(String val) { this.current_syntax = val; }

	public static FileUnit makeFile(String qname, boolean project_file) {
		qname = qname.replace(File.separatorChar, '/');
		DirUnit dir;
		String name;
		int end = qname.lastIndexOf('/');
		if (end < 0) {
			dir = Env.getProject().root_dir;
			name = qname;
		} else {
			dir = Env.getProject().root_dir.makeDir(qname.substring(0,end));
			name = qname.substring(end+1);
		}
		foreach (FileUnit fu; dir.members; name.equals(fu.fname))
			return fu;
		FileUnit fu = new FileUnit(name, project_file);
		dir.addFile(fu);
		return fu;
	}

	public FileUnit() {}

	public FileUnit(String name, boolean project_file) {
		this.fname = name;
		this.is_project_file = project_file;
	}

	public String toString() { return fname; }

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "proj") {
			if (attr == ANode.nodeattr$this)
				return this.is_project_file;
			if (attr.name == "fname")
				return true;
			return false;
		}
		else if (attr.name == "fname")
			return false;
		return super.includeInDump(dump, attr, val);
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
		ImportSyntax@ istx;
	{
		super.resolveNameR(node, path)
	;
		srpkg.name != "",
		trace( Kiev.debug && Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		Env.getRoot().resolveNameR(node,path)
	;
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (no star): "+istx),
		istx.resolveNameR(node,path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (with star): "+istx),
		istx.resolveNameR(node,path)
	}

	public rule resolveMethodR(ISymbol@ node, ResInfo path, CallType mt)
		ImportSyntax@ istx;
	{
		super.resolveMethodR(node, path, mt)
	;
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (no star): "+istx),
		istx.resolveMethodR(node,path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (with star): "+istx),
		istx.resolveMethodR(node,path,mt)
	}

}


@ThisIsANode(name="NameSpace", lang=CoreLang)
public class NameSpace extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {

	public static final NameSpace[] emptyArray = new NameSpace[0];

	// declare NodeAttr_members to be an attribute for ANode.nodeattr$syntax_parent
	static final class NodeAttr_members extends SpaceAttAttrSlot<ASTNode> {
		public final ANode[] getArray(ANode parent) { return ((NameSpace)parent).members; }
		public final ANode[] get(ANode parent) { return ((NameSpace)parent).members; }
		public final void setArray(ANode parent, Object narr) { ((NameSpace)parent).members = (ASTNode∅)narr; }
		public final void set(ANode parent, Object narr) { ((NameSpace)parent).members = (ASTNode∅)narr; }
		NodeAttr_members(String name, TypeInfo typeinfo) {
			super(name, ANode.nodeattr$syntax_parent, typeinfo);
		}
	}

	@nodeAttr public SymbolRef<KievPackage>	srpkg;
	@nodeAttr public ASTNode∅					members;
	
	@getter public ComplexTypeDecl get$ctx_tdecl() { return null; }
	@getter public ComplexTypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public NameSpace() {
		this.srpkg = new SymbolRef<KievPackage>(Env.getRoot());
	}
	
	public KievPackage getPackage() {
		KievPackage td = srpkg.dnode;
		if (td != null)
			return td;
		if (srpkg.name == "") {
			td = Env.getRoot();
			srpkg.symbol = td;
		} else {
			if (parent() != null && parent().ctx_name_space != null)
				td = parent().ctx_name_space.getPackage();
			if (td == null || td instanceof Env) {
				td = Env.getRoot().newPackage(srpkg.name);
				srpkg.symbol = td;
				srpkg.qualified = true;
			} else {
				td = Env.getRoot().newPackage(td.qname() + "\u001f" + srpkg.name);
				srpkg.symbol = td;
				srpkg.qualified = false;
			}
		}
		return td;
	}

	public String toString() { return srpkg.name; }

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ASTNode@ syn;
	{
		syn @= members,
		{
			path.checkNodeName(syn),
			node ?= syn
		;	syn instanceof Import,
			trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
			((Import)syn).resolveNameR(node,path)
		}
	;
		path.getPrevSlotName() != "srpkg",
		trace( Kiev.debug && Kiev.debugResolve, "In namespace package: "+srpkg),
		getPackage().resolveNameR(node,path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveNameR(node,path)
	}

	public rule resolveMethodR(ISymbol@ node, ResInfo path, CallType mt)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	}

	public DNode[] resolveAutoComplete(String name, AttrSlot slot) {
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
					ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(node,info))
						return new KievPackage[0];
					scope = (KievPackage)node;
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<KievPackage> vect = new Vector<KievPackage>();
					KievPackage@ node;
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(node,info)) {
						if (!vect.contains(node))
							vect.append(node);
					}
					return vect.toArray();
				}
			} while (dot > 0);
		}
		return super.resolveAutoComplete(name,slot);
	}
}


