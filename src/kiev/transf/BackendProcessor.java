package kiev.transf;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

public abstract class BackendProcessor {
	private Kiev.Backend backend;
	
	BackendProcessor(Kiev.Backend backend) {
		this.backend = backend;
	}
	
	// create back-end nodes
	public void		preGenerate()				{ foreach (FileUnit fu; Kiev.files) preGenerate(fu); }
	public void		preGenerate(ASTNode node)	{ return; }
	// resolve back-end
	public void		resolve(ASTNode node)		{ return; }
	// rewrite back-end nodes
	public void		rewriteNode(ASTNode node)	{ return; }
	// generate back-end
	public void		generate(ASTNode node)		{ return; }
}

