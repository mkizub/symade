package kiev.gui.swing;

import kiev.fmt.*;
import kiev.gui.ActionPoint;
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.vtree.ANode;

public final class NewElemHere extends NewElemEditor implements Runnable {

	public NewElemHere(Editor editor) { super(editor); }

	public void run() {
		Drawable dr = getEditor().getCur_elem().dr;
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
		ActionPoint ap = getEditor().getActionPoint(false);
		if (ap != null && ap.length >= 0) {
			Draw_SyntaxElem se = ap.dr.syntax;
			Draw_SyntaxList slst;
			//if (se instanceof Draw_SyntaxElemWrapper)
			//	slst = ((Draw_SyntaxElemWrapper)se).list;
			//else
				slst = (Draw_SyntaxList)ap.dr.syntax;
			setIdx(ap.index);
			makeMenu("Insert new item", ap.node, slst, dr.text_syntax);
			return;
		}
	}
	
	public static Factory newFactory(){
		return new Factory();
	}

	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Create a new element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
				//ANode n = dr.drnode;
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
			Draw_SyntaxAttr slst = (Draw_SyntaxAttr)ap.dr.syntax;
			ExpectedTypeInfo[] exp = slst.getExpectedTypes();
			if (exp == null)
				return null;
			return new NewElemHere(editor);
		}
	}
}
