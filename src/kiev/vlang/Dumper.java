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

import kiev.Main;
import kiev.stdlib.*;

/**
 * @author Maxim Kizub
 * @version $Revision: 182 $
 *
 */

public class Dumper {

	private StringBuffer sb = new StringBuffer(4096);
	private boolean nl = false;
	private boolean sp = false;
	private boolean sp_forsed = false;
	private int indent = 0;
	static String linesep = System.getProperty("line.separator");
	static String indent_image = "  ";
	static boolean force_space = false;

	public Dumper() {}

	private void addNewLine() {
		sb.append(linesep);
		for(int i=0; i < indent; i++)
			sb.append(indent_image);
		nl = false;
		sp = false;
		sp_forsed = false;
	}

	public Dumper append(Type o) {
		if( o == null ) return this;
		return o.toJava(this);
	}

	public Dumper append(Object o) {
		if( o == null ) return this;
		if( o instanceof Type )
			((Type)o).toJava(this);
		else
			append(o.toString());
		return this;
	}

	public Dumper append(ASTNode o) {
		if( o == null ) return this;
		try {
			o.toJava(this);
		} catch( Throwable e ) {
			System.out.println("Internal error in dumper for "+o.getClass());
			e.printStackTrace();
			append("/*INTERNAL ERROR*/");
		}
		return this;
	}

	public Dumper append(String s) {
		if( s == null || s.length() == 0 ) return this;
		if( nl ) addNewLine();
		else if( sp ) {
			if( force_space || s.charAt(0)=='$' || Character.isLetterOrDigit(s.charAt(0)) || sp_forsed )
				sb.append(' ');
			sp = false;
			sp_forsed = false;
		}
		sb.append(s);
		return this;
	}

	public Dumper append(char c) {
		if( nl ) addNewLine();
		else if( sp ) {
			if( force_space || c=='$' || Character.isLetterOrDigit(c) || sp_forsed )
				sb.append(' ');
			sp = false;
			sp_forsed = false;
		}
		sb.append(c);
		return this;
	}

	public Dumper space() {
		int len = sb.length();
		if( len > 0 && (force_space || sb.charAt(len-1)=='$' ||  Character.isLetter(sb.charAt(len-1))) )
			sp = true;
		return this;
	}

	public Dumper forsed_space() {
		sp = true;
		sp_forsed = true;
		return this;
	}

	public Dumper newLine() {
		nl = true;
		sp = false;
		return this;
	}

	public Dumper newLine(int i) {
		indent += i;
		nl = true;
		sp = false;
		sp_forsed = false;
		return this;
	}

	public Dumper newLines(int n) {
		for (int i=0; i < n; i++)
			sb.append(linesep);
		for(int i=0; i < indent; i++)
			sb.append(indent_image);
		nl = false;
		sp = false;
		sp_forsed = false;
		return this;
	}

	public String toString() {
		return sb.toString();
	}

}
