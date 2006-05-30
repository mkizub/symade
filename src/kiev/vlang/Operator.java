package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.OpTypes.*;
import kiev.vlang.types.*;
import kiev.be.java15.Instr;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.OpTypes.*;
import static kiev.vlang.Operator.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public class OpTypes {
	public TypeRule		trtypes[];
    public Method		method;

	public OpTypes() {}

	public static Type getExprType(Object n, Type tp) {
		Type t = null;
		switch(n) {
		case ENode:
		{
			ENode e = (ENode)n;
			trace( Kiev.debugOperators,"type of "+n+" is "+e.getType());
			t = e.getType();
			break;
		}
		case Type:
			trace( Kiev.debugOperators,"type of "+n+" is "+((Type)n));
			t = (Type)n;
			break;
		case Struct:
			trace( Kiev.debugOperators,"type of "+n+" is "+((Struct)n).xtype);
			t = ((Struct)n).xtype;
			break;
		}
		if( t == null )
			throw new RuntimeException("getExprType of "+(n==null?"null":n.getClass().toString()));
		return t;
	}

	public abstract class TypeRule {
		int position;
		boolean auto_cast;
		public TypeRule() { auto_cast = true; }
		public TypeRule(boolean auto_cast) { this.auto_cast = auto_cast; }
		public abstract rule resolve(Type[] types, ASTNode[] nodes);
	}

	public class rtAny extends TypeRule {
		public rtAny() {}

		public rtAny(boolean auto ) { super(auto); }

		public rule resolve(Type[] types, ASTNode[] nodes)
		{
			trace( Kiev.debugOperators,"check "+(position==0?"ret":"arg"+position)+" ("+nodes[position]+") to be "+this),
			{
				// Skip rule
//				types[position] == null, nodes[position] == null, $cut,
//				trace( Kiev.debugOperators,"type "+types[position]+" ("+nodes[position]+") match to be "+this)
//			;
				// If both present, then check they match
				types[position] != null, nodes[position] != null, $cut,
				{
					auto_cast,
					getExprType(nodes[position],types[position]).isAutoCastableTo(types[position])
				;	!auto_cast,
					getExprType(nodes[position],types[position]).isInstanceOf(types[position])
				},
				trace( Kiev.debugOperators,"type "+getExprType(nodes[position],types[position])+" ("+nodes[position]+") match to be "+types[position])
			;
//				// If type exists - let it be
//				types[position] != null, $cut,
//				trace( Kiev.debugOperators,"type "+types[position]+" ("+nodes[position]+") match to be "+this)
//			;
				// If type not exists - try resolve expression
				types[position] == null, nodes[position] != null, $cut,
				types[position] = getExprType(nodes[position],types[position]),
				trace( Kiev.debugOperators,"type "+types[position]+" ("+nodes[position]+") resolved and match to be "+this)
			},
			trace( Kiev.debugOperators,"type "+types[position]+" resolved for "+(position==0?"ret":"arg"+position))
		}
		public String toString() { return "<any>"; }
	}

	public class rtType extends TypeRule {
		public Type				type;
		public int				flag;

		public rtType( int flag ) { this.flag = flag; }

		public rtType( Type type ) { this.type = type; }

		public rtType( int flag, boolean auto ) { super(auto); this.flag = flag; }

		public rtType( Type type, boolean auto ) { super(auto); this.type = type; }

		public rule resolve(Type[] types, ASTNode[] nodes)
		{
			trace( Kiev.debugOperators,"check "+(position==0?"ret":"arg"+position)+" ("+nodes[position]+") to be "+this),
			{
				// If type given
				type!=null, $cut,
				{
					// If nodes not exists - simply set the type
					nodes[position] == null, $cut,
					types[position] = type
				;
					// If nodes exists, check it match
					nodes[position] != null, $cut,
					{
						auto_cast,
						{
							types[position] != null, types[position].isAutoCastableTo(type)
						;	getExprType(nodes[position],types[position]).isAutoCastableTo(type),
							types[position] = getExprType(nodes[position],types[position]),
							types[position].isAutoCastableTo(type)
						}
					;	!auto_cast,
						{
							types[position] != null, types[position].isInstanceOf(type)
						;	getExprType(nodes[position],types[position]).isInstanceOf(type),
							types[position] = getExprType(nodes[position],types[position]),
							types[position].isInstanceOf(type)
						}
					}
				}
			;
				// Now, if a flag of type is known
				$cut,
				{
//					// If nodes not exists - skip the rule
//					nodes[position] == null, $cut
//				;
					// If nodes exists, check it match
					nodes[position] != null, $cut,
					{
						types[position] != null, (this.flag & types[position].flags) != 0
					;	(this.flag & getExprType(nodes[position],types[position]).flags) != 0,
						types[position] = getExprType(nodes[position],types[position])
					}
				}
			},
			trace( Kiev.debugOperators,"type "+types[position]+" resolved for "+(position==0?"ret":"arg"+position))
		}

		public String toString() {
			if( type != null ) return type.toString();
			switch(flag) {
			case Type.flReference:		return "<reference>";
			case Type.flInteger:		return "<integer>";
			case Type.flFloat:			return "<float>";
			case Type.flNumber:			return "<number>";
			case Type.flArray:			return "<array>";
			case Type.flBoolean:		return "<boolean>";
			}
			return "<type "+flag+">";
		}
	}

	rule resolveRef(TypeRule tr, int ref, Type[] types, ASTNode[] nodes)
		TypeRule@ at;
	{
		trace( Kiev.debugOperators,"need to find out "+(ref==0?"ret":"arg"+ref)+" ("+nodes[ref]+") referred by "+tr),
		{
//			types[ref] == Type.tpAny,					// locked
//			$cut,
//			trace( Kiev.debugOperators,"op_type_res: type at "+ref+" is already locked!!! Assuming own type "+getExprType(nodes[ref],types[ref])),
//			types[ref] = getExprType(nodes[ref],null)
//		;
			types[ref] == null,							// not resolved yet
			$cut,
			at ?= this.trtypes[ref],
			at.resolve(types,nodes)						// resolve
		;
			types[ref] != null,							// not resolved yet
			$cut,
			trace( Kiev.debugOperators,"op_type_res: type at "+ref+" is already resolved as "+types[ref]),
			types[ref] ≢ Type.tpAny					// already resolved
		},
		trace( Kiev.debugOperators,"type "+types[ref]+" resolved for "+(ref==0?"ret":"arg"+ref))
	}

	public class rtSame extends TypeRule {
		public int		ref;

		public rtSame(int ref) { this.ref = ref; }
		public rule resolve(Type[] types, ASTNode[] nodes)
		{
			trace( Kiev.debugOperators,"opt_check "+position+": "+nodes[position]+" to be Same("+ref+")"),
			// Resolve reference, if need
			resolveRef(this,ref,types,nodes),
			trace( Kiev.debugOperators,"opt_check "+position+": "+types[position]+" to be Same("+ref+")"),
			{
				// If both ebscant then assign
				types[position] == null, nodes[position] == null, $cut,
				trace( Kiev.debugOperators,"opt_check "+position+" type set to be "+types[ref]),
				types[position] = types[ref]
			;
				// If both present, then check they match
				types[position] != null, nodes[position] != null, $cut,
				trace( Kiev.debugOperators,"opt_check "+position+" type "+getExprType(nodes[position],types[position])+" to be same as "+types[ref]),
				getExprType(nodes[position],types[position]).isAutoCastableTo(types[ref])
			;
				// If type exists - let it be
				types[position] != null, $cut,
				trace( Kiev.debugOperators,"opt_check "+position+" type "+types[position]+" to be same as "+types[ref]),
				types[position].isAutoCastableTo(types[ref])
			;
				// If type not exists - try resolve expression
				types[position] == null, nodes[position] != null, $cut,
				types[position] = getExprType(nodes[position],types[ref]),
				trace( Kiev.debugOperators,"opt_check "+position+" type "+types[position]+" to be same as "+types[ref]),
				types[position].isAutoCastableTo(types[ref])
			},
			trace( Kiev.debugOperators,"opt_check "+position+" set to be "+types[position])
		}

		public String toString() { return "<same "+ref+">"; }
	}

	public class rtUpperCastNumber extends TypeRule {
		public int		ref1;
		public int		ref2;

		public rtUpperCastNumber(int ref1, int ref2) { this.ref1 = ref1; this.ref2 = ref2; }
		public rule resolve(Type[] types, ASTNode[] nodes)
			Type@ tp;
		{
			trace( Kiev.debugOperators,"opt_check "+position+": "+nodes[position]+" to be UpperCastNumber("+ref1+","+ref2+")"),
			{
				ref1 != position, resolveRef(this,ref1,types,nodes)
			;	ref1 == position, types[ref1] = getExprType(nodes[ref1],types[ref1])
			},
			{
				ref2 != position, resolveRef(this,ref2,types,nodes)
			;	ref2 == position, types[ref2] = getExprType(nodes[ref2],types[ref2])
			},
			trace( Kiev.debugOperators,"opt_check "+types[ref1]+" and "+types[ref2]+" to be UpperCastable to "+types[position]),
			{
				types[ref1] == null && types[ref2] == null, $cut
			;
				types[ref1] == null && types[ref2].isNumber(), $cut
			;
				types[ref2] == null && types[ref1].isNumber(), $cut
			;
				types[ref1].isNumber() && types[ref2].isNumber(),
				{
					types[ref1] ≡ Type.tpDouble || types[ref2] ≡ Type.tpDouble, tp ?= Type.tpDouble
				;	types[ref1] ≡ Type.tpFloat || types[ref2] ≡ Type.tpFloat, tp ?= Type.tpFloat
				;	types[ref1] ≡ Type.tpLong || types[ref2] ≡ Type.tpLong, tp ?= Type.tpLong
				;	types[ref1] ≡ Type.tpInt || types[ref2] ≡ Type.tpInt, tp ?= Type.tpInt
				;	types[ref1] ≡ Type.tpChar || types[ref2] ≡ Type.tpChar, tp ?= Type.tpChar
				;	types[ref1] ≡ Type.tpShort || types[ref2] ≡ Type.tpShort, tp ?= Type.tpShort
				;	types[ref1] ≡ Type.tpByte || types[ref2] ≡ Type.tpByte, tp ?= Type.tpByte
				},
				types[position] = tp
			},
			trace( Kiev.debugOperators,"opt_check "+position+" set to be "+types[position])
		}

		public String toString() { return "<uppercast "+ref1+","+ref2+">"; }
	}

	public class rtDownCast extends TypeRule {
		public int		ref1;
		public int		ref2;

		public rtDownCast(int ref1, int ref2) { this.ref1 = ref1; this.ref2 = ref2; }
		public rule resolve(Type[] types, ASTNode[] nodes)
		{
			trace( Kiev.debugOperators,"opt_check "+position+": "+nodes[position]+" to be DownCast("+ref1+","+ref2+")"),
			{
				ref1 != position, resolveRef(this,ref1,types,nodes)
			;	ref1 == position, types[ref1] = getExprType(nodes[ref1],types[ref1])
			},
			{
				ref2 != position, resolveRef(this,ref2,types,nodes)
			;	ref2 == position, types[ref2] = getExprType(nodes[ref2],types[ref2])
			},
			{
				{ types[ref1] == null ; types[ref2] == null }
			;
				types[ref1].isReference() && types[ref2].isReference(),
				types[position] = Type.leastCommonType(types[ref1],types[ref2])
			},
			trace( Kiev.debugOperators,"opt_check "+position+" set to be "+types[position])
		}

		public String toString() { return "<downcast "+ref1+","+ref2+">"; }
	}

	public boolean match(Type[] ts, ASTNode[] nodes) {
		trace( Kiev.debugOperators,"Resolving "+Arrays.toString(nodes)+" to match "+Arrays.toString(ts));
		if( method != null ) {
			if( method.isStatic() ) {
				// Check we've imported the method
			} else {
				// Check method is of nodes[1]'s class
				if( method.type.arity == (nodes.length-2) && nodes[1] != null
					&& getExprType(nodes[1],ts[1]).isInstanceOf(method.ctx_tdecl.xtype)
				)
					;
				// Check method arg of nodes[1]'s class
				//else if( method.type.args.length == 1 && nodes[1] != null
				//	&& getExprType(nodes[1],ts[1]).isInstanceOf(method.type.args[0])
				//)
				//	;
				else
					return false;
			}
		}
		for(int i=0; i < ts.length; i++) {
//			if( ts[i] == null ) {
				trace( Kiev.debugOperators,"need to find out "+(i==0?"ret":"arg"+i)+" ("+nodes[i]+") to match "+ts[i]);
				if( !trtypes[i].resolve(ts,nodes) ) {
					trace( Kiev.debugOperators,"op_type_res: "+i+" may not be defined");
					return false;
				}
				trace( Kiev.debugOperators,"op_type_res: "+i+" was found to be "+ts[i]);
//			}
		}
		return true;
	}

	public boolean match(ASTNode[] nodes) {
		Type[] ts = new Type[trtypes.length];
		return match(ts,nodes);
	}

	public String toString() { return "{"+Arrays.toString(trtypes)+"}"; }

}


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

	public static OpTypes 								iopt;	// initialization OpTypes
	public static OpTypes.TypeRule otAny()				{ return iopt.new rtAny(); }
	public static OpTypes.TypeRule otTheAny()			{ return iopt.new rtAny(false); }
	public static OpTypes.TypeRule otBoolean()			{ return iopt.new rtType( Type.flBoolean ); }
	public static OpTypes.TypeRule otNumber()			{ return iopt.new rtType( Type.flNumber ); }
	public static OpTypes.TypeRule otInteger()			{ return iopt.new rtType( Type.flInteger ); }
	public static OpTypes.TypeRule otReference()		{ return iopt.new rtType( Type.flReference ); }
	public static OpTypes.TypeRule otArray()			{ return iopt.new rtType( Type.flArray ); }
	public static OpTypes.TypeRule otSame(int i)		{ return iopt.new rtSame( i ); }
	public static OpTypes.TypeRule otType(Type tp)		{ return iopt.new rtType( tp ); }
	public static OpTypes.TypeRule otTheType(Type tp)	{ return iopt.new rtType( tp, false ); }
	public static OpTypes.TypeRule otUpperCastNumber(int i, int j)	{ return iopt.new rtUpperCastNumber( i, j ); }
	public static OpTypes.TypeRule otDownCast(int i, int j)			{ return iopt.new rtDownCast( i, j ); }

	public				int			priority;
	public				String		image;
	public				String		name;
	public				Instr		instr;
    public				int			mode;
    public				boolean		is_standard;
    public				OpTypes[]	types;
	@virtual @abstract
    public:r,r,r,rw		String		smode;

	protected Operator(int pr, String img, String nm, Instr in, String oa, boolean std) {
		priority = pr;
		image = img.intern();
		name = nm.intern();
		instr = in;
		is_standard = std;
		types = new OpTypes[0];
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

	public void addTypes(OpTypes.TypeRule rt, ...) {
		iopt.trtypes = new TypeRule[va_args.length+1];
		iopt.trtypes[0] = rt;
		iopt.trtypes[0].position = 0;
		for(int i=1; i < iopt.trtypes.length; i++) {
			iopt.trtypes[i] = (TypeRule)va_args[i-1];
			iopt.trtypes[i].position = i;
		}
		types = (OpTypes[])Arrays.append(types,iopt);
	}

	public void removeTypes(OpTypes opt) {
		for(int i=0; i < types.length; i++) {
			if( types[i] == opt ) {
				removeTypes(i);
				return;
			}
		}
		throw new RuntimeException("OpTypes "+opt+" do not exists in "+this);
	}

	public void removeTypes(int index) {
		OpTypes[] newtypes = new OpTypes[types.length-1];
		for(int k=0, l=0; k < types.length; k++) {
			if( k != index ) newtypes[l++] = types[k];
		}
		types = newtypes;
	}

	public static void cleanupMethod(Method m) {
		Hashtable<String,Operator> hashes[] = new Hashtable<String,Operator>[]{
			AssignOperator.hash, BinaryOperator.hash, PrefixOperator.hash,
			PostfixOperator.hash, MultiOperator.hash
		};
		foreach( Hashtable<String,Operator> hash; hashes ) {
			foreach( Operator op; hash; op.types.length > 0 ) {
				for(int i=0; i < op.types.length; i++) {
					OpTypes opt = op.types[i];
					if (opt.method == m) {
						op.removeTypes(i);
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
		OpTypes@ opt;
	{
		opt @= types,
		opt.method != null,
		info.check(opt.method),
		opt.method.equalsByCast(name,mt,Type.tpVoid,info),
		node ?= opt.method
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
			iopt=new OpTypes();
			Assign.addTypes(otSame(1),otTheAny(),otSame(1));
		Assign2 = newAssignOperator(":=", "L := V",null,true);
			iopt=new OpTypes();
			Assign2.addTypes(otSame(1),otTheAny(),otSame(1));

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
			iopt=new OpTypes();
			Conditional.addTypes(otDownCast(2,3),otBoolean(),otAny(),otAny());
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

