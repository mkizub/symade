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
		int dot = name.indexOf('·');
		while (dot > 0) {
			String head;
			head = name.substring(0,dot).intern();
			name = name.substring(dot+1).intern();
			if (scope == null)
				scope = (ScopeOfNames)Env.getRoot();
			ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
			if!(scope.resolveNameR(info)) {
				Kiev.reportError(this,"Unresolved identifier "+head+" in "+scope);
				return false;
			}
			scope = (ScopeOfNames)info.resolvedDNode();
			dot = name.indexOf('·');
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
		CallType mt = new CallType(null,null,types,Type.tpAny,false);
		ResInfo<Method> info = new ResInfo<Method>(this,name);
		if( !((ScopeOfMethods)scope).resolveMethodR(info,mt) ) {
			Kiev.reportError(this,"Unresolved method "+Method.toString(name,mt)+" in "+scope);
			return false;
		}
		this.name.symbol = info.resolvedSymbol();
		return false;
	}

	public rule resolveNameR(ResInfo path)
		DNode@ sub;
	{
		this.name.dnode instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS, this.name.dnode instanceof ComplexTypeDecl,
		{
			!star && !path.doImportStar(),
			path ?= (ComplexTypeDecl)this.name.dnode
		;
			star && path.doImportStar(),
			((ComplexTypeDecl)this.name.dnode).checkResolved(),
			path @= ((ComplexTypeDecl)this.name.dnode).members
		}
	;
		mode == ImportMode.IMPORT_CLASS, this.name.dnode instanceof KievPackage,
		{
			!star && !path.doImportStar(),
			path ?= (KievPackage)this.name.dnode
		;
			star && path.doImportStar(),
			((KievPackage)this.name.dnode).resolveNameR(path)
		}
	;
		mode == ImportMode.IMPORT_STATIC,
		{
			!(this.name.dnode instanceof TypeDecl),
			!star && !path.doImportStar(),
			path ?= this.name.dnode
		;
			this.name.dnode instanceof TypeDecl,
			star && path.doImportStar(),
			path.isStaticAllowed(),
			((TypeDecl)this.name.dnode).checkResolved(),
			path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
			((TypeDecl)this.name.dnode).resolveNameR(path),
			path.resolvedDNode() instanceof Field && path.resolvedDNode().isStatic() && !path.resolvedDNode().isPrivate()
		}
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && !path.doImportStar() && this.name.dnode instanceof Method,
		path ?= ((Method)this.name.dnode).equalsByCast(path.getName(),mt,Type.tpVoid,path)
	;
		mode == ImportMode.IMPORT_STATIC && star && path.doImportStar() && this.name.dnode instanceof TypeDecl,
		((TypeDecl)this.name.dnode).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		((TypeDecl)this.name.dnode).resolveMethodR(path,mt),
		path.resolvedDNode().isStatic() && !path.resolvedDNode().isPrivate()
	}

	public Symbol[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getRoot();
			int dot = name.indexOf('·');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = (ScopeOfNames)info.resolvedDNode();
					dot = name.indexOf('·');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<Symbol> vect = new Vector<Symbol>();
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

	public rule resolveNameR(ResInfo path)
	{
		this.name.dnode instanceof KievSyntax,
		this.name.dnode.resolveNameR(path)
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
	{
		this.name.dnode instanceof KievSyntax,
		this.name.dnode.resolveMethodR(path,mt),
		path.resolvedDNode().isStatic() && !path.resolvedDNode().isPrivate()
	}

	public Symbol[] resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getRoot();
			int dot = name.indexOf('·');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = (ScopeOfNames)info.resolvedDNode();
					dot = name.indexOf('·');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<Symbol> vect = new Vector<Symbol>();
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
					ResInfo<KievSyntax> info = new ResInfo<KievSyntax>(this,head,flags);
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
	
	public rule resolveNameR(ResInfo path) {
		path.space_prev == this.dtype,
		path ?= this.arg
	}

	public String toString() {
		return "typedef "+arg+op+" "+dtype+";";
	}
}

