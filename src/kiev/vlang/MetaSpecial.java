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
 * @author Maxim Kizub
 *
 */

@node
public class MetaVirtual extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.virtual");

	/** Getter/setter methods for this field */
	@ref public Method		get;
	@ref public Method		set;

	public MetaVirtual() {
		super(new MetaType(NAME));
	}

	public MetaVirtual(MetaType type) {
		super(type);
	}
	
}

@node
public class MetaPacked extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.packed");
	public static final KString nameSize   = KString.from("size");
	public static final KString nameOffset = KString.from("offset");
	public static final KString nameIn     = KString.from("in");

	@ref
	public Field			 packer;
	
	@virtual
	public virtual abstract int size;
	@virtual
	public virtual abstract int offset;
	@virtual
	public virtual abstract int in;

	public MetaPacked() {
		super(new MetaType(NAME));
	}

	public MetaPacked(MetaType type) {
		super(type);
	}
	
	@getter public int get$size() { return getI(nameSize); }
	@setter public void set$size(int val) { setI(nameSize, val); }
	@getter public int get$offset() { return getI(nameOffset); }
	@setter public void set$offset(int val) { setI(nameOffset, val); }
	@getter public KString get$fld() { return getS(nameIn); }
	@setter public void set$fld(KString val) { setS(nameIn, val); }
}

@node
public class MetaPacker extends Meta {
	public static final KString NAME = KString.from("kiev.stdlib.meta.packer");
	public static final KString nameSize = KString.from("size");

	@virtual
	public virtual abstract int size;

	public MetaPacker() {
		super(new MetaType(NAME));
	}

	public MetaPacker(MetaType type) {
		super(type);
	}
	
	@getter public int get$size() { return getI(nameSize); }
	@setter public void set$size(int val) { setI(nameSize, val); }
}


