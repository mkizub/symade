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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Hashtable;

import kiev.gui.event.BindingSet;
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
import kiev.gui.event.InputEventInfo;

public class Configuration {
	public static String EVENT_BINDINGS_NAME = "kiev/gui/swing/bindings";
	public static String EVENT_BINDINGS_FILE = "kiev/gui/swing/bindings.xml";
	
	static BindingSet bindings;

	public static void doGUIBeep() {
		java.awt.Toolkit.getDefaultToolkit().beep();
	}

	public static Hashtable<Object,UIActionFactory[]> getEditorActionMap() {
		Hashtable<Object,UIActionFactory[]> naviMap = new Hashtable<Object,UIActionFactory[]>();
		final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
		final int CTRL  = KeyEvent.CTRL_DOWN_MASK;
		final int ALT   = KeyEvent.ALT_DOWN_MASK;

//		naviMap.put(new InputEventInfo(0,1,		java.awt.event.MouseEvent.BUTTON1_MASK),	new UIActionFactory[]{new MouseActions.Select()});
		naviMap.put(new InputEventInfo(0,1,		java.awt.event.MouseEvent.BUTTON3_MASK),	new UIActionFactory[]{new MouseActions.PopupContextMenu()});

		naviMap.put(new InputEventInfo(ALT,		KeyEvent.VK_X),				new UIActionFactory[]{UIManager.newExprEditActionsFlatten()});

		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_UP),			new UIActionFactory[]{NavigateView.newLineUp()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_DOWN),			new UIActionFactory[]{NavigateView.newLineDn()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_PAGE_UP),		new UIActionFactory[]{NavigateView.newPageUp()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_PAGE_DOWN),		new UIActionFactory[]{NavigateView.newPageDn()});

		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_LEFT),			new UIActionFactory[]{NavigateEditor.newGoPrev()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_RIGHT),			new UIActionFactory[]{NavigateEditor.newGoNext()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_UP),			new UIActionFactory[]{NavigateEditor.newGoLineUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_DOWN),			new UIActionFactory[]{NavigateEditor.newGoLineDn()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_HOME),			new UIActionFactory[]{NavigateEditor.newGoLineHome()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_END),			new UIActionFactory[]{NavigateEditor.newGoLineEnd()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		new UIActionFactory[]{NavigateEditor.newGoPageUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		new UIActionFactory[]{NavigateEditor.newGoPageDn()});

		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_Z),				new UIActionFactory[]{EditActions.newUndo()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_C),				new UIActionFactory[]{EditActions.newCopy()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_X),				new UIActionFactory[]{EditActions.newCut()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_DELETE),		new UIActionFactory[]{EditActions.newDel()});

		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_F),				new UIActionFactory[]{UIManager.newFunctionExecutorFactory()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_F),				new UIActionFactory[]{UIManager.newFunctionExecutorFactory()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_O),				new UIActionFactory[]{FolderTrigger.newFactory()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_N),				new UIActionFactory[]{UIManager.newNewElemHereFactory()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_A),				new UIActionFactory[]{UIManager.newNewElemNextFactory()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_V),				new UIActionFactory[]{UIManager.newPasteHereFactory()});
		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_B),				new UIActionFactory[]{UIManager.newPasteNextFactory()});
//		naviMap.put(new InputEventInfo(CTRL,	KeyEvent.VK_E),				new UIActionFactory[]{new ChooseItemEditor()});
		
		for (Item item: bindings.items){
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
					UIActionFactory af = null;
					try {
						af = (UIActionFactory) Class.forName(bnd.action.actionClass).newInstance();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} 
					if (af != null && ei != null){
						naviMap.put(ei,	new UIActionFactory[]{af});
					}
				}
			}
		}
		
		naviMap.put(new InputEventInfo(0,KeyEvent.VK_E), new UIActionFactory[]{
			new kiev.gui.swing.TextEditor.Factory(),
			new kiev.gui.swing.IntEditor.Factory(),
			new kiev.gui.swing.EnumEditor.Factory(),
			new kiev.gui.swing.AccessEditor.Factory(),
			new kiev.gui.ChooseItemEditor()});
		naviMap.put(new InputEventInfo(0,KeyEvent.VK_O), new UIActionFactory[]{
			new kiev.gui.swing.FolderTrigger.Factory()});
		naviMap.put(new InputEventInfo(0,KeyEvent.VK_N), new UIActionFactory[]{
			new kiev.gui.swing.NewElemHere.Factory()});
		naviMap.put(new InputEventInfo(0,KeyEvent.VK_A), new UIActionFactory[]{
			new kiev.gui.swing.NewElemNext.Factory()});
		
		return naviMap;
	}
	public static Hashtable<Object,UIActionFactory[]> getProjectViewActionMap() {
		Hashtable<Object,UIActionFactory[]> naviMap = new Hashtable<Object,UIActionFactory[]>();

		naviMap.put(new InputEventInfo(0,1,		java.awt.event.MouseEvent.BUTTON1_MASK),	new UIActionFactory[]{new MouseActions.Select()});
		naviMap.put(new InputEventInfo(0,2,		java.awt.event.MouseEvent.BUTTON1_MASK),	new UIActionFactory[]{new MouseActions.TreeToggle()});

		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_UP),			new UIActionFactory[]{new NavigateView.LineUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_DOWN),			new UIActionFactory[]{new NavigateView.LineDn()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		new UIActionFactory[]{new NavigateView.PageUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		new UIActionFactory[]{new NavigateView.PageDn()});

		return naviMap;
	}
	public static Hashtable<Object,UIActionFactory[]> getInfoViewActionMap() {
		Hashtable<Object,UIActionFactory[]> naviMap = new Hashtable<Object,UIActionFactory[]>();
//		final int SHIFT = KeyEvent.SHIFT_DOWN_MASK;
//		final int CTRL  = KeyEvent.CTRL_DOWN_MASK;
//		final int ALT   = KeyEvent.ALT_DOWN_MASK;

		naviMap.put(new InputEventInfo(0,1,		java.awt.event.MouseEvent.BUTTON1_MASK),	new UIActionFactory[]{new MouseActions.RequestFocus()});
		naviMap.put(new InputEventInfo(0,1,		java.awt.event.MouseEvent.BUTTON3_MASK),	new UIActionFactory[]{new MouseActions.RequestFocus()});

		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_UP),			new UIActionFactory[]{new NavigateView.LineUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_DOWN),			new UIActionFactory[]{new NavigateView.LineDn()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_UP),		new UIActionFactory[]{new NavigateView.PageUp()});
		naviMap.put(new InputEventInfo(0,		KeyEvent.VK_PAGE_DOWN),		new UIActionFactory[]{new NavigateView.PageDn()});

		return naviMap;
	}
	
	public static Hashtable<Object,UIActionFactory[]> getTreeViewActionMap() {
		Hashtable<Object,UIActionFactory[]> naviMap = new Hashtable<Object,UIActionFactory[]>();
		naviMap.put(new InputEventInfo(0,2,		java.awt.event.MouseEvent.BUTTON1_MASK),	new UIActionFactory[]{new MouseActions.TreeToggle()});
		return naviMap;
	}

	public static BindingSet loadBindings(String name) {
		BindingSet bs = null;
		InputStream inp = null;
		try {
			inp = ClassLoader.getSystemResourceAsStream(name.replace('\u001f','/')+".ser");
			System.out.println("Loading event bindings from "+name.replace('\u001f','/')+".ser");
			ObjectInput oi = new ObjectInputStream(inp);
			bs = (BindingSet)oi.readObject();
			bs.init();
		} catch (Exception e) {
			System.out.println("Read error while bindings deserialization: "+e);
		} finally {
			if (inp != null)
				try {
					inp.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return bs;
	}
	
	static {
		bindings = loadBindings(EVENT_BINDINGS_NAME);
	//	bindings = DumpUtils.deserializeFromXmlFile(new File(EVENT_BINDINGS_FILE));
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

