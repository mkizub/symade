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
package kiev.vlang;

import kiev.vlang.OpArg.EXPR;
import kiev.vlang.OpArg.TYPE;
import kiev.vlang.OpArg.IDNT;
import kiev.vlang.OpArg.OPER;
import kiev.vlang.OpArg.SEQS;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class OpArg {
	public case EXPR(final int priority);
	public case TYPE();
	public case IDNT();
	public case OPER(final String text);
	public case SEQS(final OpArg el, final OPER sep, int min);
	
	public String toString() {
		switch (this) {
		case EXPR(int priority)	: return "EXPR/"+priority;
		case TYPE()				: return "TYPE";
		case IDNT()				: return "IDNT";
		case OPER(String text)	: return "OPER "+text;
		case SEQS(final OpArg el, final OPER sep, int min) : return "SEQS{"+el+" / "+sep+" / "+min+"}";
		}
	}

	public static OpArg[] fromOpString(int pr, String nm) {
		String[] names = nm.split("\\s+");
		Stack<String> stk = new Stack<String>();
		for (int i=names.length-1; i >= 0; i--)
			stk.push(names[i]);
		Vector<OpArg> args = new Vector<OpArg>();
		while (!stk.isEmpty())
			args.append(fromOpSymbol(pr,stk));
		return args.toArray();
	}
	private static OpArg fromOpSymbol(int pr, Stack<String> names) {
		String s = names.pop();
		if (s.equals("X")) return new EXPR(pr+1);
		if (s.equals("Y")) return new EXPR(pr);
		if (s.equals("Z")) return new EXPR(0);
		if (s.equals("T")) return new TYPE();
		if (s.equals("I")) return new IDNT();
		if (s.equals("{")) {
			OpArg el  = fromOpSymbol(pr, names);
			OPER sep = (OPER)fromOpSymbol(pr, names);
			s = names.pop();
			if (s.equals("}"))
				return new SEQS(el,sep,2);
			if (s.equals("}+"))
				return new SEQS(el,sep,1);
			if (s.equals("}*"))
				return new SEQS(el,sep,0);
			throw new RuntimeException("Bad operator format in syntax end: "+s);
		}
		return new OPER(s.intern());
	}
	public static String toOpName(OpArg[] args) {
		StringBuffer sb = new StringBuffer();
		foreach (OpArg a; args) {
			switch (a) {
			case EXPR(_)			: sb.append("V "); break;
			case TYPE()				: sb.append("T "); break;
			case IDNT()				: sb.append("I "); break;
			case OPER(String text)	: sb.append(text).append(' '); break;
			case SEQS(final OpArg el, final OPER sep, int min) :
				sb.append("{ ");
				sb.append(toOpName(new OpArg[]{el}));
				sb.append(' ').append(sep.text).append(' ');
				if (min == 0)
					sb.append("}* ");
				else if (min == 1)
					sb.append("}+ ");
				else
					sb.append("} ");
				break;
			}
		}
		return sb.toString().trim().intern();
	}
}

public final class Operator implements Constants {

	public static Hashtable<String,Operator>	allOperatorNamesHash = new Hashtable<String,Operator>();
	public static Hashtable<String,Operator>	allOperatorDeclsHash = new Hashtable<String,Operator>();

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
	public static final Operator AssignElem;

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
	public static final Operator CallAccess;
	public static final Operator CallTypesAccess;
	public static final Operator NewAccess;
	public static final Operator ClassAccess;
	public static final Operator MacroAccess;
	public static final Operator Comma;
	public static final Operator ElemAccess;
	public static final Operator RuleIsThe;
	public static final Operator RuleIsOneOf;
	
	// Unary prefix operators
	public static final Operator Pos;
	public static final Operator Neg;
	public static final Operator PreIncr;
	public static final Operator PreDecr;
	public static final Operator BitNot;
	public static final Operator BooleanNot;
	public static final Operator PostIncr;
	public static final Operator PostDecr;
	public static final Operator Parenth;
	
	public static final Operator TypeAccess;
	public static final Operator PostTypeArgs;
	public static final Operator PostTypeArgs2;
	public static final Operator PostTypePVar;
	public static final Operator PostTypeRef;
	public static final Operator PostTypeAST;
	public static final Operator PostTypeWrapper;
	public static final Operator PostTypeWildcardCoVariant;
	public static final Operator PostTypeWildcardContraVariant;
	public static final Operator PostTypeArray;
	public static final Operator PostTypeVararg;
	public static final Operator PostTypeSpace;

	// Multi operators
	public static final Operator Conditional;
	public static final Operator Cast;
	public static final Operator CastForce;
	public static final Operator Reinterp;
	public static final Operator CallApplay;
	
	public static final Operator[] allAssignOperators;
	public static final Operator[] allBoolOperators;
	public static final Operator[] allMathOperators;

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
		AssignElem					= newOperator(256, "X [ Z ] = Y"); // 256 to disable usage in parsing
		
		allAssignOperators = new Operator[]{Assign,Assign2,AssignBitOr,AssignBitXor,AssignBitAnd,
			AssignLeftShift,AssignRightShift,AssignUnsignedRightShift,
			AssignAdd,AssignSub,AssignMul,AssignDiv,AssignMod //,AssignElem
		};

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

		allBoolOperators = new Operator[]{BooleanOr,BooleanAnd,BitOr,BitXor,BitAnd,
			Equals,NotEquals,InstanceOf,LessThen,LessEquals,GreaterThen,GreaterEquals
		};
		
		LeftShift = newOperator(opShiftPriority, "X << X");
		RightShift = newOperator(opShiftPriority, "X >> X");
		UnsignedRightShift = newOperator(opShiftPriority, "X >>> X");

		Add = newOperator(opAddPriority, "Y + X");
		Sub = newOperator(opAddPriority, "Y - X");
		Mul = newOperator(opMulPriority, "Y * X");
		Div = newOperator(opMulPriority, "Y / X");
		Mod = newOperator(opMulPriority, "Y % X");

		allMathOperators = new Operator[]{Add,Sub,Mul,Div,Mod,LeftShift,RightShift,UnsignedRightShift};
		
		Access = newOperator(opAccessPriority, "Y . I");
		CallAccess = newOperator(opAccessPriority, "Y . I ( { Z , }* )");
		CallTypesAccess = newOperator(opAccessPriority, "Y . I < { T , }* > ( { Z , }* )");
		NewAccess = newOperator(opAccessPriority, "Y . new T ( { Z , }* )");
		ClassAccess = newOperator(opAccessPriority, "T . class");
		MacroAccess = newOperator(opAccessPriority, "Y \u21a3 I"); // ↣
		Comma = newOperator(256, "{ X , }"); // 256 to disable usage in parsing
		ElemAccess = newOperator(opAccessPriority, "Y [ Z ]");

		RuleIsThe = newOperator(opAssignPriority, "X ?= X");
		RuleIsOneOf = newOperator(opAssignPriority, "X @= X");

		// Unary operators
		Pos = newOperator(opNegPriority, "+ Y");
		Neg = newOperator(opNegPriority, "- Y");
		PreIncr = newOperator(opIncrPriority, "++ X");
		PreDecr = newOperator(opIncrPriority, "-- X");
		BitNot = newOperator(opBitNotPriority, "~ Y");
		BooleanNot = newOperator(opBooleanNotPriority, "! Y");
		PostIncr = newOperator(opIncrPriority, "X ++");
		PostDecr = newOperator(opIncrPriority, "X --");
		Parenth = newOperator(255, "( Z )");

		TypeAccess      = newOperator(255, "T . I");						TypeAccess.is_type_operator = true;
		PostTypeArgs    = newOperator(255, "T < { T , }+ >");				PostTypeArgs.is_type_operator = true;
		PostTypeArgs2   = newOperator(255, "T <\u0335 { T , }+ >\u0335");	PostTypeArgs2.is_type_operator = true; // T <̵ { T , }+ >̵
		PostTypePVar    = newOperator(255, "T @");							PostTypePVar.is_type_operator = true;
		PostTypeRef     = newOperator(255, "T &");							PostTypeRef.is_type_operator = true;
		PostTypeAST     = newOperator(255, "T #");							PostTypeAST.is_type_operator = true;
		PostTypeWrapper = newOperator(255, "T \u229b");					PostTypeWrapper.is_type_operator = true; // ⊛
		PostTypeWildcardCoVariant     = newOperator(255, "T \u207a");		PostTypeWildcardCoVariant.is_type_operator = true; // superscript ⁺
		PostTypeWildcardContraVariant = newOperator(255, "T \u207b");		PostTypeWildcardContraVariant.is_type_operator = true; // superscript ⁻
		PostTypeArray   = newOperator(255, "T []");						PostTypeArray.is_type_operator = true;
		PostTypeVararg  = newOperator(255, "T ...");						PostTypeVararg.is_type_operator = true;
		PostTypeSpace   = newOperator(255, "T \u2205");					PostTypeSpace.is_type_operator = true; // ∅

		// Multi operators
		Conditional = newOperator(opConditionalPriority, "X ? X : Y");

		Cast = newOperator(opCastPriority, "( T ) Y");
		CastForce = newOperator(opCastPriority, "( $cast T ) Y");
		Reinterp = newOperator(opCastPriority, "( $reinterp T ) Y");
		CallApplay = newOperator(opAccessPriority, "I ( { Z , }* )");
	}

	public static Operator newOperator(int pr, String decl) {
		decl = decl.intern();
		Operator op = allOperatorDeclsHash.get(decl);
		if( op != null )
			return op;
		OpArg[] args = OpArg.fromOpString(pr, decl);
		String name = OpArg.toOpName(args).intern();
		op = allOperatorNamesHash.get(name);
		if( op != null )
			throw new RuntimeException("Redeclaration of an operator from "+op.decl+" to "+decl);
		int ar = 0;
		int ma = 0;
		foreach (OpArg a; args) {
			switch (a) {
			case EXPR(_)	: ar++; ma++; break;
			case TYPE()		: ar++; ma++; break;
			case IDNT()		: ar++; ma++; break;
			case OPER(_)	: ma++; break;
			case SEQS(_,_, int min):
				if (min > 0)
					ma = min*2 - 1;
				 break;
			}
		}
		return new Operator(args,pr,ar,ma,name,decl);
	}

	public final		OpArg[]		args;
	public final		int			priority;
	public final		int			arity;
	public final		int			min_args;
	public final		String		name;
	public final		String		decl;
	public				boolean		is_type_operator;
	private				Method[]	methods;

	private Operator(OpArg[] args, int pr, int ar, int ma, String name, String decl) {
		this.args = args;
		this.priority = pr;
		this.arity = ar;
		this.min_args = ma;
		this.name = name.intern();
		this.decl = decl.intern();
		this.methods = new Method[0];
		allOperatorNamesHash.put(name,this);
		allOperatorDeclsHash.put(decl,this);
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Operator)) return false;
		Operator op = (Operator)o;
		return this.name == op.name;
	}

	public void addMethod(Method m) {
		assert (m.hasName(this.name));
		for(int i=0; i < methods.length; i++)
			if (methods[i] == m)
				return;
		methods = (Method[])Arrays.appendUniq(methods,m);
	}

	public static void cleanupMethod(Method m) {
		foreach( Operator op; allOperatorDeclsHash ) {
			Method[] methods = op.methods;
			for(int i=0; i < methods.length; i++) {
				if (methods[i] == m) {
					Method[] tmp = new Method[methods.length-1];
					for (int j=0; j < i; j++)
						tmp[j] = methods[j];
					for (int j=i; j < tmp.length; j++)
						tmp[j] = methods[j+1];
					op.methods = methods = tmp;
					i--;
				}
			}
		}
	}

	public static Operator lookupOperatorForMethod(Method m) {
		foreach (Operator o; Operator.allOperatorDeclsHash) {
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
					if (e == null || e.getPriority() < priority)
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
				if (eidx == idx) return this.priority+1;
				eidx++;
				continue;
			case OPER(String text):
				continue;
			}
		}
		return 256;
	}

	public static Operator getOperatorByName(String nm) {
		return allOperatorNamesHash.get(nm);
	}
	public static Operator getOperatorByDecl(String nm) {
		return allOperatorDeclsHash.get(nm);
	}

	public Method resolveMethod(ENode expr) {
		Method@ m;
		ENode[] args = expr.getArgs();
		ResInfo info = new ResInfo(expr, this.name, ResInfo.noStatic);
		Type[] tps = new Type[args.length-1];
		for (int i=0; i < tps.length; i++) {
			tps[i] = args[i+1].getType();
			tps[i].checkResolved();
		}
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

