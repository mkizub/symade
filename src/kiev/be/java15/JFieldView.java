package kiev.be.java15;

import kiev.*;
import kiev.stdlib.*;
import kiev.parser.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import static kiev.stdlib.Debug.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 242 $
 *
 */

@nodeview
public final view JField of Field extends JLvalDNode {

	public:ro	Access				acc;
	public:ro	Type				ftype;
	public:ro	JENode				init;
	public:ro	JConstExpr			const_value;
	public		Attr[]				attrs;
	
	@getter public final Type	get$type()	{ return ((Field)this).ftype.getType(); }
	
	public final boolean isVirtual();
	public final boolean isEnumField();
	public final boolean isPackerField();
	public final boolean isPackedField();

	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		for(int i=0; i < attrs.length; i++) {
			if(attrs[i].name == a.name) {
				attrs[i] = a;
				return a;
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		Attr[] attrs = this.attrs;
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
	}

	public boolean	isConstantExpr() {
		if( this.isFinal() ) {
			if (this.init != null && this.init.isConstantExpr())
				return true;
			else if (this.const_value != null)
				return true;
		}
		return false;
	}
	public Object	getConstValue() {
		if (this.init != null && this.init.isConstantExpr())
			return this.init.getConstValue();
		else if (this.const_value != null)
			return this.const_value.getConstValue();
    	throw new RuntimeException("Request for constant value of non-constant expression");
	}

}

