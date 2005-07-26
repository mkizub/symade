/* Generated By:JJTree: Do not edit this line. ASTAnnotation.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;

/**
 * @author Maxim Kizub
 *
 */

@node
public class ASTAnnotation extends ASTNode {

	public static ASTAnnotation[] emptyArray = new ASTAnnotation[0];

	@att public ASTIdentifier            ident;	
	@att public final NArr<ASTAnnotationValue> values;
	
	public ASTAnnotation() {}

	public ASTAnnotation(int id) {
		super(0);
		values = new NArr<ASTAnnotationValue>(this);
	}

	public void jjtAddChild(ASTNode n, int i) {
		if (n instanceof ASTIdentifier) {
			ident = (ASTIdentifier)n;
			this.pos = n.pos;
		}
		else if (n instanceof ASTAnnotationValue) {
			values.append((ASTAnnotationValue)n);
		}
		else {
			throw new CompilerException(n.getPos(),"Bad child number "+i+": "+n+" ("+n.getClass()+")");
		}
	}

	public Meta getMeta() {
		ASTNode n = null;
		try {
			n = ident.resolve(null);
		} catch (Exception e) {
			Kiev.reportError(pos, e);
		}
		if (!(n instanceof Struct) || !n.isAnnotation()) {
			Kiev.reportError(pos, "Annotation name expected");
			return null;
		}
		Struct s = (Struct)n;
		Meta m = new Meta(new MetaType(s.name.name));
		foreach (ASTAnnotationValue v; values) {
			m.set(makeValue(v));
		}
		return m;
	}

	public static MetaValue makeValue(ASTAnnotationValue v) {
		KString name;
		if (v.ident == null)
			name = KString.from("value");
		else
			name = v.ident.name;
		MetaValueType mvt = new MetaValueType(name);
		if (v.value instanceof ASTAnnotation) {
			ASTNode value = v.value;
			value = ((ASTAnnotation)value).getMeta();
			return new MetaValueScalar(mvt, value);
		}
		else if (v.value instanceof ASTAnnotationValueValueArrayInitializer) {
			MetaValueArray mva = new MetaValueArray(mvt);
			foreach (ASTNode av; ((ASTAnnotationValueValueArrayInitializer)v.value).values) {
				if (av instanceof ASTAnnotation)
					mva.values.add(((ASTAnnotation)av).getMeta());
				else
					mva.values.add(av);
			}
			return mva;
		}
		else {
			return new MetaValueScalar(mvt, v.value);
		}
	}
}
