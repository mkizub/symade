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
import kiev.transf.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class Import extends DNode implements Constants, ScopeOfNames, ScopeOfMethods {
	public static final Import[] emptyArray = new Import[0];

	public enum ImportMode {
		IMPORT_CLASS,
		IMPORT_STATIC,
		IMPORT_PACKAGE,
		IMPORT_SYNTAX;
	}

	@node
	static final class ImportImpl extends DNodeImpl {
		ImportImpl() {}
		@att NameRef				name;
		@att ImportMode				mode = ImportMode.IMPORT_CLASS;
		@att boolean				star;
		@att NArr<TypeRef>			args;
		
		@ref boolean				of_method;
		@ref DNode					resolved;
	}
	@nodeview
	static final view ImportView of ImportImpl extends DNodeView {
		public				NameRef				name;
		public				ImportMode			mode;
		public				boolean				star;
		public access:ro	NArr<TypeRef>		args;
		public				boolean				of_method;
		public				DNode				resolved;
	}
	public NodeView			getNodeView()		{ return new ImportView((ImportImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new ImportView((ImportImpl)this.$v_impl); }
	public ImportView		getImportView()		{ return new ImportView((ImportImpl)this.$v_impl); }

	@att public abstract virtual				NameRef					name;
	@att public abstract virtual				ImportMode				mode;
	@att public abstract virtual				boolean					star;
	@att public abstract virtual access:ro		NArr<TypeRef>			args;

	@ref public abstract virtual				boolean					of_method;
	@ref public abstract virtual				DNode					resolved;

	@getter public NameRef				get$name()	{ return this.getImportView().name; }
	@getter public ImportMode			get$mode()	{ return this.getImportView().mode; }
	@getter public boolean				get$star()	{ return this.getImportView().star; }
	@getter public NArr<TypeRef>		get$args()	{ return this.getImportView().args; }
	@getter public boolean				get$of_method()	{ return this.getImportView().of_method; }
	@getter public DNode				get$resolved()	{ return this.getImportView().resolved; }
	
	@setter public void set$name(NameRef val)			{ this.getImportView().name = val; }
	@setter public void set$mode(ImportMode val)		{ this.getImportView().mode = val; }
	@setter public void set$star(boolean val)			{ this.getImportView().star = val; }
	@setter public void set$of_method(boolean val)		{ this.getImportView().of_method = val; }
	@setter public void set$resolved(DNode val)		{ this.getImportView().resolved = val; }

	public Import() { super(new ImportImpl()); }

	public Import(DNode node, ImportMode mode, boolean star) {
		super(new ImportImpl());
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

	public boolean preGenerate()	{ return false; }
	public boolean mainResolveIn(TransfProcessor proc)		{ return false; }

	public ASTNode resolveImports() {
		if (!of_method || (mode==ImportMode.IMPORT_STATIC && star)) return this;
		int i = 0;
		Type[] types;
		if( args.length > 0 && args[0]==Type.tpRule) {
			types = new Type[args.length-1];
			i++;
		} else {
			types = new Type[args.length];
		}
		for(int j=0; j < types.length; j++,i++)
			types[j] = args[i].getType();
		DNode@ v;
		MethodType mt = MethodType.newMethodType(null,types,Type.tpAny);
		if( !PassInfo.resolveMethodR(this,v,null,name.name,mt) )
			throw new CompilerException(this,"Unresolved method "+Method.toString(name.name,mt));
		DNode n = v;
		if (mode != ImportMode.IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(this,"Identifier "+name+" is not a method");
		resolved = n;
		return this;
	}

	public void resolveDecl() {}

	public void generate(Code code) {}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name)
		Struct@ s;
		Struct@ sub;
		DNode@ tmp;
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
		;	s.isPackage(), s.resolveNameR(node,path,name)
		}
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		path.isStaticAllowed(),
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveNameR(node,path,name),
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
		;	tmp instanceof TypeDefOp,
			trace(Kiev.debugResolve,"Syntax check typedef "+tmp+" == "+name),
			((TypeDefOp)tmp).name.equals(name),
			node ?= ((TypeDefOp)tmp)
		//;	trace(Kiev.debugResolve,"Syntax check "+tmp.getClass()+" "+tmp+" == "+name), false
		}
	}

	public rule resolveMethodR(DNode@ node, ResInfo path, KString name, MethodType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && this.resolved instanceof Method,
		((Method)this.resolved).equalsByCast(name,mt,null,path),
		node ?= ((Method)this.resolved)
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).type.resolveCallStaticR(node,path,name,mt),
		node instanceof Method && node.isStatic() && node.isPublic()
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(this.toString()).append(";").newLine();
		return dmp;
	}

}


@node
public final class TypeDefOp extends TypeDef implements Named {

	@node
	static final class TypeDefOpImpl extends TypeDefImpl {
		TypeDefOpImpl() {}
		TypeDefOpImpl(int pos) { super(pos); }
		@att KString		name;
		@att TypeRef		type;
		@att TypeArgDef		typearg;
	}
	@nodeview
	static final view TypeDefOpView of TypeDefOpImpl extends TypeDefView {
		public	KString			name;
		public	TypeRef			type;
		public	TypeArgDef		typearg;
	}
	public NodeView			getNodeView()		{ return new TypeDefOpView((TypeDefOpImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new TypeDefOpView((TypeDefOpImpl)this.$v_impl); }
	public TypeDefView		getTypeDefView()	{ return new TypeDefOpView((TypeDefOpImpl)this.$v_impl); }
	public TypeDefOpView	getTypeDefOpView()	{ return new TypeDefOpView((TypeDefOpImpl)this.$v_impl); }

	@getter public KString			get$name()		{ return this.getTypeDefOpView().name; }
	@getter public TypeRef			get$type()		{ return this.getTypeDefOpView().type; }
	@getter public TypeArgDef		get$typearg()	{ return this.getTypeDefOpView().typearg; }

	@setter public void set$name(KString val)			{ this.getTypeDefOpView().name = val; }
	@setter public void set$type(TypeRef val)			{ this.getTypeDefOpView().type = val; }
	@setter public void set$typearg(TypeArgDef val)	{ this.getTypeDefOpView().typearg = val; }
	
	public static TypeDefOp[]		emptyArray = new TypeDefOp[0];

	@att public abstract virtual KString		name;
	@att public abstract virtual TypeRef		type;
	@att public abstract virtual TypeArgDef	typearg;

	public TypeDefOp() {
		super(new TypeDefOpImpl());
	}
	
	public TypeDefOp(int pos, KString name) {
		super(new TypeDefOpImpl(pos));
		this.name = name;
	}
	
	public TypeDefOp(Type tp, KString name) {
		super(new TypeDefOpImpl());
		this.type = new TypeRef(tp);
		this.name = name;
	}
	
	public boolean checkResolved() {
		Type t = type.getType();
		if (t != null && t.getStruct() != null)
			return t.getStruct().checkResolved();
		return true;
	}
	
	public NodeName	getName() {
		return new NodeName(name);
	}
	
	public Type getType() {
		return type.getType();
	}

	public void set(NameRef id, ASTOperator op, TypeRef tp) {
		typearg = new TypeArgDef(id.name);
		name = op.image;

		TypeWithArgsRef natp = (TypeWithArgsRef)tp;
		TypeNameRef arg = (TypeNameRef)natp.args[0];
		KString argnm = arg.name.name;
		//if (!typearg.name.short_name.equals(argnm))
		//	throw new ParseException("Typedef args "+typearg.name.short_name+" and "+type+" do not match");
		natp.args[0] = new TypeRef(typearg.getType());
		type = natp;
		return;
	}
	
	public void set(TypeRef tp, NameRef id) {
		type = tp;
		name = id.name;
	}

	public boolean preGenerate()	{ return false; }
	public boolean mainResolveIn(TransfProcessor proc)		{ return false; }

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

