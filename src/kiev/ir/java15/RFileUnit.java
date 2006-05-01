package kiev.ir.java15;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public static final view RFileUnit of FileUnit extends RDNode {
	public		String					filename;
	public		TypeNameRef				pkg;
	public:ro	NArr<ASTNode>			members;
	public:ro	boolean[]				disabled_extensions;
	public		boolean					scanned_for_interface_only;

	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving file "+filename);
		String curr_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (DNode n; members) {
				try {
					n.resolveDecl();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}
}

