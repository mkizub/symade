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

public final class ProcessVNode implements Constants {

	private static final KString mnNode = KString.from("kiev.vlang.node"); 
	private static final KString mnAtt  = KString.from("kiev.vlang.att"); 
	private static final KString mnRef  = KString.from("kiev.vlang.ref"); 
	private static final KString nameNArr  = KString.from("kiev.vlang.NArr"); 
	
	/////////////////////////////////////////////
	//      Verify the VNode tree structure    //
    /////////////////////////////////////////////

	public boolean verify() {
		boolean failed = false;
		for (int i=0; i < Kiev.files.length; i++) {
			FileUnit fu = Kiev.files[i]; 
			if( fu == null ) continue;
			try {
				verify(fu);
			} catch (Exception e) {
				Kiev.reportError(0,e); failed = true;
			}
		}
		return failed;
	}
	
	private void verify(ASTNode:ASTNode node) {
	}
	
	private void verify(FileUnit:ASTNode fu) {
		foreach (ASTNode n; fu.members; n instanceof Struct)
			verify(n);
	}
	
	private void verify(Struct:ASTNode s) {
		Meta m = s.meta.get(mnNode);
		if (m != null) {
			// Check fields of the @node
			foreach (Field f; s.fields) {
				if (f.parent == null) {
					Kiev.reportWarning(f.pos,"Field "+f+" has no parent");
					f.parent = s;
				}
				if (f.parent != s) {
					Kiev.reportError(f.pos,"Field "+f+" has wrong parent "+f.parent);
					return;
				}
				verify(f);
			}
		}
		else if (s.super_clazz != null && s.super_clazz.clazz.meta.get(mnNode) != null) {
			Kiev.reportError(s.pos,"Class "+s+" must be marked with @node: it extends @node "+s.super_clazz);
			return;
		}
	}
	
	private void verify(Field:ASTNode f) {
		Meta fmatt = f.meta.get(mnAtt);
		Meta fmref = f.meta.get(mnRef);
		//if (fmatt != null || fmref != null) {
		//	System.out.println("process @node: field "+f+" has @att="+fmatt+" and @ref="+fmref);
		if (fmatt != null && fmref != null) {
			Kiev.reportError(f.pos,"Field "+f.parent+"."+f+" marked both @att and @ref");
		}
		if (fmatt != null || fmref != null) {
			Struct fs = (Struct)f.type.clazz;
			if (fs.name.name == nameNArr)
				fs = f.type.args[0].clazz;
			Meta fsm = fs.meta.get(mnNode);
			if (fsm == null) {
				Kiev.reportWarning(f.pos,"Type "+fs+" of a field "+f.parent+"."+f+" is not a @node");
				fs.meta.unset(mnAtt);
				fs.meta.unset(mnRef);
				return;
			} else {
				//System.out.println("process @node: field "+f+" of type "+fs+" has correct @att="+fmatt+" or @ref="+fmref);
			}
		} else {
			Struct fs = (Struct)f.type.clazz;
			if (fs.name.name == nameNArr)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
			else if (fs.type.clazz.meta.get(mnNode) != null)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @att or @ref");
		}
	}
	
}
