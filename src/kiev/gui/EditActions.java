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
import kiev.fmt.DrawTerm;
import kiev.vtree.ANode;
import kiev.vtree.Transaction;

public final class EditActions implements Runnable {
	
	final Editor editor;
	final String action;
	
	EditActions(Editor editor, String action) {
		this.editor = editor;
		this.action = action;
	}
	
	public void run() {
		if (action == "close") {
			if (editor.getItem_editor() != null)
				editor.stopItemEditor(true);
			editor.getCur_elem().set(null); 
			editor.parent_window.closeEditor(editor);
			editor.parent_window.getInfo_view().formatAndPaint(true);
		}
		else if (action == "undo") {
			Transaction tr = editor.changes.pop();
			tr.rollback(false);
			editor.formatAndPaint(true);
		}
		else if (action == "cut" || action == "del") {
			ANode node = editor.getCur_elem().node;
			editor.changes.push(Transaction.open("Actions.java:cut"));
			node.detach();
			editor.changes.peek().close();
			if (action == "cut") {
				UIManager.setClipboardContent(node);
			}
			editor.formatAndPaint(true);
		}
		else if (action == "copy") {
			DrawTerm dr = editor.getCur_elem().dr;
			if (dr instanceof DrawNodeTerm) {
				Object obj = ((DrawNodeTerm)dr).getAttrObject();
				UIManager.setClipboardContent(obj);
			} else {
				UIManager.setClipboardContent(editor.getCur_elem().node);
			}
		}
	}


	public final static class Undo implements UIActionFactory {
		public String getDescr() { return "Undo last change"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.changes.size() > 0)
				return new EditActions(context.editor, "undo");
			return null;
		}
	}
	
	public final static class Cut implements UIActionFactory {
		public String getDescr() { return "Cut current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "cut");
			return null;
		}
	}
	
	public final static class Del implements UIActionFactory {
		public String getDescr() { return "Delete current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "del");
			return null;
		}
	}
	
	public final static class Copy implements UIActionFactory {
		public String getDescr() { return "Copy current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "copy");
			return null;
		}
	}
	
	public final static class CloseWindow implements UIActionFactory {
		public String getDescr() { return "Close the editor window"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new EditActions(context.editor, "close");
			return null;
		}
	}

	
}
