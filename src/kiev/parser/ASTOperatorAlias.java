/* Generated By:JJTree: Do not edit this line. ASTOperatorAlias.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.OpTypes.*;
import static kiev.vlang.Operator.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public final class ASTOperatorAlias extends ASTAlias {
	public static final int	XFIX_UNKNOWN = 0;
	public static final int	XFIX_PREFIX  = 1;
	public static final int	XFIX_POSTFIX = 2;
	public static final int	XFIX_INFIX   = 3;

	@virtual typedef This  = ASTOperatorAlias;
	@virtual typedef NImpl = ASTOperatorAliasImpl;
	@virtual typedef VView = ASTOperatorAliasView;

	@nodeimpl
	public static class ASTOperatorAliasImpl extends ASTAliasImpl {
		@virtual typedef ImplOf = ASTOperatorAlias;
		@att public int					prior;
		@att public int					opmode;
		@att public KString				image;
		@att public int					xfix;
	}
	@nodeview
	public static view ASTOperatorAliasView of ASTOperatorAliasImpl extends ASTAliasView {
		public int					prior;
		public int					opmode;
		public KString				image;
		public int					xfix;
	}

	public VView getVView() alias operator(210,fy,$cast) { return (VView)this.$v_impl; }
	public JView getJView() alias operator(210,fy,$cast) { return (JView)this.$v_impl; }
	
	public ASTOperatorAlias() { super(new ASTOperatorAliasImpl()); }
	
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
	
	public void setMode(NameRef n) {
		opmode = -1;
		KString optype = n.name;
		for(int i=0; i < Operator.orderAndArityNames.length; i++) {
			if( Operator.orderAndArityNames[i].equals(optype) ) {
				opmode = i;
				break;
			}
		}
		if( opmode < 0 )
			throw new CompilerException(n,"Operator mode must be one of "+Arrays.toString(Operator.orderAndArityNames));
		return;
	}
	
	public void setPriority(ConstIntExpr n) {
		prior = n.value;
		if( prior < 0 || prior > 255 )
			throw new CompilerException(n,"Operator priority must have value from 0 to 255");
		pos = n.pos;
		return;
	}

  	public void set(Token t) {
  		if (t.image.equals("prefix"))		xfix = XFIX_PREFIX;
  		else if (t.image.equals("suffix"))	xfix = XFIX_POSTFIX;
  		else if (t.image.equals("postfix"))	xfix = XFIX_POSTFIX;
  		else if (t.image.equals("infix"))	xfix = XFIX_INFIX;
  		else if (t.image.equals("binary"))	xfix = XFIX_INFIX;
    	else
    		throw new RuntimeException("Bad xfix mode of operator declaration "+t);
	}

    private void checkPublicAccess(Method m) {
    	if( !m.isStatic() ) return;
    	if( m.isPrivate() || m.isProtected() ) return;
    	Struct pkg = (Struct)m.parent;
    	while( pkg != null && !pkg.isPackage() ) pkg = pkg.package_clazz;
    	if( pkg == null || pkg == Env.root ) return;
    	foreach(ASTNode n; pkg.imported; n == m ) return;
    }

	public void attach(ASTNode n) {
		if( !(n instanceof Method) )
			throw new CompilerException(this,"Node of type "+n.getClass()+" cannot be aliased with operator");
		Method m = (Method)n;
		iopt = null;

		if (xfix != XFIX_UNKNOWN) {
			Operator op = null;
			switch (xfix) {
			case XFIX_INFIX:
				op = BinaryOperator.getOperator(image);
				if (op == null)
					throw new CompilerException(this,"Infix operator "+image+" not known");
				opmode = op.mode;
				prior = op.priority;
				break;
			case XFIX_PREFIX:
				op = PrefixOperator.getOperator(image);
				if (op == null)
					throw new CompilerException(this,"Prefix operator "+image+" not known");
				opmode = op.mode;
				prior = op.priority;
				break;
			case XFIX_POSTFIX:
				op = PostfixOperator.getOperator(image);
				if (op == null)
					throw new CompilerException(this,"Postfix operator "+image+" not known");
				opmode = op.mode;
				prior = op.priority;
				break;
			default:
				throw new CompilerException(this,"Internal error: xfix "+xfix+" unknown");
			}
		}

		switch(opmode) {
		case Operator.LFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals(nameArrayOp) ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.type.arity != 2 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 2 arguments");
					if( m.type.ret() ≉ m.type.arg(1) )
						throw new CompilerException(this,"Method "+m+" must return "+m.type.arg(1));
					m.name.addAlias(nameArrayOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}
				if( image.equals(nameNewOp) ) {
					if( !m.isStatic() )
						throw new CompilerException(this,"'new' operator must be static");
					if( m.type.ret() ≉ m.ctx_clazz.ctype )
						throw new CompilerException(this,"Method "+m+" must return "+m.ctx_clazz.ctype);
					m.name.addAlias(nameNewOp);
					if( Kiev.verbose ) System.out.println("Attached operator new to method "+m);
					return;
				}

				Type opret = m.type.ret();
				Type oparg1, oparg2;
				if( prior != Constants.opAssignPriority )
					throw new CompilerException(this,"Assign operator must have priority "+Constants.opAssignPriority);
				if( m.isStatic() )
					throw new CompilerException(this,"Assign operator can't be static");
				else if( !m.isStatic() && m.type.arity == 1 )
					{ oparg1 = m.ctx_clazz.ctype; oparg2 = m.type.arg(0); }
				else
					throw new CompilerException(this,"Method "+m+" must be virtual and have 1 argument");
				AssignOperator op = AssignOperator.newAssignOperator(
					image,m.name.name,null,false
					);
				iopt=new OpTypes();
				op.addTypes(otTheType(opret),otTheType(oparg1),otType(oparg2));
				if( Kiev.verbose ) System.out.println("Attached assign "+op+" to method "+m);
			}
			break;
		case Operator.XFX:
		case Operator.YFX:
		case Operator.XFY:
		case Operator.YFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals(nameArrayOp) ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.type.arity != 1 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 1 argument");
					if( m.type.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					m.name.addAlias(nameArrayOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}

				Type opret = m.type.ret();
				Type oparg1, oparg2;
				if( m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 2 )
					{ oparg1 = m.type.arg(0); oparg2 = m.type.arg(1); }
				else if( m.isStatic() && m instanceof RuleMethod && m.type.arity == 3 )
					{ oparg1 = m.type.arg(1); oparg2 = m.type.arg(2); }
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 1 )
					{ oparg1 = m.ctx_clazz.ctype; oparg2 = m.type.arg(0); }
				else if( !m.isStatic() && m instanceof RuleMethod && m.type.arity == 2 )
					{ oparg1 = m.ctx_clazz.ctype; oparg2 = m.type.arg(1); }
				else
					throw new CompilerException(this,"Method "+m+" must have 2 arguments");
				BinaryOperator op = BinaryOperator.newBinaryOperator(
					prior,image,m.name.name,null,Operator.orderAndArityNames[opmode],false
					);
				iopt=new OpTypes();
				op.addTypes(otType(opret),otType(oparg1),otType(oparg2));
				if( Kiev.verbose ) System.out.println("Attached binary "+op+" to method "+m);
			}
			break;
		case Operator.FX:
		case Operator.FY:
			{
				// Special case fo "$cast" operator
				if( image.equals(nameCastOp) ) {
					if( m.isStatic() && m.type.arity != 1 )
						throw new CompilerException(this,"Static cast method "+m+" must have 1 argument");
					else if( !m.isStatic() && m.type.arity != 0 )
						throw new CompilerException(this,"Virtual scast method "+m+" must have no arguments");
					if( m.type.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					m.name.addAlias(nameCastOp);
					return;
				}

				Type opret = m.type.ret();
				Type oparg;
				if( m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 1 )
					oparg = m.type.arg(0);
				else if( m.isStatic() && m instanceof RuleMethod && m.type.arity == 2 )
					oparg = m.type.arg(1);
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 0 )
					oparg = m.ctx_clazz.ctype;
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 1 )
					oparg = m.type.arg(0);
				else if( !m.isStatic() && m instanceof RuleMethod && m.type.arity == 1 )
					oparg = m.ctx_clazz.ctype;
				else {
					if (m.isStatic())
						throw new CompilerException(this,"Static method "+m+" must have 1 argument");
					else
						throw new CompilerException(this,"Non-static method "+m+" must have 0 or 1 argument");
				}
				PrefixOperator op = PrefixOperator.newPrefixOperator(
					prior,image,m.name.name,null,Operator.orderAndArityNames[opmode],false
					);
				iopt=new OpTypes();
				op.addTypes(otType(opret),otType(oparg));
				if( Kiev.verbose ) System.out.println("Attached prefix "+op+" to method "+m);
			}
			break;
		case Operator.XF:
		case Operator.YF:
			{
				Type opret = m.type.ret();
				Type oparg;
				if( m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 1 )
					oparg = m.type.arg(0);
				else if( m.isStatic() && m instanceof RuleMethod && m.type.arity == 2 )
					oparg = m.type.arg(1);
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.arity == 0 )
					oparg = m.ctx_clazz.ctype;
				else if( !m.isStatic() && m instanceof RuleMethod && m.type.arity == 1 )
					oparg = m.ctx_clazz.ctype;
				else
					throw new CompilerException(this,"Method "+m+" must have 1 argument");
				PostfixOperator op = PostfixOperator.newPostfixOperator(
					prior,image,m.name.name,null,Operator.orderAndArityNames[opmode],false
					);
				iopt=new OpTypes();
				op.addTypes(otType(opret),otType(oparg));
				if( Kiev.verbose ) System.out.println("Attached postfix "+op+" to method "+m);
			}
			break;
		case Operator.XFXFY:
			throw new CompilerException(this,"Multioperators are not supported yet");
		default:
			throw new CompilerException(this,"Unknown operator mode "+opmode);
		}
		checkPublicAccess(m);
		iopt.method = m;
		m.setOperatorMethod(true);
	}

	public String toString() {
		return image.toString();
	}

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append("/* alias operator(")
			.append(Integer.toString(prior)).append(",")
			.append(Operator.orderAndArityNames[opmode]).append(",")
			.append(image).append(") */").space();
	}

}
