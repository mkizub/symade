/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.gui.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.EventListenerList;

import kiev.fmt.SyntaxManager;
import kiev.gui.ChooseItemEditor;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.InfoView;
import kiev.gui.NavigateNode;
import kiev.gui.NewElemHere;
import kiev.gui.NewElemNext;
import kiev.gui.ProjectView;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIView;
import kiev.gui.BgFormatter;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class Window extends JFrame implements ActionListener, FocusListener {
	private static final long serialVersionUID = -1330097168227311246L;
	JTabbedPane explorers;
	JTabbedPane editors;
	JTabbedPane infos;
	JSplitPane  split_left;
	JSplitPane  split_bottom;
	Editor[]	editor_views;
	public InfoView	info_view;
	InfoView	clip_view;
	TreeView	expl_view;
	InfoView	tree_view;
	ANodeTree	expl_tree;
	ANodeTable prop_table; 
	TableView prop_view;  
	Canvas		info_canvas;
	Canvas		clip_canvas;
	Canvas		tree_canvas;
	
	Component	cur_component;

  /** List of listeners */
  protected EventListenerList listenerList = new EventListenerList();

	public Window() {
		super("SymADE");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		UIActionMenuItem mi;
		{
			menu = new JMenu("File");
			menu.setMnemonic(KeyEvent.VK_F);

			mi = new UIActionMenuItem(this, "Load", KeyEvent.VK_L, new FileActions.LoadFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Save As...", KeyEvent.VK_A, new FileActions.SaveFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Save As API", 0, new FileActions.SaveFileAsApi());
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Save", KeyEvent.VK_S, new FileActions.SaveFile());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			menu.add(mi);

			menu.add(new JSeparator());

			mi = new UIActionMenuItem(this, "Close", KeyEvent.VK_C,  EditActions.newCloseWindow());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Edit");
			menu.setMnemonic(KeyEvent.VK_E);

			mi = new UIActionMenuItem(this, "Undo", KeyEvent.VK_U, EditActions.newUndo());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Copy", KeyEvent.VK_C,  EditActions.newCopy());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Cut", KeyEvent.VK_U,  EditActions.newCut());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Del", KeyEvent.VK_D,  EditActions.newCut());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
			menu.add(mi);

			menu.add(new JSeparator());

			mi = new UIActionMenuItem(this, "New Element Here", KeyEvent.VK_H, NewElemHere.newFactory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "New Element Next", KeyEvent.VK_N,  NewElemNext.newFactory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Edit Element", KeyEvent.VK_E, new ChooseItemEditor());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Select Parent", KeyEvent.VK_P, NavigateNode.newNodeUp());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Insert Mode", KeyEvent.VK_I,  NavigateNode.newInsertMode());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Render");
			menu.setMnemonic(KeyEvent.VK_R);

			mi = new UIActionMenuItem(this, "Syntax As...", KeyEvent.VK_S, new RenderActions.SyntaxFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Unfold all", KeyEvent.VK_U, new RenderActions.OpenFoldedAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Fold all", KeyEvent.VK_F, new RenderActions.CloseFoldedAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.SHIFT_MASK|ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "AutoGenerated", KeyEvent.VK_A, new RenderActions.ToggleShowAutoGenerated());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Placeholders", KeyEvent.VK_P, new RenderActions.ToggleShowPlaceholders());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "HintEscaped", KeyEvent.VK_E, new RenderActions.ToggleHintEscaped());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Redraw", KeyEvent.VK_R, new RenderActions.Redraw());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Compiler");
			menu.setMnemonic(KeyEvent.VK_C);

			mi = new UIActionMenuItem(this, "Merge Tree", KeyEvent.VK_M, new FileActions.MergeTreeAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Compile Backend All", KeyEvent.VK_B, new FileActions.RunBackendAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Compile Frontend All", KeyEvent.VK_A, new FileActions.RunFrontendAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem(this, "Compile Frontend", KeyEvent.VK_F, new FileActions.RunFrontend());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		this.setJMenuBar(menuBar);

		expl_tree   = new ANodeTree();	expl_tree.addFocusListener(this);
		info_canvas = new Canvas();		info_canvas.addFocusListener(this);
		clip_canvas = new Canvas();		clip_canvas.addFocusListener(this);
		tree_canvas = new Canvas();		tree_canvas.addFocusListener(this);
		prop_table = new ANodeTable(); prop_table.addFocusListener(this);
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
		explorers.addTab("Project", tree_canvas);
		infos.addTab("Info", info_canvas);
		infos.addTab("Clipboard", clip_canvas);
		infos.addTab("Inspector", new JScrollPane(prop_table));
		
		this.getContentPane().add(split_left, BorderLayout.CENTER);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(screenSize.width, (screenSize.height*3)/4);
		this.setVisible(true);
		editor_views = new Editor[0];
		info_view = new InfoView(this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"), info_canvas);
		clip_view = new InfoView(this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"), clip_canvas);
		prop_view = new TableView(this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"), prop_table);
		expl_view = new TreeView(this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"), expl_tree);
		tree_view = new ProjectView(this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"), tree_canvas);
		addListeners();
		initBgFormatters();
		expl_view.setRoot(Env.getProject());
		expl_view.formatAndPaint(true);
		expl_tree.requestFocus();
		tree_view.setRoot(Env.getProject());
		tree_view.formatAndPaint(true);
	}
	
	private void initBgFormatters() {
		if (info_view.isRegisteredToElementEvent())
			info_view.setBg_formatter(new BgFormatter(info_view)).start();
		if (prop_view.isRegisteredToElementEvent())
			prop_view.setBg_formatter(new BgFormatter(prop_view)).start();
	}

	private void addListeners() {
		addElementChangeListener(info_view);
		addElementChangeListener(prop_view);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof UIActionMenuItem) {
			UIActionMenuItem m = (UIActionMenuItem)src;
			Runnable r = m.factory.getAction(new UIActionViewContext(this, getCurrentView()));
			if (r != null)
				r.run();
		}
	}
	
	public void focusGained(FocusEvent e) {
		if (e.isTemporary())
			return;
		else if (e.getSource() instanceof Canvas)
			cur_component = (Canvas)e.getSource();
		else if (e.getSource() instanceof ANodeTree)
			cur_component = (ANodeTree)e.getSource();
	}
	
	public void focusLost(FocusEvent e) {
		if (e.isTemporary())
			return;
		else if (e.getSource() instanceof Canvas)
			cur_component = null;
		else if (e.getSource() instanceof ANodeTree)
			cur_component = null;
	}
	
	public UIView getCurrentView() {
		Component cc = cur_component;
		for (Editor e : editor_views) {
			if (e.view_canvas == cc)
				return e;
		}
		if (info_view.view_canvas == cc)
			return info_view;
		if (clip_view.view_canvas == cc)
			return clip_view;
		if (expl_view.the_tree == cc)
			return expl_view;
		if (tree_view.view_canvas == cc)
			return tree_view;
		return null;
	}
	
	public void openEditor(FileUnit fu) {
		openEditor(fu, new ANode[0]);
	}
	
	public void openEditor(FileUnit fu, ANode[] path) {
		for (Editor e: editor_views) {
			if (e.the_root == fu || e.the_root.get$ctx_file_unit() == fu) {
				e.goToPath(path);
				editors.setSelectedComponent(e.view_canvas);
				e.view_canvas.requestFocus();
				return;
			}
		}
		Canvas edit_canvas = new Canvas();
		edit_canvas.addFocusListener(this);
		editors.addTab(fu.pname(), edit_canvas);
		editors.setSelectedComponent(edit_canvas);
		Editor editor_view = new Editor  (this, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"), edit_canvas);
		editor_views = (Editor[])kiev.stdlib.Arrays.append(editor_views, editor_view);
		editor_view.setRoot(fu);
		editor_view.formatAndPaint(true);
		editor_view.goToPath(path);
		edit_canvas.requestFocus();
	}

	public void closeEditor(Editor ed) {
		java.util.Vector<Editor> v = new java.util.Vector<Editor>();
		for (Editor e: editor_views) {
			if (e != ed) {
				v.add(e);
				continue;
			}
			editors.remove(e.view_canvas);
		}
		editor_views = v.toArray(new Editor[v.size()]);
	}
	
	/**
	 * Forwards the given notification event to all
	 * <code>ElementChangeListeners</code> that registered
	 * themselves as listeners for <code>ElementEvent</code> event.
	 *
	 * @param e  the event to be forwarded
	 *
	 * @see #addElementChangeListener
	 * @see ElementEvent
	 * @see EventListenerList
	 */
	public void fireElementChanged(ElementEvent e) {
		//Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		//Process the listeners last to first, notifying
		//those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ElementChangeListener.class) {
				((ElementChangeListener)listeners[i+1]).elementChanged(e);
			}
		}
	}

  /**
   * Adds a listener to the list that's notified each time an element change
   * occurs.
   *
   * @param	l		the ElementChangeListener
   */
  public void addElementChangeListener(ElementChangeListener l) {
  	listenerList.add(ElementChangeListener.class, l);
  }

	/**
	 * @return the listenerList
	 */
	public EventListenerList getListenerList() {
		return listenerList;
	}
	
}
