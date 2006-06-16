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

public abstract class TransfProcessor implements Constants {
	
	private final Kiev.Ext extension;
	
	public TransfProcessor(Kiev.Ext ext) {
		this.extension = ext;
	}
	public boolean isEnabled() {
		return Kiev.enabled(extension);
	}
	public boolean isDisabled() {
		return Kiev.disabled(extension);
	}
	
	public abstract String getDescr();
	public abstract void process(ASTNode node, Transaction tr);
}

