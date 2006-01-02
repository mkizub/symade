/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.

 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.bytecode;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Method implements BytecodeElement,BytecodeFileConstants {
	public static final Method[]	emptyArray = new Method[0];

	public int					flags;
	public Utf8PoolConstant		cp_name;
	public Utf8PoolConstant		cp_type;
	public Attribute[]			attrs;

	public KString getName(Clazz clazz) {
		return cp_name.value;
	}

	public KString getSignature(Clazz clazz) {
		return cp_type.value;
	}

	public int size() {
		int size = 8;	// flags+name+type+attrs.length
		for(int i=0; i < attrs.length; i++) {
			assert( attrs[i] != null, "Attribute "+i+" is null");
			size += attrs[i].size();
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

