/* Generated By:JJTree: Do not edit this line. ASTNewArrayExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public class ASTNewArrayExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = ASTNewArrayExpressionImpl;
	@virtual typedef VView = ASTNewArrayExpressionView;

	@nodeimpl
	public static class ASTNewArrayExpressionImpl extends ENodeImpl {
		@virtual typedef ImplOf = ASTNewArrayExpression;
		@att public int					dim;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		public ASTNewArrayExpressionImpl() {}
		public ASTNewArrayExpressionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTNewArrayExpressionView of ASTNewArrayExpressionImpl extends ENodeView {
		public				int				dim;
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTNewArrayExpression() {
		super(new ASTNewArrayExpressionImpl());
	}
	
	public Type getType() {
		Type t = ArrayType.newArrayType(this.type.getType());
		for (int i=1; i < dim; i++)
			t = ArrayType.newArrayType(t);
		return t;
	}
	
	public void resolve(Type reqType) {
    	for(int i=0; i < args.length; i++) {
        	try {
            	args[i].resolve(Type.tpInt);
            } catch(Exception e) {
            	Kiev.reportError(args[i],e);
            }
        }
		type.getType(); // resolve the type
		replaceWithNodeResolve(reqType, new NewArrayExpr(pos,(TypeRef)~type,args.delToArray(),dim));
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("new ").append(type);
		for(int i=0; i < dim; i++) {
			sb.append('[');
			if( i < args.length && args[i] != null ) sb.append(args[i]);
			sb.append(']');
		}
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(type);
		for(int i=0; i < dim; i++) {
			dmp.append('[');
			if( args.length > i && args[i] != null ) args[i].toJava(dmp);
			dmp.append(']');
		}
		return dmp;
	}
}
