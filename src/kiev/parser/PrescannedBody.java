package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public final class PrescannedBody extends ASTNode {
	
	public static final int BlockMode		= 0;
	public static final int RuleBlockMode	= 1;
	public static final int CondBlockMode	= 2;

	public static PrescannedBody[] emptyArray = new PrescannedBody[0];

	@virtual typedef This  = PrescannedBody;
	@virtual typedef NImpl = PrescannedBodyImpl;
	@virtual typedef VView = PrescannedBodyView;

	@nodeimpl
	public static final class PrescannedBodyImpl extends NodeImpl {
		@virtual typedef ImplOf = PrescannedBody;
		public int			lineno;	
		public int			columnno;
		public int			mode;
	}
	@nodeview
	public static final view PrescannedBodyView of PrescannedBodyImpl extends NodeView {
		public int			lineno;	
		public int			columnno;
		public int			mode;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	
	public PrescannedBody() { super(new PrescannedBodyImpl()); }
	
	public PrescannedBody(int lineno, int columnno) {
		super(new PrescannedBodyImpl());
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

