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
public final class TypeNameRef extends TypeRef {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeNameRef;

	@att public TypeNameRef			outer;
	@att public TypeRef[]			args;

	public TypeNameRef() {}

	public TypeNameRef(String nm) {
		int li = nm.lastIndexOf('.');
		if (li >= 0) {
			outer = new TypeNameRef(nm.substring(0,li));
			this.ident = new SymbolRef<DNode>(nm.substring(li+1));
		} else {
			this.ident = new SymbolRef<DNode>(nm);
		}
	}

	public TypeNameRef(String nm, Type tp) {
		assert (nm.indexOf('.') < 0);
		this.pos = pos;
		this.ident.name = nm;
		this.lnk = tp;
	}

	public TypeNameRef(Type tp) {
		String nm = tp.meta_type.tdecl.qname();
		int li = nm.lastIndexOf('.');
		if (li >= 0) {
			outer = new TypeNameRef(nm.substring(0,li));
			this.ident = new SymbolRef<TypeDecl>(nm.substring(li+1));
		} else {
			this.ident = new SymbolRef<TypeDecl>(nm);
		}
		this.lnk = tp;
	}

	public TypeNameRef(TypeNameRef outer, String nm) {
		assert (nm.indexOf('.') < 0);
		this.pos = pos;
		this.outer = outer;
		this.ident.name = nm;
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
		if (ident.dnode instanceof TypeDecl) {
			if (outer != null)
				tp = ((TypeDecl)ident.dnode).getType().bind(outer.getType().bindings());
			else
				tp = ((TypeDecl)ident.dnode).getType();
		}
		else if (this.outer != null) {
			Type outer = this.outer.getType();
			ResInfo info = new ResInfo(this,ident.name,ResInfo.noImports|ResInfo.noForwards|ResInfo.noSuper);
			TypeDecl@ td;
			if!(outer.meta_type.tdecl.resolveNameR(td,info))
				throw new CompilerException(this,"Unresolved type "+ident+" in "+outer);
			ident.symbol = td.id;
			td.checkResolved();
			tp = td.getType().bind(outer.bindings());
		} else {
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(((TypeNameRef)this),td,new ResInfo(this,ident.name,ResInfo.noForwards)) )
				throw new CompilerException(this,"Unresolved type "+ident);
			ident.symbol = td.id;
			td.checkResolved();
			tp = td.getType();
		}
		if (args.length > 0) {
			TVarSet tpset = tp.meta_type.getTemplBindings();
			TVarBld set = new TVarBld();
			int a = 0;
			for(int b=0; a < args.length && b < tpset.tvars.length; b++) {
				if (tpset.tvars[b].unalias().val != null)
					continue;
				Type bound = args[a].getType();
				if (bound == null)
					throw new CompilerException(this,"Type "+args[a]+" is not found");
				if!(bound.isInstanceOf(tpset.tvars[b].var))
					throw new CompilerException(this,"Type "+bound+" is not applayable to "+tpset.tvars[b].var);
				set.append(tpset.tvars[b].var, bound);
				a++;
			}
			if (a < args.length)
				Kiev.reportError(this,"Type "+tp+" has only "+a+" unbound type parameters");
			tp = tp.meta_type.make(set);
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
		if (outer == null && args.length == 0)
			return ident.toString();
		StringBuffer sb = new StringBuffer();
		sb.append(outer).append('.').append(ident);
		if (args.length > 0) {
			sb.append('<');
			for (int i=0; i < args.length; i++) {
				sb.append(args[i]);
				if (i < args.length-1) sb.append(',');
			}
			sb.append('>');
		}
		return sb.toString();
	}
}

