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
				ASTFileUnit fu = Kiev.file_unit[i]; 
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
	
	public ASTNode pass1(ASTFileUnit:ASTNode astn, ASTNode pn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		boolean[] exts = Kiev.getExtSet();
		try {
        		Kiev.setExtSet(astn.disabled_extensions);
			if( astn.pkg != null ) {
				astn.pkg = (Struct) pass1((ASTPackage)astn.pkg, null);
			} else {
				astn.pkg = Env.root;
			}
			FileUnit fu = new FileUnit(astn.filename,(Struct)astn.pkg);
			astn.file_unit = fu;
			fu.disabled_extensions = astn.disabled_extensions;
			fu.bodies = astn.bodies;
			PassInfo.push(astn.pkg);
			try {
				for(int i=0; i < astn.decls.length; i++) {
					try {
						Struct s = (Struct) pass1((ASTNode)astn.decls[i], astn.file_unit);
						fu.members.add( s );
					} catch(Exception e ) {
						Kiev.reportError/*Warning*/(((ASTNode)astn.decls[i]).getPos(),e);
					}
				}
			} finally { PassInfo.pop(astn.pkg); }
			return astn.file_unit;
		} finally { Kiev.curFile = oldfn; Kiev.setExtSet(exts); }
	}

	public ASTNode pass1(ASTPackage:ASTNode astn, ASTNode pn) {
		return Env.newPackage(ClazzName.fromToplevelName(astn.name,false));
	}

	private int makeStructFlags(int flags, ASTNode[] modifiers) {
		// TODO: check flags for structures
		int n = modifiers.length;
		for(int i=0; i < n; i++)
			flags |= ((ASTModifier)modifiers[i]).flag();
		return flags;
	}
	
	private void setSourceFile(Struct me, ASTNode astn) {
		if( astn.parent instanceof ASTFileUnit || astn.parent instanceof ASTTypeDeclaration )
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
		            if( outer_args == null || outer_args.length <= i
					|| !outer_args[i].clazz.name.short_name.equals(arg.name) )
						throw new CompilerException(arg.getPos(),"Inner class arguments must match outer class argument,"
							+" but arg["+i+"] is "+arg.name
							+" and have to be "+outer_args[i].clazz.name.short_name);
				}
				/* Create type for class's arguments, if any */
				if( astn.argument.length > 0 ) {
					targs = astnp.me.type.args;
				}
			} else {
				for(int i=0; i < astn.argument.length; i++) {
					Struct arg =
						Env.newArgument(((ASTArgumentDeclaration)astn.argument[i]).name,me);
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

		int flags = makeStructFlags(ACC_PIZZACASE|ACC_STATIC|ACC_RESOLVED, astn.modifier);

		ASTTypeDeclaration astnp = (ASTTypeDeclaration)astn.parent;
		astn.me = Env.newStruct(clname,astnp.me,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		setSourceFile(me, astn);
		astnp.me.addCase(me);
		me.super_clazz = astnp.me.type;
		setupStructType(me, astn, true);

		return me;
	}
	
	public ASTNode pass1(ASTEnumDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for enum "+astn.name);
		boolean isTop = (astn.parent != null && astn.parent instanceof ASTFileUnit);
		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz,astn.name,false,!isTop);

		int flags = makeStructFlags(ACC_ENUM|ACC_RESOLVED, astn.modifier);
		if( !(astn.parent instanceof ASTFileUnit) ) flags |= ACC_STATIC;

		astn.me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		setSourceFile(me, astn);
		setupStructType(me, astn, false);

		return me;
	}
	
	public ASTNode pass1(ASTSyntaxDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for synax "+astn.name);
		boolean isTop = (astn.parent != null && astn.parent instanceof ASTFileUnit);
		ClazzName clname = ClazzName.fromOuterAndName(PassInfo.clazz, astn.name, false, !isTop);

		int flags = makeStructFlags(ACC_PRIVATE|ACC_ABSTRACT|ACC_SYNTAX|ACC_RESOLVED|
			ACC_MEMBERS_GENERATED|ACC_STATEMENTS_GENERATED, astn.modifier);

		astn.me = Env.newStruct(clname,PassInfo.clazz,flags,true);
		Struct me = astn.me;
		me.parent = pn;
		setSourceFile(me, astn);
		setupStructType(me, astn, false);

		return me;
	}

	public ASTNode pass1(ASTTypeDeclaration:ASTNode astn, ASTNode pn) {
		trace(Kiev.debugResolve,"Pass 1 for class "+astn.name);
		Struct piclz = PassInfo.clazz;
		int flags = makeStructFlags(astn.kind|ACC_RESOLVED, astn.modifier);

		if( astn.name != null ) {
			ClazzName clname;
			if( PassInfo.method != null ) {
				// Construct name of local class
				KString bytecode_name =
					KString.from(piclz.name.bytecode_name
						+"$"+piclz.anonymouse_inner_counter
						+"$"+astn.name);
				//KString name = kiev.vlang.ClazzName.fixName(bytecode_name.replace('/','.'));
				KString name = bytecode_name.replace('/','.');
				clname = new ClazzName(name,astn.name,bytecode_name,false,false);
			} else {
				boolean isTop = (astn.parent != null && astn.parent instanceof ASTFileUnit);
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
						pass1(m,me);
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
				ASTFileUnit fu = Kiev.file_unit[i]; 
				if( fu == null ) continue;
				try { pass1_1(fu,null);
				} catch (Exception e) {
					Kiev.reportError(0,e); Kiev.file_unit[i] = null; failed = true;
				}
			}
			for(int i=0; i < Kiev.files_scanned.length; i++) {
				ASTFileUnit fu = Kiev.files_scanned[i]; 
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

	public ASTNode pass1_1(ASTFileUnit:ASTNode astn, ASTNode pn) {
		KString oldfn = Kiev.curFile;
		Kiev.curFile = astn.filename;
		PassInfo.push(astn.file_unit);
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(astn.disabled_extensions);

			// Process file imports...
			boolean java_lang_found = false;
			KString java_lang_name = KString.from("java.lang");
			boolean kiev_stdlib_found = false;
			KString kiev_stdlib_name = KString.from("kiev.stdlib");

			foreach (ASTNode n; astn.syntax) {
				try {
					if (n instanceof ASTImport && ((ASTImport)n).mode == ASTImport.IMPORT_STATIC && !((ASTImport)n).star)
						continue; // process later
					ASTNode sn = pass1_1(n, astn.file_unit);
					if (sn == null)
						continue;
					astn.file_unit.syntax.add(sn);
					if (sn instanceof Import) {
						if( sn.mode == Import.IMPORT_CLASS && ((Struct)sn.node).name.name.equals(java_lang_name))
							java_lang_found = true;
						else if( sn.mode == Import.IMPORT_CLASS && ((Struct)sn.node).name.name.equals(kiev_stdlib_name))
							kiev_stdlib_found = true;
					}
					trace(Kiev.debugResolve,"Add "+sn);
				} catch(Exception e ) {
					Kiev.reportError(n.getPos(),e);
				}
			}
			// Add standard imports, if they were not defined
			if( !Kiev.javaMode && !kiev_stdlib_found )
				astn.file_unit.syntax.add(new Import(0,pn,Env.newPackage(kiev_stdlib_name),Import.IMPORT_CLASS,true));
			if( !java_lang_found )
				astn.file_unit.syntax.add(new Import(0,pn,Env.newPackage(java_lang_name),Import.IMPORT_CLASS,true));

			// Process members - pass1_1()
			foreach (ASTNode n; astn.decls) {
				pass1_1(n, astn.file_unit);
			}
		} finally { Kiev.setExtSet(exts); PassInfo.pop(astn.file_unit); Kiev.curFile = oldfn; }
		return astn.file_unit;
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
		return new Import(astn.pos, pn, n, astn.mode, astn.star);
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
		return astn.td;
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
					return op;
				}
				op = AssignOperator.newAssignOperator(image,null,null,false);
				if( Kiev.verbose ) System.out.println("Declared assign operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return op;
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
					return op;
				}
				op = BinaryOperator.newBinaryOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared infix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return op;
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
					return op;
				}
				op = PrefixOperator.newPrefixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared prefix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return op;
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
					return op;
				}
				op = PostfixOperator.newPostfixOperator(prior,image,null,null,Operator.orderAndArityNames[opmode],false);
				if( Kiev.verbose ) System.out.println("Declared postfix operator "+op+" "+Operator.orderAndArityNames[op.mode]+" "+op.priority);
				return op;
			}
		case Operator.XFXFY:
			throw new CompilerException(pos,"Multioperators are not supported yet");
		default:
			throw new CompilerException(pos,"Unknown operator mode "+opmode);
		}
	}

}


