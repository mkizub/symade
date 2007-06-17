package test.l_inner;

public class test_001 {
 private void test0() { System.out.println("test0()->void"); }
 private int  test1() { System.out.println("test1()->int"); return 0; }
 private void test2(int arg) { System.out.println("test2(int)->void"); }
 private int  test3(int arg) { System.out.println("test3(int)->int"); return arg; }
 private static void stest0() { System.out.println("static stest0()->void"); }
 private static int  stest1() { System.out.println("static stest1()->int"); return 0; }
 private static void stest2(int arg) { System.out.println("static stest2(int)->void"); }
 private static int  stest3(int arg) { System.out.println("static stest3(int)->int"); return arg; }
 
 void test() {
  test0();
  test1();
  test2(2);
  test3(3);
  stest0();
  stest1();
  stest2(2);
  stest3(3);
 }
 class Inner {
   void test() {
     test0();
     test1();
     test2(2);
     test3(3);
     stest0();
     stest1();
     stest2(2);
     stest3(3);
   }
 }

 public static void main(String[] args) {
   test_001 t = new test_001();
   t.test();
   test_001.Inner i = t.new Inner();
   i.test();
 }

}

