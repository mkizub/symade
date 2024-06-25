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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * The Control editor just extend the custom <code>ControlEditor</code> 
 * by adding direct placement to the display.
 */
public class ControlEditor extends org.eclipse.swt.custom.ControlEditor {
	
	/**
	 * The super parent seems to be locked.
	 */
	protected final Composite parent;
	
	/**
	 * The editor.
	 */
	private Control editor;
	
	/**
	 * The location.
	 */
	private Point location;

	/**
	 * The Constructor.
	 * @param parent the composite
	 */
	public ControlEditor(Composite parent) {
		super(parent);
		this.parent = parent;

	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.custom.ControlEditor#setEditor(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setEditor (Control editor) {
		super.setEditor(editor);
		this.editor = editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.custom.ControlEditor#getEditor()
	 */
	@Override
	public Control getEditor () {
		return editor;
	}


	/**
	 * Returns the location of control.
	 * @return the location
	 */
	public Point getLocation() {
		return location;
	}

	/**
	 * Sets the location of control.
	 * @param location the location to set
	 */
	public void setLocation(Point location) {
		this.location = location;
	}

	/**
	 * Sets the location of the control widget relative to the display. 
	 * @param x the x coordinate
	 * @param y the y coordinate
	 */
	public void setLocation(int x, int y) {
		location = new Point(x, y);
	}

	/**
	 * Compute bounds used for align control position. If no alignment
	 * is set up then exact location is choose.
	 * @return the bounds
	 */
	private Rectangle calculateBounds() {
		final Rectangle clientArea = parent.getClientArea();
		final Rectangle editorRect = new Rectangle(clientArea.x, clientArea.y, minimumWidth, minimumHeight);

		if (grabHorizontal)
			editorRect.width = Math.max(clientArea.width, minimumWidth);

		if (grabVertical)
			editorRect.height = Math.max(clientArea.height, minimumHeight);

		switch (horizontalAlignment) {
		case SWT.RIGHT:
			editorRect.x += clientArea.width - editorRect.width;
			break;
		case SWT.LEFT:
			break;
		case SWT.NONE:
			if (location == null){
				editorRect.x += (clientArea.width - editorRect.width)/2;
				break;
			}
			editorRect.x = location.x;
			break;
		default:
			editorRect.x += (clientArea.width - editorRect.width)/2;
		}

		switch (verticalAlignment) {
		case SWT.BOTTOM:
			editorRect.y += clientArea.height - editorRect.height;
			break;
		case SWT.TOP:
			break;
		case SWT.NONE:
			if (location == null){
				editorRect.y += (clientArea.height - editorRect.height)/2;
				break;
			}
			editorRect.y = location.y;
			break;
		default :
			editorRect.y += (clientArea.height - editorRect.height)/2;
		}
		return editorRect;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.custom.ControlEditor#layout()
	 */
	@Override
	public void layout() {
		boolean hadFocus = false;
		if (editor == null || editor.isDisposed()) return;
		if (editor.getVisible()) {
			hadFocus = editor.isFocusControl();
		} 
		editor.setBounds(calculateBounds());
		if (hadFocus) {
			if (editor == null || editor.isDisposed()) return;
			editor.setFocus();
		}
	}

}
