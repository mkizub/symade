package kiev.be.java;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

import kiev.vlang.FileUnit.FileUnitImpl;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeview
public final view JFileUnit of FileUnitImpl extends JDNode {
	public				KString					filename;
	public				TypeNameRef				pkg;
	public:ro	NArr<DNode>				syntax;
	public:ro	NArr<DNode>				members;
	public:ro	NArr<PrescannedBody>	bodies;
	public:ro	boolean[]				disabled_extensions;
	public				boolean					scanned_for_interface_only;

	@getter public JFileUnit get$jctx_file_unit() { return this; }

	public final FileUnit getFileUnit() { return (FileUnit)this.getNode(); }
	
	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		KString cur_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (DNode dn; members; dn instanceof Struct) {
				diff_time = curr_time = System.currentTimeMillis();
				((Struct)dn).getJView().generate();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose )
					Kiev.reportInfo("Generated clas "+dn,diff_time);
			}
		} finally { Kiev.curFile = cur_file; Kiev.setExtSet(exts); }
	}

	static void make_output_dir(String top_dir, String filename) throws IOException {
		File dir;
		dir = new File(top_dir,filename);
		dir = new File(dir.getParent());
		dir.mkdirs();
		if( !dir.exists() || !dir.isDirectory() ) throw new RuntimeException("Can't create output dir "+dir);
	}
}

