package kiev;
import syntax kiev.Syntax;

import java.io.*;

public class SOPTest {
	static boolean verbose;
	static boolean debug;
	
    public static void main(String[] args) {
        String start=".";
		foreach (String arg; args) {
			if (arg.equals("-v") || arg.equals("-verbose") )
				SOPTest.verbose = true;
			else if (arg.equals("-debug") )
				SOPTest.debug = true;
			else
				start = arg;
		}
        visitAllDirsAndFiles(new File(start), start);
    }

    public static void visitAllDirsAndFiles(File dir, String start) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
			java.util.Arrays.sort(children);
            for (int i = 0; i < children.length; i++) {
                visitAllDirsAndFiles(new File(dir, children[i]), 
                        start + "/" + children[i]);
            }
        } else {
            if (dir.getName().endsWith(".java")) {
                String fileName=start;
                System.out.println("="+fileName);
                try {
					String runcmd;
					if (System.getProperty("os.name").startsWith("Windows"))
						runcmd = "symade.bat -no-p -d classes4 ";
					else
						runcmd = "./symade.sh -no-p -d classes4 ";
					if (SOPTest.verbose)
						runcmd = runcmd + "-verbose ";
					if (SOPTest.debug)
						runcmd = runcmd + "-debug all ";
					runcmd = runcmd + fileName;
                    Process process = Runtime.getRuntime().exec(runcmd);
					new MonitorThread(process.getInputStream()).start();
					new MonitorThread(process.getErrorStream()).start();
                    int status = process.waitFor();
                    System.out.println("status="+status);
                    process.destroy();
                    if (status!=0)System.err.println("Compilation error for "+fileName);
                } catch (Exception e) {
                    System.err.println("Error for " + 
                            fileName
                            + "\n" + e);
                }
            }
        }
    }
	
	static class MonitorThread extends Thread {
		final InputStream in;
		MonitorThread(InputStream in) {
			this.in = in;
			setDaemon(true);
		}
		public void run() {
			byte[] buf = new byte[1024];
			int res;
			try {
				while ( (res=in.read(buf)) > 0) {
					if (SOPTest.verbose || SOPTest.debug)
						System.out.write(buf, 0, res);
				}
			} catch (IOException e) {}
		}
	}
}

