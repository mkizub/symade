package kiev.dump.bin;

public class CommentElem extends Elem {

	public String text;
	
	public CommentElem(String text) {
		super(0);
		this.text = text;
	}
	public CommentElem(int id, int addr) {
		super(id, addr);
	}
}
