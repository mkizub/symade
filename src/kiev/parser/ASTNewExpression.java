/* Generated By:JJTree: Do not edit this line. ASTNewExpression.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public class ASTNewExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@node
	public static class ASTNewExpressionImpl extends ENodeImpl {
		@att public TypeRef				type;
		@att public NArr<ENode>			args;
		@att public Struct				clazz;
		public ASTNewExpressionImpl() {}
		public ASTNewExpressionImpl(int pos) { super(pos); }
	}
	@nodeview
	public static view ASTNewExpressionView of ASTNewExpressionImpl extends ENodeView {
		public				TypeRef			type;
		public access:ro	NArr<ENode>		args;
		public				Struct			clazz;
	}
	
	@att public abstract virtual			TypeRef				type;
	@att public abstract virtual access:ro	NArr<ENode>			args;
	@att public abstract virtual			Struct				clazz;
	
	@getter public TypeRef			get$type()				{ return this.getASTNewExpressionView().type; }
	@getter public NArr<ENode>		get$args()				{ return this.getASTNewExpressionView().args; }
	@getter public Struct			get$clazz()				{ return this.getASTNewExpressionView().clazz; }
	
	@setter public void		set$type(TypeRef val)			{ this.getASTNewExpressionView().type = val; }
	@setter public void		set$clazz(Struct val)			{ this.getASTNewExpressionView().clazz = val; }

	public NodeView					getNodeView()				{ return new ASTNewExpressionView((ASTNewExpressionImpl)this.$v_impl); }
	public ENodeView				getENodeView()				{ return new ASTNewExpressionView((ASTNewExpressionImpl)this.$v_impl); }
	public ASTNewExpressionView		getASTNewExpressionView()	{ return new ASTNewExpressionView((ASTNewExpressionImpl)this.$v_impl); }
	
	public ASTNewExpression() {
		super(new ASTNewExpressionImpl());
	}

	public Type getType() {
		return type.getType();
	}
	
	public boolean preResolveIn(TransfProcessor proc) {
		// don't pre-resolve clazz
		Type tp = type.getType();
		tp.checkResolved();
		foreach (ENode a; args) proc.preResolve(a);
		return false;
	}
	
	public boolean mainResolveIn(TransfProcessor proc) {
		// don't pre-resolve clazz
		Type tp = type.getType();
		tp.checkResolved();
		proc.mainResolve(type);
		foreach (ENode a; args) proc.mainResolve(a);
		return false;
	}
	
	public void resolve(Type reqType) {
		// Find out possible constructors
		Type tp = type.getType();
		tp.checkResolved();
		Type[] targs = Type.emptyArray;
		if( args.length > 0 ) {
			targs = new Type[args.length];
			boolean found = false;
			Struct ss = tp.getStruct();
			foreach(ASTNode n; ss.members; n instanceof Method) {
				Method m = (Method)n;
				if (!(m.name.equals(nameInit) || m.name.equals(nameNewOp)))
					continue;
				Type[] mtargs = m.type.args;
				int i = 0;
				if( !ss.package_clazz.isPackage() && !ss.isStatic() ) i++;
				if( mtargs.length > i && mtargs[i].isInstanceOf(Type.tpTypeInfo) ) i++;
				if( mtargs.length-i != args.length )
					continue;
				found = true;
				for(int j=0; i < mtargs.length; i++,j++) {
					if( targs[j] == null )
						targs[j] = Type.getRealType(tp,mtargs[i]);
					else if( targs[j] ≡ Type.tpVoid )
						;
					else if( targs[j] ≉ Type.getRealType(tp,mtargs[i]) )
						targs[j] = Type.tpVoid;
				}
			}
			if( !found )
				throw new CompilerException(this,"Class "+tp+" do not have constructors with "+args.length+" arguments");
		}
		for(int i=0; i < args.length; i++) {
			try {
				if( targs[i] ≢ Type.tpVoid )
					args[i].resolve(targs[i]);
				else
					args[i].resolve(null);
			} catch(Exception e) {
				Kiev.reportError(args[i],e);
			}
		}
		if( clazz == null ) {
			replaceWithNodeResolve(reqType, new NewExpr(pos,tp,args.delToArray()));
			return;
		}
		// Local anonymouse class
		BaseType sup  = (BaseType)tp;
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		clazz.setStatic(ctx_method==null || ctx_method.isStatic());
		if( sup.isInterface() ) {
			clazz.super_type = Type.tpObject;
			clazz.interfaces.add(new TypeRef(sup));
		} else {
			clazz.super_type = sup;
		}

		{
			//clazz.type = Type.createRefType(clazz,Type.emptyArray);
			// Create default initializer, if number of arguments > 0
			if( args.length > 0 ) {
				MethodType mt;
				Type[] targs = Type.emptyArray;
				Vector<FormPar> params = new Vector<FormPar>();
				for(int i=0; i < args.length; i++) {
					args[i].resolve(null);
					Type at = args[i].getType();
					targs = (Type[])Arrays.append(targs,at);
					params.append(new FormPar(pos,KString.from("arg$"+i),at,FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
				}
				mt = new MethodType(targs,Type.tpVoid);
				Constructor init = new Constructor(mt,ACC_PUBLIC);
				init.params.addAll(params.toArray());
				init.pos = pos;
				init.body = new BlockStat(pos);
				init.setPublic(true);
				clazz.addMethod(init);
			}
		}

        // Process inner classes and cases
		Kiev.runProcessorsOn(clazz);
		ENode ne = new NewExpr(pos,clazz.type,args.toArray());
		ne.clazz = (Struct)~clazz;
		replaceWithNodeResolve(reqType, ne);
	}

	public int		getPriority() { return Constants.opAccessPriority; }

	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append(" new ").append(type).append('(');
		for(int i=0; i < args.length; i++) {
			sb.append(args[i]);
			if( i < args.length-1 )
				sb.append(',');
		}
		sb.append(')');
		if( clazz != null ) {
			sb.append("{ ... }");
		}
		return sb.toString();
	}

	public Dumper toJava(Dumper dmp) {
    	dmp.append("new").space().append(type).append('(');
		for(int i=0; i < args.length; i++) {
			args[i].toJava(dmp);
			if( i < args.length-1 )
				dmp.append(',');
		}
		dmp.append(')');
		if( clazz != null ) {
			dmp.space().append('{').newLine(1);
			for(int j=0; j < clazz.members.length; j++) dmp.append(clazz.members[j]);
			dmp.newLine(-1).append('}').newLine();
		}
		return dmp;
	}
}
