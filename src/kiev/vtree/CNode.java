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
package kiev.vtree;

import syntax kiev.Syntax;

@ThisIsANode(lang=CoreLang)
public abstract class CNode extends ANode {

	public static ANode toNode(Class clazz, Object value) {
		if (clazz == Boolean.class   || clazz == Boolean.TYPE)			return new CBoolNode((Boolean)value);
		if (clazz == Character.class || clazz == Character.TYPE)		return new CCharNode((Character)value);
		if (clazz == Byte.class      || clazz == Byte.TYPE)				return new CByteNode((Byte)value);
		if (clazz == Short.class     || clazz == Short.TYPE)			return new CShortNode((Short)value);
		if (clazz == Integer.class   || clazz == Integer.TYPE)			return new CIntNode((Integer)value);
		if (clazz == Long.class      || clazz == Long.TYPE)				return new CLongNode((Long)value);
		if (clazz == Float.class     || clazz == Float.TYPE)			return new CFloatNode((Float)value);
		if (clazz == Double.class    || clazz == Double.TYPE)			return new CDoubleNode((Double)value);
		if (clazz == String.class)										return new CStringNode((String)value);
		return new CObjectNode(value);
	}
	
	public abstract Object getValue();
}

@ThisIsANode(lang=CoreLang)
public final class CBoolNode extends CNode {
	public final boolean value;

	public CBoolNode(boolean value) { this.value = value; }
	public CBoolNode(Boolean value) { this.value = value.booleanValue(); }

	public Boolean getValue() { return Boolean.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CBoolNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CCharNode extends CNode {
	public final char value;

	public CCharNode(char value) { this.value = value; }
	public CCharNode(Character value) { this.value = value.charValue(); }

	public Character getValue() { return Character.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CCharNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CByteNode extends CNode {
	public final byte value;

	public CByteNode(byte value) { this.value = value; }
	public CByteNode(Byte value) { this.value = value.byteValue(); }

	public Byte getValue() { return Byte.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CByteNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CShortNode extends CNode {
	public final short value;

	public CShortNode(short value) { this.value = value; }
	public CShortNode(Short value) { this.value = value.shortValue(); }

	public Short getValue() { return Short.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CShortNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CIntNode extends CNode {
	public final int value;

	public CIntNode(int value) { this.value = value; }
	public CIntNode(Integer value) { this.value = value.intValue(); }

	public Integer getValue() { return Integer.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CIntNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CLongNode extends CNode {
	public final long value;

	public CLongNode(long value) { this.value = value; }
	public CLongNode(Long value) { this.value = value.longValue(); }

	public Long getValue() { return Long.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CLongNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CFloatNode extends CNode {
	public final float value;

	public CFloatNode(float value) { this.value = value; }
	public CFloatNode(Float value) { this.value = value.floatValue(); }

	public Float getValue() { return Float.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CFloatNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CDoubleNode extends CNode {
	public final double value;

	public CDoubleNode(double value) { this.value = value; }
	public CDoubleNode(Double value) { this.value = value.doubleValue(); }

	public Object getValue() { return Double.valueOf(this.value); }
	
	public Object copy(CopyContext cc) {
		return new CDoubleNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CNullNode extends CNode {
	public CNullNode() {}

	public Object getValue() { return null; }
	
	public Object copy(CopyContext cc) {
		return new CNullNode();
	}
}

@ThisIsANode(lang=CoreLang)
public final class CStringNode extends CNode {
	public final String value;

	public CStringNode(String value) { this.value = value; }

	public String getValue() { return this.value; }
	
	public Object copy(CopyContext cc) {
		return new CStringNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CObjectNode extends CNode {
	public final Object value;

	public CObjectNode(Object value) { this.value = value; }

	public Object getValue() { return this.value; }
	
	public Object copy(CopyContext cc) {
		return new CObjectNode(this.value);
	}
}

@ThisIsANode(lang=CoreLang)
public final class CSpaceNode<+N extends INode> extends CNode {
	public final N[] value;

	public CSpaceNode(N[] value) { this.value = value; }

	public N[] getValue() { return this.value; }
	
	public Object copy(CopyContext cc) {
		AttrSlot slot = this.pslot();
		ANode parent = this.parent();
		if (parent != null && slot.isAttr()) {
			N[] narr = (N[])value.clone();
			for (int x=0; x < narr.length; x++) {
				INode n = (INode)this.value[x].copy(cc);
				narr[x] = n;
				parent.callbackDataSet(slot, null, n, x);
			}
			return new CSpaceNode<N>(narr);
		}
		return new CSpaceNode<N>(this.value);
	}
}

