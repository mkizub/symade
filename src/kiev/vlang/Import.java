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

@nodeset
public final class Import extends DNode implements Constants, ScopeOfNames, ScopeOfMethods {
	public static final Import[] emptyArray = new Import[0];

	public enum ImportMode {
		IMPORT_CLASS,
		IMPORT_STATIC,
		IMPORT_PACKAGE,
		IMPORT_SYNTAX;
	}

	@virtual typedef NImpl = ImportImpl;
	@virtual typedef VView = ImportView;

	@nodeimpl
	static final class ImportImpl extends DNodeImpl {
		@virtual typedef ImplOf = Import;
		ImportImpl() {}
		@att NameRef				name;
		@att ImportMode				mode = ImportMode.IMPORT_CLASS;
		@att boolean				star;
		@att NArr<TypeRef>			args;
		
		@ref boolean				of_method;
		@ref DNode					resolved;
	}
	@nodeview
	public static final view ImportView of ImportImpl extends DNodeView {
		public				NameRef				name;
		public				ImportMode			mode;
		public				boolean				star;
		public access:ro	NArr<TypeRef>		args;
		public				boolean				of_method;
		public				DNode				resolved;
		
		public boolean mainResolveIn(TransfProcessor proc)		{ return false; }
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

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
		DNode@ v;
		CallType mt = new CallType(types,Type.tpAny);
		if( !PassInfo.resolveMethodR(this,v,null,name.name,mt) )
			throw new CompilerException(this,"Unresolved method "+Method.toString(name.name,mt));
		DNode n = v;
		if (mode != ImportMode.IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(this,"Identifier "+name+" is not a method");
		resolved = n;
		return this;
	}

	public void resolveDecl() {}

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
			sub @= s.sub_clazz,
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
		;	tmp instanceof TypeDef,
			trace(Kiev.debugResolve,"Syntax check typedef "+tmp+" == "+name),
			((TypeDef)tmp).name.equals(name),
			node ?= ((TypeDef)tmp)
		//;	trace(Kiev.debugResolve,"Syntax check "+tmp.getClass()+" "+tmp+" == "+name), false
		}
	}

	public rule resolveMethodR(DNode@ node, ResInfo path, KString name, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && this.resolved instanceof Method,
		((Method)this.resolved).equalsByCast(name,mt,null,path),
		node ?= ((Method)this.resolved)
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).ctype.resolveCallStaticR(node,path,name,mt),
		node instanceof Method && node.isStatic() && node.isPublic()
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(this.toString()).append(";").newLine();
		return dmp;
	}

}

@nodeset
public final class TypeOpDef extends TypeDecl implements Named, ScopeOfNames {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef NImpl = TypeOpDefImpl;
	@virtual typedef VView = TypeOpDefView;

	@nodeimpl
	static final class TypeOpDefImpl extends TypeDeclImpl {
		@virtual typedef ImplOf = TypeOpDef;
		TypeOpDefImpl() {}
		TypeOpDefImpl(int pos) { super(pos); }
		@att ASTOperator	op;
		@att TypeRef		type;
		@att TypeDef		arg;
	}
	@nodeview
	public static final view TypeOpDefView of TypeOpDefImpl extends TypeDeclView {
		public	ASTOperator		op;
		public	TypeRef			type;
		public	TypeDef			arg;

		public boolean mainResolveIn(TransfProcessor proc)		{ return false; }
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }

	public TypeOpDef() {
		super(new TypeOpDefImpl());
	}
	
	public boolean checkResolved() {
		return type.getType().checkResolved();
	}
	
	public NodeName	getName() {
		return new NodeName(op.image);
	}
	
	public Struct getStruct() {
		return getType().getStruct();
	}
	
	public Type getType() {
		return type.getType();
	}

	public rule resolveNameR(DNode@ node, ResInfo path, KString name) {
		path.space_prev == this.type,
		this.arg.name.name == name,
		node ?= this.arg
	}

	public boolean preGenerate()	{ return false; }

	public String toString() {
		return "typedef "+arg+op+" "+type+"<"+arg+">;";
	}

	public Dumper toJava(Dumper dmp) {
    	return dmp.append("/* ").append(toString()).append(" */").newLine();
    }
}

