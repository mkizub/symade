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

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.io.File;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Graphics2D;
import javax.swing.text.TextAction;

/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public abstract class UIView extends ANode implements MouseListener, ComponentListener  {

	/** The workplace window */
	protected Window		parent_window;
	/** The formatter of the current view */
	protected GfxFormatter	formatter;
	/** The root node to display */
	@ref public ANode		the_root;
	/** The root node of document we edit - the whole program */
	@ref public Drawable	view_root;
	/** The syntax in use */
	@ref public ATextSyntax	syntax;
	/** A flag to show auto-generated nodes */
	@att public boolean		show_auto_generated;

	public UIView(Window window, ATextSyntax syntax) {
		this.parent_window = window;
		this.syntax        = syntax;
	}
	
	public ATextSyntax getSyntax() { return syntax; }

	public void setSyntax(ATextSyntax syntax) {
		this.syntax = syntax;
		view_root = null;
		formatter.setSyntax(syntax);
		formatAndPaint(true);
	}
	
	public abstract void setRoot(ANode root) {}
	
	public abstract void formatAndPaint(boolean full);

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
}
