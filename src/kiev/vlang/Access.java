package kiev.vlang;

import kiev.Kiev;
import kiev.stdlib.*;

import kiev.be.java15.JNode;

import static kiev.stdlib.Debug.*;

/**
 * $Header$
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

public interface Accessable {
	@virtual
	public abstract Access	acc;
}

public class Access implements Constants {
	public int flags;
	public @packed:1,flags,7 boolean	r_public;
	public @packed:1,flags,6 boolean	w_public;
	public @packed:1,flags,5 boolean	r_protected;
	public @packed:1,flags,4 boolean	w_protected;
	public @packed:1,flags,3 boolean	r_default;
	public @packed:1,flags,2 boolean	w_default;
	public @packed:1,flags,1 boolean	r_private;
	public @packed:1,flags,0 boolean	w_private;

	public Access(int flags) {
		this.flags = flags | (flags << 16);
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
	
	public static final int getFlags(DNode n) {
		int flags;
		Access acc = ((Accessable)n).acc;
		if (acc != null)
			return acc.flags;
		else if (n.isPublic())
			return 0xFF;
		else if (n.isProtected())
			return 0x3F;
		else if (n.isPrivate())
			return 0x03;
		return 0x0F;
	}

	public static boolean readable(Accessable n) {
		int flags = getFlags((DNode)n);
		return (flags & 0xAA) != 0;
	}
	public static boolean writeable(Accessable n) {
		int flags = getFlags((DNode)n);
		return (flags & 0x55) != 0;
	}

	public static void verifyDecl(Accessable n) {
		Access acc = n.acc;
		if (acc != null)
			acc.verifyAccessDecl((DNode)n);
	}
	
	private void verifyAccessDecl(DNode n) {
		flags &= 0xFFFF0000;
		flags |= (flags >>> 16);
		if( flags == 0 ) {
			if( n.isPublic() ) flags = 0xFF;
			else if( n.isProtected() ) flags = 0x3F;
			else if( n.isPrivate() ) flags = 0x3;
			else flags = 0xF;
		}

		if( r_public || w_public ) {
			if( !n.isPublic() ) {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared public");
				n.setPublic();
			}
		}
		else if( r_protected || w_protected ) {
			if( !(n.isPublic() || n.isProtected()) ) {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared protected or public");
				n.setProtected();
			}
		}
		else if( r_default || w_default ) {
			if( n.isPrivate() ) {
				Kiev.reportWarning(n,"Node "+n+" needs to be declared with default/protected or public access");
				n.setPrivate();
			}
		}
		if( r_public ) {
			if( !r_protected ) {
				Kiev.reportWarning(n,"Node "+n+" should have protected read access");
				r_protected = true;
			}
			if( !r_default ) {
				Kiev.reportWarning(n,"Node "+n+" should have default (package) read access");
				r_default = true;
			}
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_protected ) {
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( r_default ) {
			if( !r_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private read access");
				r_private = true;
			}
		}
		if( w_public ) {
			if( !w_protected ) {
				Kiev.reportWarning(n,"Node "+n+" should have protected write access");
				w_protected = true;
			}
			if( !w_default ) {
				Kiev.reportWarning(n,"Node "+n+" should have default (package) write access");
				w_default = true;
			}
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_protected ) {
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
		if( w_default ) {
			if( !w_private ) {
				Kiev.reportWarning(n,"Node "+n+" should have private write access");
				w_private = true;
			}
		}
	}

	public static void verifyRead(ASTNode from, DNode n) { verifyAccess(from,n,2); }
	public static void verifyWrite(ASTNode from, DNode n) { verifyAccess(from,n,1); }
	public static void verifyReadWrite(ASTNode from, DNode n) { verifyAccess(from,n,3); }
	public static void verifyRead(ASTNode.NodeView from, ASTNode.NodeView n) { verifyAccess((ASTNode)from,(ASTNode)n,2); }
	public static void verifyWrite(ASTNode.NodeView from, ASTNode.NodeView n) { verifyAccess((ASTNode)from,(ASTNode)n,1); }
	public static void verifyReadWrite(ASTNode.NodeView from, ASTNode.NodeView n) { verifyAccess((ASTNode)from,(ASTNode)n,3); }
	public static void verifyRead(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,2); }
	public static void verifyWrite(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,1); }
	public static void verifyReadWrite(JNode from, JNode n) { verifyAccess((ASTNode)from,(ASTNode)n,3); }

	private static TypeDecl getStructOf(ASTNode n) {
		if( n instanceof TypeDecl ) return (TypeDecl)n;
		return n.ctx_tdecl;
	}

	private static Struct getPackageOf(ASTNode n) {
		TypeDecl pkg = getStructOf(n);
		while( !pkg.isPackage() ) pkg = pkg.package_clazz;
		return (Struct)pkg;
	}

	private static void verifyAccess(ASTNode from, ASTNode n, int acc) {
		int flags = getFlags((DNode)n);

		// Quick check for public access
		if( ((flags>>>6) & acc) == acc ) return;

		// Check for private access
		if( getStructOf(from) == getStructOf(n) ) {
			if( (flags & acc) != acc ) throwAccessError(from,n,acc,"private");
			return;
		}

		// Check for private access from inner class
		if (((DNode)n).isPrivate()) {
			TypeDecl outer1 = getStructOf(from);
			TypeDecl outer2 = getStructOf(n);
			while (!outer1.package_clazz.isPackage())
				outer1 = outer1.package_clazz;
			while (!outer2.package_clazz.isPackage())
				outer2 = outer2.package_clazz;
			if (outer1 == outer2) {
				if( (flags & acc) == acc ) {
					n.setAccessedFromInner(true);
					return;
				}
				throwAccessError(from,n,acc,"private");
			}
		}

		// Check for default access
		if( getPackageOf(from) == getPackageOf(n) ) {
			if( ((flags>>>2) & acc) != acc ) throwAccessError(from,n,acc,"default");
			return;
		}

		// Check for protected access
		if( getStructOf(from).instanceOf(getStructOf(n)) ) {
			if( ((flags>>>4) & acc) != acc ) throwAccessError(from,n,acc,"protected");
			return;
		}

		// Public was already checked, just throw an error
		throwAccessError(from,n,acc,"public");
	}

	private static void throwAccessError(ASTNode from, ASTNode n, int acc, String astr) {
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
		else sb.append(n.parent()).append('.').append(n);
		sb.append("\n\tfrom class ").append(getStructOf(from));
		Kiev.reportError(from,new RuntimeException(sb.toString()));
	}
}
