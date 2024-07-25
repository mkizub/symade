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
package kiev.bytecode;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

public class Method implements BytecodeElement,BytecodeFileConstants {
	public static final Method[]	emptyArray = new Method[0];

	public int					flags;
	public Utf8PoolConstant		cp_name;
	public Utf8PoolConstant		cp_type;
	public Attribute[]			attrs;
	public int					start_pos;

	public String getName(Clazz clazz) {
		return cp_name.value;
	}

	public String getSignature(Clazz clazz) {
		return cp_type.value;
	}

	public String getMethodSignature(Clazz clazz) {
		foreach (GenericsSignatureAttribute a; attrs)
			return a.getSignature(clazz);
		return null;
	}

	public int size(int offset) {
		int size = 8;	// flags+name+type+attrs.length
		for(int i=0; i < attrs.length; i++) {
			assert( attrs[i] != null, "Attribute "+i+" is null");
			attrs[i].start_pos = size+offset;
			size += attrs[i].size(size+offset);
		}
		return size;
	}
	public void read(ReadContext cont) {
		int idx;
		
		flags = cont.readShort();
		
		idx = cont.readShort();
		assert(idx > 0 && idx < cont.clazz.pool.length ,"Method name index "+idx+" out of range");
		assert(cont.clazz.pool[idx].constant_type() == CONSTANT_UTF8 ,"Method name index dos not points to CONSTANT_UTF8");
		cp_name = (Utf8PoolConstant)cont.clazz.pool[idx];
		trace(Clazz.traceRead,cont.offset+": method name "+idx+" = "+cp_name.value);
		
		idx = cont.readShort();
		assert(idx > 0 && idx < cont.clazz.pool.length ,"Method signature index "+cp_type+" out of range");
		assert(cont.clazz.pool[idx].constant_type() == CONSTANT_UTF8 ,"Method signature index dos not points to CONSTANT_UTF8");
		cp_type = (Utf8PoolConstant)cont.clazz.pool[idx];
		trace(Clazz.traceRead,cont.offset+": method signature "+idx+" = "+cp_type.value);
		
		attrs = Attribute.readAttributes(cont);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": method flags=0x"+Integer.toHexString(flags)
			+" ref_name="+cp_name.idx+", name="+cp_name.value
			+" ref_type="+cp_type.idx+", signature="+cp_type.value);
		assert(start_pos == 0 || start_pos == cont.offset);
		cont.writeShort(flags);
		cont.writeShort(cp_name.idx);
		cont.writeShort(cp_type.idx);
		trace(Clazz.traceWrite,cont.offset+": number of attrs is "+attrs.length);
		cont.writeShort(attrs.length);
		for(int i=0; i < attrs.length; i++) {
			trace(Clazz.traceWrite,cont.offset+": writing method attribute "+attrs[i].getName(cont.clazz));
			attrs[i].write(cont);
		}
	}
}

