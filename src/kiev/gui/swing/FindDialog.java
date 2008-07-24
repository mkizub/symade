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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import kiev.gui.Editor;
import kiev.vlang.FileUnit;
import kiev.vtree.ANode;
import kiev.vtree.TreeWalker;

public final class FindDialog extends JDialog implements ActionListener {
	private static final long serialVersionUID = -2062602325873536210L;
	private Editor the_view;
	private ANode cur_node;
	private JTextField text;
	private JOptionPane optionPane;
	FindDialog(JFrame parent, Editor the_view) {
		super(parent,"Find",false);
		this.the_view = the_view;
		cur_node = the_view.getCur_elem().node;
		this.text = new JTextField();
		JButton bnFind = new JButton("Find");
		JButton bnCancel = new JButton("Cancel");
		bnFind.setActionCommand("find");
		bnFind.addActionListener(this);
		bnCancel.setActionCommand("cancel");
		bnCancel.addActionListener(this);
		this.optionPane = new JOptionPane(
			text,
			JOptionPane.QUESTION_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION,
			null,
			new Object[]{bnFind,bnCancel},
			bnFind
		);
		setContentPane(this.optionPane);
	}
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
	private void lookup(final String txt) {
		try {
			cur_node.walkTree(new TreeWalker() {
				public boolean pre_exec(ANode n) {
					if (n.getClass().getName().indexOf(txt) >= 0)
						throw new FoundException(n);
					return true;
				}
			});
		} catch (FoundException e) {
			setNode(e.node);
		}
	}
	private void setNode(ANode n) {
		java.util.Vector<ANode> path = new java.util.Vector<ANode>();
		path.add(n);
		while (n.parent() != null) {
			n = n.parent();
			path.add(n);
			if (n instanceof FileUnit)
				break;
		}
		the_view.goToPath(path.toArray(new ANode[path.size()]));
	}
	static class FoundException extends RuntimeException {
		private static final long serialVersionUID = -3393988705244860916L;
		final ANode node;
		FoundException(ANode n) { this.node = n; }
	}
}

