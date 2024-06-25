package kiev.gui.swing;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

import kiev.gui.IEditor;
import kiev.gui.IWindow;
import kiev.gui.UIView;

@SuppressWarnings("serial")
public class StatusBarEditing extends JLabel {

	class MouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			e.consume();
			if (e.getClickCount() == 2) {
				UIView uiv = window.getCurrentView();
				if (uiv instanceof IEditor) {
					IEditor editor = (IEditor)uiv;
					if (editor.isInTextEditMode())
						editor.stopTextEditMode();
					statusUpdate();
				}
			}
		}
	}
	
	private final IWindow window;
	
	StatusBarEditing(IWindow window) {
		super(" edt ");
		this.window = window;
		this.setBorder(new BevelBorder(BevelBorder.LOWERED));
		this.addMouseListener(new MouseListener());
		this.setToolTipText("Currently in text editor");
	}
	
	void statusUpdate() {
		boolean on = false;
		UIView uiv = window.getCurrentView();
		if (uiv instanceof IEditor) {
			IEditor editor = (IEditor)uiv;
			if (editor.isInTextEditMode())
				on = true;
		}
		if (on)
			setForeground(Color.BLACK);
		else
			setForeground(Color.LIGHT_GRAY);
	}
}
