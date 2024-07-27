/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
 *******************************************************************************/
package kiev.gui;

import java.util.Enumeration;

import kiev.fmt.DrawElemWrapper;
import kiev.fmt.DrawNonTermList;
import kiev.fmt.Drawable;
import kiev.fmt.common.Draw_SyntaxAttr;
import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.INode;

/**
 * Action Point.
 */
public class ActionPoint {
	/** The action editor. */
	public final Editor editor;
	
	/** The drawable for the action. */
	public final Drawable dr;
	
	/** The current node. */
	public final INode curr_node;
	
	/** The current node slot if drawable displays the node's attribute. */
	public final AttrSlot curr_slot;
	
	/** The current Draw_SyntaxAttr for the slot. */
	public final Draw_SyntaxAttr curr_syntax;
	
	/** The parent node for space actions. */
	public final INode space_node;
	
	/** The parent's node space slot. */
	public final AttrSlot space_slot;
	
	/** The space Draw_SyntaxAttr for the slot. */
	public final Draw_SyntaxAttr space_syntax;
	
	/** The 'previous' insertion position index (insert before). */
	public final int prev_index;
	
	/** The 'next' insertion position index (insert after).
	 */
	public final int next_index;
	
	/** The length of the space. */
	public final int space_length;
	
	/**
	 * The constructor.
	 * @param dr the drawable
	 * @param slot the slot
	 * @param idx the index
	 */
	public ActionPoint(Editor editor, Drawable dr) {
		this.editor = editor;
		this.dr = dr;
		if (dr == null) {
			this.curr_node = null;
			this.curr_slot = null;
			this.curr_syntax = null;
			this.space_node = null;
			this.space_slot = null;
			this.space_syntax = null;
			this.prev_index = -1;
			this.next_index = -1;
			this.space_length = -1;
			return;
		}
		this.curr_node = dr.drnode;
		// find the current attribute slot
		Drawable p = null;
		while (dr != null) {
			p = (Drawable)dr.parent();
			if (p == null || p.syntax instanceof Draw_SyntaxAttr)
				break;
			dr = p;
		}
		if (p == null) {
			this.curr_slot = null;
			this.curr_syntax = null;
			this.space_node = null;
			this.space_slot = null;
			this.space_syntax = null;
			this.prev_index = -1;
			this.next_index = -1;
			this.space_length = -1;
			return;
		}
		AttrSlot attr_slot = getSlotFor(p.drnode, ((Draw_SyntaxAttr)p.syntax).name);
		// check we can use it for 'curr_slot'
		if (p.drnode == this.curr_node && attr_slot != null) {
			this.curr_slot = attr_slot;
			this.curr_syntax = (Draw_SyntaxAttr)p.syntax;
		} else {
			this.curr_slot = null;
			this.curr_syntax = null;
		}
		// check we can use it for 'space_slot'
		if (!(attr_slot instanceof ASpaceAttrSlot)) {
			// find upper space slot
			dr = p;
			while (dr != null) {
				p = (Drawable)dr.parent();
				if (p == null || p.syntax instanceof Draw_SyntaxAttr)
					break;
				dr = p;
			}
		}
		if (p == null) {
			this.space_node = null;
			this.space_slot = null;
			this.space_syntax = null;
			this.prev_index = -1;
			this.next_index = -1;
			this.space_length = -1;
			return;
		}
		attr_slot = getSlotFor(p.drnode, ((Draw_SyntaxAttr)p.syntax).name);
		if (p instanceof DrawNonTermList) {
			DrawNonTermList lst = (DrawNonTermList)p;
			this.space_node = p.drnode;
			this.space_slot = attr_slot;
			this.space_syntax = (Draw_SyntaxAttr)p.syntax;
			this.prev_index = lst.getInsertIndex(dr, false);
			this.next_index = lst.getInsertIndex(dr, true);
			this.space_length = getSpaceLength(this.space_node, attr_slot);
		}
		else if (p instanceof DrawElemWrapper) {
			DrawElemWrapper lst = (DrawElemWrapper)p;
			this.space_node = p.drnode;
			this.space_slot = attr_slot;
			this.space_syntax = (Draw_SyntaxAttr)p.syntax;
			this.prev_index = lst.getInsertIndex(dr, false);
			this.next_index = lst.getInsertIndex(dr, true);
			this.space_length = getSpaceLength(this.space_node, attr_slot);
		}
		else {
			this.space_node = null;
			this.space_slot = null;
			this.space_syntax = null;
			this.prev_index = -1;
			this.next_index = -1;
			this.space_length = -1;
		}
	}
		
	private static AttrSlot getSlotFor(INode node, String aname) {
		if (node == null)
			return null;
		for (AttrSlot attr : node.values()) {
			if (attr.name == aname)
				return attr;
		}
		return null;
	}
	
	private static int getSpaceLength(INode node, AttrSlot slot) {
		if (slot instanceof ASpaceAttrSlot) {
			int length = 0;
			Enumeration en = (Enumeration)((ASpaceAttrSlot)slot).iterate(node);
			while (en.hasMoreElements()) {
				en.nextElement();
				length++;
			}
			return length;
		}
		return -1;
	}
	
}

