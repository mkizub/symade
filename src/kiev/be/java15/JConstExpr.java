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

import static kiev.be.java15.Instr.*;

import syntax kiev.Syntax;

public final view JConstBoolExpr of ConstBoolExpr extends JConstExpr implements IBoolExpr {
	public:ro boolean	value;

	public void generate(Code code, Type reqType) {
		trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: "+this);
		code.setLinePos(this);
		if( reqType ≢ Type.tpVoid ) {
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

public final view JConstNullExpr of ConstNullExpr extends JConstExpr {
}

public final view JConstByteExpr of ConstByteExpr extends JConstExpr {
	public:ro byte		value;
}

public final view JConstShortExpr of ConstShortExpr extends JConstExpr {
	public:ro short		value;
}

public final view JConstIntExpr of ConstIntExpr extends JConstExpr {
	public:ro int		value;
}

public final view JConstLongExpr of ConstLongExpr extends JConstExpr {
	public:ro long		value;
}
	
public final view JConstCharExpr of ConstCharExpr extends JConstExpr {
	public:ro char		value;
}

public final view JConstFloatExpr of ConstFloatExpr extends JConstExpr {
	public:ro float		value;
}

public final view JConstDoubleExpr of ConstDoubleExpr extends JConstExpr {
	public:ro double		value;
}

public final view JConstStringExpr of ConstStringExpr extends JConstExpr {
	public:ro String	value;
}

public abstract view JConstExpr of ConstExpr extends JENode {

	public void generate(Code code, Type reqType) {
		JConstExpr.generateConst(this, code, reqType);
	}

	public static void generateConst(JConstExpr self, Code code, Type reqType) {
		Object value;
		if (self == null) {
			trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating dummy value");
			value = null;
		} else {
			value = self.getConstValue();
			trace(Kiev.debug && Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
			code.setLinePos(self);
		}
		if( value == null ) {
			// Special case for generation of parametriezed
			// with primitive types classes
			if( reqType != null && !reqType.isReference() ) {
				switch(reqType.getJType().java_signature.byteAt(0)) {
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
			code.addConst(KString.from((String)value));
		}
		else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
		if( reqType ≡ Type.tpVoid ) code.addInstr(op_pop);
	}

}

