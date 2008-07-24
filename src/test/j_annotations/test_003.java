package test.j_annotations;

import java.lang.annotation.Annotation;

@interface TestAnn {
}

@TestAnn
class Test {
	public static void main(String[] args) {
		System.out.println(Test.class.getAnnotation(TestAnn.class));
	}
}
