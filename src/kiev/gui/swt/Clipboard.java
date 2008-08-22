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

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.MessageBox;

import kiev.vtree.ANode;

public class Clipboard {
	
	public static void setClipboardContent(Object obj) {
		org.eclipse.swt.dnd.Clipboard clipboard
		= new org.eclipse.swt.dnd.Clipboard(Window.display);
		Transfer tr = null;
		if (obj instanceof ANode){ 
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
			MessageBox mb = new MessageBox(Window.shell, SWT.ICON_QUESTION | SWT.RETRY | SWT.CANCEL);
			mb.setMessage("There was a problem when accessing the system clipboard. Retry?");
			if (SWT.OK == mb.open())
				setClipboardContent(obj);
		}
		finally {
			clipboard.dispose();
		}
			
	}
	
	public static Object getClipboardContent() {
		org.eclipse.swt.dnd.Clipboard clipboard
		= new org.eclipse.swt.dnd.Clipboard(Window.display);
		LocalObjectTransfer lot = LocalObjectTransfer.getTransfer();
		Transfer tr = lot;
		Object content = null;
		try {
			content = clipboard.getContents(tr);
		}
		finally {
			clipboard.dispose();
		}
		if (content == null){
			System.out.println("Clipboard is empty");
			content = lot.getObject();
		}
		return content;
	}

}
