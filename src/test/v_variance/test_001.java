package test.v_variance;

public class test_001<+A,-B,C> {
	// check co-variant A
	public A																fld11;
	public test_001<A,Object,Object>										fld12;
	public test_001<test_001<A,Object,Object>,Object,Object>				fld13;
	
	public final A															fld21;
	public final test_001<A,Object,Object>									fld22;
	public final test_001<test_001<A,Object,Object>,Object,Object>		fld23;

	public:ro A																fld31;
	public:ro test_001<A,Object,Object>									fld32;
	public:ro test_001<test_001<A,Object,Object>,Object,Object>			fld33;

	// check contra-variant B
	public B																fld41;
	public test_001<Object,B,Object>										fld42;
	public test_001<test_001<Object,B,Object>,B,Object>					fld43;
	
	public final B															fld51;
	public final test_001<Object,B,Object>									fld52;
	public final test_001<test_001<Object,B,Object>,B,Object>				fld53;

	public:ro B																fld61;
	public:ro test_001<Object,B,Object>									fld62;
	public:ro test_001<test_001<Object,B,Object>,B,Object>					fld63;

	// check in-variant C
	public C																fld71;
	public test_001<Object,Object,C>										fld72;
	public test_001<test_001<Object,Object,C>,Object,C>					fld73;
	
	public final C															fld81;
	public final test_001<Object,Object,C>									fld82;
	public final test_001<test_001<Object,Object,C>,Object,C>				fld83;

	public:ro C																fld91;
	public:ro test_001<Object,Object,C>									fld92;
	public:ro test_001<test_001<Object,Object,C>,Object,C>					fld93;

	public A fooA() { return null; }
	public B fooB() { return null; }
	public C fooC() { return null; }
	public test_001<A,Object,Object> barA() { return null; }
	public test_001<Object,B,Object> barB() { return null; }
	public test_001<Object,Object,C> barC() { return null; }

	public void fooA(A a) {}
	public void fooB(B b) {}
	public void fooC(C c) {}
	public void barA(test_001<A,Object,Object> ta) {}
	public void barB(test_001<Object,B,Object> tb) {}
	public void barC(test_001<Object,Object,C> tc) {}

	public <X ≤ A> X fooXextendsA() { return null; }
	public <X ≥ A> X fooXsuperA()   { return null; }
	public <X ≤ B> X fooXextendsB() { return null; }
	public <X ≥ B> X fooXsuperB()   { return null; }
	public <X ≤ C> X fooXextendsC() { return null; }
	public <X ≥ C> X fooXsuperC()   { return null; }
	
	public <X ≤ A> test_001<X,Object,Object> barXextendsA() { return null; }
	public <X ≥ A> test_001<X,Object,Object> barXsuperA()   { return null; }
	public <X ≤ B> test_001<Object,X,Object> barXextendsB() { return null; }
	public <X ≥ B> test_001<Object,X,Object> barXsuperB()   { return null; }
	public <X ≤ C> test_001<Object,Object,X> barXextendsC() { return null; }
	public <X ≥ C> test_001<Object,Object,X> barXsuperC()   { return null; }
	
	public <X ≤ A> void fooXextendsA(X a) {}
	public <X ≥ A> void fooXsuperA(X a) {}
	public <X ≤ B> void fooXextendsB(X a) {}
	public <X ≥ B> void fooXsuperB(X a) {}
	public <X ≤ C> void fooXextendsC(X a) {}
	public <X ≥ C> void fooXsuperC(X a) {}

	public <X ≤ A> void barXextendsA(test_001<X,Object,Object> ta) {}
	public <X ≥ A> void barXsuperA(test_001<X,Object,Object> ta) {}
	public <X ≤ B> void barXextendsB(test_001<Object,X,Object> ta) {}
	public <X ≥ B> void barXsuperB(test_001<Object,X,Object> ta) {}
	public <X ≤ C> void barXextendsB(test_001<Object,Object,X> ta) {}
	public <X ≥ C> void barXsuperB(test_001<Object,Object,X> ta) {}

}
