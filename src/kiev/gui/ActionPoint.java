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
package kiev.gui;

import kiev.fmt.Drawable;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.SpaceAttrSlot;

public class ActionPoint {
	public final Drawable	dr;
	public final ANode		node;
	public final AttrSlot	slot;
	public final int		index;
	public final int		length;
	public ActionPoint(Drawable dr, AttrSlot slot) {
		this.dr = dr;
		this.node = dr.drnode;
		this.slot = slot;
		if (slot instanceof SpaceAttrSlot) {
			this.index = 0;
			this.length = ((SpaceAttrSlot)slot).getArray(node).length;
		} else {
			this.index = -1;
			this.length = -1;
		}
	}
	public ActionPoint(Drawable dr, SpaceAttrSlot slot, int idx) {
		this.dr = dr;
		this.node = dr.drnode;
		this.slot = slot;
		this.length = slot.getArray(node).length;
		if (idx <= 0) {
			this.index = 0;
		} else {
			if (idx >= this.length)
				this.index = this.length;
			else
				this.index = idx;
		}
	}
	public ActionPoint(Drawable dr, ExtSpaceAttrSlot slot, int idx) {
		this.dr = dr;
		this.node = dr.drnode;
		this.slot = slot;
		int length = 0;
		kiev.stdlib.Enumeration en = slot.iterate(node);
		while (en.hasMoreElements()) {
			en.nextElement();
			length++;
		}
		this.length = length;
		if (idx <= 0) {
			this.index = 0;
		} else {
			if (idx >= this.length)
				this.index = this.length;
			else
				this.index = idx;
		}
	}
}

