package test.k_enum;

public enum test_004 {
 zero,
 one(1),
 two(2);
 
 private int idx;
 
 private test_004() {
   this(0);
 }

 private test_004(int idx) {
   this.idx = idx;
 }
 
 public int getIndex() { idx }
 
}

