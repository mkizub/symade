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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/bytecode/Attribute.java,v 1.3.2.1.2.2 1999/05/29 21:03:05 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.2 $
 *
 */

public class Attribute implements BytecodeElement,BytecodeFileConstants,BytecodeAttributeNames {

	public static final Attribute[]	emptyArray = new Attribute[0];

	public static Hashtable<KString,Class>	attrMap;
	static {
		attrMap = new Hashtable<KString,Class>();
		attrMap.put(attrCode,			Class.forName("kiev.bytecode.CodeAttribute"));
		attrMap.put(attrSourceFile,		Class.forName("kiev.bytecode.SourceFileAttribute"));
		attrMap.put(attrLocalVarTable,	Class.forName("kiev.bytecode.LocalVariableTableAttribute"));
		attrMap.put(attrLinenoTable,	Class.forName("kiev.bytecode.LineNumberTableAttribute"));
		attrMap.put(attrExceptions,		Class.forName("kiev.bytecode.ExceptionsAttribute"));
		attrMap.put(attrInnerClasses,	Class.forName("kiev.bytecode.InnerClassesAttribute"));
		attrMap.put(attrConstantValue,	Class.forName("kiev.bytecode.ConstantValueAttribute"));
		attrMap.put(attrClassArguments,	Class.forName("kiev.bytecode.KievClassArgumentsAttribute"));
		attrMap.put(attrPizzaCase,		Class.forName("kiev.bytecode.KievCaseAttribute"));
		attrMap.put(attrKiev,			null /*Class.forName("kiev.bytecode.KievAttribute")*/);
		attrMap.put(attrFlags,			Class.forName("kiev.bytecode.KievFlagsAttribute"));
		attrMap.put(attrAlias,			Class.forName("kiev.bytecode.KievAliasAttribute"));
		attrMap.put(attrTypedef,		Class.forName("kiev.bytecode.KievTypedefAttribute"));
		attrMap.put(attrOperator,		Class.forName("kiev.bytecode.KievOperatorAttribute"));
		attrMap.put(attrImport,			Class.forName("kiev.bytecode.KievImportAttribute"));
		attrMap.put(attrEnum,			Class.forName("kiev.bytecode.KievEnumAttribute"));
		attrMap.put(attrRequire,		Class.forName("kiev.bytecode.KievContractAttribute"));
		attrMap.put(attrEnsure,			Class.forName("kiev.bytecode.KievContractAttribute"));
		attrMap.put(attrCheckFields,	Class.forName("kiev.bytecode.KievCheckFieldsAttribute"));
		attrMap.put(attrGenerations,	Class.forName("kiev.bytecode.KievGenerationsAttribute"));
		attrMap.put(attrPackedFields,	Class.forName("kiev.bytecode.KievPackedFieldsAttribute"));
		attrMap.put(attrPackerField,	Class.forName("kiev.bytecode.KievPackerFieldAttribute"));

		attrMap.put(attrRVAnnotations,		Class.forName("kiev.bytecode.RVAnnotations"));
		attrMap.put(attrRIAnnotations,		Class.forName("kiev.bytecode.RIAnnotations"));
		attrMap.put(attrRVParAnnotations,	Class.forName("kiev.bytecode.RVParAnnotations"));
		attrMap.put(attrRIParAnnotations,	Class.forName("kiev.bytecode.RIParAnnotations"));
		attrMap.put(attrAnnotationDefault,	Class.forName("kiev.bytecode.AnnotationDefault"));
	}

	public int					cp_name;
	public byte[]				data;

	public KString getName(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_name]).value;
	}
	public int size() {
		assert( data != null, "Null data in attribute "+getClass());
		return 6+data.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		// Name must already be read
		int len = cont.readInt();
		assert(cont.data.length-cont.offset >= len,"Too big length "+len+" specified for attribute");
		trace(Clazz.traceRead,cont.offset+": attribute length is "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		cont.offset += len;
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value
			+" len="+data.length);
		cont.writeShort(cp_name);
		cont.writeInt(data.length);
		cont.write(data);
	}

	public static Attribute[] readAttributes(ReadContext cont) {
		int num = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": number of attributes is "+num);
		if( num == 0 ) return Attribute.emptyArray;
		Attribute[] attrs = new Attribute[num];
		for(int i=0; i < num; i++) {
			int cp_name = cont.readShort();
			assert(cp_name > 0 && cp_name < cont.clazz.pool.length ,"Attribute name index "+cp_name+" out of range");
			assert(cont.clazz.pool[cp_name].constant_type == CONSTANT_UTF8 ,"Attribute name index dos not points to CONSTANT_UTF8");
			trace(Clazz.traceRead,cont.offset+": attribute name "+cp_name+" = "+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
			Class aclass = attrMap.get( ((Utf8PoolConstant)cont.clazz.pool[cp_name]).value );
			Attribute attr;
			if( aclass == null )
				attr = new Attribute();
			else
				attr = (Attribute)aclass.newInstance();
			attr.cp_name = cp_name;
			attr.read(cont);
			attrs[i] = attr;
		}
		return attrs;
	}
}

public class SourceFileAttribute extends Attribute {

	public int					cp_filename;

	public int size() {
		return 6+2;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 2,"Wrong attribute length "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		cp_filename = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": filename is "+getFileName(cont.clazz));
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		trace(Clazz.traceWrite,cont.offset+": filename is "+getFileName(cont.clazz));
		cont.writeShort(cp_name);
		cont.writeInt(2);
		cont.writeShort(cp_filename);
	}
	public KString getFileName(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_filename]).value;
	}
}

public class ConstantValueAttribute extends Attribute {

	public int					cp_value;

	public int size() {
		return 6+2;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 2,"Wrong attribute length "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		cp_value = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2);
		cont.writeShort(cp_value);
	}
	public Number getValue(Clazz clazz) {
		return ((NumberPoolConstant)clazz.pool[cp_value]).getValue();
	}
}

public class ExceptionsAttribute extends Attribute {

	public int[]					cp_exceptions;

	public int size() {
		return 6+2+2*cp_exceptions.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		cp_exceptions = new int[elen];
		for(int i=0; i < elen; i++)
			cp_exceptions[i] = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+cp_exceptions.length*2);
		cont.writeShort(cp_exceptions.length);
		for(int i=0; i < cp_exceptions.length; i++) {
			cont.writeShort(cp_exceptions[i]);
		}
	}
	public KString getException(int i, Clazz clazz) {
		ClazzPoolConstant cpc = (ClazzPoolConstant)clazz.pool[cp_exceptions[i]];
		return ((Utf8PoolConstant)clazz.pool[cpc.ref]).value;
	}
}

public class CodeAttribute extends Attribute implements JavaOpcodes {

	public int					max_stack;
	public int					max_locals;
	public byte[]				code;
	public int[]				catchers_start_pc;
	public int[]				catchers_end_pc;
	public int[]				catchers_handler_pc;
	public int[]				catchers_cp_signature;
	public Attribute[]			attrs;

	public int size() {
		int len = 6+2+2+4+code.length+2+catchers_start_pc.length*8+2;
		for(int i=0; i < attrs.length; i++)
			len += attrs[i].size();
		return len;
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		max_stack = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": max_stack is "+max_stack);
		max_locals = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": max_locals is "+max_locals);
		int codelen = cont.readInt();
		trace(Clazz.traceRead,cont.offset+": code length "+codelen);
		code = new byte[codelen];
		cont.read(code);
		int catchlen = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": there is "+catchlen+" catchers here");
		catchers_start_pc = new int[catchlen];
		catchers_end_pc = new int[catchlen];
		catchers_handler_pc = new int[catchlen];
		catchers_cp_signature = new int[catchlen];
		for(int i=0; i < catchlen; i++) {
			catchers_start_pc[i] = cont.readShort();
			catchers_end_pc[i] = cont.readShort();
			catchers_handler_pc[i] = cont.readShort();
			catchers_cp_signature[i] = cont.readShort();
			trace(Clazz.traceRead,cont.offset+": catcher "+i+":"+
				catchers_start_pc[i]+"-"+catchers_end_pc[i]+" -> "+catchers_handler_pc[i]+
				" for "+(catchers_cp_signature[i]==0 ? "<any>" :
					((Utf8PoolConstant)cont.clazz.pool[((ClazzPoolConstant)
						cont.clazz.pool[catchers_cp_signature[i]]).ref]).value.toString()) );
		}
		attrs = Attribute.readAttributes(cont);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(size()-6);
		trace(Clazz.traceWrite,cont.offset+": max_stack is "+max_stack);
		cont.writeShort(max_stack);
		trace(Clazz.traceWrite,cont.offset+": max_locals is "+max_locals);
		cont.writeShort(max_locals);
		trace(Clazz.traceWrite,cont.offset+": code length "+code.length);
		cont.writeInt(code.length);
		if(Clazz.traceWrite) trace_code(cont);
		cont.write(code);
		trace(Clazz.traceWrite,"\ttotal "+catchers_start_pc.length+" catchers");
		cont.writeShort(catchers_start_pc.length);
		for(int i=0; i < catchers_start_pc.length; i++) {
			trace(Clazz.traceWrite,"\t"+catchers_start_pc[i]+" - "
				+catchers_end_pc[i]+" -> "+catchers_handler_pc[i]+" "
				+(catchers_cp_signature[i]==0?
					" finally":refname(cont,catchers_cp_signature[i])));
			cont.writeShort(catchers_start_pc[i]);
			cont.writeShort(catchers_end_pc[i]);
			cont.writeShort(catchers_handler_pc[i]);
			cont.writeShort(catchers_cp_signature[i]);
		}
		cont.writeShort(attrs.length);
		for(int i=0; i < attrs.length; i++)
			attrs[i].write(cont);
	}
	public void trace_code(ReadContext cont) {
		int pc = 0;
		int cp;
		int l;
		while(pc < code.length) {
			int instr = 0xFF & code[pc];
			System.out.print(pc+":\t"+kiev.vlang.Constants.opcNames[instr]);
			switch(instr) {
			default:
				break;
			case opc_ldc:
				cp = 0xFF & code[pc+1];
				System.out.print(" "+cont.clazz.pool[cp]);
				break;
			case opc_ldc_w:
			case opc_ldc2_w:
				cp = ((0xFF & code[pc+1]) << 8)+ (0xFF & code[pc+2]);
				System.out.print(" "+cont.clazz.pool[cp]);
				break;
			case opc_new:
			case opc_anewarray:
			case opc_multianewrray:
			case opc_checkcast:
			case opc_instanceof:
			case opc_invokemethodref:
				cp = ((0xFF & code[pc+1]) << 8)+ (0xFF & code[pc+2]);
				System.out.print(" "+refname(cont,cp));
				break;
			case opc_newmethodref:
			case opc_getstatic:
			case opc_putstatic:
			case opc_getfield:
			case opc_putfield:
			case opc_invokevirtual:
			case opc_invokespecial:
			case opc_invokestatic:
			case opc_invokeinterface:
				cp = ((0xFF & code[pc+1]) << 8)+ (0xFF & code[pc+2]);
				System.out.print(" "+clnametype(cont,cp));
				break;
			case opc_ifeq:
			case opc_ifne:
			case opc_iflt:
			case opc_ifge:
			case opc_ifgt:
			case opc_ifle:
			case opc_if_icmpeq:
			case opc_if_icmpne:
			case opc_if_icmplt:
			case opc_if_icmpge:
			case opc_if_icmpgt:
			case opc_if_icmple:
			case opc_if_acmpeq:
			case opc_if_acmpne:
			case opc_goto:
			case opc_jsr:
				l = ((0xFF & code[pc+1]) << 8)+ (0xFF & code[pc+2]);
				System.out.print(" "+(l+pc));
				break;
			case opc_tableswitch:
				{
					int sw_pc = pc;
					pc++;
					while( (pc % 4) != 0 ) pc++;
					int def = ((0xFF & code[pc+3]))+
						((0xFF & code[pc+2]) << 8)+
						((0xFF & code[pc+1]) << 16)+
						((0xFF & code[pc]));
					pc += 4;
					int low = ((0xFF & code[pc+3]))+
						((0xFF & code[pc+2]) << 8)+
						((0xFF & code[pc+1]) << 16)+
						((0xFF & code[pc]));
					pc += 4;
					int high = ((0xFF & code[pc+3]))+
						((0xFF & code[pc+2]) << 8)+
						((0xFF & code[pc+1]) << 16)+
						((0xFF & code[pc]));
					pc += 4;
					System.out.println("");
					System.out.println("\t\tdefault:\t"+(def+sw_pc));
					for(int i=low; i <= high; i++) {
						int label = ((0xFF & code[pc+3]))+
							((0xFF & code[pc+2]) << 8)+
							((0xFF & code[pc+1]) << 16)+
							((0xFF & code[pc]));
						pc += 4;
						System.out.println("\t\tvalue "+i+":\t"+(label+sw_pc));
					}
				}
				continue;
			case opc_lookupswitch:
				{
					int sw_pc = pc;
					pc++;
					while( (pc % 4) != 0 ) pc++;
					int def = ((0xFF & code[pc+3]))+
						((0xFF & code[pc+2]) << 8)+
						((0xFF & code[pc+1]) << 16)+
						((0xFF & code[pc]));
					pc += 4;
					int size = ((0xFF & code[pc+3]))+
						((0xFF & code[pc+2]) << 8)+
						((0xFF & code[pc+1]) << 16)+
						((0xFF & code[pc]));
					pc += 4;
					System.out.println("");
					System.out.println("\t\tdefault:\t"+(def+sw_pc));
					for(int i=0; i < size; i++) {
						int val = ((0xFF & code[pc+3]))+
							((0xFF & code[pc+2]) << 8)+
							((0xFF & code[pc+1]) << 16)+
							((0xFF & code[pc]));
						pc += 4;
						int label = ((0xFF & code[pc+3]))+
							((0xFF & code[pc+2]) << 8)+
							((0xFF & code[pc+1]) << 16)+
							((0xFF & code[pc]));
						pc += 4;
						System.out.println("\t\tvalue "+val+":\t"+(label+sw_pc));
					}
				}
				continue;
			}
			System.out.println("");
			pc += kiev.vlang.Constants.opcLengths[instr];
		}
	}
	private Object utf8(ReadContext cont, int pos) {
		return ((Utf8PoolConstant)cont.clazz.pool[pos]).value;
	}
	private Object refname(ReadContext cont, int pos) {
		RefPoolConstant obj = (RefPoolConstant)cont.clazz.pool[pos];
		return ((Utf8PoolConstant)cont.clazz.pool[obj.ref]).value;
	}
	private Object clnametype(ReadContext cont, int pos) {
		ClazzNameTypePoolConstant obj = (ClazzNameTypePoolConstant)cont.clazz.pool[pos];
		NameAndTypePoolConstant nt = (NameAndTypePoolConstant)cont.clazz.pool[obj.ref_nametype];
		return refname(cont,obj.ref_clazz)+": "+utf8(cont,nt.ref_name)+": "+utf8(cont,nt.ref_type);
	}
}

public class LocalVariableTableAttribute extends Attribute {

	public int[]					start_pc;
	public int[]					length_pc;
	public int[]					cp_varname;
	public int[]					cp_signature;
	public int[]					slot;

	public int size() {
		return 6+2+10*start_pc.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		start_pc = new int[elen];
		length_pc = new int[elen];
		cp_varname = new int[elen];
		cp_signature = new int[elen];
		slot = new int[elen];
		trace(Clazz.traceRead,cont.offset+": there is "+elen+" vars in table");
		for(int i=0; i < elen; i++) {
			start_pc[i] = cont.readShort();
			length_pc[i] = cont.readShort();
			cp_varname[i] = cont.readShort();
			cp_signature[i] = cont.readShort();
			slot[i] = cont.readShort();
			trace(Clazz.traceRead,cont.offset+": var "+i+":"+
				start_pc[i]+"-"+(start_pc[i]+length_pc[i])+" in "+slot[i]+" slot "+
				" for "+((Utf8PoolConstant)cont.clazz.pool[cp_varname[i]]).value+
				" of type "+((Utf8PoolConstant)cont.clazz.pool[cp_varname[i]]).value);
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+start_pc.length*10);
		int elen = start_pc.length;
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			cont.writeShort(start_pc[i]);
			cont.writeShort(length_pc[i]);
			cont.writeShort(cp_varname[i]);
			cont.writeShort(cp_signature[i]);
			cont.writeShort(slot[i]);
		}
	}
}

public class InnerClassesAttribute extends Attribute {

	public int[]					cp_inners;
	public int[]					cp_outers;
	public int[]					cp_inner_names;
	public int[]					cp_inner_flags;

	public int size() {
		return 6+2+8*cp_inners.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		cp_inners = new int[elen];
		cp_outers = new int[elen];
		cp_inner_names = new int[elen];
		cp_inner_flags = new int[elen];
		for(int i=0; i < elen; i++) {
			cp_inners[i] = cont.readShort();
			cp_outers[i] = cont.readShort();
			cp_inner_names[i] = cont.readShort();
			cp_inner_flags[i] = cont.readShort();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+8*cp_inners.length);
		cont.writeShort(cp_inners.length);
		for(int i=0; i < cp_inners.length; i++) {
			cont.writeShort(cp_inners[i]);
			cont.writeShort(cp_outers[i]);
			cont.writeShort(cp_inner_names[i]);
			cont.writeShort(cp_inner_flags[i]);
		}
	}
	public KString getInnerName(int i, Clazz clazz) {
		ClazzPoolConstant cpc = (ClazzPoolConstant)clazz.pool[cp_inners[i]];
		return ((Utf8PoolConstant)clazz.pool[cpc.ref]).value;
	}
	public KString getOuterName(int i, Clazz clazz) {
		ClazzPoolConstant cpc = (ClazzPoolConstant)clazz.pool[cp_outers[i]];
		return ((Utf8PoolConstant)clazz.pool[cpc.ref]).value;
	}
}

public class LineNumberTableAttribute extends Attribute {

	public int[]					start_pc;
	public int[]					lineno;

	public int size() {
		return 6+2+4*start_pc.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		start_pc = new int[elen];
		lineno = new int[elen];
		for(int i=0; i < elen; i++) {
			start_pc[i] = cont.readShort();
			lineno[i] = cont.readShort();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+start_pc.length*4);
		int elen = start_pc.length;
		trace(Clazz.traceWrite,cont.offset+": total "+start_pc.length+" lines in the table");
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			trace(Clazz.traceWrite,cont.offset+": pc: "+start_pc[i]+" for line "+lineno[i]);
			cont.writeShort(start_pc[i]);
			cont.writeShort(lineno[i]);
		}
	}
}

public class KievClassArgumentsAttribute extends Attribute {

	public int[]					cp_argname;
	public int[]					cp_supername;
	public int[]					argno;

	public int size() {
		return 6+2+argno.length*6;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		cp_argname = new int[elen];
		cp_supername = new int[elen];
		argno = new int[elen];
		for(int i=0; i < elen; i++) {
			cp_argname[i] = cont.readShort();
			cp_supername[i] = cont.readShort();
			argno[i] = cont.readShort();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+argno.length*6);
		cont.writeShort(argno.length);
		for(int i=0; i < argno.length; i++) {
			cont.writeShort(cp_argname[i]);
			cont.writeShort(cp_supername[i]);
			cont.writeShort(argno[i]);
		}
	}
}

public class KievFlagsAttribute extends Attribute {

	public int					flags;

	public int size() {
		return 6+4;				// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 4,"Wrong attribute length "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		flags = cont.readInt();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute = 0x"+Integer.toHexString(flags));
		cont.writeShort(cp_name);
		cont.writeInt(4);
		cont.writeInt(flags);
	}
}

public class KievImportAttribute extends Attribute {

	public int					cp_ref;

	public int size() {
		return 6+2;				// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 2,"Wrong attribute length "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		cp_ref = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute clazz="+getClazzName(cont.clazz)
			+" name="+getNodeName(cont.clazz)+" sign="+getSignature(cont.clazz));
		cont.writeShort(cp_name);
		cont.writeInt(2);
		cont.writeShort(cp_ref);
	}
	public KString getClazzName(Clazz clazz) {
		ClazzNameTypePoolConstant cpc = (ClazzNameTypePoolConstant)clazz.pool[cp_ref];
		ClazzPoolConstant clpc = (ClazzPoolConstant)clazz.pool[cpc.ref_clazz];
		return ((Utf8PoolConstant)clazz.pool[clpc.ref]).value;
	}
	public KString getNodeName(Clazz clazz) {
		ClazzNameTypePoolConstant cpc = (ClazzNameTypePoolConstant)clazz.pool[cp_ref];
		NameAndTypePoolConstant ntpc = (NameAndTypePoolConstant)clazz.pool[cpc.ref_nametype];
		return ((Utf8PoolConstant)clazz.pool[ntpc.ref_name]).value;
	}
	public KString getSignature(Clazz clazz) {
		ClazzNameTypePoolConstant cpc = (ClazzNameTypePoolConstant)clazz.pool[cp_ref];
		NameAndTypePoolConstant ntpc = (NameAndTypePoolConstant)clazz.pool[cpc.ref_nametype];
		return ((Utf8PoolConstant)clazz.pool[ntpc.ref_type]).value;
	}
}

public class KievAliasAttribute extends Attribute {

	public int[]				cp_alias;

	public int size() {
		return 6+2*cp_alias.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		cp_alias = new int[len/2];
		for(int i=0; i < cp_alias.length; i++)
			cp_alias[i] = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(cp_alias.length*2);
		for(int i=0; i < cp_alias.length; i++) {
			trace(Clazz.traceWrite,cont.offset+": alias "+getAlias(i,cont.clazz));
			cont.writeShort(cp_alias[i]);
		}
	}
	public KString getAlias(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_alias[i]]).value;
	}
}

public class KievTypedefAttribute extends Attribute {

	public int					cp_type;
	public int					cp_tpnm;

	public int size() {
		return 6+4;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		cp_type = cont.readShort();
		cp_tpnm = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(4);
		cont.writeShort(cp_type);
		cont.writeShort(cp_tpnm);
	}
	public KString getType(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_type]).value;
	}
	public KString getTypeName(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_tpnm]).value;
	}
}

public class KievOperatorAttribute extends Attribute {

	public int					priority;
	public int					cp_optype;
	public int					cp_image;

	public int size() {
		return 6+6;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 6,"Wrong attribute length "+len);
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		priority = cont.readShort();
		cp_optype = cont.readShort();
		cp_image = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(6);
		cont.writeShort(priority);
		cont.writeShort(cp_optype);
		cont.writeShort(cp_image);
	}
	public KString getOpType(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_optype]).value;
	}
	public KString getImage(Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[cp_image]).value;
	}
}

public class KievCaseAttribute extends Attribute {

	public int					caseno;
	public int[]				cp_casefields;

	public int size() {
		return 6+4+cp_casefields.length*2;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		caseno = cont.readShort();
		int cflen = cont.readShort();
		cp_casefields = new int[cflen];
		for(int i=0; i < cflen; i++)
			cp_casefields[i] = cont.readShort();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+cp_casefields.length*2);
		cont.writeShort(caseno);
		cont.writeShort(cp_casefields.length);
		for(int i=0; i < cp_casefields.length; i++)
			cont.writeShort(cp_casefields[i]);
	}
}

public class KievEnumAttribute extends Attribute {

	public int[]					fields;
	public int[]					values;

	public int size() {
		return 6+2+6*fields.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		fields = new int[elen];
		values = new int[elen];
		for(int i=0; i < elen; i++) {
			fields[i] = cont.readShort();
			values[i] = cont.readInt();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+fields.length*6);
		int elen = fields.length;
		trace(Clazz.traceWrite,cont.offset+": total "+elen+" enum fields");
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			trace(Clazz.traceWrite,cont.offset+": field "+getFieldName(i,cont.clazz)+" has value "+values[i]);
			cont.writeShort(fields[i]);
			cont.writeInt(values[i]);
		}
	}
	public KString getFieldName(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[fields[i]]).value;
	}
}

public class KievContractAttribute extends Attribute {

	public int					max_stack;
	public int					max_locals;
	public byte[]				code;
	public Attribute[]			attrs;

	public int size() {
		int len = 6+2+2+4+code.length+2+2;
		for(int i=0; i < attrs.length; i++)
			len += attrs[i].size();
		return len;
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		max_stack = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": max_stack is "+max_stack);
		max_locals = cont.readShort();
		trace(Clazz.traceRead,cont.offset+": max_locals is "+max_locals);
		int codelen = cont.readInt();
		trace(Clazz.traceRead,cont.offset+": code length "+codelen);
		code = new byte[codelen];
		cont.read(code);
		int catchlen = cont.readShort();
		assert( catchlen == 0 , "Contract attribute catchers length != 0");
		attrs = Attribute.readAttributes(cont);
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(size()-6);
		cont.writeShort(max_stack);
		cont.writeShort(max_locals);
		cont.writeInt(code.length);
		cont.write(code);
		cont.writeShort(0);
		cont.writeShort(attrs.length);
		for(int i=0; i < attrs.length; i++)
			attrs[i].write(cont);
	}
}

public class KievCheckFieldsAttribute extends Attribute {

	public int[]					fields;

	public int size() {
		return 6+2+2*fields.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		fields = new int[elen];
		for(int i=0; i < elen; i++) {
			fields[i] = cont.readShort();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+fields.length*2);
		int elen = fields.length;
		trace(Clazz.traceWrite,cont.offset+": total "+elen+" checked fields");
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			trace(Clazz.traceWrite,cont.offset+": field "+getFieldName(i,cont.clazz));
			cont.writeShort(fields[i]);
		}
	}
	public KString getFieldName(int i, Clazz clazz) {
		FieldPoolConstant fpc = (FieldPoolConstant)clazz.pool[fields[i]];
		NameAndTypePoolConstant ntpc = (NameAndTypePoolConstant)clazz.pool[fpc.ref_nametype];
		return ((Utf8PoolConstant)clazz.pool[ntpc.ref_name]).value;
	}
	public KString getFieldClass(int i, Clazz clazz) {
		FieldPoolConstant fpc = (FieldPoolConstant)clazz.pool[fields[i]];
		ClazzPoolConstant cpc = (ClazzPoolConstant)clazz.pool[fpc.ref_clazz];
		return ((Utf8PoolConstant)clazz.pool[cpc.ref]).value;
	}
}

public class KievGenerationsAttribute extends Attribute {

	public int[]					gens;

	public int size() {
		return 6+2+2*gens.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		gens = new int[elen];
		for(int i=0; i < elen; i++) {
			gens[i] = cont.readShort();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+gens.length*2);
		int elen = gens.length;
		trace(Clazz.traceWrite,cont.offset+": total "+elen+" generated types");
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			trace(Clazz.traceWrite,cont.offset+": gen.class "+getGenName(i,cont.clazz));
			cont.writeShort(gens[i]);
		}
	}
	public KString getGenName(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[gens[i]]).value;
	}
}

public class KievPackedFieldsAttribute extends Attribute {

	public int[]					fields;
	public int[]					signatures;
	public int[]					packers;
	public int[]					sizes;
	public int[]					offsets;

	public int size() {
		return 6+2+8*fields.length;	// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		fields = new int[elen];
		signatures = new int[elen];
		packers = new int[elen];
		sizes = new int[elen];
		offsets = new int[elen];
		for(int i=0; i < elen; i++) {
			fields[i] = cont.readShort();
			signatures[i] = cont.readShort();
			packers[i] = cont.readShort();
			sizes[i] = cont.readByte();
			offsets[i] = cont.readByte();
		}
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": attribute"
			+" ref_name="+cp_name+", name="+((Utf8PoolConstant)cont.clazz.pool[cp_name]).value);
		cont.writeShort(cp_name);
		cont.writeInt(2+8*fields.length);
		int elen = fields.length;
		trace(Clazz.traceWrite,cont.offset+": total "+elen+" packed fields");
		cont.writeShort(elen);
		for(int i=0; i < elen; i++) {
			trace(Clazz.traceWrite,cont.offset+": name="+getFieldName(i,cont.clazz)
				+" sig="+getSignature(i,cont.clazz)
				+" packer="+getPackerName(i,cont.clazz)
				+" size="+sizes[i]+" offset="+offsets[i]
				);
			cont.writeShort(fields[i]);
			cont.writeShort(signatures[i]);
			cont.writeShort(packers[i]);
			cont.writeByte(sizes[i]);
			cont.writeByte(offsets[i]);
		}
	}
	public KString getFieldName(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[fields[i]]).value;
	}
	public KString getSignature(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[signatures[i]]).value;
	}
	public KString getPackerName(int i, Clazz clazz) {
		return ((Utf8PoolConstant)clazz.pool[packers[i]]).value;
	}
}

public class KievPackerFieldAttribute extends Attribute {

	public int					size;

	public int size() {
		return 6+4;				// name+size(int)+data.length
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		assert(len == 4,"Wrong attribute length "+len);
		size = cont.readInt();
	}
	public void write(ReadContext cont) {
		trace(Clazz.traceWrite,cont.offset+": size = 0x"+Integer.toHexString(size));
		cont.writeShort(cp_name);
		cont.writeInt(4);
		cont.writeInt(size);
	}
}

public abstract class Annotation extends Attribute {

	public static abstract class element_value {
		// 'B', 'C', 'D', 'F', 'I', 'J', 'S', and 'Z' indicate a primitive type.
		// 's' String
		// 'e' java enum
		// 'c' java class
		// '@' annotation type
		// '[' array
		public byte tag;
		public abstract int size();
		public abstract void read(ReadContext cont);
		public abstract void write(ReadContext cont);
		
		public static element_value Read(ReadContext cont) {
			byte tag  = cont.readByte();
			element_value v;
			switch (tag) {
			case 'B': case 'C': case 'D': case 'F':
			case 'I': case 'J': case 'S': case 'Z': case 's':
				v = new element_value_const();
				break;
			case 'e': v = new element_value_enum_const(); break;
			case 'c': v = new element_value_class_info(); break;
			case '@': v = new element_value_annotation(); break;
			case '[': v = new element_value_array();      break;
			default:
				throw new ClassFormatError("unknow annotation value tag: "+(char)tag);
			}
			v.tag = tag;
			v.read(cont);
			return v;
		}
		
	}
	public static class element_value_const extends element_value {
		public int  const_value_index;
		public int size() { return 1+2; }
		public void read(ReadContext cont) {
			const_value_index = cont.readShort();
		}
		public void write(ReadContext cont) {
			cont.writeShort(const_value_index);
		}
	}
	public static class element_value_enum_const extends element_value {
		public int  type_name_index;
		public int  const_name_index;
		public int size() { return 1+2+2; }
		public void read(ReadContext cont) {
			type_name_index = cont.readShort();
			const_name_index = cont.readShort();
		}
		public void write(ReadContext cont) {
			cont.writeShort(type_name_index);
			cont.writeShort(const_name_index);
		}
	}
	public static class element_value_class_info extends element_value {
		public int  class_info_index;
		public int size() { return 1+2; }
		public void read(ReadContext cont) {
			class_info_index = cont.readShort();
		}
		public void write(ReadContext cont) {
			cont.writeShort(class_info_index);
		}
	}
	public static class element_value_annotation extends element_value {
		public annotation annotation_value;
		public int size() { return 1+annotation_value.size(); }
		public void read(ReadContext cont) {
			annotation_value = new annotation();
			annotation_value.read(cont);
		}
		public void write(ReadContext cont) {
			annotation_value.write(cont);
		}
	}
	public static class element_value_array extends element_value {
		public element_value[] values;
		public int size() {
			int sz = 1+2;
			foreach (element_value p; values)
				sz += p.size(); 
			return sz;
		}
		public void read(ReadContext cont) {
			int elen = cont.readShort();
			values = new element_value[elen];
			for(int i=0; i < elen; i++)
				values[i] = element_value.Read(cont);
		}
		public void write(ReadContext cont) {
			cont.writeShort(values.length);
			for(int i=0; i < values.length; i++) {
				cont.writeByte(values[i].tag);
				values[i].write(cont);
			}
		}
	}
	public static class annotation {
		public int             type_index;
		public int[]           names;
		public element_value[] values;
		public int size() {
			int sz = 4;
			foreach (element_value p; values)
				sz += 2+p.size(); 
			return sz;
		}
		public void read(ReadContext cont) {
			type_index = cont.readShort();
			int elen = cont.readShort();
			names  = new int[elen];
			values = new element_value[elen];
			for(int i=0; i < elen; i++) {
				names[i]  = cont.readShort();
				values[i] = element_value.Read(cont);
			}
		}
		public void write(ReadContext cont) {
			cont.writeShort(type_index);
			cont.writeShort(names.length);
			for(int i=0; i < names.length; i++) {
				cont.writeShort(names[i]);
				cont.writeByte(values[i].tag);
				values[i].write(cont);
			}
		}
	}

}

public abstract class Annotations extends Annotation {
	public annotation[]	annotations;
	
	public int size() {
		int sz = 6+2;
		foreach (annotation a; annotations)
			sz += a.size();
		return sz;
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int elen = cont.readShort();
		annotations = new annotation[elen];
		for(int i=0; i < elen; i++) {
			annotations[i] = new annotation();
			annotations[i].read(cont);
		}
	}
	public void write(ReadContext cont) {
		cont.writeShort(cp_name);
		cont.writeInt(size()-6);
		cont.writeShort(annotations.length);
		for(int i=0; i < annotations.length; i++) {
			annotations[i].write(cont);
		}
	}
}
public class RVAnnotations extends Annotations {
}

public class RIAnnotations extends Annotations {
}

public abstract class ParAnnotations extends Annotation {
	public annotation[][]	annotations;
	
	public int size() {
		int sz = 6+1+2*annotations.length;
		foreach (annotation[] aa; annotations) {
			foreach (annotation a; aa) {
				sz += a.size();
			}
		}
		return sz;
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		int npar = cont.readByte();
		annotations = new annotation[npar][];
		for(int p=0; p < npar; p++) {
			int elen = cont.readShort();
			annotations[p] = new annotation[elen];
			for(int i=0; i < elen; i++) {
				annotations[p][i] = new annotation();
				annotations[p][i].read(cont);
			}
		}
	}
	public void write(ReadContext cont) {
		cont.writeShort(cp_name);
		cont.writeInt(size()-6);
		cont.writeByte(annotations.length);
		for(int p=0; p < annotations.length; p++) {
			for(int i=0; i < annotations[p].length; i++) {
				cont.writeShort(annotations[p].length);
				annotations[p][i].write(cont);
			}
		}
	}
}

public class RVParAnnotations extends ParAnnotations {
}

public class RIParAnnotations extends ParAnnotations {
}

public class AnnotationDefault extends Annotation {
	public element_value value;

	public int size() {
		return 6+value.size();
	}
	public void read(ReadContext cont) {
		int len = cont.readInt();
		data = new byte[len];
		System.arraycopy(cont.data,cont.offset,data,0,len);
		value = element_value.Read(cont);
	}
	public void write(ReadContext cont) {
		cont.writeShort(cp_name);
		cont.writeInt(size()-6);
		cont.writeByte(value.tag);
		value.write(cont);
	}
}


