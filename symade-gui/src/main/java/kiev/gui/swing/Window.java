/*******************************************************************************
 * Copyright (c) 2005-2008 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *     Roman Chepelyev (gromanc@gmail.com) - implementation and refactoring
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
import java.util.Timer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import kiev.fmt.SyntaxManager;
import kiev.fmt.common.UIDrawPath;
import kiev.gui.ClipboardActions;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.ErrorsView;
import kiev.gui.FileActions;
import kiev.gui.IEditor;
import kiev.gui.NavigateNode;
import kiev.gui.NewElemHere;
import kiev.gui.NewElemNext;
import kiev.gui.ProjectView;
import kiev.gui.RenderActions;
import kiev.gui.UIAction;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIView;
import kiev.gui.swing.UIActionMenuItem.MenuItem;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.INode;
import kiev.WorkerThreadGroup;

public class Window extends kiev.gui.Window 
implements ActionListener, FocusListener {
	
	static Timer guiTimer = new Timer("GUI timer", true);
	
	/**
	 * The frame.
	 */
	private JFrame frame; 

	/**
	 * The explorers.
	 */
	private JTabbedPane explorers;

	/**
	 * The editors.
	 */
	private JTabbedPane editors;

	/**
	 * The infos.
	 */
	private JTabbedPane infos;

	/**
	 * Split left pane.
	 */
	private JSplitPane split_left;

	/**
	 * Split bottom pane.
	 */
	private JSplitPane split_bottom;

	/**
	 * Status bar panel and it's labels.
	 */
	private JPanel status_bar;
	private StatusBarHeapInfo sb_memory;
	private StatusBarInsMode  sb_insmode;
	private StatusBarEditing  sb_editing;

	/**
	 * Editor views.
	 */
	private Editor[] editor_views = new Editor[0];

	/**
	 * The info view.
	 */
	private UIView info_view;

	/**
	 * The clip view.
	 */
	@SuppressWarnings("unused")
	private UIView clip_view;

	/**
	 * The tree view.
	 */
	private kiev.gui.UIView tree_view;

	/**
	 * Editor views.
	 */
	private Editor test_view;

	/**
	 * The info view.
	 */
	private ErrorsView error_view;

	/**
	 * The info canvas.
	 */
	private Canvas info_canvas;

	/**
	 * The clip canvas.
	 */
	private Canvas clip_canvas;

	/**
	 * The tree canvas.
	 */
	private Canvas tree_canvas;

	/**
	 * The tree canvas.
	 */
	private Canvas test_canvas;

	/**
	 * The tree canvas.
	 */
	private Canvas error_canvas;

	/**
	 * The current component.
	 */
	private Component	cur_component;
	

	/**
	 * The constructor.
	 * @param env the environment
	 */
	public Window(WorkerThreadGroup thrg) {
		super(thrg);
		frame = new JFrame("SymADE");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		JMenuBar menuBar = new JMenuBar();
		JMenu menu;
		UIActionMenuItem mi;
		
		// "File" menu
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);

		mi = new UIActionMenuItem(this, "New...", KeyEvent.VK_N, new FileActions.NewFile());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Load...", KeyEvent.VK_L, new FileActions.LoadFileAs());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Save As...", KeyEvent.VK_A, new FileActions.SaveFileAs());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Save", KeyEvent.VK_S, new FileActions.SaveFile());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		menu.add(new JSeparator());

		mi = new UIActionMenuItem(this, "Import", 0,  new FileActions.ImportFileAs());
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Export", 0,  new FileActions.ExportFileAs());
		menu.add(mi.item);

		menu.add(new JSeparator());

		mi = new UIActionMenuItem(this, "Close", KeyEvent.VK_C,  new EditActions.CloseWindow());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		menuBar.add(menu);

		// "Edit" menu
		menu = new JMenu("Edit");
		menu.setMnemonic(KeyEvent.VK_E);

		mi = new UIActionMenuItem(this, "Undo", KeyEvent.VK_U, new EditActions.Undo());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Copy", KeyEvent.VK_C,  new EditActions.Copy());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Cut", KeyEvent.VK_U,  new EditActions.Cut());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Del", KeyEvent.VK_D,  new EditActions.Del());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Paste prev", KeyEvent.VK_B,  new ClipboardActions.PastePrevFactory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Paste here", KeyEvent.VK_V,  new ClipboardActions.PasteHereFactory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Paste next", KeyEvent.VK_F,  new ClipboardActions.PasteNextFactory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		menu.add(new JSeparator());

		mi = new UIActionMenuItem(this, "New Element Here", KeyEvent.VK_N, new NewElemHere.Factory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "New Element Next", KeyEvent.VK_A, new NewElemNext.Factory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Edit Element", KeyEvent.VK_E, new EditActions.ChooseItemEditor());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Select Parent", KeyEvent.VK_P, new NavigateNode.NodeUp());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Insert Mode", KeyEvent.VK_I, new NavigateNode.InsertMode());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Enter Key Code", KeyEvent.VK_K, new KeyCodeEditor.Factory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Enter Mouse Code", KeyEvent.VK_M, new MouseButtonEditor.Factory());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		menuBar.add(menu);

		// "Render" menu
		menu = new JMenu("Render");
		menu.setMnemonic(KeyEvent.VK_R);

		mi = new UIActionMenuItem(this, "Syntax As...", KeyEvent.VK_S, new RenderActions.SyntaxFileAs());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Style As...", KeyEvent.VK_Y, new RenderActions.StyleAs());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Unfold all", KeyEvent.VK_U, new RenderActions.OpenFoldedAll());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Fold all", KeyEvent.VK_F, new RenderActions.CloseFoldedAll());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.SHIFT_MASK|ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Placeholders", KeyEvent.VK_P, new RenderActions.ToggleShowPlaceholders());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Redraw", KeyEvent.VK_R, new RenderActions.Redraw());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		menuBar.add(menu);

		// "Compiler" menu
		menu = new JMenu("Compiler");
		menu.setMnemonic(KeyEvent.VK_C);

		mi = new UIActionMenuItem(this, "Merge Tree", KeyEvent.VK_M, new FileActions.MergeTreeAll());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Compile Backend All", KeyEvent.VK_B, new FileActions.RunBackendAll());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Compile Frontend All", KeyEvent.VK_A, new FileActions.RunFrontendAll());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Compile Frontend", KeyEvent.VK_F, new FileActions.RunFrontend());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Use event bindings", KeyEvent.VK_U, new FileActions.UseEventBindings());
		mi.item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.ALT_MASK|ActionEvent.CTRL_MASK));
		menu.add(mi.item);

		mi = new UIActionMenuItem(this, "Reset event bindings", KeyEvent.VK_R, new FileActions.ResetEventBindings());
		menu.add(mi.item);

		menuBar.add(menu);

		frame.setJMenuBar(menuBar);
		
		status_bar = new JPanel();
		status_bar.setBorder(new BevelBorder(BevelBorder.RAISED));
		sb_editing = new StatusBarEditing(this);
		sb_insmode = new StatusBarInsMode(this);
		sb_memory  = new StatusBarHeapInfo(this);
		status_bar.setLayout(new BoxLayout(status_bar, BoxLayout.X_AXIS));
		status_bar.add(Box.createHorizontalGlue());
		status_bar.add(sb_editing);
		status_bar.add(sb_insmode);
		status_bar.add(sb_memory);

		info_canvas = new Canvas();		info_canvas.addFocusListener(this);
		clip_canvas = new Canvas();		clip_canvas.addFocusListener(this);
		tree_canvas = new Canvas();		tree_canvas.addFocusListener(this);
		test_canvas = new Canvas();		test_canvas.addFocusListener(this);
		error_canvas= new Canvas();		error_canvas.addFocusListener(this);
		explorers = new JTabbedPane();
		editors = new JTabbedPane();
		infos = new JTabbedPane();
		split_bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, editors, infos);
		split_bottom.setResizeWeight(0.75);
		split_bottom.setOneTouchExpandable(true);
		split_left = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false,	explorers, split_bottom);
		split_left.setResizeWeight(0.25);
		split_left.setOneTouchExpandable(true);
		explorers.addTab("Project", (Component)tree_canvas);
		infos.addTab("Info", (Component)info_canvas);
		infos.addTab("Clipboard", (Component)clip_canvas);
		//infos.addTab("Inspector", new JScrollPane(prop_table));
		infos.addTab("Test", (Component)test_canvas);
		infos.addTab("Errors", (Component)error_canvas);

		frame.getContentPane().add(split_left, BorderLayout.CENTER);
		frame.getContentPane().add(status_bar, BorderLayout.SOUTH);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setSize(screenSize.width*4/5, screenSize.height*4/5-20);
		Dimension frameSize = frame.getSize();
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		frame.setVisible(true);
		info_view = new UIView(this, info_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		clip_view = new UIView(this, clip_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		tree_view = new ProjectView(this, tree_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-project-tree"));
		test_view = new Editor(this, test_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java")); 
		error_view = new ErrorsView(this, error_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-errors")); 
		addListeners();
		getEditorThreadGroup().runTask(new Runnable() {
			public void run() {
				new FileActions(Window.this,null);
				new RenderActions(null,null);
				tree_view.setRoot(getCurrentProject(), true);
				error_view.setRoot(getCurrentEnv().root, true);
			}
		});
		updateStatusBar();
	}

	/**
	 * Gets the frame.
	 * @return the frame
	 */
	JFrame getFrame() {
		return frame;
	}

	/**
	 * Add listeners.
	 */
	private void addListeners() {
		addElementChangeListener(info_view);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof MenuItem) {
			final UIActionMenuItem m = ((MenuItem)src).owner;
			final InputEventInfo evt = new InputEventInfo(e);
			getEditorThreadGroup().runTaskLater(new Runnable() {
				public void run() {
					UIAction action = m.getFactory().getAction(new UIActionViewContext(Window.this, evt, getCurrentView()));
					if (action != null)
						action.exec();
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {
		if (e.isTemporary()) return;
		else if (e.getSource() instanceof Canvas) cur_component = (Canvas)e.getSource();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
	 */
	public void focusLost(FocusEvent e) {
		if (e.isTemporary()) return;
		else if (e.getSource() instanceof Canvas) cur_component = null;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#getCurrentView()
	 */
	public UIView getCurrentView() {
		if (views == null) return null;
		for (UIView v: (UIView[])views) if (v.getViewPeer() == cur_component)	return v;
		return null;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#openEditor(kiev.vlang.FileUnit, kiev.vtree.INode[])
	 */
	public IEditor openEditor(FileUnit fu, INode[] path) {
		for (Editor e: editor_views) {
			if (e.getFileUnit() == fu || Env.ctxFileUnit(e.getRoot()) == fu) {
				e.goToPath(new UIDrawPath(path));
				editors.setSelectedComponent((Component)e.getViewPeer());
				e.getViewPeer().requestFocus();
				return e;
			}
		}
		Canvas edit_canvas = new Canvas();
		edit_canvas.addFocusListener(this);
		editors.addTab(fu.pname(), edit_canvas);
		editors.setSelectedComponent(edit_canvas);
		Editor editor_view = new Editor(this, edit_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		editor_views = (Editor[])kiev.stdlib.Arrays.append(editor_views, editor_view);
		editor_view.setFileUnit(fu);
		editor_view.formatAndPaint(true);
		editor_view.goToPath(new UIDrawPath(path));
		edit_canvas.requestFocus();
		test_view.setFileUnit(fu);
		test_view.formatAndPaint(true);
		return editor_view;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#closeEditor(kiev.gui.Editor)
	 */
	public void closeEditor(IEditor ed) {
		// shrink editors
		java.util.Vector<Editor> v = new java.util.Vector<Editor>();
		for (Editor e: editor_views) {
			if (e != ed) {
				v.add(e); continue;
			}
			editors.remove((Component)e.getViewPeer());
		}
		editor_views = v.toArray(new Editor[v.size()]);
		
		// shrink views
		java.util.Vector<UIView> w = new java.util.Vector<UIView>();
		for (UIView e: views) if (e != ed) w.add(e);
		views = w.toArray(new UIView[w.size()]);

		super.closeEditor(ed);
	}
	
	public void updateStatusBar() {
		sb_editing.statusUpdate();
		sb_insmode.statusUpdate();
		sb_memory.statusUpdate();
	}

	/* (non-Javadoc)
	 * @see kiev.gui.Window#enableMenuItems()
	 */
	protected void enableMenuItems() {}

	/**
	 * Notify that errors list may be changed.
	 */
	public void fireErrorsModified() {
		error_view.formatAndPaintLater();
	}

}
