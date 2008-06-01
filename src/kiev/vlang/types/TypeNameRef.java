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
package kiev.vlang.types;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@ThisIsANode(lang=CoreLang)
public final class TypeNameRef extends TypeRef {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	@virtual typedef This  = TypeNameRef;

	@nodeAttr public TypeRef			outer;
	@nodeAttr public TypeRef[]			args;

	public TypeNameRef() {}

	public TypeNameRef(String nm) {
		this.ident = nm;
	}

	public TypeNameRef(String nm, Type tp) {
		this.ident = nm;
		this.type_lnk = tp;
	}

	public TypeNameRef(int pos, String nm, Type tp) {
		this.pos = pos;
		this.ident = nm;
		this.type_lnk = tp;
	}

	public TypeNameRef(Type tp) {
		String nm = tp.meta_type.qname();
		this.ident = nm;
		this.type_lnk = tp;
	}

	public TypeNameRef(TypeRef outer, String nm) {
		this.outer = outer;
		this.ident = nm;
	}

	public String qname() {
		if (type_lnk != null)
			return this.type_lnk.meta_type.qname();
		TypeRef outer = this.outer;
		if (outer == null)
			return ident;
		if (outer instanceof TypeNameRef)
			return (outer.qname() + '\u001f' + ident);
		return ident;
	}
	
	@setter final public void setAutoGenerated(boolean on) {
		this.is_auto_generated = on;
		if (this.outer != null)
			this.outer.setAutoGenerated(on);
	}

	public Type getType() {
		if (this.type_lnk != null)
			return this.type_lnk;
		TypeDecl td = getTypeDecl();
		td.checkResolved();
		Type tp = td.getType();
		if (this.outer != null)
			tp = tp.bind(this.outer.getType().bindings());
		if (args.length > 0) {
			TVarSet tpset = tp.meta_type.getTemplBindings();
			TVarBld set = new TVarBld();
			int a = 0;
			for(int b=0; a < args.length && b < tpset.tvars.length; b++) {
				if (tpset.tvars[b].unalias(tpset).val != null)
					continue;
				Type bound = args[a].getType();
				if (bound == null)
					throw new CompilerException(this,"Type "+args[a]+" is not found");
				if!(bound.isInstanceOf(tpset.tvars[b].var)) {
					if (!(bound instanceof ArgType) || ((ArgType)bound).definer.super_types.length > 0)
						throw new CompilerException(this,"Type "+bound+" is not applayable to "+tpset.tvars[b].var);
				}
				set.append(tpset.tvars[b].var, bound);
				a++;
			}
			if (a < args.length)
				Kiev.reportError(this,"Type "+tp+" has only "+a+" unbound type parameters");
			tp = tp.meta_type.make(set);
		}
		this.type_lnk = tp;
		return tp;
	}

	public Struct getStruct() {
		TypeDecl td = getTypeDecl();
		if (td instanceof Struct)
			return (Struct)td;
		return null;
	}

	public TypeDecl getTypeDecl() {
		if (this.type_lnk != null) return this.type_lnk.meta_type.tdecl;
		if (this.dnode instanceof TypeDecl)
			return (TypeDecl)this.dnode;
		TypeDecl scope;
		String name = this.ident;
		String head;
		if (this.outer == null) {
			int dot = name.indexOf('\u001f');
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1);
			} else {
				head = name;
				name = "";
			}
			TypeDecl@ td;
			if( !PassInfo.resolveNameR(this,td,new ResInfo(this,head,ResInfo.noForwards)) )
				throw new CompilerException(this,"Unresolved type "+head);
			scope = td;
		} else {
			Type outer = this.outer.getType();
			scope = outer.meta_type.tdecl;
		}
		while (name.length() > 0) {
			int dot = name.indexOf('\u001f');
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1);
			} else {
				head = name.intern();
				name = "";
			}
			TypeDecl@ td;
			if!(scope.resolveNameR(td,new ResInfo(this,head,ResInfo.noForwards)))
				throw new CompilerException(this,"Unresolved identifier "+head+" in "+scope);
			scope = td;
		}
		this.symbol = scope;
		return scope;
	}

	public DNode[] findForResolve(String name, AttrSlot slot, boolean by_equals) {
		if (slot.name == "ident") {
			TypeDecl scope;
			String head;
			int dot = name.indexOf('\u001f');
			if (dot > 0) {
				head = name.substring(0,dot).intern();
				name = name.substring(dot+1);
			} else {
				head = name;
				name = "";
			}
			if (this.outer == null) {
				if (dot < 0) {
					int flags = ResInfo.noForwards;
					if (!by_equals)
						flags |= ResInfo.noEquals;
					Vector<TypeDecl> vect = new Vector<TypeDecl>();
					TypeDecl@ td;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (PassInfo.resolveNameR(this,td,info)) {
						if (!vect.contains(td))
							vect.append(td);
					}
					return vect.toArray();
				} else {
					TypeDecl@ td;
					if( !PassInfo.resolveNameR(this,td,new ResInfo(this,head,ResInfo.noForwards)) )
						return new TypeDecl[0];
					scope = td;
				}
			} else {
				Type outer = this.outer.getType();
				scope = outer.meta_type.tdecl;
			}
			while (dot >= 0) {
				dot = name.indexOf('\u001f');
				if (dot > 0) {
					head = name.substring(0,dot).intern();
					name = name.substring(dot+1);
				} else {
					head = name.intern();
					name = "";
				}
				if (dot < 0) {
					int flags = ResInfo.noForwards;
					if (!by_equals)
						flags |= ResInfo.noEquals;
					Vector<TypeDecl> vect = new Vector<TypeDecl>();
					TypeDecl@ td;
					ResInfo info = new ResInfo(this,head,flags);
					foreach (scope.resolveNameR(td,info)) {
						if (!vect.contains(td))
							vect.append(td);
					}
					return vect.toArray();
				} else {
					TypeDecl@ td;
					if!(scope.resolveNameR(td,new ResInfo(this,head,ResInfo.noForwards)))
						return new TypeDecl[0];
					scope = td;
				}
			}
		}
		return super.findForResolve(name,slot,by_equals);
	}

	public String toString() {
		if (outer == null && args.length == 0)
			return ident.replace('\u001f','.');
		StringBuffer sb = new StringBuffer();
		sb.append(outer).append('.').append(ident.replace('\u001f','.'));
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

