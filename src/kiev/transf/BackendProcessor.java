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

public abstract class BackendProcessor implements Constants {
	private Kiev.Backend backend;
	
	BackendProcessor(Kiev.Backend backend) {
		this.backend = backend;
	}
	public boolean isEnabled() {
		return this.backend == Kiev.Backend.Generic || this.backend == Kiev.useBackend;
	}

	public abstract String getDescr();
	
	// create back-end nodes
	public void process(ASTNode node) { return; }
}

