package test.k_enum;

public enum test_003 {
 zero(0),
 one(1),
 two(2);
 
 private int idx;
 
 private test_003(int idx) {
   this.idx = idx;
 }
 
 public int getIndex() { idx }
 
}

