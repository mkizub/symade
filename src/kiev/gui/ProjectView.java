package kiev.gui;

import java.awt.event.MouseEvent;

import kiev.fmt.*;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class ProjectView extends InfoView {
	
	private DrawTerm cur_dr;

	public ProjectView(IWindow window, Draw_ATextSyntax syntax, ICanvas view_canvas) {
		super(window, syntax, view_canvas);
	}
	
	public void formatAndPaint(boolean full) {
		view_canvas.setCurrent(cur_dr);
		if (cur_dr != null)
			view_canvas.setCurrent_node(cur_dr.get$drnode());
		if (full) {
			formatter.setWidth(view_canvas.getImgWidth());
			formatter.setShowAutoGenerated(show_auto_generated);
			formatter.setShowPlaceholders(show_placeholders);
			formatter.setHintEscapes(show_hint_escapes);
			view_canvas.setDlb_root(null);
			if (the_root != null && full) {
				formatter.format(the_root, view_root, getSyntax());
				view_root = formatter.getRootDrawable();
				view_canvas.setDlb_root(formatter.getRootDrawLayoutBlock());
			}
		}
		view_canvas.repaint();
	}

	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
		int x = e.getX();
		int y = e.getY() + view_canvas.getTranslated_y();
		DrawTerm dr_vis = view_canvas.getFirst_visible();
		GfxDrawTermLayoutInfo dr = dr_vis == null ? null : dr_vis.getGfxFmtInfo();
		for (; dr != null; dr = dr.getNext()) {
			int w = dr.width;
			int h = dr.height;
			if (dr.x < x && dr.y < y && dr.x+w >= x && dr.y+h >= y) {
				break;
			}
			if (dr.dterm == view_canvas.getLast_visible())
				return;
		}
		if (dr == null)
			return;
		e.consume();
		cur_dr = dr.dterm;
		if (e.getClickCount() >= 2) {
			ANode n = dr.getDrawable().get$drnode();
			if (n instanceof FileUnit) {
				parent_window.openEditor((FileUnit)n, ANode.emptyArray);
			}
			if (cur_dr.parent() instanceof DrawTreeBranch) {
				DrawTreeBranch dtb = (DrawTreeBranch)cur_dr.parent();
				dtb.setDrawFolded(!dtb.getDrawFolded());
				formatAndPaint(true);
				return;
			}
			else if (cur_dr.parent() instanceof DrawNonTermSet && cur_dr.parent().parent() instanceof DrawTreeBranch) {
				DrawTreeBranch dtb = (DrawTreeBranch)cur_dr.parent().parent();
				dtb.setDrawFolded(!dtb.getDrawFolded());
				formatAndPaint(true);
				return;
			}
		}
		formatAndPaint(false);
	}
	
}
