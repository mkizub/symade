package kiev.parser;

import kiev.Kiev;
import kiev.vlang.*;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node
public final class PrescannedBody extends ASTNode {
	
	public static final int BlockMode		= 0;
	public static final int RuleBlockMode	= 1;
	public static final int CondBlockMode	= 2;

	public static PrescannedBody[] emptyArray = new PrescannedBody[0];

	public int			lineno;	
	public int			columnno;
	public int			mode;
	
	public PrescannedBody() { super(new NodeImpl()); }
	
	public PrescannedBody(int lineno, int columnno) {
		super(new NodeImpl());
		this.lineno = lineno;	
		this.columnno = columnno;
	}
}

