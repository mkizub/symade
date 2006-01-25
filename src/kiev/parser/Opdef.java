package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

import static kiev.stdlib.Debug.*;
import static kiev.vlang.OpTypes.*;
import static kiev.vlang.Operator.*;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeset
public class Opdef extends DNode {
	@virtual typedef NImpl = OpdefImpl;
	@virtual typedef VView = OpdefView;

	@nodeimpl
	static class OpdefImpl extends DNodeImpl {
		@virtual typedef ImplOf = Opdef;
		OpdefImpl() {}
		@att int					prior;
		@att int					opmode;
		@att KString				image;
		@ref Operator				resolved;
	}
	@nodeview
	public static class OpdefView extends DNodeView {
		final OpdefImpl impl;
		OpdefView(OpdefImpl impl) {
			super(impl);
			this.impl = impl;
		}
		@getter public final int				get$prior()		{ return this.impl.prior; }
		@getter public final int				get$opmode()	{ return this.impl.opmode; }
		@getter public final KString			get$image()		{ return this.impl.image; }
		@getter public final Operator			get$resolved()	{ return this.impl.resolved; }
		
		@setter public final void set$prior(int val)			{ this.impl.prior = val; }
		@setter public final void set$opmode(int val)			{ this.impl.opmode = val; }
		@setter public final void set$image(KString val)		{ this.impl.image = val; }
		@setter public final void set$resolved(Operator val)	{ this.impl.resolved = val; }
	}
	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public Opdef() {
		super(new OpdefImpl());
	}
	
	public Opdef(Operator op) {
		super(new OpdefImpl());
		this.prior = op.priority;
		this.opmode = op.mode;
		this.image = op.image;
		this.resolved = op;
	}
	
	public void setImage(ASTNode n) {
		this.pos = n.pos;
		if( n instanceof ASTOperator ) {
			image = ((ASTOperator)n).image;
			return;
		}
		else if( n instanceof NameRef ) {
			image = ((NameRef)n).name;
			return;
		}
		throw new CompilerException(n,"Bad operator definition");
	}
	
	public void setMode(NameRef n) {
		opmode = -1;
		KString optype = ((NameRef)n).name;
		for(int i=0; i < Operator.orderAndArityNames.length; i++) {
			if( Operator.orderAndArityNames[i].equals(optype) ) {
				opmode = i;
				break;
			}
		}
		if( opmode < 0 )
			throw new CompilerException(n,"Operator mode must be one of "+Arrays.toString(Operator.orderAndArityNames));
		return;
	}
	
	public void setPriority(ConstIntExpr n) {
		prior = n.value;
		if( prior < 0 || prior > 255 )
			throw new CompilerException(n,"Operator priority must have value from 0 to 255");
		pos = n.pos;
		return;
	}
	
	public String toString() {
		return image.toString();
	}

	public void resolveDecl() {
	}
	
	public Dumper toJavaDecl(Dumper dmp) {
		return toJava(dmp);
	}
	
	public Dumper toJava(Dumper dmp) {
		return dmp.space().append("/* operator ")
			.append(Integer.toString(prior)).forsed_space()
			.append(Operator.orderAndArityNames[opmode]).forsed_space()
			.append(image).append(" */").space();
	}

}

