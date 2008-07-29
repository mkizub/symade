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

@ThisIsANode(lang=CoreLang)
public class Import extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {
	public static final Import[] emptyArray = new Import[0];

	public enum ImportMode {
		IMPORT_CLASS,
		IMPORT_STATIC;
	}

	@nodeAttr public SymbolRef<DNode>	name;

	@nodeAttr public ImportMode			mode = ImportMode.IMPORT_CLASS;

	@AttrXMLDumpInfo(attr=true, name="all")
	@nodeAttr public boolean			star;

	@nodeAttr public TypeRef∅			args;

	@AttrXMLDumpInfo(attr=true, name="methods")
	@nodeAttr public boolean			of_method;

	public Import() {
		this.name = new SymbolRef<DNode>();
		this.name.qualified = true;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this") {
			ANode p = parent();
			if (p instanceof KievSyntax)
				return true;
			return false;
		}
		return super.includeInDump(dump, attr, val);
	}

	public boolean mainResolveIn() { return false; }

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		if (mode == ImportMode.IMPORT_STATIC)  str.append("static ");
		str.append(name);
		if (star) str.append(".*");
		return str.toString();
	}

	public boolean preResolveIn() {
		if (!of_method || (mode==ImportMode.IMPORT_STATIC && star))
			return false;
		String name = this.name.name;
		ScopeOfNames scope = null;
		int dot = name.indexOf('\u001f');
		while (dot > 0) {
			String head;
			head = name.substring(0,dot).intern();
			name = name.substring(dot+1).intern();
			if (scope == null)
				scope = (ScopeOfNames)Env.getRoot();
			DNode@ node;
			if!(scope.resolveNameR(node,new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext))) {
				Kiev.reportError(this,"Unresolved identifier "+head+" in "+scope);
				return false;
			}
			scope = (ScopeOfNames)node;
			dot = name.indexOf('\u001f');
		}
		if !(scope instanceof ScopeOfMethods) {
			Kiev.reportError(this,"Scope "+scope+" has no methods");
			return false;
		}
		
		int i = 0;
		Type[] types;
		if( args.length > 0 && args[0].getType() ≡ Type.tpRule) {
			types = new Type[args.length-1];
			i++;
		} else {
			types = new Type[args.length];
		}
		for(int j=0; j < types.length; j++,i++)
			types[j] = args[i].getType();
		ISymbol@ v;
		CallType mt = new CallType(null,null,types,Type.tpAny,false);
		if( !((ScopeOfMethods)scope).resolveMethodR(v,new ResInfo(this,name),mt) ) {
			Kiev.reportError(this,"Unresolved method "+Method.toString(name,mt)+" in "+scope);
			return false;
		}
		this.name.symbol = (ISymbol)v;
		return false;
	}

	public rule resolveNameR(ISymbol@ node, ResInfo path)
		DNode@ sub;
	{
		this.name.dnode instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS, this.name.dnode instanceof ComplexTypeDecl,
		{
			!star && !path.doImportStar(),
			path.checkNodeName((ComplexTypeDecl)this.name.dnode),
			node ?= (ComplexTypeDecl)this.name.dnode
		;
			star && path.doImportStar(),
			((ComplexTypeDecl)this.name.dnode).checkResolved(),
			sub @= ((ComplexTypeDecl)this.name.dnode).members,
			path.checkNodeName(sub),
			node ?= sub.$var
		}
	;
		mode == ImportMode.IMPORT_CLASS, this.name.dnode instanceof KievPackage,
		{
			!star && !path.doImportStar(),
			path.checkNodeName((KievPackage)this.name.dnode),
			node ?= (KievPackage)this.name.dnode
		;
			star && path.doImportStar(),
			((KievPackage)this.name.dnode).resolveNameR(node,path)
		}
	;
		mode == ImportMode.IMPORT_STATIC,
		{
			!(this.name.dnode instanceof TypeDecl),
			!star && !path.doImportStar(),
			node ?= this.name.dnode,
			path.checkNodeName(node)
		;
			this.name.dnode instanceof TypeDecl,
			star && path.doImportStar(),
			path.isStaticAllowed(),
			((TypeDecl)this.name.dnode).checkResolved(),
			path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
			((TypeDecl)this.name.dnode).resolveNameR(node,path),
			node instanceof Field && ((Field)node).isStatic() && !((Field)node).isPrivate()
		}
	}

	public rule resolveMethodR(ISymbol@ node, ResInfo path, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && !path.doImportStar() && this.name.dnode instanceof Method,
		node ?= ((Method)this.name.dnode).equalsByCast(path.getName(),mt,Type.tpVoid,path)
	;
		mode == ImportMode.IMPORT_STATIC && star && path.doImportStar() && this.name.dnode instanceof TypeDecl,
		((TypeDecl)this.name.dnode).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		((TypeDecl)this.name.dnode).resolveMethodR(node,path,mt),
		node.dnode.isStatic() && !node.dnode.isPrivate()
	}

	public ISymbol[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getRoot();
			int dot = name.indexOf('\u001f');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					KievPackage@ node;
					ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(node,info))
						return new DNode[0];
					scope = (ScopeOfNames)node;
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<DNode> vect = new Vector<DNode>();
					DNode@ node;
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

@ThisIsANode(lang=CoreLang)
public class ImportSyntax extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {

	public static final ImportSyntax[] emptyArray = new ImportSyntax[0];

	@nodeAttr public SymbolRef<KievSyntax>		name;

	public ImportSyntax() {
		this.name = new SymbolRef<KievSyntax>();
		this.name.qualified = true;
	}

	public boolean preResolveIn() { false }
	public boolean mainResolveIn() { false }

	public String toString() {
		return "import syntax "+name;
	}

	public rule resolveNameR(ISymbol@ node, ResInfo path)
		DNode@ sub;
	{
		this.name.dnode instanceof KievSyntax,
		this.name.dnode.resolveNameR(node,path)
	}

	public rule resolveMethodR(ISymbol@ node, ResInfo path, CallType mt)
	{
		this.name.dnode instanceof KievSyntax,
		this.name.dnode.resolveMethodR(node,path,mt),
		node.dnode.isStatic() && !node.dnode.isPrivate()
	}

	public ISymbol[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getRoot();
			int dot = name.indexOf('\u001f');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					KievPackage@ node;
					ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(node,info))
						return new DNode[0];
					scope = (ScopeOfNames)node;
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<KievSyntax> vect = new Vector<KievSyntax>();
					KievSyntax@ node;
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

@ThisIsANode(lang=CoreLang)
public final class TypeOpDef extends DNode implements ScopeOfNames {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public TypeDef			arg;
	@nodeAttr public TypeRef			dtype;
	
	@abstract
	@nodeData public String				op;
	
	@getter public String get$op() {
		String sname = this.sname;
		if (sname == null || sname.startsWith("T "))
			return null;
		return sname.substring(2);
	}
	@setter public void set$op(String val) {
		if (val == null)
			this.sname = null;
		else
			this.sname = "T "+val;
	}
	
	public TypeOpDef() {}
	
	public Type getType() { return dtype.getType(); }
	
	public boolean mainResolveIn() { return false; }

	public void checkResolved() { dtype.getType().checkResolved(); }
	
	public Struct getStruct() {
		return getType().getStruct();
	}
	
	public rule resolveNameR(ISymbol@ node, ResInfo path) {
		path.space_prev == this.dtype,
		path.checkNodeName(this.arg),
		node ?= this.arg
	}

	public String toString() {
		return "typedef "+arg+op+" "+dtype+";";
	}
}

