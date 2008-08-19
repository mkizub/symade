package kiev.gui;

import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxElem;
import kiev.fmt.Draw_SyntaxElemWrapper;
import kiev.fmt.ExpectedTypeInfo;
import kiev.gui.ActionPoint;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

public final class NewElemNext extends NewElemEditor implements Runnable {
	NewElemNext(Editor editor) { super(editor); }

	public void run() {
		ActionPoint ap = editor.getActionPoint(true);
		if (ap != null && ap.length >= 0) {
			Draw_SyntaxElem se = ap.dr.syntax;
			if (se instanceof Draw_SyntaxElemWrapper)
				se = ((Draw_SyntaxElemWrapper)se).element;
			idx = ap.index;
			if (se instanceof Draw_SyntaxAttr)
				makeMenu("Append new item", ap.node, (Draw_SyntaxAttr)se, ap.dr.text_syntax);
			return;
		}
	}
	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Create a new element at next position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			ActionPoint ap = editor.getActionPoint(true);
			if (ap == null || ap.length < 0)
				return null;
			Draw_SyntaxElem se = ap.dr.syntax;
			if (se instanceof Draw_SyntaxElemWrapper)
				se = ((Draw_SyntaxElemWrapper)se).element;
			if (se instanceof Draw_SyntaxAttr) {
				Draw_SyntaxAttr satt = (Draw_SyntaxAttr)se;
				ExpectedTypeInfo[] exp = satt.getExpectedTypes();
				if (exp == null)
					return null;
				return new NewElemNext(editor);
			}
			return null;
		}
	}
}
