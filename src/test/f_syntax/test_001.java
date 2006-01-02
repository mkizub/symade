package test.f_syntax;

import syntax_001;

syntax syntax_001 {
	
	import test_001_A;
	import test_001_B.gf_b;
	import test_001_B.gm_b();

}

public static class test_001_A {
	
	static int gf_a = 1;
	
	static int gm_a() { return 10; }
	static int gm_a1(int i) { return i; }
}
	
public class test_001_B {

	public static int gf_b = 3;
	
	public static int gm_b() { return 30; }
	
}


public class test_001 {

	public static void main(String[] args) {
	
		int gv_a = gf_a;
		int gv_b = gf_b;
	
		System.out.println(""+(gv_a+gm_a()));
		System.out.println(""+(gv_b+gm_b()));
	
	}
	
}


