package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public class DrawContext implements Cloneable {
	public int					width;
	public int					x, y;
	public int					space_before;
	public int					strength;
	public int					newline_kind;
	public int					indents_stack[];
	public int					indents_stack_depth;
	public Stack<DrawNonTerm>	non_terms;
	private DrawContext			prev_ctx;
	
	public DrawContext() {
		space_before = -1;
		indents_stack = new int[64];
		non_terms = new Stack<DrawNonTerm>();
	}
	
	public Object clone() {
		try { return super.clone(); } catch (Exception e) {return null;}
	}

	public DrawContext pushState() {
		DrawContext ctx = (DrawContext)this.clone();
		ctx.prev_ctx = this;
		return ctx;
	}
	
	public DrawContext popState() {
		return prev_ctx;
	}

	public void pushNonTerm(DrawNonTerm nt) {
		non_terms.push(nt);
	}
	public void popNonTerm(DrawNonTerm nt) {
		DrawNonTerm pop = non_terms.pop();
		assert (nt == pop);
		processNewLineRequest(nt);
	}
	
	public void formatAsText(DrawTerm dr) {
		DrawGeometry dg = dr.geometry;
		dg.x = 0;
		dg.y = 0;
		String text = dr.getText();
		dg.w = text.length();
		dg.h = 1;
		dg.b = 0;

		this.x += dg.w;
	}

	public boolean addLeaf(DrawTerm leaf) {
		if (leaf.getExtraSpaceLeft() < 0)
			space_before = 0;
		else if (space_before < 0)
			space_before = 0;
		else
			space_before = Math.max(space_before, leaf.getExtraSpaceLeft());
		processNewLineRequest(leaf);
		leaf.geometry.x = x + space_before;
		x += space_before + leaf.geometry.w;
		space_before = leaf.getExtraSpaceRight();
		processIndentRequest(leaf);
		return (x < width);
	}
	
	private void processNewLineRequest(Drawable dr) {
		SyntaxElem le = dr.layout;
		int t_nl = dr.getNewlineKind();
		if (newline_kind != SyntaxElem.NL_NONE) {
			if ((newline_kind & SyntaxElem.NL_MASK_DISCARDABLE) != 0) {
				if ((t_nl & SyntaxElem.NL_MASK_DO_DISCARD) != 0) {
					if ((newline_kind & SyntaxElem.NL_MASK_DOUBLE) != 0) {
						if ((t_nl & SyntaxElem.NL_MASK_DO_DISCARD_DBL) != 0)
							newline_kind = SyntaxElem.NL_NONE;
						else
							newline_kind &= ~SyntaxElem.NL_MASK_DOUBLE;
					} else {
						newline_kind = SyntaxElem.NL_NONE;
					}
				}
			}
			if ((newline_kind & SyntaxElem.NL_MASK_TRANSFERABLE) != 0) {
				if ((t_nl & SyntaxElem.NL_MASK_DO_TRANSFER) != 0) {
					t_nl |= newline_kind;
					newline_kind = SyntaxElem.NL_NONE;
				}
			}
		}
		if (newline_kind != SyntaxElem.NL_NONE) {
			DrawTerm l;
			if (dr instanceof DrawTerm)
				l = dr.getPrevLeaf();
			else
				l = dr.getLastLeaf();
			if ((newline_kind & SyntaxElem.NL_MASK_DOUBLE) != 0)
				l.geometry.do_newline = 2;
			else
				l.geometry.do_newline = 1;
			x = getIndent();
			space_before = 0;
		}
		newline_kind = t_nl & 0xFF;
	}
	
	private void processIndentRequest(Drawable dr) {
		int ik = dr.getIndentKind();
		if (ik != SyntaxElem.INDENT_KIND_NONE) {
			if (ik == SyntaxElem.INDENT_KIND_FIXED_SIZE) {
				// find previous fixed-size indent
				int prev_indent = 0;
				for (int i=indents_stack_depth-1; i >= 0; i--) {
					if ((indents_stack[i] & 1) != 0) {
						prev_indent = indents_stack[i] >> 1;
						break;
					}
				}
				indents_stack[++indents_stack_depth] = ((prev_indent + 10) << 1)+1; 
			}
			else if (ik == SyntaxElem.INDENT_KIND_TOKEN_SIZE) {
				int prev_indent = indents_stack[indents_stack_depth] >> 1;
				indents_stack[++indents_stack_depth] =
					(prev_indent + dr.geometry.w + dr.getExtraSpaceRight()) << 1; 
			}
			else if (ik == SyntaxElem.INDENT_KIND_UNINDENT) {
				if (indents_stack_depth > 0)
					indents_stack_depth--;
				else
					System.err.println("underflow unindent stack");
			}
		}
	}
	
	private int getIndent() {
		return indents_stack[indents_stack_depth] >> 1;
	}
}


