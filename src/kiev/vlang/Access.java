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
 * $Header$
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface Accessable {
	public virtual abstract Access	acc;
	public abstract Access get$acc();
	public abstract void   set$acc(Access a);
}

public class Access implements Constants {
	public int flags;
	public packed:1,flags,7 boolean	r_public;
	public packed:1,flags,6 boolean	w_public;
	public packed:1,flags,5 boolean	r_protected;
	public packed:1,flags,4 boolean	w_protected;
	public packed:1,flags,3 boolean	r_default;
	public packed:1,flags,2 boolean	w_default;
	public packed:1,flags,1 boolean	r_private;
	public packed:1,flags,0 boolean	w_private;

	public Access(int flags) {
		this.flags = flags;
	}

	public boolean readable() {
		return r_private || r_default || r_protected || r_public;
	}
	public boolean writeable() {
		return w_private || w_default || w_protected || w_public;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("access:");

		if( r_public && w_public ) sb.append("rw,");
		else if( r_public ) sb.append("r,");
		else if( w_public ) sb.append("w,");
		else sb.append("n,");

		if( r_protected && w_protected ) sb.append("rw,");
		else if( r_protected ) sb.append("r,");
		else if( w_protected ) sb.append("w,");
		else sb.append("n,");

		if( r_default && w_default ) sb.append("rw,");
		else if( r_default ) sb.append("r,");
		else if( w_default ) sb.append("w,");
		else sb.append("n,");

		if( r_private && w_private ) sb.append("rw");
		else if( r_private ) sb.append("r");
		else if( w_private ) sb.append("w");
		else sb.append("n");

		return sb.toString();
	}

	public void verifyAccessDecl(ASTNode n) {

		if( flags == 0 ) {
			if( n.isPublic() ) flags = 0xFF;
			else if( n.isProtected() ) flags = 0x3F;
			else if( n.isPrivate() ) flags = 0x3;
			else flags = 0xF;
		}

		if( r_public || w_public ) {
			if( !n.isPublic() ) {
				Kiev.reportWarning(n.pos,"Node "+n+" needs to be declared public");
				n.setPublic(true);
			}
		}
		else if( r_protected || w_protected ) {
			if( !(n.isPublic() || n.isProtected()) ) {
				Kiev.reportWarning(n.pos,"Node "+n+" needs to be declared protected or public");
				n.setProtected(true);
			}
		}
		else if( r_default || w_default ) {
			if( n.isPrivate() ) {
				Kiev.reportWarning(n.pos,"Node "+n+" needs to be declared with default/protected or public access");
				n.setPrivate(false);
			}
		}
		if( r_public ) {
			if( !r_protected ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have protected read access");
				r_protected = true;
			}
			if( !r_default ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have default (package) read access");
				r_default = true;
			}
			if( !r_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_protected ) {
			if( !r_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_default ) {
			if( !r_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( w_public ) {
			if( !w_protected ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have protected write access");
				w_protected = true;
			}
			if( !w_default ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have default (package) write access");
				w_default = true;
			}
			if( !w_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_protected ) {
			if( !w_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_default ) {
			if( !w_private ) {
				Kiev.reportWarning(n.pos,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
	}

	public void verifyReadAccess(ASTNode n) { verifyAccess(n,2); }
	public void verifyWriteAccess(ASTNode n) { verifyAccess(n,1); }
	public void verifyReadWriteAccess(ASTNode n) { verifyAccess(n,3); }

	private Struct getStructOf(ASTNode n) {
		if( n instanceof Struct ) return (Struct)n;
		return (Struct)n.parent;
	}

	private Struct getPackageOf(ASTNode n) {
		Struct pkg = getStructOf(n);
		while( !pkg.isPackage() ) pkg = pkg.package_clazz;
		return pkg;
	}

	public void verifyAccess(ASTNode n, int acc) {
		assert( n instanceof Accessable && ((Accessable)n).acc == this );

		// Quick check for public access
		if( ((flags>>>6) & acc) == acc ) return;

		// Check for private access
		if( PassInfo.clazz == getStructOf(n) ) {
			if( (flags & acc) != acc ) throwAccessError(n,acc,"private");
			return;
		}

		// Check for private access from inner class
		if (n.isPrivate()) {
			Struct outer1 = PassInfo.clazz;
			Struct outer2 = getStructOf(n);
			while (!outer1.package_clazz.isPackage())
				outer1 = outer1.package_clazz;
			while (!outer2.package_clazz.isPackage())
				outer2 = outer2.package_clazz;
			if (outer1 == outer2) {
				if( (flags & acc) == acc ) {
					n.setAccessedFromInner(true);
					return;
				}
				throwAccessError(n,acc,"private");
			}
		}

		// Check for default access
		if( getPackageOf(PassInfo.clazz) == getPackageOf(n) ) {
			if( ((flags>>>2) & acc) != acc ) throwAccessError(n,acc,"default");
			return;
		}

		// Check for protected access
		if( PassInfo.clazz.instanceOf(getStructOf(n)) ) {
			if( ((flags>>>4) & acc) != acc ) throwAccessError(n,acc,"protected");
			return;
		}

		// Public was already checked, just throw an error
		throwAccessError(n,acc,"public");
	}

	private void throwAccessError(ASTNode n, int acc, String astr) {
		StringBuffer sb = new StringBuffer();
		sb.append("Access denied - ").append(astr).append(' ');
		if( acc == 2 ) sb.append("read");
		else if( acc == 1 ) sb.append("write");
		else if( acc == 3 ) sb.append("read/write");
		sb.append("\n\tto ");
		if( n instanceof Field ) sb.append("field ");
		else if( n instanceof Method ) sb.append("method ");
		else if( n instanceof Struct ) sb.append("class ");
		if( n instanceof Struct ) sb.append(n);
		else sb.append(n.parent).append('.').append(n);
		sb.append("\n\tfrom class ").append(PassInfo.clazz);
		throw new RuntimeException(sb.toString());
	}
}
