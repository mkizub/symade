/* Generated By:JJTree: Do not edit this line. ASTIdentifier.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="Id")
public class ASTIdentifier extends ENode {

	@dflow(out="this:in") private static class DFI {}

	@virtual typedef This  = ASTIdentifier;

	@att public String name;

	@setter
	public void set$name(String value) {
		this.name = (value != null) ? value.intern() : null;
	}
	
	public ASTIdentifier() {}

	public ASTIdentifier(String name) {
		this.name = name;
	}

	public ASTIdentifier(Token t) {
		this.pos = t.getPos();
		this.name = t.image;
	}

	public ASTIdentifier(int pos, String name) {
		this.pos = pos;
		this.name = name;
	}

	public int getPriority() { return 256; }

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = t.image;
	}
	
	public boolean preResolveIn() {
		if( name == Constants.nameThis ) {
			ThisExpr te = new ThisExpr(pos);
			replaceWithNode(te);
			return false;
		}
		else if( name == Constants.nameSuper ) {
			ThisExpr te = new ThisExpr(pos);
			te.setSuperExpr(true);
			replaceWithNode(te);
			return false;
		}

		// resolve in the path of scopes
		ASTNode@ v;
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR((ASTNode)this,v,info,name) )
			throw new CompilerException(this,"Unresolved identifier "+name);
		if( v instanceof Opdef ) {
			ASTOperator op = new ASTOperator();
			op.pos = pos;
			op.image = name;
			replaceWithNode(op);
		}
		else if( v instanceof TypeDecl ) {
			TypeDecl td = (TypeDecl)v;
			td.checkResolved();
			replaceWithNode(new TypeRef(td.getType()));
		}
		else {
			replaceWithNode(info.buildAccess((ASTNode)this, null, v).closeBuild());
		}
		return false;
	}

	public void resolve(Type reqType) {
		if( name == Constants.nameFILE ) {
			replaceWithNode(new ConstStringExpr(Kiev.curFile));
			return;
		}
		else if( name == Constants.nameLINENO ) {
			replaceWithNode(new ConstIntExpr(pos>>>11));
			return;
		}
		else if( name == Constants.nameMETHOD ) {
			if( ctx_method != null )
				replaceWithNode(new ConstStringExpr(ctx_method.id.sname));
			else
				replaceWithNode(new ConstStringExpr(nameInit));
			return;
		}
		else if( name == Constants.nameDEBUG ) {
			replaceWithNode(new ConstBoolExpr(Kiev.debugOutputA));
			return;
		}
		else if( name == Constants.nameReturnVar ) {
			Kiev.reportWarning(this,"Keyword '$return' is deprecated. Replace with 'Result', please");
			name = Constants.nameResultVar;
		}
		DNode@ v;
		ResInfo info = new ResInfo(this);
		if( !PassInfo.resolveNameR(this,v,info,name) ) {
			if( name.startsWith(Constants.nameDEF) ) {
				String prop = name.toString().substring(2);
				String val = Env.getProperty(prop);
				if( val == null ) val = Env.getProperty(prop.replace('_','.'));
				if( val != null ) {
					if( reqType ≡ null || reqType ≈ Type.tpString) {
						replaceWithNode(new ConstStringExpr(val));
						return;
					}
					if( reqType.isBoolean() ) {
						if( val == "" ) 
							replaceWithNode(new ConstBoolExpr(true));
						else
							replaceWithNode(new ConstBoolExpr(Boolean.valueOf(val).booleanValue()));
						return;
					}
					if( reqType.isInteger() ) {
						replaceWithNode(new ConstIntExpr(Integer.valueOf(val).intValue()));
						return;
					}
					if( reqType.isNumber() ) {
						replaceWithNode(new ConstDoubleExpr(Double.valueOf(val).doubleValue()));
					}
					replaceWithNode(new ConstStringExpr(val));
					return;
				}
				if( reqType.isBoolean() )
					replaceWithNode(new ConstBoolExpr(false));
				else
					replaceWithNode(new ConstNullExpr());
				return;
			}
			throw new CompilerException(this,"Unresolved identifier "+name);
		}
		if( v instanceof Struct ) {
			Struct s = (Struct)v;
			s.checkResolved();
			if( reqType != null && reqType.equals(Type.tpInt) ) {
				if( s.isPizzaCase() ) {
//					PizzaCaseAttr case_attr = (PizzaCaseAttr)s.getAttr(attrPizzaCase);
//					if( case_attr != null ) {
//						replaceWithNodeResolve(reqType, new ConstIntExpr(case_attr.caseno));
//						return;
//					}
					MetaPizzaCase meta = s.getMetaPizzaCase();
					if( meta != null ) {
						replaceWithNodeResolve(reqType, new ConstIntExpr(meta.getTag()));
						return;
					}
				}
			}
			replaceWithNode(new TypeNameRef(new SymbolRef<TypeDecl>(pos,name),s.xtype));
			return;
		}
		else if( v instanceof TypeDecl ) {
			replaceWithNode(new TypeRef(((TypeDecl)v).getType()));
			return;
		}
		replaceWithNodeResolve(reqType, info.buildAccess(this, null, v).closeBuild());
	}

	public String toString() {
		return name;
	}

	public Object doRewrite(RewriteContext ctx) {
		return ctx.root.getVal(name);
	}
}

