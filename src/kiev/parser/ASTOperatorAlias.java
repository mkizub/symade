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

@ThisIsANode(lang=CoreLang)
public final class ASTOperatorAlias extends Symbol {

	@nodeAttr public int					prior;
	@nodeAttr public OpdefMode				opmode;
	@nodeAttr public String					image;

	public ASTOperatorAlias() {
		super("operator ???");
	}
	public ASTOperatorAlias(String opname) {
		super(opname);
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
			m.setOperatorMethod(true);
			return;
		}
		
		switch(opmode) {
		case OpdefMode.LFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals("[]") ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.mtype.arity != 2 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 2 arguments");
					if( m.mtype.ret() ≉ m.mtype.arg(1) )
						throw new CompilerException(this,"Method "+m+" must return "+m.mtype.arg(1));
					setAliasName(nameArraySetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}
				if( image.equals("new") ) {
					if( !m.isStatic() )
						throw new CompilerException(this,"'new' operator must be static");
					if( m.mtype.ret() ≉ m.ctx_tdecl.xtype )
						throw new CompilerException(this,"Method "+m+" must return "+m.ctx_tdecl.xtype);
					setAliasName(nameNewOp);
					if( Kiev.verbose ) System.out.println("Attached operator new to method "+m);
					return;
				}

				Type opret = m.mtype.ret();
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
		case OpdefMode.XFX:
		case OpdefMode.YFX:
		case OpdefMode.XFY:
		case OpdefMode.YFY:
			{
				// Special case fo "[]" and "new" operators
				if( image.equals("[]") ) {
					if( m.isStatic() )
						throw new CompilerException(this,"'[]' operator can't be static");
					if( m.mtype.arity != 1 )
						throw new CompilerException(this,"Method "+m+" must be virtual and have 1 argument");
					if( m.mtype.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					setAliasName(nameArrayGetOp);
					if( Kiev.verbose ) System.out.println("Attached operator [] to method "+m);
					return;
				}

				Type opret = m.mtype.ret();
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
		case OpdefMode.FX:
		case OpdefMode.FY:
			{
				// Special case fo "$cast" operator
				if( image.equals("$cast") ) {
					if( m.isStatic() && m.mtype.arity != 1 )
						throw new CompilerException(this,"Static cast method "+m+" must have 1 argument");
					else if( !m.isStatic() && m.mtype.arity != 0 )
						throw new CompilerException(this,"Virtual scast method "+m+" must have no arguments");
					if( m.mtype.ret() ≡ Type.tpVoid )
						throw new CompilerException(this,"Method "+m+" must not return void");
					setAliasName(nameCastOp);
					return;
				}

				Type opret = m.mtype.ret();
				Operator op = Operator.getOperatorByName(image+" V");
				if (op == null)
					throw new CompilerException(this,"Prefix operator "+image+" not found");
				setAliasName(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached prefix "+op+" to method "+m);
			}
			break;
		case OpdefMode.XF:
		case OpdefMode.YF:
			{
				Type opret = m.mtype.ret();
				Operator op = Operator.getOperatorByName("V "+image);
				if (op == null)
					throw new CompilerException(this,"Postfix operator "+image+" not found");
				setAliasName(op.name);
				op.addMethod(m);
				if( Kiev.verbose ) System.out.println("Attached postfix "+op+" to method "+m);
			}
			break;
		case OpdefMode.XFXFY:
			throw new CompilerException(this,"Multioperators are not supported yet");
		default:
			throw new CompilerException(this,"Unknown operator mode "+opmode);
		}
		m.setOperatorMethod(true);
	}

	public String toString() {
		if (image != null)
			return image.toString();
		return sname;
	}
}
