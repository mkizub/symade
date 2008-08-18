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
import kiev.gui.Editor;
import kiev.gui.UIActionFactory;
import kiev.gui.UIActionViewContext;

public class Configuration {

	private static final int SHIFT  = org.eclipse.swt.SWT.SHIFT;
	private static final int CTRL   = org.eclipse.swt.SWT.CTRL;
	private static final int ALT    = org.eclipse.swt.SWT.ALT;

	private static BindingSet editorBindingsDefault;
	private static BindingSet editorBindings;
	private static EventActionMap editorNaviMap;

	private static BindingSet infoBindingsDefault;
	private static BindingSet infoBindings;
	private static EventActionMap infoNaviMap;

	private static BindingSet projectBindingsDefault;
	private static BindingSet projectBindings;
	private static EventActionMap projectNaviMap;

	public static void doGUIBeep() {
		//java.awt.Toolkit.getDefaultToolkit().beep();
	}
	
	public static void resetBindings() {
		editorBindings = editorBindingsDefault;
		infoBindings = infoBindingsDefault;
		projectBindings = projectBindingsDefault;
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
							ei = new InputEventInfo(mask, code); 
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
							ei = new InputEventInfo(mask, count, button); 
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
		addBindings(naviMap, editorBindings);
		return naviMap;
	}

	public static EventActionMap getProjectViewActionMap() {
		if (projectNaviMap != null)
			return projectNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		projectNaviMap = naviMap;
		addBindings(naviMap, projectBindings);
		return naviMap;
	}

	public static EventActionMap getInfoViewActionMap() {
		if (infoNaviMap != null)
			return infoNaviMap;
		
		EventActionMap naviMap = new EventActionMap();
		infoNaviMap = naviMap;
		addBindings(naviMap, infoBindings);
		return naviMap;
	}
	
	public static void attachBindings(BindingSet bs) {
		if ("kiev.gui.swt.bindings-editor".equals(bs.qname)) {
			editorBindings = bs;
			editorNaviMap = null;
		}
		if ("kiev.gui.swt.bindings-info".equals(bs.qname)) {
			infoBindings = bs;
			infoNaviMap = null;
		}
		if ("kiev.gui.swt.bindings-project".equals(bs.qname)) {
			projectBindings = bs;
			projectNaviMap = null;
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
		editorBindingsDefault = loadBindings("kiev/gui/swt/bindings-editor.ser");
		infoBindingsDefault = loadBindings("kiev/gui/swt/bindings-info.ser");
		projectBindingsDefault = loadBindings("kiev/gui/swt/bindings-project.ser");
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

