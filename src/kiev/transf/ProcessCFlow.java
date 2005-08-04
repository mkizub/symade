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

public final class ProcessCFlow implements Constants {

	public static final KString mnCFNode = KString.from("kiev.vlang.cfnode"); 
	public static final KString mnCFLink = KString.from("kiev.vlang.cflink"); 
	public static final KString nameNArr  = KString.from("kiev.vlang.NArr"); 
	
	/////////////////////////////////////////////
	//      Verify the CFNode graph structure  //
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
		KString oldfn = Kiev.curFile;
		Kiev.curFile = fu.filename;
		PassInfo.push(fu);
		try {
			foreach (ASTNode n; fu.members) {
				verify(n);
			}
		} finally { PassInfo.pop(fu); Kiev.curFile = oldfn; }
	}
	
	private void verify(Struct:ASTNode s) {
		Meta m = s.meta.get(mnCFNode);
		if (m != null) {
			// Check fields of the @cfnode
			//foreach (ASTNode n; s.members; n instanceof Field) {
			//	verify(n);
			//}
		}
		else if (s.super_clazz != null && s.super_clazz.clazz.meta.get(mnCFNode) != null) {
			Kiev.reportError(s.pos,"Class "+s+" must be marked with @cfnode: it extends @cfnode "+s.super_clazz);
			return;
		}
	}
	
	private void verify(Field:ASTNode f) {
		Meta fmlnk = f.meta.get(mnCFLink);
		Struct fs = (Struct)f.type.clazz;
		boolean isArr = false;
		if (fs.name.name == nameNArr) {
			fs = f.type.args[0].clazz;
			isArr = true;
		}
		if (fmlnk != null) {
			Meta fsm = fs.meta.get(mnCFNode);
			if (fsm == null) {
				Kiev.reportWarning(f.pos,"Type "+fs+" of a field "+f.parent+"."+f+" is not a @cfnode");
				fs.meta.unset(mnCFLink);
				return;
			}
		} else {
			if (fs.type.clazz.meta.get(mnCFNode) != null)
				Kiev.reportWarning(f.pos,"Field "+f.parent+"."+f+" must be marked with @cflink");
		}
	}
	
}
