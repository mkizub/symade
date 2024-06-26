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
 * @version $Revision: 296 $
 *
 */

@ThisIsANode(lang=CoreLang)
public abstract class Import extends SNode implements Constants {

	@nodeAttr public final DNode⇑	name;

	public Import() {
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

	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { return false; }

	public AutoCompleteResult resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getEnv().root;
			int dot = name.indexOf('·');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(Env.getEnv(),this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = (ScopeOfNames)info.resolvedDNode();
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

}

@ThisIsANode(lang=CoreLang)
public class ImportImpl extends Import implements ScopeOfNames {

	@AttrXMLDumpInfo(attr=true, name="all")
	@nodeAttr public boolean			star;

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		str.append(name);
		if (star) str.append(".*");
		return str.toString();
	}

	public rule resolveNameR(ResInfo path)
		DNode@ sub;
	{
		this.name.dnode instanceof ComplexTypeDecl,
		{
			!star && !path.doImportStar(),
			path ?= (ComplexTypeDecl)this.name.dnode
		;
			star && path.doImportStar(),
			((ComplexTypeDecl)this.name.dnode).checkResolved(path.env),
			path @= ((ComplexTypeDecl)this.name.dnode).members
		}
	;
		this.name.dnode instanceof KievPackage,
		{
			!star && !path.doImportStar(),
			path ?= (KievPackage)this.name.dnode
		;
			star && path.doImportStar(),
			((KievPackage)this.name.dnode).resolveNameR(path)
		}
	}
}

@ThisIsANode(lang=CoreLang)
public class ImportMethod extends Import implements Constants, ScopeOfMethods {

	@nodeAttr public final Method⇑		method;
	@nodeAttr public TypeRef∅			args;

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		str.append(name);
		str.append(".");
		str.append(method);
		str.append("(");
		foreach (TypeRef tr; args)
			str.append(tr);
		str.append(")");
		return str.toString();
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) {
		DNode dn = name.dnode;
		if !(dn instanceof ScopeOfMethods) {
			Kiev.reportError(this,"Scope "+dn+" has no methods");
			return false;
		}
		
		int i = 0;
		Type[] types;
		if( args.length > 0 && args[0].getType(env) ≡ env.tenv.tpRule) {
			types = new Type[args.length-1];
			i++;
		} else {
			types = new Type[args.length];
		}
		for(int j=0; j < types.length; j++,i++)
			types[j] = args[i].getType(env);
		CallType mt = new CallType(null,null,types,env.tenv.tpAny,false);
		ResInfo<Method> info = new ResInfo<Method>(env,this,method.name);
		if( !((ScopeOfMethods)dn).resolveMethodR(info,mt) ) {
			Kiev.reportError(this,"Unresolved method "+Method.toString(method.name,mt)+" in "+dn);
			return false;
		}
		this.method.symbol = info.resolvedSymbol();
		return false;
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
	{
		path ?= this.method.dnode.equalsByCast(path.getName(),mt,path.env.tenv.tpVoid,path)
	}

}

@ThisIsANode(lang=CoreLang)
public class ImportStatic extends Import implements ScopeOfNames, ScopeOfMethods {

	@AttrXMLDumpInfo(attr=true, name="all")
	@nodeAttr public boolean			star;

	public String toString() {
		StringBuffer str = new StringBuffer("import static ");
		str.append(name);
		if (star) str.append(".*");
		return str.toString();
	}

	public rule resolveNameR(ResInfo path)
		DNode@ sub;
	{
		!(this.name.dnode instanceof TypeDecl),
		!star && !path.doImportStar(),
		path ?= this.name.dnode
	;
		this.name.dnode instanceof TypeDecl,
		star && path.doImportStar(),
		path.isStaticAllowed(),
		((TypeDecl)this.name.dnode).checkResolved(path.env),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		((TypeDecl)this.name.dnode).resolveNameR(path),
		path.resolvedDNode() instanceof Field && path.resolvedDNode().isStatic() && !path.resolvedDNode().isPrivate()
	}

	public rule resolveMethodR(ResInfo path, CallType mt)
	{
		!star && !path.doImportStar() && this.name.dnode instanceof Method,
		path ?= ((Method)this.name.dnode).equalsByCast(path.getName(),mt,path.env.tenv.tpVoid,path)
	;
		star && path.doImportStar() && this.name.dnode instanceof TypeDecl,
		((TypeDecl)this.name.dnode).checkResolved(path.env),
		path.enterMode(ResInfo.noForwards|ResInfo.noSyntaxContext) : path.leaveMode(),
		((TypeDecl)this.name.dnode).resolveMethodR(path,mt),
		path.resolvedDNode().isStatic() && !path.resolvedDNode().isPrivate()
	}
}

@ThisIsANode(lang=CoreLang)
public class ImportOperators extends Import {

	public String toString() {
		StringBuffer str = new StringBuffer("import operators ");
		str.append(name);
		return str.toString();
	}

}

@ThisIsANode(lang=CoreLang)
public class ImportSyntax extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {

	public static final ImportSyntax[] emptyArray = new ImportSyntax[0];

	@nodeAttr public final KievSyntax⇑		name;

	public ImportSyntax() {
		this.name.qualified = true;
	}

	public boolean preResolveIn(Env env, INode parent, AttrSlot slot) { false }
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { false }

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

	public AutoCompleteResult resolveAutoComplete(String name, AttrSlot slot) {
		if (slot.name == "name") {
			ScopeOfNames scope = (ScopeOfNames)Env.getEnv().root;
			int dot = name.indexOf('·');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					ResInfo<KievPackage> info = new ResInfo<KievPackage>(Env.getEnv(),this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext);
					if !(scope.resolveNameR(info))
						return null;
					scope = (ScopeOfNames)info.resolvedDNode();
					dot = name.indexOf('·');
				}
				if (dot < 0) {
					head = name.intern();
					AutoCompleteResult result = new AutoCompleteResult(false);
					int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
					ResInfo<KievSyntax> info = new ResInfo<KievSyntax>(Env.getEnv(),this,head,flags);
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

}

@ThisIsANode(lang=CoreLang)
public final class TypeOpDef extends DNode implements ScopeOfNames {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@nodeAttr public TypeRef			dtype;
	
	@AttrBinDumpInfo(ignore=true)
	@abstract
	@nodeData public String				op;
	
	@getter public String get$op() {
		String sname = this.sname;
		if (sname == null || !sname.startsWith("T "))
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
	
	public Type getType(Env env) { return dtype.getType(env); }
	
	public boolean mainResolveIn(Env env, INode parent, AttrSlot slot) { return false; }

	public void checkResolved(Env env) { dtype.getType(env).checkResolved(); }
	
	public Struct getStruct() {
		return getType(Env.getEnv()).getStruct();
	}
	
	public rule resolveNameR(ResInfo path) {
		path.getPrevNode() == this.dtype,
		path ?= path.env.tenv.tpTypeOpDefArg.definer
	}

	public String toString() {
		return "typedef _oparg_ "+op+" "+dtype+";";
	}
}

