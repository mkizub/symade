package test.l_inner;

public class test_002 {
 private int f;
 private static int s;
 
 class Inner {
   void test() {
     int x;
     x = f;
     x = s;
     f = x;
     s = x;
     s = f = s = x;
     f += 2;
     s += 2;
     f--;
     --f;
     s++;
     ++s;
   }
 }

 public static void main(String[] args) {
   test_002 t = new test_002();
   test_002.Inner i = t.new Inner();
   i.test();
 }

}

