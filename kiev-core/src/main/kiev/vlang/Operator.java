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
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class Operator implements Constants {

	public static Hashtable<String,Operator>	allOperatorNamesHash = new Hashtable<String,Operator>();

	// Assign (binary) operators
	public static final Operator Assign;
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
	public static final Operator NewType;
	public static final Operator NewAccess;
	public static final Operator ClassAccess;
	public static final Operator PathTypeAccess;
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
	
	public static final Operator PostTypeAST;
	public static final Operator PostTypeWrapper;
	public static final Operator PostTypeWildcardCoVariant;
	public static final Operator PostTypeWildcardContraVariant;
	public static final Operator PostTypeArray;
	public static final Operator PostTypeVararg;

	// Multi operators
	public static final Operator Conditional;
	public static final Operator Cast;
	public static final Operator CastForce;
	public static final Operator Reinterp;
	
	static {
		// Assign operators
		Assign						= newOperator("V = V");
		AssignBitOr					= newOperator("V |= V");
		AssignBitXor				= newOperator("V ^= V");
		AssignBitAnd				= newOperator("V &= V");
		AssignLeftShift				= newOperator("V <<= V");
		AssignRightShift			= newOperator("V >>= V");
		AssignUnsignedRightShift	= newOperator("V >>>= V");
		AssignAdd					= newOperator("V += V");
		AssignSub					= newOperator("V -= V");
		AssignMul					= newOperator("V *= V");
		AssignDiv					= newOperator("V /= V");
		AssignMod					= newOperator("V %= V");
		AssignElem					= newOperator("V [ V ] = V");
		
		// Binary operators
		BooleanOr = newOperator("V || V");
		BooleanAnd = newOperator("V && V");

		BitOr = newOperator("V | V");
		BitXor = newOperator("V ^ V");
		BitAnd = newOperator("V & V");

		Equals = newOperator("V == V");
		NotEquals = newOperator("V != V");
		InstanceOf = newOperator("V instanceof T");
		LessThen = newOperator("V < V");
		LessEquals = newOperator("V <= V");
		GreaterThen = newOperator("V > V");
		GreaterEquals = newOperator("V >= V");

		LeftShift = newOperator("V << V");
		RightShift = newOperator("V >> V");
		UnsignedRightShift = newOperator("V >>> V");

		Add = newOperator("V + V");
		Sub = newOperator("V - V");
		Mul = newOperator("V * V");
		Div = newOperator("V / V");
		Mod = newOperator("V % V");

		Access = newOperator("V . I");
		NewType = newOperator("new T"); // 256 to disable usage in parsing
		NewAccess = newOperator("V . new T ( { V , }* )");
		ClassAccess = newOperator("T . class");
		PathTypeAccess = newOperator("I . { I . }* type");
		MacroAccess = newOperator("V \u21a3 I"); // ↣
		Comma = newOperator("{ V , }"); // 256 to disable usage in parsing
		ElemAccess = newOperator("V [ V ]");

		RuleIsThe = newOperator("V ?= V");
		RuleIsOneOf = newOperator("V @= V");

		// Unary operators
		Pos = newOperator("+ V");
		Neg = newOperator("- V");
		PreIncr = newOperator("++ V");
		PreDecr = newOperator("-- V");
		BitNot = newOperator("~ V");
		BooleanNot = newOperator("! V");
		PostIncr = newOperator("V ++");
		PostDecr = newOperator("V --");

		PostTypeAST     = newOperator("T #");
		PostTypeWrapper = newOperator("T \u229b");						// ⊛
		PostTypeWildcardCoVariant     = newOperator("T \u207a");		// superscript ⁺
		PostTypeWildcardContraVariant = newOperator("T \u207b");		// superscript ⁻
		PostTypeArray   = newOperator("T []");
		PostTypeVararg  = newOperator("T ...");

		// Multi operators
		Conditional = newOperator("V ? V : V");

		Cast = newOperator("( T ) V");
		CastForce = newOperator("( $cast T ) V");
		Reinterp = newOperator("( $reinterp T ) V");
	}

	public static Operator newOperator(String name) {
		Operator op = allOperatorNamesHash.get(name);
		if( op != null )
			throw new RuntimeException("Redeclaration of an "+op.name);
		return new Operator(name);
	}

	public final		String		name;

	public Operator(String name) {
		this.name = name.intern();
		allOperatorNamesHash.put(name,this);
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Operator)) return false;
		Operator op = (Operator)o;
		return this.name == op.name;
	}

	public String toString() { return name; }

	public static Operator getOperatorByName(String nm) {
		return allOperatorNamesHash.get(nm);
	}

}

