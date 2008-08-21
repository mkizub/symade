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
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import kiev.fmt.SyntaxManager;
import kiev.gui.ChooseItemEditor;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.IWindow;
import kiev.gui.InfoView;
import kiev.gui.NavigateNode;
import kiev.gui.NewElemHere;
import kiev.gui.NewElemNext;
import kiev.gui.ProjectView;
import kiev.gui.RenderActions;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIView;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class Window extends JFrame 
	implements IWindow, ActionListener, FocusListener {
	private static final long serialVersionUID = -1330097168227311246L;
	JTabbedPane explorers;
	JTabbedPane editors;
	JTabbedPane infos;
	JSplitPane split_left;
	JSplitPane split_bottom;
	Editor[] editor_views;
	InfoView info_view;
	InfoView clip_view;
	InfoView tree_view;
	Canvas info_canvas;
	Canvas clip_canvas;
	Canvas tree_canvas;
	
	Component	cur_component;

	/** List of listeners */
	protected ElementChangeListener[] elementChangeListeners = new ElementChangeListener[0];

	public Window() {
		super("SymADE");
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		UIActionMenuItem mi;
		{
			menu = new JMenu("File");
			menu.setMnemonic(KeyEvent.VK_F);

			mi = new UIActionMenuItem((IWindow)this, "Load", KeyEvent.VK_L, new FileActions.LoadFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Save As...", KeyEvent.VK_A, new FileActions.SaveFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Save As API", 0, new FileActions.SaveFileAsApi());
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Save", KeyEvent.VK_S, new FileActions.SaveFile());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
			menu.add(mi);

			menu.add(new JSeparator());

			mi = new UIActionMenuItem((IWindow)this, "Close", KeyEvent.VK_C,  EditActions.newCloseWindow());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Edit");
			menu.setMnemonic(KeyEvent.VK_E);

			mi = new UIActionMenuItem((IWindow)this, "Undo", KeyEvent.VK_U, EditActions.newUndo());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Copy", KeyEvent.VK_C,  EditActions.newCopy());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Cut", KeyEvent.VK_U,  EditActions.newCut());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Del", KeyEvent.VK_D,  EditActions.newCut());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Paste here", KeyEvent.VK_V,  new kiev.gui.Clipboard.PasteHereFactory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Paste next", KeyEvent.VK_B,  new kiev.gui.Clipboard.PasteNextFactory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
			menu.add(mi);

			menu.add(new JSeparator());

			mi = new UIActionMenuItem((IWindow)this, "New Element Here", KeyEvent.VK_H, new NewElemHere.Factory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "New Element Next", KeyEvent.VK_N, new NewElemNext.Factory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Edit Element", KeyEvent.VK_E, new ChooseItemEditor());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Select Parent", KeyEvent.VK_P, NavigateNode.newNodeUp());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Insert Mode", KeyEvent.VK_I,  NavigateNode.newInsertMode());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Enter Key Code", KeyEvent.VK_K, new KeyCodeEditor.Factory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Enter Mouse Code", KeyEvent.VK_M, new MouseButtonEditor.Factory());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.ALT_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Render");
			menu.setMnemonic(KeyEvent.VK_R);

			mi = new UIActionMenuItem((IWindow)this, "Syntax As...", KeyEvent.VK_S, new RenderActions.SyntaxFileAs());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Unfold all", KeyEvent.VK_U, new RenderActions.OpenFoldedAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Fold all", KeyEvent.VK_F, new RenderActions.CloseFoldedAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.SHIFT_MASK|ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "AutoGenerated", KeyEvent.VK_A, new RenderActions.ToggleShowAutoGenerated());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Placeholders", KeyEvent.VK_P, new RenderActions.ToggleShowPlaceholders());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "HintEscaped", KeyEvent.VK_E, new RenderActions.ToggleHintEscaped());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Redraw", KeyEvent.VK_R, new RenderActions.Redraw());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			menuBar.add(menu);
		}
		{
			menu = new JMenu("Compiler");
			menu.setMnemonic(KeyEvent.VK_C);

			mi = new UIActionMenuItem((IWindow)this, "Merge Tree", KeyEvent.VK_M, new FileActions.MergeTreeAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Compile Backend All", KeyEvent.VK_B, new FileActions.RunBackendAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Compile Frontend All", KeyEvent.VK_A, new FileActions.RunFrontendAll());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Compile Frontend", KeyEvent.VK_F, new FileActions.RunFrontend());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Use event bindings", KeyEvent.VK_U, new FileActions.UseEventBindings());
			mi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
			menu.add(mi);

			mi = new UIActionMenuItem((IWindow)this, "Reset event bindings", KeyEvent.VK_R, new FileActions.ResetEventBindings());
			menu.add(mi);

			menuBar.add(menu);
		}
		this.setJMenuBar(menuBar);

		info_canvas = new Canvas();		info_canvas.addFocusListener(this);
		clip_canvas = new Canvas();		clip_canvas.addFocusListener(this);
		tree_canvas = new Canvas();		tree_canvas.addFocusListener(this);
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
		explorers.addTab("Project", (Component)tree_canvas);
		infos.addTab("Info", (Component)info_canvas);
		infos.addTab("Clipboard", (Component)clip_canvas);
		//infos.addTab("Inspector", new JScrollPane(prop_table));
		
		this.getContentPane().add(split_left, BorderLayout.CENTER);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.setSize(screenSize.width, (screenSize.height*3)/4);
		this.setVisible(true);
		editor_views = new Editor[0];
		info_view = new InfoView((IWindow)this, info_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		clip_view = new InfoView((IWindow)this, clip_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		tree_view = new ProjectView((IWindow)this, tree_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-project-tree"));
		addListeners();
		initBgFormatters();
		tree_view.setRoot(Env.getProject());
		tree_view.formatAndPaint(true);
	}
	
	private void initBgFormatters() {
		info_view.bg_formatter = new BgFormatter(info_view);
		info_view.bg_formatter.start();
	}

	private void addListeners() {
		addElementChangeListener(info_view);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof UIActionMenuItem) {
			UIActionMenuItem m = (UIActionMenuItem)src;
			InputEventInfo evt = new InputEventInfo(e);
			Runnable r = m.factory.getAction(new UIActionViewContext((IWindow)this, evt, getCurrentView()));
			if (r != null)
				r.run();
		}
	}
	
	public void focusGained(FocusEvent e) {
		if (e.isTemporary())
			return;
		else if (e.getSource() instanceof Canvas)
			cur_component = (Canvas)e.getSource();
	}
	
	public void focusLost(FocusEvent e) {
		if (e.isTemporary())
			return;
		else if (e.getSource() instanceof Canvas)
			cur_component = null;
	}
	
	public UIView getCurrentView() {
		Component cc = cur_component;
		for (Editor e : editor_views) {
			if (e.getView_canvas() == cc)
				return e;
		}
		if (info_view.getView_canvas() == cc)
			return info_view;
		if (clip_view.getView_canvas() == cc)
			return clip_view;
		if (tree_view.getView_canvas() == cc)
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
				editors.setSelectedComponent((Component)e.getView_canvas());
				e.getView_canvas().requestFocus();
				return;
			}
		}
		Canvas edit_canvas = new Canvas();
		edit_canvas.addFocusListener(this);
		editors.addTab(fu.pname(), edit_canvas);
		editors.setSelectedComponent(edit_canvas);
		Editor editor_view = new Editor  ((IWindow)this, edit_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
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
			editors.remove((Component)e.getView_canvas());
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
		for (ElementChangeListener l : elementChangeListeners) {
			l.elementChanged(e);
		}
	}

  /**
	 * Adds a listener to the list that's notified each time an element change
	 * occurs.
	 * 
	 * @param l
	 *            the ElementChangeListener
	 */
	public void addElementChangeListener(ElementChangeListener l) {
		for (ElementChangeListener ecl : elementChangeListeners) {
			if (ecl == l)
				return;
		}
		ElementChangeListener[] tmp = new ElementChangeListener[elementChangeListeners.length + 1];
		for (int i = 0; i < elementChangeListeners.length; i++)
			tmp[i] = elementChangeListeners[i];
		tmp[elementChangeListeners.length] = l;
		elementChangeListeners = tmp;
	}

	/**
	 * @return the info_view
	 */
	public InfoView getInfo_view() {
		return info_view;
	}

	/**
	 * @param info_view the info_view to set
	 */
	public void setInfo_view(InfoView info_view) {
		this.info_view = info_view;
	}
	
}
