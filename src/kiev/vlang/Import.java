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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class Import extends SNode implements Constants, ScopeOfNames, ScopeOfMethods {
	public static final Import[] emptyArray = new Import[0];

	public enum ImportMode {
		IMPORT_CLASS,
		IMPORT_STATIC,
		IMPORT_PACKAGE,
		IMPORT_SYNTAX;
	}

	@virtual typedef This  = Import;

	@att public SymbolRef<DNode>	name;
	@att public ImportMode			mode = ImportMode.IMPORT_CLASS;
	@att public boolean				star;
	@att public TypeRef[]			args;
	@att public boolean				of_method;

	public Import() {
		this.name = new SymbolRef<DNode>();
		this.name.qualified = true;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (dump == "api" && attr.name == "this") {
			ANode p = parent();
			if (p instanceof Struct && p.isSyntax())
				return true;
			return false;
		}
		return super.includeInDump(dump, attr, val);
	}

	public boolean mainResolveIn() { return false; }

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		if (mode == ImportMode.IMPORT_STATIC)  str.append("static ");
		if (mode == ImportMode.IMPORT_PACKAGE) str.append("package ");
		if (mode == ImportMode.IMPORT_SYNTAX)  str.append("syntax ");
		str.append(name);
		if (star) str.append(".*");
		return str.toString();
	}

	public void resolveImports() {
		if (!of_method || (mode==ImportMode.IMPORT_STATIC && star))
			return;
		String name = this.name.name;
		TypeDecl scope = null;
		int dot = name.indexOf('\u001f');
		while (dot > 0) {
			String head;
			head = name.substring(0,dot).intern();
			name = name.substring(dot+1).intern();
			if (scope == null)
				scope = Env.getRoot();
			TypeDecl@ node;
			if!(scope.resolveNameR(node,new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports))) {
				Kiev.reportError(this,"Unresolved identifier "+head+" in "+scope);
				return;
			}
			scope = (TypeDecl)node;
			dot = name.indexOf('\u001f');
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
		Method@ v;
		CallType mt = new CallType(null,null,types,Type.tpAny,false);
		if( !scope.resolveMethodR(v,new ResInfo(this,name),mt) ) {
			Kiev.reportError(this,"Unresolved method "+Method.toString(name,mt)+" in "+scope);
			return;
		}
		this.name.open();
		this.name.symbol = (Method)v;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		Struct@ s;
		DNode@ sub;
	{
		this.name.dnode instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS, this.name.dnode instanceof Struct,
		{
			!star && !path.doImportStar(),
			//((Struct)this.name.dnode).checkResolved(),
			s ?= ((Struct)this.name.dnode),
			!s.isPackage(),
			path.checkNodeName(s), node ?= s.$var
		;
			star && path.doImportStar(),
			((Struct)this.name.dnode).checkResolved(),
			s ?= ((Struct)this.name.dnode),
			{
				!s.isPackage(),
				sub @= s.sub_decls,
				path.checkNodeName(sub),
				node ?= sub.$var
			;	s.isPackage(), s.resolveNameR(node,path)
			}
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
			path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
			((TypeDecl)this.name.dnode).resolveNameR(node,path),
			node instanceof Field && ((Field)node).isStatic() && !((Field)node).isPrivate()
		}
	;
		mode == ImportMode.IMPORT_SYNTAX && this.name.dnode instanceof Struct,
		((Struct)this.name.dnode).resolveNameR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && !path.doImportStar() && this.name.dnode instanceof Method,
		((Method)this.name.dnode).equalsByCast(path.getName(),mt,Type.tpVoid,path),
		node ?= ((Method)this.name.dnode)
	;
		mode == ImportMode.IMPORT_STATIC && star && path.doImportStar() && this.name.dnode instanceof TypeDecl,
		((TypeDecl)this.name.dnode).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((TypeDecl)this.name.dnode).resolveMethodR(node,path,mt),
		node instanceof Method && node.isStatic() && !node.isPrivate()
	;
		mode == ImportMode.IMPORT_SYNTAX && this.name.dnode instanceof Struct,
		((Struct)this.name.dnode).resolveMethodR(node,path,mt),
		node instanceof Method && node.isStatic() && !node.isPrivate()
	}

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "name") {
			TypeDecl scope = Env.getRoot();
			int dot = name.indexOf('\u001f');
			do {
				String head;
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
					DNode@ node;
					ResInfo info = new ResInfo(this,head,ResInfo.noForwards|ResInfo.noSuper|ResInfo.noImports);
					if !(scope.resolveNameR(node,info))
						return new DNode[0];
					if (node instanceof TypeDecl)
						scope = (TypeDecl)node;
					else
						return new DNode[0];
					dot = name.indexOf('\u001f');
				}
				if (dot < 0) {
					head = name.intern();
					Vector<DNode> vect = new Vector<DNode>();
					DNode@ node;
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

@node
public final class TypeOpDef extends TypeDecl implements ScopeOfNames {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeOpDef;

	@att public ASTOperator		op;
	@att public TypeRef			type;
	@att public TypeDef			arg;

	public TypeOpDef() { super(null); }
	
	public Type getType() { return type.getType(); }
	
//	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
//		if (dump == "api" && attr.name == "this")
//			return false;
//		return super.includeInDump(dump, attr, val);
//	}

	public boolean mainResolveIn() { return false; }

	public boolean checkResolved() {
		return type.getType().checkResolved();
	}
	
	public Struct getStruct() {
		return getType().getStruct();
	}

	public boolean hasName(String nm, boolean by_equals) {
		if (by_equals && op != null && op.ident != null && op.ident != "") {
			return ("T "+op.ident).equals(nm);
		}
		return false;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path) {
		path.space_prev == this.type,
		path.checkNodeName(this.arg),
		node ?= this.arg
	}

	public String toString() {
		return "typedef "+arg+op+" "+type+";";
	}
}

