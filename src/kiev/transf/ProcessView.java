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
		
		Field fview = clazz.addField(new Field(nameView,clazz.view_of.getType(), ACC_PRIVATE|ACC_FINAL));

		foreach (DNode dn; clazz.members; dn instanceof Method) {
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
			m.body = new BlockStat(m.pos);
			CallExpr ce = new CallExpr(m.pos, new IFldExpr(m.pos, new ThisExpr(m.pos), fview), vm, ENode.emptyArray);
			foreach (FormPar fp; m.params)
				ce.args.append(new LVarExpr(fp.pos, fp));
			if (ct.ret() ≢ Type.tpVoid)
				m.body.addStatement(new ReturnStat(m.pos, ce));
			else
				m.body.addStatement(new ExprStat(m.pos, ce));
		}
	}

}

