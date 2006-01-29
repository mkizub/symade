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

@nodeset
public class ASTCastOperator extends ASTOperator {

	@dflow(out="this:in") private static class DFI {}

	static final KString fakeImage = KString.from("$cast");
	
	@virtual typedef This  = ASTCastOperator;
	@virtual typedef NImpl = ASTCastOperatorImpl;
	@virtual typedef VView = ASTCastOperatorView;

	@nodeimpl
	public static class ASTCastOperatorImpl extends ASTOperatorImpl {
		@virtual typedef ImplOf = ASTCastOperator;
		@att public TypeRef	type;
		@att public boolean  reinterp;
		@att public boolean  sure;
	}
	@nodeview
	public static view ASTCastOperatorView of ASTCastOperatorImpl extends ASTOperatorView {
		public TypeRef	type;
		public boolean  reinterp;
		public boolean  sure;

		public int		getPriority() { return Constants.opCastPriority; }

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
			String[] names = String.valueOf(tnr.name).split("\\.");
			ENode e = new ASTIdentifier(type.pos, KString.from(names[0]));
			for (int i=1; i < names.length; i++)
				e = new AccessExpr(type.pos, e, new NameRef(type.pos, KString.from(names[i])));
			replaceWithNode(e);
			throw ReWalkNodeException.instance;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	
	public ASTCastOperator() {
		super(new ASTCastOperatorImpl());
		image = fakeImage;
	}
	
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
