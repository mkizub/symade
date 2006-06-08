/* Generated By:JJTree: Do not edit this line. ASTOperator.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Op")
public final class ASTOperator extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = ASTOperator;
	@virtual typedef VView = VASTOperator;

	@att public String		image;

	@setter
	public void set$image(String value) {
		this.image = (value != null) ? value.intern() : null;
	}
	
	@nodeview
	public static view VASTOperator of ASTOperator extends VENode {
		public String		image;
	}
	
	ASTOperator() {}
	ASTOperator(Token t) {
		this.pos = t.getPos();
		this.image = t.image;
	}
	
	public void resolve(Type reqType) {
		throw new RuntimeException();
	}

	public String toString() {
		return image.toString();
	}
    
    public Dumper toJava(Dumper dmp) {
    	dmp.space().append(image).space();
        return dmp;
    }
}

