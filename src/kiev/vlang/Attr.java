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
import kiev.stdlib.*;

import java.io.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Attr.java,v 1.4.2.1.2.2 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.2 $
 *
 */

public class Attr implements Constants {
	public static Attr[] emptyArray = new Attr[0];

	/** Name of the attribute */
	public KString		name;
	@virtual
	public abstract virtual boolean isKiev;

	@getter public boolean get$isKiev() { return false; }
	@setter public void set$isKiev(boolean b) { return; }

	protected Attr(KString name) {
		this.name = name;
	}

	public kiev.bytecode.Attribute write() {
//		kiev.bytecode.Attribute a = new kiev.bytecode.Attribute();
//		a.cp_name = ConstPool.getAsciiCP(name).pos;
//		a.data = new byte[0];
//		return a;
		throw new RuntimeException("Unknown attribute generation: "+name);
	}
	public void generate() {
		ConstPool.addAsciiCP(name);
	}
}

public class CodeAttr extends Attr {

	public Method	method;
	public int		max_stack;
	public int		max_locals;
	CodeCatchInfo[]	catchers;
	Attr[]			code_attrs;
	public byte[]	code;
	public CP[]		constants;
	public int[]	constants_pc;

	public CodeAttr(Method m, int max_st,int max_locs, byte[] code,
			CodeCatchInfo[] catchers, Attr[] code_attrs) {
		super(attrCode);
		method = m;
		this.code = code;
		max_stack = max_st;
		max_locals = max_locs;
		this.catchers = catchers;
		this.code_attrs = code_attrs;
	}

	protected CodeAttr(KString nm, int max_st,int max_locs, byte[] code, Attr[] code_attrs) {
		super(nm);
		method = null;
		this.code = code;
		max_stack = max_st;
		max_locals = max_locs;
		this.catchers = null;
		this.code_attrs = code_attrs;
	}

	public Attr getAttr(KString name) {
		if( code_attrs != null )
			for(int i=0; i < code_attrs.length; i++)
				if( code_attrs[i].name.equals(name) )
					return code_attrs[i];
		return null;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; code_attrs!=null && i < code_attrs.length; i++)
			code_attrs[i].generate();
		for(int i=0; catchers!=null && i < catchers.length; i++) {
			if(catchers[i].type != null) {
				ClazzCP cl_cp = ConstPool.addClazzCP(catchers[i].type.java_signature);
				ConstPool.addAsciiCP(cl_cp.asc.value);
			}
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.CodeAttribute ca = new kiev.bytecode.CodeAttribute();
		ca.cp_name = ConstPool.getAsciiCP(name).pos;
		ca.max_stack = max_stack;
		ca.max_locals = max_locals;
		ca.code = code;
		int clen = catchers.length;
		ca.catchers_start_pc = new int[clen];
		ca.catchers_end_pc = new int[clen];
		ca.catchers_handler_pc = new int[clen];
		ca.catchers_cp_signature = new int[clen];
		for(int i=0; i < clen; i++) {
			ca.catchers_start_pc[i] = catchers[i].start_pc;
			ca.catchers_end_pc[i] = catchers[i].end_pc;
			ca.catchers_handler_pc[i] = catchers[i].handler.pc;
			if(catchers[i].type != null)
				ca.catchers_cp_signature[i] = ConstPool.getClazzCP(catchers[i].type.java_signature).pos;
		}
		ca.attrs = new kiev.bytecode.Attribute[code_attrs.length];
		for(int i=0; i < code_attrs.length; i++) {
			ca.attrs[i] = code_attrs[i].write();
		}
		return ca;
	}
}

public class SourceFileAttr extends Attr {

	/** File name */
	public KString		filename;

	/** Constructor for bytecode reader and raw field creation */
	public SourceFileAttr(KString filename) {
		super(attrSourceFile);
		this.filename = KString.from((new java.io.File(filename.toString())).getName());
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		ConstPool.addAsciiCP(filename);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.SourceFileAttribute sfa = new kiev.bytecode.SourceFileAttribute();
		sfa.cp_name = ConstPool.getAsciiCP(name).pos;
		sfa.cp_filename = ConstPool.getAsciiCP(filename).pos;
		return sfa;
	}
}

public class LocalVarTableAttr extends Attr {

	public CodeVar[]		vars;

	/** Constructor for bytecode reader and raw field creation */
	public LocalVarTableAttr() {
		super(attrLocalVarTable);
		vars = new CodeVar[0];
	}

	public void addVar(CodeVar var) {
		vars = (CodeVar[])Arrays.append(vars,var);
		var.index = vars.length-1;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < vars.length; i++) {
			Var v = vars[i].var;
			ConstPool.addAsciiCP(v.name.name);
			if( v.isNeedRefProxy() )
				ConstPool.addAsciiCP(Type.getProxyType(v.type).java_signature);
			else
				ConstPool.addAsciiCP(v.type.java_signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.LocalVariableTableAttribute lvta = new kiev.bytecode.LocalVariableTableAttribute();
		lvta.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = vars.length;
		lvta.start_pc = new int[len];
		lvta.length_pc = new int[len];
		lvta.cp_varname = new int[len];
		lvta.cp_signature = new int[len];
		lvta.slot = new int[len];
		for(int i=0; i < len; i++) {
			Var v = vars[i].var;
			KString sign;
			if( v.isNeedRefProxy() )
				sign = Type.getProxyType(v.type).java_signature;
			else
				sign = v.type.java_signature;

			lvta.start_pc[i] = vars[i].start_pc;
			lvta.length_pc[i] = vars[i].end_pc-vars[i].start_pc;
			lvta.cp_varname[i] = ConstPool.getAsciiCP(v.name.name).pos;
			lvta.cp_signature[i] = ConstPool.getAsciiCP(sign).pos;
			lvta.slot[i] = vars[i].stack_pos;
		}
		return lvta;
	}
}

public class LinenoTableAttr extends Attr {

	/** Line number table (see Code class for format description) */
	public int[]		table;

	/** Constructor for bytecode reader and raw field creation */
	public LinenoTableAttr() {
		super(attrLinenoTable);
		table = new int[0];
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.LineNumberTableAttribute lnta = new kiev.bytecode.LineNumberTableAttribute();
		lnta.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = table.length;
		lnta.start_pc = new int[len];
		lnta.lineno = new int[len];
		for(int i=0; i < len; i++) {
			lnta.start_pc[i] = table[i] >>> 16;
			lnta.lineno[i] = table[i] & 0xFFFF;
		}
		return lnta;
	}
}

public class ExceptionsAttr extends Attr {

	/** Line number table (see Code class for format description) */
	public Type[]		exceptions;

	/** Constructor for bytecode reader and raw field creation */
	public ExceptionsAttr() {
		super(attrExceptions);
		exceptions = new Type[0];
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < exceptions.length; i++)
			ConstPool.addClazzCP(exceptions[i].java_signature);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.ExceptionsAttribute ea = new kiev.bytecode.ExceptionsAttribute();
		ea.cp_name = ConstPool.getAsciiCP(name).pos;
		ea.cp_exceptions = new int[exceptions.length];
		for(int i=0; i < exceptions.length; i++)
			ea.cp_exceptions[i] = ConstPool.getClazzCP(exceptions[i].java_signature).pos;
		return ea;
	}
}


public class InnerClassesAttr extends Attr {

	/** Line number table (see Code class for format description) */
	public Struct[]		inner;
	public Struct[]		outer;
	public short[]		acc;

	/** Constructor for bytecode reader and raw field creation */
	public InnerClassesAttr() {
		super(attrInnerClasses);
		inner = new Struct[0];
		outer = new Struct[0];
		acc = new short[0];
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < inner.length; i++) {
			if( inner[i] != null) {
				ConstPool.addClazzCP(((Type)inner[i].type).java_signature);
			}
			if( outer[i] != null ) {
				ConstPool.addClazzCP(((Type)outer[i].type).java_signature);
			}
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.InnerClassesAttribute ica = new kiev.bytecode.InnerClassesAttribute();
		ica.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = inner.length;
		ica.cp_inners = new int[len];
		ica.cp_outers = new int[len];
		ica.cp_inner_names = new int[len];
		ica.cp_inner_flags = new int[len];
		for(int i=0; i < len; i++) {
			if( inner[i] != null ) {
				ica.cp_inners[i] = ConstPool.getClazzCP(((Type)inner[i].type).java_signature).pos;
			}
			if( outer[i] != null ) {
				ica.cp_outers[i] = ConstPool.getClazzCP(((Type)outer[i].type).java_signature).pos;
			}
			if( inner[i] != null ) {
				ica.cp_inner_names[i] = ConstPool.getClazzCP(((Type)inner[i].type).java_signature).asc.pos;
			}
			ica.cp_inner_flags[i] = acc[i];
		}
		return ica;
	}
}

public class ConstantValueAttr extends Attr {

	public Object		value;

	/** Constructor for bytecode reader and raw field creation */
	public ConstantValueAttr(ConstExpr val) {
		super(attrConstantValue);
		value = val.getConstValue();
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		if( value instanceof Number )
			ConstPool.addNumberCP((Number)value);
		else if( value instanceof Character )
			ConstPool.addNumberCP(Integer.valueOf((int)((Character)value).charValue()));
		else if( value instanceof KString )
			ConstPool.addStringCP((KString)value);
		else if( value instanceof Boolean ) {
			Integer i = Integer.valueOf(((Boolean)value).booleanValue()? 1: 0 );
			ConstPool.addNumberCP(i);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.ConstantValueAttribute cva = new kiev.bytecode.ConstantValueAttribute();
		cva.cp_name = ConstPool.getAsciiCP(name).pos;
		switch( value ) {
		case Boolean:
			cva.cp_value = ConstPool.getNumberCP(Integer.valueOf(((Boolean)value).booleanValue()? 1: 0 )).pos;
			break;
		case Character:
			cva.cp_value = ConstPool.getNumberCP(Integer.valueOf((int)((Character)value).charValue())).pos;
			break;
		case Number:
			cva.cp_value = ConstPool.getNumberCP((Number)value).pos;
			break;
		case KString:
			cva.cp_value = ConstPool.getStringCP((KString)value).pos;
			break;
		default:
			throw new RuntimeException("Bad type for ConstantValueAttr: "+value.getClass());
		}
		return cva;
	}
}

public class PizzaCaseAttr extends Attr {

	/** For pizza cases - case number (1 based),
		for outer class = 0
	*/
	public int				caseno = 0;

	/** Array of case fields defined in this case structure */
	public Field[]			casefields = new Field[0];

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public PizzaCaseAttr() {
		super(attrPizzaCase);
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < casefields.length; i++) {
			ConstPool.addNameTypeCP(casefields[i].name.name,((Type)casefields[i].type).java_signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievCaseAttribute kca = new kiev.bytecode.KievCaseAttribute();
		kca.cp_name = ConstPool.getAsciiCP(name).pos;
		kca.caseno = caseno;
		kca.cp_casefields = new int[casefields.length];
		for(int i=0; i < casefields.length; i++) {
			kca.cp_casefields[i] = ConstPool.getNameTypeCP(casefields[i].name.name,
				((Type)casefields[i].type).java_signature).pos;
		}
		return kca;
	}
}

public class ClassArgumentsAttr extends Attr {

	/** Line number table (see Code class for format description) */
	public Type[]		args;
	public short[]		argno;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public ClassArgumentsAttr() {
		super(attrClassArguments);
		args = new Type[0];
		argno= new short[0];
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < args.length; i++) {
			ConstPool.addAsciiCP(args[i].signature);
			ConstPool.addAsciiCP(args[i].getSuperType().signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievClassArgumentsAttribute kaa = new kiev.bytecode.KievClassArgumentsAttribute();
		kaa.cp_name = ConstPool.getAsciiCP(name).pos;
		kaa.cp_argname = new int[args.length];
		kaa.cp_supername = new int[args.length];
		kaa.argno = new int[args.length];
		for(int i=0; i < args.length; i++) {
			kaa.cp_argname[i] = ConstPool.getAsciiCP(args[i].signature).pos;
			kaa.cp_supername[i] = ConstPool.getAsciiCP(args[i].getSuperType().signature).pos;
			kaa.argno[i] = argno[i];
		}
		return kaa;
	}
}

public class KievAttr extends Attr {

	public byte[]		dump;

	/** Constructor for bytecode reader and raw field creation */
	public KievAttr() {
		super(attrKiev);
	}

	public KievAttr(byte[] dump) {
		super(attrKiev);
		this.dump = dump;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.Attribute ka = new kiev.bytecode.Attribute();
		ka.cp_name = ConstPool.getAsciiCP(name).pos;
		Debug.assert( dump != null, "Null data in KievAttr" );
		ka.data = dump;
		return ka;
	}

	public int size() {	return dump==null? 0: dump.length; }
}

public class FlagsAttr extends Attr {

	/* Extended flags */
	int			flags;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public FlagsAttr(int fl) {
		super(attrFlags);
		flags = fl;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievFlagsAttribute kfa = new kiev.bytecode.KievFlagsAttribute();
		kfa.cp_name = ConstPool.getAsciiCP(name).pos;
		kfa.flags = flags;
		return kfa;
	}
}

public class AliasAttr extends Attr {

	NodeName		nname;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public AliasAttr(NodeName nm) {
		super(attrAlias);
		nname = nm;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		ConstPool.addAsciiCP(nname.name);
		foreach(KString n; nname.aliases)
			ConstPool.addAsciiCP(n);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievAliasAttribute kaa = new kiev.bytecode.KievAliasAttribute();
		kaa.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = nname.aliases.length();
		kaa.cp_alias = new int[len];
		for(int i=0; i < len; i++) {
			kaa.cp_alias[i] = ConstPool.getAsciiCP(nname.aliases.at(i)).pos;
		}
		return kaa;
	}
}

public class TypedefAttr extends Attr {

	public Type		type;
	public KString	type_name;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public TypedefAttr(Typedef td) {
		super(attrTypedef);
		this.type = td.type.getType();
		this.type_name = td.name;
	}

	public TypedefAttr(Type type, KString type_name) {
		super(attrTypedef);
		this.type = type;
		this.type_name = type_name;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		ConstPool.addAsciiCP(type.signature);
		ConstPool.addAsciiCP(type_name);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievTypedefAttribute ktda = new kiev.bytecode.KievTypedefAttribute();
		ktda.cp_name = ConstPool.getAsciiCP(name).pos;
		ktda.cp_type = ConstPool.getAsciiCP(type.signature).pos;
		ktda.cp_tpnm = ConstPool.getAsciiCP(type_name).pos;
		return ktda;
	}
}

public class OperatorAttr extends Attr {

	public Operator			op;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public OperatorAttr(Operator op) {
		super(attrOperator);
		this.op = op;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		ConstPool.addAsciiCP(Operator.orderAndArityNames[op.mode]);
		ConstPool.addAsciiCP(op.image);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievOperatorAttribute koa = new kiev.bytecode.KievOperatorAttribute();
		koa.cp_name = ConstPool.getAsciiCP(name).pos;
		koa.priority = op.priority;
		koa.cp_optype = ConstPool.getAsciiCP(Operator.orderAndArityNames[op.mode]).pos;
		koa.cp_image = ConstPool.getAsciiCP(op.image).pos;
		return koa;
	}
}

public class ImportAttr extends Attr {

	public ASTNode	node;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public ImportAttr(ASTNode node) {
		super(attrImport);
		this.node = node;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		if( node instanceof Field ) {
			Field f = (Field)node;
			ConstPool.addFieldCP(((Struct)f.parent).type.signature,
				f.name.name,f.type.signature);
		}
		else if( node instanceof Method ) {
			Method m = (Method)node;
			ConstPool.addMethodCP(((Struct)m.parent).type.signature,
				m.name.name,m.type.signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievImportAttribute kia = new kiev.bytecode.KievImportAttribute();
		kia.cp_name = ConstPool.getAsciiCP(name).pos;
		if( node instanceof Field ) {
			Field f = (Field)node;
			kia.cp_ref = ConstPool.getFieldCP(((Struct)f.parent).type.signature,
				f.name.name,f.type.signature).pos;
		}
		else if( node instanceof Method ) {
			Method m = (Method)node;
			kia.cp_ref = ConstPool.addMethodCP(((Struct)m.parent).type.signature,
				m.name.name,m.type.signature).pos;
		}
		else
			throw new RuntimeException("Unknown node in import attribute: "+node.getClass());
		return kia;
	}
}

public class EnumAttr extends Attr {

	public Field[]		fields;
	public int[]		values;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public EnumAttr(Field[] fields, int[] values) {
		super(attrEnum);
		this.fields = fields;
		this.values = values;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach(Field f; fields)
			ConstPool.addAsciiCP(f.name.name);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievEnumAttribute kea = new kiev.bytecode.KievEnumAttribute();
		kea.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = fields.length;
		kea.fields = new int[len];
		kea.values = new int[len];
		for(int i=0; i < len; i++) {
			kea.fields[i] = ConstPool.getAsciiCP(fields[i].name.name).pos;
			kea.values[i] = values[i];
		}
		return kea;
	}
}

public class CheckFieldsAttr extends Attr {

	/** Fields, checked by the invariant */
	public Field[]		fields;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public CheckFieldsAttr(Field[] fields) {
		super(attrCheckFields);
		this.fields = fields;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach(Field f; fields) {
			ConstPool.addFieldCP(((Struct)f.parent).type.signature,
				f.name.name,f.type.signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievCheckFieldsAttribute cfa = new kiev.bytecode.KievCheckFieldsAttribute();
		cfa.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = fields.length;
		cfa.fields = new int[len];
		for(int i=0; i < len; i++) {
			Field f = fields[i];
			cfa.fields[i] = ConstPool.getFieldCP(((Struct)f.parent).type.signature,
				f.name.name,f.type.signature).pos;
		}
		return cfa;
	}
}

public class ContractAttr extends CodeAttr {

	public int			cond;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public ContractAttr(int cond, int max_st,int max_locs, byte[] code, Attr[] code_attrs) {
		super( (cond==WBCType.CondRequire ? attrRequire : attrEnsure),
				max_st,max_locs,code,code_attrs
			);
		this.cond = cond;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		for(int i=0; i < constants.length; i++) {
			CP cp = constants[i];
			switch(cp) {
			case AsciiCP:
				constants[i] = AsciiCP.newAsciiCP( ((AsciiCP)cp).value );
				continue;
			case ClazzCP:
				constants[i] = ClazzCP.newClazzCP( ((ClazzCP)cp).sig );
				continue;
			case NameTypeCP:
				constants[i] = NameTypeCP.newNameTypeCP( ((NameTypeCP)cp).name_cp.value, ((NameTypeCP)cp).type_cp.value );
				continue;
			case FieldCP:
				constants[i] = FieldCP.newFieldCP(
					((FieldCP)cp).clazz_cp.sig,
					((FieldCP)cp).nt_cp.name_cp.value,
					((FieldCP)cp).nt_cp.type_cp.value
				);
				continue;
			case MethodCP:
				constants[i] = MethodCP.newMethodCP(
					((MethodCP)cp).clazz_cp.sig,
					((MethodCP)cp).nt_cp.name_cp.value,
					((MethodCP)cp).nt_cp.type_cp.value
				);
				continue;
			case InterfaceMethodCP:
				constants[i] = InterfaceMethodCP.newInterfaceMethodCP(
					((InterfaceMethodCP)cp).clazz_cp.sig,
					((InterfaceMethodCP)cp).nt_cp.name_cp.value,
					((InterfaceMethodCP)cp).nt_cp.type_cp.value
				);
				continue;
			case NumberCP:
				constants[i] = NumberCP.newNumberCP( ((NumberCP)cp).value );
				continue;
			case StringCP:
				constants[i] = StringCP.newStringCP( ((StringCP)cp).asc.value );
				continue;
			}
		}
		foreach(Attr a; code_attrs) a.generate();
	}

	public kiev.bytecode.Attribute write() {
		for(int i=0; i < constants.length; i++) {
			int s = constants[i].pos;
			if( s < 1 )	throw new RuntimeException("Constant referenced, but not generated");
			int pc = constants_pc[i];
			code[pc] = (byte)(s >>> 8);
			code[pc+1] = (byte)(s & 0xFF);
		}
		kiev.bytecode.KievContractAttribute ca = new kiev.bytecode.KievContractAttribute();
		ca.cp_name = ConstPool.getAsciiCP(name).pos;
		ca.max_stack = max_stack;
		ca.max_locals = max_locals;
		ca.code = code;
		ca.attrs = new kiev.bytecode.Attribute[code_attrs.length];
		for(int i=0; i < code_attrs.length; i++) {
			ca.attrs[i] = code_attrs[i].write();
		}
		return ca;
	}
}

public class GenerationsAttr extends Attr {

	public Type[]		gens;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public GenerationsAttr(Type[] gens) {
		super(attrGenerations);
		this.gens = gens;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach(Type t; gens)
			ConstPool.addAsciiCP(t.java_signature);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievGenerationsAttribute kea = new kiev.bytecode.KievGenerationsAttribute();
		kea.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = gens.length;
		kea.gens = new int[len];
		for(int i=0; i < len; i++) {
			kea.gens[i] = ConstPool.getAsciiCP(gens[i].java_signature).pos;
		}
		return kea;
	}
}

public class PackedFieldsAttr extends Attr {

	public Struct		struct;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public PackedFieldsAttr(Struct s) {
		super(attrPackedFields);
		this.struct = s;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach(ASTNode n; struct.members; n instanceof Field && ((Field)n).isPackedField() ) {
			Field f = (Field)n;
			MetaPacked mp = f.getMetaPacked();
			if (mp == null)
				continue;
			ConstPool.addAsciiCP(f.name.name);
			ConstPool.addAsciiCP(mp.packer.name.name);
			ConstPool.addAsciiCP(f.type.signature);
		}
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievPackedFieldsAttribute kea = new kiev.bytecode.KievPackedFieldsAttribute();
		kea.cp_name = ConstPool.getAsciiCP(name).pos;
		int len = struct.countPackedFields();
		kea.fields = new int[len];
		kea.signatures = new int[len];
		kea.packers = new int[len];
		kea.sizes = new int[len];
		kea.offsets = new int[len];
		int j = 0;
		foreach(ASTNode n; struct.members; n instanceof Field && ((Field)n).isPackedField() ) {
			Field f = (Field)n;
			MetaPacked mp = f.getMetaPacked();
			if( !f.isPackedField() || mp == null ) continue;
			kea.fields[j] = ConstPool.getAsciiCP(f.name.name).pos;
			kea.signatures[j] = ConstPool.getAsciiCP(f.type.signature).pos;
			kea.packers[j] = ConstPool.getAsciiCP(mp.packer.name.name).pos;
			kea.sizes[j] = mp.size;
			kea.offsets[j] = mp.offset;
			j++;
		}
		return kea;
	}
}

public class PackerFieldAttr extends Attr {

	public Field		field;
	public int			size;

	public boolean get$isKiev() { return true; }

	/** Constructor for bytecode reader and raw field creation */
	public PackerFieldAttr(Field f) {
		super(attrPackerField);
		this.field = f;
	}
	public PackerFieldAttr(int size) {
		super(attrPackerField);
		this.size = size;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
	}

	public kiev.bytecode.Attribute write() {
		kiev.bytecode.KievPackerFieldAttribute kea = new kiev.bytecode.KievPackerFieldAttribute();
		kea.cp_name = ConstPool.getAsciiCP(name).pos;
		if( field != null )
			kea.size = field.getMetaPacked().size;
		else
			kea.size = this.size;
		return kea;
	}
}

public abstract class MetaAttr extends Attr {
	
	public MetaAttr(KString name) {
		super(name);
	}
	
	protected final void generateValue(ASTNode value) {
		if (value instanceof ConstExpr) {
			Object v = ((ConstExpr)value).getConstValue();
			if     ( v instanceof Boolean )			ConstPool.addNumberCP(Integer.valueOf(((Boolean)v).booleanValue() ? 1 : 0));
			else if( v instanceof Byte )			ConstPool.addNumberCP((Byte)v);
			else if( v instanceof Short )			ConstPool.addNumberCP((Short)v);
			else if( v instanceof Integer )			ConstPool.addNumberCP((Integer)v);
			else if( v instanceof Character )		ConstPool.addNumberCP(Integer.valueOf((int)((Character)v).charValue()));
			else if( v instanceof Long )			ConstPool.addNumberCP((Long)v);
			else if( v instanceof Float )			ConstPool.addNumberCP((Float)v);
			else if( v instanceof Double )			ConstPool.addNumberCP((Double)v);
			else if( v instanceof KString )			ConstPool.addAsciiCP((KString)v);
		}
		else if (value instanceof WrapedExpr) {
			WrapedExpr we = (WrapedExpr)value;
			if (we.expr instanceof Struct) {
				ConstPool.addClazzCP(((Struct)we.expr).type.java_signature);
			}
			else if (we.expr instanceof Field) {
				Field f = (Field)we.expr;
				Struct s = (Struct)f.parent;
				ConstPool.addAsciiCP(s.type.java_signature);
				ConstPool.addAsciiCP(f.name.name);
			}
		}
		else if (value instanceof Meta) {
			Meta m = (Meta)value;
			ConstPool.addAsciiCP(m.type.getType().java_signature);
			foreach (MetaValue v; m) {
				generateValue(v);
			}
		}
		else if (value instanceof MetaValueScalar) {
			ConstPool.addAsciiCP(value.type.name);
			generateValue(((MetaValueScalar)value).value);
		}
		else if (value instanceof MetaValueArray) {
			ConstPool.addAsciiCP(value.type.name);
			MetaValueArray va = (MetaValueArray)value; 
			foreach (ASTNode n; va.values)
				generateValue(n);
		}
	}

	public kiev.bytecode.Annotation.element_value write_values(ASTNode[] values) {
		ASTNode[] arr = values;
		kiev.bytecode.Annotation.element_value_array ev = new kiev.bytecode.Annotation.element_value_array();
		ev.tag = (byte)'[';
		ev.values = new kiev.bytecode.Annotation.element_value[arr.length];
		int n = 0;
		foreach (ASTNode node; arr) {
			ev.values[n] = write_value(node);
			n++;
		}
		return ev;
	}
	
	public kiev.bytecode.Annotation.element_value write_value(ASTNode value) {
		if (value instanceof ConstExpr) {
			kiev.bytecode.Annotation.element_value_const ev = new kiev.bytecode.Annotation.element_value_const(); 
			Object v = ((ConstExpr)value).getConstValue();
			if     ( v instanceof Boolean ) {
				ev.tag = (byte)'Z';
				ev.const_value_index = ConstPool.getNumberCP(Integer.valueOf(((Boolean)v).booleanValue() ? 1 : 0)).pos;
			}
			else if( v instanceof Byte ) {
				ev.tag = (byte)'B';
				ev.const_value_index = ConstPool.getNumberCP((Byte)v).pos;
			}
			else if( v instanceof Short ) {
				ev.tag = (byte)'S';
				ev.const_value_index = ConstPool.getNumberCP((Short)v).pos;
			}
			else if( v instanceof Integer ) {
				ev.tag = (byte)'I';
				ev.const_value_index = ConstPool.getNumberCP((Integer)v).pos;
			}
			else if( v instanceof Character ) {
				ev.tag = (byte)'C';
				ev.const_value_index = ConstPool.getNumberCP(Integer.valueOf((int)((Character)v).charValue())).pos;
			}
			else if( v instanceof Long ) {
				ev.tag = (byte)'J';
				ev.const_value_index = ConstPool.getNumberCP((Long)v).pos;
			}
			else if( v instanceof Float ) {
				ev.tag = (byte)'F';
				ev.const_value_index = ConstPool.getNumberCP((Float)v).pos;
			}
			else if( v instanceof Double ) {
				ev.tag = (byte)'D';
				ev.const_value_index = ConstPool.getNumberCP((Double)v).pos;
			}
			else if( v instanceof KString ) {
				ev.tag = (byte)'s';
				ev.const_value_index = ConstPool.getAsciiCP((KString)v).pos;
			}
			return ev;
		}
		else if (value instanceof WrapedExpr) {
			WrapedExpr we = (WrapedExpr)value;
			if (we.expr instanceof Struct) {
				kiev.bytecode.Annotation.element_value_class_info ev = new kiev.bytecode.Annotation.element_value_class_info(); 
				ev.tag = (byte)'c';
				ev.class_info_index = ConstPool.getClazzCP(((Struct)we.expr).type.java_signature).pos;
				return ev;
			}
			else if (we.expr instanceof Field) {
				kiev.bytecode.Annotation.element_value_enum_const ev = new kiev.bytecode.Annotation.element_value_enum_const(); 
				ev.tag = (byte)'e';
				Field f = (Field)we.expr;
				Struct s = (Struct)f.parent;
				ev.type_name_index = ConstPool.getAsciiCP(s.type.java_signature).pos;
				ev.const_name_index = ConstPool.getAsciiCP(f.name.name).pos;
				return ev;
			}
			throw new RuntimeException("value is: "+(we.expr==null?"null":String.valueOf(we.expr.getClass())));
		}
		else if (value instanceof Meta) {
			Meta m = (Meta)value;
			kiev.bytecode.Annotation.element_value_annotation ev = new kiev.bytecode.Annotation.element_value_annotation(); 
			ev.tag = (byte)'@';
			ev.annotation_value = new kiev.bytecode.Annotation.annotation();
			write_annotation(m, ev.annotation_value);
			return ev;
		}
		throw new RuntimeException("value is: "+(value==null?"null":String.valueOf(value.getClass())));
	}

	public void write_annotation(Meta m, kiev.bytecode.Annotation.annotation a) {
		a.type_index = ConstPool.getAsciiCP(m.type.getType().java_signature).pos;
		a.names = new int[m.size()];
		a.values = new kiev.bytecode.Annotation.element_value[m.size()];
		int n = 0;
		foreach (MetaValue v; m) {
			a.names[n] = ConstPool.addAsciiCP(v.type.name).pos;
			if (v instanceof MetaValueScalar) {
				a.values[n] = write_value(((MetaValueScalar)v).value);
			} else {
				MetaValueArray mva = (MetaValueArray)v;
				a.values[n] = write_values(mva.values.toArray());
			}
			n++;
		}
	}
}

public abstract class RMetaAttr extends MetaAttr {
	public MetaSet      ms;
	
	public RMetaAttr(KString name, MetaSet ms) {
		super(name);
		this.ms = ms;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach (Meta m; ms) {
			generateValue(m);
		}
	}
	
}

public class RVMetaAttr extends RMetaAttr {
	public RVMetaAttr(MetaSet metas) {
		super(Constants.attrRVAnnotations, metas);
	}
	public kiev.bytecode.Attribute write() {
		kiev.bytecode.RVAnnotations a = new kiev.bytecode.RVAnnotations();
		a.annotations = new kiev.bytecode.Annotation.annotation[ms.size()];
		a.cp_name = ConstPool.getAsciiCP(name).pos;
		int n = 0;
		foreach (Meta m; ms) {
			a.annotations[n] = new kiev.bytecode.Annotation.annotation();
			write_annotation(m, a.annotations[n]);
			n++;
		}
		return a;
	}
}

public class RIMetaAttr extends RMetaAttr {
	public RIMetaAttr(MetaSet metas) {
		super(Constants.attrRIAnnotations, metas);
	}
}


public abstract class ParMetaAttr extends MetaAttr {
	public MetaSet[]      mss;
	
	public ParMetaAttr(KString name, MetaSet[] mss) {
		super(name);
		this.mss = mss;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		foreach (MetaSet ms; mss; ms != null) {
			foreach (Meta m; ms) {
				generateValue(m);
			}
		}
	}

}

public class RVParMetaAttr extends ParMetaAttr {
	public RVParMetaAttr(MetaSet[] metas) {
		super(Constants.attrRVParAnnotations, metas);
	}
	public kiev.bytecode.Attribute write() {
		kiev.bytecode.RVParAnnotations a = new kiev.bytecode.RVParAnnotations();
		a.annotations = new kiev.bytecode.Annotation.annotation[mss.length][];
		a.cp_name = ConstPool.getAsciiCP(name).pos;
		for (int i=0; i < mss.length; i++) {
			MetaSet ms = mss[i];
			if (ms != null) {
				int n = 0;
				a.annotations[i] = new kiev.bytecode.Annotation.annotation[ms.size()];
				foreach (Meta m; ms) {
					a.annotations[i][n] = new kiev.bytecode.Annotation.annotation();
					write_annotation(m, a.annotations[i][n]);
					n++;
				}
			}
		}
		return a;
	}
}


public class DefaultMetaAttr extends MetaAttr {
	public MetaValue      mv;
	
	public DefaultMetaAttr(MetaValue mv) {
		super(attrAnnotationDefault);
		this.mv = mv;
	}

	public void generate() {
		ConstPool.addAsciiCP(name);
		generateValue(mv);
	}
	
	public kiev.bytecode.Attribute write() {
		kiev.bytecode.AnnotationDefault a = new kiev.bytecode.AnnotationDefault();
		a.cp_name = ConstPool.getAsciiCP(name).pos;
		if (mv instanceof MetaValueScalar)
			a.value = write_value(((MetaValueScalar)mv).value);
		else
			a.value = write_values(((MetaValueArray)mv).values.toArray());
		return a;
	}
}




