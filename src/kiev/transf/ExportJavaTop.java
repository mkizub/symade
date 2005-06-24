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
	
	public ASTNode pass1(ASTNode node, ASTNode pn) {
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
}


