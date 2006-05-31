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
public class PizzaFE_Pass3 extends TransfProcessor {
	private PizzaFE_Pass3() { super(Kiev.Ext.PizzaCase); }
	public String getDescr() { "Pizza case members" }

	public void process(ASTNode:ASTNode node) {
	}

	public void process(FileUnit:ASTNode fu) {
		foreach (ASTNode n; fu.members)
			process(n);
	}

	public void process(Struct:ASTNode clazz) {
		if !(clazz.isPizzaCase()) {
			foreach (Struct dn; clazz.members)
				this.process(dn);
			return;
		}
		if (clazz.isSingleton())
			return;
		MetaPizzaCase meta = clazz.getMetaPizzaCase();
		Field[] flds = Field.emptyArray;
		foreach (Field f; clazz.members) {
			flds = (Field[])Arrays.append(flds,f);
			meta.add(f);
		}
		// Create constructor for pizza case
		Constructor init = new Constructor(ACC_PUBLIC);
		foreach (Field f; flds)
			init.params.add(new FormPar(f.pos,f.id.sname,f.type,FormPar.PARAM_NORMAL,0));
		init.body = new Block(clazz.pos);
		int p = 0;
		foreach (Field f; flds) {
			Var v = null;
			foreach (FormPar fp; init.params; fp.id.uname == f.id.uname) {
				init.block.stats.insert(
					p++,
					new ExprStat(
						new AssignExpr(f.pos,AssignOperator.Assign,
							new IFldExpr(f.pos,new ThisExpr(f.pos),f),
							new LVarExpr(f.pos,fp)
						)
					)
				);
				break;
			}
		}
		init.pos = clazz.pos;
		clazz.addMethod(init);
	}
}

@singleton
public class PizzaME_PreGenerate extends BackendProcessor {
	private PizzaME_PreGenerate() { super(Kiev.Backend.Java15); }
	public String getDescr() { "Pizza case pre-generation" }

	public void process(ASTNode:ASTNode node) {
		return;
	}
	
	public void process(FileUnit:ASTNode fu) {
		foreach (Struct dn; fu.members)
			this.process(dn);
	}
	
	public void process(Struct:ASTNode clazz) {
		foreach (Struct dn; clazz.members)
			this.process(dn);
		if( clazz.isPizzaCase() ) {
			MetaPizzaCase meta = clazz.getMetaPizzaCase();
			//PizzaCaseAttr case_attr = (PizzaCaseAttr)clazz.getAttr(attrPizzaCase);
			Field ftag = clazz.addField(new Field(
				nameCaseTag,Type.tpInt,ACC_PUBLIC|ACC_FINAL|ACC_STATIC) );
			//ConstExpr ce = new ConstIntExpr(case_attr.caseno);
			ConstExpr ce = new ConstIntExpr(meta.getTag());
			ftag.init = ce;

			Method gettag = new Method(nameGetCaseTag,Type.tpInt,ACC_PUBLIC | ACC_SYNTHETIC);
			gettag.body = new Block(gettag.pos);
			gettag.block.stats.add(
				new ReturnStat(gettag.pos,new SFldExpr(ftag.pos,ftag))
			);
			clazz.addMethod(gettag);
		}
		else if( clazz.isHasCases() ) {
			// Add get$case$tag() method to itself
			Method gettag = new Method(Constants.nameGetCaseTag,Type.tpInt,ACC_PUBLIC | ACC_SYNTHETIC);
			gettag.body = new Block(gettag.pos);
			gettag.block.stats.add(
				new ReturnStat(gettag.pos,new ConstIntExpr(0))
			);
			clazz.addMethod(gettag);
		}

	}
}

