package kiev.fmt.common;

import java.awt.Rectangle;

import kiev.fmt.*;

import kiev.fmt.Drawable;

import static kiev.stdlib.Asserts.*;

public class DrawLayoutInfo {

	public static final DrawLayoutInfo[] emptyArray = new DrawLayoutInfo[0];

	private final DrawLayoutInfo parent;
	private final Drawable dr;

	private final Draw_Paragraph par;
	public Draw_Style style;
	private DrawLayoutInfo[] blocks;
	private int length; // sub-blocks count or alternative layouts count after the build close

	public int x_offs;
	public int y_offs;
	public int width;
	public int height;
	public int baseline;

	private DrawLayoutInfo prev_sub_block;
	private DrawLayoutInfo next_sub_block;
	private DrawLayoutInfo prev_dtli;
	private DrawLayoutInfo next_dtli;

	public LayoutSpace space_std;
	public LayoutSpace space_alt;

	private boolean do_newline;
	private boolean has_alt_layout;
	private boolean update_space_sizes;

	DrawLayoutInfo(DrawLayoutInfo parent, Drawable dr) {
		this.parent = parent;
		this.dr = dr;
		this.blocks = DrawLayoutInfo.emptyArray;
		if (this.dr != null) {
			this.par = dr.syntax.par;
			this.dr.dr_dli = this;
		} else {
			this.par = null;
		}
		this.update_space_sizes = true;
	}
	
	public Drawable getDrawable() { return dr; }
	public DrawLayoutInfo[] getBlocks() { return blocks; }
	public Draw_Paragraph getParagraph() { return par; }
	public Draw_Paragraph getEnclosingParagraph() {
		for (DrawLayoutInfo p = parent; p != null; p = p.parent) {
			if (p.par != null)
				return p.par;
		}
		return null;
	}

	public DrawLayoutInfo getParent() {
		return parent;
	}
	
	public boolean isFlow() {
		if (par == null)
			return false;
		return par.flow == ParagraphKind.FLOW || par.flow == ParagraphKind.BLOCK_FLOW;
	}
	public boolean isVertical() {
		if (par == null)
			return false;
		return par.flow == ParagraphKind.VERTICAL || par.flow == ParagraphKind.BLOCK_VERTICAL;
	}
	public boolean isSubBlock() {
		if (dr instanceof DrawTerm)
			return true;
		if (par == null)
			return false;
		if (par.flow == ParagraphKind.BLOCK_FLOW || par.flow == ParagraphKind.BLOCK_HORIZONTAL || par.flow == ParagraphKind.BLOCK_VERTICAL)
			return true;
		return false;
	}

	private void addBlock(DrawLayoutInfo dli) {
		assert (dli != null);
		if (length >= blocks.length) {
			int len = blocks.length;
			if (len < 4) len = 4;
			else if (len < 8) len = 8;
			else len = len + 8;
			DrawLayoutInfo[] tmp = new DrawLayoutInfo[len];
			for (int i=0; i < blocks.length; i++)
				tmp[i] = blocks[i];
			blocks = tmp;
		}
		if (blocks[length] != dli) {
			blocks[length] = dli;
		}
		length++;
		return;
	}
	
	public Rectangle getBounds() {
		return new Rectangle(getX(),getY(),width,height);
	}

	public final boolean hasAltLayout() {
		return has_alt_layout;
	}
	public final boolean isDoNewline() {
		return do_newline;
	}
	public void setDoNewline(boolean val) {
		do_newline = val;
	}
	
	public final int getX() {
		DrawLayoutInfo p = parent;
		while (p != null && !p.isSubBlock())
			p = p.parent;
		if (p == null)
			return this.x_offs;
		return p.getX() + x_offs;
	}
	public final int getY() {
		DrawLayoutInfo p = parent;
		while (p != null && !p.isSubBlock())
			p = p.parent;
		if (p == null)
			return this.y_offs;
		return p.getY() + y_offs;
	}
	public final void setX(int x) {
		DrawLayoutInfo p = parent;
		while (p != null && !p.isSubBlock())
			p = p.parent;
		if (p == null)
			this.x_offs = x;
		else
			this.x_offs = x - p.getX();
	}
	public final void setY(int y) {
		DrawLayoutInfo p = parent;
		while (p != null && !p.isSubBlock())
			p = p.parent;
		if (p == null)
			this.y_offs = y;
		else
			this.y_offs = y - p.getY();
	}
	
	public final DrawLayoutInfo getPrevSubBlock() { return prev_sub_block; }
	public final DrawLayoutInfo getNextSubBlock() { return next_sub_block; }
	public final void setPrevSubBlock(DrawLayoutInfo dli) { prev_sub_block = dli; }
	public final void setNextSubBlock(DrawLayoutInfo dli) { next_sub_block = dli; }

	public final DrawLayoutInfo getPrevLeaf() { return prev_dtli; }
	public final DrawLayoutInfo getNextLeaf() { return next_dtli; }
	public void setPrevLeaf(DrawLayoutInfo dli) { prev_dtli = dli; }
	public void setNextLeaf(DrawLayoutInfo dli) { next_dtli = dli; }

	public DrawLayoutInfo pushDrawable(Drawable dr) {
		if (dr.syntax != null) {
			Draw_Paragraph pl = dr.syntax.par;
			if (pl != null) {
				DrawLayoutInfo dlb = dr.dr_dli;
				if (dlb == null || dlb.parent != this) {
					dlb = new DrawLayoutInfo(this, dr);
				} else {
					assert (dlb.dr == dr);
					assert (dlb.par == pl);
				}
				dlb.length = 0;
				addBlock(dlb);
				return dlb;
			}
		}
		assert (dr.dr_dli == null);
		return this;
	}
	public DrawLayoutInfo popDrawable(Drawable dr) {
		if (dr.syntax != null) {
			Draw_Paragraph pl = dr.syntax.par;
			if (pl != null) {
				assert (this.dr == dr && this.par == pl);
				closeBuild();
				return this.parent;
			}
		}
		assert (dr.dr_dli == null);
		return this;
	}
	public void closeBuild() {
		if (blocks.length != length) {
			DrawLayoutInfo[] tmp = new DrawLayoutInfo[length];
			for (int i=0; i < length; i++)
				tmp[i] = blocks[i];
			blocks = tmp;
		}
		if (par != null) {
			if (par.flow == ParagraphKind.HORIZONTAL || par.flow == ParagraphKind.BLOCK_HORIZONTAL) {
				boolean alt_layout = space_alt != null;
				for (DrawLayoutInfo b : blocks)
					alt_layout = alt_layout || b.has_alt_layout;
				this.has_alt_layout = alt_layout;
			}
		}
	}
	
	public void addLeaf(DrawTerm dt) {
		DrawLayoutInfo dlb = dt.dr_dli;
		if (dlb == null || dlb.parent != this) {
			dlb = new DrawLayoutInfo(this, dt);
		} else {
			assert (dlb.dr == dt);
			assert (dlb.par == dt.syntax.par);
		}
		addBlock(dlb);
	}
	
	public DrawLayoutInfo getFirstSubBlock() {
		for (DrawLayoutInfo d : this.getBlocks()) {
			if (d.isSubBlock())
				return d;
			d = d.getFirstSubBlock();
			if (d != null)
				return d;
		}
		return null;
	}

	public final void lnkFormat(Drawable dr, boolean is_gfx) {
		DrawLayoutInfo last = linkSubBlocks(null);
		if (last == null)
			return;
		last.setNextSubBlock(null);
		DrawLayoutInfo dtli = linkDrawTerms(null);
		dtli.setNextLeaf(null);
		linkSpaceFormats(dr, new DrawLinkContext(is_gfx));
	}
	
	private DrawLayoutInfo linkSubBlocks(DrawLayoutInfo prev) {
		for (DrawLayoutInfo dli : this.getBlocks()) {
			if (dli.isSubBlock()) {
				if (prev != dli.getPrevSubBlock()) {
					if (prev != null)
						prev.update_space_sizes = true;
					dli.setPrevSubBlock(prev);
				}
				if (!(dli.dr instanceof DrawTerm))
					dli.linkSubBlocks(null);
				if (prev != null)
					prev.setNextSubBlock(dli);
				prev = dli;
			} else {
				prev = dli.linkSubBlocks(prev);
			}
		}
		return prev;
	}
	
	private DrawLayoutInfo linkDrawTerms(DrawLayoutInfo prev) {
		if (dr instanceof DrawTerm) {
			if (prev != null)
				prev.setNextLeaf(this);
			this.setPrevLeaf(prev);
			return this;
		}
		for (DrawLayoutInfo dli : this.getBlocks()) {
			prev = dli.linkDrawTerms(prev);
		}
		return prev;
	}
	
	private static void linkSpaceFormats(Drawable dr, DrawLinkContext cont) {
		if (dr.isUnvisible())
			return;
		DrawLayoutInfo dli = dr.dr_dli;
		
		cont.processSpaceBefore(dr);
		if (dli == null || !dli.isSubBlock()) {
			for (Drawable arg : dr.getChildren()) {
				if (arg != null)
					linkSpaceFormats(arg, cont);
			}
		} else {
			DrawLayoutInfo plnk = dli.getPrevSubBlock();
			if (plnk != null && plnk.update_space_sizes) {
				cont.flushSpace(plnk);
				plnk.update_space_sizes = false;
			}
			if (dli.update_space_sizes) {
				plnk = dli.getNextSubBlock();
				if (plnk != null)
					cont.requestSpacesUpdate();
				else
					dli.update_space_sizes = false;
			}
			if (!(dli.dr instanceof DrawTerm)) {
				DrawLinkContext c = new DrawLinkContext(cont.is_gfx);
				for (Drawable arg : dli.dr.getChildren()) {
					if (arg != null)
						linkSpaceFormats(arg, c);
				}
			}
		}
		cont.processSpaceAfter(dr);
	}

	public final DrawLayoutInfo getFirstLeaf() {
		if (dr instanceof DrawTerm)
			return this;
		DrawLayoutInfo[] blocks = this.getBlocks();
		for (int i=0; i < blocks.length; i++) {
			DrawLayoutInfo ch = blocks[i];
			DrawLayoutInfo d = ch.getFirstLeaf();
			if (d != null)
				return d;
		}
		return null;
	}
	public final DrawLayoutInfo getLastLeaf() {
		if (dr instanceof DrawTerm)
			return this;
		DrawLayoutInfo[] blocks = this.getBlocks();
		for (int i=blocks.length-1; i >= 0 ; i--) {
			DrawLayoutInfo ch = blocks[i];
			DrawLayoutInfo d = ch.getFirstLeaf();
			if (d != null)
				return d;
		}
		return null;
	}
}

