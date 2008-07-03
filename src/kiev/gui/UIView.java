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

import kiev.vtree.*;
import kiev.fmt.*;
import kiev.gui.event.ElementChangeListener;
import kiev.gui.event.ElementEvent;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * The abstract class for the view components of the GUI. 
 */
public abstract class UIView extends ANode 
implements MouseListener, ComponentListener, ElementChangeListener {

	/** The workplace window */
	protected Window			parent_window;
	/** The formatter of the current view */
	protected GfxFormatter		formatter;
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

	class BgFormatter extends Thread {
		private boolean do_format;
		BgFormatter() {
			this.setDaemon(true);
			this.setPriority(Thread.NORM_PRIORITY - 1);
		}
		public void run() {
			for (;;) {
				while (!do_format) {
					synchronized(this) { try {
						this.wait();
					} catch (InterruptedException e) {}
					}
					continue;
				}
				this.do_format = false;
				formatAndPaint(true);
			}
		}
		synchronized void schedule_run() {
			this.do_format = true;
			this.notify();
		}
	}

	public UIView(Window window, Draw_ATextSyntax syntax) {
		parent_window = window;
		this.syntax = syntax;
		bg_formatter = new BgFormatter();
		parent_window.addElementChangeListener(this);
		bg_formatter.start();
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

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		formatAndPaint(true);
	}
	
	public void elementChanged(ElementEvent e) {}

}
