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
	@att
	public NArr<Drawable> args;
	
	public DrawNonTerm() {}
	public DrawNonTerm(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
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

	public void preFormat(DrawContext cont) {
		if (this.isUnvisible())
			return;
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(cont);
		}
	}

	public void fillLayout(int i) {
		foreach (Drawable dr; args)
			dr.curr_layout = i;
	}

	public final boolean postFormat(DrawContext context, boolean parent_last_layout) {
		context.pushDrawable(this);
		try {
			context = context.pushState(); 
			// for each possible layout. assign it to all sub-components
			// and try to layout them;
			final int layouts_size = syntax.layout.count;
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
						if (parent_last_layout)
							continue next_layout;
						return false;
					}
				}
				if (fits)
					return true;
			}
		} finally {
			context.popDrawable(this);
		}
		return false;
	}

}

@node
public class DrawNonTermList extends DrawNonTerm {

	public DrawNonTermList() {}
	public DrawNonTermList(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	public void init(Formatter fmt) {
		SyntaxList slst = (SyntaxList)this.syntax;
		NArr<ASTNode> narr = (NArr<ASTNode>)node.getVal(slst.name);
		int sz = narr.size();
		boolean need_sep = false;
		for (int i=0; i < sz; i++) {
			ASTNode n = narr[i];
			if (n.isHidden())
				continue;
			if (slst.filter != null && !slst.filter.calc(n))
				continue;
			if (need_sep && slst.separator != null)
				args.append(slst.separator.makeDrawable(fmt, null));
			args.append(slst.element.makeDrawable(fmt, n));
			need_sep = true;
		}
	}

}

@node
public class DrawNonTermSet extends DrawNonTerm {

	public DrawNonTermSet() {}
	public DrawNonTermSet(ASTNode node, SyntaxElem syntax) {
		super(node, syntax);
	}
	public void init(Formatter fmt) {
		SyntaxSet sset = (SyntaxSet)this.syntax;
		foreach (SyntaxElem se; sset.elements)
			args.append(se.makeDrawable(fmt, node));
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


