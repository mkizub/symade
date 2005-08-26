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

public final class ExportJavaTop extends TransfProcessor implements Constants {
	
	public ExportJavaTop(Kiev.Ext ext) {
		super(ext);
	}
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
	
	public void pass1(ASTNode:ASTNode node) {
		return;
	}
	
	public void pass1(FileUnit:ASTNode astn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		boolean[] exts = Kiev.getExtSet();
		try {
			Kiev.setExtSet(astn.disabled_extensions);
			FileUnit fu = astn;
			fu.disabled_extensions = astn.disabled_extensions;
			fu.bodies = astn.bodies;
			PassInfo.push(fu);
			PassInfo.push(fu.pkg.clazz);
			try {
				foreach (ASTNode n; astn.members) {
					try {
						pass1(n);
					} catch(Exception e ) { Kiev.reportError(n.getPos(),e); }
				}
			} finally { PassInfo.pop(fu.pkg.clazz); PassInfo.pop(fu); }
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
				// Inner classes's arguments have to be arguments of outer classes
				// BUG BUG BUG - need to follow java scheme
				for(int i=0; i < me.args.length; i++) {
					TypeArgRef arg = me.args[i];
					Type[] outer_args = astnp.type.args;
					if( outer_args == null || outer_args.length <= i)
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class arguments");
					if !(outer_args[i].getClazzName().short_name.equals(arg.name.name))
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class argument,"
							+" but arg["+i+"] is "+arg
							+" and have to be "+outer_args[i].getClazzName().short_name);
				}
				/* Create type for class's arguments, if any */
				if( me.args.length > 0 ) {
					targs = astnp.type.args;
				}
			} else {
				for(int i=0; i < me.args.length; i++)
					targs = (Type[])Arrays.append(targs,me.args[i].getType());
			}

			/* Generate type for this structure */
			me.type = Type.newRefType(me,targs);
		} finally { PassInfo.pop(me); }

	}

	public void pass1(Struct:ASTNode astn) {
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

		// assign type of enum fields
		if (me.isEnum()) {
			foreach (ASTNode n; me.members; n instanceof Field && n.isEnumField()) {
				Field f = (Field)n;
				f.ftype = new TypeRef(me.type);
			}
		}
		
		if( !me.isPackage() ) {
			PassInfo.push(me);
			try {
				// Process inner classes and cases
				foreach (ASTNode n; me.members) {
					pass1(n);
				}
			} finally { PassInfo.pop(me); }
		}
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
	
	public void pass1_1(ASTNode:ASTNode node) {
		return;
	}

	public void pass1_1(FileUnit:ASTNode astn) {
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
					pass1_1(n);
					if (n instanceof Import) {
						if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(java_lang_name))
							java_lang_found = true;
						else if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(kiev_stdlib_name))
							kiev_stdlib_found = true;
						else if( n.mode == Import.ImportMode.IMPORT_CLASS && ((Struct)n.resolved).name.name.equals(kiev_stdlib_meta_name))
							kiev_stdlib_meta_found = true;
					}
					trace(Kiev.debugResolve,"Add "+n);
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
	}

	public void pass1_1(Import:ASTNode astn) {
		if (astn.of_method || (astn.mode==Import.ImportMode.IMPORT_STATIC && !astn.star)) return;
		KString name = astn.name.name;
		ASTNode@ v;
		if( !PassInfo.resolveNameR(v,new ResInfo(),name) ) {
			Kiev.reportError(astn.pos,"Unresolved identifier "+name);
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
	}

	public void pass1_1(Typedef:ASTNode astn) {
		try {
			if (astn.typearg != null) {
				astn.type = new TypeRef(astn.type.getType().getInitialType());
			} else {
				astn.type = new TypeRef(astn.type.getType());
			}
		} catch (RuntimeException e) { /* ignore */ }
	}

	public void pass1_1(Struct:ASTNode astn) {
		// Verify meta-data to the new structure
		Struct me = astn;
		me.meta.verify();
		
		if (me.isSyntax()) {
			trace(Kiev.debugResolve,"Pass 1_1 for syntax "+me);
			for (int i=0; i < me.members.length; i++) {
				ASTNode n = me.members[i];
				try {
					if (n instanceof Typedef) {
						pass1_1(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
					else if (n instanceof Opdef) {
						pass1_1(n);
						me.imported.add(me.members[i]);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+me);
					}
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
		}
	}
	
	public void pass1_1(Opdef:ASTNode astn) {
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
					return;
				}
				op = AssignOperator.newAssignOperator(image,null,null,false);
				if( Kiev.verbose ) System.out.println("Declared assign operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
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
					return;
				}
				op = BinaryOperator.newBinaryOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared infix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
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
					return;
				}
				op = PrefixOperator.newPrefixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared prefix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
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
					return;
				}
				op = PostfixOperator.newPostfixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared postfix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				astn.resolved = op;
				return;
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
	
	public void pass2(ASTNode:ASTNode astn) {
		return;
	}

	public void pass2(FileUnit:ASTNode astn) {
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
	}

	public void pass2(Struct:ASTNode astn) {
		Struct me = astn;
		int pos = astn.pos;
		trace(Kiev.debugResolve,"Pass 2 for class "+me);
		PassInfo.push(me);
		try {
			/* Process inheritance of class's arguments, if any */
			Type[] targs = me.type.args;
			for(int i=0; i < astn.args.length; i++) {
				TypeArgRef arg = astn.args[i];
				if( arg.super_bound != null ) {
					Type sup = arg.super_bound.getType();
					if( !sup.isReference() ) {
						Kiev.reportError(astn.pos,"Argument extends bad type "+sup);
					} else {
						((ArgumentType)arg.getType()).super_type = sup;
					}
					targs[i].checkJavaSignature();
//				} else {
//					targs[i].clazz.super_type = Type.tpObject;
				}
			}

			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode m; astn.members)
					pass2(m);
			}
		} finally { PassInfo.pop(me); }
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
				me.super_type = Type.tpEnum;
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
				if( me.type.args.length > 0 && !me.type.isInstanceOf(Type.tpClosure) ) {
					me.interfaces.append(new TypeRef(Type.tpTypeInfoInterface));
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
				if( members[i] instanceof Initializer ) {
					Initializer init = (Initializer)members[i];
					// TODO: check flags for initialzer
					if( me.isPackage() ) init.setStatic(true);
				}
				else if( members[i] instanceof RuleMethod ) {
					RuleMethod m = (RuleMethod)members[i];
					m.pass3();
					if( me.isPackage() ) m.setStatic(true);
					if( m.isPrivate() ) m.setFinal(true);
					if( me.isClazz() && me.isFinal() ) m.setFinal(true);
					else if( me.isInterface() ) 	m.setPublic(true);
					m.acc.verifyAccessDecl(m);
				}
				else if( members[i] instanceof Method ) {
					Method m = (Method)members[i];
					m.pass3();
					if( me.isPackage() ) m.setStatic(true);
					if( m.isPrivate() ) m.setFinal(false);
					else if( me.isClazz() && me.isFinal() ) m.setFinal(true);
					else if( me.isInterface() ) {
						m.setPublic(true);
					}
					if( m.name.equals(nameInit) ) {
						m.setNative(false);
						m.setAbstract(false);
						m.setSynchronized(false);
						m.setFinal(false);
					}
					m.acc.verifyAccessDecl(m);
				}
				else if (members[i] instanceof Field && me.isPizzaCase()) {
					Field f = (Field)members[i];
					PizzaCaseAttr case_attr = (PizzaCaseAttr)me.getAttr(attrPizzaCase);
					case_attr.casefields = (Field[])Arrays.append(case_attr.casefields,f);
				}
				else if (members[i] instanceof Field && members[i].isEnumField()) {
					Field f = (Field)members[i];
					KString text = f.name.name;
					MetaAlias al = f.getMetaAlias();
					if (al != null) {
						foreach (ASTNode n; al.getAliases(); n instanceof ConstStringExpr) {
							KString nm = ((ConstStringExpr)n).value;
							if (nm.len > 2 && nm.byteAt(0) == '\"') {
								f.name.addAlias(nm);
								text = nm.substr(1,nm.len-2);
								break;
							}
						}
					}
					f.init = new NewExpr(f.pos,me.type,new Expr[]{
								new ConstStringExpr(f.name.name),
								new ConstIntExpr(next_enum_val),
								new ConstStringExpr(text)
					});
					next_enum_val++;
				}
				else if( members[i] instanceof Field ) {
					Field fdecl = (Field)members[i];
					Field f = fdecl;
					f.meta.verify();
					// TODO: check flags for fields
					if( me.isPackage() )
						f.setStatic(true);
					if( me.isInterface() ) {
						if (f.isVirtual()) {
							f.setAbstract(true);
						} else {
							f.setStatic(true);
							f.setFinal(true);
						}
						f.setPublic(true);
					}
					f.acc.verifyAccessDecl(f); // recheck access
					Type ftype = fdecl.type;
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
					if (fdecl.init == null && !ftype.isArray()) {
						if(ftype.isWrapper()) {
							f.init = new NewExpr(fdecl.pos,ftype,Expr.emptyArray);
							f.setInitWrapper(true);
						}
					} else {
						if( ftype.isWrapper()) {
							if (fdecl.isInitWrapper())
								f.init = fdecl.init;
							else
								f.init = new NewExpr(fdecl.pos,ftype, (fdecl.init==null)? ENode.emptyArray : new ENode[]{fdecl.init});
							f.setInitWrapper(true);
						} else {
							f.init = fdecl.init;
							f.setInitWrapper(false);
						}
					}
				}
				else if( members[i] instanceof WBCCondition ) {
					WBCCondition inv = (WBCCondition)members[i];
					assert(inv.cond == WBCType.CondInvariant);
					// TODO: check flags for fields
					MethodType mt = MethodType.newMethodType(null,Type.emptyArray,Type.tpVoid);
					Method m = new Method(inv.name.name,mt,inv.flags);
					m.setInvariantMethod(true);
					inv.replaceWithNode(m);
					m.body = inv;
				}
				// Inner classes and cases after all methods and fields, skip now
				else if( members[i] instanceof Struct );
				else if( members[i] instanceof Import ) {
					me.imported.add(members[i]);
				}
				else {
					throw new CompilerException(members[i].getPos(),"Unknown type if structure member: "+members[i]);
				}
			}

			{
				ProcessVirtFld tp = (ProcessVirtFld)Kiev.getProcessor(Kiev.Ext.VirtualFields);
				if (tp != null)
					tp.createMembers(me);
			}
			
			// Create constructor for pizza case
			if( me.isPizzaCase() ) {
				PizzaCaseAttr case_attr = (PizzaCaseAttr)me.getAttr(attrPizzaCase);
				Vector<Type> targs = new Vector<Type>();
				foreach (Field f; case_attr.casefields)
					targs.append(f.type);
				MethodType mt = MethodType.newMethodType(null,targs.toArray(),Type.tpVoid);
				Method init = new Method(Constants.nameInit,mt,ACC_PUBLIC);
				init.pos = me.pos;
				foreach (Field f; case_attr.casefields)
					init.params.add(new FormPar(f.pos,f.name.name,f.type,0));
				me.addMethod(init);
				init.body = new BlockStat(me.pos,init);
			}

			// Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode n; me.members; n instanceof Struct)
					pass3(n);
			}
		} finally { PassInfo.pop(me); }
	}







	////////////////////////////////////////////////////
	//												   //
	//	   PASS 4 - resolve meta and generate members //
	//												   //
	////////////////////////////////////////////////////

	public boolean autoGenerateMembers() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no;
		try {
			Kiev.pass_no = TopLevelPass.passResolveMetaDefaults;
			for(int i=0; i < Kiev.file_unit.length; i++) {
				if( Kiev.file_unit[i] == null ) continue;
				try { Kiev.file_unit[i].resolveMetaDefaults();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				if( Kiev.files_scanned[i] == null ) continue;
				try {
					Kiev.files_scanned[i].resolveMetaDefaults();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
			if( Kiev.errCount > 0 ) goto stop;
			Kiev.pass_no = TopLevelPass.passResolveMetaValues;
			for(int i=0; i < Kiev.file_unit.length; i++) {
				if( Kiev.file_unit[i] == null ) continue;
				try { Kiev.file_unit[i].resolveMetaValues();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				if( Kiev.files_scanned[i] == null ) continue;
				try {
					Kiev.files_scanned[i].resolveMetaValues();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
			if( Kiev.errCount > 0 ) goto stop;

			Kiev.pass_no = TopLevelPass.passAutoProxyMethods;
			for(int i=0; i < Kiev.file_unit.length; i++) {
				if( Kiev.file_unit[i] == null ) continue;
				try { Kiev.file_unit[i].autoProxyMethods();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				if( Kiev.files_scanned[i] == null ) continue;
				try { Kiev.files_scanned[i].autoProxyMethods();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
stop:;
		failed = true;
		return failed;
	}
	
	public void autoGenerateMembers(ASTNode:ASTNode node) {
		return;
	}
	public void autoGenerateMembers(FileUnit:ASTNode node) {
		node.resolveMetaDefaults();
		node.resolveMetaValues();
		node.autoProxyMethods();
	}
	public void autoGenerateMembers(Struct:ASTNode node) {
		node.resolveMetaDefaults();
		node.resolveMetaValues();
		node.autoProxyMethods();
	}







	////////////////////////////////////////////////////
	//												   //
	//	   PASS 5 - pre-resolve                       //
	//												   //
	////////////////////////////////////////////////////

	public boolean preResolve() {
		boolean failed = false;
		TopLevelPass old_pass = Kiev.pass_no;
		try {
			Kiev.pass_no = TopLevelPass.passResolveImports;
			for(int i=0; i < Kiev.packages_scanned.length; i++) {
				PassInfo.push(Env.root);
				try{ Kiev.packages_scanned[i].resolveImports();
				} catch (Exception e) {
					Kiev.reportError(0,e); failed = true;
				} finally {
					PassInfo.pop(Env.root);
				}
			}
			Kiev.packages_scanned.cleanup();
			for(int i=0; i < Kiev.file_unit.length; i++) {
				if( Kiev.file_unit[i] == null ) continue;
				try { Kiev.file_unit[i].resolveImports();
				} catch (Exception e) {
					Kiev.reportError(0,e); failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				if( Kiev.files_scanned[i] == null ) continue;
				try { Kiev.files_scanned[i].resolveImports();
				} catch (Exception e) {
					Kiev.reportError(0,e); failed = true;
				}
			}


			Kiev.pass_no = TopLevelPass.passResolveFinalFields;
			for(int i=0; i < Kiev.file_unit.length; i++) {
				if( Kiev.file_unit[i] == null ) continue;
				try {
					Kiev.file_unit[i].resolveFinalFields(false);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				if( Kiev.files_scanned[i] == null ) continue;
				try {
					Kiev.files_scanned[i].resolveFinalFields(!Kiev.safe);
					if (!Kiev.safe)
						Kiev.files_scanned[i].cleanup();
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
stop:;
		failed = true;
		return failed;
	}
	
	public void preResolve(ASTNode:ASTNode node) {
		return;
	}
	public void preResolve(FileUnit:ASTNode node) {
		node.resolveImports();
		node.resolveFinalFields(false);
	}
	public void preResolve(Struct:ASTNode node) {
		node.resolveImports();
		node.resolveFinalFields(false);
	}
}


