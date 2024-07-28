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
package kiev.gui;

import kiev.fmt.DrawTerm;
import kiev.fmt.Drawable;
import kiev.fmt.common.Draw_ATextSyntax;
import kiev.fmt.common.Draw_StyleSheet;
import kiev.vtree.INode;

/**
 * The interface intended to implement by views. 
 */
public interface IUIView {

	/**
	 * Get the root window. 
	 * @return the root node.
	 */
	public IWindow getWindow();
	
	/**
	 * Gets the peer. The peer actually aggregates this view. 
	 * @return <code>ICanvas</code>
	 */
	public ICanvas getViewPeer();

	/**
	 * Returns the syntax.
	 * @return <code>Draw_ATextSyntax</code>
	 */
	public Draw_ATextSyntax getSyntax();
	
	/**
	 * Sets the syntax.
	 * @param syntax the syntax
	 */
	public void setSyntax(Draw_ATextSyntax syntax);
	
	/**
	 * Returns the style.
	 */
	public Draw_StyleSheet getStyle();
	
	/**
	 * Sets the style.
	 * @param style the style
	 */
	public void setStyle(Draw_StyleSheet style);
	
	/**
	 * Get the root node. 
	 * @return the root node.
	 */
	public INode getRoot();
	
	/**
	 * Sets root node. 
	 * @param root the root.
	 */
	public void setRoot(INode root, boolean format);
	
	/**
	 * Get the view root node. 
	 * @return the view root node.
	 */
	public Drawable getViewRoot();

	/**
	 * Get currently selected node. 
	 * @return the selected node or null if nothing selected.
	 */
	public INode getSelectedNode();

	/**
	 * Get current DrawTerm under cursor. 
	 * @return the pointed DrawTerm or null.
	 */
	public DrawTerm getDrawTerm();

	/**
	 * Set new selected (by mouse or so) draw term.
	 * @param dr selected term
	 */
	public void selectDrawTerm(DrawTerm dr);
	
	/**
	 * Format and paint method.
	 * @param full include lag formatting
	 */
	public void formatAndPaint(boolean full);
	
	/**
	 * Format and paint asynchronously by the different thread. 
	 * @param node the node
	 */
	public void formatAndPaintLater();
	
	/**
	 * Checks if this viewable is in text (insert) mode
	 */
	public boolean isInInsertMode();
	
	/**
	 * Sets insert mode (for editors)
	 */
	public void setInInsertMode(boolean value);
	
}
