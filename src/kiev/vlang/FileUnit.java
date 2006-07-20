package kiev.vlang;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.parser.*;
import kiev.stdlib.*;
import kiev.transf.*;
import kiev.vlang.types.*;
import java.io.*;

import kiev.be.java15.JNode;
import kiev.be.java15.JDNode;
import kiev.ir.java15.RFileUnit;
import kiev.be.java15.JFileUnit;
import kiev.be.java15.JStruct;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 * @version $Revision$
 *
 */

@node(name="FileUnit")
public final class FileUnit extends DNode implements Constants, ScopeOfNames, ScopeOfMethods {

	@virtual typedef This  = FileUnit;
	@virtual typedef JView = JFileUnit;
	@virtual typedef RView = RFileUnit;

	@att public TypeNameRef		pkg;
	@att public ASTNode[]		members;
	
	@ref public PrescannedBody[]	bodies;
		 public final boolean[]		disabled_extensions = Kiev.getCmdLineExtSet();
		 public boolean				scanned_for_interface_only;

	@getter public FileUnit get$ctx_file_unit() { return (FileUnit)this; }
	@getter public TypeDecl get$ctx_tdecl() { return null; }
	@getter public TypeDecl get$child_ctx_tdecl() { return null; }
	@getter public Method get$ctx_method() { return null; }
	@getter public Method get$child_ctx_method() { return null; }

	public FileUnit() {
		this("", Env.root);
	}
	public FileUnit(String name, Struct pkg) {
		this.id = name;
		this.pkg = new TypeNameRef(pkg.qname());
		this.pkg.lnk = pkg.xtype;
	}

	public void addPrescannedBody(PrescannedBody b) {
		bodies.append(b);
	}

	public String toString() { return String.valueOf(id); }

	public void resolveMetaDefaults() {
		trace(Kiev.debugResolve,"Resolving meta defaults in file "+id);
		String curr_file = Kiev.curFile;
		Kiev.curFile = id.sname;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(Struct n; members) {
				try {
					n.resolveMetaDefaults();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public void resolveMetaValues() {
		trace(Kiev.debugResolve,"Resolving meta values in file "+id);
		String curr_file = Kiev.curFile;
		Kiev.curFile = id.sname;
		boolean[] exts = Kiev.getExtSet();
        try {
        	Kiev.setExtSet(disabled_extensions);
			foreach(Struct n; members) {
				try {
					n.resolveMetaValues();
				} catch(Exception e) {
					Kiev.reportError(n,e);
				}
			}
		} finally { Kiev.curFile = curr_file; Kiev.setExtSet(exts); }
	}

	public boolean preResolveIn() {
		foreach (Import imp; members) {
			try {
				imp.resolveImports();
			} catch(Exception e ) {
				Kiev.reportError(imp,e);
			}
		}
		return true;
	}

	public void setPragma(ASTPragma pr) {
		foreach (ConstStringExpr e; pr.options)
			setExtension(e,pr.enable,e.value.toString());
	}

	private void setExtension(ASTNode at, boolean enabled, String s) {
		Ext ext;
		try {
			ext = Ext.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning(at,"Unknown pragma '"+s+"'");
			return;
		}
		int i = ((int)ext)-1;
		if (enabled && Kiev.getCmdLineExtSet()[i])
			Kiev.reportError(this,"Extension '"+s+"' was disabled from command line");
		disabled_extensions[i] = !enabled;
	}
	
	private boolean debugTryResolveIn(String name, String msg) {
		trace(Kiev.debugResolve,"Resolving "+name+" in "+msg);
		return true;
	}

	public rule resolveNameR(ASTNode@ node, ResInfo path)
		ASTNode@ syn;
	{
		syn @= members,
		{
			syn instanceof DNode && path.checkNodeName(syn),
			node ?= syn
		;	syn instanceof Import && !((Import)syn).star,
			((Import)syn).resolveNameR(node,path)
		;	syn instanceof Opdef && path.checkNodeName(syn),
			node ?= syn
		}
	;
		pkg != null && path.space_prev.pslot().name != "pkg",
		trace( Kiev.debugResolve, "In file package: "+pkg),
		((CompaundType)pkg.getType()).clazz.resolveNameR(node,path)
	;
		syn @= members,
		syn instanceof Import,
		((Import)syn).star,
		((Import)syn).resolveNameR(node,path)
	;
		trace( Kiev.debugResolve, "In root package"),
		path.enterMode(ResInfo.noForwards|ResInfo.noImports) : path.leaveMode(),
		Env.root.resolveNameR(node,path)
	}

	public rule resolveMethodR(Method@ node, ResInfo path, CallType mt)
		ASTNode@ syn;
	{
		pkg != null,
		pkg.getStruct().resolveMethodR(node,path,mt)
	;	syn @= members,
		syn instanceof Import && ((Import)syn).mode == Import.ImportMode.IMPORT_STATIC,
		trace( Kiev.debugResolve, "In file syntax: "+syn),
		((Import)syn).resolveMethodR(node,path,mt)
	}

	public void cleanup() {
        Kiev.parserAddresses.clear();
		Kiev.k.presc = null;
		foreach(Struct n; members) ((JStruct)n).cleanup();
	}
}


