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

public metatype NodeSpace<N extends ANode> extends N[] {
	
	@macro
	private static ENode# getAttr(Field# f) {
		case Call# self():
			(f.parent).#id"nodeattr$'f'"#	//new #SFldExpr(obj=f.parent, ident="nodeattr$'f'")
	}
	
	@macro
	public N[] delToArray()
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).delToArray(self.obj.obj)
	}

	@macro
	public void delAll()
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).delAll(self.obj.obj)
	}

	@macro
	public void addAll(N[] arr)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).addAll(self.obj.obj, arr)
	}

	@macro
	public void copyFrom(N[] arr, ANode.CopyContext cc)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).copyFrom(self.obj.obj, arr, cc)
	}

	@macro
	public int indexOf(N node)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).indexOf(self.obj.obj, node)
	}

	@macro
	public <R extends N> R set(int idx, R node)
		alias lfy operator []
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).set(self.obj.obj, idx, node)
	}

	@macro
	public N add(N node)
		alias append
		alias lfy operator +=
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).add(self.obj.obj, node)
		case Set# self(IFld# lval):
			getAttr(self.lval.var).add(self.lval.obj, self.value)
	}

	@macro
	public void del(int idx)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).del(self.obj.obj, idx)
	}

	@macro
	public void detach(N node)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).detach(self.obj.obj, node)
	}

	@macro
	public void insert(int idx, N node)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).insert(self.obj.obj, idx, node)
	}
	
	@macro
	public NodeSpaceEnumerator<N> elements()
	{
		case Call# self():
			new NodeSpaceEnumerator(this)
	}

	@macro
	public boolean contains(ANode val) {
		case Call# self():
			NodeSpaceEnumerator.contains(this, val)
	}
}


public class NodeSpaceEnumerator<N extends ANode> implements Enumeration<N>
{

	private N[]		arr;
	private int		top;
	
	public NodeSpaceEnumerator(NodeSpace<N> arr) {
		this.arr = arr;
	}
	
	public boolean hasMoreElements() {
		return arr != null && top < arr.length;
	}
	public N nextElement() {
		return arr[top++];
	}
	public static boolean contains(ANode[] ar, ANode val) {
		if (val == null)
			return false;
		val = val;
		foreach (N n; ar; val == n)
			return true;
		return false;
	}
}

public metatype NodeExtSpace<N extends ANode> extends Object {
	
	@macro
	private static ENode# getAttr(Field# f) {
		case Call# self():
			(f.parent).#id"nodeattr$'f'"#	//new #SFldExpr(obj=f.parent, ident="nodeattr$'f'")
	}
	
	@macro
	public void delAll()
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).delAll(self.obj.obj)
	}

	@macro
	public N add(N node)
		alias append
		alias lfy operator +=
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).add(self.obj.obj, node)
		case Set# self(IFld# lval):
			getAttr(self.lval.var).add(self.lval.obj, self.value)
	}

	@macro
	public void detach(N node)
		alias lfy operator -=
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).detach(self.obj.obj, node)
	}

	@macro
	public NodeExtSpaceEnumerator<N> elements()
	{
		case Call# self(IFld# obj):
			new NodeExtSpaceEnumerator((ANode)self.obj.obj, (ExtSpaceAttrSlot)getAttr(self.obj.var))
	}

	@macro
	public boolean contains(ANode val) {
		case Call# self(IFld# obj):
			new NodeExtSpaceEnumerator((ANode)self.obj.obj, (ExtSpaceAttrSlot)getAttr(self.obj.var)).contains(val)
	}
}

public class NodeExtSpaceEnumerator<N extends ANode> implements Enumeration<N>
{
	public  final ANode             parent;
	public  final ExtSpaceAttrSlot  attr;
	private final Object[]          ext_data;
	private       int               next_pos;
	
	public NodeExtSpaceEnumerator(ANode parent, ExtSpaceAttrSlot attr) {
		this.parent = parent;
		this.attr = attr;
		this.ext_data = parent.ext_data;
		this.next_pos = -1;
		if (ext_data != null)
			setNextPos();
	}
	public boolean hasMoreElements() {
		if (ext_data == null)
			return false;
		return next_pos < ext_data.length;
	}
	public N nextElement() {
		N n = (N)ext_data[next_pos];
		setNextPos();
		return n;
	}
	private void setNextPos() {
		for (next_pos++; next_pos < ext_data.length; next_pos++) {
			Object dat = ext_data[next_pos];
			if (dat instanceof ANode && (attr == null || dat.pslot() == attr))
				return;
		}
	}
	public boolean contains(ANode val) {
		if (val == null)
			return false;
		foreach (Object n; ext_data; n == val)
			return true;
		return false;
	}
}

