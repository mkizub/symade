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

import kiev.fmt.DrawIdent;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.DrawValueTerm;
import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.vtree.INode;
import kiev.vtree.ScalarAttrSlot;
import kiev.vtree.Symbol;
import kiev.vtree.SymbolRef;

/**
 * Edit Actions UI Action.
 */
public final class EditActions implements UIAction {
	
	/**
	 * The editor.
	 */
	private final Editor editor;
	
	/**
	 * The action.
	 */
	private final String action;
	
	/**
	 * The constructor.
	 * @param editor the editor
	 * @param action the action
	 */
	public EditActions(Editor editor, String action) {
		this.editor = editor;
		this.action = action;
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.UIAction#run()
	 */
	public void exec() {
		if (action == "close") {
			if (editor.isInTextEditMode())
				editor.stopTextEditMode();
			editor.setSelectedNode(null); 
			editor.setDrawTerm(null); 
			editor.window.closeEditor(editor);
		}
		else if (action == "undo") {
			editor.getWindow().undoTransaction();
			editor.formatAndPaint(true);
		}
		else if (action == "del") {
			DrawTerm dt = editor.getDrawTerm();
			INode node = editor.getSelectedNode();
			editor.getWindow().startTransaction(editor, "Actions.java:del");
			boolean succ = true;
			try {
				if (dt instanceof DrawIdent) {
					Object obj = ((DrawIdent)dt).getAttrObject();
					if (obj instanceof SymbolRef) {
						SymbolRef sr = (SymbolRef)obj;
						if (sr.getName() == null)
							sr.detach();
						else
							sr.setName(null);
					}
					else if (node instanceof Symbol) {
						Symbol sym = (Symbol)obj;
						if (sym.getSname() != null)
							sym.setSname(null);
						else
							node.detach();
					}
					else if (node instanceof SymbolRef) {
						SymbolRef sr = (SymbolRef)node;
						if (sr.getName() == null)
							sr.detach();
						else
							sr.setName(null);
					}
					else
						node.detach();
				}
				else if (dt instanceof DrawValueTerm) {
					Object obj = ((DrawValueTerm)dt).getAttrObject();
					if (obj != null)
						((DrawValueTerm)dt).setValue(null);
					else
						node.detach();
				}
				else {
					node.detach();
				}
			} catch (Exception e) {
				succ = false;
				e.printStackTrace();
			}
			editor.getWindow().stopTransaction(!succ);
			editor.formatAndPaint(true);
		}
		else if (action == "cut") {
			INode node = editor.getSelectedNode();
			editor.getWindow().startTransaction(editor, "Actions.java:cut");
			boolean succ = true;
			try {
				node.detach();
			} catch (Exception e) {
				succ = false;
				e.printStackTrace();
			}
			editor.getWindow().stopTransaction(!succ);
			if (succ)
				UIManager.setClipboardContent(node);
			editor.formatAndPaint(true);
		}
		else if (action == "copy") {
			//DrawTerm dr = editor.getDrawTerm();
			//if (dr instanceof DrawValueTerm) {
			//	Object obj = ((DrawValueTerm)dr).getAttrObject();
			//	UIManager.setClipboardContent(obj);
			//} else {
				UIManager.setClipboardContent(editor.getSelectedNode());
			//}
		}
		else if (action == "placeholder") {
			DrawPlaceHolder ph = (DrawPlaceHolder)editor.getDrawTerm();
			SymbolRef sr = (SymbolRef)ph.getScalarPtr().get();
			sr.setVal(sr.getAttrSlot("name"), "");
			editor.formatAndPaint(true);
		}
	}


	/**
	 * Undo last change.
	 */
	public final static class Undo implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Undo last change"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getWindow().getCurrentUndoEditor() == context.editor)
				return new EditActions(context.editor, "undo");
			return null;
		}
	}
	
	/**
	 * Cut current node.
	 */
	public final static class Cut implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Cut current node"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getDrawTerm() != null)
				return new EditActions(context.editor, "cut");
			return null;
		}
	}
	
	/**
	 * Delete current node.
	 */
	public final static class Del implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Delete current node"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getDrawTerm() != null) {
				if ((context.editor.isInInsertMode() || context.editor.isInTextEditMode()) && context.editor.getDrawTerm() instanceof DrawValueTerm)
					return null;
				return new EditActions(context.editor, "del");
			}
			return null;
		}
	}
	
	/**
	 * Copy current node.
	 */
	public final static class Copy implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Copy current node"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return true; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getDrawTerm() != null)
				return new EditActions(context.editor, "copy");
			return null;
		}
	}
	
	/**
	 * Close the editor window.
	 */
	public final static class CloseWindow implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Close the editor window"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor != null) return new EditActions(context.editor, "close");
			return null;
		}
	}

	/**
	 * Choose Item Editor.
	 */
	public static final class ChooseItemEditor implements UIActionFactory {
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getDescr()
		 */
		public String getDescr() { return "Edit current element"; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#isForPopupMenu()
		 */
		public boolean isForPopupMenu() { return false; }
		
		/* (non-Javadoc)
		 * @see kiev.gui.UIActionFactory#getAction(kiev.gui.UIActionViewContext)
		 */
		public UIAction getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			if (context.dt == null || context.node == null)
				return null;
			if (context.dt.drnode != context.node)
				return null;
			Drawable dr = context.dt;
			if (dr instanceof DrawValueTerm)
				return new TextEditor(editor, (DrawValueTerm)dr);
			if (context.ap.curr_node != null && context.ap.curr_slot instanceof ScalarAttrSlot) {
				Class clazz = context.ap.curr_slot.typeinfo.clazz;
				if (clazz == Boolean.TYPE || clazz == Boolean.class || Enum.class.isAssignableFrom(clazz))
					return new EnumEditor(editor, context.ap);
				if (clazz == SymbolRef.class && dr instanceof DrawPlaceHolder)
					return new EditActions(context.editor, "placeholder");
			}
			return null;
		}
	}	
}
