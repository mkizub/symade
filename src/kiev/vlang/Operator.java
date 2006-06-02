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

public class Operator implements Constants {

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
		"xfxfy"		// XFXFY
	};
	
	static {
		AssignOperator.Initialize();
		BinaryOperator.Initialize();
		MultiOperator.Initialize();
		PrefixOperator.Initialize();
		PostfixOperator.Initialize();
	}

	public				OpArg[]		args;
	public				int			priority;
	public				String		image;
	public				String		name;
	public				int			mode;
	public				boolean		is_standard;
	public				Method[]	methods;
	@virtual @abstract
	public:r,r,r,rw		String		smode;

	protected Operator(OpArg[] opa, int pr, String img, String nm, String oa, boolean std) {
		args = opa;
		priority = pr;
		image = img.intern();
		name = nm.intern();
		is_standard = std;
		methods = new Method[0];
		for(int i=0; i < orderAndArityNames.length; i++) {
			if( orderAndArityNames[i].equals(oa) ) {
				mode = i;
				break;
			}
		}
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

	public int getArgPriority() {
		switch(mode) {
		case XFX:
		case YFX:
		case XF:
		case FX:
		case XFXFY:
			return priority+1;
		case LFY:
		case XFY:
		case YFY:
		case YF:
		case FY:
			return priority;
		}
		return priority;
	}

	public int getArgPriority(int n) {
		switch(mode) {
		case YFX:
			return priority+(n==0?0:1);
		case XFX:
		case XF:
		case FX:
			return priority+1;
		case XFXFY:
			return priority+(n==2?0:1);
		case LFY:
		case XFY:
			return priority+(n==0?1:0);
		case YFY:
		case YF:
		case FY:
			return priority;
		}
		return priority;
	}

	public String toString() { return image.toString(); }

	public String toDeclString() {
		return "operator("+priority+","+orderAndArityNames[mode]+","+image+")";
	}

	public static String toDeclString(int pr,String sm,String im) {
		return "operator("+pr+","+sm+","+im+")";
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

public class AssignOperator extends Operator {

	// Assign (binary) operators
	public static final AssignOperator Assign;
	public static final AssignOperator Assign2;
	public static final AssignOperator AssignBitOr;
	public static final AssignOperator AssignBitXor;
	public static final AssignOperator AssignBitAnd;
	public static final AssignOperator AssignLeftShift;
	public static final AssignOperator AssignRightShift;
	public static final AssignOperator AssignUnsignedRightShift;
	public static final AssignOperator AssignAdd;
	public static final AssignOperator AssignSub;
	public static final AssignOperator AssignMul;
	public static final AssignOperator AssignDiv;
	public static final AssignOperator AssignMod;

	static void Initialize() {
		Assign = newAssignOperator("=", "L = V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("="),new EXPR(opAssignPriority)}
		);
		Assign2 = newAssignOperator(":=", "L := V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(":="),new EXPR(opAssignPriority)}
		);

		AssignBitOr = newAssignOperator("|=", "L |= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("|="),new EXPR(opAssignPriority)}
		);
		AssignBitXor = newAssignOperator("^=", "L ^= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("^="),new EXPR(opAssignPriority)}
		);
		AssignBitAnd = newAssignOperator("&=", "L &= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("&="),new EXPR(opAssignPriority)}
		);

		AssignLeftShift = newAssignOperator("<<=", "L <<= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("<<="),new EXPR(opAssignPriority)}
		);
		AssignRightShift = newAssignOperator(">>=", "L >>= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(">>="),new EXPR(opAssignPriority)}
		);
		AssignUnsignedRightShift = newAssignOperator(">>>=", "L >>>= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER(">>>="),new EXPR(opAssignPriority)}
		);

		AssignAdd = newAssignOperator("+=", "L += V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("+="),new EXPR(opAssignPriority)}
		);
		AssignSub = newAssignOperator("-=", "L -= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("-="),new EXPR(opAssignPriority)}
		);
		AssignMul = newAssignOperator("*=", "L *= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("*="),new EXPR(opAssignPriority)}
		);
		AssignDiv = newAssignOperator("/=", "L /= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("/="),new EXPR(opAssignPriority)}
		);
		AssignMod = newAssignOperator("%=", "L %= V", true,
			new OpArg[]{new EXPR(opAssignPriority+1),new OPER("%="),new EXPR(opAssignPriority)}
		);
	}

	protected AssignOperator(OpArg[] opa, String img, String nm, boolean std) {
		super(opa,opAssignPriority,img,nm,orderAndArityNames[LFY],std);
		allOperatorsHash.put(nm,this);
	}

	public static AssignOperator newAssignOperator(String img, String nm, boolean std, OpArg[] opa) {
		AssignOperator op = allOperatorsHash.get(nm);
		if( op != null )
			return op;
		return new AssignOperator(opa,img,nm,std);
	}

	public static AssignOperator getOperator(String nm) {
		Operator op = allOperatorsHash.get(nm);
		if (op instanceof AssignOperator)
			return (AssignOperator)op;
		return null;
	}

}

public class BinaryOperator extends Operator {

	// Binary operators
	public static final BinaryOperator BooleanOr;
	public static final BinaryOperator BooleanAnd;
	public static final BinaryOperator BitOr;
	public static final BinaryOperator BitXor;
	public static final BinaryOperator BitAnd;
	public static final BinaryOperator Equals;
	public static final BinaryOperator NotEquals;
	public static final BinaryOperator InstanceOf;
	public static final BinaryOperator LessThen;
	public static final BinaryOperator LessEquals;
	public static final BinaryOperator GreaterThen;
	public static final BinaryOperator GreaterEquals;
	public static final BinaryOperator LeftShift;
	public static final BinaryOperator RightShift;
	public static final BinaryOperator UnsignedRightShift;
	public static final BinaryOperator Add;
	public static final BinaryOperator Sub;
	public static final BinaryOperator Mul;
	public static final BinaryOperator Div;
	public static final BinaryOperator Mod;

	public static final BinaryOperator Access;
	public static final BinaryOperator Comma;
	
	static void Initialize() {
		BooleanOr = newBinaryOperator(opBooleanOrPriority, "||", "V || V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBooleanOrPriority),new OPER("||"),new EXPR(opBooleanOrPriority+1)}
		);
		BooleanAnd = newBinaryOperator(opBooleanAndPriority, "&&", "V && V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBooleanAndPriority),new OPER("&&"),new EXPR(opBooleanAndPriority+1)}
		);
		BooleanOr.is_boolean_op = true;
		BooleanAnd.is_boolean_op = true;

		BitOr = newBinaryOperator(opBitOrPriority, "|", "V | V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitOrPriority),new OPER("|"),new EXPR(opBitOrPriority+1)}
		);
		BitXor = newBinaryOperator(opBitXorPriority, "^", "V ^ V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitXorPriority),new OPER("^"),new EXPR(opBitXorPriority+1)}
		);
		BitAnd = newBinaryOperator(opBitAndPriority, "&", "V & V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opBitAndPriority),new OPER("&"),new EXPR(opBitAndPriority+1)}
		);

		Equals = newBinaryOperator(opEqualsPriority, "==", "V == V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opEqualsPriority+1),new OPER("=="),new EXPR(opEqualsPriority+1)}
		);
		NotEquals = newBinaryOperator(opEqualsPriority, "!=", "V != V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opEqualsPriority+1),new OPER("!="),new EXPR(opEqualsPriority+1)}
		);
		InstanceOf = newBinaryOperator(opInstanceOfPriority, "instanceof", "V instanceof T",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opInstanceOfPriority+1),new OPER("instanceof"),new TYPE()}
		);
		Equals.is_boolean_op = true;
		NotEquals.is_boolean_op = true;
		InstanceOf.is_boolean_op = true;

		LessThen = newBinaryOperator(opComparePriority, "<", "V < V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER("<"),new EXPR(opComparePriority+1)}
		);
		LessEquals = newBinaryOperator(opComparePriority, "<=", "V <= V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER("<="),new EXPR(opComparePriority+1)}
		);
		GreaterThen = newBinaryOperator(opComparePriority, ">", "V > V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER(">"),new EXPR(opComparePriority+1)}
		);
		GreaterEquals = newBinaryOperator(opComparePriority, ">=", "V >= V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opComparePriority+1),new OPER(">="),new EXPR(opComparePriority+1)}
		);
		LessThen.is_boolean_op = true;
		LessEquals.is_boolean_op = true;
		GreaterThen.is_boolean_op = true;
		GreaterEquals.is_boolean_op = true;

		LeftShift = newBinaryOperator(opShiftPriority, "<<", "V << V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER("<<"),new EXPR(opShiftPriority+1)}
		);
		RightShift = newBinaryOperator(opShiftPriority, ">>", "V >> V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER(">>"),new EXPR(opShiftPriority+1)}
		);
		UnsignedRightShift = newBinaryOperator(opShiftPriority, ">>>", "V >>> V",orderAndArityNames[XFX],true,
			new OpArg[]{new EXPR(opShiftPriority+1),new OPER(">>>"),new EXPR(opShiftPriority+1)}
		);

		Add = newBinaryOperator(opAddPriority, "+", "V + V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAddPriority),new OPER("+"),new EXPR(opAddPriority+1)}
		);
		Sub = newBinaryOperator(opAddPriority, "-", "V - V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAddPriority),new OPER("-"),new EXPR(opAddPriority+1)}
		);

		Mul = newBinaryOperator(opMulPriority, "*", "V * V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("*"),new EXPR(opMulPriority+1)}
		);
		Div = newBinaryOperator(opMulPriority, "/", "V / V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("/"),new EXPR(opMulPriority+1)}
		);
		Mod = newBinaryOperator(opMulPriority, "%", "V % V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opMulPriority),new OPER("%"),new EXPR(opMulPriority+1)}
		);

		Access = newBinaryOperator(opAccessPriority, ".", "V . N",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(opAccessPriority),new OPER("."),new IDENT("")}
		);
		Comma = newBinaryOperator(1, ",", "V , V",orderAndArityNames[YFX],true,
			new OpArg[]{new EXPR(1),new OPER(","),new EXPR(2)}
		);
	}

	public boolean is_boolean_op;

	protected BinaryOperator(OpArg[] opa, int pr, String img, String nm, String oa, boolean std) {
		super(opa,pr,img,nm,oa,std);
		allOperatorsHash.put(nm,this);
	}

	public static BinaryOperator newBinaryOperator(int pr, String img, String nm, String oa, boolean std, OpArg[] opa) {
		BinaryOperator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new BinaryOperator(opa,pr,img,nm,oa,std);
	}

	public static BinaryOperator getOperator(String nm) {
		Operator op = allOperatorsHash.get(nm);
		if (op instanceof BinaryOperator)
			return (BinaryOperator)op;
		return null;
	}

}

public class MultiOperator extends Operator {

	// Binary operators
	public static final MultiOperator Conditional;

	static void Initialize() {
		Conditional = newMultiOperator(opConditionalPriority, new String[]{"?",":"}, "V ? V : V",true,
			new OpArg[]{new EXPR(opConditionalPriority+1),new OPER("?"),new EXPR(opConditionalPriority+1),new OPER(":"),new EXPR(opConditionalPriority)}
		);
	}

	public String[]	images;

	protected MultiOperator(OpArg[] opa, int pr, String[] img, String nm, boolean std) {
		super(opa,pr,img[0],nm,orderAndArityNames[XFXFY],std);
		images = img;
		allOperatorsHash.put(nm,this);
	}

	public static MultiOperator newMultiOperator(int pr, String[] img, String nm, boolean std, OpArg[] opa) {
		MultiOperator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			// Verify priority, and instruction
			if( op.priority != pr || img.length != op.images.length ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,orderAndArityNames[XFXFY],img[0]));
			}
			for(int i=0; i < op.images.length; i++ ) {
				if( !img[i].equals(op.images[i]) )
					throw new RuntimeException("Wrong redeclaration of operator "+op+" should be "+op.toDeclString());
			}
			return op;
		}
		return new MultiOperator(opa,pr,img,nm,std);
	}

	public static MultiOperator getOperator(String nm) {
		Operator op = allOperatorsHash.get(nm);
		if (op instanceof MultiOperator)
			return (MultiOperator)op;
		return null;
	}

}

public class PrefixOperator extends Operator {

	// Unary prefix operators
	public static final PrefixOperator Pos;
	public static final PrefixOperator Neg;
	public static final PrefixOperator PreIncr;
	public static final PrefixOperator PreDecr;
	public static final PrefixOperator BitNot;
	public static final PrefixOperator BooleanNot;

	static void Initialize() {
		Pos = newPrefixOperator(opNegPriority, "+", "+ V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("+"),new EXPR(opNegPriority)}
		);
		Neg = newPrefixOperator(opNegPriority, "-", "- V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("-"),new EXPR(opNegPriority)}
		);

		PreIncr = newPrefixOperator(opIncrPriority, "++", "++ V",orderAndArityNames[FX],true,
			new OpArg[]{new OPER("++"),new EXPR(opIncrPriority+1)}
		);
		PreDecr = newPrefixOperator(opIncrPriority, "--", "-- V",orderAndArityNames[FX],true,
			new OpArg[]{new OPER("--"),new EXPR(opIncrPriority+1)}
		);

		BitNot = newPrefixOperator(opBitNotPriority, "~", "~ V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("~"),new EXPR(opBitNotPriority)}
		);
		BooleanNot = newPrefixOperator(opBooleanNotPriority, "!", "! V",orderAndArityNames[FY],true,
			new OpArg[]{new OPER("!"),new EXPR(opBooleanNotPriority)}
		);
	}

	protected PrefixOperator(OpArg[] opa, int pr, String img, String nm, String oa, boolean std) {
		super(opa,pr,img,nm,oa,std);
		allOperatorsHash.put(nm,this);
	}

	public static PrefixOperator newPrefixOperator(int pr, String img, String nm, String oa, boolean std, OpArg[] opa) {
		PrefixOperator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new PrefixOperator(opa,pr,img,nm,oa,std);
	}

	public static PrefixOperator getOperator(String nm) {
		Operator op = allOperatorsHash.get(nm);
		if (op instanceof PrefixOperator)
			return (PrefixOperator)op;
		return null;
	}

}

public class PostfixOperator extends Operator {

	// Unary postfix operators
	public static final PostfixOperator PostIncr;
	public static final PostfixOperator PostDecr;

	static void Initialize() {
		PostIncr = newPostfixOperator(opIncrPriority, "++", "V ++",orderAndArityNames[XF],true,
			new OpArg[]{new EXPR(opIncrPriority+1),new OPER("++")}
		);
		PostDecr = newPostfixOperator(opIncrPriority, "--", "V --",orderAndArityNames[XF],true,
			new OpArg[]{new EXPR(opIncrPriority+1),new OPER("--")}
		);
	}


	protected PostfixOperator(OpArg[] opa, int pr, String img, String nm, String oa, boolean std) {
		super(opa,pr,img,nm,oa,std);
		allOperatorsHash.put(nm,this);
	}

	public static PostfixOperator newPostfixOperator(int pr, String img, String nm, String oa, boolean std, OpArg[] opa) {
		PostfixOperator op = allOperatorsHash.get(nm);
		if( op != null ) {
			if (pr == 0)
				pr = op.priority;
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new PostfixOperator(opa,pr,img,nm,oa,std);
	}

	public static PostfixOperator getOperator(String nm) {
		Operator op = allOperatorsHash.get(nm);
		if (op instanceof PostfixOperator)
			return (PostfixOperator)op;
		return null;
	}

}

public class CastOperator extends Operator {

	private static final OpArg[] castArgs = {new OPER("("),new TYPE(),new OPER(")"),new EXPR(opCastPriority)};
	
	public Type		type;
	public boolean  reinterp;

	public CastOperator(Type tp, boolean r) {
		super(castArgs,opCastPriority,"","( T ) V",orderAndArityNames[FY],true);
		type = tp;
		reinterp = r;
	}

	public static CastOperator newCastOperator(Type tp, boolean reinterp) {
		return new CastOperator(tp,reinterp);
	}

	public String toString() {
		return (reinterp?"($reinterp ":"($cast ")+type+")";
	}

}

