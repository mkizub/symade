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
package kiev.be.java15;
import syntax kiev.Syntax;

import static kiev.be.java15.Instr.*;

public final class JConstBoolExpr extends JConstExpr implements IBoolExpr {

	@virtual typedef VT  ≤ ConstBoolExpr;

	public final boolean value;

	public static JConstBoolExpr attach(ConstBoolExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JConstBoolExpr)jn;
		return new JConstBoolExpr(impl);
	}
	
	protected JConstBoolExpr(ConstBoolExpr impl) {
		super(impl);
		this.value = impl.value;
	}
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: "+this);
		code.setLinePos(this);
		if( reqType ≢ code.tenv.tpVoid ) {
			if( value )
				code.addConst(1);
			else
				code.addConst(0);
		}
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_true "+this);
		code.setLinePos(this);
		if( value ) code.addInstr(op_goto,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_false "+this);
		code.setLinePos(this);
		if( !value ) code.addInstr(op_goto,label);
	}

}

public class JConstExpr extends JENode {

	@virtual typedef VT  ≤ ConstExpr;

	public static JConstExpr attach(ConstExpr impl)
		operator "new T"
		operator "( T ) V"
	{
		if (impl == null)
			return null;
		JNode jn = getJData(impl);
		if (jn != null)
			return (JConstExpr)jn;
		if (impl instanceof ConstBoolExpr)
			return JConstBoolExpr.attach((ConstBoolExpr)impl);
		return new JConstExpr(impl);
	}
	
	protected JConstExpr(ConstExpr impl) {
		super(impl);
	}
	
	public void generate(Code code, Type reqType) {
		JConstExpr.generateConst(this, code, reqType);
	}

	public static void generateConst(JConstExpr self, Code code, Type reqType) {
		Object value;
		if (self == null) {
			trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating dummy value");
			value = null;
		} else {
			value = self.vn().getConstValue(code.env);
			trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
			code.setLinePos(self);
		}
		if( value == null ) {
			// Special case for generation of parametriezed
			// with primitive types classes
			if( reqType != null && !reqType.isReference() ) {
				switch(code.jtenv.getJType(reqType).java_signature.charAt(0)) {
				case 'Z': case 'B': case 'S': case 'I': case 'C':
					code.addConst(0);
					break;
				case 'J':
					code.addConst(0L);
					break;
				case 'F':
					code.addConst(0.F);
					break;
				case 'D':
					code.addConst(0.D);
					break;
				default:
					code.addNullConst();
					break;
				}
			}
			else
				code.addNullConst();
		}
		else if( value instanceof Byte ) {
			code.addConst(((Byte)value).intValue());
		}
		else if( value instanceof Short ) {
			code.addConst(((Short)value).intValue());
		}
		else if( value instanceof Integer ) {
			code.addConst(((Integer)value).intValue());
		}
		else if( value instanceof Character ) {
			code.addConst((int)((Character)value).charValue());
		}
		else if( value instanceof Long ) {
			code.addConst(((Long)value).longValue());
		}
		else if( value instanceof Float ) {
			code.addConst(((Float)value).floatValue());
		}
		else if( value instanceof Double ) {
			code.addConst(((Double)value).doubleValue());
		}
		else if( value instanceof String ) {
			code.addConst((String)value);
		}
		else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
		if( reqType ≡ code.tenv.tpVoid ) code.addInstr(op_pop);
	}

}

