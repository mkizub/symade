/* Generated By:JJTree: Do not edit this line. ASTModifier.java */

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

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/parser/ASTModifier.java,v 1.4.4.1 1999/05/29 21:03:06 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.4.4.1 $
 *
 */

@node
public class ASTModifier extends ASTNode implements kiev020Constants {

	public static ASTModifier[] emptyArray = new ASTModifier[0];
	
	public int 			kind;
	public String		image;

	@ref public static final ASTModifier modPUBLIC		= new ASTModifier(PUBLIC,"public");
	@ref public static final ASTModifier modPRIVATE		= new ASTModifier(PRIVATE,"private");
	@ref public static final ASTModifier modPROTECTED	= new ASTModifier(PROTECTED,"protected");
	@ref public static final ASTModifier modFINAL		= new ASTModifier(FINAL,"final");
	@ref public static final ASTModifier modSTATIC		= new ASTModifier(STATIC,"static");
	@ref public static final ASTModifier modABSTRACT		= new ASTModifier(ABSTRACT,"abstract");
	@ref public static final ASTModifier modNATIVE		= new ASTModifier(NATIVE,"native");
	@ref public static final ASTModifier modSYNCHRONIZED	= new ASTModifier(SYNCHRONIZED,"synchronized");
	@ref public static final ASTModifier modVOLATILE		= new ASTModifier(VOLATILE,"volatile");
	@ref public static final ASTModifier modTRANSIENT	= new ASTModifier(TRANSIENT,"transient");
	@ref public static final ASTModifier modWRAPPER		= new ASTModifier(WRAPPER,"$wrapper");

	ASTModifier() {
	}

	ASTModifier(int id) {
		super(0);
	}

	private ASTModifier(int kind, String image) {
		super(0);
		this.kind = kind;
		this.image = image;
	}

	public void set(Token t) {
		this.kind = t.kind;
        pos = t.getPos();
        if( t.kind == IDENTIFIER )
        	image = t.image.intern();
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n);
    }

    public int flag() {
    	switch( kind ) {
    	case PUBLIC:		return ACC_PUBLIC;
    	case PRIVATE:		return ACC_PRIVATE;
    	case PROTECTED:		return ACC_PROTECTED;
    	case FINAL:			return ACC_FINAL;
    	case STATIC:		return ACC_STATIC;
    	case ABSTRACT:		return ACC_ABSTRACT;
    	case NATIVE:		return ACC_NATIVE;
    	case SYNCHRONIZED:	return ACC_SYNCHRONIZED;
    	case VOLATILE:		return ACC_VOLATILE;
    	case TRANSIENT:		return ACC_TRANSIENT;
    	case VIRTUAL:		return ACC_VIRTUAL;
    	case FORWARD:		return ACC_FORWARD;
    	case WRAPPER:		return ACC_WRAPPER;
    	case IDENTIFIER:
    		if     ( image == "virtual" )	return ACC_VIRTUAL;
    		else if( image == "forward" )	return ACC_FORWARD;
    		return 0;
    	default:			return 0;
    	}
    }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(image==null?tokenImage[kind]:image).space();
    }
}
