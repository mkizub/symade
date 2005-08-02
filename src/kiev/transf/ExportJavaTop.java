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

/**
 * @author Maxim Kizub
 *
 */

public final class ExportJavaTop implements Constants {
	
	/////////////////////////////////////////////////////
	//                                                 //
	//         PASS 1 - create top structures          //
	//                                                 //
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
					pass1(fu, null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass1(ASTNode:ASTNode node, ASTNode pn) {
		return node;
	}
	
	public ASTNode pass1(FileUnit:ASTNode astn, ASTNode pn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		boolean[] exts = Kiev.getExtSet();
		try {
        	Kiev.setExtSet(astn.disabled_extensions);
			if( astn.pkg != null )
				pass1(astn.pkg, astn);
			else
				astn.pkg = new ASTPackage(KString.Empty,Env.root);
			FileUnit fu = astn;
			fu.disabled_extensions = astn.disabled_extensions;
			fu.bodies = astn.bodies;
			PassInfo.push(astn.pkg.resolved);
			try {
				for(int i=0; i < astn.members.length; i++) {
					try {
						Struct s = (Struct) pass1((ASTNode)astn.members[i], astn);
						s.parent = fu;
					} catch(Exception e ) {
						Kiev.reportError/*Warning*/(((ASTNode)astn.members[i]).getPos(),e);
					}
				}
			} finally { PassInfo.pop(astn.pkg.resolved); }
			return astn;
		} finally { Kiev.curFile = oldfn; Kiev.setExtSet(exts); }
	}

	public ASTNode pass1(ASTPackage:ASTNode astn, ASTNode pn) {
		astn.resolved = Env.newPackage(ClazzName.fromToplevelName(astn.name,false));
		return astn;
	}

	private void setSourceFile(Struct me, ASTNode astn) {
		if( astn.parent instanceof FileUnit || astn.parent instanceof ASTTypeDeclaration )
			Env.setProjectInfo(me.name, Kiev.curFile);
		SourceFileAttr sfa = new SourceFileAttr(Kiev.curFile);
		me.addAttr(sfa);
	}
	
	private void setupStructType(Struct me, ASTStructDeclaration astn, boolean canHaveArgs) {
        PassInfo.push(me);
        try {
			/* Then may be class arguments - they are proceed here, but their
			   inheritance - at pass2()
			*/
			Type[] targs = Type.emptyArray;
			if (!canHaveArgs) {
				if( astn.argument.length > 0 ) {
					Kiev.reportError(astn.getPos(),"Type parameters are not allowed for "+astn.name);
				}
			}
			else if( astn.parent instanceof ASTTypeDeclaration && ((ASTTypeDeclaration)astn.parent).argument.length > 0 ) {
				ASTTypeDeclaration astnp = (ASTTypeDeclaration)astn.parent;
				// Inner classes's argumets have to be arguments of outer classes
				for(int i=0; i < astn.argument.length; i++) {
					ASTArgumentDeclaration arg = (ASTArgumentDeclaration)astn.argument[i];
					Type[] outer_args = astnp.me.type.args;
					if( outer_args == null || outer_args.length <= i)
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class arguments");
					if !(outer_args[i].clazz.name.short_name.equals(arg.ident.name))
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class argument,"
							+" but arg["+i+"] is "+arg.ident
							+" and have to be "+outer_args[i].clazz.name.short_name);
				}
				/* Create type for class's arguments, if any */
				if( astn.argument.length > 0 ) {
					targs = astnp.me.type.args;
				}
			} else {
				for(int i=0; i < astn.argument.length; i++) {
					ASTNode a = astn.argument[i];
					Struct arg = Env.newArgument(((ASTArgumentDeclaration)a).ident.name,me);
					arg.type = Type.newRefType(arg);
					targs = (Type[])Arrays.append(targs,arg.type);
				}
			}

			/* Generate type for this structure */
			me.type = Type.newRefType(me,targs);
		} finally { PassInfo.pop(me); }

	}
	
	public ASTNode pass1(ASTCaseTypeDeclaration:ASTNode astn, ASTNode pn) {
		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz,astn.name,false,true);

		int flags = ACC_STATIC | astn.modifiers.getFlags();

		ASTTypeDeclaration astnp = (ASTTypeDeclaration)astn.parent;
		astn.me = Env.newStruct(clname,astnp.me,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		me.setPizzaCase(true);
		me.setResolved(true);
		setSourceFile(me, astn);
		astnp.me.addCase(me);
		me.super_clazz = astnp.me.type;
		if( astn.acc != null ) me.acc = new Access(astn.acc.accflags);
		setupStructType(me, astn, true);

		return me;
	}
	
	public ASTNode pass1(ASTEnumDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for enum "+astn.name);
		boolean isTop = (astn.parent != null && astn.parent instanceof FileUnit);
		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz,astn.name,false,!isTop);

		int flags = astn.modifiers.getFlags();
		if( !(astn.parent instanceof FileUnit) ) flags |= ACC_STATIC;

		astn.me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		me.setEnum(true);
		me.setResolved(true);
		if( astn.acc != null ) me.acc = new Access(astn.acc.accflags);
		setSourceFile(me, astn);
		setupStructType(me, astn, false);

		return me;
	}
	
	public ASTNode pass1(ASTSyntaxDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for synax "+astn.name);
		boolean isTop = (astn.parent != null && astn.parent instanceof FileUnit);
		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz, astn.name, false, !isTop);

		int flags = ACC_PRIVATE|ACC_ABSTRACT | astn.modifiers.getFlags();

		astn.me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		me.setSyntax(true);
		me.setResolved(true);
		me.setMembersGenerated(true);
		me.setStatementsGenerated(true);
		if( astn.acc != null ) me.acc = new Access(astn.acc.accflags);
		setSourceFile(me, astn);
		setupStructType(me, astn, false);

		return me;
	}

	public ASTNode pass1(ASTTypeDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for class "+astn.name);
		Struct piclz = PassInfo.clazz;
		int flags = astn.kind | astn.modifiers.getFlags();

		if( astn.name != null ) {
			ClazzName clname;
			if( PassInfo.method != null ) {
				// Construct name of local class
				KString bytecode_name =
					KString.from(piclz.name.bytecode_name
						+"$"+piclz.countAnonymouseInnerStructs()
						+"$"+astn.name);
				//KString name = kiev.vlang.ClazzName.fixName(bytecode_name.replace('/','.'));
				KString name = bytecode_name.replace('/','.');
				clname = new ClazzName(name,astn.name,bytecode_name,false,false);
			} else {
				boolean isTop = (astn.parent != null && astn.parent instanceof FileUnit);
				clname = ClazzName.fromOuterAndName(PassInfo.clazz,astn.name,false,!isTop);
			}
			astn.me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		} else {
			astn.me = piclz;
			if( !astn.me.isPackage() || astn.me == Env.root )
				throw new CompilerException(astn.pos,"Package body declaration error");
		}

		Struct me = astn.me;
		me.parent = pn;
		me.setResolved(true);
		if( astn.acc != null ) me.acc = new Access(astn.acc.accflags);
		setSourceFile(me, astn);
		setupStructType(me, astn, true);

        PassInfo.push(me);
        try {
			// Process inner classes and cases
			if( !me.isPackage() ) {
				for(int i=0; i < astn.members.length; i++) {
					ASTNode m = astn.members[i];
					if( m instanceof ASTTypeDeclaration || m instanceof ASTCaseTypeDeclaration) {
						m.parent = astn;
						me.members.add(pass1(m,me));
					}
				}
			}
		} finally { PassInfo.pop(me); }

		return me;
	}





	/////////////////////////////////////////////////////
	//                                                 //
	//     PASS 1_1 - process syntax declarations      //
	//                                                 //
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
				try { pass1_1(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass1_1(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass1_1(ASTNode:ASTNode node, ASTNode pn) {
		return node;
	}

	public ASTNode pass1_1(FileUnit:ASTNode astn, ASTNode pn) {
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
					if (n instanceof ASTImport && ((ASTImport)n).mode == ASTImport.IMPORT_STATIC && !((ASTImport)n).star)
						continue; // process later
					ASTNode sn = pass1_1(n, astn);
					if (sn == null)
						continue;
					if (sn instanceof Import) {
						if( sn.mode == Import.IMPORT_CLASS && ((Struct)sn.node).name.name.equals(java_lang_name))
							java_lang_found = true;
						else if( sn.mode == Import.IMPORT_CLASS && ((Struct)sn.node).name.name.equals(kiev_stdlib_name))
							kiev_stdlib_found = true;
						else if( sn.mode == Import.IMPORT_CLASS && ((Struct)sn.node).name.name.equals(kiev_stdlib_meta_name))
							kiev_stdlib_meta_found = true;
					}
					trace(Kiev.debugResolve,"Add "+sn);
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
			// Add standard imports, if they were not defined
			if( !Kiev.javaMode && !kiev_stdlib_found )
				astn.syntax.add(new Import(0,pn,Env.newPackage(kiev_stdlib_name),Import.IMPORT_CLASS,true));
			if( !Kiev.javaMode && !kiev_stdlib_meta_found )
				astn.syntax.add(new Import(0,pn,Env.newPackage(kiev_stdlib_meta_name),Import.IMPORT_CLASS,true));
			if( !java_lang_found )
				astn.syntax.add(new Import(0,pn,Env.newPackage(java_lang_name),Import.IMPORT_CLASS,true));

			// Process members - pass1_1()
			foreach (ASTNode n; astn.members) {
				pass1_1(n, astn);
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
		return astn;
	}

	public ASTNode pass1_1(ASTImport:ASTNode astn, ASTNode pn) {
		if (astn.args != null || (astn.mode==ASTImport.IMPORT_STATIC && !astn.star)) return null;
		KString name = astn.name;
		ASTNode@ v;
		if( !PassInfo.resolveNameR(v,new ResInfo(),name,null,0) ) {
			Kiev.reportError(astn.pos,"Unresolved identifier "+name);
			return null;
		}
		ASTNode n = v;
		if      (astn.mode == ASTImport.IMPORT_CLASS && !(n instanceof Struct)) {
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a class or package");
			return null;
		}
		else if (astn.mode == ASTImport.IMPORT_PACKAGE && !(n instanceof Struct && ((Struct)n).isPackage())) {
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a package");
			return null;
		}
		else if (astn.mode == ASTImport.IMPORT_STATIC && !(astn.star || (n instanceof Field))) {
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a field");
			return null;
		}
		else if (astn.mode == ASTImport.IMPORT_SYNTAX && !(n instanceof Struct && n.isSyntax())) {
			Kiev.reportError(astn.pos,"Identifier "+name+" is not a syntax");
			return null;
		}
		return astn.replaceWith(new Import(astn.pos, pn, n, astn.mode, astn.star));
	}

	public ASTNode pass1_1(ASTTypedef:ASTNode astn, ASTNode pn) {
		astn.td = new Typedef(astn.pos, pn, astn.name);
		if (astn.opdef) {
			ASTQName qn = (ASTQName)astn.type;
			ASTNode@ v;
			if( !PassInfo.resolveNameR(v,new ResInfo(),qn.toKString(),null,0) )
				throw new CompilerException(astn.pos,"Unresolved identifier "+qn.toKString());
			if( !(v instanceof Struct) )
				throw new CompilerException(qn.getPos(),"Type name "+qn.toKString()+" is not a structure, but "+v);
			Struct s = (Struct)v;
			if (s.type.args.length != 1)
				throw new CompilerException(qn.getPos(),"Type "+s.type+" must have 1 argument");
			astn.td.type = s.type;
		} else {
			astn.td.type = astn.type.getType();
		}
		return astn.replaceWith(astn.td);
	}

	public ASTNode pass1_1(ASTStructDeclaration:ASTNode astn, ASTNode pn) {
		// Attach meta-data to the new structure
		astn.modifiers.getMetas(astn.me.meta);
		return astn.me;
	}
	
	public ASTNode pass1_1(ASTSyntaxDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1_1 for syntax "+astn.me);
		foreach (ASTNode n; astn.members) {
			try {
				if (n instanceof ASTTypedef) {
					n = pass1_1(n, astn.me);
					if (n != null) {
						astn.me.imported.add(n);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+astn.me);
					}
				}
				else if (n instanceof ASTOpdef) {
					n = pass1_1(n, astn.me);
					if (n != null) {
						astn.me.imported.add(n);
						trace(Kiev.debugResolve,"Add "+n+" to syntax "+astn.me);
					}
				}
			} catch(Exception e ) {
				Kiev.reportError(n.getPos(),e);
			}
		}
		// Attach meta-data to the new structure
		astn.modifiers.getMetas(astn.me.meta);
		return astn.me;
	}

	public ASTNode pass1_1(ASTOpdef:ASTNode astn, ASTNode pn) {
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
					return astn.replaceWith(op);
				}
				op = AssignOperator.newAssignOperator(image,null,null,false);
				if( Kiev.verbose ) System.out.println("Declared assign operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return astn.replaceWith(op);
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
					return astn.replaceWith(op);
				}
				op = BinaryOperator.newBinaryOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared infix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return astn.replaceWith(op);
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
					return astn.replaceWith(op);
				}
				op = PrefixOperator.newPrefixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared prefix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return astn.replaceWith(op);
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
					return astn.replaceWith(op);
				}
				op = PostfixOperator.newPostfixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared postfix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return astn.replaceWith(op);
			}
		case Operator.XFXFY:
			throw new CompilerException(pos,"Multioperators are not supported yet");
		default:
			throw new CompilerException(pos,"Unknown operator mode "+opmode);
		}
	}






	/////////////////////////////////////////////////////
	//                                                 //
	//     PASS 2 - struct args inheritance            //
	//                                                 //
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
				try { pass2(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass2(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public ASTNode pass2(ASTNode:ASTNode astn, ASTNode pn) {
		return astn;
	}

	public ASTNode pass2(FileUnit:ASTNode astn, ASTNode pn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(astn.disabled_extensions);
			// Process members - pass2()
			foreach (ASTNode n; astn.members)
				pass2(n, astn);
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
		return astn;
	}

	public ASTNode pass2(ASTStructDeclaration:ASTNode astn, ASTNode pn) {
		return astn.me;
	}
	
	public ASTNode pass2(ASTTypeDeclaration:ASTNode astn, ASTNode pn) {
		Struct me = astn.me;
		int pos = astn.pos;
		trace(Kiev.debugResolve,"Pass 2 for class "+me);
        PassInfo.push(me);
        try {
			/* Process inheritance of class's arguments, if any */
			Type[] targs = me.type.args;
	        for(int i=0; i < astn.argument.length; i++) {
				if (astn.argument[i] instanceof ASTArgumentDeclaration) {
					ASTArgumentDeclaration arg = (ASTArgumentDeclaration)astn.argument[i];
					if( arg.type != null ) {
						ASTNonArrayType at = arg.type;
						Type sup = at.getType();
						if( !sup.isReference() )
							Kiev.reportError(astn.pos,"Argument extends primitive type "+sup);
						else
							targs[i].clazz.super_clazz = sup;
						targs[i].checkJavaSignature();
					} else {
						targs[i].clazz.super_clazz = Type.tpObject;
					}
				}
			}
			// Process ASTGenerete
			if( astn.gens != null ) {
				ASTGenerate ag = (ASTGenerate)astn.gens;
				Type[][] gtypes = new Type[ag.children.length/me.type.args.length][me.type.args.length];
				for(int l=0; l < gtypes.length; l++) {
					for(int m=0; m < me.type.args.length; m++) {
						int k = l*me.type.args.length+m;
						if( ag.children[k] instanceof ASTPrimitiveType) {
							if( ((ASTArgumentDeclaration)astn.argument[m]).type != null ) {
								Kiev.reportError(pos,"Generation for primitive type for argument "+m+" is not allowed");
							}
							gtypes[l][m] = ((ASTPrimitiveType)ag.children[k]).type;
						} else { // ASTIdentifier
							KString a = ((ASTIdentifier)ag.children[k]).name;
							ASTArgumentDeclaration ad = (ASTArgumentDeclaration)astn.argument[m]; 
							if( a != ad.ident.name ) {
								Kiev.reportError(pos,"Generation argument "+astn.name+" do not match argument "+ad.ident);
							}
							gtypes[l][m] = me.type.args[m];
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
					me.gens.append(s);
					s.type = gtype;
					s.generated_from = me;
					s.super_clazz = Type.getRealType(s.type,me.super_clazz);
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
						sc.gens.append(scg);
						scg.type = scgt;
						scg.generated_from = sc;
						scg.super_clazz = Type.getRealType(scg.type,sc.super_clazz);
					}
				}
			}

	        // Process inner classes and cases
        	if( !me.isPackage() ) {
				foreach (ASTNode m; astn.members)
					pass2(m, me);
			}
		} finally { PassInfo.pop(me); }

		return me;
	}







	/////////////////////////////////////////////////////
	//                                                 //
	//     PASS 2_2 - struct inheritance               //
	//                                                 //
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
				try { pass2_2(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				FileUnit fu = Kiev.files_scanned[i]; 
				if( fu == null ) continue;
				try { pass2_2(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.files_scanned[i] = null; failed = true;
				}
			}
		} finally { Kiev.pass_no = old_pass; }
		return failed;
	}
	
	public void pass2_2(ASTNode:ASTNode astn, ASTNode pn) {
	}

	public void pass2_2(FileUnit:ASTNode astn, ASTNode pn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(astn.disabled_extensions);
			// Process members - pass2()
			foreach (ASTNode n; astn.members)
				pass2_2(n, astn);
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn); Kiev.curFile = oldfn; }
	}

	public void pass2_2(ASTEnumDeclaration:ASTNode astn, ASTNode pn) {
		int pos = astn.pos;
		Struct me = astn.me;
		trace(Kiev.debugResolve,"Pass 2_2 for enum "+me);
        PassInfo.push(me);
        try {
			/* Now, process 'extends' and 'implements' clauses */
			ASTNonArrayType at;
			if( astn.ext != null ) {
				ASTExtends exts = (ASTExtends)astn.ext;
				at = (ASTNonArrayType)exts.children[0];
				me.super_clazz = at.getType();
			}
			if( me.super_clazz == null ) {
				me.super_clazz = Type.tpEnum;
			}

			if( !me.super_clazz.isReference() ) {
				me.setPrimitiveEnum(true);
				me.type.setMeAsPrimitiveEnum();
			}
		} finally { PassInfo.pop(me); }
	}
	
	public void pass2_2(ASTSyntaxDeclaration:ASTNode astn, ASTNode pn) {
		if (!Kiev.packages_scanned.contains(astn.me))
			Kiev.packages_scanned.append(astn.me);
	}

	public void pass2_2(ASTTypeDeclaration:ASTNode astn, ASTNode pn) {
		int pos = astn.pos;
		Struct me = astn.me;
		trace(Kiev.debugResolve,"Pass 2_2 for class "+me);
		PassInfo.push(me);
		try {
			/* Now, process 'extends' and 'implements' clauses */
			ASTNonArrayType at;
			if( astn.ext != null ) {
				ASTExtends exts = (ASTExtends)astn.ext;
				if( me.isInterface() ) {
					me.super_clazz = Type.tpObject;
					for(int j=0; j < exts.children.length; j++) {
						at = (ASTNonArrayType)exts.children[j];
						me.interfaces.append(at.getType());
					}
				} else {
					at = (ASTNonArrayType)exts.children[0];
					me.super_clazz = at.getType();
				}
			}
			if( me.isAnnotation() ) {
				astn.impl = null;
				me.interfaces.delAll();
				me.interfaces.add(Type.tpAnnotation);
			}
			if( me.super_clazz == null && !me.name.name.equals(Type.tpObject.clazz.name.name)) {
				me.super_clazz = Type.tpObject;
			}
			if( astn.impl != null ) {
				ASTImplements impls = (ASTImplements)astn.impl;
				for(int j=0; j < impls.children.length; j++) {
					at = (ASTNonArrayType)impls.children[j];
					me.interfaces.append(at.getType());
				}
			}
			if( !me.isInterface() &&  me.type.args.length > 0 && !(me.type instanceof MethodType) ) {
				me.interfaces.append(Type.tpTypeInfoInterface);
			}
			if( me.interfaces.length > 0 && me.gens != null ) {
				for(int g=0; g < me.gens.length; g++) {
					for(int l=0; l < me.interfaces.length; l++) {
						me.gens[g].interfaces.add(Type.getRealType(me.gens[g].type,me.interfaces[l]));
					}
				}
			}

	        // Process inner classes and cases
			if( !me.isPackage() ) {
				foreach (ASTNode m; astn.members)
					pass2_2(m, me);
			}
		} finally { PassInfo.pop(me); }

	}
}


