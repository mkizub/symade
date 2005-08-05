/* Generated By:JJTree: Do not edit this line. ASTModifiers.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class ASTModifiers extends ASTNode {
	@att public final NArr<ASTModifier>	modifier;
	@att public ASTAccess 					acc;
	@att public ASTPack   					pack;
	@att public final NArr<ASTAnnotation>	annotations;
	
	public void jjtAddChild(ASTNode n, int i) {
		if (n instanceof ASTModifier) {
			modifier.append((ASTModifier)n);
		}
		else if (n instanceof ASTAccess) {
			if (acc != null)
				Kiev.reportParserError(n.pos, "Multiple access specifiers");
			acc = (ASTAccess)n;
		}
		else if (n instanceof ASTPack) {
			if (pack != null)
				Kiev.reportParserError(n.pos, "Multiple field pack instructions");
			pack = (ASTPack)n;
		}
		else if (n instanceof ASTAnnotation) {
			annotations.append((ASTAnnotation)n);
		}
		else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n+" ("+n.getClass()+")");
		}
	}
	
	public int getFlags() {
		int flags = 0;
		foreach (ASTModifier m; modifier)
			flags |= m.flag();
		return flags;
	}

	public MetaSet getMetas(MetaSet ms) {
	next_annotation:
		foreach (ASTAnnotation a; annotations) {
			Meta m = a.getMeta();
			if (m != null)
				ms.set(m);
		}
		return ms;
	}

    public Dumper toJava(Dumper dmp) {
		for(int i=0; i < modifier.length; i++)
			modifier[i].toJava(dmp);
		return dmp;
    }
}

