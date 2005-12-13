package kiev.be.java;

import kiev.Kiev;
import kiev.CError;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;
import kiev.parser.*;

import static kiev.vlang.Instr.*;
import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

@nodeview
public class JConstBoolExprView extends JConstExprView implements IBoolExpr {
	final ConstBoolExpr.ConstBoolExprImpl impl;
	public JConstBoolExprView(ConstBoolExpr.ConstBoolExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final boolean	get$value()				{ return this.impl.value; }

	public void generate(Code code, Type reqType) {
		trace(Kiev.debugStatGen,"\t\tgenerating ConstBoolExpr: "+this);
		code.setLinePos(this);
		if( reqType != Type.tpVoid ) {
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
public class JConstNullExprView extends JConstExprView {
	public JConstNullExprView(ConstNullExpr.ConstNullExprImpl impl) {
		super(impl);
	}
}

@nodeview
public class JConstByteExprView extends JConstExprView {
	final ConstByteExpr.ConstByteExprImpl impl;
	public JConstByteExprView(ConstByteExpr.ConstByteExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final byte		get$value()			{ return this.impl.value; }
}

@nodeview
public class JConstShortExprView extends JConstExprView {
	final ConstShortExpr.ConstShortExprImpl impl;
	public JConstShortExprView(ConstShortExpr.ConstShortExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final short		get$value()				{ return this.impl.value; }
}

@nodeview
public class JConstIntExprView extends JConstExprView {
	final ConstIntExpr.ConstIntExprImpl impl;
	public JConstIntExprView(ConstIntExpr.ConstIntExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final int		get$value()			{ return this.impl.value; }
}

@nodeview
public class JConstLongExprView extends JConstExprView {
	final ConstLongExpr.ConstLongExprImpl impl;
	public JConstLongExprView(ConstLongExpr.ConstLongExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final long		get$value()			{ return this.impl.value; }
}
	
@nodeview
public class JConstCharExprView extends JConstExprView {
	final ConstCharExpr.ConstCharExprImpl impl;
	public JConstCharExprView(ConstCharExpr.ConstCharExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final char		get$value()			{ return this.impl.value; }
}

@nodeview
public class JConstFloatExprView extends JConstExprView {
	final ConstFloatExpr.ConstFloatExprImpl impl;
	public JConstFloatExprView(ConstFloatExpr.ConstFloatExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final float		get$value()				{ return this.impl.value; }
}

@nodeview
public class JConstDoubleExprView extends JConstExprView {
	final ConstDoubleExpr.ConstDoubleExprImpl impl;
	public JConstDoubleExprView(ConstDoubleExpr.ConstDoubleExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final double		get$value()				{ return this.impl.value; }
}

@nodeview
public class JConstStringExprView extends JConstExprView {
	final ConstStringExpr.ConstStringExprImpl impl;
	public JConstStringExprView(ConstStringExpr.ConstStringExprImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final KString	get$value()				{ return this.impl.value; }
}

@nodeview
public abstract class JConstExprView extends JENodeView {
	public JConstExprView(ConstExpr.ConstExprImpl impl) {
		super(impl);
	}

	public Object getConstValue() { return this.getENode().getConstValue(); }
	
	public void generate(Code code, Type reqType) {
		Object value = getConstValue();
		trace(Kiev.debugStatGen,"\t\tgenerating ConstExpr: "+value);
		code.setLinePos(this);
		if( value == null ) {
			// Special case for generation of parametriezed
			// with primitive types classes
			if( reqType != null && !reqType.isReference() ) {
				switch(reqType.signature.byteAt(0)) {
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
		else if( value instanceof KString ) {
			code.addConst((KString)value);
		}
		else throw new RuntimeException("Internal error: unknown type of constant "+value.getClass());
		if( reqType == Type.tpVoid ) code.addInstr(op_pop);
	}

}

