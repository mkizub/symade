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
		if( args.length > 0 && args[0].getType() ≡ Type.tpRule) {
			types = new Type[args.length-1];
			i++;
		} else {
			types = new Type[args.length];
		}
		for(int j=0; j < types.length; j++,i++)
			types[j] = args[i].getType();
		DNode@ v;
		MethodType mt = new MethodType(types,Type.tpAny);
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
public final class TypeOpDef extends TypeDecl implements Named, ScopeOfNames {

	@dflow(out="this:in") private static class DFI {}

	@node
	static final class TypeOpDefImpl extends TypeDeclImpl {
		TypeOpDefImpl() {}
		TypeOpDefImpl(int pos) { super(pos); }
		@att ASTOperator	op;
		@att TypeRef		type;
		@att TypeDef		arg;
	}
	@nodeview
	static final view TypeOpDefView of TypeOpDefImpl extends TypeDeclView {
		public	ASTOperator		op;
		public	TypeRef			type;
		public	TypeDef		arg;
	}
	public NodeView			getNodeView()		{ return new TypeOpDefView((TypeOpDefImpl)this.$v_impl); }
	public DNodeView		getDNodeView()		{ return new TypeOpDefView((TypeOpDefImpl)this.$v_impl); }
	public TypeDeclView		getTypeDeclView()	{ return new TypeOpDefView((TypeOpDefImpl)this.$v_impl); }
	public TypeOpDefView	getTypeOpDefView()	{ return new TypeOpDefView((TypeOpDefImpl)this.$v_impl); }

	@getter public ASTOperator		get$op()		{ return this.getTypeOpDefView().op; }
	@getter public TypeRef			get$type()		{ return this.getTypeOpDefView().type; }
	@getter public TypeDef		get$arg()		{ return this.getTypeOpDefView().arg; }

	@setter public void set$op(ASTOperator val)		{ this.getTypeOpDefView().op = val; }
	@setter public void set$type(TypeRef val)			{ this.getTypeOpDefView().type = val; }
	@setter public void set$arg(TypeDef val)		{ this.getTypeOpDefView().arg = val; }
	
	@att public abstract virtual ASTOperator	op;
	@att public abstract virtual TypeRef		type;
	@att public abstract virtual TypeDef	arg;

	public TypeOpDef() {
		super(new TypeOpDefImpl());
	}
	
	public boolean checkResolved() {
		return type.getType().checkResolved();
	}
	
	public NodeName	getName() {
		return new NodeName(op.image);
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
	public boolean mainResolveIn(TransfProcessor proc)		{ return false; }

	public String toString() {
		return "typedef "+arg+op+" "+type+"<"+arg+">;";
	}

	public Dumper toJava(Dumper dmp) {
    	return dmp.append("/* ").append(toString()).append(" */").newLine();
    }
}

