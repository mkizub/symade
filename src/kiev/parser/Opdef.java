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

@node
public class Opdef extends DNode {
	@virtual typedef This  = Opdef;
	@virtual typedef VView = VOpdef;

	@att public int					prior;
	@att public int					opmode;
	@att public KString				image;
	@ref public Operator			resolved;

	@nodeview
	public static view VOpdef of Opdef extends VDNode {
		public int					prior;
		public int					opmode;
		public KString				image;
		public Operator				resolved;
	}

	public Opdef() {}
	
	public Opdef(Operator op) {
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

