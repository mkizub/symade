package test.f_syntax;

import syntax test.f_syntax.syntax_002;
import syntax kiev.stdlib.Syntax;

syntax syntax_002 {
	import static test.f_syntax.test_002_1.*;
	import static test.f_syntax.test_002_2.a2;
	import static test.f_syntax.test_002_2.foo2();
	
	import static test.f_syntax.test_002_3.a3;
	import static test.f_syntax.test_002_3.bar(int);
}

public class test_002 {
	public static void main(String[] args) {
		System.out.println("a1 = "+a1);
		System.out.println("foo1() = "+foo1());
		System.out.println("a2 = "+a2);
		System.out.println("foo2() = "+foo2());
		System.out.println("a3 = "+a3);
		System.out.println("bar(100) = "+bar(100));
	}
}

public class test_002_1 {
	public static int	a1 = 1;
	public static int	foo1() { return a1; }
}


public class test_002_2 {
	public static int	a2 = 2;
	public static int	foo2() { return a2 + a1; }
}

public class test_002_3 {
	public static int	a3 = 1;
	public static int	bar(int i) { return foo1() + foo2() + a3 + i; }
}



