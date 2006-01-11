package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java.JNodeView;
import kiev.be.java.JDNodeView;
import kiev.be.java.JLvalDNodeView;
import kiev.be.java.JFieldView;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public final class Field extends LvalDNode implements Named, Typed, Accessable {
	public static Field[]	emptyArray = new Field[0];

	@dflow(out="init") private static class DFI {
	@dflow(in="this:in")	ENode			init;
	}

	@virtual typedef NImpl = FieldImpl;
	@virtual typedef VView = FieldView;
	@virtual typedef JView = JFieldView;

	@node
	public static class FieldImpl extends LvalDNodeImpl {
		@virtual typedef ImplOf = Field;
		public FieldImpl() {}
		public FieldImpl(int pos) { super(pos); }
		public FieldImpl(int pos, int fl) { super(pos, fl); }

		/** Field' access */
		     public Access				acc;
		/** Name of the field */
		     public NodeName			name;
		/** Type of the field */
		@att public TypeRef				ftype;
		/** Initial value of this field */
		@att public ENode				init;
		/** Constant value of this field */
		@ref public ConstExpr			const_value;
		/** Array of attributes of this field */
		public kiev.be.java.Attr[]		attrs = kiev.be.java.Attr.emptyArray;
		/** Array of invariant methods, that check this field */
		@ref public NArr<Method>		invs;

		public void callbackChildChanged(AttrSlot attr) {
			if (parent != null && pslot != null) {
				if      (attr.name == "ftype")
					parent.callbackChildChanged(pslot);
				else if (attr.name == "meta")
					parent.callbackChildChanged(pslot);
			}
		}
	}
	@nodeview
	public static view FieldView of FieldImpl extends LvalDNodeView {
		public				Access			acc;
		public				NodeName		name;
		public				TypeRef			ftype;
		public				ENode			init;
		public				ConstExpr		const_value;
		public access:ro	NArr<Method>	invs;
		
		@setter public final void set$acc(Access val)	{ this.$view.acc = val; Access.verifyDecl((Field)getDNode()); }
		@getter public final Type	get$type()			{ return this.$view.ftype.getType(); }
		
		// is a field of enum
		public final boolean isEnumField() {
			return this.$view.is_fld_enum;
		}
		public final void setEnumField(boolean on) {
			if (this.$view.is_fld_enum != on) {
				this.$view.is_fld_enum = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// packer field (auto-generated for packed fields)
		public final boolean isPackerField() {
			return this.$view.is_fld_packer;
		}
		public final void setPackerField(boolean on) {
			if (this.$view.is_fld_packer != on) {
				this.$view.is_fld_packer = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
		// packed field
		public final boolean isPackedField() {
			return this.$view.is_fld_packed;
		}
		public final void setPackedField(boolean on) {
			if (this.$view.is_fld_packed != on) {
				this.$view.is_fld_packed = on;
				this.$view.callbackChildChanged(nodeattr$flags);
			}
		}
	}

	public VView getVView() alias operator(210,fy,$cast) { return new VView(this.$v_impl); }
	public JView getJView() alias operator(210,fy,$cast) { return new JView(this.$v_impl); }

	public Field() { super(new FieldImpl()); }
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(KString name, TypeRef ftype, int acc) {
		super(new FieldImpl(0,acc));
		this.name = new NodeName(name);
		this.ftype = ftype;
		this.meta = new MetaSet();
		trace(Kiev.debugCreation,"New field created: "+name+" with type "+ftype);
	}

	public Field(KString name, Type type, int acc) {
		this(name,new TypeRef(type),acc);
	}
	
	public final MetaVirtual getMetaVirtual() {
		return (MetaVirtual)this.getNodeData(MetaVirtual.ID);
	}

	public final MetaPacked getMetaPacked() {
		return (MetaPacked)this.getNodeData(MetaPacked.ID);
	}

	public final MetaPacker getMetaPacker() {
		return (MetaPacker)this.getNodeData(MetaPacker.ID);
	}

	public final MetaAlias getMetaAlias() {
		return (MetaAlias)this.getNodeData(MetaAlias.ID);
	}

	public String toString() { return name.toString()/*+":="+type*/; }

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
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

	public void resolveDecl() throws RuntimeException {
		foreach (Meta m; meta)
			m.resolve();
		if( init != null ) {
			if (init instanceof TypeRef)
				((TypeRef)init).toExpr(type);
			init.resolve(type);
			if (init.getType() ≉ type) {
				init = new CastExpr(init.pos, type, init);
				init.resolve(type);
			}
		}
		setResolved(true);
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(KString.Empty) )
			type.toJava(dmp).forsed_space().append(name);
		if( init != null ) {
			if( !name.equals(KString.Empty) )
				dmp.append(" = ");
			init.toJava(dmp);
		}
		return dmp.append(';');
	}
}

