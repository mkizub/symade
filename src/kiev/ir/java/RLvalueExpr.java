package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.AccessExpr.AccessExprImpl;
import kiev.vlang.AccessExpr.AccessExprView;
import kiev.vlang.IFldExpr.IFldExprImpl;
import kiev.vlang.IFldExpr.IFldExprView;
import kiev.vlang.ContainerAccessExpr.ContainerAccessExprImpl;
import kiev.vlang.ContainerAccessExpr.ContainerAccessExprView;
import kiev.vlang.ThisExpr.ThisExprImpl;
import kiev.vlang.ThisExpr.ThisExprView;
import kiev.vlang.LVarExpr.LVarExprImpl;
import kiev.vlang.LVarExpr.LVarExprView;
import kiev.vlang.SFldExpr.SFldExprImpl;
import kiev.vlang.SFldExpr.SFldExprView;
import kiev.vlang.OuterThisAccessExpr.OuterThisAccessExprImpl;
import kiev.vlang.OuterThisAccessExpr.OuterThisAccessExprView;
import kiev.vlang.ReinterpExpr.ReinterpExprImpl;
import kiev.vlang.ReinterpExpr.ReinterpExprView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 359 $
 *
 */

@nodeview
public static final view RAccessExpr of AccessExprImpl extends AccessExprView {
	
	public void resolve(Type reqType) throws CompilerException {
		ENode[] res;
		Type[] tps;

		// resolve access
		obj.resolve(null);

	try_static:
		if( obj instanceof TypeRef ) {
			tps = new Type[]{ ((TypeRef)obj).getType() };
			res = new ENode[1];
			if( ident.name.equals(nameThis) )
				res[0] = new OuterThisAccessExpr(pos,tps[0].getStruct());
		}
		else {
			ENode e = obj;
			tps = e.getAccessTypes();
			res = new ENode[tps.length];
			for (int si=0; si < tps.length; si++) {
				Type tp = tps[si];
				if( ident.name.equals(nameLength) ) {
					if( tp.isArray() ) {
						tps[si] = Type.tpInt;
						res[si] = new ArrayLengthExpr(pos,e.ncopy(), ident.ncopy());
					}
				}
			}
			// fall down
		}
		for (int si=0; si < tps.length; si++) {
			if (res[si] != null)
				continue;
			Type tp = tps[si];
			DNode@ v;
			ResInfo info;
			if (!(obj instanceof TypeRef) &&
				tp.resolveNameAccessR(v,info=new ResInfo(this,ResInfo.noStatic|ResInfo.noImports),ident.name) )
				res[si] = makeExpr(v,info,~obj);
			else if (tp.resolveStaticNameR(v,info=new ResInfo(this),ident.name))
				res[si] = makeExpr(v,info,tp.getStruct());
		}
		int cnt = 0;
		int idx = -1;
		for (int si=0; si < res.length; si++) {
			if (res[si] != null) {
				cnt ++;
				if (idx < 0) idx = si;
			}
		}
		if (cnt > 1) {
			StringBuffer msg = new StringBuffer("Umbigous access:\n");
			for(int si=0; si < res.length; si++) {
				if (res[si] == null)
					continue;
				msg.append("\t").append(res).append('\n');
			}
			msg.append("while resolving ").append(this);
			throw new CompilerException(this, msg.toString());
		}
		if (cnt == 0) {
			StringBuffer msg = new StringBuffer("Unresolved access to '"+ident+"' in:\n");
			for(int si=0; si < res.length; si++) {
				if (tps[si] == null)
					continue;
				msg.append("\t").append(tps[si]).append('\n');
			}
			msg.append("while resolving ").append(this);
			this.obj = this.obj;
			throw new CompilerException(this, msg.toString());
			//return;
		}
		this.replaceWithNodeResolve(reqType,~res[idx]);
	}
}

@nodeview
public static final view RIFldExpr of IFldExprImpl extends IFldExprView {
	public void resolve(Type reqType) throws RuntimeException {
		obj.resolve(null);

		// Set violation of the field
		if( ctx_method != null
		 && obj instanceof LVarExpr && ((LVarExpr)obj).ident.equals(nameThis)
		)
			ctx_method.addViolatedField(var);

		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RContainerAccessExpr of ContainerAccessExprImpl extends ContainerAccessExprView {

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		obj.resolve(null);
		if( obj.getType().getStruct() != null ) {
			// May be an overloaded '[]' operator, ensure overriding
			Struct s = obj.getType().getStruct();
		lookup_op:
			for(;;) {
				s.checkResolved();
				if (s instanceof Struct) {
					Struct ss = (Struct)s;
					foreach(ASTNode n; ss.members; n instanceof Method && ((Method)n).name.equals(nameArrayOp))
						break lookup_op;
				}
				if( s.super_type != null ) {
					s = s.super_type.clazz;
					continue;
				}
				throw new RuntimeException("Resolved object "+obj+" of type "+obj.getType()+" is not an array and does not overrides '[]' operator");
			}
		}
		index.resolve(null);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RThisExpr of ThisExprImpl extends ThisExprView {

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		if (ctx_method != null &&
			ctx_method.isStatic() &&
			ctx_clazz.name.short_name != nameIFaceImpl
		)
			Kiev.reportError(this,"Access '"+toString()+"' in static context");
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public final view RLVarExpr of LVarExprImpl extends LVarExprView {

	static final KString namePEnv = KString.from("$env");

	public boolean preGenerate() {
		if (getVar().isLocalRuleVar()) {
			RuleMethod rm = (RuleMethod)ctx_method;
			assert(rm.params[0].type ≡ Type.tpRule);
			Var pEnv = null;
			foreach (ENode n; rm.body.stats; n instanceof VarDecl) {
				VarDecl vd = (VarDecl)n;
				if (vd.var.name.equals(namePEnv)) {
					assert(vd.var.type.isInstanceOf(Type.tpRule));
					pEnv = vd.var;
					break;
				}
			}
			if (pEnv == null) {
				Kiev.reportError(this, "Cannot find "+namePEnv);
				return false;
			}
			Struct s = ((LocalStructDecl)rm.body.stats[0]).clazz;
			Field f = s.resolveField(ident.name);
			replaceWithNode(new IFldExpr(pos, new LVarExpr(pos, pEnv), ~ident, f));
		}
		return true;
	}
	
	public void resolve(Type reqType) throws RuntimeException {
		// Check if we try to access this var from local inner/anonymouse class
		if( ctx_clazz.isLocal() ) {
			if( getVar().ctx_clazz != this.ctx_clazz ) {
				var.setNeedProxy(true);
				setAsField(true);
				// Now we need to add this var as a fields to
				// local class and to initializer of this class
				Field vf;
				if( (vf = ctx_clazz.resolveField(ident.name,false)) == null ) {
					// Add field
					vf = ctx_clazz.addField(new Field(ident.name,var.type,ACC_PUBLIC));
					vf.setNeedProxy(true);
					vf.init = this.getENode().ncopy();
				}
			}
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RSFldExpr of SFldExprImpl extends SFldExprView {

	public void resolve(Type reqType) throws RuntimeException {
		if( isResolved() ) return;
		// Set violation of the field
		if( ctx_method != null )
			ctx_method.addViolatedField(var);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view ROuterThisAccessExpr of OuterThisAccessExprImpl extends OuterThisAccessExprView {

	public void resolve(Type reqType) throws RuntimeException {
		outer_refs.delAll();
		trace(Kiev.debugResolve,"Resolving "+this);
		Field ou_ref = OuterThisAccessExpr.outerOf(ctx_clazz);
		if( ou_ref == null )
			throw new RuntimeException("Outer 'this' reference in non-inner or static inner class "+ctx_clazz);
		do {
			trace(Kiev.debugResolve,"Add "+ou_ref+" of type "+ou_ref.type+" to access path");
			outer_refs.append(ou_ref);
			if( ou_ref.type.isInstanceOf(outer.ctype) ) break;
			ou_ref = OuterThisAccessExpr.outerOf(ou_ref.type.getStruct());
		} while( ou_ref!=null );
		if( !outer_refs[outer_refs.length-1].type.isInstanceOf(outer.ctype) )
			throw new RuntimeException("Outer class "+outer+" not found for inner class "+ctx_clazz);
		if( Kiev.debugResolve ) {
			StringBuffer sb = new StringBuffer("Outer 'this' resolved as this");
			for(int i=0; i < outer_refs.length; i++)
				sb.append("->").append(outer_refs[i].name);
			System.out.println(sb.toString());
		}
		if( ctx_method.isStatic() && !ctx_method.isVirtualStatic() ) {
			throw new RuntimeException("Access to 'this' in static method "+ctx_method);
		}
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

@nodeview
public static final view RReinterpExpr of ReinterpExprImpl extends ReinterpExprView {

	public void resolve(Type reqType) {
		trace(Kiev.debugResolve,"Resolving "+this);
		expr.resolve(null);
		Type type = this.getType();
		Type extp = expr.getType();
		if (type ≈ extp) {
			replaceWithNode(~expr);
			return;
		}
		if (type.isIntegerInCode() && extp.isIntegerInCode())
			;
		else if (extp.isInstanceOf(type))
			;
		else if (type instanceof CTimeType && type.getEnclosedType() ≈ extp)
			;
		else if (extp instanceof CTimeType && extp.getEnclosedType() ≈ type)
			;
		else
			Kiev.reportError(this, "Cannot reinterpret "+extp+" as "+type);
		setResolved(true);
		if (isAutoReturnable())
			ReturnStat.autoReturn(reqType, this);
	}
}

