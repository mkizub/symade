/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.parser;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
@unerasable
public final class ASTOperatorAlias extends Symbol<Method> {

	@virtual typedef This  = ASTOperatorAlias;

	@att public int					prior;
	@att public int					opmode;
	@att public String				image;

	public ASTOperatorAlias() {
		super("operator ???");
	}
	public ASTOperatorAlias(String opname) {
		super(opname);
	}
	
	public void setImage(ASTNode n) {
		this.pos = n.pos;
		if( n instanceof ASTOperator ) {
			image = ((ASTOperator)n).ident;
			return;
		}
		else if( n instanceof ASTIdentifier ) {
			image = ((ASTIdentifier)n).ident;
			return;
		}
		throw new CompilerException(n,"Bad operator definition");
	}
	
	public void setMode(String optype) {
		opmode = -1;
		for(int i=0; i < Opdef.orderAndArityNames.length; i++) {
			if( Opdef.orderAndArityNames[i].equals(optype) ) {
				opmode = i;
				break;
			}
		}
		if( opmode < 0 )
			throw new CompilerException(this,"Operator mode must be one of "+Arrays.toString(Opdef.orderAndArityNames));
		return;
	}
	
	public void setPriority(ConstIntExpr n) {
		prior = n.value;
		if( prior < 0 || prior > 255 )
			throw new CompilerException(n,"Operator priority must have value from 0 to 255");
		pos = n.pos;
		return;
	}

	public void setName(ConstStringExpr n) {
		this.pos = n.pos;
		this.sname = n.value;
	}
	
    private void checkPublicAccess(Method m) {
    	if( !m.isStatic() ) return;
    	if( m.isPrivate() || m.isProtected() ) return;
    	TypeDecl pkg = m.ctx_tdecl;
    	while( pkg != null && !pkg.isPackage() ) pkg = pkg.package_clazz.dnode;
    	if( pkg == null || pkg instanceof Env ) return;
    	foreach(ASTNode n; pkg.members; n == m ) return;
    }
	
	private void setAliasName(String s) {
		if (sname != s)
			sname = s;
	}

	public void pass3() {
		Method m = (Method)parent();

		if (sname != null && sname != "" && sname != "operator ???") {
			if (sname == "new T")
				return;
			Operator op = Operator.getOperatorByName(sname);
			if (op == null) {
				//throw new CompilerException(this,"Operator "+sname+" not found");
				Kiev.reportWarning(this,"Operator "+sname+" not found");
				return;
			}
			op.addMethod(m);
			return;
		}
		
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
					setAliasName(nameArraySetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}
				if( image.equals("new") ) {
					if( !m.isStatic() )
						throw new CompilerException(this,"'new' operator must be static");
					if( m.type.ret() ≉ m.ctx_tdecl.xtype )
						throw new CompilerException(this,"Method "+m+" must return "+m.ctx_tdecl.xtype);
					setAliasName(nameNewOp);
					if( Kiev.verbose ) System.out.println("Attached operator new to method "+m);
					return;
				}

				Type opret = m.type.ret();
				if( prior == 0 )
					prior = Constants.opAssignPriority;
				if( prior != Constants.opAssignPriority )
					throw new CompilerException(this,"Assign operator must have priority "+Constants.opAssignPriority);
				Operator op = Operator.getOperatorByName("V "+image+" V");
				if (op == null)
					throw new CompilerException(this,"Assign operator "+image+" not found");
				setAliasName(op.name);
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
					setAliasName(nameArrayGetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}

				Type opret = m.type.ret();
				Operator op = Operator.getOperatorByName("V "+image+" V");
				if (op == null)
					op = Operator.getOperatorByName("V "+image+" T");
				if (op == null)
					throw new CompilerException(this,"Binary operator "+image+" not found");
				setAliasName(op.name);
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
					setAliasName(nameCastOp);
					return;
				}

				Type opret = m.type.ret();
				Operator op = Operator.getOperatorByName(image+" V");
				if (op == null)
					throw new CompilerException(this,"Prefix operator "+image+" not found");
				setAliasName(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached prefix "+op+" to method "+m);
			}
			break;
		case Opdef.XF:
		case Opdef.YF:
			{
				Type opret = m.type.ret();
				Operator op = Operator.getOperatorByName("V "+image);
				if (op == null)
					throw new CompilerException(this,"Postfix operator "+image+" not found");
				setAliasName(op.name);
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
}
