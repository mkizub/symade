package kiev.gui.swt;

import kiev.gui.IBgFormatter;
import kiev.gui.UIView;

public class BgFormatter implements IBgFormatter {
	private boolean do_format;
	private UIView view;
	public BgFormatter(UIView view) {
		this.view = view;
	}
	public void schedule_run() {
		view.formatAndPaint(true);		
	}
	public void start() {
		// TODO Auto-generated method stub
		
	}

}
