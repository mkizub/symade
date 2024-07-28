/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev.stdlib;

import syntax kiev.stdlib.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision: 213 $
 *
 */

public class ArrayEnumerator<A> implements Enumeration<A>, Cloneable
{

	private A[]		arr;
	private int		top;
	
	public ArrayEnumerator(A[] arr) {
		this.arr = arr;
	}
	
	public boolean	hasMoreElements() {
		return arr != null && top < arr.length;
	}
	public A		nextElement() {
		return arr[top++];
	}
	
	public static boolean contains(A[] ar, A val) {
		for(int i=0; i < ar.length; i++) {
			if( val!=null && (val == ar[i] || val.equals(ar[i])) )
				return true;
		}
		return false;
	}
}

