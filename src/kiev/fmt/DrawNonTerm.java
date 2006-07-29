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
	public Drawable[] args;
	
	public DrawNonTerm() {}
	public DrawNonTerm(ANode node, SyntaxElem syntax) {
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

	public final boolean postFormat(DrawContext context, boolean parent_last_layout) {
		context.pushDrawable(this);
		try {
			// for each possible layout. assign it to all sub-components
			// and try to layout them;
			final int layouts_size = syntax.lout.count;
		next_layout:
			for (int i=0; i < layouts_size; i++) {
				context = context.pushState(i);
				boolean save = false;
				boolean fits = true;
				try {
					boolean last = (i == layouts_size-1);
					fits = (context.x < context.width);
					for (int j=0; j < args.length; j++) {
						Drawable dr = args[j];
						if (dr.isUnvisible())
							continue;
						fits &= dr.postFormat(context, last && parent_last_layout);
						if (!fits && !last)
							continue next_layout;
					}
					save = true;
				} finally {
					context = context.popState(save); 
				}
				return fits;
			}
		} finally {
			context.popDrawable(this);
		}
		return false;
	}

}

@node
public class DrawNonTermList extends DrawNonTerm {

	@att public boolean draw_optional;
	ANode[] oarr;
	
	public DrawNonTermList() {}
	public DrawNonTermList(ANode node, SyntaxList syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
		}
		SyntaxList slst = (SyntaxList)this.syntax;
		ANode[] narr = (ANode[])oarr;
		try {
			oarr = (ANode[])node.getVal(slst.name);
		} catch (RuntimeException e) {
			oarr = new ANode[0];
		}
		if (narr != oarr) {
			narr = (ANode[])oarr;
			int sz = narr.length;
			if (sz == 0) {
				if (args.length != 1) {
					args.delAll();
					args.append(slst.empty.makeDrawable(cont.fmt, node));
				}
			} else {
				Drawable[] old_args = args.delToArray();
				boolean need_sep = false;
			next_node:
				for (int i=0; i < sz; i++) {
					ANode n = narr[i];
					if (n instanceof ASTNode && n.isAutoGenerated())
						continue;
					if (slst.filter != null && !slst.filter.calc(n))
						continue;
					if (need_sep && slst.separator != null)
						args.append(slst.separator.makeDrawable(cont.fmt, null));
					foreach (Drawable dr; old_args; dr.node == n) {
						args.append(dr);
						need_sep = true;
						continue next_node;
					}
					args.append(slst.element.makeDrawable(cont.fmt, n));
					need_sep = true;
				}
			}
		}
		if (this.isUnvisible())
			return;
		if (narr.length == 0) {
			assert(args.length == 0 || args.length == 1);
			if (args.length > 0) {
				if (slst.empty.fmt.is_hidden)
					args[0].geometry.is_hidden = !draw_optional;
				args[0].preFormat(cont,slst.empty,this.node);
			}
		}
		else if (slst.separator != null) {
			for (int i=0; i < args.length; i++) {
				if ((i & 1) == 0)
					args[i].preFormat(cont,slst.element,args[i].node);
				else
					args[i].preFormat(cont,slst.separator,this.node);
			}
		}
		else {
			for (int i=0; i < args.length; i++)
				args[i].preFormat(cont,slst.element,args[i].node);
		}
	}
	
	public int getInsertIndex(Drawable dr) {
		assert (dr.parent() == this);
		if (oarr.length == 0)
			return 0;
		SyntaxList slst = (SyntaxList)this.syntax;
		if (slst.separator != null) {
			for (int i=0; i < args.length; i++) {
				if (args[i] == dr)
					return (1+i)/2;
			}
			return oarr.length;
		}
		for (int i=0; i < args.length; i++) {
			if (args[i] == dr)
				return i;
		}
		return oarr.length;
	}
}

@node
public class DrawNonTermSet extends DrawNonTerm {

	public DrawNonTermSet() {}
	public DrawNonTermSet(ANode node, SyntaxElem syntax) {
		super(node, syntax);
	}

	public void preFormat(DrawContext cont, SyntaxElem expected_stx, ANode expected_node) {
		if (!expected_stx.check(cont, syntax, expected_node, this.node)) {
			Drawable dr = expected_stx.makeDrawable(cont.fmt, expected_node);
			replaceWithNode(dr);
			dr.preFormat(cont, expected_stx, expected_node);
		}
		if (this.isUnvisible())
			return;
		SyntaxSet sset = (SyntaxSet)this.syntax;
		if (args.length != sset.elements.length) {
			args.delAll();
			foreach (SyntaxElem se; sset.elements)
				args.append(se.makeDrawable(cont.fmt, this.node));
		}
		for (int i=0; i < args.length; i++) {
			Drawable dr = args[i];
			dr.preFormat(cont,sset.elements[i],this.node);
		}
	}
}


