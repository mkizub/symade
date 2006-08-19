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

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
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
	Editor[]	editor_views;
	UIView		info_view;
	UIView		clip_view;
	UIView		expl_view;
	ANodeTree	expl_tree;
	Canvas		info_canvas;
	Canvas		clip_canvas;

	public Window() {
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		expl_tree   = new ANodeTree();
		info_canvas = new Canvas();
		clip_canvas = new Canvas();
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
		explorers.addTab("Explorer", new JScrollPane(expl_tree));
		infos.addTab("Info", info_canvas);
		infos.addTab("Clipboard", clip_canvas);
		this.getContentPane().add(split_left, BorderLayout.CENTER);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(screenSize.width, (screenSize.height*3)/4);
		this.show();
		editor_views = new Editor[0];
		info_view   = new InfoView(this, (TextSyntax)Env.resolveGlobalDNode("stx-fmt.syntax-for-java"), info_canvas);
		clip_view   = new InfoView(this, (TextSyntax)Env.resolveGlobalDNode("stx-fmt.syntax-for-java"), clip_canvas);
		expl_view   = new TreeView(this, (TreeSyntax)Env.resolveGlobalDNode("stx-fmt.syntax-for-project-tree"), expl_tree);
		expl_view.setRoot(Env.root);
		expl_view.formatAndPaint(true);
		expl_tree.requestFocus();
	}
	
	public void setRoot(FileUnit fu) {
		openEditor(fu, new ANode[0]);
	}
	
	public void openEditor(FileUnit fu, ANode[] path) {
		foreach (Editor e; editor_views) {
			if (e.the_root == fu || e.the_root.ctx_file_unit == fu) {
				e.goToPath(path);
				editors.setSelectedComponent(e.view_canvas);
				e.view_canvas.requestFocus();
				return;
			}
		}
		Canvas edit_canvas = new Canvas();
		editors.addTab(fu.id.sname, edit_canvas);
		editors.setSelectedComponent(edit_canvas);
		Editor editor_view = new Editor  (this, (TextSyntax)Env.resolveGlobalDNode("stx-fmt.syntax-for-java"), edit_canvas);
		editor_views = (Editor[])Arrays.append(editor_views, editor_view);
		editor_view.setRoot(fu);
		editor_view.formatAndPaint(true);
		editor_view.goToPath(path);
		edit_canvas.requestFocus();
	}

	public void closeEditor(Editor ed) {
		Vector<Editor> v = new Vector<Editor>();
		foreach (Editor e; editor_views) {
			if (e != ed) {
				v.append(e);
				continue;
			}
			editors.remove(e.view_canvas);
		}
		editor_views = v.toArray();
	}
}

