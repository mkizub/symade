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
import kiev.fmt.common.DrawLayoutInfo;
import kiev.fmt.common.IFmtGfx;
import kiev.vtree.AutoCompleteResult;
import kiev.vtree.INode;

/**
 * The <code>Canvas</code>es must implement this interface to be able 
 * create different implementations such as Swing and SWT and be able to communicate 
 * to the common code in <code>kiev.gui</code> package.
 * @see <code>kiev.gui.UIManager</code>
 */
public interface ICanvas {
	
	/**
	 * Returns the aggregated view.
	 * @return <code>IUIView</code>
	 */
	public IUIView getUIView();
	
	/**
	 * This allows to initialize peer. Note that peer couldn't self-initialize via 
	 * constructor because the creation order.
	 * @param uiv the view
	 */
	public void setUIView(IUIView uiv);
	
	/**
	 * Let us return this infamous graphics object.
	 * @return <code>IFmtGfx</code>
	 */
	public IFmtGfx getFmtGraphics();
	
	/**
	 * Get/init popup menu.
	 * @param listener listener for the menu
	 * @param menu initialization data
	 * @return an instance of popup menu peer interface
	 */
	public IPopupMenuPeer getPopupMenu(IPopupMenuListener listener, IMenu menu);
	
	/**
	 * Show/hide combo-box with provided auto-completition.
	 * @param autocomplete_result
	 * @param qualified
	 */
	public void setPopupComboContent(AutoCompleteResult autocomplete_result, boolean qualified); 
	
	/**
	 * The <code>Canvas</code> usually is able to redraw the client area.
	 */
	public void repaint();
	
	/**
	 * Usually we intentionally set focus instead of making request.
	 */
	public void requestFocus();
	
	/**
	 * Returns drawable at the given position. 
	 * @param x the position X
	 * @param y the position Y
	 * @return <code>Drawable</code>
	 */
	public Drawable getDrawableAt(int x, int y);

	/**
	 * Sets vertical offset (scroll down) in pixels.
	 * @param val the vertical offset
	 */
	public void setVertOffset(int val);
	
	/**
	 * Gets vertical offset (scroll down) in pixels.
	 * @return current vertical offset in pixels 
	 */
	public int getVertOffset();
	
	/**
	 * Sets horizontal offset (scroll right) in pixels.
	 * @param val the vertical offset
	 */
	public void setHorizOffset(int val);
	
	/**
	 * Gets horizontal offset (scroll down) in pixels.
	 * @return current horizontal offset in pixels 
	 */
	public int getHorizOffset();
	
	/**
	 * Increment the vertical offset by the <code>val</code> value.
	 * @param val the number of pixels to increment.
	 */
	public void incrVertOffset(int val);
	
	/**
	 * Gets the graphics image width.
	 * @return <code>int</code>
	 */
	public int getImgWidth();
	
	/**
	 * Gets the graphics image height. 
	 * @return <code>int</code>
	 */
	public int getImgHeight();
	
	/**
	 * Checks whether double buffering is used while painting the image.
	 * @return boolean
	 */
	public boolean isDoubleBuffered();
	
	/**
	 * Sets the <code>Drawable</code> root. 
	 * Drawables are implemented in the separate tree.
	 * @param dlb_root the root
	 */
	public void setDlb_root(DrawLayoutInfo dlb_root);
	
	/**
	 * Sets current <code>Drawable</code> leaf. This what we see and select in the 
	 * client area. With this drawable element is tied the current node. That's what
	 * we actually change. This is important to set these in pair. However, 
	 * the <code>drnode</code> attribute contains the needed value. 
	 * @param current the current drawable
	 * @param current_node the current node
	 */
	public void setCurrent(DrawTerm current, INode current_node);

	/**
	 * Returns the current <code>Drawable</code> leaf.
	 * @return <code>DrawTerm</code>
	 */
	public DrawTerm getCurrent();
	
	/**
	 * Returns the last visible line's graphical info.
	 * @return <code>DrawTermLayoutInfo</code>
	 */
	public DrawLayoutInfo getLast_visible();
	
	/**
	 * Returns the first visible line's graphical info.
	 * @return <code>DrawTermLayoutInfo</code>
	 */
	public DrawLayoutInfo getFirst_visible();
	
	/**
	 * Returns the position of the text caret.
	 * @return <code>int</code>
	 */
	public int getCursor_offset();
	
	/**
	 * Sets the position of the text caret. 
	 * @param cursor_offset the position
	 */
	public void setCursor_offset(int cursor_offset);
	
}
