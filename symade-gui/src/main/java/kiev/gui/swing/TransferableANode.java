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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import kiev.vtree.INode;

/**
 * Transferable.
 */
public final class TransferableANode implements Transferable, ClipboardOwner {

	/**
	 * Transferable Flavor.
	 */
	private static DataFlavor transferableANodeFlavor;

	/**
	 * The node.
	 */
	public final INode node;

	/*
	 * The initializer.
	 */
	static {
		try {
			transferableANodeFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=kiev.vtree.ANode");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The constructor.
	 * @param node the node
	 */
	TransferableANode(INode node) {
		this.node = node;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
	 */
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {
				DataFlavor.stringFlavor,
				transferableANodeFlavor
		};
	}
	
	/* (non-Javadoc)
	 * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
	 */
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (DataFlavor df: getTransferDataFlavors()) 
			if( df.equals(flavor)) return true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
	 */
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException 	{
		if (transferableANodeFlavor.equals(flavor)) return node;
		if (DataFlavor.stringFlavor.equals(flavor)) return String.valueOf(node);
		throw new UnsupportedFlavorException(flavor);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.datatransfer.ClipboardOwner#lostOwnership(java.awt.datatransfer.Clipboard, java.awt.datatransfer.Transferable)
	 */
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	
	/**
	 * Get Transferable ANode Flavor.
	 * @return the transferableANodeFlavor
	 */
	static DataFlavor getTransferableANodeFlavor() {
		return transferableANodeFlavor;
	}
	
	/**
	 * Set Transferable ANode Flavor.
	 * @param transferableANodeFlavor the transferableANodeFlavor to set
	 */
	static void setTransferableANodeFlavor(DataFlavor transferableANodeFlavor) {
		TransferableANode.transferableANodeFlavor = transferableANodeFlavor;
	}
}

