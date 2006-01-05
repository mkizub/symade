package kiev.be.java;

import kiev.Kiev;
import kiev.transf.*;
import kiev.parser.*;
import kiev.vlang.*;


import static kiev.stdlib.Debug.*;
import static kiev.be.java.Instr.*;
import static kiev.vlang.Operator.*;

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

		if( !bcclazz.getClazzName().startsWith(cl.name.bytecode_name) ) {
			throw new RuntimeException("Expected to load class "+cl.name.bytecode_name
				+" but class "+bcclazz.getClazzName()+" found");
		}

		// Clean some structure flags
		cl.flags = bcclazz.flags;
		cl.acc.verifyAccessDecl(cl);

		cl.setResolved(true);
		cl.setMembersGenerated(true);
		cl.setStatementsGenerated(true);

		trace(Kiev.debugBytecodeRead,"Clazz type "+bcclazz.getClazzName());
		//cl.type = (BaseType)Signature.getTypeOfClazzCP(new KString.KStringScanner(bcclazz.getClazzName()));

		// This class's superclass name (load if not loaded)
		if( bcclazz.getSuperClazzName() != null ) {
			KString cl_super_name = bcclazz.getSuperClazzName(); //kaclazz==null? bcclazz.getSuperClazzName() : kaclazz.getSuperClazzName() ;
			trace(Kiev.debugBytecodeRead,"Super-class is "+cl_super_name);
		    cl.super_type = (BaseType)Signature.getTypeOfClazzCP(new KString.KStringScanner(cl_super_name));
			if( Env.getStruct(((BaseType)cl.super_type).clazz.name) == null )
				throw new RuntimeException("Class "+cl.super_type.clazz.name+" not found");
		}

		// Read interfaces
		KString[] interfs = bcclazz.getInterfaceNames();
		for(int i=0; i < interfs.length; i++) {
			trace(Kiev.debugBytecodeRead,"Class implements "+interfs[i]);
			BaseType interf = (BaseType)Signature.getTypeOfClazzCP(new KString.KStringScanner(interfs[i]));
			if( Env.getStruct(((BaseType)interf).clazz.name) == null )
				throw new RuntimeException("Class "+interf+" not found");
			if( !interf.isInterface() )
				throw new RuntimeException("Class "+interf+" is not an interface");
			cl.interfaces.append(new TypeRef(interf));
		}

		cl.members.delAll();
//		jclazz = new JStruct(cl);
//		jclazz.setLoadedFromBytecode(true);
		
		for(int i=0; i < bcclazz.fields.length; i++) {
			readField(null,i);
		}

		for(int i=0; i < bcclazz.methods.length; i++) {
			readMethod(null,i);
		}

		kiev.bytecode.Attribute[] attrs = bcclazz.attrs;
		for(int i=0; i < attrs.length; i++) {
			Attr at = readAttr(bcclazz.attrs[i],bcclazz);
			if( at != null ) {
				cl.getJStructView().addAttr(at);
			}
		}
		ProcessVirtFld tp = (ProcessVirtFld)Kiev.getProcessor(Kiev.Ext.VirtualFields);
		if (tp != null)
			tp.addAbstractFields(cl);
		return cl;
	}

	public Field readField(Field f, int index) {
		kiev.bytecode.Field bcf = bcclazz.fields[index];
		int f_flags = bcf.flags;
		KString f_name = bcf.getName(bcclazz);
		KString f_type = bcf.getSignature(bcclazz);
		Attr[] attrs = Attr.emptyArray;
		ENode f_init = null;
		NodeName nm = null;
		int packer_size = -1;
		Access acc = null;
		for(int i=0; i < bcf.attrs.length; i++) {
			Attr at = readAttr(bcf.attrs[i],bcclazz);
			if( at == null ) continue;
			if( at.name.equals(attrConstantValue) && (f_flags & ACC_FINAL)!=0 ) {
				ConstantValueAttr a = (ConstantValueAttr)at;
				f_init = ConstExpr.fromConst(a.value);
			}
		}
		Type ftype = Signature.getType(f_type);
		f = new Field(f_name,ftype,f_flags);
		if( acc != null ) f.acc = acc;
		if( nm != null )
			f.name.aliases = nm.aliases;
		if( packer_size >= 0 ) {
			MetaPacker mpr = new MetaPacker();
			mpr.size = packer_size;
			f.meta.set(mpr);
			f.setPackerField(true);
		}
		f.init = f_init;
		cl.members.append(f);
//		jclazz.addMember(new JField(f));
		return f;
	}

	public Method readMethod(Method m, int index) {
		kiev.bytecode.Method bcm = bcclazz.methods[index];
		int m_flags = bcm.flags;
		KString m_name = bcm.getName(bcclazz);
		KString m_type_java = bcm.getSignature(bcclazz);
		KString m_type = m_type_java;
		Attr[] attrs = Attr.emptyArray;
		NodeName nm = null;
		Operator op = null;
		WBCCondition[] conditions = null;
		for(int i=0; i < bcm.attrs.length; i++) {
			Attr at = readAttr(bcm.attrs[i],bcclazz);
			if( at == null ) continue;
			if( at.name.equals(attrExceptions) ) {
				if( m != null )
					m.getJMethodView().addAttr(at);
				else
					attrs = (Attr[])Arrays.append(attrs,at);
			}
			else if( at.name.equals(attrRequire) || at.name.equals(attrEnsure) ) {
				WBCCondition wbc = new WBCCondition();
				if (at.name.equals(attrRequire))
					wbc.cond = WBCType.CondRequire;
				else
					wbc.cond = WBCType.CondEnsure;
				wbc.code_attr = (ContractAttr)at;
				if( m == null ) {
					if( conditions == null )
						conditions = new WBCCondition[]{wbc};
					else
						conditions = (WBCCondition[])Arrays.appendUniq(conditions,wbc);
				} else {
					wbc.definer = m;
					m.conditions.appendUniq(wbc);
				}
			}
		}
		MethodType mtype = (MethodType)Signature.getType(m_type);
		MethodType jtype = mtype;
		if( m == null ) {
//			if( (m_flags & ACC_RULEMETHOD) != 0 ) {
//				mtype = new MethodType(mtype.args,Type.tpRule);
//				m = new RuleMethod(m_name,m_flags);
//			}
//			else
			if (m_name == nameInit || m_name == nameClassInit)
				m = new Constructor(m_flags);
			else
				m = new Method(m_name,mtype.ret,m_flags);
			cl.members.append(m);
			for (int i=0; i < mtype.args.length; i++) {
				if( (m_flags & ACC_VARARGS) != 0 && i == mtype.args.length-1) {
					FormPar fp = new FormPar(new NameRef(KString.from("va_arg")),
						new TypeRef(mtype.args[i]),new TypeRef(jtype.args[i]),FormPar.PARAM_VARARGS,ACC_FINAL);
						m.params.add(fp);
						mtype = m.etype;
						break;
				} else {
					FormPar fp = new FormPar(new NameRef(KString.from("arg"+i)),
						new TypeRef(mtype.args[i]),new TypeRef(jtype.args[i]),FormPar.PARAM_NORMAL,0);
						m.params.add(fp);
				}
			}
			trace(Kiev.debugBytecodeRead,"read method "+m+" with flags 0x"+Integer.toHexString(m.getFlags()));
			if( conditions != null ) {
				m.conditions.addAll(conditions);
				for(int i=0; i < conditions.length; i++)
					m.conditions[i].definer = m;
			}
			m.getJMethodView().attrs = attrs;
		} else {
			trace(Kiev.debugBytecodeRead,"read2 method "+m+" with flags 0x"+Integer.toHexString(m.getFlags()));
		}
		if( nm != null ) {
			m.name.aliases = nm.aliases;
			if( Kiev.verbose && m.name.equals(nameArrayOp)) {
				System.out.println("Attached operator [] to method "+m);
			}
			else if( Kiev.verbose && m.name.equals(nameNewOp)) {
				System.out.println("Attached operator new to method "+m);
			}
		}
		if( op != null ) {
			Type opret = m.type.ret;
			Type oparg1, oparg2;
			Operator.iopt = null;
			switch(op.mode) {
			case Operator.LFY:
				if( m.isStatic() )
					throw new RuntimeException("Assign operator can't be static");
				else if( !m.isStatic() && m.type.args.length == 1 )
					{ oparg1 = ((Struct)m.parent).type; oparg2 = m.type.args[0]; }
				else
					throw new RuntimeException("Method "+m+" must be virtual and have 1 argument");
				if( Kiev.verbose ) System.out.println("Attached assign "+op+" to method "+m);
				Operator.iopt = new OpTypes();
				op.addTypes(otSame(1),otType(oparg1),otType(oparg2));
				break;
			case Operator.XFX:
			case Operator.YFX:
			case Operator.XFY:
			case Operator.YFY:
				if( m.isStatic() && !(m instanceof RuleMethod) && m.type.args.length == 2 )
					{ oparg1 = m.type.args[0]; oparg2 = m.type.args[1]; }
				else if( m.isStatic() && m instanceof RuleMethod && m.type.args.length == 3 )
					{ oparg1 = m.type.args[1]; oparg2 = m.type.args[2]; }
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.args.length == 1 )
					{ oparg1 = ((Struct)m.parent).type; oparg2 = m.type.args[0]; }
				else if( !m.isStatic() && m instanceof RuleMethod && m.type.args.length == 2 )
					{ oparg1 = ((Struct)m.parent).type; oparg2 = m.type.args[1]; }
				else
					throw new RuntimeException("Method "+m+" must have 2 arguments");
				if( Kiev.verbose ) System.out.println("Attached binary "+op+" to method "+m);
				Operator.iopt = new OpTypes();
				op.addTypes(otType(opret),otType(oparg1),otType(oparg2));
				break;
			case Operator.FX:
			case Operator.FY:
			case Operator.XF:
			case Operator.YF:
				if( m.isStatic() && !(m instanceof RuleMethod) && m.type.args.length == 1 )
					oparg1 = m.type.args[0];
				else if( m.isStatic() && m instanceof RuleMethod && m.type.args.length == 2 )
					oparg1 = m.type.args[1];
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.args.length == 0 )
					oparg1 = ((Struct)m.parent).type;
				else if( !m.isStatic() && !(m instanceof RuleMethod) && m.type.args.length == 1 )
					oparg1 = m.type.args[0];
				else if( !m.isStatic() && m instanceof RuleMethod && m.type.args.length == 1 )
					oparg1 = ((Struct)m.parent).type;
				else
					throw new RuntimeException("Method "+m+" must have 1 argument");
				if( Kiev.verbose ) System.out.println("Attached unary "+op+" to method "+m);
				Operator.iopt = new OpTypes();
				op.addTypes(otType(opret),otType(oparg1));
				break;
			case Operator.XFXFY:
				throw new RuntimeException("Multioperators are not supported yet");
			default:
				throw new RuntimeException("Unknown operator mode "+op.mode);
			}

			Operator.iopt.method = m;
			m.setOperatorMethod(true);
		}
		if( m.isStatic()
		 && !m.name.equals(nameClassInit)
		 && cl.package_clazz.isInterface()
		 && cl.name.short_name.equals(nameIdefault)
		)
			m.setVirtualStatic(true);
//		jclazz.addMember(new JMethod(m));
		return m;
	}

	public Attr readAttr(kiev.bytecode.Attribute bca, kiev.bytecode.Clazz clazz) {
		Attr a = null;
		KString name = bca.getName(clazz);
//		Debug.trace(true,"reading attr "+name);
		if( name.equals(attrSourceFile) ) {
			a = new SourceFileAttr(((kiev.bytecode.SourceFileAttribute)bca).getFileName(clazz));
		}
		else if( name.equals(attrExceptions) ) {
			kiev.bytecode.ExceptionsAttribute ea = (kiev.bytecode.ExceptionsAttribute)bca;
			JStructView[] exceptions = new JStructView[ea.cp_exceptions.length];
			for(int i=0; i < exceptions.length; i++) {
				exceptions[i] = Env.newStruct(ClazzName.fromBytecodeName(ea.getException(i,clazz), false)).getJStructView();
			}
			a = new ExceptionsAttr();
			((ExceptionsAttr)a).exceptions = exceptions;
		}
		else if( name.equals(attrInnerClasses) ) {
			kiev.bytecode.InnerClassesAttribute ica = (kiev.bytecode.InnerClassesAttribute)bca;
			int elen = ica.cp_inners.length;
			JStructView[] inner = new JStructView[elen];
			JStructView[] outer = new JStructView[elen];
			KString[] inner_name = new KString[elen];
			short[] access = new short[elen];
			for(int i=0; i < elen; i++) {
				try {
					ClazzName cn;
					if( ica.cp_outers[i] != null ) {
						cn = ClazzName.fromBytecodeName(ica.getOuterName(i,clazz),false);
						outer[i] = Env.getStruct(cn).getJStructView();
						if( outer[i] == null )
							throw new RuntimeException("Class "+cn+" not found");
					} else {
						outer[i] = null;
					}
					if( ica.cp_inners[i] != null ) {
						cn = ClazzName.fromBytecodeName(ica.getInnerName(i,clazz),false);
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
						if (anon || cn.package_name() != cl.name.name) {
							inner[i] == null;
						} else {
							inner[i] = Env.getStruct(cn).getJStructView();
							if( inner[i] == cl ) {
								Kiev.reportWarning("Class "+cl+" is inner for itself");
							} else {
								if( inner[i] == null )
									throw new RuntimeException("Class "+cn+" not found");
								cl.members.add((Struct)~inner[i].getStruct());
							}
						}
					} else {
						inner[i] = null;
					}
					access[i] = (short)ica.cp_inner_flags[i];
				} catch(Exception e ) {
					Kiev.reportError(e);
				}
			}
			a = new InnerClassesAttr();
			((InnerClassesAttr)a).inner = inner;
			((InnerClassesAttr)a).outer = outer;
			((InnerClassesAttr)a).acc = access;
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
		else {
			a = null; // new Attr(cl,name);
		}
		return a;
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
		KString cl_sig = cl.type.getJType().java_signature;
		trace(Kiev.debugBytecodeGen,"note: class "+cl+" class signature = "+cl_sig);
		bcclazz.cp_clazz = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(cl_sig).pos];
	    // This class's superclass name
	    if( cl.super_type != null ) {
		    KString sup_sig = cl.super_type.getJType().java_signature;
		    bcclazz.cp_super_clazz = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(sup_sig).pos];
		} else {
			bcclazz.cp_super_clazz = null;
		}

	    bcclazz.cp_interfaces = new kiev.bytecode.ClazzPoolConstant[cl.interfaces.length];
		for(int i=0; i < cl.interfaces.length; i++) {
		    KString interf_sig = cl.interfaces[i].getJType().java_signature;
			bcclazz.cp_interfaces[i] = (kiev.bytecode.ClazzPoolConstant)bcclazz.pool[constPool.getClazzCP(interf_sig).pos];
		}

		{
			Vector<kiev.bytecode.Field> flds = new Vector<kiev.bytecode.Field>(); 
			foreach (ASTNode n; cl.members; n instanceof Field) {
				Field f = (Field)n;
				if( f.isPackedField() ) continue;
				if (/*!kievmode &&*/ f.isAbstract()) continue;
				flds.append(writeField(f));
			}
			bcclazz.fields = flds.copyIntoArray();
		}

		{
			Vector<kiev.bytecode.Method> mthds = new Vector<kiev.bytecode.Method>();
			foreach (ASTNode n; cl.members; n instanceof Method) {
				Method m = (Method)n;
				mthds.append(writeMethod(m));
			}
			bcclazz.methods = mthds.copyIntoArray();
		}

	    // Number of class attributes
		JStructView jcl = cl.getJStructView();
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
		bcf.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(f.name.name).pos];
		JType tp = f.type.getJType();
		bcf.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(tp.java_signature).pos];
		bcf.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		JFieldView jf = f.getJFieldView();
		bcf.attrs = new kiev.bytecode.Attribute[jf.attrs.length];
		for(int i=0; i < jf.attrs.length; i++)
			bcf.attrs[i] = writeAttr(jf.attrs[i]);
		return bcf;
	}

    public kiev.bytecode.Method writeMethod(Method m) {
		Struct jcl = cl;
		kiev.bytecode.Method bcm = new kiev.bytecode.Method();
		bcm.flags = m.getJavaFlags();
		bcm.cp_name = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(m.name.name).pos];
		bcm.cp_type = (kiev.bytecode.Utf8PoolConstant)bcclazz.pool[constPool.getAsciiCP(m.etype.getJType().java_signature).pos];
		bcm.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		JMethodView jm = m.getJMethodView();
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

