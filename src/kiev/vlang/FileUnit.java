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
import java.util.HashSet;
import java.util.Collections;

/**
 * @author Maxim Kizub
 * @version $Revision: 295 $
 *
 */

@ThisIsANode(lang=CoreLang)
public class SyntaxScope extends SNode implements ScopeOfNames, ScopeOfMethods {
	@nodeAttr @final
	public KievPackage⇑			srpkg;
	@nodeAttr
	public ImportSyntax∅			syntaxes;
	@nodeAttr(parent="kiev·vtree·ANode.nodeattr$syntax_parent")
	public ASTNode∅					members;

	private COpdef[]						all_opdefs;

	public SyntaxScope() {
		this.srpkg.symbol = Env.getEnv().root.symbol;
		this.srpkg.qualified = true;
	}
	
	public KievPackage getPackage() {
		KievPackage td = srpkg.dnode;
		if (td != null)
			return td;
		if (srpkg.name == "") {
			td = Env.getEnv().root;
			srpkg.symbol = td.symbol;
		} else {
			SyntaxScope ss = Env.ctxSyntaxScope(parent());
			if (ss != null)
				td = ss.getPackage();
			if (td == null || td instanceof KievRoot) {
				td = Env.getEnv().newPackage(srpkg.name);
				srpkg.symbol = td.symbol;
				srpkg.qualified = true;
			} else {
				td = Env.getEnv().newPackage(td.qname() + "·" + srpkg.name);
				srpkg.symbol = td.symbol;
				srpkg.qualified = false;
			}
		}
		return td;
	}

	public rule resolveNameR(ResInfo path)
		ASTNode@ syn;
		ImportSyntax@ istx;
	{
		syn @= members,
		{
			path ?= syn
		;	syn instanceof Import && syn instanceof ScopeOfNames,
			trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
			((ScopeOfNames)syn).resolveNameR(path)
		}
	;
		path.getPrevSlotName() != "srpkg",
		trace( Kiev.debug && Kiev.debugResolve, "In namespace package: "+srpkg),
		getPackage().resolveNameR(path)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import && syn instanceof ScopeOfNames,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((ScopeOfNames)syn).resolveNameR(path)
	;
		srpkg.name != "",
		trace( Kiev.debug && Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		Env.getEnv().root.resolveNameR(path)
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
		ASTNode@ syn;
		ImportSyntax@ istx;
	{
		syn @= members,
		syn instanceof Import && syn instanceof ScopeOfMethods,
		trace( Kiev.debug && Kiev.debugResolve, "In import (no star): "+syn),
		((ScopeOfMethods)syn).resolveMethodR(path,mt)
	;
		path.enterMode(ResInfo.doImportStar) : path.leaveMode(),
		syn @= members,
		syn instanceof Import && syn instanceof ScopeOfMethods,
		trace( Kiev.debug && Kiev.debugResolve, "In import (with star): "+syn),
		((ScopeOfMethods)syn).resolveMethodR(path,mt)
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

	public AutoCompleteResult resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "srpkg") {
			KievPackage scope = Env.getEnv().root;
			SyntaxScope ss = Env.ctxSyntaxScope(parent());
			if (ss != null)
				scope = ss.getPackage();
			int dot = -1;
			if (scope instanceof KievRoot)
				dot = name.indexOf('·');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(Env.getEnv(),this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = info.resolvedDNode();
					dot = name.indexOf('·');
				}
				if (dot < 0) {
					head = name.intern();
					AutoCompleteResult result = new AutoCompleteResult(false);
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
					ResInfo info = new ResInfo(Env.getEnv(),this,head,flags);
					foreach (scope.resolveNameR(info)) {
						if (!result.containsData(info.resolvedSymbol()))
							result.append(info.resolvedSymbol());
					}
					return result;
				}
			} while (dot > 0);
		}
		return super.resolveAutoComplete(name,slot);
	}

	public boolean isOperator(String s) {
		foreach (ImportSyntax imp; syntaxes) {
			KievSyntax stx = imp.name.dnode;
			if (stx != null && stx.isOperator(s))
				return true;
		}
		return false;
	}
	
	public COpdef[] getAllOpdefs() {
		if (all_opdefs != null)
			return all_opdefs;
		Vector<COpdef> opdefs = new Vector<COpdef>();
		foreach (ImportSyntax imp; syntaxes) {
			KievSyntax stx = imp.name.dnode;
			if (stx != null)
				stx.getAllOpdefs(opdefs);
		}
		all_opdefs = opdefs.toArray();
		return all_opdefs;
		//return opdefs.toArray();
	}
	
}

// FileUnit is a node for a compilation unit stored in a file.
// 'ftype' specifies the type of the file, i.e. the kind of nodes it contains and the storage type, like
// 'text/java/1.6' - java source code (1.6)
// 'text/xml/tree-dump' or 'binary/tree-dump' - symade XML or Binary Tree Dump
// 'text/apache-ant+xml'
// and so on. The file type will be taken from ProjectSyntaxInfo when the file is created.
// Default file type is 'text/java/1.6' for *.java, 'text/xml/tree-dump' for .xml files
@ThisIsANode(name="FileUnit", lang=CoreLang)
public final class FileUnit extends SyntaxScope, CompilationUnit {

	@AttrBinDumpInfo(leading=true)
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public String							fname;
	@AttrXMLDumpInfo(attr=true, name="type")
	@nodeAttr public String							ftype;
	@AttrBinDumpInfo(ignore=true)
	@AttrXMLDumpInfo(ignore=true)
	@nodeData public ProjectSyntaxFactory		current_syntax;
	
	@UnVersioned
	public boolean							scanned_for_interface_only;
	@UnVersioned
	public boolean							loded_from_binary_dump;
	@UnVersioned
	public boolean							dont_run_backend;
	@UnVersioned
	public boolean							is_project_file;
	@UnVersioned
	public int									line_count;		// for text source files
	@UnVersioned
	public long								source_timestamp;
	@UnVersioned
	public String[]						generated_files;

	public String pname() {
		if!(parent() instanceof DirUnit)
			return fname;
		return ((DirUnit)parent()).pname() + '/' + fname;
	}
	
	public boolean isInterfaceOnly() { scanned_for_interface_only }
	public boolean isLodedFromBinaryDump() { loded_from_binary_dump }

	public static FileUnit makeFile(String qname, Project proj, boolean project_file) {
		qname = qname.replace(File.separatorChar, '/');
		DirUnit dir;
		String name;
		int end = qname.lastIndexOf('/');
		if (end < 0) {
			dir = proj.root_dir;
			name = qname;
		} else {
			dir = proj.root_dir.makeDir(qname.substring(0,end));
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

	public void addGeneratedFile(String fname) {
		if (generated_files == null) {
			generated_files = new String[] {fname};
			return;
		}
		HashSet<String> hset = new HashSet<String>();
		Collections.addAll(hset, generated_files);
		if (!hset.contains(fname)) {
			hset.add(fname);
			generated_files = hset.toArray(new String[hset.size()]);
		}
	}

	public String toString() { return fname; }

}


@ThisIsANode(name="NameSpace", lang=CoreLang)
public class NameSpace extends SyntaxScope {

	public String toString() { return "namespace "+srpkg.name; }
}


