/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.parser.*;
import kiev.stdlib.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Import.java,v 1.5.2.1.2.1 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.1 $
 *
 */

@node
public class Import extends ASTNode implements Constants, ScopeOfNames, ScopeOfMethods {
	public static final Import[] emptyArray = new Import[0];

	public enum ImportMode {
		IMPORT_CLASS,
		IMPORT_STATIC,
		IMPORT_PACKAGE,
		IMPORT_SYNTAX;
	}

	@att public ASTIdentifier			name;
	@att public ImportMode				mode = ImportMode.IMPORT_CLASS;
         public boolean					star;
         public boolean					of_method;
	@att public final NArr<TypeRef>		args;

    @ref public ASTNode		resolved;

	public Import() {
	}

	public Import(ASTNode node, ImportMode mode, boolean star) {
		this.resolved = node;
		this.mode = mode;
		this.star = star;
	}

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
		ASTNode@ v;
		int i = 0;
		Expr[] exprs;
		if( args.length > 0 && args[0]==Type.tpRule) {
			exprs = new Expr[args.length-1];
			i++;
		} else {
			exprs = new Expr[args.length];
		}
		for(int j=0; j < exprs.length; j++,i++)
			exprs[j] = new VarAccessExpr(0,new Var(0,null,KString.Empty,args[i].getType(),0));
		if( !PassInfo.resolveMethodR(v,null,name.name,exprs,null,null) )
			throw new CompilerException(pos,"Unresolved method "+Method.toString(name.name,exprs));
		ASTNode n = v;
		if (mode != ImportMode.IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(pos,"Identifier "+name+" is not a method");
		resolved = n;
		return this;
	}

	public ASTNode resolve() throws RuntimeException {
		return this;
	}

	public void generate() {}

	public rule resolveNameR(ASTNode@ node, ResInfo path, KString name, Type tp)
		Struct@ s;
		Struct@ sub;
		ASTNode@ tmp;
	{
		this.resolved instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && !star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		!s.isPackage(),
		{
			s.name.name.equals(name), node ?= s.$var
		;	s.name.short_name.equals(name), node ?= s.$var
		}
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		{
			!s.isPackage(),
			sub @= s.sub_clazz, !sub.isArgument(),
			{
				sub.name.name.equals(name), node ?= sub.$var
			;	sub.name.short_name.equals(name), node ?= sub.$var
			}
		;	s.isPackage(), s.resolveNameR(node,path,name,tp)
		}
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		path.isStaticAllowed(),
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveNameR(node,path,name,tp),
		node instanceof Field && node.isStatic() && node.isPublic()
	;
		mode == ImportMode.IMPORT_SYNTAX && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		tmp @= ((Struct)this.resolved).imported,
		{
			tmp instanceof Field,
			trace(Kiev.debugResolve,"Syntax check field "+tmp+" == "+name),
			((Field)tmp).name.equals(name),
			node ?= tmp
		;	tmp instanceof Typedef,
			trace(Kiev.debugResolve,"Syntax check typedef "+tmp+" == "+name),
			((Typedef)tmp).name.equals(name),
			node ?= ((Typedef)tmp).type.getType()
		//;	trace(Kiev.debugResolve,"Syntax check "+tmp.getClass()+" "+tmp+" == "+name), false
		}
	}

	public rule resolveMethodR(ASTNode@ node, ResInfo path, KString name, Expr[] args, Type ret, Type type)
	{
		mode == ImportMode.IMPORT_STATIC && !star && this.resolved instanceof Method,
		((Method)this.resolved).equalsByCast(name,args,ret,type),
		node ?= ((Method)this.resolved)
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveMethodR(node,path,name,args,ret,type),
		node instanceof Method && node.isStatic() && node.isPublic()
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(this.toString()).append(";").newLine();
		return dmp;
	}

	public void cleanup() {
		parent=null;
		resolved = null;
	}

}


@node
public class Typedef extends ASTNode implements Named {

	public static Typedef[]	emptyArray = new Typedef[0];

	@att public KString		name;
	@att public TypeRef		type;
	@att public BaseStruct	typearg;

	public Typedef() {
	}
	
	public Typedef(int pos, ASTNode par, KString name) {
		super(pos,par);
		this.name = name;
	}
	
	public NodeName	getName() {
		return new NodeName(name);
	}

	public void set(ASTIdentifier id, ASTOperator op, ASTType tp) {
		typearg = Env.newMethodArgument(id.name, Env.root);
		name = op.image;

		ASTNonArrayType natp = (ASTNonArrayType)tp;
		ASTType arg = (ASTType)natp.children[1];
		KString argnm = ((ASTIdentifier)((ASTNonArrayType)arg).children[0]).toKString();
		//if (!typearg.name.short_name.equals(argnm))
		//	throw new ParseException("Typedef args "+typearg.name.short_name+" and "+type+" do not match");
		natp.children[1] = typearg.type;
		type = natp;
		return;
	}
	
	public void set(ASTType tp, ASTIdentifier id) {
		type = tp;
		name = id.name;
	}

	public String toString() {
		if (typearg != null)
			return "typedef type"+name+" "+type+"<type>;";
		else
    		return "typedef "+type+" "+name+";";
	}

	public Dumper toJava(Dumper dmp) {
    	return dmp.append("/* ").append(toString()).append(" */").newLine();
    }
}

