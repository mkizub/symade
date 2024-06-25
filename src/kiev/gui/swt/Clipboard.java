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
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.MessageBox;

import kiev.vtree.INode;

/**
 * Put it out or get something to it.
 */
public final class Clipboard {
	
	/**
	 * We put object into clipboard and get content from it.
	 * @param obj the object
	 */
	public static void setClipboardContent(Object obj) {
		org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(Window.getDisplay());
		Transfer tr = null;
		if (obj instanceof INode){ 
			LocalObjectTransfer lot = LocalObjectTransfer.getTransfer();
			lot.setObject(obj);
			lot.setObjectSetTime(System.currentTimeMillis());
			tr = lot;
		} else {
			tr = TextTransfer.getInstance();
		}
		Object[] data = new Object[] {obj};
		Transfer[] transfers = new Transfer[] {tr};
		try {
			clipboard.setContents(data, transfers);
		} catch (SWTError e){
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD) {
				throw e;
			}
			MessageBox mb = new MessageBox(Window.getShell(), SWT.ICON_QUESTION | SWT.RETRY | SWT.CANCEL);
			mb.setMessage(Window.resources.getString("Clipboard_problem"));
			if (SWT.OK == mb.open())
				setClipboardContent(obj);
		}
		finally {
			clipboard.dispose();
		}
			
	}
	
	/**
	 * The Local Object transfer used to transfer object inside the JVM. Clipboard is
	 * a normal widget that we used to dispose.
	 * @return The clipboard content.
	 */
	public static Object getClipboardContent() {
		org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(Window.getDisplay());
		LocalObjectTransfer lot = LocalObjectTransfer.getTransfer();
		Object content = null;
		try {
			content = clipboard.getContents(lot);
		}
		finally {
			clipboard.dispose();
		}
		if (content == null){
			content = lot.getObject();
			assert (content == null);
		}
		return content;
	}

}
