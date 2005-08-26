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
	
	public boolean		pass1()								{ return false; }
	public void			pass1(ASTNode node)					{ return; }
	public boolean		pass1_1()							{ return false; }
	public void			pass1_1(ASTNode node)				{ return; }
	public boolean		pass2()								{ return false; }
	public void			pass2(ASTNode node)					{ return; }
	public boolean		pass2_2()							{ return false; }
	public void			pass2_2(ASTNode node)				{ return; }
	public boolean		pass3()								{ return false; }
	public void			pass3(ASTNode node)					{ return; }
//	public boolean		autoProxyMethods()					{ return false; }
//	public void			autoProxyMethods(ASTNode node)		{ return; }
	public boolean		autoGenerateMembers()				{ return false; }
	public void			autoGenerateMembers(ASTNode node)	{ return; }
//	public boolean		resolveImports()					{ return false; }
//	public void			resolveImports(ASTNode node)		{ return; }
//	public boolean		resolveFinalFields(boolean cleanup)				{ return false; }
//	public void			resolveFinalFields(ASTNode node, boolean cleanup)	{ return; }
	public boolean		preResolve()						{ return false; }
	public void			preResolve(ASTNode node)			{ return; }
	public boolean		verify()							{ return false; }
	public void			verify(ASTNode node)				{ return; }
	public boolean		resolve()							{ return false; }
	public void			resolve(ASTNode node)				{ return; }
	public boolean		generate()							{ return false; }
	public void			generate(ASTNode node)				{ return; }
}

