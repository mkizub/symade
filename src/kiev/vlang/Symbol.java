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

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@ThisIsANode(lang=void)
public interface ISymbol extends ASTNode {
	@virtual @abstract
	public:ro String		sname;
	@virtual @abstract
	public:ro DNode			dnode;
	@virtual @abstract
	public:ro Symbol		symbol;

	@getter public String	get$sname(); // source code name, may be null for anonymouse symbols
	@getter public DNode	get$dnode();
	@getter public Symbol	get$symbol();
}

@ThisIsANode(lang=CoreLang)
public class Symbol extends ASTNode implements ISymbol {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final Symbol[] emptyArray = new Symbol[0];
	
	@AttrXMLDumpInfo(attr=true, name="name")
	@nodeAttr public	String		sname; // source code name, may be null for anonymouse symbols

	@AttrXMLDumpInfo(attr=true)
	@UnVersioned
	@nodeAttr(copyable=false)
	public	String					uuid; // source code name, may be null for anonymouse symbols
	
	@getter public DNode get$dnode() {
		ANode p = parent();
		if (p instanceof DNode)
			return (DNode)p;
		return null;
	}
	
	@getter public Symbol get$symbol() {
		return this;
	}
	
	final public String getUUID() {
		String u = this.uuid;
		if (u == null) {
			u = java.util.UUID.randomUUID().toString();
			this.uuid = u;
		}
		return u;
	}
	@setter final public void set$uuid(String value) {
		value = value.intern();
		if (this.uuid == value)
			return;
		assert (this.uuid == null);
		Env.getRoot().registerSymbol(value,this);
		this.uuid = value;
	}
	
	public String qname() {
		if (sname == null)
			return null;
		DNode dn = this.dnode;
		if (dn instanceof GlobalDNode)
			return ((GlobalDNode)dn).qname();
		return sname;
	}

	public Symbol() {}
	public Symbol(int pos, String sname) {
		this.pos = pos;
		this.sname = sname;
	}
	
	public Symbol(String sname) {
		this.sname = sname;
	}
	
	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (isAttached()) {
			if (attr.name == "sname")
				parent().callbackChildChanged(ChildChangeType.MODIFIED, pslot(), this);
		}
		super.callbackChildChanged(ct, attr, data);
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.sname = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.sname = t.image;
	}
	
	@getter public final String get$sname() {
		return sname;
	}
	@setter public final void set$sname(String value) {
		this.sname = (value == null) ? null : value.intern();
	}
	
	public boolean equals(Object:Object nm) {
		return false;
	}

	public boolean equals(Symbol:Object nm) {
		if (this.equals(nm.sname)) return true;
		return false;
	}

	public boolean equals(DNode:Object nm) {
		if (this.equals(nm.sname)) return true;
		return false;
	}

	public boolean equals(String:Object nm) {
		if (sname == nm) return true;
		return false;
	}

	public String toString() {
		return sname;
	}
	
	public String makeSignature() {
		if (sname == null)
			return '‣' + getUUID(); // UUID separator is \u2023
		return sname + '‣' + getUUID(); // UUID separator is \u2023
	}
}

@unerasable
@ThisIsANode(lang=CoreLang)
public final class SymbolRef<D extends DNode> extends ASTNode {

	@DataFlowDefinition(out="this:in") private static class DFI {}

	public static final SymbolRef[] emptyArray = new SymbolRef[0];

	final static class NameAndUUID {
		final String name;
		final String uuid;
		NameAndUUID(String name, String uuid) {
			this.name = name.intern();
			this.uuid = uuid.intern();
		}
	}
	
	private Object ident_or_symbol_or_type;
	
	@AttrXMLDumpInfo(attr=true)
	@abstract @nodeAttr public		String		name; // unresolved name
	@AttrXMLDumpInfo(attr=true, name="full")
	@abstract @nodeAttr public		boolean		qualified; // stored name may be qualified name
	@AttrXMLDumpInfo(ignore=true)
	@abstract @nodeData public		Symbol		symbol; // resolved symbol
	@abstract           public:ro	D			dnode; // resolved dnode (symbol.parent())
		 
	public SymbolRef() {}

	public SymbolRef(String name) {
		this.name = name;
	}

	public SymbolRef(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public SymbolRef(int pos, Symbol symbol) {
		this.pos = pos;
		this.symbol = symbol.symbol;
	}

	public SymbolRef(int pos, D symbol) {
		this.pos = pos;
		this.symbol = symbol.symbol;
	}

	public SymbolRef(Symbol symbol) {
		this.symbol = symbol;
	}

	public SymbolRef(D symbol) {
		this.symbol = symbol.symbol;
	}

	@getter public final boolean get$qualified() {
		return is_qualified;
	}

	@setter public final void set$qualified(boolean val) {
		is_qualified = val;
	}

	public boolean equals(Object nm) {
		if (nm instanceof DNode) return nm.sname == this.name;
		if (nm instanceof Symbol) return nm.equals(this.name);
		if (nm instanceof SymbolRef) return nm.name == this.name;
		if (nm instanceof String) return nm == this.name;
		return false;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}
	
	public void setNameAndUUID(String name, String uuid) {
		this.ident_or_symbol_or_type = new NameAndUUID(name, uuid);
	}

	@getter public final String get$name() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof String)
			return (String)id;
		if (id instanceof Symbol) {
			if (qualified)
				return ((Symbol)id).qname();
			return ((Symbol)id).sname;
		}
		if (id instanceof Type) {
			if (qualified)
				return ((Type)id).meta_type.qname();
			return ((Type)id).meta_type.tdecl.sname;
		}
		if (id instanceof NameAndUUID) {
			return ((NameAndUUID)id).name;
		}
		return null;
	}

	@getter public final Symbol get$symbol() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof Symbol)
			return (Symbol)id;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl.symbol;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getRoot().getSymbolByUUID(nid.uuid);
			if (sym != null) {
				this.symbol = sym;
				return sym;
			}
		}
		return null;
	}
	
	@getter public final D get$dnode() {
		Object id = ident_or_symbol_or_type;
		if (id instanceof Symbol)
			return ((Symbol)id).dnode;
		if (id instanceof Type)
			return ((Type)id).meta_type.tdecl;
		if (id instanceof NameAndUUID) {
			NameAndUUID nid = (NameAndUUID)id;
			Symbol sym = Env.getRoot().getSymbolByUUID(nid.uuid);
			if (sym != null) {
				this.symbol = sym;
				return sym.dnode;
			}
		}
		return null;
	}
	
	@setter public final void set$name(String val) {
		if (val != null) {
			val = val.intern();
			if (val.indexOf('·') >= 0)
				qualified = true;
		}
		ident_or_symbol_or_type = val;
	}
	
	@setter public final void set$symbol(Symbol val) {
		ident_or_symbol_or_type = val;
	}
	
	public String toString() { return name; }

	
	public String makeSignature() {
		Object id = this.ident_or_symbol_or_type;
		if (id instanceof Type)
			return ((Type)id).makeSignature();
		if (id instanceof Symbol) {
			Symbol sym = (Symbol)id;
			if (sym.sname == null)
				return '‣' + sym.getUUID();
			return sym.qname() + '‣' + sym.getUUID();
		}
		return (String)id;
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "qualified")
			return this.qualified; // do not dump <qualified>false</qualified>
		return super.includeInDump(dump, attr, val);
	}

	public Symbol[] resolveAutoComplete(String str, AttrSlot slot) {
		if (slot.name == "name") {
			AttrSlot pslot = pslot();
			if (pslot != null && pslot.is_auto_complete)
				return this.autoCompleteSymbol(str);
			ANode parent = parent();
			if (pslot != null && parent instanceof ASTNode)
				return parent.resolveAutoComplete(str, pslot);
			return null;
		}
		return super.resolveAutoComplete(str,slot);
	}
	
	public final boolean checkSymbolMatch(DNode dn) {
		return dn instanceof D;
	}
	
	public Symbol[] autoCompleteSymbol(String str) {
		return autoCompleteSymbol(this, str, this.pslot(), fun (DNode dn)->boolean {
			return checkSymbolMatch(dn);
		});
	}

	public static Symbol[] autoCompleteSymbol(ASTNode resolve_in, String str, AttrSlot for_slot, (DNode)->boolean check) {
		if (str == null)
			str = "";
		int dot = str.indexOf('·');
		if (dot > 0) {
			// check we start with a root package
			String head = str.substring(0,dot).intern();
			String tail = str.substring(dot+1);
			foreach (KievPackage pkg; Env.getRoot().pkg_members; pkg.sname == head) {
				// process as fully-qualified name
				return autoCompleteSymbolFromRoot(resolve_in, pkg, tail, for_slot, check);
			}
			// otherwice resolve the head and then it's sub-bodes
			ResInfo info = new ResInfo(resolve_in,head);
			if (!PassInfo.resolveNameR(resolve_in,info))
				return null;
			if !(info.resolvedDNode() instanceof GlobalDNodeContainer)
				return null;
			return autoCompleteSymbolFromRoot(resolve_in, (GlobalDNodeContainer)info.resolvedDNode(), tail, for_slot, check);
		}
		Vector<Symbol> vect = new Vector<Symbol>();
		ResInfo info = new ResInfo(resolve_in, str, ResInfo.noEquals);
		foreach (PassInfo.resolveNameR(resolve_in,info)) {
			Symbol sym = info.resolvedSymbol();
			if (vect.contains(sym))
				continue;
			// check if this node match
			if (check(sym.dnode)) {
				vect.append(sym);
				continue;
			}
			// check if it's a node from path prefix
			DNode dn = sym.dnode;
			foreach (Class c; scopesForAutoResolve(for_slot); dn != null && c.isAssignableFrom(dn.getClass())) {
				vect.append(sym);
				break;
			}
		}
		return vect.toArray();
	}
	
	private static final Class[] defaultScopesForAutoResolve = { GlobalDNodeContainer.class };
	private static Class[] scopesForAutoResolve(AttrSlot for_slot) {
		if (for_slot == null || for_slot.auto_complete_scopes == null)
			return defaultScopesForAutoResolve;
		return for_slot.auto_complete_scopes;
	}
	
	private static Symbol[] autoCompleteSymbolFromRoot(ASTNode resolve_in, GlobalDNodeContainer scope, String tail, AttrSlot for_slot, (DNode)->boolean check) {
		// check if the scope is a valid node path prefix
		boolean valid_scope = false;
		foreach (Class c; scopesForAutoResolve(for_slot); c.isAssignableFrom(scope.getClass())) {
			valid_scope = true;
			break;
		}
		if (!valid_scope)
			return null;
		int dot = tail.indexOf('·');
	next_scope:
		while (dot > 0) {
			String head = tail.substring(0,dot).intern();
			tail = tail.substring(dot+1);
			DNode dn = null;
			foreach (DNode n; scope.getMembers(); n.sname == head) {
				dn = n;
				break;
			}
			dot = tail.indexOf('·');
			if (dn instanceof GlobalDNodeContainer) {
				scope = (GlobalDNodeContainer)dn;
				valid_scope = false;
				foreach (Class c; scopesForAutoResolve(for_slot); c.isAssignableFrom(scope.getClass()))
					continue next_scope;
				return null;
			}
			return null;
		}
		String head = tail.intern();
		Vector<Symbol> vect = new Vector<Symbol>();
		int flags = ResInfo.noForwards|ResInfo.noSuper|ResInfo.noSyntaxContext|ResInfo.noEquals;
		ResInfo info = new ResInfo(resolve_in,head,flags);
		foreach (scope.resolveNameR(info)) {
			Symbol sym = info.resolvedSymbol();
			if (vect.contains(sym))
				continue;
			// check if this node match
			if (check(sym.dnode)) {
				vect.append(sym);
				continue;
			}
			// check if it's a node from path prefix
			DNode dn = sym.dnode;
			foreach (Class c; scopesForAutoResolve(for_slot); dn != null && c.isAssignableFrom(dn.getClass())) {
				vect.append(sym);
				break;
			}
		}
		return vect.toArray();
	}

	public void resolveSymbol(SeverError sever) {
		return resolveSymbol(sever, fun (DNode dn)->boolean {
			return checkSymbolMatch(dn);
		});
	}

	public void resolveSymbol(SeverError sever, (DNode)->boolean check) {
		String name = this.name;
		if (name == null || name == "") {
			//Kiev.reportAs(sever, this, "Empty reference name");
			return;
		}
		int dot = name.indexOf('·');
		if (dot > 0) {
			// check we start with a root package
			String head = name.substring(0,dot).intern();
			String tail = name.substring(dot+1);
			foreach (KievPackage pkg; Env.getRoot().pkg_members; pkg.sname == head) {
				// process as fully-qualified name
				resolveSymbolFromRoot(sever, pkg, tail, check);
				return;
			}
			// otherwice resolve the head and then it's sub-bodes
			ResInfo info = new ResInfo(this,head);
			if (!PassInfo.resolveNameR(this,info)) {
				Kiev.reportAs(sever, this, "Unresolved identifier "+head);
				return;
			}
			if !(info.resolvedDNode() instanceof GlobalDNodeContainer) {
				Kiev.reportAs(sever, this, "Resolved identifier "+head+" is not a global node container");
				return;
			}
			resolveSymbolFromRoot(sever, (GlobalDNodeContainer)info.resolvedDNode(), tail, check);
			return;
		}
		ResInfo<D> info = new ResInfo(this,name,ResInfo.noForwards);
		if (!PassInfo.resolveNameR(this, info)) {
			Kiev.reportAs(sever, this, "Unresolved "+this);
			return;
		}
		if (!check(info.resolvedDNode())) {
			Kiev.reportAs(sever, this, "Resolved "+this+" does not match required constraints");
			return;
		}
		if (this.symbol != info.resolvedSymbol())
			this.symbol = info.resolvedSymbol();
	}

	private void resolveSymbolFromRoot(SeverError sever, GlobalDNodeContainer scope, String tail, (DNode)->boolean check) {
		int dot = tail.indexOf('·');
		while (dot > 0) {
			String head = tail.substring(0,dot).intern();
			tail = tail.substring(dot+1);
			DNode dn = null;
			foreach (DNode n; scope.getMembers(); n.sname == head) {
				dn = n;
				break;
			}
			dot = tail.indexOf('·');
			if (dn instanceof GlobalDNodeContainer) {
				scope = (GlobalDNodeContainer)dn;
				continue;
			}
			Kiev.reportAs(sever, this, "Resolved identifier "+head+" in "+scope+" is not a global node container");
			return;
		}
		String head = tail.intern();
		ResInfo<D> info = new ResInfo<D>(this,head,ResInfo.noForwards);
		if (!scope.resolveNameR(info)) {
			Kiev.reportAs(sever, this, "Unresolved "+this+" in "+scope);
			return;
		}
		if (!check(info.resolvedDNode())) {
			Kiev.reportAs(sever, this, "Resolved "+this+" does not match required constraints");
			return;
		}
		if (this.symbol != info.resolvedSymbol())
			this.symbol = info.resolvedSymbol();
	}

}


