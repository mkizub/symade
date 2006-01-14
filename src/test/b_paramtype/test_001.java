package test.b_paramtype;

class test_001_1<A> {
}

class test_001_2<A,B> extends test_001_1<B> {
}

class test_001_3 extends test_001_2<Integer,Float> {
}

class test_001_4<A> extends test_001_2<test_001_1<Integer>,test_001_2<A,Float>> {
}

@unerasable
class test_001_6<A> {
}

@unerasable
class test_001_7<A,B> extends test_001_6<B> {
}

@unerasable
class test_001_8 extends test_001_7<String,Class> {
}

@unerasable
class test_001_9<A> extends test_001_7<test_001_6<Integer>,test_001_7<A,Float>> {
}
