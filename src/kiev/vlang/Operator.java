/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.vlang.OpTypes.*;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.OpTypes.*;
import static kiev.vlang.Operator.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Operator.java,v 1.3.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.2 $
 *
 */

public class OpTypes {
	public TypeRule		trtypes[];
    public Method		method;

	public OpTypes() {}

	public static Type getExprType(ASTNode n, Type tp) {
		Type t = null;
		switch(n) {
		case Expr:
			trace( Kiev.debugOperators,"type of "+n+" is "+((Expr)n).getType());
			if( !n.isResolved() ) {
				n = ((Expr)n).resolve(tp);
				assert( n.isResolved() );
				goto case n;
			} else {
				t = ((Expr)n).getType();
			}
			break;
		case Type:
			trace( Kiev.debugOperators,"type of "+n+" is "+((Type)n));
			t = (Type)n;
			break;
		case Struct:
			trace( Kiev.debugOperators,"type of "+n+" is "+((Struct)n).type);
			t = ((Struct)n).type;
			break;
		}
		if( t == null )
			throw new RuntimeException("getExprType of "+(n==null?"null":n.getClass().toString()));
//		if( t == Type.tpRule ) return Type.tpBoolean;
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
			types[ref] != Type.tpAny					// already resolved
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
					types[ref1] == Type.tpDouble || types[ref2] == Type.tpDouble, tp ?= Type.tpDouble
				;	types[ref1] == Type.tpFloat || types[ref2] == Type.tpFloat, tp ?= Type.tpFloat
				;	types[ref1] == Type.tpLong || types[ref2] == Type.tpLong, tp ?= Type.tpLong
				;	types[ref1] == Type.tpInt || types[ref2] == Type.tpInt, tp ?= Type.tpInt
				;	types[ref1] == Type.tpChar || types[ref2] == Type.tpChar, tp ?= Type.tpChar
				;	types[ref1] == Type.tpShort || types[ref2] == Type.tpShort, tp ?= Type.tpShort
				;	types[ref1] == Type.tpByte || types[ref2] == Type.tpByte, tp ?= Type.tpByte
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
				if( method.type.args.length == 0 && nodes[1] != null
					&& getExprType(nodes[1],ts[1]).clazz.instanceOf((Struct)method.parent)
				)
					;
				// Check method arg of nodes[1]'s class
				else if( method.type.args.length == 1 && nodes[1] != null
					&& getExprType(nodes[1],ts[1]).isInstanceOf(method.type.args[0])
				)
					;
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


public abstract class Operator extends ASTNode implements Constants {

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
	public static final KString[]	orderAndArityNames = new KString[] {
		KString.from("lfy"),		// LFY
		KString.from("xfx"),		// XFX
		KString.from("xfy"),		// XFY
		KString.from("yfx"),		// YFX
		KString.from("yfy"),		// YFY
		KString.from("xf"),			// XF
		KString.from("yf"),			// YF
		KString.from("fx"),			// FX
		KString.from("fy"),			// FY
		KString.from("xfxfy")		// XFXFY
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

	public			int			priority;
	public			KString		image;
	public			KString		name;
	public			Instr		instr;
    public			int			mode;
    public			boolean		is_standard;
    public			OpTypes[]	types;
    public virtual abstract KString	smode;

	protected Operator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		super(0);
		priority = pr;
		image = img;
		name = nm;
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
		return this.image == op.image && this.mode == op.mode && this.priority == op.priority;
	}

	public boolean isStandard() { return is_standard; }

	public void set$smode(KString sm) { throw new RuntimeException(); }
	public KString get$smode() { return orderAndArityNames[mode]; }

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
		Hashtable<KString,Operator> hashes[] = new Hashtable<KString,Operator>[]{
			AssignOperator.hash, BinaryOperator.hash, PrefixOperator.hash,
			PostfixOperator.hash, MultiOperator.hash
		};
		foreach( Hashtable<KString,Operator> hash; hashes ) {
			foreach( Operator op; hash; op.types.length > 0 ) {
				foreach( OpTypes opt; op.types; opt.method == m) {
					op.removeTypes(opt$iter);
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

	public static String toDeclString(int pr,KString sm,KString im) {
		return "operator("+pr+","+sm+","+im+")";
	}

}

public class AssignOperator extends Operator {

	public static Hashtable<KString,AssignOperator>	hash = new Hashtable<KString,AssignOperator>();

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
		Assign = newAssignOperator(KString.from("="), KString.from("opAssign"),null,true);
			iopt=new OpTypes();
			Assign.addTypes(otSame(1),otTheAny(),otSame(1));
		Assign2 = newAssignOperator(KString.from(":="), KString.from("opAssign"),null,true);
			iopt=new OpTypes();
			Assign2.addTypes(otSame(1),otTheAny(),otSame(1));

		AssignBitOr = newAssignOperator(KString.from("|="), KString.from("opAssignBitOr"), Instr.op_or,true);
			iopt=new OpTypes();
			AssignBitOr.addTypes(otSame(1),otInteger(),otSame(1));
			AssignBitOr.addTypes(otSame(1),otBoolean(),otSame(1));
		AssignBitXor = newAssignOperator(KString.from("^="), KString.from("opAssignBitXor"), Instr.op_xor,true);
			iopt=new OpTypes();
			AssignBitXor.addTypes(otSame(1),otInteger(),otSame(1));
			AssignBitXor.addTypes(otSame(1),otBoolean(),otSame(1));
		AssignBitAnd = newAssignOperator(KString.from("&="), KString.from("opAssignBitAnd"), Instr.op_and,true);
			iopt=new OpTypes();
			AssignBitAnd.addTypes(otSame(1),otInteger(),otSame(1));
			AssignBitAnd.addTypes(otSame(1),otBoolean(),otSame(1));

		AssignLeftShift = newAssignOperator(KString.from("<<="), KString.from("opAssignLeftShift"), Instr.op_shl,true);
			iopt=new OpTypes();
			AssignLeftShift.addTypes(otSame(1),otInteger(),otType(Type.tpInt));
		AssignRightShift = newAssignOperator(KString.from(">>="), KString.from("opAssignRightShift"), Instr.op_shr,true);
			iopt=new OpTypes();
			AssignRightShift.addTypes(otSame(1),otInteger(),otType(Type.tpInt));
		AssignUnsignedRightShift = newAssignOperator(KString.from(">>>="), KString.from("opAssignUnsignedRightShift"), Instr.op_ushr,true);
			iopt=new OpTypes();
			AssignUnsignedRightShift.addTypes(otSame(1),otInteger(),otType(Type.tpInt));

		AssignAdd = newAssignOperator(KString.from("+="), KString.from("opAssignAdd"), Instr.op_add,true);
			iopt=new OpTypes();
			AssignAdd.addTypes(otSame(1),otNumber(),otSame(1));
			iopt=new OpTypes();
			AssignAdd.addTypes(otType(Type.tpChar),otType(Type.tpChar),otInteger());
		AssignSub = newAssignOperator(KString.from("-="), KString.from("opAssignSub"), Instr.op_sub,true);
			iopt=new OpTypes();
			AssignSub.addTypes(otSame(1),otNumber(),otSame(1));
			iopt=new OpTypes();
			AssignSub.addTypes(otType(Type.tpChar),otType(Type.tpChar),otInteger());
		AssignMul = newAssignOperator(KString.from("*="), KString.from("opAssignMul"), Instr.op_mul,true);
			iopt=new OpTypes();
			AssignMul.addTypes(otSame(1),otNumber(),otSame(1));
		AssignDiv = newAssignOperator(KString.from("/="), KString.from("opAssignDiv"), Instr.op_div,true);
			iopt=new OpTypes();
			AssignDiv.addTypes(otSame(1),otNumber(),otSame(1));
		AssignMod = newAssignOperator(KString.from("%="), KString.from("opAssignMod"), Instr.op_rem,true);
			iopt=new OpTypes();
			AssignMod.addTypes(otSame(1),otNumber(),otSame(1));
	}

	protected AssignOperator(KString img, KString nm, Instr in, boolean std) {
		super(opAssignPriority,img,nm,in,orderAndArityNames[LFY],std);
		hash.put(img,this);
	}

	public static AssignOperator newAssignOperator(KString img, KString nm, Instr in, boolean std) {
		AssignOperator op = hash.get(img);
		if( op != null ) {
			// Verify priority, and instruction
//			if( op.instr != in ) {
//				throw new RuntimeException("Wrong redeclaration of operator "+op+
//					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(...));
//			}
			return op;
		}
		return new AssignOperator(img,nm,in,std);
	}

	public static AssignOperator getOperator(KString im) {
		return hash.get(im);
	}

}

public class BinaryOperator extends Operator {

	public static Hashtable<KString,BinaryOperator>	hash = new Hashtable<KString,BinaryOperator>();

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

	public static final BinaryOperator DotAccess; // dot access
	public static final BinaryOperator ElemAccess; // array element access

	static {
		BooleanOr = newBinaryOperator(opBooleanOrPriority, KString.from("||"), KString.from("opBooleanOr"),null,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			BooleanOr.addTypes(otType(Type.tpBoolean),otBoolean(),otBoolean());
		BooleanAnd = newBinaryOperator(opBooleanAndPriority, KString.from("&&"), KString.from("opBooleanAnd"),null,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			BooleanAnd.addTypes(otType(Type.tpBoolean),otBoolean(),otBoolean());
		BooleanOr.is_boolean_op = true;
		BooleanAnd.is_boolean_op = true;

		BitOr = newBinaryOperator(opBitOrPriority, KString.from("|"), KString.from("opBitOr"),Instr.op_or,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			BitOr.addTypes(otSame(1),otInteger(),otSame(1));
		BitXor = newBinaryOperator(opBitXorPriority, KString.from("^"), KString.from("opBitXor"),Instr.op_xor,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			BitXor.addTypes(otSame(1),otInteger(),otSame(1));
		BitAnd = newBinaryOperator(opBitAndPriority, KString.from("&"), KString.from("opBitAnd"),Instr.op_and,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			BitAnd.addTypes(otSame(1),otInteger(),otSame(1));

		Equals = newBinaryOperator(opEqualsPriority, KString.from("=="), KString.from("opEquals"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			Equals.addTypes(otType(Type.tpBoolean),otAny(),otAny());
		NotEquals = newBinaryOperator(opEqualsPriority, KString.from("!="), KString.from("opNotEquals"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			NotEquals.addTypes(otType(Type.tpBoolean),otAny(),otAny());
		InstanceOf = newBinaryOperator(opInstanceOfPriority, KString.from("instanceof"), KString.from("opInstanceOf"),Instr.op_instanceof,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			InstanceOf.addTypes(otType(Type.tpBoolean),otReference(),otType(Type.tpVoid));
		Equals.is_boolean_op = true;
		NotEquals.is_boolean_op = true;
		InstanceOf.is_boolean_op = true;

		LessThen = newBinaryOperator(opComparePriority, KString.from("<"), KString.from("opLessThen"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			LessThen.addTypes(otType(Type.tpBoolean),otUpperCastNumber(1,2),otSame(1));
		LessEquals = newBinaryOperator(opComparePriority, KString.from("<="), KString.from("opLessEquals"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			LessEquals.addTypes(otType(Type.tpBoolean),otUpperCastNumber(1,2),otSame(1));
		GreaterThen = newBinaryOperator(opComparePriority, KString.from(">"), KString.from("opGreaterThen"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			GreaterThen.addTypes(otType(Type.tpBoolean),otUpperCastNumber(1,2),otSame(1));
		GreaterEquals = newBinaryOperator(opComparePriority, KString.from(">="), KString.from("opGreaterEquals"),null,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			GreaterEquals.addTypes(otType(Type.tpBoolean),otUpperCastNumber(1,2),otSame(1));
		LessThen.is_boolean_op = true;
		LessEquals.is_boolean_op = true;
		GreaterThen.is_boolean_op = true;
		GreaterEquals.is_boolean_op = true;

		LeftShift = newBinaryOperator(opShiftPriority, KString.from("<<"), KString.from("opLeftShift"),Instr.op_shl,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			LeftShift.addTypes(otSame(1),otInteger(),otInteger());
		RightShift = newBinaryOperator(opShiftPriority, KString.from(">>"), KString.from("opRightShift"),Instr.op_shr,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			RightShift.addTypes(otSame(1),otInteger(),otInteger());
		UnsignedRightShift = newBinaryOperator(opShiftPriority, KString.from(">>>"), KString.from("opUnsignedRightShift"),Instr.op_ushr,orderAndArityNames[XFX],true);
//			iopt=new OpTypes();
//			UnsignedRightShift.addTypes(otSame(1),otInteger(),otInteger());

		Add = newBinaryOperator(opAddPriority, KString.from("+"), KString.from("opAdd"),Instr.op_add,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			Add.addTypes(otType(Type.tpString),otType(Type.tpString),otAny());
//			iopt=new OpTypes();
//			Add.addTypes(otType(Type.tpString),otAny(),otType(Type.tpString));
//			iopt=new OpTypes();
//			Add.addTypes(otSame(1),otUpperCastNumber(1,2),otSame(1));
		Sub = newBinaryOperator(opAddPriority, KString.from("-"), KString.from("opSub"),Instr.op_sub,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			Sub.addTypes(otSame(1),otUpperCastNumber(1,2),otSame(1));

		Mul = newBinaryOperator(opMulPriority, KString.from("*"), KString.from("opMul"),Instr.op_mul,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			Mul.addTypes(otSame(1),otUpperCastNumber(1,2),otSame(1));
		Div = newBinaryOperator(opMulPriority, KString.from("/"), KString.from("opDiv"),Instr.op_div,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			Div.addTypes(otSame(1),otUpperCastNumber(1,2),otSame(1));
		Mod = newBinaryOperator(opMulPriority, KString.from("%"), KString.from("opMod"),Instr.op_rem,orderAndArityNames[YFX],true);
//			iopt=new OpTypes();
//			Mod.addTypes(otSame(1),otUpperCastNumber(1,2),otSame(1));

		DotAccess = newBinaryOperator(opAccessPriority, KString.from("."), KString.from("opDotAccess"),null,orderAndArityNames[YFX],true);
		ElemAccess = newBinaryOperator(opAccessPriority, KString.from("[]"), KString.from("opElemAccess"),null,orderAndArityNames[YFX],true);
	}

	public boolean is_boolean_op;

	protected BinaryOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static BinaryOperator newBinaryOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		BinaryOperator op = hash.get(img);
		if( op != null ) {
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new BinaryOperator(pr,img,nm,in,oa,std);
	}

	public static BinaryOperator getOperator(KString im) {
		return hash.get(im);
	}

}

public class MultiOperator extends Operator {

	public static Hashtable<KString,MultiOperator>	hash = new Hashtable<KString,MultiOperator>();

	// Binary operators
	public static final MultiOperator Conditional;

	static {
		Conditional = newMultiOperator(opConditionalPriority, new KString[]{KString.from("?"),KString.from(":")}, KString.from("opChoice"),true);
			iopt=new OpTypes();
			Conditional.addTypes(otDownCast(2,3),otBoolean(),otAny(),otAny());
	}

	public KString[]	images;

	protected MultiOperator(int pr, KString[] img, KString nm, boolean std) {
		super(pr,img[0],nm,null,orderAndArityNames[XFXFY],std);
		images = img;
		hash.put(img[0],this);
	}

	public static MultiOperator newMultiOperator(int pr, KString[] img, KString nm, boolean std) {
		MultiOperator op = hash.get(img[0]);
		if( op != null ) {
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

	public static MultiOperator getOperator(KString im) {
		return hash.get(im);
	}

}

public class PrefixOperator extends Operator {

	public static Hashtable<KString,PrefixOperator>	hash = new Hashtable<KString,PrefixOperator>();

	// Unary prefix operators
	public static final PrefixOperator Pos;
	public static final PrefixOperator Neg;
	public static final PrefixOperator PreIncr;
	public static final PrefixOperator PreDecr;
	public static final PrefixOperator BitNot;
	public static final PrefixOperator BooleanNot;

	static {
		Pos = newPrefixOperator(opNegPriority, KString.from("+"), KString.from("opPos"),Instr.op_nop,orderAndArityNames[FY],true);
//			iopt=new OpTypes();
//			Pos.addTypes(otSame(1),otNumber());
		Neg = newPrefixOperator(opNegPriority, KString.from("-"), KString.from("opNeg"),Instr.op_neg,orderAndArityNames[FY],true);
//			iopt=new OpTypes();
//			Neg.addTypes(otSame(1),otNumber());

		PreIncr = newPrefixOperator(opIncrPriority, KString.from("++"), KString.from("opPreIncr"),null,orderAndArityNames[FX],true);
//			iopt=new OpTypes();
//			PreIncr.addTypes(otSame(1),otInteger());
		PreDecr = newPrefixOperator(opIncrPriority, KString.from("--"), KString.from("opPreDecr"),null,orderAndArityNames[FX],true);
//			iopt=new OpTypes();
//			PreDecr.addTypes(otSame(1),otInteger());

		BitNot = newPrefixOperator(opBitNotPriority, KString.from("~"), KString.from("opBitNot"),null,orderAndArityNames[FY],true);
//			iopt=new OpTypes();
//			BitNot.addTypes(otSame(1),otInteger());
		BooleanNot = newPrefixOperator(opBooleanNotPriority, KString.from("!"), KString.from("opBooleanNot"),null,orderAndArityNames[FY],true);
//			iopt=new OpTypes();
//			BooleanNot.addTypes(otType(Type.tpBoolean),otBoolean());
	}

	protected PrefixOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static PrefixOperator newPrefixOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		PrefixOperator op = hash.get(img);
		if( op != null ) {
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new PrefixOperator(pr,img,nm,in,oa,std);
	}

	public static PrefixOperator getOperator(KString im) {
		return hash.get(im);
	}

}

public class PostfixOperator extends Operator {

	public static Hashtable<KString,PostfixOperator>	hash = new Hashtable<KString,PostfixOperator>();

	// Unary postfix operators
	public static final PostfixOperator PostIncr;
	public static final PostfixOperator PostDecr;

	static {
		PostIncr = newPostfixOperator(opIncrPriority, KString.from("++"), KString.from("opPostIncr"),null,orderAndArityNames[XF],true);
//			iopt=new OpTypes();
//			PostIncr.addTypes(otSame(1),otInteger());
		PostDecr = newPostfixOperator(opIncrPriority, KString.from("--"), KString.from("opPostDecr"),null,orderAndArityNames[XF],true);
//			iopt=new OpTypes();
//			PostDecr.addTypes(otSame(1),otInteger());
	}


	protected PostfixOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		super(pr,img,nm,in,oa,std);
		hash.put(img,this);
	}

	public static PostfixOperator newPostfixOperator(int pr, KString img, KString nm, Instr in, KString oa, boolean std) {
		PostfixOperator op = hash.get(img);
		if( op != null ) {
			// Verify priority, and instruction
			if( op.priority != pr || op.smode != oa ) {
				throw new RuntimeException("Wrong redeclaration of operator "+op+
					"\n\tshould be "+op.toDeclString()+" but found "+Operator.toDeclString(pr,oa,img));
			}
			return op;
		}
		return new PostfixOperator(pr,img,nm,in,oa,std);
	}

	public static PostfixOperator getOperator(KString im) {
		return hash.get(im);
	}

}

public class CastOperator extends Operator {

	public Type		type;
	public boolean  reinterp;

	public CastOperator(Type tp, boolean r) {
		super(opCastPriority,KString.Empty,KString.from("(cast)"),null,orderAndArityNames[FY],true);
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

