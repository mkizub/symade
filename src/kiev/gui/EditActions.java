package kiev.gui;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import kiev.fmt.DrawNodeTerm;
import kiev.vtree.ANode;
import kiev.vtree.Transaction;

public final class EditActions implements Runnable {
	
	final Editor editor;
	final String action;
	
	EditActions(Editor editor, String action) {
		this.editor = editor;
		this.action = action;
	}
	
	public static Undo newUndo(){
		return new Undo();
	}
	
	public static Copy newCopy(){
		return new Copy();
	}

	public static Cut newCut(){
		return new Cut();
	}

	public static Del newDel(){
		return new Del();
	}

	public static CloseWindow newCloseWindow(){
		return new CloseWindow();
	}

	public void run() {
		if (action == "close") {
			if (editor.getItem_editor() != null) {
				editor.stopItemEditor(true);
				editor.setItem_editor(null);
			}
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
				TransferableANode tr = new TransferableANode(node);
				editor.clipboard.setContents(tr, tr);
			}
			editor.formatAndPaint(true);
		}
		else if (action == "copy") {
			if (editor.getCur_elem().dr instanceof DrawNodeTerm) {
				Object obj = ((DrawNodeTerm)editor.getCur_elem().dr).getAttrObject();
				Transferable tr = null;
				if (obj instanceof ANode)
					tr = new TransferableANode((ANode)obj);
				else
					tr = new StringSelection(String.valueOf(obj));
				editor.clipboard.setContents(tr, (ClipboardOwner)tr);
			} else {
				Transferable tr = new TransferableANode(editor.getCur_elem().node);
				editor.clipboard.setContents(tr, (ClipboardOwner)tr);
			}
		}
	}


	final static class Undo implements UIActionFactory {
		public String getDescr() { return "Undo last change"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.changes.size() > 0)
				return new EditActions(context.editor, "undo");
			return null;
		}
	}
	
	final static class Cut implements UIActionFactory {
		public String getDescr() { return "Cut current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "cut");
			return null;
		}
	}
	
	final static class Del implements UIActionFactory {
		public String getDescr() { return "Delete current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "del");
			return null;
		}
	}
	
	final static class Copy implements UIActionFactory {
		public String getDescr() { return "Copy current node"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null && context.editor.getCur_elem().dr != null)
				return new EditActions(context.editor, "copy");
			return null;
		}
	}
	
	final static class CloseWindow implements UIActionFactory {
		public String getDescr() { return "Close the editor window"; }
		public boolean isForPopupMenu() { return false; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor != null)
				return new EditActions(context.editor, "close");
			return null;
		}
	}

	
}
