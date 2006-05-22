/* Generated By:JJTree: Do not edit this line. ASTCastOperator.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;
import kiev.transf.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTCastOperator extends ASTOperator {

	@dflow(out="this:in") private static class DFI {}

	static final String fakeImage = "$cast";
	
	@virtual typedef This  = ASTCastOperator;
	@virtual typedef VView = VASTCastOperator;

	@att public TypeRef	type;
	@att public boolean  reinterp;
	@att public boolean  sure;

	@nodeview
	public static view VASTCastOperator of ASTCastOperator extends VASTOperator {
		public TypeRef	type;
		public boolean  reinterp;
		public boolean  sure;

		public boolean preResolveIn() {
			if (sure)
				return true;
			try {
				type.getType();
				return false;
			} catch (CompilerException e) {
				if !(type instanceof TypeNameRef)
					throw e;
			}
			TypeNameRef tnr = (TypeNameRef)type;
			String[] names = String.valueOf(tnr.ident).split("\\.");
			ENode e = new ASTIdentifier(type.pos, names[0]);
			for (int i=1; i < names.length; i++)
				e = new AccessExpr(type.pos, e, new SymbolRef(type.pos, names[i]));
			replaceWithNode(e);
			throw new ReWalkNodeException(e);
		}
	}

	public ASTCastOperator() {
		image = fakeImage;
	}

	public int getPriority() { return Constants.opCastPriority; }

	public Operator resolveOperator() {
		Type tp = type.getType();
	    return CastOperator.newCastOperator(tp,reinterp);
	}

	public String toString() { return (reinterp?"($reinterp ":"($cast ")+type+")"; }

    public Dumper toJava(Dumper dmp) {
    	dmp.append("(").append(type).append(')');
        return dmp;
    }

}
