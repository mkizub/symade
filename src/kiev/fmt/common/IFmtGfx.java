package kiev.fmt.common;

public interface IFmtGfx {
	public Object getNative();
	public void layoutText(String text, Draw_Font font);
	public void layoutIcon(Draw_Icon icon);
	public int  textWidth();
	public int  textHeight();
	public int  textBaseline();
}
