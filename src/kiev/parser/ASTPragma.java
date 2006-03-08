/* Generated By:JJTree: Do not edit this line. ASTPragma.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;

@node
public final class ASTPragma extends DNode {

	@virtual typedef This  = ASTPragma;
	@virtual typedef VView = VASTPragma;

	@att public boolean				enable;
	@att public NArr<ConstStringExpr>	options;

	@nodeview
	public static view VASTPragma of ASTPragma extends VDNode {
		public		boolean					enable;
		public:ro	NArr<ConstStringExpr>	options;
	}

	public ASTPragma() {}
	
	public void resolve(Type reqType) {}
	
	public Dumper toJavaDecl(Dumper dmp) {
		return toJava(dmp);
	}
	
	public Dumper toJava(Dumper dmp) {
		dmp.append("/* pragma ").append(enable?"enable":"disable").space();
		foreach (ConstStringExpr e; options)
			dmp.forsed_space().append(e);
		return dmp.append("; */").newLine();
	}
}
