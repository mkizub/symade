package kiev.gui;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 * @author mkizub
 */
public class Window extends JFrame {

	JTabbedPane explorers;
	JTabbedPane editors;
	JTabbedPane infos;
	JSplitPane  split_left;
	JSplitPane  split_bottom;
	Editor editor_view;
	UIView info_view;
	UIView clip_view;
	UIView expl_view;
	UIView export_view;
	Canvas expl_canvas;
	Canvas edit_canvas;
	Canvas info_canvas;
	Canvas clip_canvas;
	Canvas export_canvas;

	public Window() {
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		expl_canvas = new Canvas();
		edit_canvas = new Canvas();
		info_canvas = new Canvas();
		clip_canvas = new Canvas();
		export_canvas = new Canvas();
		explorers = new JTabbedPane();
		editors   = new JTabbedPane();
		infos     = new JTabbedPane();
		split_bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false,
				editors, infos);
		split_bottom.setResizeWeight(0.75);
		split_bottom.setOneTouchExpandable(true);
		split_left   = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false,
				explorers, split_bottom);
		split_left.setResizeWeight(0.25);
		split_left.setOneTouchExpandable(true);
		explorers.addTab("Explorer", expl_canvas);
		editors.addTab("Meta", edit_canvas);
		editors.addTab("Export", export_canvas);
		infos.addTab("Info", info_canvas);
		infos.addTab("Clipboard", clip_canvas);
		this.getContentPane().add(split_left, BorderLayout.CENTER);
		this.setSize(950, 650);
		this.show();
		edit_canvas.requestFocus();
		editor_view = new Editor  (this, new JavaSyntax(), edit_canvas);
		info_view   = new InfoView(this, new JavaSyntax(), info_canvas);
		clip_view   = new InfoView(this, new JavaSyntax(), clip_canvas);
		expl_view   = new InfoView(this, new JavaSyntax(), expl_canvas);
		export_view = new InfoView(this, new JavaSyntax(), export_canvas);
		editor_view.setRoot(null);
		editor_view.formatAndPaint();
		expl_view.the_root = null;
		expl_view.formatAndPaint();
	}

}

