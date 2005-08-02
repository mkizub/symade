/* Generated By:JJTree: Do not edit this line. ASTTypeDeclaration.java */

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

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTTypeDeclaration.java,v 1.4.2.1.2.4 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.2.1.2.4 $
 *
 */
 
@node
public abstract class ASTStructDeclaration extends ASTNode implements TopLevelDecl {
	@att public ASTModifiers			modifiers;
	@att public ASTAccess				acc;
	public KString						name;
	@att public final NArr<ASTNode>		argument;
	@att public final NArr<ASTNode>		members;

	@ref public Struct			me;

	ASTStructDeclaration() {
	}

	public ASTNode pass1_1() {
		// Attach meta-data to the new structure
		modifiers.getMetas(me.meta);
		return me;
	}
	
	public ASTNode pass3() {
		switch (this) {
		case ASTTypeDeclaration:
			return ASTTypeDeclaration.createMembers(me, members);
		case ASTEnumDeclaration:
			return ASTEnumDeclaration.createMembers(me, ((ASTEnumDeclaration)this).enum_fields, members);
		case ASTSyntaxDeclaration:
			return ASTSyntaxDeclaration.createMembers(me, members);
		case ASTCaseTypeDeclaration:
			return ((ASTCaseTypeDeclaration)this).createMembers();
		}
		throw new CompilerException(pos, "Unknown type: "+this.getClass());
	}

}

@node
public class ASTTypeDeclaration extends ASTStructDeclaration {
    public int					kind;
    @att public ASTNode			ext;
    @att public ASTNode			impl;
    @att public ASTGenerate		gens;

	ASTTypeDeclaration() {}
	ASTTypeDeclaration(int id) {}

  	public void set(Token t) {
    	if( t.kind == kiev020Constants.INTERFACE )
        	kind |= ACC_INTERFACE;
    	else if( t.kind == kiev020Constants.CLASS )
    		;
    	else if( t.kind == kiev020Constants.OPERATOR_AT )
        	kind |= ACC_INTERFACE | ACC_ANNOTATION;
    	else
    		throw new RuntimeException("Bad kind of class declaration "+t);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if( n instanceof ASTModifiers) {
			modifiers = (ASTModifiers)n;
		}
        else if( n instanceof ASTIdentifier ) {
			name = ((ASTIdentifier)n).name;
            pos = n.getPos();
		}
        else if( n instanceof ASTArgumentDeclaration ) {
			argument.append(n);
		}
        else if( n instanceof ASTExtends ) {
			ext = n;
		}
        else if( n instanceof ASTImplements ) {
			impl = n;
		}
        else if( n instanceof ASTGenerate ) {
			gens = (ASTGenerate)n;
		}
        else {
			members.append(n);
        }
    }

	public static Struct createMembers(Struct me, NArr<ASTNode> members) {
		trace(Kiev.debugResolve,"Pass 3 for class "+me);
        PassInfo.push(me);
        try {
			// Process members
			for(int i=0; i < members.length; i++) {
				members[i].parent = me;
				if( members[i] instanceof ASTInitializer ) {
					ASTInitializer init = (ASTInitializer)members[i];
					// TODO: check flags for initialzer
					int flags = init.modifiers.getFlags();
					Field f = me.addField(new Field(me,KString.Empty,Type.tpVoid,flags));
					f.setPos(init.getPos());
					f.init = new StatExpr(init.getPos(),init.body);
					f.init.parent = f;
					f.parent = me;
					if( me.isPackage() ) f.setStatic(true);
					if( init.pbody != null ) init.pbody.setParent((StatExpr)f.init);
					members[i] = f;
				}
				else if( members[i] instanceof ASTMethodDeclaration ) {
					ASTMethodDeclaration astmd = (ASTMethodDeclaration)members[i];
					members[i] = ((ASTMethodDeclaration)members[i]).pass3();
					if( me.isPackage() ) members[i].setStatic(true);
					if( members[i].isPrivate() ) members[i].setFinal(false);
					else if( me.isClazz() && me.isFinal() ) members[i].setFinal(true);
					else if( me.isInterface() ) {
						members[i].setPublic(true);
						if( astmd.pbody == null )
							members[i].setAbstract(true);
					}
					if( ((Method)members[i]).name.equals(nameInit) ) {
						members[i].setNative(false);
						members[i].setAbstract(false);
						members[i].setSynchronized(false);
						members[i].setFinal(false);
					}
				}
				else if( members[i] instanceof ASTRuleDeclaration ) {
					ASTRuleDeclaration astmd = (ASTRuleDeclaration)members[i];
        	    	members[i] = ((ASTRuleDeclaration)members[i]).pass3();
					if( me.isPackage() ) members[i].setStatic(true);
					if( members[i].isPrivate() ) members[i].setFinal(true);
					if( me.isClazz() && me.isFinal() ) members[i].setFinal(true);
					else if( me.isInterface() ) {
						members[i].setPublic(true);
						if( astmd.pbody == null )
							members[i].setAbstract(true);
					}
				}
				else if( members[i] instanceof ASTFieldDecl ) {
					ASTFieldDecl fields = (ASTFieldDecl)members[i];
					// TODO: check flags for fields
					int flags = fields.modifiers.getFlags();
					if( me.isPackage() ) flags |= ACC_STATIC;
					if( me.isInterface() ) {
						if( (flags & ACC_VIRTUAL) != 0 ) flags |= ACC_ABSTRACT;
						else {
							flags |= ACC_STATIC;
							flags |= ACC_FINAL;
						}
						flags |= ACC_PUBLIC;
					}
					Type type = ((ASTType)fields.type).getType();
					ASTPack pack = fields.modifiers.pack;
					if( pack != null ) {
						if( !type.isIntegerInCode() ) {
							if( type.clazz.instanceOf(Type.tpEnum.clazz) ) {
								Kiev.reportError(fields.pos,"Packing of enum is not implemented yet");
							} else {
								Kiev.reportError(fields.pos,"Packing of reference type is not allowed");
							}
							pack = null;
						} else {
							int max_pack_size = 32;
							if( type == Type.tpShort || type == Type.tpChar ) {
								max_pack_size = 16;
								if( pack.size <= 0 ) pack.size = 16;
							}
							else if( type == Type.tpByte ) {
								max_pack_size = 8;
								if( pack.size <= 0 ) pack.size = 8;
							}
							else if( type == Type.tpBoolean) {
								max_pack_size = 1;
								if( pack.size <= 0 ) pack.size = 1;
							}
							if( pack.size < 0 || pack.size > max_pack_size ) {
								Kiev.reportError(fields.pos,"Bad size "+pack.size+" of packed field");
								pack = null;
							}
							else if( pack.offset >= 0 && pack.size+pack.offset > 32) {
								Kiev.reportError(fields.pos,"Size+offset "+(pack.size+pack.offset)+" do not fit in 32 bit boundary");
								pack = null;
							}
						}
					}
					for(int j=0; j < fields.vars.length; j++) {
						ASTVarDecl fdecl = (ASTVarDecl)fields.vars[j];
						KString fname = fdecl.name;
						Type tp = type;
						for(int k=0; k < fdecl.dim; k++) tp = Type.newArrayType(tp);
						Field f = new Field(me,fname,tp,flags);
						f.setPos(fdecl.pos);
						// Attach meta-data to the new structure
						fields.modifiers.getMetas(f.meta);
						if( pack == null )
							;
						else if( fdecl.dim > 0 && pack != null )
							Kiev.reportError(fdecl.pos,"Packing of reference type is not allowed");
						else if( f.isStatic() )
							Kiev.reportWarning(fdecl.pos,"Packing of static field(s) ignored");
						else {
							f.pack = new Field.PackInfo(pack.size,pack.offset,pack.packer);
							f.setPackedField(true);
						}
						if( me.isInterface() ) {
							if( f.isVirtual() )
								f.setAbstract(true);
							else {
								f.setStatic(true);
								f.setFinal(true);
							}
							f.setPublic(true);
						}
						me.addField(f);
						if (fdecl.init == null && fdecl.dim==0) {
							if(type.clazz.isWrapper()) {
								f.init = new NewExpr(fdecl.pos,type,Expr.emptyArray);
								f.setInitWrapper(true);
							}
						} else {
							if( type.clazz.isWrapper()) {
								if (fdecl.of_wrapper)
									f.init = fdecl.init;
								else
									f.init = new NewExpr(fdecl.pos,type, (fdecl.init==null)? Expr.emptyArray : new Expr[]{fdecl.init});
								f.setInitWrapper(true);
							} else {
								f.init = fdecl.init;
								f.setInitWrapper(false);
							}
						}

						if( ((ASTFieldDecl)members[i]).modifiers.acc != null )
							f.acc = new Access(((ASTFieldDecl)members[i]).modifiers.acc.accflags);
					}
				}
				else if( members[i] instanceof ASTInvariantDeclaration ) {
					members[i] = ((ASTInvariantDeclaration)members[i]).pass3();
				}
    	        // Inner classes and cases after all methods and fields, skip now
				else if( members[i] instanceof ASTTypeDeclaration );
				else if( members[i] instanceof ASTCaseTypeDeclaration );
				else if( members[i] instanceof Import ) {
					me.imported.add(members[i]);
				}
				else {
					throw new CompilerException(members[i].getPos(),"Unknown type if structure member: "+members[i]);
				}
       	    	members[i].parent = me;
			}

			new ProcessVirtFld().createMembers(me);
			me.setupWrappedField();

    	    // Process inner classes and cases
        	if( !me.isPackage() ) {
				for(int i=0; i < members.length; i++) {
					if( members[i] instanceof ASTStructDeclaration )
						members[i].pass3();
				}
			}
			// Process ASTGenerete
			for(int i=0; i < me.gens.length; i++) {
				Struct s = me.gens[i];
				s.super_clazz = Type.getRealType(s.type,me.super_clazz);
				s.package_clazz = me.package_clazz;
				if( me.interfaces.length != 0 ) {
					for(int j=0; j < me.interfaces.length; j++)
						s.interfaces.add(Type.getRealType(s.type,me.interfaces[j]));
				}
			}
		} finally { PassInfo.pop(me); }

		return me;
	}

	public ASTNode autoProxyMethods() {
		me.autoProxyMethods();
		return me;
	}

	public ASTNode resolveImports() {
		me.resolveImports();
		return me;
	}

	public ASTNode resolveFinalFields(boolean cleanup) {
		me.resolveFinalFields(cleanup);
		return me;
	}

	public Dumper toJava(Dumper dmp) {
		dmp.space().append(me.name.name).space();
		dmp.space().append('{').newLine(1);
		for(int j=0; j < members.length; j++) dmp.append(members[j]);
		dmp.newLine(-1).append('}').newLine();
		return dmp;
	}
}

