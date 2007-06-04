package kiev;

import java.io.*;

import syntax kiev.Syntax;

public class SOPTest {
    public static void main(String[] args) {
        String start=".";
        if (args.length>0)start=args[0];
        visitAllDirsAndFiles(new File(start), start);
    }

    public static void visitAllDirsAndFiles(File dir, String start) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllDirsAndFiles(new File(dir, children[i]), 
                        start + "/" + children[i]);
            }
        } else {
            if (dir.getName().endsWith(".java")) {
                String fileName=start;
                System.out.println("="+fileName);
                try {
                    Process process = Runtime.getRuntime().exec(
                            "symade.bat -no-p -d classes4 " + fileName);
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
				while ( (res=in.read(buf)) > 0)
					; //System.out.write(buf, 0, res);
			} catch (IOException e) {}
		}
	}
}

