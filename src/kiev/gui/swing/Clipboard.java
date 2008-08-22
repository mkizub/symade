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
package kiev.gui.swing;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import kiev.vtree.ANode;

public class Clipboard {

	/** The object in clipboard */
	public static final java.awt.datatransfer.Clipboard clipboard
			= java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
	public static void setClipboardContent(Object obj) {
		Transferable tr = null;
		if (obj instanceof ANode)
			tr = new TransferableANode((ANode)obj);
		else
			tr = new StringSelection(String.valueOf(obj));
		Clipboard.clipboard.setContents(tr, (ClipboardOwner)tr);
	}
	
	public static Object getClipboardContent() {
		Transferable content = Clipboard.clipboard.getContents(null);
		if (!content.isDataFlavorSupported(TransferableANode.getTransferableANodeFlavor()))
			return null;
		ANode node = null;
		try {
			node = (ANode)content.getTransferData(TransferableANode.getTransferableANodeFlavor());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return node;
	}

}
