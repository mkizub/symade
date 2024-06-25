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
package kiev.gui.swt;

import java.util.ResourceBundle;

import kiev.fmt.SyntaxManager;
import kiev.fmt.common.UIDrawPath;
import kiev.gui.ClipboardActions;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.FileActions;
import kiev.gui.IEditor;
import kiev.gui.NavigateNode;
import kiev.gui.NewElemHere;
import kiev.gui.NewElemNext;
import kiev.gui.RenderActions;
import kiev.gui.UIAction;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIView;
import kiev.vlang.Env;
import kiev.vlang.FileUnit;
import kiev.vtree.INode;
import kiev.WorkerThreadGroup;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * <code>Window</code> is the main GUI start.
 */
public class Window extends kiev.gui.Window
implements SelectionListener, FocusListener {
	
	/**
	 * The shell.
	 */
	private static Shell shell;
	
	/**
	 * The display.
	 */
	private static Display display;
	
	/**
	 * Explorers tab folder.
	 */
	private TabFolder explorersFolder;
	
	/**
	 * Editors tab folder.
	 */
	private TabFolder editorsFolder;
	
	/**
	 * Views tab folder.
	 */
	private TabFolder viewsFolder;
	
	/**
	 * Split folders to the right.
	 */
	private SashForm split_right;
	
	/**
	 * Split folders to the bottom.
	 */
	private SashForm split_bottom;
	
	/**
	 * The editors array.
	 */
	private Editor[] editors = new Editor[0];
	
	
	/**
	 * The info view.
	 */
	private UIView info_view;
	
	/**
	 * The clipboard view.
	 */
	@SuppressWarnings("unused")
	private UIView clip_view;
		
	/**
	 * The project view.
	 */
	private ProjectView tree_view;
	
	/**
	 * The properties view.
	 */
	private UIView prop_view;  
	
	/**
	 * System color black.
	 */
	@SuppressWarnings("unused")
	private static Color swtColorBlack; 
	
	/**
	 * System color white.
	 */
	private static Color swtColorWhite; 
	
	/**
	 * System default font.
	 */
	@SuppressWarnings("unused")
	private static Font swtDefaultFont; 
	
	/**
	 * The display area.
	 */
	private Composite displayArea;
	
	/**
	 * Current focused object.
	 */
	private Canvas focused;
	

	/**
	 * The resources required by the GUI in current SWT implementation. 
	 */
	static ResourceBundle resources = ResourceBundle.getBundle("kiev.gui.swt.symade");

	
	/**
	 * The constructor.
	 * @param env the environment
	 */
	public Window(WorkerThreadGroup thrg) {
		super(thrg);
		display = new Display();
		shell = new Shell(display);
		shell.setText(resources.getString("Window_title"));	
		shell.setLayout(new FillLayout());
		shell.addShellListener (new ShellAdapter () {
			
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.ShellAdapter#shellClosed(org.eclipse.swt.events.ShellEvent)
			 */
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
		destroyGUI();
		display.dispose();
		System.exit(0);
	}

	/**
	 * Design and create GUI.
	 * @param parent the container.
	 */
	public void createGUI(Composite parent) {
		Display display = parent.getDisplay();
		GridData gridData;
		TabItem item;

		//create menu bar
		createMenuBar();

		// initialize colors
		swtColorWhite = display.getSystemColor(SWT.COLOR_WHITE);
		swtColorBlack = display.getSystemColor(SWT.COLOR_BLACK);		
		swtDefaultFont = display.getSystemFont();

		// create content composite
		displayArea = new Composite(parent, SWT.NONE);
		displayArea.setLayout(new FillLayout());

		//create sash
		split_right = new SashForm(displayArea, SWT.HORIZONTAL);
		Composite right_page = new Composite(split_right, SWT.NONE);
		right_page.setLayout(new FillLayout());
		split_bottom = new SashForm(right_page, SWT.VERTICAL);

		// create tab folders
		explorersFolder = new TabFolder(split_right, SWT.NONE);
		editorsFolder = new TabFolder(split_bottom, SWT.NONE);
		viewsFolder = new TabFolder(split_bottom, SWT.NONE);

		// positioning and split size
		split_right.setWeights(new int[]{2,1});
		split_right.SASH_WIDTH = 5;
		split_bottom.setWeights(new int[]{2,1});
		split_bottom.SASH_WIDTH = 5;
				
		// Project tree view tab
		item = new TabItem (explorersFolder, SWT.NONE);
		item.setText(resources.getString("Project_title"));
		Canvas tree_canvas = new Canvas(explorersFolder, SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		tree_canvas.setLayoutData(gridData);
		tree_canvas.addFocusListener(this);
		item.setControl(tree_canvas);

		// Info view tab
		item = new TabItem (viewsFolder, SWT.NONE);
		item.setText(resources.getString("Info_title"));
		Canvas info_canvas = new Canvas(viewsFolder, SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		info_canvas.setLayoutData(gridData);
		info_canvas.setBackground(swtColorWhite);
		info_canvas.addFocusListener(this);
		item.setControl(info_canvas);

		// Clipboard view tab
		item = new TabItem (viewsFolder, SWT.NONE);
		item.setText(resources.getString("Clipboard_title"));
		Canvas clip_canvas = new Canvas(viewsFolder, SWT.BORDER | SWT.V_SCROLL);
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		clip_canvas.setLayoutData(gridData);
		clip_canvas.addFocusListener(this);
		item.setControl(clip_canvas);

		// Inspector view tab
		item = new TabItem (viewsFolder, SWT.NONE);
		item.setText(resources.getString("Inspector_title"));
		Canvas prop_table = new Canvas(viewsFolder, SWT.BORDER | SWT.V_SCROLL);					
		gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		prop_table.setLayoutData(gridData);
		prop_table.addFocusListener(this);
		item.setControl(prop_table);
		
		// create views
		info_view = new UIView(this, info_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		clip_view = new UIView(this, clip_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		prop_view = new UIView(this, prop_table, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		tree_view = new ProjectView(this, tree_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-project-tree"));
		
		// hook listeners
		addListeners();
		
		// format and paint on custom GC
		tree_view.setRoot(getCurrentProject(), true);
		tree_view.formatAndPaint(true);
	
		// select projects tab
		explorersFolder.setSelection(findTabItem(tree_canvas));
		tree_canvas.requestFocus();
		
		displayArea.pack();

		//position the windows on the screen
		Rectangle screenSize = display.getClientArea();
		parent.setSize(screenSize.width*4/5, screenSize.height*4/5-20);
	}

	/**
	 * Destroy. 
	 */
	private void destroyGUI() {
		removeListeners();		
	}

	/**
	 * Returns the display.
	 * @return the display
	 */
	final static Display getDisplay(){return display;}
	
	/**
	 * Returns the shell.
	 * @return the shell
	 */
	final static Shell getShell(){return shell;}
		
	/**
	 * Creates the menu bar.
	 */
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


	/**
	 * Creates File menu.
	 * @return the menu
	 */
	Menu createFileMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);
		MenuItem item;

		//New 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("New_menuitem"), SWT.ALT + 'N', new FileActions.NewFile());

		//Add 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Add_menuitem"), SWT.ALT + 'A', new FileActions.AddFile());

		//Load 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Load_menuitem"), SWT.CTRL + 'L', new FileActions.LoadFileAs());

		//Save As...
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("SaveAs_menuitem"), SWT.ALT + 'S', new FileActions.SaveFileAs());

		//Save
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Save_menuitem"), SWT.CTRL + 'S', new FileActions.SaveFile());


		//Close
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Close_menuitem"), SWT.CTRL + 'W', new EditActions.CloseWindow());

		new MenuItem(menu, SWT.SEPARATOR);
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

	/**
	 * Exit.
	 */
	void menuFileExit() {
		shell.close ();
	}

	/**
	 * Creates Edit menu.
	 * @return the menu
	 */
	Menu createEditMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);

		//Undo 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Undo_menuitem"), SWT.CTRL + 'Z', new EditActions.Undo());

		//Copy
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Copy_menuitem"), SWT.CTRL + 'C', new EditActions.Copy());

		//Cut
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Cut_menuitem"), SWT.CTRL + 'X', new EditActions.Cut());

		//Del
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Del_menuitem"), SWT.DEL, new EditActions.Del());

		//Paste here
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Paste_here_menuitem"), SWT.CTRL + 'V',  new ClipboardActions.PasteHereFactory());

		//Paste next  
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Paste_next_menuitem"), SWT.CTRL + 'B',  new ClipboardActions.PasteHereFactory());

		new MenuItem(menu, SWT.SEPARATOR);

		//New Element Here
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("New_Element_Here_menuitem"), SWT.CTRL + 'N',  new NewElemHere.Factory());

		//New Element Next
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("New_Element_Next_menuitem"), SWT.CTRL + 'A',  new NewElemNext.Factory());

		//Edit Element
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Edit_Element_menuitem"), SWT.CTRL + 'E',  new EditActions.ChooseItemEditor());

		//Select Parent
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Select_Parent_menuitem"), SWT.ALT + SWT.UP,  new NavigateNode.NodeUp());

		//Insert Mode
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Insert_Mode_menuitem"), SWT.ALT + 'I',  new NavigateNode.InsertMode());
		
		//Enter key code
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Enter_Key_Code_menuitem"), SWT.CTRL + 'K',  new KeyCodeEditor.Factory());

		//Enter mouse code
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Enter_Mouse_Code_menuitem"), SWT.ALT + 'K',  new MouseButtonEditor.Factory());
		
		return menu;
	}

	/**
	 * Creates Render menu.
	 * @return the menu
	 */
	Menu createRenderMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);

		//Syntax As... 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Syntax_As_menuitem"), SWT.CTRL + SWT.ALT + 'S', new RenderActions.SyntaxFileAs());

		//Unfold all
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Unfold_all_menuitem"), SWT.CTRL + SWT.ALT + 'O', new RenderActions.OpenFoldedAll());

		//Fold all
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Fold_all_menuitem"), SWT.CTRL + SWT.ALT + SWT.SHIFT+ 'O', new RenderActions.CloseFoldedAll());

		new MenuItem(menu, SWT.SEPARATOR);

		//Placeholders
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Placeholders_menuitem"), SWT.CTRL + SWT.ALT + 'P',  new RenderActions.ToggleShowPlaceholders());

		//Redraw
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Redraw_menuitem"), SWT.CTRL + SWT.ALT+ 'R',  new RenderActions.Redraw());

		return menu;
	}

	/**
	 * Creates Compiler menu.
	 * @return the menu
	 */
	Menu createCompilerMenu() {
		Menu bar = shell.getMenuBar();
		Menu menu = new Menu(bar);

		//Merge Tree 
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Merge_Tree_menuitem"), SWT.CTRL + SWT.ALT + 'M', new FileActions.MergeTreeAll());

		//Compile Backend All
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Compile_Backend_All_menuitem"), SWT.CTRL + SWT.ALT + 'C', new FileActions.RunBackendAll());

		//Compile Frontend All
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Compile_Frontend_All_menuitem"), SWT.CTRL + SWT.ALT + 'V', new FileActions.RunFrontendAll());

		//Compile Frontend
		new UIActionMenuItem(menu, SWT.PUSH, this, resources.getString("Compile_Frontend_menuitem"), SWT.ALT + 'V', new FileActions.RunFrontend());

		return menu;
	}

	/**
	 * Add listeners.
	 */
	private void addListeners() {
		addElementChangeListener(info_view);
		addElementChangeListener(prop_view);
		editorsFolder.addSelectionListener(this);
	}

	/**
	 * Remove listeners.
	 */
	private void removeListeners() {
		removeElementChangeListener(info_view);
		removeElementChangeListener(prop_view);
		if (Helper.okToUse(editorsFolder)) editorsFolder.removeSelectionListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusGained(FocusEvent e) {
		if (e.getSource() instanceof Canvas) focused = (Canvas)e.getSource();			
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
	 */
	public void focusLost(FocusEvent e) {}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#getCurrentView()
	 */
	public UIView getCurrentView() {
		if (views == null) return null;
		for (UIView v: (UIView[])views) if (v.getViewPeer() == focused)	return v;
		return null;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#openEditor(kiev.vlang.FileUnit, kiev.vtree.INode[])
	 */
	public IEditor openEditor(FileUnit fu, INode[] path) {
		
		// check if the editor is in use
		for (Editor e: editors) {
			if (e.getFileUnit() == fu || Env.ctxFileUnit(e.getRoot()) == fu) {
				e.goToPath(new UIDrawPath(path));
				Canvas can = (Canvas)e.getViewPeer();
				TabItem ti = findTabItem(editorsFolder, can);
				if (ti != null) editorsFolder.setSelection(ti);
				can.requestFocus();
				return e;
			}
		}
		
		// create editor, make sure the creation order	
		
		// create canvas
		Canvas edit_canvas = new Canvas(editorsFolder, SWT.BORDER | SWT.V_SCROLL);		
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);		
		edit_canvas.setLayoutData(gridData);
		edit_canvas.addFocusListener(this);
	
		// create editor
		Editor editor = new Editor(this, edit_canvas, SyntaxManager.loadLanguageSyntax("stx-fmt·syntax-for-java"));
		editors = (Editor[])kiev.stdlib.Arrays.append(editors, editor);

		// format and paint
		editor.setFileUnit(fu);
		editor.formatAndPaint(true);
		editor.goToPath(new UIDrawPath(path));
		
		// crate tab item
		TabItem item = new TabItem (editorsFolder, SWT.NONE);
		item.setText(fu.getFname());
		item.setControl(edit_canvas);

		// select tab
		editorsFolder.setSelection(item);	
		edit_canvas.requestFocus();
		addElementChangeListener(editor);
		enableMenuItems();
		
		return editor;
	}

	/* (non-Javadoc)
	 * @see kiev.gui.IWindow#closeEditor(kiev.gui.Editor)
	 */
	public void closeEditor(IEditor editor) {
		// shrink editors
		java.util.Vector<Editor> v = new java.util.Vector<Editor>();
		for (Editor e: editors) {
			if (e != editor) {v.add(e); continue;}
			Canvas can = (Canvas)e.getViewPeer();
			TabItem ti = findTabItem(can);
			if (ti != null) ti.dispose();			
		}
		editors = v.toArray(new Editor[v.size()]);

		// shrink views
		java.util.Vector<UIView> w = new java.util.Vector<UIView>();
		for (UIView e: views) if (e != editor) w.add(e);
		views = w.toArray(new UIView[w.size()]);

		// select tab
		if (editors.length > 0)	{
			Canvas can = (Canvas)editors[0].getViewPeer();
			editorsFolder.setSelection(findTabItem(can));
			can.requestFocus();
		}
		removeElementChangeListener((Editor)editor);
		enableMenuItems();

		super.closeEditor(editor);
	}

	/**
	 * Finding tab item in the canvas.
	 * @param can the canvas
	 * @return the tab item
	 */
	private TabItem findTabItem(Canvas can){
		TabFolder tf = (TabFolder)can.getParent();
		return findTabItem(tf, can);
	}
	
	/**
	 * Finding tab item in the canvas.
	 * @param tf the tab folder
	 * @param can the canvas
	 * @return the tab item
	 */
	private TabItem findTabItem(TabFolder tf, Canvas can){
		for (TabItem ti: tf.getItems()) if (ti.getControl() == can) return ti;			
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
		Object src = e.getSource();
		if (src instanceof MenuItem) {
			UIActionMenuItem m = (UIActionMenuItem)((MenuItem)src).getData();
			InputEventInfo evt = new InputEventInfo(e);
			final UIAction action = m.getFactory().getAction(new UIActionViewContext(this, evt, getCurrentView()));
			if (action != null)
				getDisplay().asyncExec(new Runnable(){
					public void run() { action.exec(); }
				});
		} else if (src instanceof TabFolder) {
			TabFolder tf = (TabFolder)src;
			TabItem ti = tf.getItem(tf.getSelectionIndex());		
			Canvas can = (Canvas)ti.getControl();
			if (can != null) can.requestFocus();
			enableMenuItems();		
		}
	}
	
	/* (non-Javadoc)
	 * @see kiev.gui.Window#enableMenuItems()
	 */
	@Override
	protected void enableMenuItems(){
		for (MenuItem menu: shell.getMenuBar().getItems())
			for (MenuItem item: menu.getMenu().getItems()) {
				UIActionMenuItem data = (UIActionMenuItem)item.getData();
				if (data != null) item.setEnabled(data.checkEnabled());					
			}						
	}

	public void updateStatusBar() {}

	/**
	 * Notify that errors list may be changed.
	 */
	public void fireErrorsModified() {}

}
