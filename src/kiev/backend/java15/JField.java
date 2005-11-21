package kiev.backend.java15;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@node
public class JField extends DNode implements Named, Typed, Accessable {
	
	@ref
	public final Field			vfield;
	
	public JField() {}
	
	public JField(Field vfield) {
		super(vfield.pos);
		this.vfield = vfield;
	}
}


