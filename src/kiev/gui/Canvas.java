package kiev.gui;

import java.awt.Graphics;

import kiev.fmt.DrawLayoutInfo;

public interface Canvas {
	
	public void setUIView(UIView uiv);
	public void setBounds(int x, int y, int width, int height);
	public void setFirstLine(int val);
	public void incrFirstLine(int val);
	public Graphics getGraphics();
	public int getImgWidth();
	public void repaint();
	public void requestFocus();
	public boolean isDoubleBuffered();
	public void setDlb_root(DrawLayoutInfo dlb_root);
}
