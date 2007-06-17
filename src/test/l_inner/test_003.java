package test.l_inner;

public class test_003 {
 private test_003() {}
 private test_003(int i) {}
 
 class Inner {
   void test() {
     new test_003();
     new test_003(1);
   }
 }

 public static void main(String[] args) {
   test_003 t = new test_003();
   test_003.Inner i = t.new Inner();
   i.test();
 }

}

