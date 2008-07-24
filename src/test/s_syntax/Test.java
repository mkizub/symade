package test.s_syntax;

public class Test {
	protected int a = 1, b[] = {10, 15}, c, d;
	int foo() {
		int x;
		int y[], z=0, w[][]={{1},{2,3}};
		for (int a=0, c[][]=null; a < b.length; a++) {}
		String[] sarr = {"hello","world","!"};
		foreach (String s; sarr; s.length() > 0) {}
		Object@ pvar;
		List<Object>@[] lists[];
		return 0;
	}
	void bar(String... strs) {
	}
}

public enum Animals {
	DOG : "dog", CAT : "cat", MOUSE : "mouse";
	static String foo(Animals a) { return a.toString(); }
}
public enum Colors { RED, GREEN, BLUE }

public class Foo<A> {
	case X<A>(A a);
	case Y();
	case Z;
}
