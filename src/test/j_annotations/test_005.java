package test.j_annotations;

@interface TestAnn1 {
	String test1() default "test";
	String test2() default "test";
}

@TestAnn1
class Test1 {
}

@TestAnn1(test1="other")
class Test2 {
}

@TestAnn1(test2="other")
class Test3 {
}

@TestAnn1(test1="other", test2="other")
class Test4 {
}

@interface TestAnn2 {
	String test1();
	String test2() default "test";
}

@TestAnn2(test1="other")
class Test5 {
}

@TestAnn2(test1="other", test2="other")
class Test6 {
}

@interface TestAnn3 {
	String test1();
	String test2();
}

@TestAnn3(test1="other", test2="other")
class Test7 {
}
