/*
 Copyright (C) 1997-1998, Forestro, http://forestro.com

 This file is part of the Kiev library.
 
 The Kiev library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Library General Public License as
 published by the Free Software Foundation.

 The Kiev library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU Library General Public
 License along with the Kiev compiler; see the file License.  If not,
 write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 Boston, MA 02111-1307, USA.
*/
  
package kiev.stdlib;

/**
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/MessageNotUnderstoodException.java,v 1.2 1998/10/21 19:44:44 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public class MessageNotUnderstoodException extends RuntimeException {
	public int		arity;
	public int		hash;
	public String	name;
	public MessageNotUnderstoodException() { super(); }
	public MessageNotUnderstoodException(String msg) { super(msg); }
	public MessageNotUnderstoodException(int arity,int hash) {
		super(hash+"/"+arity);
	}
	public MessageNotUnderstoodException(int arity, int hash, String name) {
		super(name+"/"+arity);
	}
}

