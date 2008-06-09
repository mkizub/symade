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
package kiev.be.java15;

import static kiev.be.java15.Instr.*;

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
	public Struct readClazz(ClazzName clname, Struct outer) {
		trace(Kiev.debug && Kiev.debugBytecodeRead,"Loading class "+clname.name);

		if (bcclazz.getClazzName() != clname.bytecode_name)
			throw new RuntimeException("Expected to load class "+clname.bytecode_name+" but class "+bcclazz.getClazzName()+" found");

		Struct variant = new JavaClass();
		if ((bcclazz.flags & ACC_ANNOTATION) == ACC_ANNOTATION)
			variant = new JavaAnnotation();
		else if ((bcclazz.flags & ACC_ENUM) == ACC_ENUM)
			variant = new JavaEnum();
		else if ((bcclazz.flags & ACC_INTERFACE) == ACC_INTERFACE)
			variant = new JavaInterface();
		else if (!outer.isPackage()) {
			// anonymouse classes are classed with bytecode name in form "$[0-9]+"
			String tail = clname.bytecode_name.substr(outer.qname().length()).toString();
			if (tail.length() > 1 && tail.charAt(0) >= '0' && tail.charAt(0) >= '9')
				variant = new JavaAnonymouseClass();
		}
		if (cl != null) {
			assert (cl.getClass() == variant.getClass());
			assert (cl.isTypeDeclNotLoaded());
		} else {
			cl = variant;
		}
		cl.initStruct(clname.src_name.toString(), outer, bcclazz.flags);
		cl.meta.is_interface_only = true;
		cl.bytecode_name = clname.bytecode_name;

		if (!cl.isAttached()) {
			Struct pkg = outer;
			if (outer.isPackage()) {
				String pname = "";
				if (pkg != null)
					pname = pkg.qname().replace('\u001f','/')+"/";
				FileUnit fu = FileUnit.makeFile(pname+clname.src_name+".class", false);
				fu.srpkg.symbol = (KievPackage)outer;
				fu.scanned_for_interface_only = true;
				fu.setAutoGenerated(true);
				fu.members.add(cl);
			} else {
				outer.members.add(cl);
			}
		}

		MetaAccess.verifyDecl(cl);

		KString.KStringScanner cl_sign_sc = null;
		KString cl_sign = bcclazz.getClazzSignature();
		if (cl_sign != null) {
			cl_sign_sc = new KString.KStringScanner(cl_sign);
			Signature.addTypeArgs(cl,cl_sign_sc);
		}

		cl.type_decl_version++;
		cl.setTypeDeclNotLoaded(false);
		cl.setFrontEndPassed();

		trace(Kiev.debug && Kiev.debugBytecodeRead,"Clazz type "+bcclazz.getClazzName());
		
		// This class's superclass name (load if not loaded)
		if (bcclazz.getSuperClazzName() != null) {
			assert(cl.super_types.length == 0);
			KString cl_super_name = bcclazz.getSuperClazzName();
			trace(Kiev.debug && Kiev.debugBytecodeRead,"Super-class is "+cl_super_name);
			CompaundType st;
			if (cl_sign_sc != null) {
				st = Signature.getClassTypeSignature(cl,cl_sign_sc);
				//ClazzName cn = ClazzName.fromBytecodeName(cl_super_name);
				//if (!st.meta_type.qname().equals(cn.name.toString().replace('.','\u001f')))
				//	throw new RuntimeException("Class "+cl+" has super-class "+cn+" but in signature super-class name is "+st.tdecl);
			} else {
				st = (CompaundType)Signature.getTypeOfClazzCP(new KString.KStringScanner(cl_super_name));
			}
			cl.super_types.append(new TypeRef(st));
			if (Env.getRoot().loadTypeDecl(st.tdecl).isTypeDeclNotLoaded())
				throw new RuntimeException("Class "+st.tdecl.qname()+" not found");
		}

		// Read interfaces
		KString[] interfs = bcclazz.getInterfaceNames();
		for(int i=0; i < interfs.length; i++) {
			trace(Kiev.debug && Kiev.debugBytecodeRead,"Class implements "+interfs[i]);
			CompaundType interf;
			if (cl_sign_sc != null) {
				interf = Signature.getClassTypeSignature(cl,cl_sign_sc);
				//ClazzName cn = ClazzName.fromBytecodeName(interfs[i]);
				//if (!interf.meta_type.qname().equals(cn.name.toString().replace('.','\u001f')))
				//	throw new RuntimeException("Class "+cl+" has super-interface "+cn+" but in signature super-interface name is "+interf.tdecl);
			} else {
				interf = (CompaundType)Signature.getTypeOfClazzCP(new KString.KStringScanner(interfs[i]));
			}
			if (Env.getRoot().loadTypeDecl(interf.tdecl).isTypeDeclNotLoaded())
				throw new RuntimeException("Class "+interf+" not found");
			if (!interf.tdecl.isInterface())
				throw new RuntimeException("Class "+interf+" is not an interface");
			cl.super_types.append(new TypeRef(interf));
		}

		cl.members.delAll();

		if (!outer.isPackage()) {
			int n = 0;
			for(TypeDecl p=outer; p.isStructInner() && !p.isStatic(); p=p.package_clazz.dnode) n++;
			TypeAssign td = new TypeAssign("outer$"+n+"$type", new TypeRef(outer.xtype));
			td.setSynthetic(true);
			cl.members.append(td);
			cl.ometa_tdef = td;
			cl.type_decl_version++;
		}

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
		return cl;
	}

	public Field readField(Field f, int index) {
		kiev.bytecode.Field bcf = bcclazz.fields[index];
		int f_flags = bcf.flags;
		KString f_name = bcf.getName(bcclazz);
		KString f_type = bcf.getSignature(bcclazz);
		KString f_type_sign = bcf.getFieldSignature(bcclazz);
		ENode f_init = null;
		int packer_size = -1;
		Type ftype;
		if (f_type_sign != null)
			ftype = Signature.getTypeFromFieldSignature(cl, new KString.KStringScanner(f_type_sign));
		else
			ftype = Signature.getType(f_type);
		if ((f_flags & ACC_ENUM)!=0) {
			f = new Field(f_name.toString(),ftype,f_flags);
			f.meta.is_enum = true;
		} else {
			f = new Field(f_name.toString(),ftype,f_flags);
		}
		f.meta.is_interface_only = true;
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
		if( packer_size >= 0 ) {
			MetaPacker mpr = new MetaPacker();
			mpr.size = packer_size;
			f.setMeta(mpr);
		}
		f.init = f_init;
		cl.members.append(f);
		if ((f_flags & ACC_ENUM)!=0)
			((JavaEnum)cl).enum_fields.append(f);
		return f;
	}

	public Method readMethod(int index) {
		kiev.bytecode.Method bcm = bcclazz.methods[index];
		Method m;
		{
			int m_flags = bcm.flags;
			KString m_name = bcm.getName(bcclazz);
			KString m_type_java = bcm.getSignature(bcclazz);
			KString m_type = m_type_java;
			KString m_type_sign = bcm.getMethodSignature(bcclazz);

			if (m_name == knameInit || m_name == knameClassInit)
				m = new Constructor(m_flags);
			else
				m = new MethodImpl(m_name.toString(),StdTypes.tpVoid,m_flags);
			m.meta.is_interface_only = true;
			cl.members.append(m);

			CallType mtype;
			if (m_type_sign != null) {
				KString.KStringScanner m_type_sign_sc = new KString.KStringScanner(m_type_sign);
				Signature.addTypeArgs(m,m_type_sign_sc);
				mtype = Signature.getTypeFromMethodSignature(m, m_type_sign_sc);
			} else {
				mtype = (CallType)Signature.getType(m_type);
			}
			m.type_ret = new TypeRef(mtype.ret());
			for (int i=0; i < mtype.arity; i++) {
				if( (m_flags & ACC_VARARGS) != 0 && i == mtype.arity-1)
					m.params += new LVar(0,"va_arg",mtype.arg(i),Var.PARAM_VARARGS,ACC_FINAL);
				else
					m.params += new LVar(0,"arg"+i,mtype.arg(i),Var.PARAM_NORMAL,0);
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
		trace(Kiev.debug && Kiev.debugBytecodeRead,"read method "+m+" with flags 0x"+Integer.toHexString(m.getFlags()));
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
			KString[] exceptions = new KString[ea.cp_exceptions.length];
			for(int i=0; i < exceptions.length; i++)
				exceptions[i] = ea.getException(i,clazz);
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
						outer[i] = (JStruct)Env.getRoot().getBackendEnv().loadStruct(cn);
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
							char ch = (char)cn.bytecode_name.byteAt(i+1);
							if (ch >= '0' && ch <= '9') {
								anon = true;
								break;
							}
						}
						if (anon || cn.package_name() != KString.from(cl.qname().replace('\u001f','.'))) {
							inner[i] == null;
						} else {
							Struct inn = Env.getRoot().getBackendEnv().loadStruct(cn);
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
			ConstPool constPool = new ConstPool();
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
				trace(Kiev.debug && Kiev.debugBytecodeRead,pc+": opc: "+JConstants.opcNames[0xFF&ca.bcode[pc]]);
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
				trace(Kiev.debug && Kiev.debugBytecodeRead,pc+": CP: "+cp);
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
				trace(Kiev.debug && Kiev.debugBytecodeRead,pc+": CP: "+constants[constants_top]);
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
				dn.setMeta(readAnnotation(clazz, ann));
			a = null;
		}
		else if( name.equals(attrRIAnnotations) ) {
			kiev.bytecode.RIAnnotations rva = (kiev.bytecode.RIAnnotations)bca;
			foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations)
				dn.setMeta(readAnnotation(clazz, ann));
			a = null;
		}
		else if( name.equals(attrRVParAnnotations) ) {
			kiev.bytecode.RVParAnnotations rva = (kiev.bytecode.RVParAnnotations)bca;
			for (int i=0; i < rva.annotations.length; i++) {
				foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations[i])
					((Method)dn).params[i].setMeta(readAnnotation(clazz, ann));
			}
			a = null;
		}
		else if( name.equals(attrRIParAnnotations) ) {
			kiev.bytecode.RIParAnnotations rva = (kiev.bytecode.RIParAnnotations)bca;
			for (int i=0; i < rva.annotations.length; i++) {
				foreach (kiev.bytecode.Annotation.annotation ann; rva.annotations[i])
					((Method)dn).params[i].setMeta(readAnnotation(clazz, ann));
			}
			a = null;
		}
		else if( name.equals(attrAnnotationDefault) ) {
			kiev.bytecode.AnnotationDefault rva = (kiev.bytecode.AnnotationDefault)bca;
			MetaValue mv = readAnnotationValue(clazz,rva.value,((Method)dn).sname);
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
		assert (sign.byteAt(0) == 'L' && sign.byteAt(sign.len-1) == ';');
		String nm = sign.toString();
		nm = nm.substring(1,nm.length()-1).replace('/','\u001f');
		UserMeta um = new UserMeta(nm);
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
		Attr[] jattrs = cl.jattrs;
		if (jattrs != null) {
			int len = 0;
			foreach(Attr a; jattrs; !a.isKiev) len++;
			bcclazz.attrs = new kiev.bytecode.Attribute[len];
			for(int i=0, j=0; i < jattrs.length; i++) {
				if( jattrs[i].isKiev ) continue;
				bcclazz.attrs[j++] = writeAttr(jattrs[i]);
			}
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
		bcf.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(f.sname).pos];
		JType tp = f.type.getJType();
		bcf.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(tp.java_signature).pos];
		bcf.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		Attr[] jattrs = f.jattrs;
		if (jattrs != null) {
			bcf.attrs = new kiev.bytecode.Attribute[jattrs.length];
			for(int i=0; i < jattrs.length; i++)
				bcf.attrs[i] = writeAttr(jattrs[i]);
		}
		return bcf;
	}

    public kiev.bytecode.Method writeMethod(Method m) {
		Struct jcl = cl;
		kiev.bytecode.Method bcm = new kiev.bytecode.Method();
		bcm.flags = m.getJavaFlags();
		KString nm;
		if (m instanceof Constructor) {
			if (m.isStatic())
				nm = knameClassInit;
			else
				nm = knameInit;
		} else {
			nm = KString.from(m.sname);
		}
		bcm.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(nm).pos];
		bcm.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(m.etype.getJType().java_signature).pos];
		bcm.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		Attr[] jattrs = m.jattrs;
		if (jattrs != null) {
			bcm.attrs = new kiev.bytecode.Attribute[jattrs.length];
			for(int i=0; i < jattrs.length; i++)
				bcm.attrs[i] = writeAttr(jattrs[i]);
		}
		return bcm;
    }

	public kiev.bytecode.Attribute writeAttr(Attr a) {
		kiev.bytecode.Attribute kba = a.write(bcclazz,constPool);
		return kba;
	}
}

