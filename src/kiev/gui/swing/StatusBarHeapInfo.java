package kiev.gui.swing;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.TimerTask;

import javax.swing.JProgressBar;
import javax.swing.border.BevelBorder;

import kiev.gui.IWindow;

@SuppressWarnings("serial")
public class StatusBarHeapInfo extends JProgressBar {

	class HeapTimerTask extends TimerTask {
		@Override
		public void run() {
			statusUpdate();
		}
	}
	
	class MouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			e.consume();
			if (e.getClickCount() == 2)
				System.gc();
		}
	}
	
	@SuppressWarnings("unused")
	private final IWindow window;
	
	StatusBarHeapInfo(IWindow window) {
		this.window = window;
		this.setBorder(new BevelBorder(BevelBorder.LOWERED));
		this.setStringPainted(true);
		Window.guiTimer.schedule(new HeapTimerTask(), 1000L, 1000L);
		this.addMouseListener(new MouseListener());
		this.setToolTipText("Java Heap information (used / total)");
	}
	
	public Dimension getMaximumSize() {
	    return getPreferredSize();
	}
	
	void statusUpdate() {
		try {
			// Read MemoryMXBean
			List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();
			long used = 0L, total = 0L;
			for (MemoryPoolMXBean mempoolmbean : mempoolsmbeans.toArray(new MemoryPoolMXBean[0])) {
				if (mempoolmbean.getType() != MemoryType.HEAP)
					continue;
				MemoryUsage mu = mempoolmbean.getUsage();
				if (mu != null) {
					total += mu.getMax();
					used  += mu.getUsed();
				}
			}
			int scale_mb = 1024*1024;
			setInfo((int)(used/scale_mb), (int)(total/scale_mb));
		}
		catch (Exception e) {}
	}
	
	void setInfo(int used, int total) {
		StatusBarHeapInfo.this.setMaximum(total);
		StatusBarHeapInfo.this.setValue(used);
		StatusBarHeapInfo.this.setString("heap "+used+" / "+total+ " Mb");
	}

}
