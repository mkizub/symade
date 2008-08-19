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

import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.Draw_SyntaxPlaceHolder;
import kiev.fmt.Drawable;
import kiev.vtree.ANode;
import kiev.vtree.AttrSlot;
import kiev.vtree.ExtChildrenIterator;
import kiev.vtree.ExtSpaceAttrSlot;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.ScalarPtr;
import kiev.vtree.SpaceAttrSlot;
import kiev.vtree.Transaction;

public class Clipboard {
	public final static class PasteHereFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			Object obj = UIManager.getClipboardContent();
			if (!(obj instanceof ANode))
				return null;
			ANode node = (ANode)obj;
			// try paste as a node into null
			if (dr instanceof DrawNodeTerm) {
				DrawNodeTerm dt = (DrawNodeTerm)dr;
				ScalarPtr pattr = dt.getScalarPtr();
				if (pattr.get() == null && pattr.slot.typeinfo.$instanceof(node))
					return new PasteElemHere(node, editor, pattr.node, pattr.slot);
			}
			// try paste as a node into placeholder
			if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
				Draw_SyntaxPlaceHolder dsph = (Draw_SyntaxPlaceHolder)dr.syntax;
				ANode drnode = dr.drnode;
				for (AttrSlot attr: drnode.values()) {
					if (attr.name != dsph.attr_name)
						continue;
					if (attr instanceof ScalarAttrSlot && ((ScalarAttrSlot)attr).get(drnode) == null && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
					else if (attr instanceof SpaceAttrSlot && ((SpaceAttrSlot)attr).getArray(drnode).length == 0 && attr.typeinfo.$instanceof(node))
						return new PasteElemHere(node, editor, drnode, attr);
				}
			}
			// try paste as an element of list
			ActionPoint ap = editor.getActionPoint(false);
			if (ap != null && ap.length >= 0 && ap.slot.typeinfo.$instanceof(node)) {
				return new PasteElemHere(node, editor, ap);
			}
			return null;
		}
	}

	public final static class PasteNextFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Object obj = UIManager.getClipboardContent();
			if (!(obj instanceof ANode))
				return null;
			ANode node = (ANode)obj;
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			if (!ap.slot.typeinfo.$instanceof(node))
				return null;
			return new PasteElemNext(node, editor);
		}
	}

	static final class PasteElemHere implements Runnable {
		final ANode       paste_node;
		final Editor      editor;
		final ANode       into_node;
		final AttrSlot    attr_slot;
		final ActionPoint ap;
		PasteElemHere(ANode paste_node, Editor editor, ANode into_node, AttrSlot attr_slot) {
			this.paste_node = paste_node;
			this.editor = editor;
			this.into_node = into_node;
			this.attr_slot = attr_slot;
			this.ap = null;
		}
		PasteElemHere(ANode paste_node, Editor editor, ActionPoint ap) {
			this.paste_node = paste_node;
			this.editor = editor;
			this.into_node = null;
			this.attr_slot = null;
			this.ap = ap;
		}
		public void run() {
			ANode paste_node = this.paste_node;
			editor.changes.push(Transaction.open("Editor.java:PasteElemHere"));
			try {
				if (paste_node.isAttached())
					paste_node = paste_node.ncopy();
				if (attr_slot != null) {
					if (attr_slot instanceof SpaceAttrSlot) {
						if (((SpaceAttrSlot)attr_slot).indexOf(into_node, paste_node) >= 0)
							paste_node = paste_node.ncopy();
						((SpaceAttrSlot)attr_slot).insert(into_node, 0, paste_node);
					}
					else if (attr_slot instanceof ExtSpaceAttrSlot) {
						for (ExtChildrenIterator iter = ((ExtSpaceAttrSlot)attr_slot).iterate(into_node); iter.hasMoreElements();) {
							if (iter.nextElement() == paste_node) {
								paste_node = paste_node.ncopy();
								break;
							}
						}
						((ExtSpaceAttrSlot)attr_slot).add(into_node, paste_node);
					}
					else if (attr_slot instanceof ScalarAttrSlot) {
						if (((ScalarAttrSlot)attr_slot).get(into_node) == paste_node)
							paste_node = paste_node.ncopy();
						((ScalarAttrSlot)attr_slot).set(into_node, paste_node);
					}
				}
				else if (ap != null) {
					if (((SpaceAttrSlot)ap.slot).indexOf(ap.node, paste_node) >= 0)
						paste_node = paste_node.ncopy();
					((SpaceAttrSlot)ap.slot).insert(ap.node,ap.index,paste_node);
				}
			} finally {
				editor.changes.peek().close();
			}
			editor.formatAndPaint(true);
		}
	}

	static final class PasteElemNext implements Runnable {
		final ANode  paste_node;
		final Editor editor;
		PasteElemNext(ANode paste_node, Editor editor) {
			this.paste_node = paste_node;
			this.editor = editor;
		}
		public void run() {
			ANode paste_node = this.paste_node;
			ActionPoint ap = editor.getActionPoint(true);
			editor.changes.push(Transaction.open("Editor.java:PasteElemNext"));
			try {
				if (paste_node.isAttached())
					paste_node = paste_node.ncopy();
				AttrSlot attr_slot = ap.slot;
				if (attr_slot instanceof SpaceAttrSlot) {
					if (((SpaceAttrSlot)attr_slot).indexOf(ap.node, paste_node) >= 0)
						paste_node = paste_node.ncopy();
					((SpaceAttrSlot)attr_slot).insert(ap.node,ap.index,paste_node);
				}
				else if (attr_slot instanceof ExtSpaceAttrSlot) {
					for (ExtChildrenIterator iter = ((ExtSpaceAttrSlot)attr_slot).iterate(ap.node); iter.hasMoreElements();) {
						if (iter.nextElement() == paste_node) {
							paste_node = paste_node.ncopy();
							break;
						}
					}
					((ExtSpaceAttrSlot)attr_slot).add(ap.node, paste_node);
				}
			} finally {
				editor.changes.peek().close();
			}
			editor.formatAndPaint(true);
		}
	}
}
