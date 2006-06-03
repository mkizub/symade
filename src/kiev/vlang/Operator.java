package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import kiev.vlang.OpArg.EXPR;
import kiev.vlang.OpArg.TYPE;
import kiev.vlang.OpArg.OPER;
import kiev.vlang.OpArg.IDENT;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class OpArg {
	public case EXPR(final int priority);
	public case TYPE();
	public case OPER(final String text);
	public case IDENT(final String text);
	
	public String toString() {
		switch (this) {
		case EXPR(int priority)	: return "EXPR/"+priority;
		case TYPE()				: return "TYPE";
		case OPER(String text)	: return "OPER "+text;
		case IDENT(String text)	: return "IDENT \""+text+"\"";
		}
	}
}

public final class Operator implements Constants {

	public static Hashtable<String,Operator>	allOperatorsHash = new Hashtable<String,Operator>();

	// Assign orders
	public static final int LFY			= 0;

	// Binary orders
	public static final int XFX			= 1;
	public static final int XFY			= 2;
	public static final int YFX			= 3;
	public static final int YFY			= 4;

	// Prefix orders
	public static final int XF			= 5;
	public static final int YF			= 6;

	// Postfix orders
	public static final int FX			= 7;
	public static final int FY			= 8;

	// Multi operators
	public static final int XFXFY		= 9;
	public static final int FXFY		= 10;

	// Order/arity strings
	public static final String[]	orderAndArityNames = new String[] {
		"lfy",		// LFY
		"xfx",		// XFX
		"xfy",		// XFY
		"yfx",		// YFX
		"yfy",		// YFY
		"xf",		// XF
		"yf",		// YF
		"fx",		// FX
		"fy",		// FY
		"xfxfy",	// XFXFY
		"fxfy"		// FXFY
	};
	
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

	// Multi operators
	public static final Operator Conditional;
	public static final Operator Cast;
	public static final Operator CastForce;
	public static final Operator Reinterp;
	

	static {
		Assign = newOperator(opAssignPriority, "=", "L = V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("="),new EXPR(opAssignPriority)}
		);
		Assign2 = newOperator(opAssignPriority, ":=", "L := V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(":="),new EXPR(opAssignPriority)}
		);

		AssignBitOr = newOperator(opAssignPriority, "|=", "L |= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("|="),new EXPR(opAssignPriority)}
		);
		AssignBitXor = newOperator(opAssignPriority, "^=", "L ^= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("^="),new EXPR(opAssignPriority)}
		);
		AssignBitAnd = newOperator(opAssignPriority, "&=", "L &= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("&="),new EXPR(opAssignPriority)}
		);

		AssignLeftShift = newOperator(opAssignPriority, "<<=", "L <<= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("<<="),new EXPR(opAssignPriority)}
		);
		AssignRightShift = newOperator(opAssignPriority, ">>=", "L >>= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(">>="),new EXPR(opAssignPriority)}
		);
		AssignUnsignedRightShift = newOperator(opAssignPriority, ">>>=", "L >>>= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(">>>="),new EXPR(opAssignPriority)}
		);

		AssignAdd = newOperator(opAssignPriority, "+=", "L += V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("+="),new EXPR(opAssignPriority)}
		);
		AssignSub = newOperator(opAssignPriority, "-=", "L -= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("-="),new EXPR(opAssignPriority)}
		);
		AssignMul = newOperator(opAssignPriority, "*=", "L *= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("*="),new EXPR(opAssignPriority)}
		);
		AssignDiv = newOperator(opAssignPriority, "/=", "L /= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("/="),new EXPR(opAssignPriority)}
		);
		AssignMod = newOperator(opAssignPriority, "%=", "L %= V", orderAndArityNames[LFY], true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("%="),new EXPR(opAssignPriority)}
		);

		// Binary operators
		BooleanOr = newOperator(opBooleanOrPriority, "||", "V || V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBooleanOrPriority),new OPER("||"),new EXPR(opBooleanOrPriority+1)}
		);
		BooleanAnd = newOperator(opBooleanAndPriority, "&&", "V && V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBooleanAndPriority),new OPER("&&"),new EXPR(opBooleanAndPriority+1)}
		);

		BitOr = newOperator(opBitOrPriority, "|", "V | V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitOrPriority),new OPER("|"),new EXPR(opBitOrPriority+1)}
		);
		BitXor = newOperator(opBitXorPriority, "^", "V ^ V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitXorPriority),new OPER("^"),new EXPR(opBitXorPriority+1)}
		);
		BitAnd = newOperator(opBitAndPriority, "&", "V & V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitAndPriority),new OPER("&"),new EXPR(opBitAndPriority+1)}
		);

		Equals = newOperator(opEqualsPriority, "==", "V == V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opEqualsPriority+1),new OPER("=="),new EXPR(opEqualsPriority+1)}
		);
		NotEquals = newOperator(opEqualsPriority, "!=", "V != V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opEqualsPriority+1),new OPER("!="),new EXPR(opEqualsPriority+1)}
		);
		InstanceOf = newOperator(opInstanceOfPriority, "instanceof", "V instanceof T",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opInstanceOfPriority+1),new OPER("instanceof"),new TYPE()}
		);

		LessThen = newOperator(opComparePriority, "<", "V < V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER("<"),new EXPR(opComparePriority+1)}
		);
		LessEquals = newOperator(opComparePriority, "<=", "V <= V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER("<="),new EXPR(opComparePriority+1)}
		);
		GreaterThen = newOperator(opComparePriority, ">", "V > V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER(">"),new EXPR(opComparePriority+1)}
		);
		GreaterEquals = newOperator(opComparePriority, ">=", "V >= V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER(">="),new EXPR(opComparePriority+1)}
		);

		LeftShift = newOperator(opShiftPriority, "<<", "V << V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER("<<"),new EXPR(opShiftPriority+1)}
		);
		RightShift = newOperator(opShiftPriority, ">>", "V >> V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER(">>"),new EXPR(opShiftPriority+1)}
		);
		UnsignedRightShift = newOperator(opShiftPriority, ">>>", "V >>> V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER(">>>"),new EXPR(opShiftPriority+1)}
		);

		Add = newOperator(opAddPriority, "+", "V + V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAddPriority),new OPER("+"),new EXPR(opAddPriority+1)}
		);
		Sub = newOperator(opAddPriority, "-", "V - V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAddPriority),new OPER("-"),new EXPR(opAddPriority+1)}
		);

		Mul = newOperator(opMulPriority, "*", "V * V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("*"),new EXPR(opMulPriority+1)}
		);
		Div = newOperator(opMulPriority, "/", "V / V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("/"),new EXPR(opMulPriority+1)}
		);
		Mod = newOperator(opMulPriority, "%", "V % V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("%"),new EXPR(opMulPriority+1)}
		);

		Access = newOperator(opAccessPriority, ".", "V . N",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAccessPriority),new OPER("."),new IDENT("")}
		);
		Comma = newOperator(1, ",", "V , V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(1),new OPER(","),new EXPR(2)}
		);

		// Unary prefix operators
		Pos = newOperator(opNegPriority, "+", "+ V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("+"),new EXPR(opNegPriority)}
		);
		Neg = newOperator(opNegPriority, "-", "- V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("-"),new EXPR(opNegPriority)}
		);

		PreIncr = newOperator(opIncrPriority, "++", "++ V",orderAndArityNames[FX],true,
			new OpArg[]{new OPER("++"),new EXPR(opIncrPriority+1)}
		);
		PreDecr = newOperator(opIncrPriority, "--", "-- V",orderAndArityNames[FX],true,
			new OpArg[]{new OPER("--"),new EXPR(opIncrPriority+1)}
		);

		BitNot = newOperator(opBitNotPriority, "~", "~ V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("~"),new EXPR(opBitNotPriority)}
		);
		BooleanNot = newOperator(opBooleanNotPriority, "!", "! V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("!"),new EXPR(opBooleanNotPriority)}
		);

		// Unary postfix operators
		PostIncr = newOperator(opIncrPriority, "++", "V ++",orderAndArityNames[XF],true,
			new OpArg[]{new EXPR(opIncrPriority+1),new OPER("++")}
		);
		PostDecr = newOperator(opIncrPriority, "--", "V --",orderAndArityNames[XF],true,
			new OpArg[]{new EXPR(opIncrPriority+1),new OPER("--")}
		);

		// Multi operators
		Conditional = newOperator(opConditionalPriority, "?:", "V ? V : V",orderAndArityNames[XFXFY],true,
			new OpArg[]{new EXPR(opConditionalPriority+1),new OPER("?"),new EXPR(opConditionalPriority+1),new OPER(":"),new EXPR(opConditionalPriority)}
		);

		Cast = newOperator(opCastPriority, "$cast", "( T ) V",orderAndArityNames[FXFY],true,
			new OpArg[]{new OPER("("),new TYPE(),new OPER(")"),new EXPR(opCastPriority)}
		);
		CastForce = newOperator(opCastPriority, "$cast", "( $cast T ) V",orderAndArityNames[FXFY],true,
			new OpArg[]{new OPER("($cast"),new TYPE(),new OPER(")"),new EXPR(opCastPriority)}
		);
		Reinterp = newOperator(opCastPriority, "$reinterp", "( $reinterp T ) V",orderAndArityNames[FXFY],true,
			new OpArg[]{new OPER("($reinterp"),new TYPE(),new OPER(")"),new EXPR(opCastPriority)}
		);
	}

	public static Operator newOperator(int pr, String img, String nm, String oa, boolean std, OpArg[] opa) {
		Operator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa )
				throw new RuntimeException("Wrong redeclaration of operator "+op);
			return op;
		}
		return new Operator(opa,pr,nm,oa,std);
	}

	public				OpArg[]		args;
	public				int			priority;
	public				String		name;
	public				int			mode;
	public				boolean		is_standard;
	public				Method[]	methods;
	@virtual @abstract
	public:r,r,r,rw		String		smode;

	protected Operator(OpArg[] opa, int pr, String nm, String oa, boolean std) {
		args = opa;
		priority = pr;
		name = nm.intern();
		is_standard = std;
		methods = new Method[0];
		for(int i=0; i < orderAndArityNames.length; i++) {
			if( orderAndArityNames[i].equals(oa) ) {
				mode = i;
				break;
			}
		}
		allOperatorsHash.put(nm,this);
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Operator)) return false;
		Operator op = (Operator)o;
		return this.name == op.name && this.mode == op.mode && this.priority == op.priority;
	}

	public boolean isStandard() { return is_standard; }

	@getter public String get$smode() { return orderAndArityNames[mode]; }

	public void addMethod(Method m) {
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
			case IDENT(String text):
				sb.append(' ').append(text).append(' ');
				continue;
			}
		}
		return sb.toString().trim();
	}

	public Dumper toJava(Dumper dmp, ENode e) {
		ENode[] exprs = e.getArgs();
		int eidx = 0;
		foreach (OpArg arg; this.args) {
			switch (arg) {
			case EXPR(int priority):
			{
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					if (e.getPriority() < priority)
						dmp.append('(').append(e).append(')');
					else
						dmp.append(e).space();
				} else {
					dmp.append("(???)");
				}
				continue;
			}
			case TYPE():
			{
				if (eidx < exprs.length) {
					ENode e = exprs[eidx++];
					dmp.append(e).space();
				} else {
					dmp.append("(???)");
				}
				continue;
			}
			case OPER(String text):
				dmp.space().append(text).space();
				continue;
			case IDENT(String text):
				dmp.space().append(text).space();
				continue;
			}
		}
		return dmp;
	}

	public static Operator getOperator(String nm) {
		return allOperatorsHash.get(nm);
	}

	public Method resolveMethod(ENode expr) {
		Method@ m;
		ENode[] args = expr.getArgs();
		ResInfo info = new ResInfo(expr, ResInfo.noStatic);
		Type[] tps = new Type[args.length-1];
		for (int i=0; i < tps.length; i++)
			tps[i] = args[i+1].getType();
		CallType mt = new CallType(args[0].getType(), null, tps, null, false);
		if (PassInfo.resolveBestMethodR(args[0].getType(),m,info,this.name,mt))
			return (Method)m;
		info = new ResInfo(expr, 0);
		tps = new Type[args.length];
		for (int i=0; i < tps.length; i++)
			tps[i] = args[i].getType();
		mt = new CallType(null, null, tps, null, false);
		if (PassInfo.resolveBestMethodR(this,m,info,this.name,mt))
			return (Method)m;
		return null;
	}

	final public rule resolveOperatorMethodR(Method@ node, ResInfo info, String name, CallType mt)
		Method@ m;
	{
		m @= this.methods,
		info.check(m),
		m.equalsByCast(name,mt,Type.tpVoid,info),
		node ?= m
	}
}

