package kiev.vlang.types;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@node
public class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  ≤ TypeNameRef;

	@att public TypeNameRef			outer;

	public TypeNameRef() {}

	public TypeNameRef(String nm) {
		if (nm.indexOf('.') >= 0) {
			outer = new TypeNameRef(nm.substring(0,nm.indexOf('.')));
			this.ident = new SymbolRef<DNode>(nm.substring(nm.indexOf('.')+1));
		} else {
			this.ident = new SymbolRef<DNode>(nm);
		}
	}

	public TypeNameRef(SymbolRef<DNode> nm) {
		assert (nm.name.indexOf('.') < 0);
		this.pos = pos;
		this.ident = nm;
	}

	public TypeNameRef(SymbolRef<DNode> nm, Type tp) {
		assert (nm.name.indexOf('.') < 0);
		this.pos = pos;
		this.ident = nm;
		this.lnk = tp;
	}

	public TypeNameRef(Type tp) {
		String nm = tp.toString();
		if (nm.indexOf('.') >= 0) {
			outer = new TypeNameRef(nm.substring(0,nm.indexOf('.')));
			this.ident = new SymbolRef<DNode>(nm.substring(nm.indexOf('.')+1));
		} else {
			this.ident = new SymbolRef<DNode>(nm);
		}
		this.lnk = tp;
	}

	public TypeNameRef(TypeNameRef outer, SymbolRef<DNode> nm) {
		assert (nm.name.indexOf('.') < 0);
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

	public String qname() {
		if (lnk != null)
			return this.lnk.meta_type.tdecl.qname();
		if (outer == null)
			return ident.name;
		return (outer.qname() + '.' + ident.name).intern();
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
			ResInfo info = new ResInfo(this,ident.name,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.meta_type.tdecl.resolveNameR(td,info))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			ident.symbol = td;
			td.checkResolved();
			tp = td.getType().bind(outer.bindings());
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(((TypeNameRef)this),td,new ResInfo(this,ident.name,ResInfo.noForwards)) )
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
			ResInfo info = new ResInfo(this,ident.name,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveNameR(td,info))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			return td.getStruct();
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(this,td,new ResInfo(this,ident.name,ResInfo.noForwards)) )
				throw new CompilerException(this,"Unresolved type "+ident);
			return td.getStruct();
		}
	}

	public TypeDecl getTypeDecl() {
		if (this.lnk != null) return this.lnk.meta_type.tdecl;
		if (this.outer != null) {
			TypeDecl outer = this.outer.getTypeDecl();
			ResInfo info = new ResInfo(this,ident.name,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.resolveNameR(td,info))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			return (TypeDecl)td;
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(this,td,new ResInfo(this,ident.name,ResInfo.noForwards)) )
				throw new CompilerException(this,"Unresolved type "+ident);
			return (TypeDecl)td.getStruct();
		}
	}

	public String toString() {
		if (outer == null)
			return ident.toString();
		return outer+"."+ident.toString();
	}
}

