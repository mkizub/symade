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

import kiev.Kiev;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
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
	
	@ref public boolean				of_method;
	@ref public DNode				resolved;

	public Import() {
		this.name = new SymbolRef<DNode>();
	}

	public Import(Struct node, boolean star) {
		this.name = new SymbolRef<DNode>(node.qname());
		this.resolved = node;
		this.mode = mode;
		this.star = star;
	}

	public boolean mainResolveIn() { return false; }

	public String toString() {
		StringBuffer str = new StringBuffer("import ");
		if (mode == ImportMode.IMPORT_STATIC)  str.append("static ");
		if (mode == ImportMode.IMPORT_PACKAGE) str.append("package ");
		if (mode == ImportMode.IMPORT_SYNTAX)  str.append("syntax ");
		if (resolved instanceof Field)  str.append(resolved.getType()).append('.');
		str.append(resolved);
		if (star) str.append(".*");
		return str.toString();
	}

	public ASTNode resolveImports() {
		if (!of_method || (mode==ImportMode.IMPORT_STATIC && star)) return this;
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
		if( !PassInfo.resolveMethodR(this,v,new ResInfo(this,name.name),mt) )
			throw new CompilerException(this,"Unresolved method "+Method.toString(name.name,mt));
		DNode n = v;
		if (mode != ImportMode.IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(this,"Identifier "+name+" is not a method");
		resolved = n;
		return this;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		Struct@ s;
		DNode@ sub;
	{
		this.resolved instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && !star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		!s.isPackage(),
		{
			s.qname() == path.getName(), node ?= s.$var
		;	path.checkNodeName(s), node ?= s.$var
		}
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		{
			!s.isPackage(),
			sub @= s.sub_decls,
			path.checkNodeName(sub),
			node ?= sub.$var
		;	s.isPackage(), s.resolveNameR(node,path)
		}
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		path.isStaticAllowed(),
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveNameR(node,path),
		node instanceof Field && ((Field)node).isStatic() && ((Field)node).isPublic()
	;
		mode == ImportMode.IMPORT_SYNTAX && this.resolved instanceof Struct,
		((Struct)this.resolved).resolveNameR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && this.resolved instanceof Method,
		((Method)this.resolved).equalsByCast(path.getName(),mt,null,path),
		node ?= ((Method)this.resolved)
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveMethodR(node,path,mt),
		node instanceof Method && node.isStatic() && node.isPublic()
	}
}

@node
public final class TypeOpDef extends TypeDecl implements ScopeOfNames {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeOpDef;

	@att public ASTOperator		op;
	@att public TypeRef			type;
	@att public TypeDef			arg;

	public TypeOpDef() { super(new Symbol<This>()); }
	
	public Type getType() { return type.getType(); }
	
	public boolean mainResolveIn() { return false; }

	public boolean checkResolved() {
		return type.getType().checkResolved();
	}
	
	public Struct getStruct() {
		return getType().getStruct();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path) {
		path.space_prev == this.type,
		path.checkNodeName(this.arg),
		node ?= this.arg
	}

	public String toString() {
		return "typedef "+arg+op+" "+type+"<"+arg+">;";
	}
}

