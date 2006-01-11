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

public class ASTNewExpression extends ENode {

	@dflow(out="args") private static class DFI {
	@dflow(in="this:in", seq="true")		ENode[]		args;
	}
	
	@virtual typedef NImpl = ASTNewExpressionImpl;
	@virtual typedef VView = ASTNewExpressionView;

	@node
	public static class ASTNewExpressionImpl extends ENodeImpl {
		@virtual typedef ImplOf = ASTNewExpression;
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
	
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
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
		CompaundType sup  = (CompaundType)tp;
		clazz.setResolved(true);
		clazz.setLocal(true);
		clazz.setAnonymouse(true);
		clazz.setStatic(ctx_method==null || ctx_method.isStatic());
		TypeRef sup_tr = (TypeRef)this.type.copy();
		if( sup.isInterface() ) {
			clazz.super_type = Type.tpObject;
			clazz.interfaces.add(sup_tr);
		} else {
			clazz.super_bound = sup_tr;
		}

		{
			// Create default initializer, if number of arguments > 0
			if( args.length > 0 ) {
				Constructor init = new Constructor(ACC_PUBLIC);
				for(int i=0; i < args.length; i++) {
					args[i].resolve(null);
					init.params.append(new FormPar(pos,KString.from("arg$"+i),args[i].getType(),FormPar.PARAM_LVAR_PROXY,ACC_FINAL));
				}
				init.pos = pos;
				init.body = new BlockStat(pos);
				init.setPublic();
				clazz.addMethod(init);
			}
		}

        // Process inner classes and cases
		Kiev.runProcessorsOn(clazz);
		ENode ne = new NewExpr(pos,clazz.concr_type,args.toArray());
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
