package kiev.gui;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.fmt.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Graphics2D;

/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public abstract class UIView extends ANode implements MouseListener, ComponentListener  {

	/** The workplace window */
	protected Window		parent_window;
	/** The canvas to show definition of current node */
	protected Canvas		view_canvas;
	/** The formatter of the current view */
	protected GfxFormatter	formatter;
	/** The root node to display */
	@ref public ANode		the_root;
	/** The root node of document we edit - the whole program */
	@ref public Drawable	view_root;
	/** The syntax in use */
	@ref public TextSyntax	syntax;

	public UIView(Window window, TextSyntax syntax, Canvas view_canvas) {
		this.parent_window = window;
		this.syntax        = syntax;
		this.view_canvas   = view_canvas;
		this.formatter     = new GfxFormatter(syntax, (Graphics2D)view_canvas.getGraphics());
		view_canvas.addMouseListener(this);
		view_canvas.addComponentListener(this);
	}
	
	public TextSyntax getSyntax() { return syntax; }
	public void setSyntax(TextSyntax syntax) {
		this.syntax = syntax;
		view_root = null;
		formatter.setSyntax(syntax);
		formatAndPaint(true);
	}
	
	public void setRoot(ANode root) {
		this.the_root = root;
		view_canvas.root = view_root = formatter.format(the_root, view_root);
	}
	
	public abstract void formatAndPaint(boolean full);

	public void mouseClicked(MouseEvent e) {
		view_canvas.requestFocus();
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void componentHidden(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentResized(ComponentEvent e) {
		formatAndPaint(true);
	}
}
