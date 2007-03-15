package test.j_annotations;

@interface TestAnn {
	String[] value() default {"test1","test2"};
}

@TestAnn
class Test1 {
}

@TestAnn("other")
class Test2 {
}

@TestAnn({"other","other"})
class Test3 {
}

@TestAnn(value = {"other","other"})
class Test4 {
}
