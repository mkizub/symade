package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import kiev.vlang.Operator;

import static kiev.fmt.SpaceAction.*;
import static kiev.fmt.SpaceKind.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node
public class TreeSyntax extends TextSyntax {

	final Hashtable<Operator, SyntaxElem> exprs;
	
	private SyntaxSet expr(Operator op, SyntaxJavaExprTemplate template)
	{
		SyntaxElem[] elems = new SyntaxElem[op.args.length];
		int earg = 0;
		for (int i=0; i < elems.length; i++) {
			OpArg arg = op.args[i];
			switch (arg) {
			case OpArg.EXPR(int priority):
				elems[i] = new SyntaxJavaExpr(earg, priority, template);
				earg++;
				continue;
			case OpArg.TYPE():
				elems[i] = new SyntaxJavaExpr(earg, 255, template);
				earg++;
				continue;
			case OpArg.OPER(String text):
				if (template != null) {
					foreach (SyntaxToken t; template.operators) {
						if (t.text == text) {
							elems[i] = t.ncopy();
							break;
						}
						if (t.text == "DEFAULT") {
							SyntaxToken st = t.ncopy();
							st.text = text;
							elems[i] = st;
						}
					}
				}
				if (elems[i] == null)
					elems[i] = new SyntaxToken(text,new SpaceCmd[0]);
				continue;
			}
		}
		SyntaxSet set = new SyntaxSet();
		set.elements.addAll(elems);
		return set;
	}

	public TreeSyntax() {
		exprs = new Hashtable<Operator, SyntaxElem>();
	}

	public SyntaxElem getSyntaxElem(ANode node) {
		String cl_name = node.getClass().getName();
		SyntaxElemDecl sed = allSyntax.get(cl_name);
		if (sed != null) {
			SyntaxElem se = sed.elem;
			if (node instanceof ENode && se instanceof SyntaxJavaExpr && se.name == "") {
				ENode e = (ENode)node;
				Operator op = e.getOp();
				if (op == null)
					return se;
				se = exprs.get(op);
				if (se == null) {
					se = expr(op,(SyntaxJavaExprTemplate)((SyntaxJavaExpr)sed.elem).template.dnode);
					exprs.put(op, se);
				}
			}
			return se;
		}
		SyntaxSet ss = new SyntaxSet();
		ss.folded_by_default = true;
		{
			String name = node.getClass().getName();
			int idx = name.lastIndexOf('.');
			if (idx >= 0)
				name = name.substring(idx+1);
			ss.folded = new SyntaxToken(name,new SpaceCmd[0]);
		}
		foreach (AttrSlot attr; node.values(); attr.is_attr) {
			if (attr.is_space) {
				SyntaxList lst = new SyntaxList(attr.name, new SpaceCmd[0]);
				lst.folded_by_default = true;
				ss.elements += lst;
			} else {
				ss.elements += new SyntaxSubAttr(attr.name, new SpaceCmd[0]);
			}
		}
		SyntaxElemDecl sed = new SyntaxElemDecl();
		sed.elem = ss;
		allSyntax.put(cl_name,sed);
		return ss;
	}
}

