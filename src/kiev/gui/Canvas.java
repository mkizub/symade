package kiev.gui;

import java.awt.Graphics;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusListener;
import java.awt.image.VolatileImage;

import javax.swing.JScrollBar;

import kiev.fmt.DrawLayoutInfo;

public interface Canvas extends AdjustmentListener {
	
	public void setBounds(int x, int y, int width, int height);
	public void setFirstLine(int val);
	public void incrFirstLine(int val);
	public void adjustmentValueChanged(AdjustmentEvent e);
	public VolatileImage createVolatileImage(int w, int h);
	public void update(Graphics gScreen);
	public void paintComponent(Graphics gScreen);
	public boolean isDoubleBuffered();
	public JScrollBar getVerticalScrollBar();
	public void setDlb_root(DrawLayoutInfo dlb_root);
	public void addFocusListener(FocusListener l);
}
