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
 * $Header: /home/CVSROOT/forestro/kiev/kiev/stdlib/FirstOrderFunction.java,v 1.2 1998/10/21 19:44:42 max Exp $
 * @author Maxim Kizub
 * @version $Revision: 1.2 $
 *
 */

public class FirstOrderFunction extends closure implements Applayable, Cloneable {

	public Applayable	self;
	public Object		obj;
	public int			hash;
	public String		name;
	
	public FirstOrderFunction(Applayable self, Object obj, int hash, int arity) {
		super(arity);
		this.self = self;
		this.obj  = obj;
		this.hash = hash;
	}

	public FirstOrderFunction(Applayable self, Object obj, int hash, int arity, String name) {
		super(arity);
		this.self = self;
		this.obj  = obj;
		this.hash = hash;
		this.name = name;
	}

	public void		call_void()		{ $message$0(hash,name); return; }
	public boolean	call_boolean()	{ return ((Cell)$message$0(hash,name)).to_boolean(); }
	public byte		call_byte()		{ return ((Cell)$message$0(hash,name)).to_byte(); }
	public char		call_char()		{ return ((Cell)$message$0(hash,name)).to_char(); }
	public short	call_short()	{ return ((Cell)$message$0(hash,name)).to_short(); }
	public int		call_int()		{ return ((Cell)$message$0(hash,name)).to_int(); }
	public long		call_long()		{ return ((Cell)$message$0(hash,name)).to_long(); }
	public float	call_float()	{ return ((Cell)$message$0(hash,name)).to_float(); }
	public double	call_double()	{ return ((Cell)$message$0(hash,name)).to_double(); }
	public Object	call_Object()	{ return $message$0(hash,name); }
	
	public Object clone() {
		try {
			return super.clone();
		} catch( java.lang.CloneNotSupportedException e) {
			throw new RuntimeException("internal error: "+e.getMessage());
		}
	}
	
	public Object $message$0(int hash, String signature) {
		if( top$arg != max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		switch( max$args ) {
		case 0:		return self.$applay$0(hash,obj);
		case 1:		return self.$applay$1(hash,obj,$args[0]);
		case 2:		return self.$applay$2(hash,obj,$args[0],$args[1]);
		case 3:		return self.$applay$3(hash,obj,$args[0],$args[1],$args[2]);
		case 4:		return self.$applay$4(hash,obj,$args[0],$args[1],$args[2],$args[3]);
		default:	return self.$applay$N(hash,obj,$args);
		}
	}

	public Object $message$1(int hash, String signature, Object arg1) {
		if( top$arg+1!= max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		switch( max$args ) {
//		case 0:		return self.$applay$0(hash,obj);
		case 1:		return self.$applay$1(hash,obj,arg1);
		case 2:		return self.$applay$2(hash,obj,$args[0],arg1);
		case 3:		return self.$applay$3(hash,obj,$args[0],$args[1],arg1);
		case 4:		return self.$applay$4(hash,obj,$args[0],$args[1],$args[2],arg1);
		default:
			$args[top$arg+1] = arg1;
			return self.$applay$N(hash,$args);
		}
	}

	public Object $message$2(int hash, String signature, Object arg1, Object arg2) {
		if( top$arg+2!= max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		switch( max$args ) {
//		case 0:		return self.$applay$0(hash,obj);
//		case 1:		return self.$applay$1(hash,obj,arg1);
		case 2:		return self.$applay$2(hash,obj,arg1,arg2);
		case 3:		return self.$applay$3(hash,obj,$args[0],arg1,arg2);
		case 4:		return self.$applay$4(hash,obj,$args[0],$args[1],arg1,arg2);
		default:
			$args[top$arg+1] = arg1;
			$args[top$arg+2] = arg2;
			return self.$applay$N(hash,$args);
		}
	}

	public Object $message$3(int hash, String signature, Object arg1, Object arg2, Object arg3) {
		if( top$arg+3!= max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		switch( max$args ) {
//		case 0:		return self.$applay$0(hash);
//		case 1:		return self.$applay$1(hash,obj,arg1);
//		case 2:		return self.$applay$2(hash,obj,arg1,arg2);
		case 3:		return self.$applay$3(hash,obj,arg1,arg2,arg3);
		case 4:		return self.$applay$4(hash,obj,$args[0],arg1,arg2,arg3);
		default:
			$args[top$arg+1] = arg1;
			$args[top$arg+2] = arg2;
			$args[top$arg+3] = arg3;
			return self.$applay$N(hash,$args);
		}
	}

	public Object $message$4(int hash, String signature, Object arg1, Object arg2, Object arg3, Object arg4) {
		if( top$arg+4!= max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		switch( max$args ) {
//		case 0:		return self.$applay$0(hash,obj);
//		case 1:		return self.$applay$1(hash,obj,arg1);
//		case 2:		return self.$applay$2(hash,obj,arg1,arg2);
//		case 3:		return self.$applay$3(hash,obj,arg1,arg2,arg3);
		case 4:		return self.$applay$4(hash,obj,arg1,arg2,arg3,arg4);
		default:
			$args[top$arg+1] = arg1;
			$args[top$arg+2] = arg2;
			$args[top$arg+3] = arg3;
			$args[top$arg+4] = arg4;
			return self.$applay$N(hash,$args);
		}
	}

	public Object $message$N(int hash, String signature, ...) {
		if( top$arg+va_args.length!= max$args )
			throw new MessageNotUnderstoodException(0,hash,signature);
		if( top$arg == 0 )
			return self.$applay$N(hash,va_args);
		for(int i=top$arg, j=0; i < max$args; i++, j++) {
			$args[i] = va_args[j];
		}
		return self.$applay$N(hash,obj,$args);
	}

}

