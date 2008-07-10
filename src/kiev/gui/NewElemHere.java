package kiev.gui;

import kiev.fmt.*;
import kiev.gui.swing.NewElemEditor;
import kiev.vtree.ANode;

public final class NewElemHere extends NewElemEditor implements Runnable {

	public NewElemHere(Editor editor) { super(editor); }

	public void run() {
		Drawable dr = getEditor().getCur_elem().dr;
		if (dr instanceof DrawPlaceHolder && dr.syntax.elem_decl != null && ((Draw_SyntaxPlaceHolder)dr.syntax).attr_name != null) {
			ANode n = dr.get$drnode();
			makeMenu("Set new item", n, (Draw_SyntaxPlaceHolder)dr.syntax);
			return;
		}
		if (dr instanceof DrawNodeTerm && (dr.get$drnode() == null || ((DrawNodeTerm)dr).getAttrObject() == null)) {
			ANode n = dr.get$drnode();
			while (n == null) {
				dr = (Drawable)dr.parent();
				n = dr.get$drnode();
			}
			Draw_SyntaxAttr satt = (Draw_SyntaxAttr)dr.syntax;
			makeMenu("Set new item", n, satt);
			return;
		}
		ActionPoint ap = getEditor().getActionPoint(false);
		if (ap != null && ap.length >= 0) {
			Draw_SyntaxAttr slst = (Draw_SyntaxAttr)ap.dr.syntax;
			setIdx(ap.index);
			makeMenu("Insert new item", ap.node, slst);
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
				//ANode n = dr.get$drnode();
				ExpectedTypeInfo[] exp = ((Draw_SyntaxPlaceHolder)dr.syntax).getExpectedTypes();
				if (exp == null)
					return null;
				return new NewElemHere(editor);
			}
			if (dr instanceof DrawNodeTerm && (dr.get$drnode() == null || ((DrawNodeTerm)dr).getAttrObject() == null)) {
				ANode n = dr.get$drnode();
				while (n == null) {
					dr = (Drawable)dr.parent();
					n = dr.get$drnode();
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
