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
package kiev.gui;

import java.util.Hashtable;

import kiev.gui.event.EventListenerList;

import kiev.fmt.Draw_ATextSyntax;
import kiev.fmt.Drawable;
import kiev.fmt.GfxFormatter;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;
import kiev.gui.event.InputEvent;
import kiev.vtree.ANode;

/**
 * The abstract class for the view components of the GUI. 
 */
public abstract class UIView extends ANode implements IUIView, ElementChangeListener {

	/** The workplace window */
	public final IWindow		parent_window;
	/** The GUI toolkit peer */
	public final IUIViewPeer	peer;
	/** The formatter of the current view */
	public GfxFormatter			formatter;
	/** The root node to display */
	public ANode				the_root;
	/** The root node of document we edit - the whole program */
	public Drawable				view_root;
	/** The syntax in use */
	public Draw_ATextSyntax		syntax;
	/** A flag to show auto-generated nodes */
	public boolean				show_auto_generated;
	/** A hint to show placeholders */
	public boolean				show_placeholders;
	/** A hint to show escaped idents and strings */
	public boolean				show_hint_escapes;
	
	/** A background thread to format and paint */
	protected BgFormatter	bg_formatter;

	public final Hashtable<Object,UIActionFactory[]> naviMap;

	public UIView(IWindow window, IUIViewPeer peer, Draw_ATextSyntax syntax) {
		this.parent_window = window;
		this.peer = peer;
		this.syntax = syntax;
		this.naviMap = UIManager.getUIActions(this);
	}
	
	public boolean isRegisteredToElementEvent() {
		EventListenerList l = parent_window.getListenerList();
		Object[] listeners = l.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ElementChangeListener.class) 
				if (((ElementChangeListener)listeners[i+1]) == this)
					return true;			
		}
		return false;
	}

	public Draw_ATextSyntax getSyntax() { return syntax; }

	public void setSyntax(Draw_ATextSyntax syntax) {
		this.syntax = syntax;
		view_root = null;
		formatAndPaint(true);
	}
	
	public abstract void setRoot(ANode root);
	
	public abstract void formatAndPaint(boolean full);

	public abstract void formatAndPaintLater(ANode node);

	public boolean inputEvent(InputEvent evt) {
		UIActionFactory[] actions = naviMap.get(evt);
		if (actions == null)
			return false;
		for (UIActionFactory af: actions) {
			Runnable r = af.getAction(new UIActionViewContext(parent_window, evt, this));
			if (r != null) {
				r.run();
				return true;
			}
		}
		return false;
	}
	
	public void elementChanged(ElementEvent e) {}

	/**
	 * @param bg_formatter the bg_formatter to set
	 */
	public BgFormatter setBg_formatter(BgFormatter bg_formatter) {
		this.bg_formatter = bg_formatter;
		return this.bg_formatter;
	}

	/**
	 * @return the item_editor
	 */
	public IUIViewPeer getViewPeer() {
		return this.peer;
	}

	/**
	 * @return the item_editor
	 */
	public Runnable getItem_editor() {
		return null;
	}

}
