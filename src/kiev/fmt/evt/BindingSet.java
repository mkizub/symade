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
	final public  BindingSet⇑ parent_set;
	@nodeAttr 
		public ASTNode∅	members;
	  public String	q_name;	// qualified name
			  
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
		if (q_name != null)
			return q_name;
		ANode p = parent();
		if (p instanceof GlobalDNode)
			q_name = (((GlobalDNode)p).qname()+"\u001f"+sname).intern();
		else
			q_name = sname;
		return q_name;
	}

	public void callbackChildChanged(ChildChangeType ct, AttrSlot attr, Object data) {
		if (attr.name == "sname")
			resetNames();
		super.callbackChildChanged(ct, attr, data);
	}
	public void callbackAttached(ParentInfo pi) {
		if (pi.isSemantic())
			resetNames();
		super.callbackAttached(pi);
	}
	public void callbackDetached(ANode parent, AttrSlot slot) {
		if (slot.isSemantic())
			resetNames();
		super.callbackDetached(parent, slot);
	}

	public boolean includeInDump(String dump, AttrSlot attr, Object val) {
		if (attr.name == "auto_generated_members")
			return false;
		return super.includeInDump(dump, attr, val);
	}

	private void resetNames() {
		q_name = null;
		if (members != null) {
			foreach (BindingSet s; members)
				s.resetNames();
		}
	}
	
	public rule resolveNameR(ISymbol@ node, ResInfo path)
		BindingSet@ set;
	{
		path.checkNodeName(this),
		node ?= this
	;
		node @= members,
		path.checkNodeName(node)
	;
		path.isSuperAllowed(),
		parent_set != null && parent_set.dnode != null,
		path.getPrevSlotName() != "parent_set",
		set ?= parent_set.dnode,
		path.enterSuper() : path.leaveSuper(),
		set.resolveNameR(node,path)
	}
	
	public boolean preResolveIn() {
		this.compiled = null;
		if (parent_set.name != null && parent_set.name != "") {
			BindingSet@ bs;
			if (!PassInfo.resolveNameR(this,bs,new ResInfo(this,parent_set.name)))
				Kiev.reportError(this,"Cannot resolve syntax '"+parent_set.name+"'");
			else if (parent_set.symbol != bs)
				parent_set.symbol = bs;
		}
		return true;
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
		Vector<Compiled_BindingSet> sub_set = new Vector<Compiled_BindingSet>();
		foreach(BindingSet set; this.members)
			sub_set.append(set.getCompiled());
		bs.sub_set = sub_set.toArray();
	}
}


