package kiev.be.java15;

import kiev.Kiev;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import static kiev.be.java15.Instr.*;
import static kiev.vlang.Operator.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public class Bytecoder implements JConstants {
	public Struct								cl;
	public ConstPool							constPool;
	public kiev.bytecode.Clazz					bcclazz;

	public Bytecoder(Struct cl, kiev.bytecode.Clazz bcclazz, ConstPool constPool) {
		this.cl = cl;
		this.bcclazz = bcclazz;
		this.constPool = constPool;
	}

	/** Reads class from DataInputStream
	 */
	public Struct readClazz() {
		trace(Kiev.debugBytecodeRead,"Loading class "+cl);

		if( !bcclazz.getClazzName().startsWith(((JStruct)cl).bname()) ) {
			throw new RuntimeException("Expected to load class "+((JStruct)cl).bname()
				+" but class "+bcclazz.getClazzName()+" found");
		}

		// Clean some structure flags
		if (bcclazz.flags != 0) {
			if ((bcclazz.flags & ACC_PUBLIC) == ACC_PUBLIC) cl.meta.setU(new MetaAccess("public"));
			if ((bcclazz.flags & ACC_PROTECTED) == ACC_PROTECTED) cl.meta.setU(new MetaAccess("protected"));
			if ((bcclazz.flags & ACC_PRIVATE) == ACC_PRIVATE) cl.meta.setU(new MetaAccess("private"));
			if ((bcclazz.flags & ACC_STATIC) == ACC_STATIC) cl.meta.setF(new MetaStatic());
			if ((bcclazz.flags & ACC_FINAL) == ACC_FINAL) cl.meta.setF(new MetaFinal());
			if ((bcclazz.flags & ACC_ABSTRACT) == ACC_ABSTRACT) cl.meta.setF(new MetaAbstract());
			if ((bcclazz.flags & ACC_SYNTHETIC) == ACC_SYNTHETIC) cl.meta.setF(new MetaSynthetic());
			cl.flags = bcclazz.flags;
		}
		MetaAccess.verifyDecl(cl);

		cl.setTypeDeclLoaded(true);
		cl.setMembersGenerated(true);

		trace(Kiev.debugBytecodeRead,"Clazz type "+bcclazz.getClazzName());

		// This class's superclass name (load if not loaded)
		if (bcclazz.getSuperClazzName() != null) {
			KString cl_super_name = bcclazz.getSuperClazzName(); //kaclazz==null? bcclazz.getSuperClazzName() : kaclazz.getSuperClazzName() ;
			trace(Kiev.debugBytecodeRead,"Super-class is "+cl_super_name);
			CompaundType st = Signature.getTypeOfClazzCP(new KString.KStringScanner(cl_super_name));
		    cl.super_types.append(new TypeRef(st));
			if (!Env.loadStruct(st.clazz).isTypeDeclLoaded())
				throw new RuntimeException("Class "+st.clazz.qname()+" not found");
		}

		// Read interfaces
		KString[] interfs = bcclazz.getInterfaceNames();
		for(int i=0; i < interfs.length; i++) {
			trace(Kiev.debugBytecodeRead,"Class implements "+interfs[i]);
			CompaundType interf = Signature.getTypeOfClazzCP(new KString.KStringScanner(interfs[i]));
			if (!Env.loadStruct(interf.clazz).isTypeDeclLoaded())
				throw new RuntimeException("Class "+interf+" not found");
			if (!interf.clazz.isInterface())
				throw new RuntimeException("Class "+interf+" is not an interface");
			cl.super_types.append(new TypeRef(interf));
		}

		cl.members.delAll();
//		jclazz = new JStruct(cl);
//		jclazz.setLoadedFromBytecode(true);
		
		for(int i=0; i < bcclazz.fields.length; i++) {
			readField(null,i);
		}

		for(int i=0; i < bcclazz.methods.length; i++) {
			readMethod(i);
		}

		kiev.bytecode.Attribute[] attrs = bcclazz.attrs;
		for(int i=0; i < attrs.length; i++) {
			Attr at = readAttr(bcclazz.attrs[i],bcclazz,cl);
			if( at != null ) {
				((JStruct)cl).addAttr(at);
			}
		}
		//ProcessVirtFld tp = (ProcessVirtFld)Kiev.getProcessor(Kiev.Ext.VirtualFields);
		//if (tp != null)
		//	tp.addAbstractFields(cl);
		return cl;
	}

	public Field readField(Field f, int index) {
		kiev.bytecode.Field bcf = bcclazz.fields[index];
		int f_flags = bcf.flags;
		KString f_name = bcf.getName(bcclazz);
		KString f_type = bcf.getSignature(bcclazz);
		Attr[] attrs = Attr.emptyArray;
		ENode f_init = null;
		Symbol nm = null;
		int packer_size = -1;
		Type ftype = Signature.getType(f_type);
		f = new Field(f_name.toString(),ftype,f_flags);
		for(int i=0; i < bcf.attrs.length; i++) {
			Attr at = readAttr(bcf.attrs[i],bcclazz,f);
			if( at == null ) continue;
			if( at.name.equals(attrConstantValue) && (f_flags & ACC_FINAL)!=0 ) {
				ConstantValueAttr a = (ConstantValueAttr)at;
				if (a.value instanceof KString)
					f_init = ConstExpr.fromConst(a.value.toString());
				else
					f_init = ConstExpr.fromConst(a.value);
			}
		}
		if( nm != null )
			f.id.aliases = nm.aliases;
		if( packer_size >= 0 ) {
			MetaPacker mpr = new MetaPacker();
			mpr.size = packer_size;
			f.meta.setU(mpr);
		}
		f.init = f_init;
		cl.members.append(f);
//		jclazz.addMember(new JField(f));
		return f;
	}

	public Method readMethod(int index) {
		kiev.bytecode.Method bcm = bcclazz.methods[index];
		int m_flags = bcm.flags;
		KString m_name = bcm.getName(bcclazz);
		String m_name_s = m_name.toString().intern();
		KString m_type_java = bcm.getSignature(bcclazz);
		KString m_type = m_type_java;
		Attr[] attrs = Attr.emptyArray;
		CallType mtype = (CallType)Signature.getType(m_type);
		CallType jtype = mtype;
		Method m; 
		if (m_name_s == nameInit || m_name_s == nameClassInit)
			m = new Constructor(m_flags);
		else
			m = new Method(m_name_s,mtype.ret(),m_flags);
		cl.members.append(m);
		for (int i=0; i < mtype.arity; i++) {
			if( (m_flags & ACC_VARARGS) != 0 && i == mtype.arity-1) {
				FormPar fp = new FormPar(new Symbol("va_arg"),
					new TypeRef(mtype.arg(i)),new TypeRef(jtype.arg(i)),FormPar.PARAM_VARARGS,ACC_FINAL);
					m.params.add(fp);
					mtype = m.etype;
					break;
			} else {
				FormPar fp = new FormPar(new Symbol("arg"+i),
					new TypeRef(mtype.arg(i)),new TypeRef(jtype.arg(i)),FormPar.PARAM_NORMAL,0);
					m.params.add(fp);
			}
		}
		for(int i=0; i < bcm.attrs.length; i++) {
			Attr at = readAttr(bcm.attrs[i],bcclazz,m);
			if( at == null ) continue;
			if( at.name.equals(attrExceptions) ) {
				((JMethod)m).addAttr(at);
			}
			else if( at.name.equals(attrRequire) || at.name.equals(attrEnsure) ) {
				WBCCondition wbc = new WBCCondition();
				if (at.name.equals(attrRequire))
					wbc.cond = WBCType.CondRequire;
				else
					wbc.cond = WBCType.CondEnsure;
				wbc.code_attr = (ContractAttr)at;
				wbc.definer = m;
				if (m.conditions.indexOf(wbc) < 0)
					m.conditions.add(wbc);
			}
		}
		((JMethod)m).attrs = attrs;
		trace(Kiev.debugBytecodeRead,"read method "+m+" with flags 0x"+Integer.toHexString(m.getFlags()));
		if( m.isStatic()
		 && !m.id.equals(nameClassInit)
		 && cl.package_clazz.isInterface()
		 && cl.id.uname == nameIFaceImpl
		)
			m.setVirtualStatic(true);
//		jclazz.addMember(new JMethod(m));
		return m;
	}

	public Attr readAttr(kiev.bytecode.Attribute bca, kiev.bytecode.Clazz clazz, DNode dn) {
		Attr a = null;
		KString name = bca.getName(clazz);
//		Debug.trace(true,"reading attr "+name);
		if( name.equals(attrSourceFile) ) {
			a = new SourceFileAttr(((kiev.bytecode.SourceFileAttribute)bca).getFileName(clazz));
		}
		else if( name.equals(attrExceptions) ) {
			kiev.bytecode.ExceptionsAttribute ea = (kiev.bytecode.ExceptionsAttribute)bca;
			JStruct[] exceptions = new JStruct[ea.cp_exceptions.length];
			for(int i=0; i < exceptions.length; i++) {
				exceptions[i] = (JStruct)Env.jenv.makeStruct(ea.getException(i,clazz),false);
			}
			a = new ExceptionsAttr();
			((ExceptionsAttr)a).exceptions = exceptions;
		}
		else if( name.equals(attrInnerClasses) ) {
			kiev.bytecode.InnerClassesAttribute ica = (kiev.bytecode.InnerClassesAttribute)bca;
			int elen = ica.cp_inners.length;
			JStruct[] inner = new JStruct[elen];
			JStruct[] outer = new JStruct[elen];
			KString[] inner_name = new KString[elen];
			short[] acc = new short[elen];
			for(int i=0; i < elen; i++) {
				try {
					ClazzName cn;
					if( ica.cp_outers[i] != null ) {
						cn = ClazzName.fromBytecodeName(ica.getOuterName(i,clazz));
						outer[i] = (JStruct)Env.jenv.loadStruct(cn);
						if( outer[i] == null )
							throw new RuntimeException("Class "+cn+" not found");
					} else {
						outer[i] = null;
					}
					if( ica.cp_inners[i] != null ) {
						cn = ClazzName.fromBytecodeName(ica.getInnerName(i,clazz));
						// load only non-anonymouse classes
						boolean anon = false;
						for (int i=0; i < cn.bytecode_name.len; i++) {
							i = cn.bytecode_name.indexOf((byte)'$',i);
							if (i < 0) break;
							char ch = cn.bytecode_name.byteAt(i+1);
							if (ch >= '0' && ch <= '9') {
								anon = true;
								break;
							}
						}
						if (anon || cn.package_name() != KString.from(cl.qname())) {
							inner[i] == null;
						} else {
							Struct inn = Env.jenv.loadStruct(cn);
							inner[i] = (JStruct)inn;
							if( inn == cl ) {
								Kiev.reportWarning("Class "+cl+" is inner for itself");
							} else {
								if( inn == null )
									throw new RuntimeException("Class "+cn+" not found");
								cl.members.add(~inn);
							}
						}
					} else {
						inner[i] = null;
					}
					acc[i] = (short)ica.cp_inner_flags[i];
				} catch(Exception e ) {
					Kiev.reportError(e);
				}
			}
			a = new InnerClassesAttr();
			((InnerClassesAttr)a).inner = inner;
			((InnerClassesAttr)a).outer = outer;
			((InnerClassesAttr)a).acc = acc;
		}
		else if( name.equals(attrConstantValue) ) {
			kiev.bytecode.ConstantValueAttribute ca = (kiev.bytecode.ConstantValueAttribute)bca;
			a = new ConstantValueAttr(ca.getValue(bcclazz));
		}
		else if( name.equals(attrRequire) || name.equals(attrEnsure) ) {
			kiev.bytecode.KievContractAttribute kca = (kiev.bytecode.KievContractAttribute)bca;
			ContractAttr ca = new ContractAttr(
				(name.equals(attrEnsure) ? WBCType.CondEnsure : WBCType.CondRequire ),
				kca.max_stack, kca.max_locals, kca.code, Attr.emptyArray
			);
			// Now, scan the bytecode to findout constants and offsets
			int pc = 0;
			int cp = 0;
			int constants_top = 0;
			CP[] constants = new CP[32];
			int[] constants_pc = new int[32];
			while( pc < ca.bcode.length ) {
				cp = 0;
				trace(Kiev.debugBytecodeRead,pc+": opc: "+JConstants.opcNames[0xFF&ca.bcode[pc]]);
				switch( 0xFF & ca.bcode[pc] ) {
				case opc_ldc:
					cp = 0xFF & ca.bcode[pc+1];
					break;
				case opc_ldc_w:
				case opc_ldc2_w:
				case opc_getstatic:
				case opc_putstatic:
				case opc_getfield:
				case opc_putfield:
				case opc_invokevirtual:
				case opc_invokespecial:
				case opc_invokestatic:
				case opc_invokeinterface:
				case opc_new:
				case opc_anewarray:
				case opc_multianewarray:
				case opc_checkcast:
				case opc_instanceof:
					cp = ((0xFF & ca.bcode[pc+1]) << 8) | (0xFF & ca.bcode[pc+2]);
					break;
				default:
					pc += JConstants.opcLengths[0xFF & ca.bcode[pc]];
					continue;
				}
				if( constants_top == constants.length ) {
					constants = (CP[])Arrays.ensureSize(constants,constants.length*2);
					constants_pc = (int[])Arrays.ensureSize(constants_pc,constants.length);
				}
				trace(Kiev.debugBytecodeRead,pc+": CP: "+cp);
				constants_pc[constants_top] = pc+1;
				kiev.bytecode.PoolConstant pcp = clazz.pool[cp];
				switch(pcp.constant_type()) {
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_CLASS:
					{
					kiev.bytecode.Utf8PoolConstant upc = ((kiev.bytecode.ClazzPoolConstant)pcp).ref;
					constants[constants_top] = new ClazzCP(constPool,new AsciiCP(constPool,upc.value));
					}
					break;
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_STRING:
					{
					kiev.bytecode.Utf8PoolConstant upc = ((kiev.bytecode.StringPoolConstant)pcp).ref;
					constants[constants_top] = new StringCP(constPool,new AsciiCP(constPool,upc.value));
					}
					break;
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_FIELD:
					{
					kiev.bytecode.ClazzPoolConstant clpc = ((kiev.bytecode.FieldPoolConstant)pcp).ref_clazz;
					kiev.bytecode.NameAndTypePoolConstant ntpc = ((kiev.bytecode.FieldPoolConstant)pcp).ref_nametype;
					constants[constants_top] = new FieldCP(
						constPool,
						new ClazzCP(
							constPool,
							new AsciiCP(constPool,clpc.ref.value)),
						new NameTypeCP(
							constPool,
							new AsciiCP(constPool,ntpc.ref_name.value),
							new AsciiCP(constPool,ntpc.ref_type.value))
					);
					}
					break;
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_METHOD:
					{
					kiev.bytecode.ClazzPoolConstant clpc = ((kiev.bytecode.MethodPoolConstant)pcp).ref_clazz;
					kiev.bytecode.NameAndTypePoolConstant ntpc = ((kiev.bytecode.MethodPoolConstant)pcp).ref_nametype;
					constants[constants_top] = new MethodCP(
						constPool,
						new ClazzCP(
							constPool,
							new AsciiCP(constPool,clpc.ref.value)),
						new NameTypeCP(
							constPool,
							new AsciiCP(constPool,ntpc.ref_name.value),
							new AsciiCP(constPool,ntpc.ref_type.value))
					);
					}
					break;
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_INTERFACEMETHOD:
					{
					kiev.bytecode.ClazzPoolConstant clpc = ((kiev.bytecode.InterfaceMethodPoolConstant)pcp).ref_clazz;
					kiev.bytecode.NameAndTypePoolConstant ntpc = ((kiev.bytecode.InterfaceMethodPoolConstant)pcp).ref_nametype;
					constants[constants_top] = new InterfaceMethodCP(
						constPool,
						new ClazzCP(
							constPool,
							new AsciiCP(constPool,clpc.ref.value)),
						new NameTypeCP(
							constPool,
							new AsciiCP(constPool,ntpc.ref_name.value),
							new AsciiCP(constPool,ntpc.ref_type.value))
					);
					}
					break;
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_INTEGER:
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_FLOAT:
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_LONG:
				case kiev.bytecode.BytecodeFileConstants.CONSTANT_DOUBLE:
					{
					kiev.bytecode.NumberPoolConstant npc = (kiev.bytecode.NumberPoolConstant)pcp;
					constants[constants_top] = new NumberCP(constPool, npc.getValue());
					}
					break;
				default:
					throw new RuntimeException("Bad pool constant "+clazz.pool[cp]+" of opcode "+JConstants.opcNames[0xFF&ca.bcode[pc]]);
				}
				trace(Kiev.debugBytecodeRead,pc+": CP: "+constants[constants_top]);
				constants_top++;
				pc += JConstants.opcLengths[0xFF & ca.bcode[pc]];
			}
			ca.constants = (CP[])Arrays.cloneToSize(constants,constants_top);
			ca.constants_pc = (int[])Arrays.cloneToSize(constants_pc,constants_top);
			a = ca;
		}
		else if( name.equals(attrRVAnnotations) ) {
			kiev.bytecode.RVAnnotations rva = (kiev.bytecode.RVAnnotations)bca;
			foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations)
				dn.meta.setU(readAnnotation(clazz, ann));
			a = null;
		}
		else if( name.equals(attrRIAnnotations) ) {
			kiev.bytecode.RIAnnotations rva = (kiev.bytecode.RIAnnotations)bca;
			foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations)
				dn.meta.setU(readAnnotation(clazz, ann));
			a = null;
		}
		else if( name.equals(attrRVParAnnotations) ) {
			kiev.bytecode.RVParAnnotations rva = (kiev.bytecode.RVParAnnotations)bca;
			for (int i=0; i < rva.annotations.length; i++) {
				foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations[i])
					((Method)dn).params[i].meta.setU(readAnnotation(clazz, ann));
			}
			a = null;
		}
		else if( name.equals(attrRIParAnnotations) ) {
			kiev.bytecode.RIParAnnotations rva = (kiev.bytecode.RIParAnnotations)bca;
			for (int i=0; i < rva.annotations.length; i++) {
				foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations[i])
					((Method)dn).params[i].meta.setU(readAnnotation(clazz, ann));
			}
			a = null;
		}
		else if( name.equals(attrAnnotationDefault) ) {
			kiev.bytecode.AnnotationDefault rva = (kiev.bytecode.AnnotationDefault)bca;
			MetaValue mv = readAnnotationValue(clazz,rva.value,((Method)dn).id.uname);
			((Method)dn).body = mv;
			a = null;
		}
		else {
			a = null; // new Attr(cl,name);
		}
		return a;
	}
	
	UserMeta readAnnotation(kiev.bytecode.Clazz clazz, kiev.bytecode.Annotation.annotation ann) {
		KString sign = ann.getSignature(clazz);
		Type tp = Signature.getType(sign);
		UserMeta um = new UserMeta(new TypeNameRef(tp));
		for (int i=0; i < ann.names.length; i++) {
			String nm = ann.getName(i,clazz).toString();
			MetaValue val = readAnnotationValue(clazz,ann.values[i],nm);
			um.set(val);
		}
		return um; 
	}

	MetaValue readAnnotationValue(kiev.bytecode.Clazz clazz, kiev.bytecode.Annotation.element_value eval, String nm) {
		MetaValue mv = null;
		switch (eval.tag) {
		case 'B':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstByteExpr((byte)((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).intValue())
				);
		case 'C':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstCharExpr((char)((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).intValue())
				);
		case 'D':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstDoubleExpr(((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).doubleValue())
				);
		case 'F':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstFloatExpr(((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).floatValue())
				);
		case 'I':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstIntExpr(((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).intValue())
				);
		case 'J':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstLongExpr(((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).longValue())
				);
		case 'S':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstShortExpr((short)((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).intValue())
				);
		case 'Z':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstBoolExpr(((Number)((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz)).intValue() != 0)
				);
		case 's':
			mv = new MetaValueScalar(
				new SymbolRef(nm),
				new ConstStringExpr(((kiev.bytecode.Annotation.element_value_const)eval).getValue(clazz).toString())
				);
			break;
		case 'e': {
			Type tp = Signature.getType(((kiev.bytecode.Annotation.element_value_enum_const)eval).getSignature(clazz));
			tp.meta_type.tdecl.checkResolved();
			KString fname = ((kiev.bytecode.Annotation.element_value_enum_const)eval).getFieldName(clazz);
			Field f = tp.meta_type.tdecl.resolveField(fname.toString().intern());
			mv = new MetaValueScalar(new SymbolRef(nm),new SFldExpr(0,f));
			}
			break;
		case 'c': {
			Type tp = Signature.getType(((kiev.bytecode.Annotation.element_value_class_info)eval).getSignature(clazz));
			mv = new MetaValueScalar(new SymbolRef(nm),new TypeRef(tp));
			}
			break;
		case '@': {
			UserMeta um = readAnnotation(clazz,((kiev.bytecode.Annotation.element_value_annotation)eval).annotation_value);
			mv = new MetaValueScalar(new SymbolRef(nm),um);
			}
			break;
		case '[': {
			mv = new MetaValueArray(new SymbolRef(nm));
			foreach (kiev.bytecode.Annotation.element_value ev; ((kiev.bytecode.Annotation.element_value_array)eval).values)
				mv.values += ~((MetaValueScalar)readAnnotationValue(clazz,ev,"")).value;
			}
			break;
		default:
			throw new ClassFormatError("unknow annotation value tag: "+(char)eval.tag);
		}
		return mv;
	}

	/** Write class
	 */
	public byte[] writeClazz() {
	    bcclazz = new kiev.bytecode.Clazz();

	    // Constant pool
		bcclazz.pool = writeConstPool();

	    // Access bitflags
		bcclazz.flags = cl.getJavaFlags();
	    if( !cl.isInterface() )
	    	bcclazz.flags |= ACC_SUPER;

		// This class name
		KString cl_sig = cl.xtype.getJType().java_signature;
		trace(Kiev.debugBytecodeGen,"note: class "+cl+" class signature = "+cl_sig);
		bcclazz.cp_clazz = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(cl_sig).pos];
	    // This class's superclass name
	    if (cl.super_types.length > 0) {
			Type tp = cl.super_types[0].getType();
			assert(tp.getStruct().isClazz());
		    KString sup_sig = tp.getJType().java_signature;
		    bcclazz.cp_super_clazz = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(sup_sig).pos];
		} else {
			bcclazz.cp_super_clazz = null;
		}

		assert(cl.super_types.length > 0);
	    bcclazz.cp_interfaces = new kiev.bytecode.ClazzPoolConstant[cl.super_types.length-1];
		for(int i=1; i < cl.super_types.length; i++) {
			Type tp = cl.super_types[i].getType();
			assert(tp.getStruct().isInterface());
		    KString interf_sig = tp.getJType().java_signature;
			bcclazz.cp_interfaces[i-1] = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(interf_sig).pos];
		}

		{
			Vector<kiev.bytecode.Field> flds = new Vector<kiev.bytecode.Field>(); 
			foreach (Field f; cl.members) {
				if( f.isPackedField() ) continue;
				if (/*!kievmode &&*/ f.isAbstract()) continue;
				flds.append(writeField(f));
			}
			bcclazz.fields = flds.copyIntoArray();
		}

		{
			Vector<kiev.bytecode.Method> mthds = new Vector<kiev.bytecode.Method>();
			foreach (Method m; cl.members) {
				mthds.append(writeMethod(m));
			}
			bcclazz.methods = mthds.copyIntoArray();
		}

	    // Number of class attributes
		JStruct jcl = (JStruct)cl;
		int len = 0;
		foreach(Attr a; jcl.attrs; !a.isKiev) len++;
		bcclazz.attrs = new kiev.bytecode.Attribute[len];
		for(int i=0, j=0; i < jcl.attrs.length; i++) {
			if( jcl.attrs[i].isKiev ) continue;
			bcclazz.attrs[j++] = writeAttr(jcl.attrs[i]);
		}
		return bcclazz.writeClazz();
	}

	public kiev.bytecode.PoolConstant[] writeConstPool() {
		// Write constant pool
		for(int i=0; i < constPool.hwm; i++)
			if( constPool.pool[i] != null )
				constPool.pool[i].pos = i;
		int hwm, lwm;
		kiev.bytecode.PoolConstant[] bcpool;
		hwm = constPool.java_hwm;
		lwm = 1;
		bcpool = new kiev.bytecode.PoolConstant[hwm];
		bcpool[0] = new kiev.bytecode.VoidPoolConstant(0);
		for(int i=1; i < hwm; i++) {
			CP c = constPool.pool[i];
			switch( c ) {
			case AsciiCP:
				bcpool[i] = new kiev.bytecode.Utf8PoolConstant(i, ((AsciiCP)c).value);
				continue;
			case NumberCP:
				{
				NumberCP num = (NumberCP)c;
				switch( num.value ) {
				case Float:
					bcpool[i] = new kiev.bytecode.FloatPoolConstant(i, num.value.floatValue());
					continue;
				case Long:
					bcpool[i] = new kiev.bytecode.LongPoolConstant(i, num.value.longValue());
					++i;
					bcpool[i] = new kiev.bytecode.VoidPoolConstant(i);
					continue;
				case Double:
					bcpool[i] = new kiev.bytecode.DoublePoolConstant(i, num.value.doubleValue());
					++i;
					bcpool[i] = new kiev.bytecode.VoidPoolConstant(i);
					continue;
				}
				bcpool[i] = new kiev.bytecode.IntegerPoolConstant(i, num.value.intValue());
				continue;
				}
			}
		}
		for(int i=1; i < hwm; i++) {
			CP c = constPool.pool[i];
			switch( c ) {
			case ClazzCP:
				bcpool[i] = new kiev.bytecode.ClazzPoolConstant(i,
							(kiev.bytecode.Utf8PoolConstant)bcpool[((ClazzCP)c).asc.pos]
				);
				continue;
			case StringCP:
				bcpool[i] = new kiev.bytecode.StringPoolConstant(i,
							(kiev.bytecode.Utf8PoolConstant)bcpool[((StringCP)c).asc.pos]
				);
				continue;
			case NameTypeCP:
				bcpool[i] = new kiev.bytecode.NameAndTypePoolConstant(i,
							(kiev.bytecode.Utf8PoolConstant)bcpool[((NameTypeCP)c).name_cp.pos],
							(kiev.bytecode.Utf8PoolConstant)bcpool[((NameTypeCP)c).type_cp.pos]
				);
				continue;
			}
		}
		for(int i=1; i < hwm; i++) {
			CP c = constPool.pool[i];
			switch( c ) {
			case AsciiCP:		continue;
			case NumberCP:		continue;
			case ClazzCP:		continue;
			case StringCP:		continue;
			case NameTypeCP:	continue;
			case FieldCP:
				bcpool[i] = new kiev.bytecode.FieldPoolConstant(i,
							(kiev.bytecode.ClazzPoolConstant)bcpool[((FieldCP)c).clazz_cp.pos],
							(kiev.bytecode.NameAndTypePoolConstant)bcpool[((FieldCP)c).nt_cp.pos]
				);
				continue;
			case MethodCP:
				bcpool[i] = new kiev.bytecode.MethodPoolConstant(i,
							(kiev.bytecode.ClazzPoolConstant)bcpool[((MethodCP)c).clazz_cp.pos],
							(kiev.bytecode.NameAndTypePoolConstant)bcpool[((MethodCP)c).nt_cp.pos]
				);
				continue;
			case InterfaceMethodCP:
				bcpool[i] = new kiev.bytecode.InterfaceMethodPoolConstant(i,
							(kiev.bytecode.ClazzPoolConstant)bcpool[((InterfaceMethodCP)c).clazz_cp.pos],
							(kiev.bytecode.NameAndTypePoolConstant)bcpool[((InterfaceMethodCP)c).nt_cp.pos]
				);
				continue;
			default:
				if (bcpool[i] instanceof kiev.bytecode.VoidPoolConstant) {
					assert(bcpool[i-1] instanceof kiev.bytecode.LongPoolConstant || bcpool[i-1] instanceof kiev.bytecode.DoublePoolConstant);
					continue;
				}
				throw new RuntimeException("Unknown tag in ConstantPool idx "+i+": "+constPool.pool[i]+" for "+c);
			}
		}
		return bcpool;
	}

	public kiev.bytecode.Field writeField(Field f) {
		kiev.bytecode.Field bcf = new kiev.bytecode.Field();
		bcf.flags = f.getJavaFlags();
		bcf.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(f.id.uname).pos];
		JType tp = f.type.getJType();
		bcf.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(tp.java_signature).pos];
		bcf.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		JField jf = (JField)f;
		bcf.attrs = new kiev.bytecode.Attribute[jf.attrs.length];
		for(int i=0; i < jf.attrs.length; i++)
			bcf.attrs[i] = writeAttr(jf.attrs[i]);
		return bcf;
	}

    public kiev.bytecode.Method writeMethod(Method m) {
		Struct jcl = cl;
		kiev.bytecode.Method bcm = new kiev.bytecode.Method();
		bcm.flags = m.getJavaFlags();
		bcm.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(m.id.uname).pos];
		bcm.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(m.etype.getJType().java_signature).pos];
		bcm.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		JMethod jm = (JMethod)m;
		bcm.attrs = new kiev.bytecode.Attribute[jm.attrs.length];
		for(int i=0; i < jm.attrs.length; i++)
			bcm.attrs[i] = writeAttr(jm.attrs[i]);
		return bcm;
    }

	public kiev.bytecode.Attribute writeAttr(Attr a) {
		kiev.bytecode.Attribute kba = a.write(bcclazz,constPool);
		return kba;
	}
}

