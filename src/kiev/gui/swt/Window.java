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

import java.util.ResourceBundle;

import kiev.fmt.SyntaxManager;
import kiev.gui.BgFormatter;
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
import kiev.gui.UIView;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEventInfo;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;

import org.eclipse.draw2d.EventListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class Window implements IWindow, SelectionListener, FocusListener {
	private TabFolder explorers;
	private TabFolder editors;
	private TabFolder infos;
	private SashForm split_right;
	private SashForm split_bottom;
	Editor[] editor_views;
	InfoView info_view;
	InfoView clip_view;
	TreeView expl_view;
	InfoView tree_view;
	ANodeTree expl_tree;
	ANodeTable prop_table; 
	TableView prop_view;  
	Canvas info_canvas;
	Canvas clip_canvas;
	Canvas tree_canvas;
	private Color swtColorBlack, swtColorWhite; 
	private Font swtDefaultFont; 
	Composite displayArea;
	final Shell shell;
	final Display display;
	Object cur_comp;
	static ResourceBundle resources = ResourceBundle.getBundle("kiev.gui.swt.symade");


	/** List of listeners */
	protected ElementChangeListener[] elementChangeListeners = new ElementChangeListener[0];

	public Window() {
		display = new Display();
		shell = new Shell(display);
		shell.setText(resources.getString("Window_title"));	
		shell.setLayout(new FillLayout());
		shell.addShellListener (new ShellAdapter () {
			public void shellClosed (ShellEvent e) {
				Shell [] shells = shell.getDisplay().getShells();
				for (int i = 0; i < shells.length; i++) {
					if (shells [i] != shell) shells [i].close ();
				}
			}
		});
		createGUI(shell);
		shell.open();
		while (! shell.isDisposed()) {
			if (! display.readAndDispatch()) display.sleep();
		}
	}

	public void createGUI(Composite parent) {
		GridLayout gridLayout;
		GridData gridData;
		Display display = parent.getDisplay();

		createMenuBar();

		swtColorWhite = new Color(display, 255, 255, 255);
		swtColorBlack = new Color(display, 0, 0, 0);		
		swtDefaultFont = display.getSystemFont();

		displayArea = new Composite(parent, SWT.NONE);
		displayArea.setLayout(new FillLayout());

		split_right   = new SashForm(displayArea, SWT.HORIZONTAL);
//	split_left.setResizeWeight(0.25);
//	split_left.setOneTouchExpandable(true);
		Composite right_page = new Composite(split_right, SWT.NONE);
		right_page.setLayout(new FillLayout());
		split_bottom = new SashForm(right_page, SWT.VERTICAL);
//		split_bottom.setResizeWeight(0.75);
//		split_bottom.setOneTouchExpandable(true);

		explorers = new TabFolder(split_right, SWT.NONE);
		editors   = new TabFolder(split_bottom, SWT.NONE);
		infos     = new TabFolder(split_bottom, SWT.NONE);

		TabItem item = new TabItem (explorers, SWT.NONE);
		item.setText("Explorer");
		Composite expl_page = new Composite(explorers, SWT.NONE);
		item.setControl(expl_page);

		expl_tree   = new ANodeTree(expl_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);	
//		expl_tree.addFocusListener(this);

		item = new TabItem (explorers, SWT.NONE);
		item.setText("Project");
		Composite proj_page = new Composite(explorers, SWT.NONE);
		item.setControl(proj_page);

		tree_canvas = new Canvas(proj_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);		
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		tree_canvas.setLayoutData(gridData);
//		tree_canvas.addFocusListener(this);

		item = new TabItem (infos, SWT.NONE);
		item.setText("Info");
		Composite info_page = new Composite(infos, SWT.NONE);
		item.setControl(info_page);

		info_canvas = new Canvas(info_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		info_canvas.setLayoutData(gridData);
		info_canvas.setBackground(swtColorWhite);
//		info_canvas.addFocusListener(this);

		item = new TabItem (infos, SWT.NONE);
		item.setText("Clipboard");
		Composite clip_page = new Composite(infos, SWT.NONE);
		item.setControl(clip_page);

		clip_canvas = new Canvas(clip_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		clip_canvas.setLayoutData(gridData);
//		clip_canvas.addFocusListener(this);

		item = new TabItem (infos, SWT.NONE);
		item.setText("Inspector");
		Composite prop_page = new Composite(infos, SWT.NONE);
		item.setControl(prop_page);

		prop_table = new ANodeTable(prop_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);	
//		prop_table.addFocusListener(this);

		
		editor_views = new Editor[0];
		info_view = new InfoView((IWindow)this, info_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
		clip_view = new InfoView((IWindow)this, clip_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
		prop_view = new TableView((IWindow)this, prop_table, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));

		expl_view = new TreeView((IWindow)this, expl_tree, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"));	
		tree_view = new ProjectView((IWindow)this, tree_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-project-tree"));
		addListeners();
		initBgFormatters();
		expl_view.setRoot(Env.getProject());
		expl_tree.setInput(expl_view.view_root);
		expl_view.formatAndPaint(true);

		expl_tree.requestFocus();
//		tree_view.setRoot(Env.getProject());
//		tree_view.formatAndPaint(true);
		Rectangle screenSize = display.getClientArea();
		parent.setSize(screenSize.width*4/5, screenSize.height*4/5-20);
	}

	public void dispose() {
		displayArea = null;
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

//		//Save As...
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("SaveAs_menuitem"), SWT.ALT + 'S', new FileActions.SaveFileAs());

//		//Save As API
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("SaveAsAPI_menuitem"), 0, new FileActions.SaveFileAsApi());

//		//Save
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Save_menuitem"), SWT.CTRL + 'S', new FileActions.SaveFile());

//		item = new MenuItem(menu, SWT.SEPARATOR);

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

//		//Unfold all
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Unfold_all_menuitem"), SWT.CTRL + SWT.ALT + 'O', new RenderActions.OpenFoldedAll());

//		//Fold all
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Fold_all_menuitem"), SWT.CTRL + SWT.ALT + SWT.SHIFT+ 'O', new RenderActions.CloseFoldedAll());

//		//AutoGenerated
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("AutoGenerated_menuitem"), SWT.CTRL + SWT.ALT + 'H', new RenderActions.ToggleShowAutoGenerated());

//		item = new MenuItem(menu, SWT.SEPARATOR);

//		//Placeholders
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Placeholders_menuitem"), SWT.CTRL + SWT.ALT + 'P',  new RenderActions.ToggleShowPlaceholders());

//		//HintEscaped
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("HintEscaped_menuitem"), SWT.CTRL + SWT.ALT + 'E',  new RenderActions.ToggleHintEscaped());

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

//		//Compile Backend All
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Compile_Backend_All_menuitem"), SWT.CTRL + SWT.ALT + 'C', new FileActions.RunBackendAll());

//		//Compile Frontend All
//		mi = new UIActionMenuItem(menu, SWT.PUSH, (IWindow)this, resources.getString("Compile_Frontend_All_menuitem"), SWT.CTRL + SWT.ALT + 'V', new FileActions.RunFrontendAll());

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
		if (e.getSource() instanceof Canvas)
		cur_comp = (Canvas)e.getSource();
		else if (e.getSource() instanceof ANodeTree)
		cur_comp = (ANodeTree)e.getSource();
	}

	public void focusLost(FocusEvent e) {
		if (e.getSource() instanceof Canvas)
		cur_comp = null;
		else if (e.getSource() instanceof ANodeTree)
		cur_comp = null;
	}

	public UIView getCurrentView() {
		Object cc = cur_comp;
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
		for (Editor e: editor_views) {
		if (e.the_root == fu || e.the_root.get$ctx_file_unit() == fu) {
		e.goToPath(path);
//		editors.setSelection(e.getView_canvas());
		e.getView_canvas().requestFocus();
		return;
		}
		}
		TabItem item = new TabItem (editors, SWT.NONE);
		item.setText(fu.pname());
		Composite edit_page = new Composite(editors, SWT.NONE);
		item.setControl(edit_page);

		Canvas edit_canvas = new Canvas(edit_page, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
				SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		tree_canvas.setLayoutData(gridData);

		edit_canvas.addFocusListener(this);
		editors.setSelection(item);
		Editor editor_view = new Editor  ((IWindow)this, edit_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt\u001fsyntax-for-java"));
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
		((Canvas)e.getView_canvas()).getShell().close();
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
