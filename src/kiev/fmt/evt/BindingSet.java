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

import kiev.dump.*;

@ThisIsANode
public class BindingSet extends DNode implements GlobalDNodeContainer, ExportXMLDump {
	@nodeAttr @SymbolRefAutoComplete @SymbolRefAutoResolve 
	final
	public BindingSet⇑			parent_set;
	@nodeAttr
	public ASTNode∅				members;
	
	public BindingSet() {}

	public final ASTNode[] getContainerMembers() { this.members }
	
	public String toString() { return "bindings: "+qname(); }

	public String exportFactory() {
		return "kiev.dump.xml.GUIBindingsExportFactory";
	}

	public String qname() {
		String q_name = this.sname;
		INode p = parent();
		if (p instanceof GlobalDNode && !(p instanceof KievRoot))
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
	
}


