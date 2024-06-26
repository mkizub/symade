package test.e_dispatching;

class test_010_A {

	void mm1(test_010_A:test_010_A oa) {
		System.out.println("A.mm1 - A:  \t"+oa.getClass());
	}

	void mm2(test_010_A:test_010_A oa1, test_010_A:test_010_A oa2) {
		System.out.println("A.mm1 - A/A:  \t"+oa1.getClass()+"/"+oa2.getClass());
	}

}

class test_010_B extends test_010_A {

	void mm1(test_010_B:test_010_A ob) {
		System.out.println("B.mm1 - B:  \t"+ob.getClass());
	}

	void mm2(test_010_A:test_010_A oa1, test_010_A:test_010_A oa2) {
		System.out.println("B.mm1 - A/A:  \t"+oa1.getClass()+"/"+oa2.getClass());
	}

	void mm2(test_010_B:test_010_A ob1, test_010_B:test_010_A ob2) {
		System.out.println("B.mm1 - B/B:  \t"+ob1.getClass()+"/"+ob2.getClass());
	}

}

class test_010_C extends test_010_B {

	void mm1(test_010_C:test_010_A o) {
		System.out.println("C.mm1 - C: \t"+o.getClass());
	}

	void mm1(test_010_B:test_010_A o) {
		System.out.println("C.mm1 - B:   \t"+o.getClass());
	}

	void mm2(test_010_C:test_010_A o1, test_010_C:test_010_A o2) {
		System.out.println("C.mm1 - C/C:  \t"+o1.getClass()+"/"+o2.getClass());
	}

	void mm2(test_010_A:test_010_A o1, test_010_C:test_010_A o2) {
		System.out.println("C.mm1 - A/C:  \t"+o1.getClass()+"/"+o2.getClass());
	}

	void mm2(test_010_C:test_010_A o1, test_010_A:test_010_A o2) {
		System.out.println("C.mm1 - C/A:  \t"+o1.getClass()+"/"+o2.getClass());
	}

	void mm2(test_010_B:test_010_A o1, test_010_B:test_010_A o2) {
		System.out.println("C.mm1 - B/B:  \t"+o1.getClass()+"/"+o2.getClass());
	}

}

class test_010_D extends test_010_C {

	void mm1(test_010_A:test_010_A o) {
		System.out.println("D.mm1 - A:  \t"+o.getClass());
	}

}

@unerasable
class test_010_X<A> {

	A mm1(test_010_X<A>:test_010_X<A> o) {
		System.out.println("X.mm1 - X:  \t"+o.getClass());
		return null;
	}

	A mm2(test_010_X<A>:test_010_X<A> o1, test_010_X<A>:test_010_X<A> o2) {
		System.out.println("X.mm1 - X/X:  \t"+o1.getClass()+"/"+o2.getClass());
		return null;
	}

}

@unerasable
class test_010_Y<A> extends test_010_X<A> {

	A y;

	A mm1(test_010_Y<A>:test_010_X<A> o) {
		System.out.println("Y.mm1 - Y:  \t"+o.getClass());
		return y;
	}

	A mm2(test_010_Y<A>:test_010_X<A> o1, test_010_Y<A>:test_010_X<A> o2) {
		System.out.println("Y.mm1 - Y/Y:  \t"+o1.getClass()+"/"+o2.getClass());
		return y;
	}

	A mm2(test_010_Y<A>:test_010_X<A> o1, test_010_Y<String>:test_010_X<A> o2) {
		o2.y = "Hello";
		System.out.println("Y.mm1 - Y/Y<String>:  \t"+o1.getClass()+"/"+o2.getClass());
		return o2.y;
	}

	A mm2(test_010_Y<A>:test_010_X<A> o1, test_010_Y<Integer>:test_010_X<A> o2) {
		o2.y = new Integer(1);
		System.out.println("Y.mm1 - Y/Y<Integer>:  \t"+o1.getClass()+"/"+o2.getClass());
		return o2.y;
	}

}

@unerasable
class test_010_N<A> {

	@unerasable case Case1<A>();
	@unerasable case Case2<A>(A a);

	void mm1(Case1 o) {
		System.out.println("Case1.mm1 - N:  \t"+o.getClass());
	}

	void mm1(Case2<A> o) {
		System.out.println("Case2.mm1 - N:  \t"+o.getClass());
	}

	void mm1(Case2<String> o) {
		System.out.println("Case2.mm1 - N<String>:  \t"+o.getClass());
	}

}


class test_010 {
/*
	public static void main(String[] args) {
		test_010_A a = new test_010_A();
		test_010_B b = new test_010_B();
		test_010_C c = new test_010_C();
		test_010_D d = new test_010_D();
		
		test_010_A t;
		
		t = a;
		System.out.println("A:");
		t.mm1(a);
		t.mm1(b);
		t.mm1(c);
		t.mm1(d);
		
		t = b;
		System.out.println("B:");
		t.mm1(a);
		t.mm1(b);
		t.mm1(c);
		t.mm1(d);
		
		t = c;
		System.out.println("C:");
		t.mm1(a);
		t.mm1(b);
		t.mm1(c);
		t.mm1(d);
		
		t = d;
		System.out.println("D:");
		t.mm1(a);
		t.mm1(b);
		t.mm1(c);
		t.mm1(d);
		
		
		t = a;
		System.out.println("A:");
		t.mm2(a,a);
		t.mm2(b,b);
		t.mm2(c,c);
		t.mm2(d,d);
		
		t = b;
		System.out.println("B:");
		t.mm2(a,a);
		t.mm2(a,b);
		t.mm2(b,a);
		t.mm2(b,b);
		
		t = c;
		System.out.println("C:");
		t.mm2(a,a);
		t.mm2(a,b);
		t.mm2(b,a);
		t.mm2(b,b);
		t.mm2(c,a);
		t.mm2(c,b);
		t.mm2(a,c);
		t.mm2(b,c);
		t.mm2(c,c);
		
		
		test_010_X<String> xs = new test_010_X<String>();
		test_010_Y<String> ys = new test_010_Y<String>();
		test_010_X<Integer> xi = new test_010_X<Integer>();
		test_010_Y<Integer> yi = new test_010_Y<Integer>();
		test_010_X<Object> xo = new test_010_X<Object>();
		test_010_Y<Object> yo = new test_010_Y<Object>();
		
		test_010_X<Object> to;

		to = xs;
		System.out.println("X<String>:");
		to.mm1(xs);
		to.mm1(ys);

		to = ys;
		System.out.println("Y<String>:");
		to.mm1(xs);
		to.mm1(ys);

		to = xs;
		System.out.println("X<String>:");
		to.mm2(xs,xs);
		to.mm2(xs,ys);
		to.mm2(ys,xs);
		to.mm2(ys,ys);

		to = ys;
		System.out.println("Y<String>:");
		to.mm2(xs,xs);
		to.mm2(xs,ys);
		to.mm2(ys,xs);
		to.mm2(ys,ys);

		to = yi;
		System.out.println("Y<Integer>:");
		to.mm2(xi,xi);
		to.mm2(xi,yi);
		to.mm2(yi,xi);
		to.mm2(yi,yi);

		to = yo;
		System.out.println("Y<Object>:");
		to.mm2(xo,xo);
		to.mm2(xo,yo);
		to.mm2(yo,xo);
		to.mm2(yo,yo);
		
		test_cased();
	}
	
	static void test_cased() {
		test_010_N.Case1<Object> c1 = test_010_N.Case1;
		test_010_N.Case2<String> c2s = new test_010_N.Case2<String>("Str");
		test_010_N.Case2<Integer> c2i = new test_010_N.Case2<Integer>(new Integer(1));

		test_010_N<Object> tc;

		tc = c1;
		System.out.println("N.Case1<Object>:");
		tc.mm1(c1);
		tc.mm1(c2s);
		tc.mm1(c2i);

	}
*/
}

