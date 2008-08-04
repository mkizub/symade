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
package kiev.fmt.evt;
import syntax kiev.Syntax;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import kiev.vtree.ASTNode;
import kiev.vtree.DumpSerialized;
import kiev.vlang.GlobalDNodeContainer;


@ThisIsANode
public class BindingSet extends DNode implements GlobalDNodeContainer, DumpSerialized  {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve 
	final
	public BindingSet⇑			parent_set;
	@nodeAttr
	public ASTNode∅				members;
	
	@UnVersioned
	protected Compiled_BindingSet compiled;

	public BindingSet() {
		this.sname = "<binding-set>";
	}

	public final ASTNode[] getMembers() { this.members }

	public Object getDataToSerialize() {
		return this.getCompiled().init();
	}

	public String qname() {
		String q_name = this.sname;
		ANode p = parent();
		if (p instanceof GlobalDNode && p != Env.getRoot())
			q_name = (((GlobalDNode)p).qname()+"\u001f"+sname).intern();
		return q_name;
	}

	public rule resolveNameR(ISymbol@ node, ResInfo path)
	{
		path.checkNodeName(this),
		node ?= this
	;
		node @= members,
		path.checkNodeName(node)
	;
		path.isSuperAllowed(),
		parent_set.dnode != null,
		path.getPrevSlotName() != "parent_set",
		path.enterSuper() : path.leaveSuper(),
		parent_set.dnode.resolveNameR(node,path)
	}
	
	public boolean preResolveIn() {
		this.compiled = null;
		return super.preResolveIn();
	}
		

	public Compiled_BindingSet getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new Compiled_BindingSet();
		fillCompiled(compiled);
		return compiled;
	}
	
	public void fillCompiled(Compiled_BindingSet bs) {
		if (this.parent_set.dnode != null)
			bs.parent_set = parent_set.dnode.getCompiled();
		else if (parent() instanceof BindingSet)
			bs.parent_set = ((BindingSet)parent()).getCompiled();
		Vector<Compiled_Item> items = new Vector<Compiled_Item>();
		foreach(BindingSet set; this.members)
			items.append(set.getCompiled());
		foreach(Action act; this.members)
			items.append(act.getCompiled());
		foreach(Binding bnd; this.members)
			items.append(bnd.getCompiled());
		bs.items = items.toArray();
	}
}


