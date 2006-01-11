/* Generated By:JJTree: Do not edit this line. ASTIdentifierAlias.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.transf.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public abstract class ASTAlias extends ASTNode {
	public static ASTAlias[]	emptyArray = new ASTAlias[0];
	
	@virtual typedef NImpl = ASTAliasImpl;
	@virtual typedef VView = ASTAliasView;

	@node
	public static class ASTAliasImpl extends NodeImpl {
		@virtual typedef ImplOf = ASTAlias;
	}
	@nodeview
	public static view ASTAliasView of ASTAliasImpl extends NodeView {
		public ASTAliasView(ASTAliasImpl $view) { super($view); }
	}

	public ASTAlias(ASTAliasImpl v_impl) { super(v_impl); }
	
	public abstract void attach(ASTNode n);

}

public final class ASTIdentifierAlias extends ASTAlias {

	@virtual typedef NImpl = ASTIdentifierAliasImpl;
	@virtual typedef VView = ASTIdentifierAliasView;

	@node
	public static class ASTIdentifierAliasImpl extends ASTAliasImpl {
		@virtual typedef ImplOf = ASTIdentifierAlias;
		@att public NameRef		name;
	}
	@nodeview
	public static view ASTIdentifierAliasView of ASTIdentifierAliasImpl extends ASTAliasView {
		public NameRef		name;
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }
	
	public ASTIdentifierAlias() { super(new ASTIdentifierAliasImpl()); }
	
	public void attach(ASTNode n) {
		switch(n) {
		case Method:
			((Method)n).name.addAlias(name.name);
			break;
		case Field:
			((Field)n).name.addAlias(name.name);
			break;
		default:
			throw new CompilerException(this,"Node of type "+n.getClass()+" cannot have aliases");
		}
	}

	public String toString() {
		return name.toString();
	}
    
	public Dumper toJava(Dumper dmp) {
		return dmp.space().append("/* alias ").append(name).append(" */").space();
	}

}

