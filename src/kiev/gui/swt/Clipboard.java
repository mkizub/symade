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

import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import kiev.vtree.ANode;

public class Clipboard {

	/** The object in clipboard */
	public static final org.eclipse.swt.dnd.Clipboard clipboard
			= new org.eclipse.swt.dnd.Clipboard(Window.display);;
	
	public static void setClipboardContent(Object obj) {
		Transfer tr = null;
		if (obj instanceof ANode) 
			tr = LocalObjectTransfer.getTransfer();
		else 
			tr = TextTransfer.getInstance();
		Object[] data = new Object[] {obj};
		Transfer[] transfers = new Transfer[] {tr};
		clipboard.setContents(data, transfers);
		
	}
	
	public static Object getClipboardContent() {
		Transfer tr = LocalObjectTransfer.getTransfer();
		Object content = clipboard.getContents(tr);
		return content;
	}

}
