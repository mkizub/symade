package kiev.fmt;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

public interface DrawDevice {
	public void draw(Drawable root);
}

public class TextPrinter implements DrawDevice {
	StringBuffer sb = new StringBuffer(1024);
	int pos_x = 0;
	int pos_y = 0;
	
	public TextPrinter(StringBuffer sb) {
		this.sb = sb;
	}
	
	public void draw(Drawable root) {
		DrawTerm leaf = root.getFirstLeaf();
		for (; leaf != null; leaf= leaf.getNextLeaf()) {
			int x = leaf.geometry.x;
			int y = leaf.geometry.y;

			if (leaf instanceof Space)
				continue;
			
			while (pos_y < y) {
				sb.append('\n');
				pos_y++;
				pos_x = 0;
			}
			while (pos_x < x) {
				sb.append(' ');
				pos_x++;
			}
			
			String text = leaf.getText();
			
			sb.append(text);
			pos_x += text.length();

			x += leaf.geometry.w;
			while (pos_x < x) {
				sb.append(' ');
				pos_x++;
			}
		}
	}
}

