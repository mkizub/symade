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

@nodeset
public class ASTIdentifier extends ENode {

	@dflow(out="this:in") private static class DFI {}

	private static KString op_instanceof = KString.from("instanceof");

	@virtual typedef This  = ASTIdentifier;
	@virtual typedef NImpl = ASTIdentifierImpl;
	@virtual typedef VView = ASTIdentifierView;

	@nodeimpl
	public static class ASTIdentifierImpl extends ENodeImpl {
		@att public KString name;
	}
	@nodeview
	public static view ASTIdentifierView of ASTIdentifierImpl extends ENodeView {
		public KString name;

		public int		getPriority() { return 256; }

		public boolean preResolveIn() {
			// predefined operators
			if( name == op_instanceof ) {
				ASTOperator op = new ASTOperator();
				op.pos = this.pos;
				op.image = op_instanceof;
				this.replaceWithNode(op);
				return false;
			}
			// predefined names
			if( name == Constants.nameFILE ) {
				ConstExpr ce = new ConstStringExpr(Kiev.curFile);
				//ce.text_name = this.name;
				replaceWithNode(ce);
				return false;
			}
			else if( name == Constants.nameLINENO ) {
				ConstExpr ce = new ConstIntExpr(pos>>>11);
				//ce.text_name = this.name;
				replaceWithNode(ce);
				return false;
			}
			else if( name == Constants.nameMETHOD ) {
				ConstExpr ce;
				if( ctx_method != null )
					ce = new ConstStringExpr(ctx_method.name.name);
				else
					ce = new ConstStringExpr(nameInit);
				//ce.text_name = this.name;
				replaceWithNode(ce);
				return false;
			}
			else if( name == Constants.nameDEBUG ) {
				ConstExpr ce = new ConstBoolExpr(Kiev.debugOutputA);
				//ce.text_name = this.name;
				replaceWithNode(ce);
				return false;
			}
			else if( name == Constants.nameReturnVar ) {
				Kiev.reportWarning(this,"Keyword '$return' is deprecated. Replace with 'Result', please");
				name = Constants.nameResultVar;
			}
			else if( name == Constants.nameThis ) {
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
			DNode@ v;
			ResInfo info = new ResInfo(this);
			if( !PassInfo.resolveNameR(this.getNode(),v,info,name) )
				throw new CompilerException(this,"Unresolved identifier "+name);
			if( v instanceof TypeDecl ) {
				TypeDecl td = (TypeDecl)v;
				td.checkResolved();
				replaceWithNode(new TypeRef(td.getType()));
			}
			else {
				replaceWithNode(info.buildAccess(this.getNode(), null, v));
			}
			return false;
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTIdentifier() {
		super(new ASTIdentifierImpl());
	}

	public ASTIdentifier(KString name) {
		super(new ASTIdentifierImpl());
		this.name = name;
	}

	public ASTIdentifier(int pos, KString name) {
		super(new ASTIdentifierImpl());
		this.pos = pos;
		this.name = name;
	}

	public void set(Token t) {
        pos = t.getPos();
		if (t.image.startsWith("#id\""))
			this.name = ConstExpr.source2ascii(t.image.substring(4,t.image.length()-2));
		else
			this.name = KString.from(t.image);
	}
	
	public Type getType() {
		return Type.tpVoid;
	}
	
	public boolean preGenerate() { /*Kiev.reportError(this,"preGenerate of ASTIdentifier");*/ return false; }
	
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
				replaceWithNode(new ConstStringExpr(ctx_method.name.name));
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
						replaceWithNode(new ConstStringExpr(KString.from(val)));
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
					replaceWithNode(new ConstStringExpr(KString.from(val)));
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
			replaceWithNode(new TypeNameRef(new NameRef(pos,name),s.ctype));
			return;
		}
		else if( v instanceof TypeDecl ) {
			replaceWithNode(new TypeRef(((TypeDecl)v).getType()));
			return;
		}
		replaceWithNodeResolve(reqType, info.buildAccess(this, null, v));
	}

	public KString toKString() {
		return name;
	}
    
	public String toString() {
		return name.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}
}

