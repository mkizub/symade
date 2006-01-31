/* Generated By:JJTree: Do not edit this line. ASTPragma.java */

package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.stdlib.*;

@nodeset
public final class ASTPragma extends DNode {

	@virtual typedef This  = ASTPragma;
	@virtual typedef NImpl = ASTPragmaImpl;
	@virtual typedef VView = ASTPragmaView;

	@nodeimpl
	static class ASTPragmaImpl extends DNodeImpl {
		@virtual typedef ImplOf = ASTPragma;
		@att boolean				enable;
		@att NArr<ConstStringExpr>	options;
	}
	@nodeview
	public static view ASTPragmaView of ASTPragmaImpl extends DNodeView {
		ASTPragmaView(ASTPragmaImpl $view) {
			super($view);
			this.$view = $view;
		}
		public           boolean				enable;
		public:ro NArr<ConstStringExpr>	options;
	}
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public ASTPragma() { super(new ASTPragmaImpl()); }
	
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
