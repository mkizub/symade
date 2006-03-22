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


@node
public abstract class DrawNonTerm extends Drawable {
	@att NArr<Drawable> args;
	
	public DrawNonTerm() {}
	public DrawNonTerm(ASTNode node, SyntaxElem layout) {
		super(node, layout);
	}

	public Drawable getFirst() {
		if (args.length == 0) return null;
		return args[0];
	}

	public Drawable getLast() {
		if (args.length == 0) return null;
		return args[args.length-1];
	}
	public DrawTerm getFirstLeaf() {
		if (this.isUnvisible())
			return null;
		for (int i=0; i < args.length; i++) {
			DrawTerm d = args[i].getFirstLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}
	public DrawTerm getLastLeaf()  {
		if (this.isUnvisible())
			return null;
		for (int i=args.length-1; i >= 0 ; i--) {
			DrawTerm d = args[i].getLastLeaf();
			if (d != null && !d.isUnvisible())
				return d;
		}
		return null;
	}

	public void fillLayout(int i) {
		foreach (Drawable dr; args)
			dr.curr_layout = i;
	}

	public final boolean postFormat(DrawContext context, boolean parent_last_layout) {
		context.pushNonTerm(this);
		context = context.pushState(); 
		// for each possible layout. assign it to all sub-components
		// and try to layout them;
		final int layouts_size = layout.getLayoutsCount();
	next_layout:
		for (int i=0; i < layouts_size; i++) {
			boolean last = (i == layouts_size-1);
			fillLayout(i);
			context = context.popState(); 
			boolean fits = (context.x < context.width);
			for (int j=0; j < args.length; j++) {
				Drawable dr = args[j];
				if (dr.isUnvisible())
					continue;
				fits &= dr.postFormat(context, last && parent_last_layout);
				if (!fits && !last) {
					//SyntaxElem[] le_next = layouts[i+1];
					//if (le_next.strength < context.strength) {
						if (parent_last_layout)
							continue next_layout;
						context.popNonTerm(this);
						return false;
					//}
				}
			}
			if (fits) {
				context.popNonTerm(this);
				return true;
			}
		}
		context.popNonTerm(this);
		return false;
	}

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(cont);
		}
	}
}

@node
public class DrawNonTermList extends DrawNonTerm {
	public DrawNonTermList() {}
	public DrawNonTermList(ASTNode node, SyntaxElem layout) {
		super(node, layout);
	}
	public void init(Formatter fmt) {
		SyntaxList slst = (SyntaxList)this.layout;
		if (slst.prefix != null)
			args.append(slst.prefix.makeDrawable(fmt, null));
		NArr<ASTNode> narr = (NArr<ASTNode>)node.getVal(slst.element.name);
		int sz = narr.size();
		for (int i=0; i < sz; i++) {
			ASTNode n = narr[i];
			if (slst.elem_prefix != null)
				args.append(slst.elem_prefix.makeDrawable(fmt, null));
			args.append(fmt.getDrawable(n));
			if (slst.elem_suffix != null)
				args.append(slst.elem_suffix.makeDrawable(fmt, null));
			if (i < (sz-1) && slst.separator != null)
				args.append(slst.separator.makeDrawable(fmt, null));
		}
		if (slst.suffix != null)
			args.append(slst.suffix.makeDrawable(fmt, null));
	}
}

@node
public class DrawNonTermSet extends DrawNonTerm {
	public DrawNonTermSet() {}
	public DrawNonTermSet(ASTNode node, SyntaxElem layout) {
		super(node, layout);
	}
	public void init(Formatter fmt) {
		SyntaxSet sset = (SyntaxSet)this.layout;
		foreach (SyntaxElem se; sset.elements)
			args.append(se.makeDrawable(fmt, node));
	}
}

