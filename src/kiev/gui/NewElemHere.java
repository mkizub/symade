package kiev.gui;

import kiev.fmt.DrawNodeTerm;
import kiev.fmt.DrawPlaceHolder;
import kiev.fmt.Draw_SyntaxAttr;
import kiev.fmt.Draw_SyntaxList;
import kiev.fmt.Draw_SyntaxPlaceHolder;
import kiev.fmt.Drawable;
import kiev.gui.swing.NewElemEditor;
import kiev.vtree.ANode;

public final class NewElemHere extends NewElemEditor implements Runnable {
	public NewElemHere(Editor editor) { super(editor); }
	public static Factory newFactory(){
		return new Factory();
	}
	public void run() {
		Drawable dr = getEditor().cur_elem.dr;
		if (dr instanceof DrawPlaceHolder && ((Draw_SyntaxPlaceHolder)dr.syntax).parent_syntax_attr != null) {
			ANode n = dr.get$drnode();
			Draw_SyntaxAttr satt = ((Draw_SyntaxPlaceHolder)dr.syntax).parent_syntax_attr;
			makeMenu("Set new item", n, satt);
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
			Draw_SyntaxList slst = (Draw_SyntaxList)ap.dr.syntax;
			setIdx(ap.index);
			makeMenu("Insert new item", ap.node, slst);
			return;
		}
	}
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Create a new element at this position"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			Drawable dr = context.dr;
			if (dr instanceof DrawPlaceHolder && ((Draw_SyntaxPlaceHolder)dr.syntax).parent_syntax_attr != null) {
				//ANode n = dr.get$drnode();
				Draw_SyntaxAttr satt = ((Draw_SyntaxPlaceHolder)dr.syntax).parent_syntax_attr;
				if (satt.expected_types == null || satt.expected_types.length == 0)
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
				if (satt.expected_types == null || satt.expected_types.length == 0)
					return null;
				return new NewElemHere(editor);
			}
			ActionPoint ap = editor.getActionPoint(false);
			if (ap == null || ap.length < 0)
				return null;
			Draw_SyntaxList slst = (Draw_SyntaxList)ap.dr.syntax;
			if (slst.expected_types == null || slst.expected_types.length == 0)
				return null;
			return new NewElemHere(editor);
		}
	}
}
