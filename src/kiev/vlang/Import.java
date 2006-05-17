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
	@virtual typedef VView = VImport;

	@att public SymbolRef			name;
	@att public ImportMode			mode = ImportMode.IMPORT_CLASS;
	@att public boolean				star;
	@att public NArr<TypeRef>		args;
	
	@ref public boolean				of_method;
	@ref public DNode				resolved;

	@nodeview
	public static final view VImport of Import extends VSNode {
		public		SymbolRef			name;
		public		ImportMode			mode;
		public		boolean				star;
		public:ro	NArr<TypeRef>		args;
		public		boolean				of_method;
		public		DNode				resolved;
		
		public boolean mainResolveIn() { return false; }
	}

	public Import() {}

	public Import(Struct node, boolean star) {
		this.name = new SymbolRef(node.qname());
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
		CallType mt = new CallType(types,Type.tpAny);
		if( !PassInfo.resolveMethodR(this,v,null,name.name,mt) )
			throw new CompilerException(this,"Unresolved method "+Method.toString(name.name,mt));
		DNode n = v;
		if (mode != ImportMode.IMPORT_STATIC || !(n instanceof Method))
			throw new CompilerException(this,"Identifier "+name+" is not a method");
		resolved = n;
		return this;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name)
		Struct@ s;
		DNode@ sub;
		ASTNode@ tmp;
	{
		this.resolved instanceof Method, $cut, false
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && !star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		!s.isPackage(),
		{
			s.qname() == name, node ?= s.$var
		;	s.id.equals(name), node ?= s.$var
		}
	;
		mode == ImportMode.IMPORT_CLASS && this.resolved instanceof Struct && star,
		((Struct)this.resolved).checkResolved(),
		s ?= ((Struct)this.resolved),
		{
			!s.isPackage(),
			sub @= s.sub_decls,
			sub.id.equals(name),
			node ?= sub.$var
		;	s.isPackage(), s.resolveNameR(node,path,name)
		}
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		path.isStaticAllowed(),
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveNameR(node,path,name),
		node instanceof Field && ((Field)node).isStatic() && ((Field)node).isPublic()
	;
		mode == ImportMode.IMPORT_SYNTAX && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		tmp @= ((Struct)this.resolved).members,
		{
			tmp instanceof Field,
			trace(Kiev.debugResolve,"Syntax check field "+tmp+" == "+name),
			((Field)tmp).id.equals(name),
			node ?= tmp
		;	tmp instanceof TypeDef,
			trace(Kiev.debugResolve,"Syntax check typedef "+tmp+" == "+name),
			((TypeDef)tmp).id.equals(name),
			node ?= ((TypeDef)tmp)
		//;	trace(Kiev.debugResolve,"Syntax check "+tmp.getClass()+" "+tmp+" == "+name), false
		}
	}

	public rule resolveMethodR(Method@ node, ResInfo path, String name, CallType mt)
	{
		mode == ImportMode.IMPORT_STATIC && !star && this.resolved instanceof Method,
		((Method)this.resolved).equalsByCast(name,mt,null,path),
		node ?= ((Method)this.resolved)
	;
		mode == ImportMode.IMPORT_STATIC && star && this.resolved instanceof Struct,
		((Struct)this.resolved).checkResolved(),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		((Struct)this.resolved).resolveMethodR(node,path,name,mt),
		node instanceof Method && node.isStatic() && node.isPublic()
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append(this.toString()).append(";").newLine();
		return dmp;
	}

}

@node
public final class TypeOpDef extends TypeDecl implements ScopeOfNames {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeOpDef;
	@virtual typedef VView = VTypeOpDef;

	@att public ASTOperator		op;
	@att public TypeRef			type;
	@att public TypeDef			arg;

	@nodeview
	public static final view VTypeOpDef of TypeOpDef extends VTypeDecl {
		public	ASTOperator		op;
		public	TypeRef			type;
		public	TypeDef			arg;

		public boolean mainResolveIn() { return false; }
	}

	public TypeOpDef() {}
	
	public Type getType() { return type.getType(); }
	
	public boolean checkResolved() {
		return type.getType().checkResolved();
	}
	
	public Struct getStruct() {
		return getType().getStruct();
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path, String name) {
		path.space_prev == this.type,
		this.arg.id.equals(name),
		node ?= this.arg
	}

	public String toString() {
		return "typedef "+arg+op+" "+type+"<"+arg+">;";
	}

	public Dumper toJava(Dumper dmp) {
    	return dmp.append("/* ").append(toString()).append(" */").newLine();
    }
}

