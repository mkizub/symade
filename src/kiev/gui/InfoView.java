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


import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


/**
 * @author Maxim Kizub
 */
@node(copyable=false)
public class InfoView extends UIView implements KeyListener {

	public InfoView(Window window, TextSyntax syntax, Canvas info_canvas) {
		super(window, syntax, info_canvas);
		view_canvas.addKeyListener(this);
	}

	public void formatAndPaint(boolean full) {
		view_canvas.root = null;
		if (the_root != null && full)
			view_canvas.root = view_root = formatter.format(the_root);
		view_canvas.repaint();
	}

	public void keyReleased(KeyEvent evt) {
		//System.out.println(evt);
	}
	public void keyTyped(KeyEvent evt) {
		//System.out.println(evt);
	}
	
	public void keyPressed(KeyEvent evt) {
		//System.out.println(evt);
		int code = evt.getKeyCode();
		int mask = evt.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK|KeyEvent.ALT_DOWN_MASK);
		if (mask == 0) {
			switch (code) {
			case KeyEvent.VK_UP:
				if (view_canvas.first_line > 0) {
					--view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_DOWN:
				if (view_canvas.first_line < view_canvas.num_lines) {
					++view_canvas.first_line;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_PAGE_UP:
				if (view_canvas.first_line > 0) {
					view_canvas.first_line -= view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno - 1;
					if (view_canvas.first_line < 0)
						view_canvas.first_line = 0;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if (view_canvas.first_line < view_canvas.num_lines) {
					view_canvas.first_line += view_canvas.last_visible.geometry.lineno - view_canvas.first_visible.geometry.lineno -1;
					if (view_canvas.first_line >= view_canvas.num_lines)
						view_canvas.first_line = view_canvas.num_lines-1;
					view_canvas.repaint();
				}
				evt.consume(); 
				break;
			}
		}
	}
}
