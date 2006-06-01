package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.types.*;
import kiev.be.java15.Instr;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Operator.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class Operator implements Constants {

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

	public				int			priority;
	public				String		image;
	public				String		name;
	public				Instr		instr;
    public				int			mode;
    public				boolean		is_standard;
    public				Method[]	methods;
	@virtual @abstract
    public:r,r,r,rw		String		smode;

	protected Operator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		priority = pr;
		image = img.intern();
		name = nm.intern();
		instr = in;
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
		Hashtable<String,Operator> hashes[] = new Hashtable<String,Operator>[]{
			AssignOperator.hash, BinaryOperator.hash, PrefixOperator.hash,
			PostfixOperator.hash, MultiOperator.hash
		};
		foreach( Hashtable<String,Operator> hash; hashes ) {
			foreach( Operator op; hash ) {
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

	public static Hashtable<String,AssignOperator>	hash = new Hashtable<String,AssignOperator>();

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

	static {
		Assign = newAssignOperator("=", "L = V",null,true);
		Assign2 = newAssignOperator(":=", "L := V",null,true);

		AssignBitOr = newAssignOperator("|=", "L |= V", Instr.op_or,true);
		AssignBitXor = newAssignOperator("^=", "L ^= V", Instr.op_xor,true);
		AssignBitAnd = newAssignOperator("&=", "L &= V", Instr.op_and,true);

		AssignLeftShift = newAssignOperator("<<=", "L <<= V", Instr.op_shl,true);
		AssignRightShift = newAssignOperator(">>=", "L >>= V", Instr.op_shr,true);
		AssignUnsignedRightShift = newAssignOperator(">>>=", "L >>>= V", Instr.op_ushr,true);

		AssignAdd = newAssignOperator("+=", "L += V", Instr.op_add,true);
		AssignSub = newAssignOperator("-=", "L -= V", Instr.op_sub,true);
		AssignMul = newAssignOperator("*=", "L *= V", Instr.op_mul,true);
		AssignDiv = newAssignOperator("/=", "L /= V", Instr.op_div,true);
		AssignMod = newAssignOperator("%=", "L %= V", Instr.op_rem,true);
	}

	protected AssignOperator(String img, String nm, Instr in, boolean std) {
		super(opAssignPriority,img,nm,in,orderAndArityNames[LFY],std);
		hash.put(img,this);
	}

	public static AssignOperator newAssignOperator(String img, String nm, Instr in, boolean std) {
		AssignOperator op = hash.get(img);
		if( op != null )
			return op;
		return new AssignOperator(img,nm,in,std);
	}

	public static AssignOperator getOperator(String im) {
		return hash.get(im);
	}

}

public class BinaryOperator extends Operator {

	public static Hashtable<String,BinaryOperator>	hash = new Hashtable<String,BinaryOperator>();

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
	
	static {
		BooleanOr = newBinaryOperator(opBooleanOrPriority, "||", "V || V",null,orderAndArityNames[YFX],true);
		BooleanAnd = newBinaryOperator(opBooleanAndPriority, "&&", "V && V",null,orderAndArityNames[YFX],true);
		BooleanOr.is_boolean_op = true;
		BooleanAnd.is_boolean_op = true;

		BitOr = newBinaryOperator(opBitOrPriority, "|", "V | V",Instr.op_or,orderAndArityNames[YFX],true);
		BitXor = newBinaryOperator(opBitXorPriority, "^", "V ^ V",Instr.op_xor,orderAndArityNames[YFX],true);
		BitAnd = newBinaryOperator(opBitAndPriority, "&", "V & V",Instr.op_and,orderAndArityNames[YFX],true);

		Equals = newBinaryOperator(opEqualsPriority, "==", "V == V",null,orderAndArityNames[XFX],true);
		NotEquals = newBinaryOperator(opEqualsPriority, "!=", "V != V",null,orderAndArityNames[XFX],true);
		InstanceOf = newBinaryOperator(opInstanceOfPriority, "instanceof", "V instanceof T",Instr.op_instanceof,orderAndArityNames[XFX],true);
		Equals.is_boolean_op = true;
		NotEquals.is_boolean_op = true;
		InstanceOf.is_boolean_op = true;

		LessThen = newBinaryOperator(opComparePriority, "<", "V < V",null,orderAndArityNames[XFX],true);
		LessEquals = newBinaryOperator(opComparePriority, "<=", "V <= V",null,orderAndArityNames[XFX],true);
		GreaterThen = newBinaryOperator(opComparePriority, ">", "V > V",null,orderAndArityNames[XFX],true);
		GreaterEquals = newBinaryOperator(opComparePriority, ">=", "V >= V",null,orderAndArityNames[XFX],true);
		LessThen.is_boolean_op = true;
		LessEquals.is_boolean_op = true;
		GreaterThen.is_boolean_op = true;
		GreaterEquals.is_boolean_op = true;

		LeftShift = newBinaryOperator(opShiftPriority, "<<", "V << V",Instr.op_shl,orderAndArityNames[XFX],true);
		RightShift = newBinaryOperator(opShiftPriority, ">>", "V >> V",Instr.op_shr,orderAndArityNames[XFX],true);
		UnsignedRightShift = newBinaryOperator(opShiftPriority, ">>>", "V >>> V",Instr.op_ushr,orderAndArityNames[XFX],true);

		Add = newBinaryOperator(opAddPriority, "+", "V + V",Instr.op_add,orderAndArityNames[YFX],true);
		Sub = newBinaryOperator(opAddPriority, "-", "V - V",Instr.op_sub,orderAndArityNames[YFX],true);

		Mul = newBinaryOperator(opMulPriority, "*", "V * V",Instr.op_mul,orderAndArityNames[YFX],true);
		Div = newBinaryOperator(opMulPriority, "/", "V / V",Instr.op_div,orderAndArityNames[YFX],true);
		Mod = newBinaryOperator(opMulPriority, "%", "V % V",Instr.op_rem,orderAndArityNames[YFX],true);

		Access = newBinaryOperator(opAccessPriority, ".", "V . N",null,orderAndArityNames[YFX],true);
		Comma = newBinaryOperator(1, ",", "V , V",null,orderAndArityNames[YFX],true);
	}

	public boolean is_boolean_op;

	protected BinaryOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static BinaryOperator newBinaryOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		BinaryOperator op = hash.get(img);
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
		return new BinaryOperator(pr,img,nm,in,oa,std);
	}

	public static BinaryOperator getOperator(String im) {
		return hash.get(im);
	}

}

public class MultiOperator extends Operator {

	public static Hashtable<String,MultiOperator>	hash = new Hashtable<String,MultiOperator>();

	// Binary operators
	public static final MultiOperator Conditional;

	static {
		Conditional = newMultiOperator(opConditionalPriority, new String[]{"?",":"}, "V ? V : V",true);
	}

	public String[]	images;

	protected MultiOperator(int pr, String[] img, String nm, boolean std) {
		super(pr,img[0],nm,null,orderAndArityNames[XFXFY],std);
		images = img;
		hash.put(img[0],this);
	}

	public static MultiOperator newMultiOperator(int pr, String[] img, String nm, boolean std) {
		MultiOperator op = hash.get(img[0]);
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
		return new MultiOperator(pr,img,nm,std);
	}

	public static MultiOperator getOperator(String im) {
		return hash.get(im);
	}

}

public class PrefixOperator extends Operator {

	public static Hashtable<String,PrefixOperator>	hash = new Hashtable<String,PrefixOperator>();

	// Unary prefix operators
	public static final PrefixOperator Pos;
	public static final PrefixOperator Neg;
	public static final PrefixOperator PreIncr;
	public static final PrefixOperator PreDecr;
	public static final PrefixOperator BitNot;
	public static final PrefixOperator BooleanNot;

	static {
		Pos = newPrefixOperator(opNegPriority, "+", "+ V",Instr.op_nop,orderAndArityNames[FY],true);
		Neg = newPrefixOperator(opNegPriority, "-", "- V",Instr.op_neg,orderAndArityNames[FY],true);

		PreIncr = newPrefixOperator(opIncrPriority, "++", "++ V",null,orderAndArityNames[FX],true);
		PreDecr = newPrefixOperator(opIncrPriority, "--", "-- V",null,orderAndArityNames[FX],true);

		BitNot = newPrefixOperator(opBitNotPriority, "~", "~ V",null,orderAndArityNames[FY],true);
		BooleanNot = newPrefixOperator(opBooleanNotPriority, "!", "! V",null,orderAndArityNames[FY],true);
	}

	protected PrefixOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static PrefixOperator newPrefixOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		PrefixOperator op = hash.get(img);
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
		return new PrefixOperator(pr,img,nm,in,oa,std);
	}

	public static PrefixOperator getOperator(String im) {
		return hash.get(im);
	}

}

public class PostfixOperator extends Operator {

	public static Hashtable<String,PostfixOperator>	hash = new Hashtable<String,PostfixOperator>();

	// Unary postfix operators
	public static final PostfixOperator PostIncr;
	public static final PostfixOperator PostDecr;

	static {
		PostIncr = newPostfixOperator(opIncrPriority, "++", "V ++",null,orderAndArityNames[XF],true);
		PostDecr = newPostfixOperator(opIncrPriority, "--", "V --",null,orderAndArityNames[XF],true);
	}


	protected PostfixOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static PostfixOperator newPostfixOperator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		PostfixOperator op = hash.get(img);
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
		return new PostfixOperator(pr,img,nm,in,oa,std);
	}

	public static PostfixOperator getOperator(String im) {
		return hash.get(im);
	}

}

public class CastOperator extends Operator {

	public Type		type;
	public boolean  reinterp;

	public CastOperator(Type tp, boolean r) {
		super(opCastPriority,"","( T ) V",null,orderAndArityNames[FY],true);
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

