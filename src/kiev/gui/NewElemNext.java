package kiev.gui;

import kiev.fmt.Draw_SyntaxList;
import kiev.gui.swing.NewElemEditor;

public final class NewElemNext extends NewElemEditor implements Runnable {
	NewElemNext(Editor editor) { super(editor); }
	public static Factory newFactory(){
		return new Factory();
	}
	public void run() {
		ActionPoint ap = getEditor().getActionPoint(true);
		if (ap != null && ap.length >= 0) {
			Draw_SyntaxList slst = (Draw_SyntaxList)ap.dr.syntax;
			setIdx(ap.index);
			makeMenu("Append new item", ap.node, slst);
			return;
		}
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Create a new element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			Draw_SyntaxList slst = (Draw_SyntaxList)ap.dr.syntax;
			if (slst.expected_types == null || slst.expected_types.length == 0)
				return null;
			return new NewElemNext(editor);
		}
	}
}
