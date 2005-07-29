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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/bytecode/KievAttributeHandler.java,v 1.2 1998/10/21 19:44:17 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public class KievAttributeHandler implements BytecodeFileConstants,AttributeHandler {
	@virtual
	public virtual KString		aname = KString.from("kiev.Kiev");

	@getter public KString get$aname() { return aname; }
	@setter public void set$aname(KString n) { aname = n; }

	public int		getPriority() { return PreprocessStage-2; }

	public void processClazz(Clazz clazz) {
		KievAttribute ka = null;
		for(int i=0; i < clazz.attrs.length; i++) {
			if( ((Utf8PoolConstant)clazz.pool[clazz.attrs[i].cp_name]).value == aname ) {
				ka = new KievAttribute(clazz,clazz.attrs[i]);
				clazz.attrs[i] = ka;
				break;
			}
		}
		if( ka == null ) return;
/*
		clazz.pool = ka.clazz.pool;
		trace(Clazz.traceRules && clazz.cp_clazz != ka.clazz.cp_clazz, "Replace clazz name "
			+((Utf8PoolConstant)clazz.pool[((ClazzPoolConstant)clazz.pool[clazz.cp_clazz]).ref]).value
			+" -> "+((Utf8PoolConstant)clazz.pool[((ClazzPoolConstant)clazz.pool[ka.clazz.cp_clazz]).ref]).value);
		clazz.cp_clazz = ka.clazz.cp_clazz;
		trace(Clazz.traceRules && clazz.cp_super_clazz != ka.clazz.cp_super_clazz, "Replace super clazz name "
			+((Utf8PoolConstant)clazz.pool[((ClazzPoolConstant)clazz.pool[clazz.cp_super_clazz]).ref]).value
			+" -> "+((Utf8PoolConstant)clazz.pool[((ClazzPoolConstant)clazz.pool[ka.clazz.cp_super_clazz]).ref]).value);
		clazz.cp_super_clazz = ka.clazz.cp_super_clazz;
		clazz.cp_interfaces = ka.clazz.cp_interfaces;
		int len = clazz.fields.length;
		for(int i=0; i < len; i++) {
			trace(Clazz.traceRules && clazz.fields[i].cp_type != ka.clazz.fields[i].cp_type, "Replace field's "
				+((Utf8PoolConstant)clazz.pool[clazz.fields[i].cp_name]).value
				+" signature "
				+((Utf8PoolConstant)clazz.pool[clazz.fields[i].cp_type]).value
				+" -> "+((Utf8PoolConstant)clazz.pool[ka.clazz.fields[i].cp_type]).value);
			clazz.fields[i].cp_name = ka.clazz.fields[i].cp_name;
			clazz.fields[i].cp_type = ka.clazz.fields[i].cp_type;
		}
		len = clazz.methods.length;
		for(int i=0; i < len; i++) {
			trace(Clazz.traceRules && clazz.methods[i].cp_type != ka.clazz.methods[i].cp_type, "Replace method's "
				+((Utf8PoolConstant)clazz.pool[clazz.methods[i].cp_name]).value
				+" signature "
				+((Utf8PoolConstant)clazz.pool[clazz.methods[i].cp_type]).value
				+" -> "+((Utf8PoolConstant)clazz.pool[ka.clazz.methods[i].cp_type]).value);
			clazz.methods[i].cp_name = ka.clazz.methods[i].cp_name;
			clazz.methods[i].cp_type = ka.clazz.methods[i].cp_type;
		}
*/
	}

}

public class KievAttribute extends Attribute {

	public KievAttributeClazz		clazz;

	private KievAttribute(Attribute a) {
		this.cp_name = a.cp_name;
		this.data = a.data;
	}

	public static KievAttribute newKievAttribute(Clazz inclazz, Attribute a)
		alias operator(240,lfy,new)
	{
		if( a instanceof KievAttribute ) return (KievAttribute)a;
		KievAttribute ka = new KievAttribute(a);
		ka.clazz = new KievAttributeClazz(inclazz);
		ka.clazz.readClazz(ka.data);
		return ka;
	}

	public int size() {
		return 6+data.length;				// name+size(int)+data.length
	}
}

public class KievAttributeClazz extends Clazz implements BytecodeElement,BytecodeFileConstants {
	public Clazz	inclazz;
	public int		pool_offset;

	public KievAttributeClazz(Clazz inclazz) {
		this.inclazz = inclazz;
	}

	public void readConstantPool(ReadContext cont) {
		pool_offset = inclazz.pool.length;
		pool = PoolConstant.readKievConstantPool(cont,inclazz.pool);
	}

	public void writeConstantPool(ReadContext cont) {
		PoolConstant.writeKievConstantPool(cont,pool,pool_offset);
	}

	public int poolSize() {
		int size = 2;
		int len = pool.length;
		for(int i=pool_offset; i < len; i++) {
			assert( pool[i] != null, "PoolConstant "+i+" is null" );
			size += pool[i].size();
		}
		return size;
	}

}
