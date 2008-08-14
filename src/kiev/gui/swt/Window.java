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
package kiev.gui.swt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import kiev.fmt.SyntaxManager;
import kiev.gui.ChooseItemEditor;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.IWindow;
import kiev.gui.InfoView;
import kiev.gui.NavigateNode;
import kiev.gui.ProjectView;
import kiev.gui.TableView;
import kiev.gui.TreeView;
import kiev.gui.UIActionViewContext;
import kiev.gui.BgFormatter;
import kiev.gui.UIView;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEventInfo;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

public class Window  
implements IWindow, SelectionListener, FocusListener {
//	private static final long serialVersionUID = -1330097168227311246L;
//	JTabbedPane explorers;
//	JTabbedPane editors;
//	JTabbedPane infos;
//	JSplitPane split_left;
//	JSplitPane split_bottom;
	Shell shell;
	Editor[] editor_views;
	InfoView info_view;
	InfoView clip_view;
	TreeView expl_view;
	InfoView tree_view;
//	ANodeTree expl_tree;
//	ANodeTable prop_table; 
	TableView prop_view;  
	Canvas info_canvas;
	Canvas clip_canvas;
	Canvas tree_canvas;
	private Color swtColorBlack, swtColorWhite; 
	private Font swtDefaultFont; 

	Component	cur_component;
	static ResourceBundle resources = ResourceBundle.getBundle("kiev.gui.swt.symade");


	/** List of listeners */
	protected ElementChangeListener[] elementChangeListeners = new ElementChangeListener[0];

	public Window() {
		Display display = new Display();
		Shell shell = open(display);
		while (!shell.isDisposed())
			if (!display.readAndDispatch()) display.sleep();
		display.dispose();

	}

	public void createGUI(Composite parent) {
		GridLayout gridLayout;
		GridData gridData;
		Display display = parent.getDisplay();
		
		swtColorWhite = new Color(display, 255, 255, 255);
		swtColorBlack = new Color(display, 0, 0, 0);		
		swtDefaultFont = display.getSystemFont();

		Composite displayArea = new Composite(parent, SWT.NONE);
		gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		displayArea.setLayout(gridLayout);
		info_canvas = new Canvas(displayArea, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
			SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		info_canvas.setLayoutData(gridData);
		info_canvas.setBackground(swtColorWhite);
//		info_canvas.addFocusListener(this);
		clip_canvas = new Canvas(displayArea, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
			SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		clip_canvas.setLayoutData(gridData);
//		clip_canvas.addFocusListener(this);
		tree_canvas = new Canvas(displayArea, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);		
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		tree_canvas.setLayoutData(gridData);
//		tree_canvas.addFocusListener(this);

//	expl_tree   = new ANodeTree();	expl_tree.addFocusListener(this);
//	prop_table = new ANodeTable(); prop_table.addFocusListener(this);
//	explorers = new JTabbedPane();
//	editors   = new JTabbedPane();
//	infos     = new JTabbedPane();
//	split_bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false,
//	editors, infos);
//	split_bottom.setResizeWeight(0.75);
//	split_bottom.setOneTouchExpandable(true);
//	split_left   = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false,
//	explorers, split_bottom);
//	split_left.setResizeWeight(0.25);
//	split_left.setOneTouchExpandable(true);
//	explorers.addTab("Explorer", new JScrollPane(expl_tree));
//	explorers.addTab("Project", (Component)tree_canvas);
//	infos.addTab("Info", (Component)info_canvas);
//	infos.addTab("Clipboard", (Component)clip_canvas);
//	infos.addTab("Inspector", new JScrollPane(prop_table));

//	this.getContentPane().add(split_left, BorderLayout.CENTER);
//	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
//	this.setSize(screenSize.width, (screenSize.height*3)/4);
//	this.setVisible(true);
//	editor_views = new Editor[0];
//	info_view = new InfoView((IWindow)this, info_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
//	clip_view = new InfoView((IWindow)this, clip_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
//	prop_view = new TableView((IWindow)this, prop_table, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
//	expl_view = new TreeView((IWindow)this, expl_tree, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"));
//	tree_view = new ProjectView((IWindow)this, tree_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"));
//	addListeners();
//	initBgFormatters();
//	expl_view.setRoot(Env.getProject());
//	expl_view.formatAndPaint(true);
//	expl_tree.requestFocus();
//	tree_view.setRoot(Env.getProject());
//	tree_view.formatAndPaint(true);
	}

	void createShell(Display display) {
		shell = new Shell(display);
		shell.setText(resources.getString("Window_title"));	
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		shell.setLayout(layout);
		shell.addShellListener (new ShellAdapter () {
			public void shellClosed (ShellEvent e) {
				//TODO cleanup
			}
		});
	}
	public Shell open(Display display) {
		createShell(display);
		createGUI(shell);
		createMenuBar();
		shell.setSize(500, 400);
		shell.open();
		return shell;
	}
	void createMenuBar() {
		Menu bar = new Menu(shell, SWT.BAR);
		shell.setMenuBar(bar);

		MenuItem fileItem = new MenuItem (bar, SWT.CASCADE);
		fileItem.setText(resources.getString("File_menuitem"));
		fileItem.setMenu(createFileMenu());

		MenuItem editItem = new MenuItem (bar, SWT.CASCADE);
		editItem.setText(resources.getString("Edit_menuitem"));
		editItem.setMenu(createEditMenu());

		MenuItem renderItem = new MenuItem (bar, SWT.CASCADE);
		renderItem.setText(resources.getString("Render_menuitem"));
		renderItem.setMenu(createRenderMenu());

		MenuItem compilerItem = new MenuItem (bar, SWT.CASCADE);
		compilerItem.setText(resources.getString("Compiler_menuitem"));
		compilerItem.setMenu(createCompilerMenu());
	}

	
	Menu createFileMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);
		MenuItem item;
		UIActionMenuItem mi;
		
//		//Load 
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Load_menuitem"), SWT.MOD1 + 'L', new FileActions.LoadFileAs());
//
//		//Save As...
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("SaveAs_menuitem"), SWT.ALT + 'S', new FileActions.SaveFileAs());
//
//		//Save As API
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("SaveAsAPI_menuitem"), 0, new FileActions.SaveFileAsApi());
//		
//		//Save
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Save_menuitem"), SWT.CTRL + 'S', new FileActions.SaveFile());
//
//		item = new MenuItem(menu, SWT.SEPARATOR);
//		
		//Close
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Close_menuitem"), SWT.CTRL + 'W',  EditActions.newCloseWindow());
		
		// Exit
		item = new MenuItem(menu, SWT.PUSH);
		item.setText(resources.getString("Exit_menuitem"));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				menuFileExit();
			}
		});
		return menu;
	}
	
	void menuFileExit() {
		shell.close ();
	}

	Menu createEditMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);
		MenuItem item;
		UIActionMenuItem mi;
		
		//Undo 
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Undo_menuitem"), SWT.CTRL + 'Z', EditActions.newUndo());

		//Copy
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Copy_menuitem"), SWT.CTRL + 'C', EditActions.newCopy());

		//Cut
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Cut_menuitem"), SWT.CTRL + 'X', EditActions.newCut());
		
		//Del
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Del_menuitem"), SWT.DEL, EditActions.newCut());

		item = new MenuItem(menu, SWT.SEPARATOR);
		
//		//New Element Here
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("New_Element_Here_menuitem"), SWT.CTRL + 'N',  NewElemHere.newFactory());
//		
//		//New Element Next
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("New_Element_Next_menuitem"), SWT.CTRL + 'A',  NewElemNext.newFactory());

		//Edit Element
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Edit_Element_menuitem"), SWT.CTRL + 'E',  new ChooseItemEditor());

		//Select Parent
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Select_Parent_menuitem"), SWT.ALT + SWT.UP,  NavigateNode.newNodeUp());

		//Insert Mode
		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Insert_Mode_menuitem"), SWT.ALT + 'I',  NavigateNode.newInsertMode());

		return menu;
	}
	
	Menu createRenderMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);
		MenuItem item;
		UIActionMenuItem mi;
		
//		//Syntax As... 
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Syntax_As_menuitem"), SWT.CTRL + SWT.ALT + 'S', new RenderActions.SyntaxFileAs());
//
//		//Unfold all
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Unfold_all_menuitem"), SWT.CTRL + SWT.ALT + 'O', new RenderActions.OpenFoldedAll());
//
//		//Fold all
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Fold_all_menuitem"), SWT.CTRL + SWT.ALT + SWT.SHIFT+ 'O', new RenderActions.CloseFoldedAll());
//		
//		//AutoGenerated
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("AutoGenerated_menuitem"), SWT.CTRL + SWT.ALT + 'H', new RenderActions.ToggleShowAutoGenerated());
//
//		item = new MenuItem(menu, SWT.SEPARATOR);
//		
//		//Placeholders
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Placeholders_menuitem"), SWT.CTRL + SWT.ALT + 'P',  new RenderActions.ToggleShowPlaceholders());
//		
//		//HintEscaped
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("HintEscaped_menuitem"), SWT.CTRL + SWT.ALT + 'E',  new RenderActions.ToggleHintEscaped());
//
//		//Redraw
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Redraw_menuitem"), SWT.CTRL + SWT.ALT+ 'R',  new RenderActions.Redraw());

		return menu;
	}

	Menu createCompilerMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);
		MenuItem item;
		UIActionMenuItem mi;
		
//		//Merge Tree 
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Merge_Tree_menuitem"), SWT.CTRL + SWT.ALT + 'M', new FileActions.MergeTreeAll());
//
//		//Compile Backend All
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Compile_Backend_All_menuitem"), SWT.CTRL + SWT.ALT + 'C', new FileActions.RunBackendAll());
//
//		//Compile Frontend All
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Compile_Frontend_All_menuitem"), SWT.CTRL + SWT.ALT + 'V', new FileActions.RunFrontendAll());
//		
//		//Compile Frontend
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Compile_Frontend_menuitem"), SWT.ALT + 'V', new FileActions.RunFrontend());

		return menu;
	}
	
	private void initBgFormatters() {
		info_view.bg_formatter = new BgFormatter(info_view);
		info_view.bg_formatter.start();
		prop_view.bg_formatter = new BgFormatter(prop_view);
		prop_view.bg_formatter.start();
	}

	private void addListeners() {
		addElementChangeListener(info_view);
		addElementChangeListener(prop_view);
	}

	public void focusGained(FocusEvent e) {
//		if (e.isTemporary())
//		return;
//		else if (e.getSource() instanceof Canvas)
//		cur_component = (Canvas)e.getSource();
//		else if (e.getSource() instanceof ANodeTree)
//		cur_component = (ANodeTree)e.getSource();
	}

	public void focusLost(FocusEvent e) {
//		if (e.isTemporary())
//		return;
//		else if (e.getSource() instanceof Canvas)
//		cur_component = null;
//		else if (e.getSource() instanceof ANodeTree)
//		cur_component = null;
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
		if (expl_view.getView_tree() == cc)
			return expl_view;
		if (tree_view.getView_canvas() == cc)
			return tree_view;
		return null;
	}

	public void openEditor(FileUnit fu) {
		openEditor(fu, new ANode[0]);
	}

	public void openEditor(FileUnit fu, ANode[] path) {
//		for (Editor e: editor_views) {
//		if (e.the_root == fu || e.the_root.get$ctx_file_unit() == fu) {
//		e.goToPath(path);
//		editors.setSelectedComponent((Component)e.getView_canvas());
//		e.getView_canvas().requestFocus();
//		return;
//		}
//		}
//		Canvas edit_canvas = new Canvas();
//		edit_canvas.addFocusListener(this);
//		editors.addTab(fu.pname(), edit_canvas);
//		editors.setSelectedComponent(edit_canvas);
//		Editor editor_view = new Editor  ((IWindow)this, edit_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
//		editor_views = (Editor[])kiev.stdlib.Arrays.append(editor_views, editor_view);
//		editor_view.setRoot(fu);
//		editor_view.formatAndPaint(true);
//		editor_view.goToPath(path);
//		edit_canvas.requestFocus();
	}

	public void closeEditor(Editor ed) {
//		java.util.Vector<Editor> v = new java.util.Vector<Editor>();
//		for (Editor e: editor_views) {
//		if (e != ed) {
//		v.add(e);
//		continue;
//		}
//		editors.remove((Component)e.getView_canvas());
//		}
//		editor_views = v.toArray(new Editor[v.size()]);
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
	 * @param	l		the ElementChangeListener
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

	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub

	}

	public void widgetSelected(SelectionEvent e) {
		Object src = e.getSource();
		if (src instanceof MenuItem) {
			UIActionMenuItem m = (UIActionMenuItem)((MenuItem)src).getData();
			InputEventInfo evt = new InputEventInfo(e);
			Runnable r = m.factory.getAction(new UIActionViewContext((IWindow)this, evt, getCurrentView()));
			if (r != null)
				r.run();		
		}
	}
}
