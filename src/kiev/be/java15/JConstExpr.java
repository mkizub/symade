package kiev.be.java15;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.be.java15.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public final view JConstBoolExpr of ConstBoolExpr extends JConstExpr implements IBoolExpr {
	public:ro boolean	value;

	public Object	getConstValue()		{ return value ? Boolean.TRUE: Boolean.FALSE; }
	
	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: "+this);
		code.setLinePos(this);
		if( reqType ≢ Type.tpVoid ) {
			if( value )
				code.addConst(1);
			else
				code.addConst(0);
		}
	}

	public void generate_iftrue(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_true "+this);
		code.setLinePos(this);
		if( value ) code.addInstr(op_goto,label);
	}

	public void generate_iffalse(Code code, CodeLabel label) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: if_false "+this);
		code.setLinePos(this);
		if( !value ) code.addInstr(op_goto,label);
	}

}

@nodeview
public final view JConstNullExpr of ConstNullExpr extends JConstExpr {
	public Object	getConstValue()		{ return null; }
}

@nodeview
public final view JConstByteExpr of ConstByteExpr extends JConstExpr {
	public:ro byte		value;
	public Object	getConstValue()		{ return Byte.valueOf(value); }
}

@nodeview
public final view JConstShortExpr of ConstShortExpr extends JConstExpr {
	public:ro short		value;
	public Object	getConstValue()		{ return Short.valueOf(value); }
}

@nodeview
public final view JConstIntExpr of ConstIntExpr extends JConstExpr {
	public:ro int		value;
	public Object	getConstValue()		{ return Integer.valueOf(value); }
}

@nodeview
public final view JConstLongExpr of ConstLongExpr extends JConstExpr {
	public:ro long		value;
	public Object	getConstValue()		{ return Long.valueOf(value); }
}
	
@nodeview
public final view JConstCharExpr of ConstCharExpr extends JConstExpr {
	public:ro char		value;
	public Object	getConstValue()		{ return Character.valueOf(value); }
}

@nodeview
public final view JConstFloatExpr of ConstFloatExpr extends JConstExpr {
	public:ro float		value;
	public Object	getConstValue()		{ return Float.valueOf(value); }
}

@nodeview
public final view JConstDoubleExpr of ConstDoubleExpr extends JConstExpr {
	public:ro double		value;
	public Object	getConstValue()		{ return Double.valueOf(value); }
}

@nodeview
public final view JConstStringExpr of ConstStringExpr extends JConstExpr {
	public:ro String	value;
	public Object	getConstValue()		{ return value; }
}

@nodeview
public abstract view JConstExpr of ConstExpr extends JENode {

	public boolean isConstantExpr() { return true; }
	public abstract Object getConstValue();
	
	public void generate(Code code, Type reqType) {
		Object value = getConstValue();
		trace(Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
		code.setLinePos(this);
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

