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

package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public final class ExportJavaTop implements Constants {
	
	/////////////////////////////////////////////////////
	//												   //
	//		   PASS 1 - create top structures		   //
	//												   //
	/////////////////////////////////////////////////////
	
	public boolean pass1() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no; 
		Kiev.pass_no = TopLevelPass.passCreateTopStruct;
		try
		{
			for (int i=0; i < Kiev.file_unit.length; i++) {
				FileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try {
					pass1(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass1(ASTNode:ASTNode node) {
		return node;
	}
	
	public ASTNode pass1(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);
			FileUnit fu = astn;
			fu.disabled_extensions = astn.disabled_extensions;
			fu.bodies = astn.bodies;
			PassInfo.push(astn.pkg);
			try {
				foreach (ASTNode n; astn.members) {
					try {
						pass1(n);
					} catch(Exception e ) { Kiev.reportError(n.getPos(),e); }
				}
			} finally { PassInfo.pop(astn.pkg); }
			return astn;
		} finally { Kiev.curFile = oldfn; Kiev.setExtSet(exts); }
	}

	private void setSourceFile(Struct me) {
		if( !me.isLocal() )
			Env.setProjectInfo(me.name, Kiev.curFile);
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
	}
	
	private void setupStructType(Struct me, boolean canHaveArgs) {
		PassInfo.push(me);
		try {
			/* Then may be class arguments - they are proceed here, but their
			   inheritance - at pass2()
			*/
			Type[] targs = Type.emptyArray;
			if (!canHaveArgs) {
				if( me.args.length > 0 ) {
					Kiev.reportError(me.pos,"Type parameters are not allowed for "+me);
					me.args.delAll();
				}
			}
			else if( me.parent instanceof Struct && ((Struct)me.parent).args.length > 0 ) {
				Struct astnp = (Struct)me.parent;
				// Inner classes's argumets have to be arguments of outer classes
				for(int i=0; i < me.args.length; i++) {
					BaseStruct arg = me.args[i];
					Type[] outer_args = astnp.type.args;
					if( outer_args == null || outer_args.length <= i)
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class arguments");
					if !(outer_args[i].clazz.name.short_name.equals(arg.name.short_name))
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class argument,"
							+" but arg["+i+"] is "+arg
							+" and have to be "+outer_args[i].clazz.name.short_name);
				}
				/* Create type for class's arguments, if any */
				if( me.args.length > 0 ) {
					targs = astnp.type.args;
				}
			} else {
				for(int i=0; i < me.args.length; i++)
					targs = (Type[])Arrays.append(targs,me.args[i].type);
			}

			/* Generate type for this structure */
			me.type = Type.newRefType(me,targs);
		} finally { PassInfo.pop(me); }

	}

	public ASTNode pass1(Struct:ASTNode astn) {
		trace(Kiev.debugResolve,"Pass 1 for struct "+astn);

		Struct piclz = PassInfo.clazz;
		Struct me = astn;
		me.setResolved(true);
		setSourceFile(me);
		if (me.isEnum()) {
			if !(astn.parent instanceof FileUnit)
				me.setStatic(true);
			setupStructType(me, false);
		}
		else if (me.isPizzaCase()) {
			me.setStatic(true);
			Struct p = (Struct)me.parent;
			p.addCase(me);
			me.super_type = p.type;
			setupStructType(me, true);
		}
		else if (me.isSyntax()) {
			me.setPrivate(true);
			me.setAbstract(true);
			me.setMembersGenerated(true);
			me.setStatementsGenerated(true);
			setupStructType(me, false);
		}
		else
			setupStructType(me, true);

		PassInfo.push(me);
		try {
			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode n; me.members) {
					pass1(n);
				}
			}
		} finally { PassInfo.pop(me); }

		return me;
	}





	/////////////////////////////////////////////////////
	//												   //
	//	   PASS 1_1 - process syntax declarations	   //
	//												   //
	/////////////////////////////////////////////////////


	public boolean pass1_1() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no; 
		Kiev.pass_no = TopLevelPass.passProcessSyntax;
		try
		{
			for(int i=0; i < Kiev.file_unit.length; i++) {
				FileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try { pass1_1(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass1_1(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass1_1(ASTNode:ASTNode node) {
		return node;
	}

	public ASTNode pass1_1(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);

			// Process file imports...
			boolean java_lang_found = false;
			KString java_lang_name = KString.from("java.lang");
			boolean kiev_stdlib_found = false;
			KString kiev_stdlib_name = KString.from("kiev.stdlib");
			boolean kiev_stdlib_meta_found = false;
			KString kiev_stdlib_meta_name = KString.from("kiev.stdlib.meta");

			foreach (ASTNode n; astn.syntax) {
				try {
					if (n instanceof Import && ((Import)n).mode == Import.ImportMode.IMPORT_STATIC && !((Import)n).star)
						continue; // process later
					ASTNode sn = pass1_1(n);
					if (sn instanceof Import) {
						if( sn.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)sn.resolved).name.name.equals(java_lang_name))
							java_lang_found = true;
						else if( sn.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)sn.resolved).name.name.equals(kiev_stdlib_name))
							kiev_stdlib_found = true;
						else if( sn.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)sn.resolved).name.name.equals(kiev_stdlib_meta_name))
							kiev_stdlib_meta_found = true;
					}
					trace(Kiev.debugResolve,"Add "+sn);
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
			// Add standard imports, if they were not defined
			if( !Kiev.javaMode && !kiev_stdlib_found )
				astn.syntax.add(new Import(Env.newPackage(kiev_stdlib_name),Import.ImportMode.IMPORT_CLASS,true));
			if( !Kiev.javaMode && !kiev_stdlib_meta_found )
				astn.syntax.add(new Import(Env.newPackage(kiev_stdlib_meta_name),Import.ImportMode.IMPORT_CLASS,true));
			if( !java_lang_found )
				astn.syntax.add(new Import(Env.newPackage(java_lang_name),Import.ImportMode.IMPORT_CLASS,true));

			// Process members - pass1_1()
			foreach (ASTNode n; astn.members) {
				pass1_1(n);
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
		return astn;
	}

	public ASTNode pass1_1(Import:ASTNode astn) {
		if (astn.of_method || (astn.mode==Import.ImportMode.IMPORT_STATIC && !astn.star)) return astn;
		KString name = astn.name;
		ASTNode@ v;
		if( !PassInfo.resolveNameR(v,new ResInfo(),name,null,0) ) {
			Kiev.reportError(astn.pos,"Unresolved identifier "+name);
			return astn;
		}
		ASTNode n = v;
		if		(astn.mode == Import.ImportMode.IMPORT_CLASS && !(n instanceof Struct))
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a class or package");
		else if (astn.mode == Import.ImportMode.IMPORT_PACKAGE && !(n instanceof Struct && ((Struct)n).isPackage()))
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a package");
		else if (astn.mode == Import.ImportMode.IMPORT_STATIC && !(astn.star || (n instanceof Field)))
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a field");
		else if (astn.mode == Import.ImportMode.IMPORT_SYNTAX && !(n instanceof Struct && n.isSyntax()))
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a syntax");
		else
			astn.resolved = n;
		return astn;
	}

	public ASTNode pass1_1(Typedef:ASTNode astn) {
		try {
			if (astn.typearg != null) {
				astn.type = new TypeRef(astn.type.getType().clazz.type);
			} else {
				astn.type = new TypeRef(astn.type.getType());
			}
		} catch (RuntimeException e) { /* ignore */ }
		return astn;
	}

	public ASTNode pass1_1(Struct:ASTNode astn) {
		// Verify meta-data to the new structure
		Struct me = astn;
		me.meta.verify();
		
		if (me.isSyntax()) {
			trace(Kiev.debugResolve,"Pass 1_1 for syntax "+me);
			foreach (ASTNode n; me.members) {
				try {
					if (n instanceof Typedef) {
						n = pass1_1(n);
						if (n != null) {
							me.imported.add(n);
							trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
						}
					}
					else if (n instanceof Opdef) {
						n = pass1_1(n);
						if (n != null) {
							me.imported.add(n);
							trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
						}
					}
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
		}
		
		return me;
	}
	
	public ASTNode pass1_1(Opdef:ASTNode astn) {
		int pos = astn.pos;
		int prior = astn.prior;
		int opmode = astn.opmode;
		KString image = astn.image;
		switch(opmode) {
		case Operator.LFY:
			{
				AssignOperator op = AssignOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(pos,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(pos,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return astn;
				}
				op = AssignOperator.newAssignOperator(image,null,null,false);
				if( Kiev.verbose ) System.out.println("Declared assign operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return astn;
			}
		case Operator.XFX:
		case Operator.YFX:
		case Operator.XFY:
		case Operator.YFY:
			{
				BinaryOperator op = BinaryOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(pos,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(pos,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return astn;
				}
				op = BinaryOperator.newBinaryOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared infix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return astn;
			}
		case Operator.FX:
		case Operator.FY:
			{
				PrefixOperator op = PrefixOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(pos,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(pos,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return astn;
				}
				op = PrefixOperator.newPrefixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared prefix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return astn;
			}
		case Operator.XF:
		case Operator.YF:
			{
				PostfixOperator op = PostfixOperator.getOperator(image);
				if (op != null) {
					if (prior != op.priority)
						throw new CompilerException(pos,"Operator declaration conflict: priority "+prior+" and "+op.priority+" are different");
					if (opmode != op.mode)
						throw new CompilerException(pos,"Operator declaration conflict: "+Operator.orderAndArityNames[opmode]+" and "+Operator.orderAndArityNames[op.mode]+" are different");
					astn.resolved = op;
					return astn;
				}
				op = PostfixOperator.newPostfixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared postfix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return astn;
			}
		case Operator.XFXFY:
			throw new CompilerException(pos,"Multioperators are not supported yet");
		default:
			throw new CompilerException(pos,"Unknown operator mode "+opmode);
		}
	}






	/////////////////////////////////////////////////////
	//													//
	//	   PASS 2 - struct args inheritance			//
	//													//
	/////////////////////////////////////////////////////


	public boolean pass2() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no; 
		Kiev.pass_no = TopLevelPass.passArgumentInheritance;
		try
		{
			for(int i=0; i < Kiev.file_unit.length; i++) {
				FileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try { pass2(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass2(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass2(ASTNode:ASTNode astn) {
		return astn;
	}

	public ASTNode pass2(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);
			// Process members - pass2()
			foreach (ASTNode n; astn.members)
				pass2(n);
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
		return astn;
	}

	public ASTNode pass2(Struct:ASTNode astn) {
		Struct me = astn;
		int pos = astn.pos;
		trace(Kiev.debugResolve,"Pass 2 for class "+me);
		PassInfo.push(me);
		try {
			/* Process inheritance of class's arguments, if any */
			Type[] targs = me.type.args;
			for(int i=0; i < astn.args.length; i++) {
				BaseStruct arg = astn.args[i];
				if( arg.super_bound.isBound() ) {
					Type sup = arg.super_bound.getType();
					if( !sup.isReference() )
						Kiev.reportError(astn.pos,"Argument extends primitive type "+sup);
					else
						targs[i].clazz.super_type = sup;
					targs[i].checkJavaSignature();
				} else {
					targs[i].clazz.super_type = Type.tpObject;
				}
			}
			// Process ASTGenerete
			if( astn.gens.length > 0 ) {
				Type[][] gtypes = new Type[astn.gens.length][me.type.args.length];
				for(int l=0; l < gtypes.length; l++) {
					if !(me.gens[l] instanceof ASTNonArrayType) {
						for(int m=0; m < me.type.args.length; m++) {
							gtypes[l][m] = me.gens[l].getType().args[m];
						}
					} else {
						ASTNonArrayType ag = (ASTNonArrayType)me.gens[l];
						for(int m=0; m < me.type.args.length; m++) {
							if( ag.children[m+1] instanceof ASTPrimitiveType) {
								if( astn.args[m].super_type != Type.tpObject ) {
									Kiev.reportError(pos,"Generation for primitive type for argument "+m+" is not allowed");
								}
								gtypes[l][m] = ag.children[m+1].getType();
							} else { // ASTIdentifier
								KString a = ((ASTIdentifier)ag.children[m+1]).name;
								Type ad = me.type.args[m]; 
								if( a != ad.clazz.name.short_name ) {
									Kiev.reportError(pos,"Generation argument "+astn.name+" do not match argument "+ad);
								}
								gtypes[l][m] = me.type.args[m];
							}
						}
					}
				}
				// Clone 'me' for generated types
				for(int k=0; k < gtypes.length; k++) {
					KStringBuffer ksb;
					ksb = new KStringBuffer(
						me.name.bytecode_name.length()
						+3+me.type.args.length);
					ksb.append_fast(me.name.bytecode_name)
						.append_fast((byte)'_').append_fast((byte)'_');
					for(int l=0; l < me.type.args.length; l++) {
						if( gtypes[k][l].isReference() )
							ksb.append_fast((byte)'A');
						else
							ksb.append_fast(gtypes[k][l].signature.byteAt(0));
					}
					ksb.append_fast((byte)'_');
					ClazzName cn = ClazzName.fromBytecodeName(ksb.toKString(),false);
					Struct s = Env.newStruct(cn,true);
					s.flags = me.flags;
					s.acc = me.acc;
					Type gtype = Type.newRefType(me,gtypes[k]);
					gtype.java_signature = cn.signature();
					gtype.clazz = s;
					me.gens[k] = new TypeRef(gtype);
					s.type = gtype;
					s.generated_from = me;
					s.super_type = Type.getRealType(s.type,me.super_type);
					// Add generation for inner parametriezed classes
					for(int l=0; l < me.sub_clazz.length; l++) {
						Struct sc = me.sub_clazz[l];
						if( sc.type.args.length == 0 ) continue;
						ksb = new KStringBuffer(
							s.name.bytecode_name.length()
							+sc.name.short_name.length()
							+4+sc.type.args.length);
						ksb.append_fast(s.name.bytecode_name)
							.append_fast((byte)'$')
							.append_fast(sc.name.short_name)
							.append_fast((byte)'_').append_fast((byte)'_');
						for(int m=0; m < sc.type.args.length; m++) {
							if( Type.getRealType(gtype,sc.type.args[m]).isReference() )
								ksb.append_fast((byte)'A');
							else
								ksb.append_fast(Type.getRealType(gtype,sc.type.args[m]).signature.byteAt(0));
						}
						ksb.append_fast((byte)'_');
						cn = ClazzName.fromBytecodeName(ksb.toKString(),false);
						Struct scg = Env.newStruct(cn,true);
						scg.flags = sc.flags;
						Type scgt = Type.getRealType(gtype,sc.type);
						scgt.java_signature = cn.signature();
						scgt.clazz = scg;
						sc.gens.append(new TypeRef(scgt));
						scg.type = scgt;
						scg.generated_from = sc;
						scg.super_type = Type.getRealType(scg.type,sc.super_type);
					}
				}
			}

			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode m; astn.members)
					pass2(m);
			}
		} finally { PassInfo.pop(me); }

		return me;
	}







	/////////////////////////////////////////////////////
	//												   //
	//	   PASS 2_2 - struct inheritance			   //
	//												   //
	/////////////////////////////////////////////////////


	public boolean pass2_2() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no; 
		Kiev.pass_no = TopLevelPass.passStructInheritance;
		try
		{
			for(int i=0; i < Kiev.file_unit.length; i++) {
				FileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try { pass2_2(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass2_2(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public void pass2_2(ASTNode:ASTNode astn) {
	}

	public void pass2_2(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);
			// Process members - pass2()
			foreach (ASTNode n; astn.syntax) {
				try {
					if (n instanceof Import && ((Import)n).mode == Import.ImportMode.IMPORT_STATIC && !((Import)n).star)
						continue; // process later
					pass2_2(n);
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
			foreach (ASTNode n; astn.members)
				pass2_2(n);
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
	}

	public void pass2_2(Typedef:ASTNode astn) {
		if (astn.type == null) {
			if (astn.typearg != null) {
				astn.type = new TypeRef(astn.type.getType().clazz.type);
			} else {
				astn.type = new TypeRef(astn.type.getType());
			}
		}
	}

	public void pass2_2(Struct:ASTNode astn) {
		int pos = astn.pos;
		Struct me = astn;
		trace(Kiev.debugResolve,"Pass 2_2 for class "+me);
		PassInfo.push(me);
		try {
			/* Now, process 'extends' and 'implements' clauses */
			if( me.isAnnotation() ) {
				me.super_type = Type.tpObject;
				me.interfaces.add(new TypeRef(Type.tpAnnotation));
			}
			else if( me.isInterface() ) {
				me.super_type = Type.tpObject;
				foreach(TypeRef tr; me.interfaces)
					tr.getType();
			}
			else if( me.isEnum() ) {
				if( astn.super_type != null ) {
					me.super_type.getType();
					if( !me.super_type.isReference() ) {
						me.setPrimitiveEnum(true);
						me.type.setMeAsPrimitiveEnum();
					}
				} else {
					me.super_type = Type.tpEnum;
				}
			}
			else if( me.isSyntax() ) {
				me.super_type = null;
				if (!Kiev.packages_scanned.contains(me))
					Kiev.packages_scanned.append(me);
			}
			else if( me.isPizzaCase() ) {
				// already set
				//me.super_clazz = ((Struct)me.parent).type;
				assert (me.super_type == ((Struct)me.parent).type);
			}
			else {
				Type sup = me.super_bound.getType();
				if (sup == null && !me.name.name.equals(Type.tpObject.clazz.name.name))
					me.super_type = Type.tpObject;
				foreach(TypeRef tr; me.interfaces)
					tr.getType();
				if( me.type.args.length > 0 && !(me.type instanceof MethodType) ) {
					me.interfaces.append(new TypeRef(Type.tpTypeInfoInterface));
				}
			}
			if( me.interfaces.length > 0 && me.gens != null ) {
				for(int g=0; g < me.gens.length; g++) {
					for(int l=0; l < me.interfaces.length; l++) {
						Struct s = (Struct)me.gens[g].clazz;
						s.interfaces.add(new TypeRef(Type.getRealType(me.gens[g],me.interfaces[l])));
					}
				}
			}

			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode m; astn.members)
					pass2_2(m);
			}
		} finally { PassInfo.pop(me); }

	}






	////////////////////////////////////////////////////
	//												   //
	//	   PASS 3- struct members					   //
	//												   //
	////////////////////////////////////////////////////


	public boolean pass3() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no; 
		Kiev.pass_no = TopLevelPass.passStructInheritance;
		try
		{
			for(int i=0; i < Kiev.file_unit.length; i++) {
				FileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try { pass3(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass3(fu);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public void pass3(ASTNode:ASTNode astn) {
	}

	public void pass3(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);
			// Process members - pass3()
			foreach (ASTNode n; astn.members)
				pass3(n);
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
	}


	public void pass3(Struct:ASTNode astn) {
		int pos = astn.pos;
		Struct me = astn;
		int next_enum_val = 0;
		trace(Kiev.debugResolve,"Pass 3 for class "+me);
		if (me.isSyntax()) {
			if (!Kiev.packages_scanned.contains(me))
				Kiev.packages_scanned.append(me);
			return;
		}
		PassInfo.push(me);
		try {
			// Process members
			ASTNode[] members = astn.members.toArray();
			for(int i=0; i < members.length; i++) {
				if( members[i] instanceof ASTInitializer ) {
					ASTInitializer init = (ASTInitializer)members[i];
					// TODO: check flags for initialzer
					Initializer in = new Initializer(init.pos, init.modifiers.getFlags());
					if (init.body != null)
						in.addStatement(init.body);
					else
						in.pbody = init.pbody;
					if( me.isPackage() ) in.setStatic(true);
					init.replaceWith(in);
				}
				else if( members[i] instanceof ASTMethodDeclaration ) {
					ASTMethodDeclaration astmd = (ASTMethodDeclaration)members[i];
					Method m = (Method)((ASTMethodDeclaration)members[i]).pass3();
					if( me.isPackage() ) m.setStatic(true);
					if( m.isPrivate() ) m.setFinal(false);
					else if( me.isClazz() && me.isFinal() ) m.setFinal(true);
					else if( me.isInterface() ) {
						m.setPublic(true);
						if( astmd.pbody == null )
							m.setAbstract(true);
					}
					if( m.name.equals(nameInit) ) {
						m.setNative(false);
						m.setAbstract(false);
						m.setSynchronized(false);
						m.setFinal(false);
					}
				}
				else if( members[i] instanceof ASTRuleDeclaration ) {
					ASTRuleDeclaration astmd = (ASTRuleDeclaration)members[i];
					Method m = (Method)((ASTRuleDeclaration)members[i]).pass3();
					if( me.isPackage() ) m.setStatic(true);
					if( m.isPrivate() ) m.setFinal(true);
					if( me.isClazz() && me.isFinal() ) m.setFinal(true);
					else if( me.isInterface() ) {
						m.setPublic(true);
						if( astmd.pbody == null )
							m.setAbstract(true);
					}
				}
				else if( members[i] instanceof ASTFieldDecl ) {
					ASTFieldDecl fdecl = (ASTFieldDecl)members[i];
					// TODO: check flags for fields
					int flags = fdecl.modifiers.getFlags();
					if( me.isPackage() ) flags |= ACC_STATIC;
					if( me.isInterface() ) {
						if( (flags & ACC_VIRTUAL) != 0 ) flags |= ACC_ABSTRACT;
						else {
							flags |= ACC_STATIC;
							flags |= ACC_FINAL;
						}
						flags |= ACC_PUBLIC;
					}
					Type ftype = fdecl.type.getType();
					KString fname = fdecl.name;
					for(int k=0; k < fdecl.dim; k++) ftype = Type.newArrayType(ftype);
					Field f = new Field(me,fname,ftype,flags);
					f.setPos(fdecl.pos);
					// Attach meta-data to the new structure
					fdecl.modifiers.getMetas(f.meta);
					MetaPacked pack = f.getMetaPacked();
					if( pack != null ) {
						if( f.isStatic() ) {
							Kiev.reportWarning(fdecl.pos,"Packing of static field(s) ignored");
							f.meta.unset(pack);
						}
						else if( !ftype.isIntegerInCode() ) {
							if( ftype.clazz.instanceOf(Type.tpEnum.clazz) ) {
								Kiev.reportError(fdecl.pos,"Packing of enum is not implemented yet");
							} else {
								Kiev.reportError(fdecl.pos,"Packing of reference type is not allowed");
							}
							f.meta.unset(pack);
						} else {
							int max_pack_size = 32;
							if( ftype == Type.tpShort || ftype == Type.tpChar ) {
								max_pack_size = 16;
								if( pack.size <= 0 ) pack.size = 16;
							}
							else if( ftype == Type.tpByte ) {
								max_pack_size = 8;
								if( pack.size <= 0 ) pack.size = 8;
							}
							else if( ftype == Type.tpBoolean) {
								max_pack_size = 1;
								if( pack.size <= 0 ) pack.size = 1;
							}
							if( pack.size < 0 || pack.size > max_pack_size ) {
								Kiev.reportError(fdecl.pos,"Bad size "+pack.size+" of packed field");
								f.meta.unset(pack);
							}
							else if( pack.offset >= 0 && pack.size+pack.offset > 32) {
								Kiev.reportError(fdecl.pos,"Size+offset "+(pack.size+pack.offset)+" do not fit in 32 bit boundary");
								f.meta.unset(pack);
							}
						}
					}
					if( f.getMetaPacked() != null )
						f.setPackedField(true);
					if( me.isInterface() ) {
						if( f.isVirtual() )
							f.setAbstract(true);
						else {
							f.setStatic(true);
							f.setFinal(true);
						}
						f.setPublic(true);
					}
					fdecl.replaceWith(f);
					if (fdecl.init == null && fdecl.dim==0) {
						if(ftype.clazz.isWrapper()) {
							f.init = new NewExpr(fdecl.pos,ftype,Expr.emptyArray);
							f.setInitWrapper(true);
						}
					} else {
						if( ftype.clazz.isWrapper()) {
							if (fdecl.of_wrapper)
								f.init = fdecl.init;
							else
								f.init = new NewExpr(fdecl.pos,ftype, (fdecl.init==null)? Expr.emptyArray : new Expr[]{fdecl.init});
							f.setInitWrapper(true);
						} else {
							f.init = fdecl.init;
							f.setInitWrapper(false);
						}
					}
					if( ((ASTFieldDecl)members[i]).modifiers.acc != null )
						f.acc = ((ASTFieldDecl)members[i]).modifiers.acc;
				}
				else if (members[i] instanceof ASTEnumFieldDeclaration) {
					ASTEnumFieldDeclaration efd = (ASTEnumFieldDeclaration)members[i];
					Type me_type = me.type;
					Field f = new Field(me,efd.name.name,me_type,ACC_PUBLIC | ACC_STATIC | ACC_FINAL | ACC_ENUM);
					f.pos = efd.pos;
					f.setEnumField(true);
					efd.replaceWith(f);
					if (me.isPrimitiveEnum()) {
						if (efd.val != null) {
							if (efd.val.val instanceof Character)
								next_enum_val = ((Character)efd.val.val).charValue();
							else
								next_enum_val = ((Number)efd.val.val).intValue();
						}
						f.init = new ConstExpr(efd.pos,new Integer(next_enum_val));
					} else {
						if (efd.val != null)
							Kiev.reportError(me.pos,"Enum "+me+" is not a primitive enum");
						if (efd.text == null)
							f.init = new NewExpr(f.pos,me.type,new Expr[]{
										new ConstExpr(efd.name.pos,efd.name.name),
										new ConstExpr(efd.pos, new Integer(next_enum_val)),
										new ConstExpr(efd.name.pos,efd.name.name)
							});
						else
							f.init = new NewExpr(f.pos,me.type,new Expr[]{
										new ConstExpr(efd.name.pos,efd.name.name),
										new ConstExpr(efd.pos, new Integer(next_enum_val)),
										new ConstExpr(efd.text.pos, efd.text.val)
							});
					}
					next_enum_val++;
					if (efd.text != null)
						f.name.addAlias(KString.from("\""+efd.text.val+"\""));
				}
				else if( members[i] instanceof ASTFormalParameter) {
					PizzaCaseAttr case_attr = (PizzaCaseAttr)me.getAttr(attrPizzaCase);
					Var v = ((ASTFormalParameter)members[i]).pass3();
					Field f = new Field(me,v.name.name,v.type,ACC_PUBLIC);
					case_attr.casefields = (Field[])Arrays.append(case_attr.casefields,f);
					members[i].replaceWith(f);
				}
				else if( members[i] instanceof ASTInvariantDeclaration ) {
					((ASTInvariantDeclaration)members[i]).pass3();
				}
				// Inner classes and cases after all methods and fields, skip now
				else if( members[i] instanceof Struct );
				else if( members[i] instanceof Method );
				else if( members[i] instanceof Field );
				else if( members[i] instanceof Import ) {
					me.imported.add(members[i]);
				}
				else {
					throw new CompilerException(members[i].getPos(),"Unknown type if structure member: "+members[i]);
				}
			}

			new ProcessVirtFld().createMembers(me);
			me.setupWrappedField();
			
			// Create constructor for pizza case
			if( me.isPizzaCase() ) {
				PizzaCaseAttr case_attr = (PizzaCaseAttr)me.getAttr(attrPizzaCase);
				NArr<Type> targs = new NArr<Type>();
				foreach (Field f; case_attr.casefields)
					targs.add(f.type);
				MethodType mt = MethodType.newMethodType(Type.tpMethodClazz,null,targs.toArray(),Type.tpVoid);
				Method init = new Method(me,Constants.nameInit,mt,ACC_PUBLIC);
				init.pos = me.pos;
				init.params.add(new Var(me.pos,Constants.nameThis,me.type,0));
				foreach (Field f; case_attr.casefields)
					init.params.add(new Var(f.pos,f.name.name,f.type,0));
				me.addMethod(init);
				init.body = new BlockStat(me.pos,init);
			}

			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode n; me.members; n instanceof Struct)
					pass3(n);
			}
			// Process ASTGenerete
			for(int i=0; i < me.gens.length; i++) {
				TypeRef g = me.gens[i];
				Struct s = (Struct)g.clazz;
				s.super_type = Type.getRealType(g,me.super_type);
				s.package_clazz = me.package_clazz;
				if( me.interfaces.length != 0 ) {
					for(int j=0; j < me.interfaces.length; j++)
						s.interfaces.add(new TypeRef(Type.getRealType(s.type,me.interfaces[j])));
				}
			}
		} finally { PassInfo.pop(me); }
	}

}


