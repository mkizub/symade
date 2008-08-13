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
public final class FileUnit extends NameSpace, CompilationUnit {

	public static final FileUnit[] emptyArray = new FileUnit[0];

	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String					fname;
	@nodeAttr public ImportSyntax∅			syntaxes;
	
	public boolean							scanned_for_interface_only;
	public final boolean[]					disabled_extensions = Compiler.getCmdLineExtSet();
	public String							current_syntax;
	@UnVersioned
	public boolean							is_project_file;
	@UnVersioned
	public int								line_count;		// for text source files

	@getter public ComplexTypeDecl get$ctx_tdecl() { return null; }
	public ComplexTypeDecl get_child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	public Method get_child_ctx_method() { return null; }

	public String pname() {
		if!(parent() instanceof DirUnit)
			return fname;
		return ((DirUnit)parent()).pname() + '/' + fname;
	}
	
	// for GUI
	public String getCurrentSyntax() { this.current_syntax }
	// for GUI
	public void setCurrentSyntax(String val) { this.current_syntax = val; }

	public boolean isInterfaceOnly() { scanned_for_interface_only }

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

	private FileUnit(String name, boolean project_file) {
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
	
	public rule resolveNameR(ResInfo path)
		ImportSyntax@ istx;
	{
		super.resolveNameR(path)
	;
		srpkg.name != "",
		trace( Kiev.debug && Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		Env.getRoot().resolveNameR(path)
	;
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (no star): "+istx),
		istx.resolveNameR(path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (with star): "+istx),
		istx.resolveNameR(path)
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
		ImportSyntax@ istx;
	{
		super.resolveMethodR(path,mt)
	;
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (no star): "+istx),
		istx.resolveMethodR(path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		istx @= syntaxes,
		trace( Kiev.debug && Kiev.debugResolve, "In syntax (with star): "+istx),
		istx.resolveMethodR(path,mt)
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

	@nodeAttr public final KievPackage⇑				srpkg;
	@nodeAttr public       ASTNode∅					members;
	
	@getter public ComplexTypeDecl get$ctx_tdecl() { return null; }
	public ComplexTypeDecl get_child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	public Method get_child_ctx_method() { return null; }

	public NameSpace() {
		this.srpkg.symbol = Env.getRoot().symbol;
		this.srpkg.qualified = true;
	}
	
	public KievPackage getPackage() {
		KievPackage td = srpkg.dnode;
		if (td != null)
			return td;
		if (srpkg.name == "") {
			td = Env.getRoot();
			srpkg.symbol = td.symbol;
		} else {
			if (parent() != null && parent().ctx_name_space != null)
				td = parent().ctx_name_space.getPackage();
			if (td == null || td instanceof Env) {
				td = Env.getRoot().newPackage(srpkg.name);
				srpkg.symbol = td.symbol;
				srpkg.qualified = true;
			} else {
				td = Env.getRoot().newPackage(td.qname() + "\u001f" + srpkg.name);
				srpkg.symbol = td.symbol;
				srpkg.qualified = false;
			}
		}
		return td;
	}

	public String toString() { return srpkg.name; }

	public rule resolveNameR(ResInfo path)
		ASTNode@ syn;
	{
		syn @= members,
		{
			path ?= syn
		;	syn instanceof Import,
			trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
			((Import)syn).resolveNameR(path)
		}
	;
		path.getPrevSlotName() != "srpkg",
		trace( Kiev.debug && Kiev.debugResolve, "In namespace package: "+srpkg),
		getPackage().resolveNameR(path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveNameR(path)
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
		ASTNode@ syn;
	{
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
		((Import)syn).resolveMethodR(path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((Import)syn).resolveMethodR(path,mt)
	}

	public ISymbol[] resolveAutoComplete(String name, AttrSlot slot) {
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
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = info.resolvedDNode();
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<ISymbol> vect = new Vector<ISymbol>();
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(info)) {
						if (!vect.contains(info.resolvedSymbol()))
							vect.append(info.resolvedSymbol());
					}
					return vect.toArray();
				}
			} while (dot > 0);
		}
		return super.resolveAutoComplete(name,slot);
	}
}


