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
import kiev.transf.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.Instr.*;
import static kiev.vlang.Operator.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 1.5.2.1.2.3 $
 *
 */

public class Bytecoder implements Constants {
	public Struct								cl;
	public kiev.bytecode.Clazz					bcclazz;
	public kiev.bytecode.KievAttributeClazz		kaclazz;
	public boolean								kievmode;

	public Bytecoder(Struct cl, kiev.bytecode.Clazz bcclazz) {
		this.cl = cl;
		this.bcclazz = bcclazz;
	}

	/** Reads class from DataInputStream
	 */
	public Struct readClazz() {
		trace(Kiev.debugBytecodeRead,"Loading class "+cl);

		if( !bcclazz.getClazzName().startsWith(cl.name.bytecode_name) ) {
			throw new RuntimeException("Expected to load class "+cl.name.bytecode_name
				+" but class "+bcclazz.getClazzName()+" found");
		}
		cl.setResolved(true);
		cl.setMembersGenerated(true);
		cl.setStatementsGenerated(true);

		foreach(kiev.bytecode.Attribute kba; bcclazz.attrs; kba.getName(bcclazz)==attrKiev) {
			kaclazz = new kiev.bytecode.KievAttribute(bcclazz,kba).clazz;
			break;
		}

		if( kaclazz != null ) {
			trace(Kiev.debugBytecodeRead,"Clazz type "+kaclazz.getClazzName());
			cl.type = (BaseType)Signature.getTypeOfClazzCP(new KString.KStringScanner(kaclazz.getClazzName()));
		} else {
			trace(Kiev.debugBytecodeRead,"Clazz type "+bcclazz.getClazzName());
			cl.type = (BaseType)Signature.getTypeOfClazzCP(new KString.KStringScanner(bcclazz.getClazzName()));
		}

		// This class's superclass name (load if not loaded)
		if( bcclazz.getSuperClazzName() != null ) {
			KString cl_super_name = kaclazz==null? bcclazz.getSuperClazzName() : kaclazz.getSuperClazzName() ;
			trace(Kiev.debugBytecodeRead,"Super-class is "+cl_super_name);
		    cl.super_type = Signature.getTypeOfClazzCP(new KString.KStringScanner(cl_super_name));
			if( Env.getStruct(cl.super_type.clazz.name) == null )
				throw new RuntimeException("Class "+cl.super_type.clazz.name+" not found");
		}

		int fl = cl.getFlags();
		// Clean java flags
		fl &= ~JAVA_ACC_MASK;
		// Clean some structure flags
		fl &= ~(ACC_PACKAGE|ACC_ARGUMENT|ACC_PIZZACASE|ACC_ENUM|ACC_SYNTAX);
		fl |= bcclazz.flags;
		if ((fl & (ACC_PUBLIC | ACC_PRIVATE)) == (ACC_PUBLIC | ACC_PRIVATE)) {
			fl &= ~ACC_PRIVATE;
			fl |=  ACC_PACKAGE;
		}
		cl.setFlags( fl );

		cl.setResolved(true);
		cl.setMembersGenerated(true);
		cl.setStatementsGenerated(true);

		// Read interfaces
		KString[] interfs = kaclazz==null? bcclazz.getInterfaceNames() : kaclazz.getInterfaceNames();
		for(int i=0; i < interfs.length; i++) {
			trace(Kiev.debugBytecodeRead,"Class implements "+interfs[i]);
			Type interf = Signature.getTypeOfClazzCP(new KString.KStringScanner(interfs[i]));
			if( Env.getStruct(interf.clazz.name) == null )
				throw new RuntimeException("Class "+interf.clazz.name+" not found");
			if( !interf.isInterface() )
				throw new RuntimeException("Class "+interf+" is not an interface");
			cl.interfaces.append(new TypeRef(interf));
		}

		cl.members.delAll();
		
		for(int i=0; i < bcclazz.fields.length; i++) {
			cl.members.append(readField(null,i));
		}

		for(int i=0; i < bcclazz.methods.length; i++) {
			cl.members.append(readMethod(null,i));
		}

		kiev.bytecode.Attribute[] attrs = bcclazz.attrs;
		for(int i=0; i < attrs.length; i++) {
			Attr at = readAttr(bcclazz.attrs[i],bcclazz);
			if( at != null ) {
				cl.addAttr(at);
			}
		}
		if( kaclazz != null ) {
			attrs = kaclazz.attrs;
			for(int i=0; i < attrs.length; i++) {
				Attr at = readAttr(kaclazz.attrs[i],kaclazz);
				if( at != null ) {
					cl.addAttr(at);
					if( at.name.equals(attrFlags) ) {
						int flags = ((FlagsAttr)at).flags;
						if ((flags & 1) == 1) {
//							if (Kiev.verbose) System.out.println("Class "+cl+" is a wrapper class");
//							cl.setWrapper(true);
						}
						else if ((flags & 2) == 2) {
							if (Kiev.verbose) System.out.println("Class "+cl+" is a syntax class");
							cl.setSyntax(true);
						}
					}
					else if (at.name.equals(attrTypedef)) {
						Type type = ((TypedefAttr)at).type;
						KString name = ((TypedefAttr)at).type_name;
						Typedef td = new Typedef(0,cl,name);
						td.type = new TypeRef(type);
						cl.imported.add(td);
					}
					else if( at.name.equals(attrOperator) ) {
						Operator op = ((OperatorAttr)at).op;
						cl.imported.add(new Opdef(op));
					}
				}
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
		KString f_type = kaclazz==null? bcf.getSignature(bcclazz) : kaclazz.fields[index].getSignature(kaclazz);
		Attr[] attrs = Attr.emptyArray;
		Expr f_init = null;
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
			else if( at.name.equals(attrPackerField) ) {
				packer_size = ((PackerFieldAttr)at).size;
			}
			else if( at.name.equals(attrAlias) ) {
				nm = ((AliasAttr)at).nname;
			}
			else if( at.name.equals(attrFlags) ) {
				int flags = ((FlagsAttr)at).flags;
				if( f==null ) {
					if( (flags & 2) != 0 ) f_flags |= ACC_VIRTUAL;
					if( (flags & 8) != 0 ) f_flags |= ACC_FORWARD;
					if( (flags & 0xFF000000) != 0 ) acc = new Access(flags >>> 24);
				} else {
					f.setVirtual( (flags & 2) != 0  );
					f.setForward( (flags & 8) != 0  );
					f.acc = new Access(flags >>> 24);
				}
			}
		}
		Type ftype = Signature.getType(new KString.KStringScanner(f_type));
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
		return f;
	}

	public Method readMethod(Method m, int index) {
		kiev.bytecode.Method bcm = bcclazz.methods[index];
		int m_flags = bcm.flags;
		KString m_name = bcm.getName(bcclazz);
		KString m_type_java = bcm.getSignature(bcclazz);
		KString m_type = kaclazz==null? m_type_java : kaclazz.methods[index].getSignature(kaclazz);
		Attr[] attrs = Attr.emptyArray;
		NodeName nm = null;
		Operator op = null;
		WBCCondition[] conditions = null;
		for(int i=0; i < bcm.attrs.length; i++) {
			Attr at = readAttr(bcm.attrs[i],bcclazz);
			if( at == null ) continue;
			if( at.name.equals(attrExceptions) ) {
				if( m != null )
					m.addAttr(at);
				else
					attrs = (Attr[])Arrays.append(attrs,at);
			}
			else if( at.name.equals(attrAlias) ) {
				nm = ((AliasAttr)at).nname;
			}
			else if( at.name.equals(attrOperator) ) {
				op = ((OperatorAttr)at).op;
			}
			else if( at.name.equals(attrFlags) ) {
				int flags = ((FlagsAttr)at).flags;
				if( m==null ) {
					if( (flags & 1) != 0  ) m_flags |= ACC_MULTIMETHOD;
					if( (flags & 4) != 0  ) m_flags |= ACC_VARARGS;
					if( (flags & 16) != 0  ) m_flags |= ACC_RULEMETHOD;
					if( (flags & 32) != 0  ) m_flags |= ACC_INVARIANT_METHOD;
				} else {
					m.setMultiMethod( (flags & 1) != 0  );
					m.setVarArgs( (flags & 4) != 0  );
					m.setRuleMethod( (flags & 16) != 0  );
			    	m.setInvariantMethod( (flags & 32) != 0 );
				}
			}
			else if( at.name.equals(attrRequire) || at.name.equals(attrEnsure) ) {
				WBCCondition wbc = new WBCCondition();
				if (at.name.equals(attrRequire))
					wbc.cond = WBCType.CondRequire;
				else
					wbc.cond = WBCType.CondEnsure;
				wbc.code = (ContractAttr)at;
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
		MethodType mtype = (MethodType)Signature.getType(new KString.KStringScanner(m_type));
		MethodType jtype;
		if( kaclazz != null )
			jtype = (MethodType)Signature.getType(new KString.KStringScanner(m_type_java));
		else
			jtype = mtype;
		if( m == null ) {
			if( (m_flags & ACC_RULEMETHOD) != 0 ) {
				mtype = MethodType.newMethodType(mtype.fargs,mtype.args,Type.tpRule);
				m = new RuleMethod(m_name,mtype,m_flags);
			}
			else if (m_name == nameInit || m_name == nameClassInit)
				m = new Constructor(mtype,m_flags);
			else
				m = new Method(m_name,mtype,m_flags);
			m.parent = cl;
			for (int i=0; i < mtype.args.length; i++) {
				FormPar fp = new FormPar(new ASTIdentifier(KString.from("arg"+1)),
					new TypeRef(mtype.args[i]),new TypeRef(jtype.args[i]),0);
				m.params.add(fp);
			}
			trace(Kiev.debugBytecodeRead,"read method "+m+" with flags 0x"+Integer.toHexString(m.getFlags()));
			if( conditions != null ) {
				m.conditions.addAll(conditions);
				for(int i=0; i < conditions.length; i++)
					m.conditions[i].definer = m;
			}
			m.attrs = attrs;
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
		return m;
	}

	public Attr readAttr(kiev.bytecode.Attribute bca, kiev.bytecode.Clazz clazz) {
		Attr a = null;
		KString name = bca.getName(clazz);
//		Debug.trace(true,"reading attr "+name);
		if( name.equals(attrSourceFile) ) {
			a = new SourceFileAttr(((kiev.bytecode.SourceFileAttribute)bca).getFileName(clazz));
		}
		else if( name.equals(attrFlags) ) {
			a = new FlagsAttr(((kiev.bytecode.KievFlagsAttribute)bca).flags);
		}
		else if( name.equals(attrAlias) ) {
			NodeName nm = new NodeName(KString.Empty);
			kiev.bytecode.KievAliasAttribute aa = (kiev.bytecode.KievAliasAttribute)bca;
			for(int i=0; i < aa.cp_alias.length; i++)
				nm.addAlias( aa.getAlias(i,clazz));
			a = new AliasAttr(nm);
		}
		else if( name.equals(attrTypedef) ) {
			kiev.bytecode.KievTypedefAttribute tda = (kiev.bytecode.KievTypedefAttribute)bca;
			KString sign = tda.getType(clazz);
			KString name = tda.getTypeName(clazz);
			a = new TypedefAttr(Type.fromSignature(sign),name);
		}
		else if( name.equals(attrOperator) ) {
			kiev.bytecode.KievOperatorAttribute oa = (kiev.bytecode.KievOperatorAttribute)bca;
			int prior = oa.priority;
			KString optype = oa.getOpType(clazz);
			KString image = oa.getImage(clazz);
			int opmode = -1;
			for(int i=0; i < Operator.orderAndArityNames.length; i++) {
				if( Operator.orderAndArityNames[i].equals(optype) ) {
					opmode = i;
					break;
				}
			}
			if( opmode < 0 )
				throw new RuntimeException("Operator mode must be one of "+Arrays.toString(Operator.orderAndArityNames));
			Operator op = null;
			switch(opmode) {
			case Operator.LFY:
				op = AssignOperator.newAssignOperator(image,KString.Empty,null,false);
				break;
			case Operator.XFX:
			case Operator.YFX:
			case Operator.XFY:
			case Operator.YFY:
				op = BinaryOperator.newBinaryOperator(prior,image,KString.Empty,null,optype,false);
				break;
			case Operator.FX:
			case Operator.FY:
				op = PrefixOperator.newPrefixOperator(prior,image,KString.Empty,null,optype,false);
				break;
			case Operator.XF:
			case Operator.YF:
				op = PostfixOperator.newPostfixOperator(prior,image,KString.Empty,null,optype,false);
				break;
			case Operator.XFXFY:
				throw new RuntimeException("Multioperators are not supported yet");
			default:
				throw new RuntimeException("Unknown operator mode "+opmode);
			}
			a = new OperatorAttr(op);
		}
		else if( name.equals(attrPizzaCase) ) {
			kiev.bytecode.KievCaseAttribute kca = (kiev.bytecode.KievCaseAttribute)bca;
			int caseno = kca.caseno;
			int casefieldsno = kca.cp_casefields.length;
			Field[] casefields = new Field[casefieldsno];
			for(int j=0; j < casefieldsno; j++) {
				kiev.bytecode.NameAndTypePoolConstant nat =
					(kiev.bytecode.NameAndTypePoolConstant)clazz.pool[kca.cp_casefields[j]];
				KString f_name = ((kiev.bytecode.Utf8PoolConstant)clazz.pool[nat.ref_name]).value;
				foreach (ASTNode n; cl.members; n instanceof Field) {
					Field f = (Field)n;
					if( f.name.equals(f_name) ) {
						casefields[j] = f;
						break;
					}
				}
			}
			cl.setPizzaCase(true);
			cl.super_type.clazz.setHasCases(true);
			a = new PizzaCaseAttr();
			((PizzaCaseAttr)a).caseno = caseno;
			((PizzaCaseAttr)a).casefields = casefields;
		}
		else if( name.equals(attrExceptions) ) {
			kiev.bytecode.ExceptionsAttribute ea = (kiev.bytecode.ExceptionsAttribute)bca;
			Type[] exceptions = new Type[ea.cp_exceptions.length];
			for(int i=0; i < exceptions.length; i++) {
				exceptions[i] = Type.newRefType(
					ClazzName.fromBytecodeName( ea.getException(i,clazz), false ));
			}
			a = new ExceptionsAttr();
			((ExceptionsAttr)a).exceptions = exceptions;
		}
		else if( name.equals(attrEnum) ) {
			cl.setEnum(true);
			kiev.bytecode.KievEnumAttribute ea = (kiev.bytecode.KievEnumAttribute)bca;
			Vector<Field> vf = new Vector<Field>();
			int i = 0;
			foreach (ASTNode n; cl.members; n instanceof Field && n.isEnumField()) {
				Field f = (Field)n;
				// Values and fields must be in the same order, as fields of struct
				if( ea.getFieldName(i,clazz) != f.name.name )
					throw new RuntimeException("Invalid entry "+i+" in "+attrEnum+" attribute");
				vf.append(f);
				i++;
			}
			a = new EnumAttr(vf.copyIntoArray(),ea.values);
		}
		else if( name.equals(attrPackerField) ) {
			a = new PackerFieldAttr(((kiev.bytecode.KievPackerFieldAttribute)bca).size);
		}
		else if( name.equals(attrPackedFields) ) {
			kiev.bytecode.KievPackedFieldsAttribute pf = (kiev.bytecode.KievPackedFieldsAttribute)bca;
			for(int i=0; i < pf.fields.length; i++) {
				Field f = new Field(
					pf.getFieldName(i,clazz),
					Signature.getType(new KString.KStringScanner(pf.getSignature(i,clazz))),
					ACC_PUBLIC
					);
				cl.addField(f);
				MetaPacked mp = new MetaPacked();
				mp.size = pf.sizes[i];
				mp.offset = pf.offsets[i];
				mp.packer = cl.resolveField(pf.getPackerName(i,clazz));
				mp.fld = mp.packer.name.name;
				f.meta.set(mp);
				f.setPackedField(true);
			}
			a = null;
		}
		else if( name.equals(attrInnerClasses) ) {
			kiev.bytecode.InnerClassesAttribute ica = (kiev.bytecode.InnerClassesAttribute)bca;
			int elen = ica.cp_inners.length;
			Struct[] inner = new Struct[elen];
			Struct[] outer = new Struct[elen];
			KString[] inner_name = new KString[elen];
			short[] access = new short[elen];
			for(int i=0; i < elen; i++) {
				try {
					ClazzName cn;
					if( ica.cp_outers[i] != 0 ) {
						cn = ClazzName.fromBytecodeName(ica.getOuterName(i,clazz),false);
						outer[i] = Env.getStruct(cn);
						if( outer[i] == null )
							throw new RuntimeException("Class "+cn+" not found");
					} else {
						outer[i] = null;
					}
					if( ica.cp_inners[i] != 0 ) {
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
						if (anon) {
							inner[i] == null;
						} else {
							inner[i] = Env.getStruct(cn);
							if( inner[i] == null )
								throw new RuntimeException("Class "+cn+" not found");
							cl.members.add(inner[i]);
						}
					} else {
						inner[i] = null;
					}
//					if( inner[i] == null ) {
//						cn = ClazzName.fromBytecodeName(((AsciiCP)pool[nm]).value);
//						inner[i] = Env.getStruct(cn);
//						if( inner[i] == null )
//							throw new RuntimeException("Class "+cn+" not found");
//					}
					access[i] = (short)ica.cp_inner_flags[i];
				} catch(Exception e ) {
					Kiev.reportError(0,e);
				}
			}
			a = new InnerClassesAttr();
			((InnerClassesAttr)a).inner = inner;
			((InnerClassesAttr)a).outer = outer;
			((InnerClassesAttr)a).acc = access;
		}
		else if( name.equals(attrImport) ) {
			kiev.bytecode.KievImportAttribute kia = (kiev.bytecode.KievImportAttribute)bca;
			KString clname = kia.getClazzName(clazz);
			Struct s = Env.getStruct(ClazzName.fromBytecodeName(clname,false));
			if( s == null )
				Kiev.reportWarning(0,"Bytecode imports a member from unknown class "+clname);
			else if( Kiev.passLessThen(TopLevelPass.passResolveImports) ) {
				Import imp = new Import();
				if( clazz.pool[kia.cp_ref] instanceof kiev.bytecode.FieldPoolConstant ) {
					imp.name = new ASTIdentifier(KString.from(s.name.name+"."+kia.getNodeName(clazz)));
				} else {
					imp.name = new ASTIdentifier(KString.from(s.name.name+"."+kia.getNodeName(clazz)));
					imp.of_method = true;
					KString sig = kia.getSignature(clazz);
					MethodType mt = (MethodType)Signature.getType(new KString.KStringScanner(sig));
					foreach (Type t; mt.args)
						imp.args.append(new TypeRef(t));
				}
				cl.imported.add(imp);
			} else {
				ASTNode node;
				if( clazz.pool[kia.cp_ref] instanceof kiev.bytecode.FieldPoolConstant ) {
					node = s.resolveName(kia.getNodeName(clazz));
				} else {
					node = s.resolveMethod(kia.getNodeName(clazz),kia.getSignature(clazz));
				}
				if( node == null )
					Kiev.reportWarning(0,"Package bytecode imports unknown method / field "+
						kia.getNodeName(clazz)+" "+kia.getSignature(clazz)+" from class "+s);
				cl.imported.add(node);
			}
			a = null;
		}
		else if( name.equals(attrConstantValue) ) {
			kiev.bytecode.ConstantValueAttribute ca = (kiev.bytecode.ConstantValueAttribute)bca;
			kiev.bytecode.PoolConstant cav = clazz.pool[ca.cp_value];
			ConstExpr ce;
			if( cav instanceof kiev.bytecode.NumberPoolConstant )
				ce = ConstExpr.fromConst(cav.getValue());
			else if( cav instanceof kiev.bytecode.StringPoolConstant ) {
				kiev.bytecode.StringPoolConstant sc = (kiev.bytecode.StringPoolConstant)cav;
				ce = new ConstStringExpr(((kiev.bytecode.Utf8PoolConstant)clazz.pool[sc.ref]).value);
			} else
				throw new RuntimeException("Bad ConstantValue attribute: "+cav.getClass());
			a = new ConstantValueAttr(ce);
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
			while( pc < ca.code.length ) {
				cp = 0;
				trace(Kiev.debugBytecodeRead,pc+": opc: "+Constants.opcNames[0xFF&ca.code[pc]]);
				switch( 0xFF & ca.code[pc] ) {
				case opc_ldc:
					cp = 0xFF & ca.code[pc+1];
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
					cp = ((0xFF & ca.code[pc+1]) << 8) | (0xFF & ca.code[pc+2]);
					break;
				default:
					pc += Constants.opcLengths[0xFF & ca.code[pc]];
					continue;
				}
				if( constants_top == constants.length ) {
					constants = (CP[])Arrays.ensureSize(constants,constants.length*2);
					constants_pc = (int[])Arrays.ensureSize(constants_pc,constants.length);
				}
				trace(Kiev.debugBytecodeRead,pc+": CP: "+cp);
				constants_pc[constants_top] = pc+1;
				kiev.bytecode.PoolConstant pcp = clazz.pool[cp];
				switch(pcp) {
				case kiev.bytecode.ClazzPoolConstant:
					{
					kiev.bytecode.Utf8PoolConstant upc = (kiev.bytecode.Utf8PoolConstant)
						clazz.pool[((kiev.bytecode.ClazzPoolConstant)pcp).ref];
					constants[constants_top] = new ClazzCP(new AsciiCP(upc.value));
					}
					break;
				case kiev.bytecode.StringPoolConstant:
					{
					kiev.bytecode.Utf8PoolConstant upc = (kiev.bytecode.Utf8PoolConstant)
						clazz.pool[((kiev.bytecode.StringPoolConstant)pcp).ref];
					constants[constants_top] = new StringCP(new AsciiCP(upc.value));
					}
					break;
				case kiev.bytecode.FieldPoolConstant:
					{
					kiev.bytecode.ClazzPoolConstant clpc = (kiev.bytecode.ClazzPoolConstant)
						clazz.pool[((kiev.bytecode.FieldPoolConstant)pcp).ref_clazz];
					kiev.bytecode.NameAndTypePoolConstant ntpc = (kiev.bytecode.NameAndTypePoolConstant)
						clazz.pool[((kiev.bytecode.FieldPoolConstant)pcp).ref_nametype];
					constants[constants_top] = new FieldCP(
						new ClazzCP(new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[clpc.ref]).value)),
						new NameTypeCP(
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_name]).value),
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_type]).value))
					);
					}
					break;
				case kiev.bytecode.MethodPoolConstant:
					{
					kiev.bytecode.ClazzPoolConstant clpc = (kiev.bytecode.ClazzPoolConstant)
						clazz.pool[((kiev.bytecode.MethodPoolConstant)pcp).ref_clazz];
					kiev.bytecode.NameAndTypePoolConstant ntpc = (kiev.bytecode.NameAndTypePoolConstant)
						clazz.pool[((kiev.bytecode.MethodPoolConstant)pcp).ref_nametype];
					constants[constants_top] = new MethodCP(
						new ClazzCP(new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[clpc.ref]).value)),
						new NameTypeCP(
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_name]).value),
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_type]).value))
					);
					}
					break;
				case kiev.bytecode.InterfaceMethodPoolConstant:
					{
					kiev.bytecode.ClazzPoolConstant clpc = (kiev.bytecode.ClazzPoolConstant)
						clazz.pool[((kiev.bytecode.InterfaceMethodPoolConstant)pcp).ref_clazz];
					kiev.bytecode.NameAndTypePoolConstant ntpc = (kiev.bytecode.NameAndTypePoolConstant)
						clazz.pool[((kiev.bytecode.InterfaceMethodPoolConstant)pcp).ref_nametype];
					constants[constants_top] = new InterfaceMethodCP(
						new ClazzCP(new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[clpc.ref]).value)),
						new NameTypeCP(
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_name]).value),
							new AsciiCP(((kiev.bytecode.Utf8PoolConstant)clazz.pool[ntpc.ref_type]).value))
					);
					}
					break;
				case kiev.bytecode.NumberPoolConstant:
					constants[constants_top] = new NumberCP(
						((kiev.bytecode.NumberPoolConstant)pcp).getValue());
					break;
				default:
					throw new RuntimeException("Bad pool constant "+clazz.pool[cp]+" of opcode "+Constants.opcNames[0xFF&ca.code[pc]]);
				}
				trace(Kiev.debugBytecodeRead,pc+": CP: "+constants[constants_top]);
				constants_top++;
				pc += Constants.opcLengths[0xFF & ca.code[pc]];
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
		Struct jcl = cl;
		if( kievmode ) {
		    bcclazz = new kiev.bytecode.KievAttributeClazz(null);
		    ((kiev.bytecode.KievAttributeClazz)bcclazz).pool_offset = ConstPool.java_hwm;
		} else {
		    bcclazz = new kiev.bytecode.Clazz();
		}

	    // Constant pool
		bcclazz.pool = writeConstPool();

	    // Access bitflags
	    if( !cl.isInterface() && !cl.isArgument() )
	    	cl.setSuper(true);
		bcclazz.flags = cl.getJavaFlags();

		// This class name
		KString cl_sig;
		if (kievmode)
			cl_sig = jcl.type.signature;
		else
			cl_sig = jcl.type.java_signature;
		trace(Kiev.debugBytecodeGen,"note: class "+cl+" class signature = "+cl_sig);
		bcclazz.cp_clazz = ConstPool.getClazzCP(cl_sig).pos;
	    // This class's superclass name
	    if( cl.super_type != null ) {
		    KString sup_sig = kievmode ?
					jcl.super_type.signature
				  : jcl.super_type.java_signature;
		    bcclazz.cp_super_clazz = ConstPool.getClazzCP(sup_sig).pos;
		} else {
			bcclazz.cp_super_clazz = 0;
		}

	    bcclazz.cp_interfaces = new int[cl.interfaces.length];
		for(int i=0; i < cl.interfaces.length; i++) {
		    KString interf_sig = kievmode ?
				jcl.interfaces[i].signature
			  : jcl.interfaces[i].java_signature;
			bcclazz.cp_interfaces[i] = ConstPool.getClazzCP(interf_sig).pos;
		}

		{
			Vector<kiev.bytecode.Field> flds = new Vector<kiev.bytecode.Field>(); 
			foreach (ASTNode n; cl.members; n instanceof Field) {
				Field f = (Field)n;
				if( f.isPackedField() ) continue;
				if (!kievmode && f.isAbstract()) continue;
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
	    if( !kievmode ) {
	    	int len = 0;
	    	foreach(Attr a; cl.attrs; !a.isKiev) len++;
		    bcclazz.attrs = new kiev.bytecode.Attribute[len];
			for(int i=0, j=0; i < cl.attrs.length; i++) {
				if( cl.attrs[i].isKiev ) continue;
				bcclazz.attrs[j++] = writeAttr(cl.attrs[i]);
			}
		} else {
	    	int len = 0;
	    	foreach(Attr a; cl.attrs; a.isKiev) len++;
		    bcclazz.attrs = new kiev.bytecode.Attribute[len];
			for(int i=0, j=0; i < cl.attrs.length; i++) {
				if( !cl.attrs[i].isKiev ) continue;
				bcclazz.attrs[j++] = writeAttr(cl.attrs[i]);
			}
		}
		return bcclazz.writeClazz();
	}

	public kiev.bytecode.PoolConstant[] writeConstPool() {
		// Write constant pool
		for(int i=0; i < ConstPool.hwm; i++)
			if( ConstPool.pool[i] != null )
				ConstPool.pool[i].pos = i;
		int hwm, lwm;
		kiev.bytecode.PoolConstant[] bcpool;
		if( kievmode ) {
			hwm = ConstPool.hwm;
			lwm = ConstPool.java_hwm;
		} else {
			hwm = ConstPool.java_hwm;
			lwm = 1;
			bcpool = new kiev.bytecode.PoolConstant[hwm];
		}
		bcpool = new kiev.bytecode.PoolConstant[hwm];
		bcpool[0] = new kiev.bytecode.VoidPoolConstant();
		for(int i=1; i < hwm; i++) {
			CP c = ConstPool.pool[i];
			switch( c ) {
			case AsciiCP:
				bcpool[i] = new kiev.bytecode.Utf8PoolConstant();
				((kiev.bytecode.Utf8PoolConstant)bcpool[i]).value = ((AsciiCP)c).value;
				continue;
			case NumberCP:
			{
				NumberCP num = (NumberCP)c;
				switch( num.value ) {
				case Float:
					bcpool[i] = new kiev.bytecode.FloatPoolConstant();
					((kiev.bytecode.FloatPoolConstant)bcpool[i]).value = num.value.floatValue();
					continue;
				case Long:
					bcpool[i] = new kiev.bytecode.LongPoolConstant();
					((kiev.bytecode.LongPoolConstant)bcpool[i]).value = num.value.longValue();
					bcpool[++i] = new kiev.bytecode.VoidPoolConstant();
					continue;
				case Double:
					bcpool[i] = new kiev.bytecode.DoublePoolConstant();
					((kiev.bytecode.DoublePoolConstant)bcpool[i]).value = num.value.doubleValue();
					bcpool[++i] = new kiev.bytecode.VoidPoolConstant();
					continue;
				}
				bcpool[i] = new kiev.bytecode.IntegerPoolConstant();
				((kiev.bytecode.IntegerPoolConstant)bcpool[i]).value = num.value.intValue();
				continue;
			}
			case ClazzCP:
				bcpool[i] = new kiev.bytecode.ClazzPoolConstant();
				((kiev.bytecode.ClazzPoolConstant)bcpool[i]).ref = ((ClazzCP)c).asc.pos;
				continue;
			case StringCP:
				bcpool[i] = new kiev.bytecode.StringPoolConstant();
				((kiev.bytecode.StringPoolConstant)bcpool[i]).ref = ((StringCP)c).asc.pos;
				continue;
			case FieldCP:
				bcpool[i] = new kiev.bytecode.FieldPoolConstant();
				((kiev.bytecode.FieldPoolConstant)bcpool[i]).ref_clazz = ((FieldCP)c).clazz_cp.pos;
				((kiev.bytecode.FieldPoolConstant)bcpool[i]).ref_nametype = ((FieldCP)c).nt_cp.pos;
				continue;
			case MethodCP:
				bcpool[i] = new kiev.bytecode.MethodPoolConstant();
				((kiev.bytecode.MethodPoolConstant)bcpool[i]).ref_clazz = ((MethodCP)c).clazz_cp.pos;
				((kiev.bytecode.MethodPoolConstant)bcpool[i]).ref_nametype = ((MethodCP)c).nt_cp.pos;
				continue;
			case InterfaceMethodCP:
				bcpool[i] = new kiev.bytecode.InterfaceMethodPoolConstant();
				((kiev.bytecode.InterfaceMethodPoolConstant)bcpool[i]).ref_clazz = ((InterfaceMethodCP)c).clazz_cp.pos;
				((kiev.bytecode.InterfaceMethodPoolConstant)bcpool[i]).ref_nametype = ((InterfaceMethodCP)c).nt_cp.pos;
				continue;
			case NameTypeCP:
				bcpool[i] = new kiev.bytecode.NameAndTypePoolConstant();
				((kiev.bytecode.NameAndTypePoolConstant)bcpool[i]).ref_name = ((NameTypeCP)c).name_cp.pos;
				((kiev.bytecode.NameAndTypePoolConstant)bcpool[i]).ref_type = ((NameTypeCP)c).type_cp.pos;
				continue;
			default:
				throw new RuntimeException("Unknown tag in ConstantPool "+ConstPool.pool[i]);
			}
		}
		return bcpool;
	}

	public kiev.bytecode.Field writeField(Field f) {
		kiev.bytecode.Field bcf = new kiev.bytecode.Field();
		bcf.flags = f.getJavaFlags();
		bcf.cp_name = ConstPool.getAsciiCP(f.name.name).pos;
		if( !kievmode ) {
			Type tp = f.type;
			bcf.cp_type = ConstPool.getAsciiCP(tp.java_signature).pos;
		}
		else
			bcf.cp_type = ConstPool.getAsciiCP(f.type.signature).pos;
		bcf.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		if( !kievmode ) {
			bcf.attrs = new kiev.bytecode.Attribute[f.attrs.length];
			for(int i=0; i < f.attrs.length; i++)
				bcf.attrs[i] = writeAttr(f.attrs[i]);
		} else {
			for(int i=0; i < f.attrs.length; i++) {
				if( f.attrs[i].name.equals(attrFlags) ) {
					bcf.attrs = new kiev.bytecode.Attribute[]{writeAttr(f.attrs[i])};
					break;
				}
			}
		}
		return bcf;
	}

    public kiev.bytecode.Method writeMethod(Method m) {
		Struct jcl = cl;
		kiev.bytecode.Method bcm = new kiev.bytecode.Method();
		bcm.flags = m.getJavaFlags();
		bcm.cp_name = ConstPool.getAsciiCP(m.name.name).pos;
		if( !kievmode )
			bcm.cp_type = ConstPool.getAsciiCP(m.jtype.java_signature).pos;
		else
			bcm.cp_type = ConstPool.getAsciiCP(m.type.signature).pos;
		bcm.attrs = kiev.bytecode.Attribute.emptyArray;
		// Number of type attributes
		if( !kievmode ) {
			bcm.attrs = new kiev.bytecode.Attribute[m.attrs.length];
			for(int i=0; i < m.attrs.length; i++)
				bcm.attrs[i] = writeAttr(m.attrs[i]);
		} else {
			for(int i=0; i < m.attrs.length; i++) {
				if( m.attrs[i].name.equals(attrFlags) ) {
					bcm.attrs = new kiev.bytecode.Attribute[]{writeAttr(m.attrs[i])};
					break;
				}
			}
		}
		return bcm;
    }

	public kiev.bytecode.Attribute writeAttr(Attr a) {
		kiev.bytecode.Attribute kba = a.write();
		return kba;
	}
}
