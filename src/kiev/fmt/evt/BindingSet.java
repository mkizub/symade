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
import java.awt.event.KeyEvent;

import kiev.vtree.ASTNode;
import kiev.vtree.DumpSerialized;
import kiev.vlang.GlobalDNodeContainer;

import kiev.gui.event.Item;


@ThisIsANode
public class BindingSet extends DNode implements GlobalDNodeContainer, DumpSerialized  {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve 
	final
	public BindingSet⇑			parent_set;
	@nodeAttr
	public ASTNode∅				members;
	
	@UnVersioned
	protected kiev.gui.event.BindingSet compiled;

	public BindingSet() {}

	public final ASTNode[] getContainerMembers() { this.members }
	
	public String toString() { return "bindings: "+qname(); }

	public Object getDataToSerialize() {
		return this.getCompiled().init();
	}

	public String qname() {
		String q_name = this.sname;
		ANode p = parent();
		if (p instanceof GlobalDNode && p != Env.getRoot())
			q_name = (((GlobalDNode)p).qname()+"·"+sname).intern();
		return q_name;
	}

	public rule resolveNameR(ResInfo path)
	{
		path ?= this
	;
		path @= members
	;
		path.isSuperAllowed(),
		parent_set.dnode != null,
		path.getPrevSlotName() != "parent_set",
		path.enterSuper() : path.leaveSuper(),
		parent_set.dnode.resolveNameR(path)
	}
	
	public boolean preResolveIn() {
		this.compiled = null;
		return super.preResolveIn();
	}
	
	public kiev.gui.event.BindingSet getCompiled() {
		if (compiled != null)
			return compiled;
		compiled = new kiev.gui.event.BindingSet();
		fillCompiled(compiled);
		return compiled;
	}
	
	public void fillCompiled(kiev.gui.event.BindingSet bs) {
		bs.qname = qname().replace('·', '.');
		if (this.parent_set.dnode != null)
			bs.parent_set = parent_set.dnode.getCompiled();
		else if (parent() instanceof BindingSet)
			bs.parent_set = ((BindingSet)parent()).getCompiled();
		Vector<Item> items = new Vector<Item>();
		foreach(BindingSet set; this.members)
			items.append(set.getCompiled());
		foreach(Action act; this.members)
			items.append(act.getCompiled());
		foreach(Binding bnd; this.members)
			items.append(bnd.getCompiled());
		bs.items = items.toArray();
	}
}


