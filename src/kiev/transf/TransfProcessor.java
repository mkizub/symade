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

public abstract class TransfProcessor {
	
	private final Kiev.Ext extension;
	
	public TransfProcessor(Kiev.Ext ext) {
		this.extension = ext;
		Kiev.transfProcessors[(int)ext] = this;
	}
	public boolean isEnabled() {
		return Kiev.enabled(extension);
	}
	public boolean isDisabled() {
		return Kiev.disabled(extension);
	}
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    Import        //////////////////////////////
	///////////////////////////////////////////////////////////////////////
	// process syntax
	public void			pass1(ASTNode node)					{ return; }
	// resolve and create types of classes
	public void			pass2(ASTNode node)					{ return; }
	// process meta declarations
	public void			resolveMetaDecl(ASTNode node)		{ return; }
	// process meta default values
	public void			resolveMetaDefaults(ASTNode node)	{ return; }
	// process meta values of classes and members
	public void			resolveMetaValues(ASTNode node)		{ return; }
	// process declared class members
	public void			pass3(ASTNode node)					{ return; }
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    VNode language     /////////////////////////
	///////////////////////////////////////////////////////////////////////
	// auto-create class members
	public void			autoGenerateMembers(ASTNode node)	{ return; }
	// resolve vnodes
	public void			preResolve(ASTNode node)			{ return; }
	// resolve vnodes
	public void			mainResolve(ASTNode node)			{ return; }
	// verify resolved tree
	public void			verify(ASTNode node)				{ return; }
	
	///////////////////////////////////////////////////////////////////////
	///////////////////////    Back-end    ////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	
	public BackendProcessor getBackend(Kiev.Backend backend) { return null; }
}

