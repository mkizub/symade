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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import kiev.fmt.common.UIDrawPath;
import kiev.gui.Editor;
import kiev.vlang.FileUnit;
import kiev.vtree.AttrSlot;
import kiev.vtree.INode;
import kiev.vtree.ITreeWalker;

/**
 * Find Dialog.
 */
@SuppressWarnings("serial")
public final class FindDialog extends JDialog implements ActionListener {
	
	/**
	 * The view.
	 */
	private final Editor the_view;
	
	/**
	 * The current node.
	 */
	private INode cur_node;
	
	/**
	 * The text.
	 */
	private JTextField text;
	
	/**
	 * The option Pane.
	 */
	private JOptionPane optionPane;
	
	/**
	 * Found Exception
	 */
	class FoundException extends RuntimeException {
				
		/**
		 * The node.
		 */
		final INode node;
		
		/**
		 * The constructor.
		 * @param n the node
		 */
		FoundException(INode n) { node = n; }
	}

	/**
	 * The constructor.
	 * @param parent the parent
	 * @param the_view the view
	 */
	FindDialog(JFrame parent, Editor the_view) {
		super(parent, "Find", false);
		this.the_view = the_view;
		cur_node = the_view.getViewPeer().getCurrent().drnode;
		text = new JTextField();
		JButton bnFind = new JButton("Find");
		JButton bnCancel = new JButton("Cancel");
		bnFind.setActionCommand("find");
		bnFind.addActionListener(this);
		bnCancel.setActionCommand("cancel");
		bnCancel.addActionListener(this);
		optionPane = new JOptionPane(text, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
			null, new Object[]{bnFind,bnCancel}, bnFind);
		setContentPane(optionPane);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ("find".equals(e.getActionCommand())) {
			System.out.println("Find: "+text.getText());
			String txt = text.getText();
			if (txt != null && txt.length() > 0)
				lookup(txt);
		}
		else if ("cancel".equals(e.getActionCommand())) {
			this.dispose();
		}
	}
	
	/**
	 * lookup
	 * @param txt the text
	 */
	private void lookup(final String txt) {
		try {
			cur_node.walkTree(null, null, new ITreeWalker() {
				public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
					if (n.getClass().getName().indexOf(txt) >= 0)
						throw new FoundException(n);
					return true;
				}
			});
		} catch (FoundException e) {
			setNode(e.node);
		}
	}
	
	/**
	 * set Node.
	 * @param n the node
	 */
	private void setNode(INode n) {
		Vector<INode> path = new Vector<INode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
			if (n instanceof FileUnit)
				break;
		}
		the_view.goToPath(new UIDrawPath(path));
	}
	
}

