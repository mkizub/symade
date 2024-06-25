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

@ThisIsANode(lang=CoreLang)
public final class KievSyntax extends DNode implements GlobalDNodeContainer, ScopeOfMethods, CompilationUnit {
	@SymbolRefAutoComplete(scopes={KievPackage})
	@nodeAttr public KievSyntax⇑∅		super_syntax;
	@nodeAttr public ASTNode∅			members;
	
	private Hash<String> allOperators;
	private Hashtable<Method,Opdef> mapMethodsToOpdefs;

	public KievSyntax() {}
	
	public final ASTNode[] getContainerMembers() { this.members }
	
	public String qname() {
		if (sname == null || sname == "")
			return null;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			return (p.qname()+"·"+sname).intern();
		return sname;
	}

	public String toString() {
		String q = qname();
		if (q == null)
			return "<anonymouse>";
		return q.replace('·','.');
	}
	
	public boolean isOperator(String s) {
		if (allOperators == null)
			allOperators = addOperators(new Hash<String>());
		return allOperators.contains(s);
	}
	
	public void getAllOpdefs(Vector<COpdef> opdefs) {
		foreach (Opdef opd; members) {
			COpdef[] copdefs = opd.compile();
			foreach (COpdef c; copdefs)
				opdefs.append(c);
		}
		foreach (SymbolRef<KievSyntax> stx; super_syntax; stx.dnode != null)
			stx.dnode.getAllOpdefs(opdefs);
	}
	
	private void fillOpdefs(Env env, Hashtable<Method,Opdef> map) {
		foreach (Opdef opd; members) {
			foreach (Method⇑ mr; opd.methods; map.get(mr.dnode) == null) {
				Method m = mr.dnode;
				if (m == null)
					continue;
				map.put(m, opd);
				if (m.body instanceof CoreExpr) {
					CoreOperation cop = ((CoreExpr)m.body).getCoreOperation(env);
					if (cop != null && map.get(cop) == null)
						map.put(cop, opd);
				}
			}
		}
		foreach (KievSyntax⇑ sup; super_syntax) {
			KievSyntax stx = sup.dnode;
			if (stx != null)
				stx.fillOpdefs(env, map);
		}
	}
	
	public Opdef getOpdefForMethod(Env env, Method m) {
		if (mapMethodsToOpdefs == null) {
			mapMethodsToOpdefs = new Hashtable<Method,Opdef>();
			fillOpdefs(env, mapMethodsToOpdefs);
		}
		return mapMethodsToOpdefs.get(m);
	}
	
	protected Hash<String> addOperators(Hash<String> tbl) {
		foreach (Opdef opd; members; opd.resolved != null) {
			foreach (OpArgOPER arg; opd.args)
				tbl.put(arg.symbol.sname);
		}
		foreach (SymbolRef<KievSyntax> stx; super_syntax; stx.dnode != null)
			stx.dnode.addOperators(tbl);
		return tbl;
	}

	public rule resolveNameR(ResInfo info)
		ASTNode@ n;
		SymbolRef@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: Resolving name "+info.getName()+" in "+this),
		{
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in "+this),
			info ?= this
		;	// resolve in this syntax
			info @= members
		;	// resolve in imports and opdefs
			n @= members,
			n instanceof Import && n instanceof ScopeOfNames,
			trace( Kiev.debug && Kiev.debugResolve, "In import ("+(info.doImportStar() ? "with star" : "no star" )+"): "+n),
			((ScopeOfNames)n).resolveNameR(info)
		;
			info.getPrevSlotName() != "super_syntax",
			trace(Kiev.debug && Kiev.debugResolve,"KievSyntax: resolving in super-syntax of "+this),
			super_stx @= super_syntax,
			super_stx.dnode instanceof KievSyntax,
			((KievSyntax)super_stx.dnode).resolveNameR(info)
		}
	}

	public rule resolveMethodR(ResInfo info, CallType mt)
		ASTNode@ member;
		SymbolRef<KievSyntax>@	super_stx;
	{
		info.isStaticAllowed(),
		trace(Kiev.debug && Kiev.debugResolve, "Resolving "+info.getName()+" in "+this),
		{
			member @= members,
			member instanceof Method,
			info ?= ((Method)member).equalsByCast(info.getName(),mt,info.env.tenv.tpVoid,info)
		;
			member @= members,
			{
				member instanceof Import && member instanceof ScopeOfMethods,
				((ScopeOfMethods)member).resolveMethodR(info,mt)
			;
				member instanceof ImportSyntax,
				((ImportSyntax)member).resolveMethodR(info,mt)
			}
		;
			super_stx @= super_syntax,
			super_stx.dnode != null,
			super_stx.dnode.resolveMethodR(info,mt)
		}
	}
}

