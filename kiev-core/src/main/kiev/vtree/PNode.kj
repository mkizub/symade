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
package kiev.vtree;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */


@ThisIsANode
public final class PNode extends ANode {
	
	private static final AttrSlot[] $values = {};

	private final NodeTypeInfo node_type_info;

	public PNode(NodeTypeInfo node_type_info) {
		super(new AHandle(), null);
		this.node_type_info = node_type_info;
	}

	public NodeTypeInfo getNodeTypeInfo() {
		return this.node_type_info;
	}

	public AttrSlot[] values() { this.getNodeTypeInfo().getAllAttributes() }

	public Language getCompilerLang() { this.getNodeTypeInfo().getCompilerLang() }
	public String getCompilerNodeName() { this.getNodeTypeInfo().getCompilerNodeName() }

	public Object copy(CopyContext cc) {
		return this.copyTo(new PNode(this.getNodeTypeInfo()), cc);
	}

}

public class PScalarAttrSlot extends ScalarAttrSlot {
	public PScalarAttrSlot(ScalarAttrSlot slot) {
		super(slot);
	}
	public PScalarAttrSlot(String name, ParentAttrSlot p_attr, TypeInfo ti) {
		super(name, p_attr, ti);
	}
	public final void set(INode parent, Object value) {
		parent.setVal(this, value);
	}
	public final Object get(INode parent) {
		return parent.getVal(this);
	}
	public final void clear(INode parent) {
		return parent.setVal(this, null);
	}
	public final void detach(INode parent, INode old) {
		return parent.setVal(this, null);
	}
}

public class PSpaceAttrSlot extends SpaceAttrSlot<INode> {
	public PSpaceAttrSlot(SpaceAttrSlot slot) {
		super(slot);
	}
	public PSpaceAttrSlot(String name, ParentAttrSlot p_attr) {
		super(name, p_attr, TypeInfo.newTypeInfo(ANode.class,null));
	}
	public INode[] getArray(INode parent) {
		INode[] arr = (INode[])parent.getVal(this);
		if (arr == null)
			return ANode.emptyArray;
		return arr;
	}
	public void setArray(INode parent, Object/*N[]*/ arr) {
		if (arr == null || ((INode[])arr).length == 0)
			parent.setVal(this, ANode.emptyArray);
		else
			parent.setVal(this, arr);
	}
}

public class PExtSpaceAttrSlot extends ExtSpaceAttrSlot<INode> {
	public PExtSpaceAttrSlot(ExtSpaceAttrSlot slot) {
		super(slot);
	}
	public PExtSpaceAttrSlot(String name, ParentAttrSlot p_attr) {
		super(name,p_attr,TypeInfo.newTypeInfo(INode.class,null));
	}
}

