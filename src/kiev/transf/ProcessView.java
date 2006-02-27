package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */
@singleton
public class ProcessView extends TransfProcessor implements Constants {

	private ProcessView() {
		super(Kiev.Ext.View);
	}
	
	private Struct getViewImpl(TypeRef tr) {
		if (tr == null) return null;
		Struct clazz = tr.getStruct();
		if (clazz == null) return null;
		return getViewImpl(clazz);
	}
	private Struct getViewImpl(Struct clazz) {
		if !(clazz.isStructView())
			return null;
		return clazz.iface_impl;
	}

	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	
	public void autoGenerateMembers(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.autoGenerateMembers(dn);
	}
	
	public void autoGenerateMembers(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (ASTNode dn; clazz.members; dn instanceof Struct) {
				this.autoGenerateMembers(dn);
			}
			return;
		}
		
		if (clazz.isForward() || getViewImpl(clazz) != null)
			return;
		
		clazz.setInterface(true);
		
		// generate implementation
		Struct impl = Env.newStruct(
			ClazzName.fromOuterAndName(clazz,nameIFaceImpl, false, true),
			clazz,
			ACC_PUBLIC | ACC_SYNTHETIC | ACC_FORWARD
			);
		impl.pos = clazz.pos;
		impl.setResolved(true);
		clazz.iface_impl = impl;
		{
			autoGenerateMembers(clazz.super_bound.getStruct());
			Struct s = getViewImpl(clazz.super_bound);
			if (s != null)
				impl.super_bound = new TypeRef(s.ctype);
			impl.interfaces.add(new TypeRef(clazz.ctype));
		}
		clazz.members.append(impl);
		foreach (DNode dn; clazz.members.toArray()) {
			if (dn instanceof Method && !(dn instanceof Constructor) && dn.isPublic() && !dn.isStatic()) {
				Method cm = dn;
				Block b = cm.body;
				if (b != null)
					~b;
				Method m = cm.ncopy();
				m.setFinal(false);
				m.setPublic();
				m.setAbstract(true);
				impl.members.add(~cm);
				clazz.addMethod(m);
				if (b != null)
					cm.body = b;
				continue;
			}
			else if (dn instanceof Field && !(dn.isStatic() && dn.isFinal())) {
				Field cf = dn;
				ENode b = cf.init;
				if (b != null)
					~b;
				Field f = cf.ncopy();
				f.setPublic();
				f.setAbstract(true);
				impl.members.add(~cf);
				clazz.addField(f);
				if (b != null)
					cf.init = b;
				continue;
			}
			if (dn instanceof Struct)
				continue;
			impl.members.add(~dn);
		}
		Kiev.runProcessorsOn(impl);
		
		// generate a field for the object this view represents
		Field fview = impl.resolveField(nameImpl, false);
		if (fview == null)
			fview = impl.addField(new Field(nameImpl,clazz.view_of.getType(), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));

		// generate bridge methods 
		foreach (DNode dn; impl.members; dn instanceof Method) {
			Method m = (Method)dn;
			if (m.isStatic() || m.isAbstract() || m.body != null)
				continue;
			CallType ct = m.type;
			Method vm;
			try {
				vm = clazz.view_of.getStruct().resolveMethod(m.name.name, ct.ret(), ct.params());
			} catch (CompilerException e) {
				Kiev.reportError(m, e.getMessage());
				m.setAbstract(true);
				continue;
			}
			m.body = new Block(m.pos);
			CallExpr ce = new CallExpr(m.pos,
				new CastExpr(m.pos, clazz.view_of.getType(),
					new IFldExpr(m.pos, new ThisExpr(m.pos), fview)
					),
				vm, ENode.emptyArray);
			foreach (FormPar fp; m.params)
				ce.args.append(new LVarExpr(fp.pos, fp));
			if (ct.ret() ≢ Type.tpVoid)
				m.body.stats.add(new ReturnStat(m.pos, ce));
			else
				m.body.stats.add(new ExprStat(m.pos, ce));
		}
		
		// add a cast from clazz.view_of to this view
		boolean cast_found = false;
		foreach (DNode dn; clazz.view_of.getStruct().members; dn instanceof Method) {
			if (dn.name.equals(nameCastOp) && dn.type.ret() ≈ clazz.ctype) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new Method(nameCastOp, clazz.ctype, ACC_PUBLIC|ACC_SYNTHETIC);
			if (clazz.isAbstract()) {
				cast.setAbstract(true);
			} else {
				cast.body = new Block();
				cast.body.stats.add(new ReturnStat(0, new NewExpr(0, impl.ctype, new ENode[]{new ThisExpr()})));
			}
			clazz.view_of.getStruct().addMethod(cast);
		}
		// add a cast from this view to the clazz
		cast_found = false;
		foreach (DNode dn; clazz.members; dn instanceof Method) {
			if (dn.name.equals(nameCastOp) && dn.type.ret() ≈ clazz.view_of) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new Method(nameCastOp, clazz.view_of.getType(), ACC_PUBLIC|ACC_SYNTHETIC);
			cast.body = new Block();
			cast.body.stats.add(new ReturnStat(0, new CastExpr(0, clazz.view_of.getType(), new IFldExpr(0, new ThisExpr(), fview))));
			impl.addMethod(cast);
		}
	}

}

