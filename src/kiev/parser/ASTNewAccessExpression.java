/* Generated By:JJTree: Do not edit this line. ASTNewAccessExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class ASTNewAccessExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in")				ENode		obj;
	@dflow(in="obj", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = ASTNewAccessExpressionImpl;
	@virtual typedef VView = ASTNewAccessExpressionView;

	@node
	public static class ASTNewAccessExpressionImpl extends ENodeImpl {
		@virtual typedef ImplOf = ASTNewAccessExpression;
		@att public ENode				obj;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		public ASTNewAccessExpressionImpl() {}
		public ASTNewAccessExpressionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTNewAccessExpressionView of ASTNewAccessExpressionImpl extends ENodeView {
		public				ENode			obj;
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTNewAccessExpression() {
		super(new ASTNewAccessExpressionImpl());
	}
	
	public void resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
        	try {
            	args[i].resolve(null);
            } catch(Exception e) {
            	Kiev.reportError(args[i],e);
            }
        }
		replaceWithNodeResolve(reqType, new NewExpr(pos,type.getType(),args.delToArray(),(ENode)~obj));
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public Dumper toJava(Dumper dmp) {
    	dmp.append(obj).append('.').append("new").space().append(type).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		return dmp.append(')');
	}
}
