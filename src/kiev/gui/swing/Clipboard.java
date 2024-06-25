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
package kiev.gui.swing;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import kiev.vtree.INode;

/**
 * The Clipboard.
 */
public class Clipboard {

	/** 
	 * The object in clipboard. 
	 */
	private static final java.awt.datatransfer.Clipboard clipboard
			= java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
	
	/**
	 * The cached content.
	 */
	private static Object cachedContent;
	
	/**
	 * The cached time.
	 */
	private static long cachedTime;
	
	/**
	 * Sets the clipboard content.
	 * @param obj the object
	 */
	public static void setClipboardContent(Object obj) {
		Transferable tr = null;
		if (obj instanceof INode)
			tr = new TransferableANode((INode)obj);
		else
			tr = new StringSelection(String.valueOf(obj));
		Clipboard.clipboard.setContents(tr, (ClipboardOwner)tr);
	}
	
	/**
	 * Returns the clipboard content.
	 * @return the object
	 */
	public static Object getClipboardContent() {
		if (Math.abs(System.currentTimeMillis() - cachedTime) < 50)
			return cachedContent;
		INode node = null;
		Transferable content = Clipboard.clipboard.getContents(null);
		if (content.isDataFlavorSupported(TransferableANode.getTransferableANodeFlavor())) {
			try {
				node = (INode)content.getTransferData(TransferableANode.getTransferableANodeFlavor());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cachedContent = node;
		cachedTime = System.currentTimeMillis();
		return node;
	}

}
