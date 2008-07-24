package test.j_annotations;

@interface TestAnn {
	String value() default "test";
}

@TestAnn
class Test {
}

@TestAnn("other")
class Test1 {
}
