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

/** a class for errors due to missing patterns
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/MatchError.java,v 1.3 1998/10/26 23:47:14 max Exp $
 * @author   Martin Odersky
 * @author Maxim Kizub
 * @version $Revision: 1.3 $
 *
 */
public class MatchError extends Error {

    public MatchError() {
	super();
    }

    public MatchError(String s) {
	super(s);
    }
} 

