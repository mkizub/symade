package kiev.gui;

import kiev.fmt.*;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class ProjectView extends InfoView {
	
	private DrawTerm cur_dr;

	public ProjectView(IWindow window, ICanvas view_canvas, Draw_ATextSyntax syntax) {
		super(window, view_canvas, syntax);
	}
	
	@Override
	public void formatAndPaint(boolean full) {
		view_canvas.setCurrent(cur_dr, cur_dr == null ? null : cur_dr.drnode);
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

	public void selectDrawTerm(DrawTerm dr) {
		cur_dr = (DrawTerm)dr;
		formatAndPaint(false);
	}

	public void toggleItem(Drawable dr) {
		if (dr instanceof DrawTerm)
			cur_dr = (DrawTerm)dr;
		ANode n = dr.drnode;
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
		formatAndPaint(false);
	}
	
}
