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

import kiev.vtree.AttrSlot;
import kiev.vtree.ASpaceAttrSlot;
import kiev.vtree.Copier;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;

/**
 * The Clipboard Actions and Action Factories.
 */
public class ClipboardActions {
	
	/** Disable instance creation. */
	private ClipboardActions(){}
	
	/**
	 * Paste Here Factory. Paste before selected element.
	 */
	public static final class PasteHereFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null) return null;
			Editor editor = context.editor;
			Object obj = UIManager.getClipboardContent();
			if (! (obj instanceof INode)) return null;
			INode node = (INode)obj;
			ActionPoint ap = editor.getActionPoint();
			if (ap.curr_node != null && ap.curr_slot == null && ap.curr_node.pslot() != null && ap.curr_node.pslot().typeinfo.$instanceof(node))
				return new PasteElemHere(node, editor, ap.curr_node, null, -1);
			if (ap.curr_node != null && ap.curr_slot != null && ap.curr_slot.typeinfo.$instanceof(node))
				return new PasteElemHere(node, editor, ap.space_node, ap.space_slot, ap.prev_index);
			return null;
		}
	}

	/**
	 * Paste Prev Factory. Paste before the selected element. 
	 */
	public static final class PastePrevFactory implements UIActionFactory {
		
		public String getDescr() { return "Paste an element at previous position"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null || context.ap.space_node == null)
				return null;
			Editor editor = context.editor;
			Object obj = UIManager.getClipboardContent();
			if (!(obj instanceof INode))
				return null;
			INode node = context.ap.space_node;
			AttrSlot slot = context.ap.space_slot;
			if (!slot.typeinfo.$instanceof(obj))
				return null;
			return new PasteElemNext((INode)obj, editor, node, slot, context.ap.prev_index);
		}
	}

	/**
	 * Paste Next Factory. Paste after the selected element. 
	 */
	public static final class PasteNextFactory implements UIActionFactory {
		public String getDescr() { return "Paste an element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null || context.ap.space_node == null)
				return null;
			Editor editor = context.editor;
			Object obj = UIManager.getClipboardContent();
			if (!(obj instanceof INode))
				return null;
			INode node = context.ap.space_node;
			AttrSlot slot = context.ap.space_slot;
			if (!slot.typeinfo.$instanceof(obj))
				return null;
			return new PasteElemNext((INode)obj, editor, node, slot, context.ap.next_index);
		}
	}

	/**
	 * Paste Element Here action.
	 */
	public static final class PasteElemHere implements UIAction {
		
		/** Paste node. */
		private final INode paste_node;
		
		/** The editor. */
		private final Editor editor;
		
		/** Into node. */
		private final INode into_node;
		
		/** The attributes slot. */
		private final AttrSlot attr_slot;
		
		/** The index in the space slot. */
		private final int index;
		
		/**
		 * The constructor.
		 * @param paste_node the paste node
		 * @param editor the editor
		 * @param into_node the into node
		 * @param attr_slot the attributes
		 */
		private PasteElemHere(INode paste_node, Editor editor, INode into_node, AttrSlot attr_slot, int idx) {
			this.paste_node = paste_node;
			this.editor = editor;
			this.into_node = into_node;
			this.attr_slot = attr_slot;
			this.index = idx;
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIAction#run()
		 */
		public void exec() {
			INode paste_node = this.paste_node;
			editor.getWindow().startTransaction(editor, "Action:PasteElemHere");
			try {
				if (attr_slot != null) {
					// insert
					if (paste_node.isAttachedBy(attr_slot))
						paste_node = new Copier().copyFull(paste_node);
					if (attr_slot instanceof ASpaceAttrSlot) {
						into_node.insVal(attr_slot, index, paste_node);
					}
					else if (attr_slot instanceof ScalarAttrSlot) {
						into_node.setVal(attr_slot, paste_node);
					}
				}
				else if (into_node != null) {
					// replace
					if (paste_node.isAttached())
						paste_node = new Copier().copyFull(paste_node);
					INode parent = into_node.parent();
					AttrSlot pslot = into_node.pslot();
					if (pslot instanceof ASpaceAttrSlot) {
						Enumeration en = (Enumeration)((ASpaceAttrSlot)pslot).iterate(parent);
						for (int i = 0; en.hasMoreElements(); i++) {
							if (into_node == en.nextElement()) {
								parent.insVal(pslot, i, paste_node);
								break;
							}
						}
					}
					else if (pslot instanceof ScalarAttrSlot) {
						parent.setVal(pslot, paste_node);
					}
				}
			} finally {
				editor.getWindow().stopTransaction(false);
			}
			editor.formatAndPaint(true);
		}
	}

	/**
	 * Paste element next. Paste after the selected element action.
	 */
	public static final class PasteElemNext implements UIAction {
		
		/**
		 * The paste node.
		 */
		private final INode paste_node;
		
		/**
		 * The editor.
		 */
		private final Editor editor;
		
		/**
		 * The parent node into which to paste.
		 */
		private final INode parent;
		
		/**
		 * The slot to paste.
		 */
		private final AttrSlot attr_slot;
		
		/**
		 * The index to paste at.
		 */
		private final int index;
		
		/**
		 * The constructor.
		 * @param paste_node the paste node
		 * @param editor the editor
		 */
		private PasteElemNext(INode paste_node, Editor editor, INode parent, AttrSlot attr_slot, int index) {
			this.paste_node = paste_node;
			this.editor = editor;
			this.parent = parent;
			this.attr_slot = attr_slot;
			this.index = index;
		}
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIAction#exec()
		 */
		public void exec() {
			INode paste_node = this.paste_node;
			editor.getWindow().startTransaction(editor, "Action:PasteElemNext");
			try {
				if (paste_node.isAttachedBy(attr_slot))
					paste_node = new Copier().copyFull(paste_node);
				if (attr_slot instanceof ASpaceAttrSlot) {
					int idx = index;
					int length = 0;
					Enumeration en = (Enumeration)((ASpaceAttrSlot)attr_slot).iterate(parent);
					while (en.hasMoreElements()) {
						length++;
						en.nextElement();
					}
					if (idx < 0) idx = 0;
					if (idx > length) idx = length;
					parent.insVal(attr_slot, idx, paste_node);
				}
			} finally {
				editor.getWindow().stopTransaction(false);
			}
			editor.formatAndPaint(true);
		}
	}
}
