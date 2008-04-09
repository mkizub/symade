package test.b_paramtype;

import java.util.Vector;
import kiev.stdlib.*;
import java.lang.*;
import java.util.*;

class test_005 {

	public static void main(String[] args) {

		String[] arr = new String[]{"Hello"," ","world"};
		foreach(String s; arr)
			System.out.print(s);
		System.out.println("!");
		
		int[] arr1 = new int[]{ 0,1,2,3,4,5,6,7,8,9 };
		foreach(int i; arr1; (i & 1) != 0 )
			System.out.print(i + ", ");
		System.out.println("!");
		
		java.util.Vector vec = new java.util.Vector();
		vec.setSize(10);
		for(int i=0; i < 10; i++) vec.setElementAt(new Integer(i),i);
		foreach(Object i; vec)
			System.out.print(i + ", ");
		System.out.println("!");
		foreach(Object i; vec.elements(); (((Integer)i).intValue() & 1) != 0 )
			System.out.print(i + ", ");
		System.out.println("!");
		
		List<Integer> lst = List.Nil;
		for(int i=0; i < 10; i++) lst = new List.Cons<Integer>(new Integer(i),lst);
		foreach(Integer i; lst)
			System.out.print(i + ", ");
		System.out.println("!");
		foreach(Integer i; lst.elements())
			System.out.print(i + ", ");
		System.out.println("!");
		
		vec = new Vector();
		vec.setSize(10);
		for(int i=0; i < 10; i++) vec.setElementAt(new Integer(i),i);
		foreach(Object i; vec)
			System.out.print(i + ", ");
		System.out.println("!");
	}
}
