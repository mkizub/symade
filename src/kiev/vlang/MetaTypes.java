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
 *
 */

public metatype NodeSpace<N extends ANode> extends N[] {
	
	@macro
	public N getVers(int idx) alias xfy operator []
	{
		case Call# self():
			ANode.getVersion( this, idx )
		case SetAccess# self():
			ANode.getVersion( self.obj, self.index )
	}

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
	public void copyFrom(N[] arr)
	{
		case Call# self(IFld# obj):
			getAttr(self.obj.var).copyFrom(self.obj.obj, arr)
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
		return ANode.getVersion(arr[top++]);
	}
	public static boolean contains(ANode[] ar, ANode val) {
		if (val == null)
			return false;
		val = ANode.getVersion(val);
		foreach (N n; ar; val == ANode.getVersion(n))
			return true;
		return false;
	}
}

