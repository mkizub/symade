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

public class TransfProcessor {
	private Kiev.Ext extension;
	public TransfProcessor(Kiev.Ext ext) {
		this.extension = ext;
	}
	public boolean isEnabled() {
		return Kiev.enabled(extension);
	}
	public boolean isDisabled() {
		return Kiev.disabled(extension);
	}
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    Import        //////////////////////////////
	///////////////////////////////////////////////////////////////////////
	// create base types
	public void			pass1(ASTNode node)					{ return; }
	// resolve syntax
	public void			pass1_1(ASTNode node)				{ return; }
	// resolve and create parameters of types
	public void			pass2(ASTNode node)					{ return; }
	// create real types
	public void			pass2_2(ASTNode node)				{ return; }
	// process meta declarations
	public void			resolveMetaDecl(ASTNode node)		{ return; }
	// process meta default values
	public void			resolveMetaDefaults(ASTNode node)	{ return; }
	// process meta values of classes and members
	public void			resolveMetaValues(ASTNode node)		{ return; }
	// process declared class members
	public void			pass3(ASTNode node)					{ return; }
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    VNode language     /////////////////////////
	///////////////////////////////////////////////////////////////////////
	// auto-create class members
	public void			autoGenerateMembers(ASTNode node)	{ return; }
	// resolve vnodes
	public void			preResolve(ASTNode node)			{ return; }
	// resolve vnodes
	public void			mainResolve(ASTNode node)			{ return; }
	// verify resolved tree
	public void			verify(ASTNode node)				{ return; }
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    Back-end    ////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	
	public BackendProcessor getBackend(Kiev.Backend backend) { return null; }
}

