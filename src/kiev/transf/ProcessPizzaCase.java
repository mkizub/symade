package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class ProcessPizzaCase extends TransfProcessor implements Constants {
	
	private PizzaCaseBackend javaBackend = new PizzaCaseBackend();

	public ProcessPizzaCase(Kiev.Ext ext) {
		super(ext);
	}

	public void pass3(ASTNode:ASTNode node) {
	}

	public void pass3(FileUnit:ASTNode fu) {
		foreach (ASTNode n; fu.members)
			pass3(n);
	}

	public void pass3(Struct:ASTNode clazz) {
		if !(clazz.isPizzaCase()) {
			foreach (DNode dn; clazz.members; dn instanceof Struct)
				this.pass3(dn);
			return;
		}
		MetaPizzaCase meta = clazz.getMetaPizzaCase();
		Field[] flds = Field.emptyArray;
//		PizzaCaseAttr case_attr = (PizzaCaseAttr)clazz.getAttr(attrPizzaCase);
//		case_attr.casefields = Field.emptyArray;
		foreach (DNode dn; clazz.members; dn instanceof Field) {
			Field f = (Field)dn;
//			case_attr.casefields = (Field[])Arrays.append(case_attr.casefields,f);
			flds = (Field[])Arrays.append(flds,f);
			meta.add(f);
		}
		// Create constructor for pizza case
		Vector<Type> targs = new Vector<Type>();
		foreach (Field f; flds/*case_attr.casefields*/)
			targs.append(f.type);
		MethodType mt = MethodType.newMethodType(null,targs.toArray(),Type.tpVoid);
		Constructor init = new Constructor(mt,ACC_PUBLIC);
		init.pos = clazz.pos;
		foreach (Field f; flds/*case_attr.casefields*/)
			init.params.add(new FormPar(f.pos,f.name.name,f.type,FormPar.PARAM_NORMAL,0));
		clazz.addMethod(init);
		init.body = new BlockStat(clazz.pos);
		int p = 0;
		foreach (Field f; flds/*case_attr.casefields*/) {
			Var v = null;
			foreach (FormPar fp; init.params; fp.name.name == f.name.name) {
				init.body.stats.insert(
					new ExprStat(
						new AssignExpr(f.pos,AssignOperator.Assign,
							new IFldExpr(f.pos,new ThisExpr(f.pos),f),
							new LVarExpr(f.pos,fp)
						)
					),p++
				);
				break;
			}
		}
	}

	public BackendProcessor getBackend(Kiev.Backend backend) {
		if (backend == Kiev.Backend.Java15)
			return javaBackend;
		return null;
	}
	
}

final class PizzaCaseBackend extends BackendProcessor implements Constants {

	public PizzaCaseBackend() {
		super(Kiev.Backend.Java15);
	}
	
	public void preGenerate(ASTNode:ASTNode node) {
		return;
	}
	
	public void preGenerate(FileUnit:ASTNode fu) {
		foreach (DNode dn; fu.members; dn instanceof Struct)
			this.preGenerate(dn);
	}
	
	public void preGenerate(Struct:ASTNode clazz) {
		foreach (ASTNode dn; clazz.members; dn instanceof Struct) {
			this.preGenerate(dn);
		}
		if( clazz.isPizzaCase() ) {
			MetaPizzaCase meta = clazz.getMetaPizzaCase();
			//PizzaCaseAttr case_attr = (PizzaCaseAttr)clazz.getAttr(attrPizzaCase);
			Field ftag = clazz.addField(new Field(
				nameCaseTag,Type.tpInt,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
			//ConstExpr ce = new ConstIntExpr(case_attr.caseno);
			ConstExpr ce = new ConstIntExpr(meta.getTag());
			ftag.init = ce;

			Method gettag = new Method(nameGetCaseTag,
				MethodType.newMethodType(Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new SFldExpr(ftag.pos,ftag))
			);
			clazz.addMethod(gettag);
		}
		else if( clazz.isHasCases() ) {
			// Add get$case$tag() method to itself
			Method gettag = new Method(Constants.nameGetCaseTag,
				MethodType.newMethodType(Type.emptyArray,Type.tpInt),ACC_PUBLIC);
			gettag.body = new BlockStat(gettag.pos);
			((BlockStat)gettag.body).addStatement(
				new ReturnStat(gettag.pos,new ConstIntExpr(0))
			);
			clazz.addMethod(gettag);
		}

	}

}

