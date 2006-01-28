/* Generated By:JJTree: Do not edit this line. ASTNewInitializedArrayExpression.java */

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
public class ASTNewInitializedArrayExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = ASTNewInitializedArrayExpressionImpl;
	@virtual typedef VView = ASTNewInitializedArrayExpressionView;

	@nodeimpl
	public static class ASTNewInitializedArrayExpressionImpl extends ENodeImpl {
		@virtual typedef ImplOf = ASTNewInitializedArrayExpression;
		@att public int					dim;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
	}
	@nodeview
	public static view ASTNewInitializedArrayExpressionView of ASTNewInitializedArrayExpressionImpl extends ENodeView {
		public				int				dim;
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;

		public int		getPriority() { return Constants.opAccessPriority; }
	
		public void mainResolveOut() {
			Type tp = type.getType();
			while( this.dim > 0 ) { tp = new ArrayType(tp); this.dim--; }
			if( !tp.isArray() )
				throw new CompilerException(this,"Type "+type+" is not an array type");
			int dim = 0;
			while( tp.isArray() ) { dim++; tp = ((ArrayType)tp).arg; }
			replaceWithNode(new NewInitializedArrayExpr(pos,new TypeRef(tp),dim,args.delToArray()));
		}
	}
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTNewInitializedArrayExpression() {
		super(new ASTNewInitializedArrayExpressionImpl());
	}

	public void resolve(Type reqType) {
		Type tp;
		if( type == null ) {
			tp = reqType;
		} else {
			tp = type.getType();
			while( this.dim > 0 ) { tp = new ArrayType(tp); this.dim--; }
		}
		if( !tp.isArray() )
			throw new CompilerException(this,"Type "+type+" is not an array type");
		ArrayType at = (ArrayType)tp;
    	for(int i=0; i < args.length; i++) {
        	try {
				args[i].resolve(at.arg);
            } catch(Exception e) {
            	Kiev.reportError(args[i],e);
            }
        }
        int dim = 0;
        while( tp.isArray() ) { dim++; tp = ((ArrayType)tp).arg; }
		replaceWithNodeResolve(reqType, new NewInitializedArrayExpr(pos,new TypeRef(tp),dim,args.delToArray()));
	}

	public Dumper toJava(Dumper dmp) {
		dmp.append("new ").append(type);
		for(int i=0; i < dim; i++) dmp.append("[]");
		dmp.append('{');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 ) dmp.append(',').space();
		}
		dmp.append('}');
		return dmp;
	}
}
