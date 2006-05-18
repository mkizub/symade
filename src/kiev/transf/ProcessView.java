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
		foreach (Struct dn; fu.members)
			this.autoGenerateMembers(dn);
	}
	
	public void autoGenerateMembers(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members) {
				this.autoGenerateMembers(dn);
			}
			return;
		}
		
		if (clazz.isForward() || getViewImpl(clazz) != null)
			return;
		
		clazz.setInterface(true);
		
		// add a cast from clazz.view_of to this view
		boolean cast_found = false;
		foreach (Method dn; clazz.view_of.getStruct().members) {
			if (dn.id.equals(nameCastOp) && dn.type.ret() ≈ clazz.xtype) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new Method(nameCastOp, clazz.xtype, ACC_PUBLIC|ACC_SYNTHETIC);
			if (clazz.isAbstract()) {
				cast.setAbstract(true);
			} else {
				cast.body = new Block();
				cast.block.stats.add(new ReturnStat(0, new ConstBoolExpr()));
			}
			clazz.view_of.getStruct().addMethod(cast);
		}
		// add a cast from this view to the clazz
		cast_found = false;
		foreach (Method dn; clazz.members) {
			if (dn.id.equals(nameCastOp) && dn.type.ret() ≈ clazz.view_of.getType()) {
				cast_found = true;
				break;
			}
		}
		if (!cast_found) {
			Method cast = new Method(nameCastOp, clazz.view_of.getType(), ACC_PUBLIC|ACC_SYNTHETIC|ACC_ABSTRACT);
			clazz.addMethod(cast);
		}
	}
	
	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return JavaViewBackend;
		return null;
	}
	
}

@singleton
class JavaViewBackend extends BackendProcessor implements Constants {

	private JavaViewBackend() {
		super(Kiev.Backend.Java15);
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

	////////////////////////////////////////////////////
	//	   PASS - preGenerate                         //
	////////////////////////////////////////////////////

	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode clazz) {
		if !( clazz.isStructView() ) {
			foreach (Struct dn; clazz.members)
				this.preGenerate(dn);
			return;
		}
		
		if (clazz.isForward() || getViewImpl(clazz) != null)
			return;
		
		// generate implementation
		Struct impl = Env.newStruct(nameIFaceImpl,true,clazz,ACC_PUBLIC|ACC_SYNTHETIC|ACC_FORWARD,true);
		impl.pos = clazz.pos;
		impl.setResolved(true);
		if (clazz.isAbstract()) {
			clazz.setAbstract(false);
			impl.setAbstract(true);
		}
		if (clazz.isFinal()) {
			clazz.setFinal(false);
			impl.setFinal(true);
		}
		clazz.iface_impl = impl;
		{
			foreach (TypeRef st; clazz.super_types)
				preGenerate(st.getStruct());
			Struct s = getViewImpl(clazz.super_types[0]);
			if (s != null)
				impl.super_types += new TypeRef(s.xtype);
			else
				impl.super_types += new TypeRef(Type.tpObject);
			impl.super_types += new TypeRef(clazz.xtype);
			
			if (clazz.super_types[0].getType() ≉ Type.tpObject)
				clazz.super_types.insert(0, new TypeRef(Type.tpObject));
		}
		clazz.members.append(impl);
		foreach (DNode dn; clazz.members.getArray()) {
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
				if (!cf.isPublic()) {
					Kiev.reportWarning(cf, "Field "+clazz+'.'+cf+" must be public");
					cf.setPublic();
				}
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
			if (dn instanceof Method && !(dn instanceof Constructor) && !dn.isPublic() && !dn.isStatic()) {
				Kiev.reportWarning(dn, "Method "+clazz+'.'+dn+" must be public");
				dn.setPublic();
			}
			impl.members.add(~dn);
		}
		
		// generate a field for the object this view represents
		Field fview = impl.resolveField(nameImpl, false);
		if (fview == null)
			fview = impl.addField(new Field(nameImpl,clazz.view_of.getType(), ACC_PUBLIC|ACC_FINAL|ACC_SYNTHETIC));

		// generate bridge methods
		foreach (Method m; impl.members) {
			if (m.isStatic() || m.isAbstract() || m.body != null)
				continue;
			CallType ct = m.type;
			Method vm;
			try {
				vm = clazz.view_of.getStruct().resolveMethod(m.id.uname, ct.ret(), ct.params());
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
				m.block.stats.add(new ReturnStat(m.pos, ce));
			else
				m.block.stats.add(new ExprStat(m.pos, ce));
		}
		
		// generate getter/setter methods
		foreach (Field f; impl.members) {
			Method mv_set = (Method)Field.SETTER_ATTR.get(f);
			if (mv_set != null && mv_set.isSynthetic()) {
				Method set_var = mv_set;
				Block body = new Block(f.pos);
				set_var.body = body;
				Field view_fld = clazz.view_of.getType().getStruct().resolveField(f.id.sname);
				ENode val = new LVarExpr(f.pos,set_var.params[0]);
				ENode ass_st = new ExprStat(f.pos,
					new AssignExpr(f.pos,AssignOperator.Assign,
						new IFldExpr(f.pos,
							new CastExpr(f.pos,
								clazz.view_of.getType(),
								new IFldExpr(f.pos,
									new ThisExpr(f.pos),
									fview
								)
							),
							view_fld
						),
						val
					)
				);
				body.stats.append(ass_st);
				body.stats.append(new ReturnStat(f.pos,null));
				if!(f.getType().isAutoCastableTo(view_fld.getType()))
					val.replaceWith(fun ()->ASTNode { return new CastExpr(f.pos,view_fld.getType(),~val); });
				set_var.setAbstract(false);
			}
			Method mv_get = (Method)Field.GETTER_ATTR.get(f);
			if (mv_get != null && mv_get.isSynthetic()) {
				Method get_var = mv_get;
				Block body = new Block(f.pos);
				get_var.body = body;
				ENode val = new IFldExpr(f.pos,
					new CastExpr(f.pos,
						clazz.view_of.getType(),
						new IFldExpr(f.pos,new ThisExpr(f.pos),fview)
					),
					clazz.view_of.getType().getStruct().resolveField(f.id.sname)
				);
				body.stats.add(new ReturnStat(f.pos,val));
				if!(val.getType().isAutoCastableTo(f.getType()))
					val.replaceWith(fun ()->ASTNode { return new CastExpr(f.pos,f.getType(),~val); });
				get_var.setAbstract(false);
			}
		}
		
		// add a cast from clazz.view_of to this view
		foreach (Method dn; clazz.view_of.getStruct().members) {
			if (dn.id.equals(nameCastOp) && dn.type.ret() ≈ clazz.xtype) {
				if (!dn.isAbstract() && dn.isSynthetic()) {
					dn.block.stats[0] = new ReturnStat(0, new NewExpr(0, impl.xtype, new ENode[]{new ThisExpr()}));
				}
				break;
			}
		}
		// add a cast from this view to the clazz
		boolean cast_found = false;
		foreach (Method dn; impl.members) {
			if (dn.id.equals(nameCastOp) && dn.type.ret() ≈ clazz.view_of.getType()) {
				if (dn.isSynthetic()) {
					dn.setAbstract(false);
					dn.body = new Block();
					dn.block.stats.add(new ReturnStat(0, new CastExpr(0, clazz.view_of.getType(), new IFldExpr(0, new ThisExpr(), fview))));
				}
				break;
			}
		}

		Kiev.runProcessorsOn(impl);
	}
	
}
