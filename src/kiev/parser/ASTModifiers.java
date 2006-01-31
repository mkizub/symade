/* Generated By:JJTree: Do not edit this line. ASTModifiers.java */

package kiev.parser;

import kiev.Kiev;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;

import kiev.vlang.types.TypeRef.TypeRefImpl;
import kiev.vlang.types.TypeRef.TypeRefView;

/**
 * @author Maxim Kizub
 *
 */

@nodeset
public final class ASTModifiers extends ASTNode {
	
	@virtual typedef This  = ASTModifiers;
	@virtual typedef NImpl = ASTModifiersImpl;
	@virtual typedef VView = ASTModifiersView;

	@nodeimpl
	public static final class ASTModifiersImpl extends NodeImpl {
		@virtual typedef ImplOf = ASTModifiers;
		@att public Access 				acc;
		@att public NArr<Meta>			annotations;
		     public MetaSpecial[]		specials = MetaSpecial.emptyArray;
	}
	@nodeview
	public static final view ASTModifiersView of ASTModifiersImpl extends NodeView {
		public				Access 				acc;
		public:ro	NArr<Meta>			annotations;
		public				MetaSpecial[]		specials;		
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public ASTModifiers() { super(new ASTModifiersImpl()); }
	
	public MetaSpecial add(MetaSpecial sa)
		alias operator(5, lfy, +=)
	{
		this.specials = (MetaSpecial[])Arrays.append(this.specials, sa);
		return sa;
	}

    public Dumper toJava(Dumper dmp) {
		foreach (Meta m; annotations)
			dmp.append(m);
		//Env.toJavaModifiers(dmp,(short)modifier);
		//if( (modifier & ACC_VIRTUAL		) > 0 ) dmp.append("/*virtual*/ ");
		//if( (modifier & ACC_FORWARD		) > 0 ) dmp.append("/*forward*/ ");
		
		if (acc != null) dmp.append(acc.toString());
		
		return dmp;
    }
}

