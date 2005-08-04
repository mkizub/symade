/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev compiler.

 The Kiev compiler is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License as
 published by the Free Software Foundation.

 The Kiev compiler is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with the Kiev compiler; see the file License.  If not, write to
 the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/

package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import static kiev.stdlib.Debug.*;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/vlang/Field.java,v 1.3.2.1.2.3 1999/05/29 21:03:11 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.3.2.1.2.3 $
 *
 */

@node
public class Field extends ASTNode implements Named, Typed, Accessable, TopLevelDecl {
	public static Field[]	emptyArray = new Field[0];

	/** Field' access */
	@virtual
	public virtual Access	acc;

	/** Name of the field */
	public NodeName			name;

	/** Type of the field */
	@ref public Type		type;

	/** Initial value of this field */
	@att public Expr		init = null;

	/** Meta-information (annotations) of this structure */
	@att public MetaSet		meta;

	/** Array of attributes of this field */
	public Attr[]			attrs = Attr.emptyArray;

	/** Array of invariant methods, that check this field */
	public Method[]			invs = Method.emptyArray;

	@ref public Field		generated_from;

	public static class PackInfo {
		public int		size;
		public int		offset = -1;
		public Field	packer;
		public KString	packer_name;
		public PackInfo(int size) {
			this.size = size;
			this.offset = -1;
		}
		public PackInfo(int size,Field packer) {
			this.size = size;
			this.packer = packer;
			if( packer != null )
				this.packer_name = packer.name.name;
		}
		public PackInfo(int size,KString packer) {
			this.size = size;
			this.packer_name = packer;
		}
		public PackInfo(int size,int offset,Field packer) {
			this.size = size;
			this.offset = offset;
			this.packer = packer;
			if( packer != null )
				this.packer_name = packer.name.name;
		}
		public PackInfo(int size,int offset,KString packer) {
			this.size = size;
			this.offset = offset;
			this.packer_name = packer;
		}
	}

	public Field() {
	}
	
    /** Constructor for new field
	    This constructor must not be called directly,
	    but via factory method newField(...) of Clazz
     */
	public Field(ASTNode clazz, KString name, Type type, int acc) {
		super(0,acc);
		this.name = new NodeName(name);
		this.type = type;
        // Parent node is always a class this field was declared in
		this.parent = clazz;
		this.acc = new Access(0);
		this.meta = new MetaSet(this);
		trace(Kiev.debugCreation,"New field created: "+name
			+" with type "+type);
	}

	@getter public Access get$acc() {
		return acc;
	}

	@setter public void set$acc(Access a) {
		acc = a;
		acc.verifyAccessDecl(this);
	}
	
	public MetaVirtual getMetaVirtual() {
		return (MetaVirtual)this.meta.get(MetaVirtual.NAME);
	}

	public MetaPacked getMetaPacked() {
		return (MetaPacked)this.meta.get(MetaPacked.NAME);
	}

	public MetaPacker getMetaPacker() {
		return (MetaPacker)this.meta.get(MetaPacker.NAME);
	}

	public void jjtAddChild(ASTNode n, int i) {
		throw new RuntimeException("Bad compiler pass to add child");
	}

	public String toString() { return name.toString()/*+":="+type*/; }

	public NodeName getName() { return name; }

	public Type	getType() { return type; }

	public Dumper toJava(Dumper dmp) {
		return dmp.space().append(name).space();
	}

	/** Add information about new attribute that belongs to this class */
	public Attr addAttr(Attr a) {
		// Check we already have this attribute
		if( !(a.name==attrOperator || a.name==attrImport
			|| a.name==attrRequire || a.name==attrEnsure) ) {
			for(int i=0; i < attrs.length; i++) {
				if(attrs[i].name == a.name) {
					attrs[i] = a;
					return a;
				}
			}
		}
		attrs = (Attr[])Arrays.append(attrs,a);
		return a;
	}

	public Attr getAttr(KString name) {
		for(int i=0; i < attrs.length; i++)
			if( attrs[i].name.equals(name) )
				return attrs[i];
		return null;
	}

	public ASTNode resolve(Type reqType) throws RuntimeException {
		foreach (Meta m; meta)
			m.resolve();
		if( name.equals(KString.Empty) ) return this;
		if( init != null ) {
			if( init instanceof Expr )
				init = ((Expr)init).resolveExpr(type);
		}
		return this;
	}

	public Dumper toJavaDecl(Dumper dmp) {
		Env.toJavaModifiers(dmp,getJavaFlags());
		if( !name.equals(KString.Empty) )
			Type.getRealType(Kiev.argtype,type).toJava(dmp).forsed_space().append(name);
		if( init != null ) {
			if( !name.equals(KString.Empty) )
				dmp.append(" = ");
			init.toJava(dmp);
		}
		return dmp.append(';');
	}
}

