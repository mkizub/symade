package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import syntax kiev.Syntax;
import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeNameRef;
	@virtual typedef VView = VTypeNameRef;

	@att public TypeRef			outer;

	@nodeview
	public static view VTypeNameRef of TypeNameRef extends VTypeRef {
		public TypeRef				outer;
	}

	public TypeNameRef() {}

	public TypeNameRef(String nm) {
		this.ident = new SymbolRef(nm);
	}

	public TypeNameRef(SymbolRef nm) {
		this.pos = pos;
		this.ident = nm;
	}

	public TypeNameRef(SymbolRef nm, Type tp) {
		this.pos = pos;
		this.ident = nm;
		this.lnk = tp;
	}

	public TypeNameRef(TypeRef outer, SymbolRef nm) {
		this.pos = pos;
		this.outer = outer;
		this.ident = nm;
	}

	public TypeNameRef(TypeNameRef tnr) {
		this.pos = tnr.pos;
		if (tnr.outer != null)
			this.outer = ~tnr.outer;
		this.ident = ~tnr.ident;
	}

	public Type getType() {
		if (this.lnk != null)
			return this.lnk;
		Type tp;
		if (ident.symbol instanceof TypeDecl) {
			if (outer != null)
				tp = ((TypeDecl)ident.symbol).getType().bind(outer.getType().bindings());
			else
				tp = ((TypeDecl)ident.symbol).getType();
		}
		else if (this.outer != null) {
			Type outer = this.outer.getType();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.meta_type.tdecl.resolveNameR(td,info,ident.name))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			ident.symbol = td;
			td.checkResolved();
			tp = td.getType().bind(outer.bindings());
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveQualifiedNameR(((TypeNameRef)this),td,new ResInfo(this,ResInfo.noForwards),ident.name) )
				throw new CompilerException(this,"Unresolved type "+ident);
			ident.symbol = td;
			td.checkResolved();
			tp = td.getType();
		}
		this.lnk = tp;
		return this.lnk;
	}

	public boolean isBound() {
		return true;
	}

	public Struct getStruct() {
		if (this.lnk != null) return this.lnk.getStruct();
		if (this.outer != null) {
			Struct outer = this.outer.getStruct();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveNameR(td,info,ident.name))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			return td.getStruct();
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveQualifiedNameR(this,td,new ResInfo(this,ResInfo.noForwards),ident.name) )
				throw new CompilerException(this,"Unresolved type "+ident);
			return td.getStruct();
		}
	}

	public TypeDecl getTypeDecl() {
		if (this.lnk != null) return this.lnk.meta_type.tdecl;
		if (this.outer != null) {
			TypeDecl outer = this.outer.getTypeDecl();
			ResInfo info = new ResInfo(this,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveNameR(td,info,ident.name))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			return (TypeDecl)td;
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveQualifiedNameR(this,td,new ResInfo(this,ResInfo.noForwards),ident.name) )
				throw new CompilerException(this,"Unresolved type "+ident);
			return (TypeDecl)td.getStruct();
		}
	}

	public String toString() {
		if (outer == null)
			return ident.toString();
		return outer+"."+ident.toString();
	}
	public Dumper toJava(Dumper dmp) {
		if (outer == null)
			return dmp.append(ident);
		return dmp.append(outer).append('.').append(ident);
	}
}

