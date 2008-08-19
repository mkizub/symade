package kiev.gui;

import kiev.fmt.*;
import kiev.gui.ActionPoint;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ANode;

public final class NewElemHere extends NewElemEditor implements Runnable {

	public NewElemHere(Editor editor) { super(editor); }

	public void run() {
		Drawable dr = editor.getCur_elem().dr;
		if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
			ANode n = dr.drnode;
			makeMenu("Set new item", n, (Draw_SyntaxPlaceHolder)dr.syntax, dr.text_syntax);
			return;
		}
		if (dr instanceof DrawNodeTerm && (dr.drnode == null || ((DrawNodeTerm)dr).getAttrObject() == null)) {
			ANode n = dr.drnode;
			while (n == null) {
				dr = (Drawable)dr.parent();
				n = dr.drnode;
			}
			Draw_SyntaxAttr satt = (Draw_SyntaxAttr)dr.syntax;
			makeMenu("Set new item", n, satt, dr.text_syntax);
			return;
		}
		ActionPoint ap = editor.getActionPoint(false);
		if (ap != null && ap.length >= 0) {
			Draw_SyntaxElem se = ap.dr.syntax;
			if (se instanceof Draw_SyntaxElemWrapper)
				se = ((Draw_SyntaxElemWrapper)se).element;
			idx = ap.index;
			if (se instanceof Draw_SyntaxAttr)
				makeMenu("Insert new item", ap.node, (Draw_SyntaxAttr)se, dr.text_syntax);
			return;
		}
	}
	
	public final static class Factory implements UIActionFactory {
		public String getDescr() { return "Create a new element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
				ExpectedTypeInfo[] exp = ((Draw_SyntaxPlaceHolder)dr.syntax).getExpectedTypes();
				if (exp == null)
					return null;
				return new NewElemHere(editor);
			}
			if (dr instanceof DrawNodeTerm && (dr.drnode == null || ((DrawNodeTerm)dr).getAttrObject() == null)) {
				ANode n = dr.drnode;
				while (n == null) {
					dr = (Drawable)dr.parent();
					n = dr.drnode;
				}
				Draw_SyntaxAttr satt = (Draw_SyntaxAttr)dr.syntax;
				ExpectedTypeInfo[] exp = satt.getExpectedTypes();
				if (exp == null)
					return null;
				return new NewElemHere(editor);
			}
			ActionPoint ap = editor.getActionPoint(false);
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
				return new NewElemHere(editor);
			}
			return null;
		}
	}
}
