package kiev.gui.swing;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;

class AutoCompleteMenu extends JWindow {
	private final Canvas canvas;
	private JList theList;
	private DefaultListModel theModel;
	private Point theRelativePosition;

	private class WordMenuKeyListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				onSelected();
			}
		}
	}

	private class WordMenuMouseListener extends MouseAdapter {
		public void mouseClicked(MouseEvent e) {
			if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
				onSelected();
			}
		}
	}

	public AutoCompleteMenu(Canvas canvas) {
		super();
		this.canvas = canvas;
		theModel = new DefaultListModel();
		theRelativePosition = new Point(0, 0);
		loadUIElements();
		setEventManagement();
	}

	private void loadUIElements() {
		theList = new JList(theModel) {
			public int getVisibleRowCount() {
				return Math.min(theModel.getSize(), 10);
			}
		};
		theList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		theList.setBackground(new Color(235, 244, 254));
		JScrollPane scrollPane = new JScrollPane(theList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//scrollPane.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
		setContentPane(scrollPane);
	}

	private void setEventManagement() {
		theList.addKeyListener(new WordMenuKeyListener());
		theList.addMouseListener(new WordMenuMouseListener());
	}

	private void onSelected() {
		Object item = theList.getSelectedValue();
		canvas.setItem(item);
		setVisible(false);
	}

	public void display(int x, int y) {
		theRelativePosition = new Point(x, y);
		Point p = canvas.getLocationOnScreen();
		setLocation(new Point(p.x + theRelativePosition.x, p.y + theRelativePosition.y));
		setVisible(true);
	}

	public void move() {
		if (theRelativePosition != null) {
			Point p = canvas.getLocationOnScreen();
			setLocation(new Point(p.x + theRelativePosition.x, p.y + theRelativePosition.y));
		}
	}

	public void removeAllItems() {
		theModel.clear();
	}
	
	public void addItem(Object item) {
		theModel.addElement(item);
	}
	
	public int getItemCount() {
		return theModel.getSize();
	}
	
	public Object getSelectedItem() {
		return theList.getSelectedValue();
	}
	
	public void selectNone() {
		theList.setSelectedIndex(-1);
	}

	public void moveDown() {
		if (theModel.getSize() < 1) return;
		int current = theList.getSelectedIndex();
		int newIndex = Math.min(theModel.getSize() - 1, current + 1);
		theList.setSelectionInterval(newIndex, newIndex);
		theList.scrollRectToVisible(theList.getCellBounds(newIndex, newIndex));
	}

	public void moveUp() {
		if (theModel.getSize() < 1) return;
		int current = theList.getSelectedIndex();
		int newIndex = Math.max(0, current - 1);
		theList.setSelectionInterval(newIndex, newIndex);
		theList.scrollRectToVisible(theList.getCellBounds(newIndex, newIndex));
	}

	public void moveStart() {
		if (theModel.getSize() < 1) return;
		theList.setSelectionInterval(0, 0);
		theList.scrollRectToVisible(theList.getCellBounds(0, 0));
	}

	public void moveEnd() {
		if (theModel.getSize() < 1) return;
		int endIndex = theModel.getSize() - 1;
		theList.setSelectionInterval(endIndex, endIndex);
		theList.scrollRectToVisible(theList.getCellBounds(endIndex, endIndex));
	}

	public void movePageUp() {
		if (theModel.getSize() < 1) return;
		int current = theList.getSelectedIndex();
		int newIndex = Math.max(0, current - Math.max(0, theList.getVisibleRowCount() - 1));
		theList.setSelectionInterval(newIndex, newIndex);
		theList.scrollRectToVisible(theList.getCellBounds(newIndex, newIndex));
	}

	public void movePageDown() {
		if (theModel.getSize() < 1) return;
		int current = theList.getSelectedIndex();
		int newIndex = Math.min(theModel.getSize() - 1, current + Math.max(0, theList.getVisibleRowCount() - 1));
		theList.setSelectionInterval(newIndex, newIndex);
		theList.scrollRectToVisible(theList.getCellBounds(newIndex, newIndex));
	}
}
