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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import kiev.vtree.ANode;

public class TransferableANode implements Transferable, ClipboardOwner {
	private static DataFlavor transferableANodeFlavor;
	static {
		try {
			transferableANodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=kiev.vtree.ANode");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public final ANode node;
	public TransferableANode(ANode node) {
		this.node = node;
	}
    public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {
			DataFlavor.stringFlavor,
			transferableANodeFlavor
		};
	}
    public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor df: getTransferDataFlavors()) 
			if( df.equals(flavor))
				return true;
		return false;
	}
    public Object getTransferData(DataFlavor flavor)
		throws UnsupportedFlavorException
	{
		if (transferableANodeFlavor.equals(flavor))
			return node;
		if (DataFlavor.stringFlavor.equals(flavor))
			return String.valueOf(node);
		throw new UnsupportedFlavorException(flavor);
	}
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	/**
	 * @return the transferableANodeFlavor
	 */
	public static DataFlavor getTransferableANodeFlavor() {
		return transferableANodeFlavor;
	}
	/**
	 * @param transferableANodeFlavor the transferableANodeFlavor to set
	 */
	public static void setTransferableANodeFlavor(DataFlavor transferableANodeFlavor) {
		TransferableANode.transferableANodeFlavor = transferableANodeFlavor;
	}
}

