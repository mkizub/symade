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

import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;

import kiev.gui.event.BindingSet;
import kiev.gui.event.EventActionMap;
import kiev.gui.event.KeyboardEvent;
import kiev.gui.event.Event;
import kiev.gui.event.Binding;
import kiev.gui.event.Item;
import kiev.gui.event.MouseEvent;

import kiev.fmt.DrawFolded;
import kiev.fmt.Drawable;
import kiev.gui.EditActions;
import kiev.gui.Editor;
import kiev.gui.MouseActions;
import kiev.gui.NavigateEditor;
import kiev.gui.NavigateView;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;
import kiev.gui.UIManager;
import kiev.gui.ChooseItemEditor;

public class Configuration {

	private static final int SHIFT  = java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
	private static final int CTRL   = java.awt.event.KeyEvent.CTRL_DOWN_MASK;
	private static final int ALT    = java.awt.event.KeyEvent.ALT_DOWN_MASK;
	private static final int MOUSE1 = java.awt.event.MouseEvent.BUTTON1_MASK;
	//private static final int MOUSE2 = java.awt.event.MouseEvent.BUTTON2_MASK;
	private static final int MOUSE3 = java.awt.event.MouseEvent.BUTTON3_MASK;

	private static BindingSet editorBindingsDefault;
	private static BindingSet editorBindings;
	private static EventActionMap editorNaviMap;

	private static BindingSet infoBindingsDefault;
	private static BindingSet infoBindings;
	private static EventActionMap infoNaviMap;

	private static BindingSet projectBindingsDefault;
	private static BindingSet projectBindings;
	private static EventActionMap projectNaviMap;

	private static BindingSet treeBindingsDefault;
	private static BindingSet treeBindings;
	private static EventActionMap treeNaviMap;

	public static void doGUIBeep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}
	
	public static void resetBindings() {
		editorBindings = editorBindingsDefault;
		infoBindings = infoBindingsDefault;
		projectBindings = projectBindingsDefault;
		treeBindings = treeBindingsDefault;
	}
	
	private static void addBindings(EventActionMap naviMap, BindingSet bindings) {
		if (bindings == null)
			return;
		for (Item item: bindings.items){
			try {
				if (item instanceof Binding){
					Binding bnd = (Binding)item;
					for (Event event: bnd.events){
						InputEventInfo ei = null;
						if (event instanceof KeyboardEvent){
							KeyboardEvent kbe = (KeyboardEvent)event;
							int mask = 0, code;
							code = kbe.keyCode;
							if (kbe.withAlt) 
								mask |= ALT;
							else if (kbe.withCtrl) 
								mask |= CTRL;
							else if (kbe.withShift)
								mask |= SHIFT;  
							ei = new InputEventInfo(mask,	code); 
						} 
						else if (event instanceof MouseEvent){
							MouseEvent me = (MouseEvent)event;
							int mask = 0, button, count;
							count = me.count;
							button = me.button;
							if (me.withAlt) 
								mask |= ALT;
							else if (me.withCtrl) 
								mask |= CTRL;
							else if (me.withShift)
								mask |= SHIFT;  
							ei = new InputEventInfo(mask,	count, button); 
							System.out.println("Mouse event mask="+mask+", count="+count+", button="+button);
						}		
						//create instance of action class
						UIActionFactory af = (UIActionFactory) Class.forName(bnd.action.actionClass).newInstance();
						naviMap.add(ei,	af);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} 
		}
	}

	public static EventActionMap getEditorActionMap() {
		if (editorNaviMap != null)
			return editorNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		editorNaviMap = naviMap;

		naviMap.add(new InputEventInfo(0,1,		MOUSE1),	new MouseActions.Select());
		naviMap.add(new InputEventInfo(0,1,		MOUSE3),	new MouseActions.PopupContextMenu());

		naviMap.add(new InputEventInfo(ALT,		KeyEvent.VK_X),				UIManager.newExprEditActionsFlatten());

		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_UP),			NavigateView.newLineUp());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_DOWN),			NavigateView.newLineDn());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_PAGE_UP),		NavigateView.newPageUp());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_PAGE_DOWN),		NavigateView.newPageDn());

		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_LEFT),			NavigateEditor.newGoPrev());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_RIGHT),			NavigateEditor.newGoNext());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_UP),			NavigateEditor.newGoLineUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_DOWN),			NavigateEditor.newGoLineDn());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_HOME),			NavigateEditor.newGoLineHome());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_END),			NavigateEditor.newGoLineEnd());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		NavigateEditor.newGoPageUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		NavigateEditor.newGoPageDn());

		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_Z),				EditActions.newUndo());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_C),				EditActions.newCopy());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_X),				EditActions.newCut());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_DELETE),		EditActions.newDel());

		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_F),				UIManager.newFunctionExecutorFactory());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_F),				UIManager.newFunctionExecutorFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_O),				FolderTrigger.newFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_N),				UIManager.newNewElemHereFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_A),				UIManager.newNewElemNextFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_V),				UIManager.newPasteHereFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_B),				UIManager.newPasteNextFactory());
		naviMap.add(new InputEventInfo(CTRL,	KeyEvent.VK_E),				new ChooseItemEditor());
		
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_E), new kiev.gui.swing.TextEditor.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_E), new kiev.gui.swing.IntEditor.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_E), new kiev.gui.swing.EnumEditor.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_E), new kiev.gui.swing.AccessEditor.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_E), new kiev.gui.ChooseItemEditor());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_O), new kiev.gui.swing.FolderTrigger.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_N), new kiev.gui.swing.NewElemHere.Factory());
		naviMap.add(new InputEventInfo(0,KeyEvent.VK_A), new kiev.gui.swing.NewElemNext.Factory());

		addBindings(naviMap, editorBindings);
	
		return naviMap;
	}

	public static EventActionMap getProjectViewActionMap() {
		if (projectNaviMap != null)
			return projectNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		projectNaviMap = naviMap;

		naviMap.add(new InputEventInfo(0,1,		MOUSE1),	new MouseActions.Select());
		naviMap.add(new InputEventInfo(0,2,		MOUSE1),	new MouseActions.TreeToggle());

		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_UP),			new NavigateView.LineUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_DOWN),			new NavigateView.LineDn());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		new NavigateView.PageUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		new NavigateView.PageDn());

		addBindings(naviMap, projectBindings);
		return naviMap;
	}

	public static EventActionMap getInfoViewActionMap() {
		if (infoNaviMap != null)
			return infoNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		infoNaviMap = naviMap;

		naviMap.add(new InputEventInfo(0,1,		MOUSE1),	new MouseActions.RequestFocus());
		naviMap.add(new InputEventInfo(0,1,		MOUSE3),	new MouseActions.RequestFocus());

		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_UP),			new NavigateView.LineUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_DOWN),			new NavigateView.LineDn());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		new NavigateView.PageUp());
		naviMap.add(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		new NavigateView.PageDn());

		addBindings(naviMap, infoBindings);
		return naviMap;
	}
	
	public static EventActionMap getTreeViewActionMap() {
		if (treeNaviMap != null)
			return treeNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		treeNaviMap = naviMap;

		naviMap.add(new InputEventInfo(0,2,		MOUSE1),	new MouseActions.TreeToggle());

		addBindings(naviMap, treeBindings);
		return naviMap;
	}

	public static void attachBindings(BindingSet bs) {
		if ("kiev.gui.swing.bindings-editor".equals(bs.qname)) {
			editorBindings = bs;
			editorNaviMap = null;
		}
		if ("kiev.gui.swing.bindings-info".equals(bs.qname)) {
			infoBindings = bs;
			infoNaviMap = null;
		}
		if ("kiev.gui.swing.bindings-project".equals(bs.qname)) {
			projectBindings = bs;
			projectNaviMap = null;
		}
		if ("kiev.gui.swing.bindings-tree".equals(bs.qname)) {
			treeBindings = bs;
			treeNaviMap = null;
		}
	}
	
	public static BindingSet loadBindings(String name) {
		InputStream inp = null;
		try {
			inp = ClassLoader.getSystemResourceAsStream(name);
			System.out.println("Loading event bindings from "+name);
			ObjectInput oi = new ObjectInputStream(inp);
			BindingSet bs = (BindingSet)oi.readObject();
			bs.init();
			attachBindings(bs);
			return bs;
		} catch (Exception e) {
			System.out.println("Read error while bindings deserialization: "+e);
		} finally {
			try { inp.close(); } catch (Exception e) {}
		}
		return null;
	}
	
	static {
		editorBindingsDefault = loadBindings("kiev/gui/swing/bindings-editor.ser");
		infoBindingsDefault = loadBindings("kiev/gui/swing/bindings-info.ser");
		projectBindingsDefault = loadBindings("kiev/gui/swing/bindings-project.ser");
		treeBindingsDefault = loadBindings("kiev/gui/swing/bindings-tree.ser");
	}
}

final class FolderTrigger implements Runnable {
	private final Editor editor;
	private final DrawFolded df;
	FolderTrigger(Editor editor, DrawFolded df) {
		this.editor = editor;
		this.df = df;
	}
	public void run() {
		df.setDrawFolded(!df.getDrawFolded());
		editor.formatAndPaint(true);
	}

	public static Factory newFactory(){
		return new Factory();
	}
	
	final static class Factory implements UIActionFactory {
		public String getDescr() { return "Toggle folding"; }
		public boolean isForPopupMenu() { return true; }
		public Runnable getAction(UIActionViewContext context) {
			if (context.editor == null)
				return null;
			Editor editor = context.editor;
			for (Drawable dr = editor.getCur_elem().dr; dr != null; dr = (Drawable)dr.parent()) {
				if (dr instanceof DrawFolded)
					return new FolderTrigger(editor, (DrawFolded)dr);
			}
			return null;
		}
	}
}

