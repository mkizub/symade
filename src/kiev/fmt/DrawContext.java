package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.fmt.IndentKind.*;
import static kiev.fmt.NewLineAction.*;
import static kiev.fmt.SpaceAction.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;


public class DrawContext implements Cloneable {
	
	static class CtxSpaceInfo {
		String	name;
		int		size;
		CtxSpaceInfo(String name, int size) {
			this.name = name;
			this.size = size;
		}
	}
	static class CtxNewLineInfo {
		String	name;
		boolean	group;
		CtxNewLineInfo(String name, boolean group) {
			this.name = name;
			this.group = group;
		}
	}
	static class CtxNewLineTransf {
		CtxNewLineInfo	nli;
		Drawable		dr;
		CtxNewLineTransf(CtxNewLineInfo nli, Drawable dr) {
			this.nli = nli;
			this.dr = dr;
		}
	}
	
	public int						width;
	public int						x, y;
	public boolean					line_started;
	public Vector<CtxSpaceInfo>		space_infos;
	public Vector<CtxNewLineInfo>	newline_infos;
	public Stack<CtxNewLineTransf>	newline_transfers;
	public Stack<Integer>			indents;
	public Stack<DrawNonTerm>		non_terms;
	private DrawTerm				last_term;
	private DrawContext				prev_ctx;
	
	public DrawContext() {
		line_started = true;
		space_infos = new Vector<CtxSpaceInfo>();
		newline_infos = new Vector<CtxNewLineInfo>();
		newline_transfers = new Stack<CtxNewLineTransf>();
		indents = new Stack<Integer>();
		non_terms = new Stack<DrawNonTerm>();
	}
	
	public Object clone() {
		DrawContext dc = (DrawContext)super.clone();
		dc.space_infos = (Vector<CtxSpaceInfo>)this.space_infos.clone();
		dc.newline_infos = (Vector<CtxNewLineInfo>)this.newline_infos.clone();
		dc.newline_transfers = (Stack<CtxNewLineTransf>)this.newline_transfers.clone();
		dc.indents = (Stack<Integer>)this.indents.clone();
		dc.non_terms = (Stack<DrawNonTerm>)this.non_terms.clone();
		return dc;
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
		processNewLineBeforeRequest(nt);
		processSpaceBeforeRequest(nt);
		processUnindentRequest(nt);
		non_terms.push(nt);
	}
	public void popNonTerm(DrawNonTerm nt) {
		DrawNonTerm pop = non_terms.pop();
		assert (nt == pop);
		processSpaceAfterRequest(nt);
		processIndentRequest(nt);
		processNewLineAfterRequest(nt);
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
		processNewLineBeforeRequest(leaf);
		processSpaceBeforeRequest(leaf);
		processUnindentRequest(leaf);
		flushSpaceRequests();
		leaf.geometry.x = x;
		x += leaf.geometry.w;
		line_started = false;
		last_term = leaf;
		processSpaceAfterRequest(leaf);
		processIndentRequest(leaf);
		processNewLineAfterRequest(leaf);
		return (x < width);
	}
	
	private void flushSpaceRequests() {
		int max_space = 0;
		foreach (CtxSpaceInfo csi; space_infos) {
			if (csi.size >= 0)
				max_space = Math.max(csi.size, max_space);
		}
		space_infos.removeAllElements();
		if (line_started)
			this.x = getIndent();
		else
			this.x += max_space;

		int nl = 0;
		foreach (CtxNewLineInfo cnl; newline_infos) {
			if (cnl.group) {
				nl = 2;
				break;
			} else {
				nl = 1;
			}
		}
		newline_infos.removeAllElements();
		if (nl > 0) {
			if (last_term != null)
				last_term.geometry.do_newline = nl;
			this.x = getIndent();
			this.line_started = true;
		}
	}
	
	private void processSpaceBeforeRequest(Drawable dr) {
	next_si:
		foreach (SpaceInfo drsi; dr.syntax.layout.spaces) {
			String name = drsi.name;
			switch (drsi.action) {
			case SP_ADD_BEFORE:
				foreach (CtxSpaceInfo csi; space_infos; csi.name.equals(name)) {
					if (csi.size >= 0)
						csi.size = Math.max(csi.size, drsi.text_size);
					continue next_si;
				}
				space_infos.append(new CtxSpaceInfo(name, drsi.text_size));
				continue next_si;
			case SP_EAT_BEFORE:
				foreach (CtxSpaceInfo csi; space_infos; csi.name.equals(name)) {
					csi.size = -1;
					continue next_si;
				}
				space_infos.append(new CtxSpaceInfo(name, -1));
				continue next_si;
			}
			
		}
	}
	
	private void processSpaceAfterRequest(Drawable dr) {
	next_si:
		foreach (SpaceInfo drsi; dr.syntax.layout.spaces) {
			String name = drsi.name;
			switch (drsi.action) {
			case SP_ADD_AFTER:
				foreach (CtxSpaceInfo csi; space_infos; csi.name.equals(name)) {
					if (csi.size >= 0)
						csi.size = Math.max(csi.size, drsi.text_size);
					continue next_si;
				}
				space_infos.append(new CtxSpaceInfo(name, drsi.text_size));
				continue next_si;
			case SP_EAT_AFTER:
				foreach (CtxSpaceInfo csi; space_infos; csi.name.equals(name)) {
					csi.size = -1;
					continue next_si;
				}
				space_infos.append(new CtxSpaceInfo(name, -1));
				continue next_si;
			}
			
		}
	}
	
	private void processNewLineBeforeRequest(Drawable dr) {
	next_nl:
		foreach (NewLineInfo nli; dr.syntax.layout.new_lines) {
			String name = nli.name;
			switch (nli.action) {
			case NL_ADD_BEFORE:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					continue next_nl;
				}
				newline_infos.append(new CtxNewLineInfo(name, false));
				continue next_nl;
			case NL_ADD_GROUP_BEFORE:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					cnl.group = true;
					continue next_nl;
				}
				newline_infos.append(new CtxNewLineInfo(name, true));
				continue next_nl;
			case NL_DEL_BEFORE:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					newline_infos.removeElement(cnl);
					continue next_nl;
				}
				continue next_nl;
			case NL_DEL_GROUP_BEFORE:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					cnl.group = false;
					continue next_nl;
				}
				continue next_nl;
			case NL_TRANSFER:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					newline_infos.removeElement(cnl);
					newline_transfers.push(new CtxNewLineTransf(cnl, dr));
					continue next_nl;
				}
				continue next_nl;
			}
		}
	}
	
	private void processNewLineAfterRequest(Drawable dr) {
	next_nl:
		foreach (NewLineInfo nli; dr.syntax.layout.new_lines) {
			String name = nli.name;
			switch (nli.action) {
			case NL_ADD_AFTER:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					continue next_nl;
				}
				newline_infos.append(new CtxNewLineInfo(name, false));
				continue next_nl;
			case NL_ADD_GROUP_AFTER:
				foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(name)) {
					cnl.group = true;
					continue next_nl;
				}
				newline_infos.append(new CtxNewLineInfo(name, true));
				continue next_nl;
			}
		}
	next_tr:
		while (!newline_transfers.isEmpty() && newline_transfers.peek().dr == dr) {
			CtxNewLineTransf tr = newline_transfers.pop();
			foreach (CtxNewLineInfo cnl; newline_infos; cnl.name.equals(tr.nli.name)) {
				cnl.group = tr.nli.group;
				continue next_tr;
			}
			newline_infos.append(tr.nli);
		}
	}
	
	private void processUnindentRequest(Drawable dr) {
		if (dr.syntax.layout.indent == INDENT_KIND_UNINDENT) {
			if (!indents.isEmpty())
				indents.pop();
		}
	}
	
	private void processIndentRequest(Drawable dr) {
		if (dr.syntax.layout.indent == INDENT_KIND_FIXED_SIZE) {
			indents.push(new Integer(this.getIndent() + 8));
		}
	}
	
	private int getIndent() {
		if (indents.isEmpty()) return 0;
		return indents.peek().intValue();
	}
}


