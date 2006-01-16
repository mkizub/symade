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

@node
public class ASTNewInitializedArrayExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@node
	public static class ASTNewInitializedArrayExpressionImpl extends ENodeImpl {
		@att public int					dim;
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		public ASTNewInitializedArrayExpressionImpl() {}
		public ASTNewInitializedArrayExpressionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTNewInitializedArrayExpressionView of ASTNewInitializedArrayExpressionImpl extends ENodeView {
		public				int				dim;
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
	}
	
	@att public abstract virtual			int					dim;
	@att public abstract virtual			TypeRef				type;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	
	@getter public int				get$dim()				{ return this.getASTNewInitializedArrayExpressionView().dim; }
	@getter public TypeRef			get$type()				{ return this.getASTNewInitializedArrayExpressionView().type; }
	@getter public NArr<ENode>		get$args()				{ return this.getASTNewInitializedArrayExpressionView().args; }
	
	@setter public void		set$dim(int val)				{ this.getASTNewInitializedArrayExpressionView().dim = val; }
	@setter public void		set$type(TypeRef val)			{ this.getASTNewInitializedArrayExpressionView().type = val; }

	public NodeView							getNodeView()						{ return new ASTNewInitializedArrayExpressionView((ASTNewInitializedArrayExpressionImpl)this.$v_impl); }
	public ENodeView						getENodeView()						{ return new ASTNewInitializedArrayExpressionView((ASTNewInitializedArrayExpressionImpl)this.$v_impl); }
	public ASTNewInitializedArrayExpressionView	getASTNewInitializedArrayExpressionView()	{ return new ASTNewInitializedArrayExpressionView((ASTNewInitializedArrayExpressionImpl)this.$v_impl); }
	
	public ASTNewInitializedArrayExpression() {
		super(new ASTNewInitializedArrayExpressionImpl());
	}
	
	public void mainResolveOut() {
		Type tp = type.getType();
		while( this.dim > 0 ) { tp = new ArrayType(tp); this.dim--; }
		if( !tp.isArray() )
			throw new CompilerException(this,"Type "+type+" is not an array type");
        int dim = 0;
        while( tp.isArray() ) { dim++; tp = ((ArrayType)tp).arg; }
		replaceWithNode(new NewInitializedArrayExpr(pos,new TypeRef(tp),dim,args.delToArray()));
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

	public int		getPriority() { return Constants.opAccessPriority; }

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
