package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class PrescannedBody extends ENode {
	
	public static final int BlockMode			= 0;
	public static final int RuleBlockMode		= 1;
	public static final int CondBlockMode		= 2;
	public static final int RewriteMatchMode	= 3;

	public static PrescannedBody[] emptyArray = new PrescannedBody[0];

	@virtual typedef This  = PrescannedBody;
	@virtual typedef VView = PrescannedBodyView;

	public int		lineno;
	public int		columnno;
	public int		mode;
	@ref
	public ASTNode	expected_parent;

	@nodeview
	public static final view PrescannedBodyView of PrescannedBody extends NodeView {
		public int			lineno;	
		public int			columnno;
		public int			mode;
	}

	public PrescannedBody() {}
	
	public PrescannedBody(ASTNode expected_parent, int lineno, int columnno) {
		this.expected_parent = expected_parent;
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

