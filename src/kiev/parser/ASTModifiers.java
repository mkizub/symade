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
	     public int							modifier;
	@att public Access 						acc;
	@att public final NArr<Meta>			annotations;
	
	public int getFlags() {
		return modifier;
	}

	public MetaSet getMetas(MetaSet ms) {
	next_annotation:
		foreach (Meta v; annotations) {
			try {
				v = v.verify();
			} catch (CompilerException e) {
				Kiev.reportError(pos, e);
				continue;
			}
			v = (Meta)v.copy();
			ms.set(v);
		}
		return ms;
	}

    public Dumper toJava(Dumper dmp) {
		foreach (Meta m; annotations)
			dmp.append(m);
		Env.toJavaModifiers(dmp,(short)modifier);
		if( (modifier & ACC_VIRTUAL		) > 0 ) dmp.append("/*virtual*/ ");
		if( (modifier & ACC_FORWARD		) > 0 ) dmp.append("/*forward*/ ");
		
		if (acc != null) dmp.append(acc.toString());
		
		return dmp;
    }
}

