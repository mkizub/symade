/* Generated By:JJTree: Do not edit this line. ASTOperatorAlias.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Operator.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class ASTOperatorAlias extends ASTAlias {

	@virtual typedef This  = ASTOperatorAlias;
	@virtual typedef VView = ASTOperatorAliasView;

	@att public int					prior;
	@att public int					opmode;
	@att public String				image;

	@nodeview
	public static view ASTOperatorAliasView of ASTOperatorAlias extends ASTAliasView {
		public int					prior;
		public int					opmode;
		public String				image;
	}

	public ASTOperatorAlias() {}
	
	public void setImage(ASTNode n) {
		this.pos = n.pos;
		if( n instanceof ASTOperator ) {
			image = ((ASTOperator)n).image;
			return;
		}
		else if( n instanceof ASTIdentifier ) {
			image = ((ASTIdentifier)n).name;
			return;
		}
		throw new CompilerException(n,"Bad operator definition");
	}
	
	public void setMode(SymbolRef n) {
		opmode = -1;
		String optype = n.name;
		for(int i=0; i < Opdef.orderAndArityNames.length; i++) {
			if( Opdef.orderAndArityNames[i].equals(optype) ) {
				opmode = i;
				break;
			}
		}
		if( opmode < 0 )
			throw new CompilerException(n,"Operator mode must be one of "+Arrays.toString(Opdef.orderAndArityNames));
		return;
	}
	
	public void setPriority(ConstIntExpr n) {
		prior = n.value;
		if( prior < 0 || prior > 255 )
			throw new CompilerException(n,"Operator priority must have value from 0 to 255");
		pos = n.pos;
		return;
	}

    private void checkPublicAccess(Method m) {
    	if( !m.isStatic() ) return;
    	if( m.isPrivate() || m.isProtected() ) return;
    	TypeDecl pkg = m.ctx_tdecl;
    	while( pkg != null && !pkg.isPackage() ) pkg = pkg.package_clazz;
    	if( pkg == null || pkg == Env.root ) return;
    	foreach(ASTNode n; pkg.members; n == m ) return;
    }

	public void attach(ASTNode n) {
		if( !(n instanceof Method) )
			throw new CompilerException(this,"Node of type "+n.getClass()+" cannot be aliased with operator");
		Method m = (Method)n;

		switch(opmode) {
		case Opdef.LFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals("[]") ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.type.arity != 2 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 2 arguments");
					if( m.type.ret() ≉ m.type.arg(1) )
						throw new CompilerException(this,"Method "+m+" must return "+m.type.arg(1));
					m.id.addAlias(nameArraySetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}
				if( image.equals("new") ) {
					if( !m.isStatic() )
						throw new CompilerException(this,"'new' operator must be static");
					if( m.type.ret() ≉ m.ctx_tdecl.xtype )
						throw new CompilerException(this,"Method "+m+" must return "+m.ctx_tdecl.xtype);
					m.id.addAlias(nameNewOp);
					if( Kiev.verbose ) System.out.println("Attached operator new to method "+m);
					return;
				}

				Type opret = m.type.ret();
				if( prior == 0 )
					prior = Constants.opAssignPriority;
				if( prior != Constants.opAssignPriority )
					throw new CompilerException(this,"Assign operator must have priority "+Constants.opAssignPriority);
				Operator op = Operator.getOperator("V "+image+" V");
				if (op == null)
					throw new CompilerException(this,"Assign operator "+image+" not found");
				m.id.addAlias(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached assign "+op+" to method "+m);
			}
			break;
		case Opdef.XFX:
		case Opdef.YFX:
		case Opdef.XFY:
		case Opdef.YFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals("[]") ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.type.arity != 1 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 1 argument");
					if( m.type.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					m.id.addAlias(nameArrayGetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}

				Type opret = m.type.ret();
				Operator op = Operator.getOperator("V "+image+" V");
				if (op == null)
					op = Operator.getOperator("V "+image+" T");
				if (op == null)
					throw new CompilerException(this,"Binary operator "+image+" not found");
				m.id.addAlias(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached binary "+op+" to method "+m);
			}
			break;
		case Opdef.FX:
		case Opdef.FY:
			{
				// Special case fo "$cast" operator
				if( image.equals("$cast") ) {
					if( m.isStatic() && m.type.arity != 1 )
						throw new CompilerException(this,"Static cast method "+m+" must have 1 argument");
					else if( !m.isStatic() && m.type.arity != 0 )
						throw new CompilerException(this,"Virtual scast method "+m+" must have no arguments");
					if( m.type.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					m.id.addAlias(nameCastOp);
					return;
				}

				Type opret = m.type.ret();
				Operator op = Operator.getOperator(image+" V");
				if (op == null)
					throw new CompilerException(this,"Prefix operator "+image+" not found");
				m.id.addAlias(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached prefix "+op+" to method "+m);
			}
			break;
		case Opdef.XF:
		case Opdef.YF:
			{
				Type opret = m.type.ret();
				Operator op = Operator.getOperator("V "+image);
				if (op == null)
					throw new CompilerException(this,"Postfix operator "+image+" not found");
				m.id.addAlias(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached postfix "+op+" to method "+m);
			}
			break;
		case Opdef.XFXFY:
			throw new CompilerException(this,"Multioperators are not supported yet");
		default:
			throw new CompilerException(this,"Unknown operator mode "+opmode);
		}
		checkPublicAccess(m);
		m.setOperatorMethod(true);
	}

	public String toString() {
		return image.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append("/* alias operator(")
			.append(Integer.toString(prior)).append(",")
			.append(Opdef.orderAndArityNames[opmode]).append(",")
			.append(image).append(") */").space();
	}

}
