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


public class DrawGeometry {
	public boolean is_hidden; // don't draw/print
	public int     do_newline; // used by formatter to mark actual new-lines after a DrawTerm
	public int     lineno; // line number for text-kind draw/print formatters
	public int     x, y, w, h, b;
}

