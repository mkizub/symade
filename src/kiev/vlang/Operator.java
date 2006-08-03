package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import kiev.vlang.OpArg.EXPR;
import kiev.vlang.OpArg.TYPE;
import kiev.vlang.OpArg.OPER;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class OpArg {
	public case EXPR(final int priority);
	public case TYPE();
	public case OPER(final String text);
	
	public String toString() {
		switch (this) {
		case EXPR(int priority)	: return "EXPR/"+priority;
		case TYPE()				: return "TYPE";
		case OPER(String text)	: return "OPER "+text;
		}
	}

	public static OpArg[] fromOpString(int pr, String nm) {
		String[] names = nm.split("\\s+");
		OpArg[] args = new OpArg[names.length];
		for (int i=0; i < names.length; i++) {
			String s = names[i];
			if (s.equals("X")) { args[i] = new EXPR(pr+1); continue; }
			if (s.equals("Y")) { args[i] = new EXPR(pr); continue; }
			if (s.equals("T")) { args[i] = new TYPE(); continue; }
			args[i] = new OPER(s.intern());
		}
		return args;
	}
	public static String toOpName(OpArg[] args) {
		StringBuffer sb = new StringBuffer();
		foreach (OpArg a; args) {
			switch (a) {
			case EXPR(_)			: sb.append("V "); break;
			case TYPE()				: sb.append("T "); break;
			case OPER(String text)	: sb.append(text).append(' '); break;
			}
		}
		return sb.toString().trim().intern();
	}
}

public final class Operator implements Constants {

	public static Hashtable<String,Operator>	allOperatorsHash = new Hashtable<String,Operator>();

	// Assign (binary) operators
	public static final Operator Assign;
	public static final Operator Assign2;
	public static final Operator AssignBitOr;
	public static final Operator AssignBitXor;
	public static final Operator AssignBitAnd;
	public static final Operator AssignLeftShift;
	public static final Operator AssignRightShift;
	public static final Operator AssignUnsignedRightShift;
	public static final Operator AssignAdd;
	public static final Operator AssignSub;
	public static final Operator AssignMul;
	public static final Operator AssignDiv;
	public static final Operator AssignMod;

	// Binary operators
	public static final Operator BooleanOr;
	public static final Operator BooleanAnd;
	public static final Operator BitOr;
	public static final Operator BitXor;
	public static final Operator BitAnd;
	public static final Operator Equals;
	public static final Operator NotEquals;
	public static final Operator InstanceOf;
	public static final Operator LessThen;
	public static final Operator LessEquals;
	public static final Operator GreaterThen;
	public static final Operator GreaterEquals;
	public static final Operator LeftShift;
	public static final Operator RightShift;
	public static final Operator UnsignedRightShift;
	public static final Operator Add;
	public static final Operator Sub;
	public static final Operator Mul;
	public static final Operator Div;
	public static final Operator Mod;

	public static final Operator Access;
	public static final Operator Comma;
	public static final Operator RuleIsThe;
	public static final Operator RuleIsOneOf;
	
	// Unary prefix operators
	public static final Operator Pos;
	public static final Operator Neg;
	public static final Operator PreIncr;
	public static final Operator PreDecr;
	public static final Operator BitNot;
	public static final Operator BooleanNot;

	// Unary postfix operators
	public static final Operator PostIncr;
	public static final Operator PostDecr;
	
	public static final Operator PostTypePVar;
	public static final Operator PostTypeRef;
	public static final Operator PostTypeAST;
	public static final Operator PostTypeWrapper;
	public static final Operator PostTypeArray;
	public static final Operator PostTypeVararg;

	// Multi operators
	public static final Operator Conditional;
	public static final Operator Cast;
	public static final Operator CastForce;
	public static final Operator Reinterp;
	

	static {
		// Assign operators
		Assign						= newOperator(opAssignPriority, "X = Y");
		Assign2						= newOperator(opAssignPriority, "X := Y");
		AssignBitOr					= newOperator(opAssignPriority, "X |= Y");
		AssignBitXor				= newOperator(opAssignPriority, "X ^= Y");
		AssignBitAnd				= newOperator(opAssignPriority, "X &= Y");
		AssignLeftShift				= newOperator(opAssignPriority, "X <<= Y");
		AssignRightShift			= newOperator(opAssignPriority, "X >>= Y");
		AssignUnsignedRightShift	= newOperator(opAssignPriority, "X >>>= Y");
		AssignAdd					= newOperator(opAssignPriority, "X += Y");
		AssignSub					= newOperator(opAssignPriority, "X -= Y");
		AssignMul					= newOperator(opAssignPriority, "X *= Y");
		AssignDiv					= newOperator(opAssignPriority, "X /= Y");
		AssignMod					= newOperator(opAssignPriority, "X %= Y");

		// Binary operators
		BooleanOr = newOperator(opBooleanOrPriority, "Y || X");
		BooleanAnd = newOperator(opBooleanAndPriority, "Y && X");

		BitOr = newOperator(opBitOrPriority, "Y | X");
		BitXor = newOperator(opBitXorPriority, "Y ^ X");
		BitAnd = newOperator(opBitAndPriority, "Y & X");

		Equals = newOperator(opEqualsPriority, "X == X");
		NotEquals = newOperator(opEqualsPriority, "X != X");
		InstanceOf = newOperator(opInstanceOfPriority, "X instanceof T");
		LessThen = newOperator(opComparePriority, "X < X");
		LessEquals = newOperator(opComparePriority, "X <= X");
		GreaterThen = newOperator(opComparePriority, "X > X");
		GreaterEquals = newOperator(opComparePriority, "X >= X");

		LeftShift = newOperator(opShiftPriority, "X << X");
		RightShift = newOperator(opShiftPriority, "X >> X");
		UnsignedRightShift = newOperator(opShiftPriority, "X >>> X");

		Add = newOperator(opAddPriority, "Y + X");
		Sub = newOperator(opAddPriority, "Y - X");
		Mul = newOperator(opMulPriority, "Y * X");
		Div = newOperator(opMulPriority, "Y / X");
		Mod = newOperator(opMulPriority, "Y % X");

		Access = newOperator(opAccessPriority, "Y . I");
		Comma = newOperator(1, "Y , X");

		RuleIsThe = newOperator(opAssignPriority, "X ?= X");
		RuleIsOneOf = newOperator(opAssignPriority, "X @= X");

		// Unary prefix operators
		Pos = newOperator(opNegPriority, "+ Y");
		Neg = newOperator(opNegPriority, "- Y");

		PreIncr = newOperator(opIncrPriority, "++ X");
		PreDecr = newOperator(opIncrPriority, "-- X");

		BitNot = newOperator(opBitNotPriority, "~ Y");
		BooleanNot = newOperator(opBooleanNotPriority, "! Y");

		// Unary postfix operators
		PostIncr = newOperator(opIncrPriority, "X ++");
		PostDecr = newOperator(opIncrPriority, "X --");

		PostTypePVar    = newOperator(opIncrPriority, "T @");
		PostTypeRef     = newOperator(opIncrPriority, "T &");
		PostTypeAST     = newOperator(opIncrPriority, "T #");
		PostTypeWrapper = newOperator(opIncrPriority, "T \u229b"); // ⊛
		PostTypeArray   = newOperator(opIncrPriority, "T []");
		PostTypeVararg  = newOperator(opIncrPriority, "T ...");

		// Multi operators
		Conditional = newOperator(opConditionalPriority, "X ? X : Y");

		Cast = newOperator(opCastPriority, "( T ) Y");
		CastForce = newOperator(opCastPriority, "( $cast T ) Y");
		Reinterp = newOperator(opCastPriority, "( $reinterp T ) Y");
	}

	public static Operator newOperator(int pr, String nm) {
		OpArg[] args = OpArg.fromOpString(pr, nm);
		nm = OpArg.toOpName(args);
		Operator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			return op;
		}
		return new Operator(args,pr,nm);
	}

	public final		OpArg[]		args;
	public final		int			priority;
	public final		String		name;
	private				Method[]	methods;

	private Operator(OpArg[] args, int pr, String nm) {
		this.args = args;
		this.priority = pr;
		this.name = nm.intern();
		this.methods = new Method[0];
		allOperatorsHash.put(nm,this);
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Operator)) return false;
		Operator op = (Operator)o;
		return this.name == op.name;
	}

	public void addMethod(Method m) {
		assert (m.hasName(this.name,true));
		for(int i=0; i < methods.length; i++)
			if (methods[i] == m)
				return;
		methods = (Method[])Arrays.appendUniq(methods,m);
	}

	public static void cleanupMethod(Method m) {
		foreach( Operator op; allOperatorsHash ) {
			Method[] methods = op.methods;
			for(int i=0; i < methods.length; i++) {
				if (methods[i] == m) {
					Method[] tmp = new Method[methods.length-1];
					for (int j=0; j < i; j++)
						tmp[j] = methods[j];
					for (int j=i; j < tmp.length; j++)
						tmp[j] = methods[j+1];
					op.methods[i] = methods = tmp;
					i--;
				}
			}
		}
	}

	public static Operator lookupOperatorForMethod(Method m) {
		foreach (Operator o; Operator.allOperatorsHash) {
			foreach (Method x; o.methods; x == m) {
				return o;
			}
		}
		return null;
	}

	public String toString() { return name; }

	public String toString(ENode e) {
		ENode[] exprs = e.getArgs();
		StringBuffer sb = new StringBuffer();
		int eidx = 0;
		foreach (OpArg arg; this.args) {
			switch (arg) {
			case EXPR(int priority):
			{
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					if (e.getPriority() < priority)
						sb.append('(').append(e).append(')');
					else
						sb.append(e).append(' ');
				} else {
					sb.append("(???)");
				}
				continue;
			}
			case TYPE():
			{
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					sb.append(e).append(' ');
				} else {
					sb.append("(???)");
				}
				continue;
			}
			case OPER(String text):
				sb.append(' ').append(text).append(' ');
				continue;
			}
		}
		return sb.toString().trim();
	}
	
	public int getArgPriority(int idx) {
		int eidx = 0;
		foreach (OpArg arg; this.args) {
			switch (arg) {
			case EXPR(int priority):
				if (eidx == idx) return priority;
				eidx++;
				continue;
			case TYPE():
				if (eidx == idx) return 255;
				eidx++;
				continue;
			case OPER(String text):
				continue;
			}
		}
		return 255;
	}

	public static Operator getOperator(String nm) {
		return allOperatorsHash.get(nm);
	}

	public Method resolveMethod(ENode expr) {
		Method@ m;
		ENode[] args = expr.getArgs();
		ResInfo info = new ResInfo(expr, this.name, ResInfo.noStatic);
		Type[] tps = new Type[args.length-1];
		for (int i=0; i < tps.length; i++)
			tps[i] = args[i+1].getType();
		CallType mt = new CallType(args[0].getType(), null, tps, null, false);
		if (PassInfo.resolveBestMethodR(args[0].getType(),m,info,mt))
			return (Method)m;
		info = new ResInfo(expr, this.name, 0);
		tps = new Type[args.length];
		for (int i=0; i < tps.length; i++)
			tps[i] = args[i].getType();
		mt = new CallType(null, null, tps, null, false);
		if (PassInfo.resolveBestMethodR(this,m,info,mt))
			return (Method)m;
		return null;
	}

	final public rule resolveOperatorMethodR(Method@ node, ResInfo info, CallType mt)
		Method@ m;
	{
		m @= this.methods,
		info.checkNodeName(m),
		info.check(m),
		m.equalsByCast(info.getName(),mt,Type.tpVoid,info),
		node ?= m
	}
}

