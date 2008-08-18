package kiev.gui;

import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxElem;
import kiev.fmt.Draw_SyntaxList;
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
			Draw_SyntaxList slst;
			//if (se instanceof Draw_SyntaxElemWrapper)
			//	slst = ((Draw_SyntaxElemWrapper)se).list;
			//else
				slst = (Draw_SyntaxList)ap.dr.syntax;
			idx = ap.index;
			makeMenu("Append new item", ap.node, slst, ap.dr.text_syntax);
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
			Draw_SyntaxAttr slst = (Draw_SyntaxAttr)ap.dr.syntax;
			ExpectedTypeInfo[] exp = slst.getExpectedTypes();
			if (exp == null)
				return null;
			return new NewElemNext(editor);
		}
	}
}
