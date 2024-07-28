package kiev.fmt.common;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Vector;

import kiev.fmt.common.Formatter.LstStx;
import kiev.parser.*;
import kiev.vtree.INode;
import kiev.vlang.ENode;
import kiev.vlang.Env;

public class Draw_ATextSyntax implements Serializable {
	private static final long serialVersionUID = 4993170764930753026L;
	
	static final class PairOpdefClass {
		final Opdef opd;
		final Class clazz;
		public PairOpdefClass(Opdef opd, Class clazz) {
			this.opd = opd;
			this.clazz = clazz;
		}
		public int hashCode() {
			final int PRIME = 31;
			int result = 1;
			result = PRIME * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = PRIME * result + ((opd == null) ? 0 : opd.hashCode());
			return result;
		}
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			PairOpdefClass other = (PairOpdefClass) obj;
			if (clazz != other.clazz)
				return false;
			if (opd !=  other.opd)
				return false;
			return true;
		}
		
	}
	
	protected transient Hashtable<String,Draw_SyntaxElem>					badSyntax;
	protected transient Hashtable<String,Draw_SyntaxElem>					allSyntax;
	protected transient Hashtable<PairOpdefClass, Draw_SyntaxElem> 			allSyntaxExprs;

	public Draw_ATextSyntax				parent_syntax;
	public Draw_ATextSyntax[]			sub_syntax;
	public Draw_SyntaxElemDecl[]		declared_syntax_elements;
	public Draw_SyntaxNodeTemplate[]	node_templates;
	public String						q_name;	// qualified name
	public Draw_StyleSheet	style_sheet;

	
	Object readResolve() throws ObjectStreamException {
		if (this.q_name != null) this.q_name = this.q_name.intern();
		return this;
	}

	public Draw_ATextSyntax init() {
		if (allSyntax != null)
			return this;
		badSyntax = new Hashtable<String,Draw_SyntaxElem>();
		allSyntax = new Hashtable<String,Draw_SyntaxElem>();
		allSyntaxExprs = new Hashtable<PairOpdefClass, Draw_SyntaxElem>();
		for (Draw_SyntaxElemDecl sed : declared_syntax_elements) {
			if (sed.node_type_id != null && sed.elem != null)
				allSyntax.put(sed.node_type_id, sed.elem);
		}
		if (style_sheet != null)
			style_sheet.init();
		if (parent_syntax != null)
			parent_syntax.init();
		for (Draw_ATextSyntax stx : sub_syntax)
			stx.init();
		return this;
	}

	private void fillExpr(OpArgument[] args, Draw_SyntaxExpr sexpr, Draw_SyntaxElemDecl elem_decl, Vector<Draw_SyntaxElem> elems) {
	next_arg:
		for (OpArgument arg : args) {
			String arg_attr_name = arg.getAttrName();
			if (arg instanceof OpArgEXPR) {
				elems.add( new Draw_SyntaxAutoParenth(elem_decl, arg_attr_name, ((OpArgEXPR)arg).getPriority(), sexpr.template) );
				continue;
			}
			if (arg instanceof OpArgTYPE) {
				elems.add( new Draw_SyntaxAutoParenth(elem_decl, arg_attr_name, 255, sexpr.template) );
				continue;
			}
			if (arg instanceof OpArgIDNT) {
				Draw_SyntaxSubAttr sa = new Draw_SyntaxSubAttr(elem_decl);
				if (arg_attr_name != null && arg_attr_name.length() > 0)
					sa.name = arg_attr_name;
				else
					sa.name = "ident";
				elems.add( sa );
				continue;
			}
			if (arg instanceof OpArgNODE) {
				Draw_SyntaxSubAttr sa = new Draw_SyntaxSubAttr(elem_decl);
				sa.name = arg_attr_name;
				elems.add( sa );
				continue;
			}
			if (arg instanceof OpArgSEQS) {
				fillExpr(((OpArgSEQS)arg).getSeqArgs(), sexpr, elem_decl, elems);
				continue;
			}
			if (arg instanceof OpArgOPTIONAL) {
				fillExpr(((OpArgOPTIONAL)arg).getOptArgs(), sexpr, elem_decl, elems);
				continue;
			}
			if (arg instanceof OpArgALTR) {
				fillExpr(new OpArgument[]{((OpArgALTR)arg).getAltArgs()[0]}, sexpr, elem_decl, elems);
				continue;
			}
			if (arg instanceof OpArgLIST) {
				Draw_SyntaxList sa = new Draw_SyntaxList(elem_decl);
				sa.name = arg_attr_name;
				sa.element = new Draw_SyntaxNode(elem_decl);
				OpArgOPER sep = ((OpArgLIST)arg).getSeparator();
				if (sep != null)
					((Draw_SyntaxNode)sa.element).sufix = new Draw_SyntaxToken(elem_decl,sep.getText());
				elems.add( sa );
				continue;
			}
			if (arg instanceof OpArgOPER) {
				String text = ((OpArgOPER)arg).getText();
				if (sexpr.template != null) {
					for (Draw_SyntaxToken t : sexpr.template.operators) {
						if (t.text != text) continue;
						if (t.text == "DEFAULT") {
							elems.add( t.copyWithText(text) );
							continue next_arg;
						} else {
							elems.add( t );
						}
					}
				}
				elems.add( new Draw_SyntaxToken(elem_decl,text) );
				continue;
			}
			elems.add( new Draw_SyntaxToken(elem_decl,"???") );
		}
	}
	
	protected Draw_SyntaxSet expr(Opdef opd, Draw_SyntaxExpr sexpr)
	{
		Draw_SyntaxElemDecl elem_decl = new Draw_SyntaxElemDecl();
		Vector<Draw_SyntaxElem> elems = new Vector<Draw_SyntaxElem>();
		fillExpr(opd.getOpdefArgs(), sexpr, elem_decl, elems);
		Draw_SyntaxSet set = new Draw_SyntaxSet(elem_decl);
		set.elements = elems.toArray(new Draw_SyntaxElem[elems.size()]);
		return set;
	}

	public Draw_SyntaxElem getSyntaxElem(INode for_node, LstStx syntax_stack, Env env) {
		if (for_node != null) {
			String cl_name = for_node.getNodeTypeInfo().getId();
			Draw_SyntaxElem elem = allSyntax.get(cl_name);
			if (elem != null) {
				Draw_SyntaxElem se = elem;
				if (se instanceof Draw_SyntaxExpr) {
					if (for_node instanceof ENode) {
						ENode e = (ENode)for_node;
						Opdef opd = e.getFakeOpdef(env);
						if (opd == null)
							return se;
						se = allSyntaxExprs.get(new PairOpdefClass(opd,for_node.getClass()));
						if (se == null) {
							se = expr(opd, (Draw_SyntaxExpr)elem);
							allSyntaxExprs.put(new PairOpdefClass(opd,for_node.getClass()), se);
						}
						return se;
					}
				}
				else
					return se;
			}
		}
		if (parent_syntax != null)
			return parent_syntax.getSyntaxElem(for_node, syntax_stack, env);
		if (syntax_stack != null)
			return syntax_stack.stx.getSyntaxElem(for_node, syntax_stack.tl, env);
		Draw_SyntaxElem se;
		if (for_node == null) {
			se = badSyntax.get("<null>");
			if (se == null) {
				se = new Draw_SyntaxToken(null,"(?null?)");
				badSyntax.put("<null>", se);
			}
		} else {
			//Language lng = for_node.getNodeTypeInfo().getCompilerLang();
			//if (lng != null) {
			//	Draw_ATextSyntax stx = SyntaxManager.getDefaultEditorSyntax(lng);
			//	if (stx != this) {
			//		String text = lng.getClass().getName();
			//		Draw_SyntaxSwitch ssw = (Draw_SyntaxSwitch)badSyntax.get(text);
			//		if (ssw != null)
			//			return ssw;
			//		ssw = new Draw_SyntaxSwitch(null,
			//			new Draw_SyntaxToken(null,"#lang\""+text+"\"{"),
			//			new Draw_SyntaxToken(null,"}#"),
			//			stx
			//			);
			//		badSyntax.put(text, ssw);
			//		return ssw;
			//	}
			//}
			String cl_name = for_node.getNodeTypeInfo().getId();
			se = badSyntax.get(cl_name);
			if (se == null) {
				se = new Draw_SyntaxToken(null,"(?"+cl_name+"?)");
				badSyntax.put(cl_name, se);
			}
		}
		return se;
	}
}

