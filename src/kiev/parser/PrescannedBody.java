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
	@virtual typedef VView = PrescannedBodyView;

	public int			lineno;	
	public int			columnno;
	public int			mode;

	@nodeview
	public static final view PrescannedBodyView of PrescannedBody extends NodeView {
		public int			lineno;	
		public int			columnno;
		public int			mode;
	}

	public PrescannedBody() {}
	
	public PrescannedBody(int lineno, int columnno) {
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

