package kiev.be.java;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.*;
import java.io.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@nodeview
public class JFileUnitView extends DNode.DNodeView {
	final FileUnit.FileUnitImpl impl;
	public JFileUnitView(FileUnit.FileUnitImpl impl) {
		super(impl);
		this.impl = impl;
	}
	@getter public final KString				get$filename()	{ return this.impl.filename; }
	@getter public final TypeNameRef			get$pkg()		{ return this.impl.pkg; }
	@getter public final NArr<DNode>			get$syntax()	{ return this.impl.syntax; }
	@getter public final NArr<DNode>			get$members()	{ return this.impl.members; }
	@getter public final NArr<PrescannedBody>	get$bodies()	{ return this.impl.bodies; }
	@getter public final boolean[]				get$disabled_extensions()			{ return this.impl.disabled_extensions; }
	@getter public final boolean				get$scanned_for_interface_only()	{ return this.impl.scanned_for_interface_only; }

	@setter public final void set$filename(KString val)					{ this.impl.filename = val; }
	@setter public final void set$pkg(TypeNameRef val)						{ this.impl.pkg = val; }
	@setter public final void set$scanned_for_interface_only(boolean val)	{ this.impl.scanned_for_interface_only = val; }

	public void generate() {
		long curr_time = 0L, diff_time = 0L;
		KString cur_file = Kiev.curFile;
		Kiev.curFile = filename;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach (DNode dn; members; dn instanceof Struct) {
				diff_time = curr_time = System.currentTimeMillis();
				((Struct)dn).getJStructView().generate();
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

