package kiev.gui.swing;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.border.BevelBorder;

import kiev.gui.IWindow;
import kiev.gui.UIView;

@SuppressWarnings("serial")
public class StatusBarInsMode extends JLabel {

	class MouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			e.consume();
			if (e.getClickCount() == 2) {
				UIView uiv = window.getCurrentView();
				if (uiv != null)
					uiv.setInInsertMode(!uiv.isInInsertMode());
			}
		}
	}
	
	private final IWindow window;
	
	StatusBarInsMode(IWindow window) {
		super(" ins ");
		this.window = window;
		this.setBorder(new BevelBorder(BevelBorder.LOWERED));
		this.addMouseListener(new MouseListener());
		this.setToolTipText("Insert or NodeTree mode");
	}
	
	void statusUpdate() {
		UIView uiv = window.getCurrentView();
		if (uiv == null || !uiv.isInInsertMode())
			setForeground(Color.LIGHT_GRAY);
		else
			setForeground(Color.BLACK);
	}
}
