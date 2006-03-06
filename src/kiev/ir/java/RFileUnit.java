package kiev.ir.java;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.FileUnit.FileUnitView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RFileUnit of FileUnit extends FileUnitView {
	
	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving file "+filename);
		KString curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			for(int i=0; i < members.length; i++) {
				try {
					members[i].resolveDecl();
				} catch(Exception e) {
					Kiev.reportError(members[i],e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}
}

