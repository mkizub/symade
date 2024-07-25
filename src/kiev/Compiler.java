/*******************************************************************************
 * Copyright (c) 2005-2007 UAB "MAKSINETA".
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License Version 1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     "Maxim Kizub" mkizub@symade.com - initial design and implementation
 *******************************************************************************/
package kiev;
import syntax kiev.Syntax;

import kiev.stdlib.Arrays;
import kiev.fmt.common.TextParser;
import kiev.dump.DumpFactory;

import java.util.IdentityHashMap;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.management.*;


/**
 * @author Maxim Kizub
 * @version $Revision: 299 $
 *
 */

public class EditorThreadGroup extends WorkerThreadGroup {
	public EditorThreadGroup(WorkerThreadGroup bthr) {
		super(bthr, "editor");
		semantic_context = new SemContext(bthr.semantic_context);
	}
}

public class FrontendThreadGroup extends WorkerThreadGroup {
	public static final FrontendThreadGroup THE_GROUP = new FrontendThreadGroup();
	
	private CompilerParseInfo[]		args;
	
	private FrontendThreadGroup() {
		super(null, "frontend");
		this.theEnv = Env.createEnv();
		semantic_context = new SemContext(null);
	}

	public void cleanEnv() {
		args = null;
		super.cleanEnv();
		this.theEnv = Env.createEnv();
	}

	public void runTask(CompilerParseInfo[] args) {
		this.args = args;
		this.roots = null;
		this.errCount = 0;
		this.warnCount = 0;
		runFrontEndParse();
	}

	private void runFrontEndParse() {
		this.programm_start = this.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		try {
//			if( Kiev.verbose ) System.out.println(Compiler.version);
//			Runtime.getRuntime().traceMethodCalls(Compiler.methodTrace);

			if (this.getEnv().root == null) {
				if (args == null)
					args = new CompilerParseInfo[0];
				executorService.submit(new Runnable() { public void run() {
						Transaction tr = Transaction.open("Bootstrap", WorkerThreadGroup.this);
						try {
							this.getEnv().InitializeEnv(Kiev.compiler_classpath);
							foreach (CompilerParseInfo cpi; args; cpi.add_to_project && cpi.fname != null)
								this.getProject().addProjectFile(cpi.fname);
							addRequaredToMake();
						} catch (Throwable t) {
							t.printStackTrace();
						} finally {
							tr.close(WorkerThreadGroup.this);
						}
				}}).get();
			}

			if( args == null || args.length == 0 )
				return;

			Kiev.resetFrontEndPass();
			
			for(int i=0; i < args.length; i++) {
				CompilerParseInfo cpi = args[i];
				Callable<String> task = new Callable<String>() { public String call() {
					String cpiFile = cpi.fname.replace('/', File.separatorChar);
					String curFile = cpiFile;
					if (!Compiler.prefer_source && Compiler.btd_dir != null && !cpiFile.toLowerCase().endsWith(".btd")) {
						String btdFile = cpiFile;
						int ext = btdFile.lastIndexOf('.');
						if (ext > 0)
							btdFile = Compiler.btd_dir + "/" + btdFile.substring(0,ext)+".btd";
						else
							btdFile = Compiler.btd_dir + "/" + btdFile+".btd";
						long btd_time = Kiev.newFile(btdFile).lastModified();
						long cpi_time = Kiev.newFile(cpiFile).lastModified();
						if (btd_time > cpi_time)
							curFile = btdFile;
					}
					ProjectSyntaxInfo psi = cpi.getProjectSyntaxInfo();
					try {
						long curr_time = 0L, diff_time = 0L;
						diff_time = curr_time = System.currentTimeMillis();
						Kiev.setCurFile(cpiFile);
						if (curFile.toLowerCase().endsWith(".btd")) {
							String fdir = Kiev.newFile(cpiFile).getParent();
							File f = Kiev.newFile(curFile);
							INode[] roots = DumpFactory.getBinDumper().loadFromBinFile(this.getEnv(), fdir, f, cpi.fdata);
							if (roots != null && roots.length > 0 && roots[0] instanceof ASTNode) {
								ASTNode root = (ASTNode)roots[0];
								if !(root instanceof FileUnit) {
									FileUnit fu = FileUnit.makeFile(DumpFactory.getRelativePath(f), this.getEnv().proj, false);
									fu.current_syntax = new ProjectSyntaxFactoryBinDump();
									fu.members += root;
									cpi.fu = fu;
								} else {
									cpi.fu = (FileUnit)root;
								}
								cpi.fu.source_timestamp = f.lastModified();
								//Kiev.runProcessorsOn(cpi.fu);
							}
						}
						else if (psi != null && psi.parser != null) {
							File f = Kiev.newFile(curFile);
							TextParser parser = (TextParser)psi.parser.makeTextProcessor();
							INode[] roots = parser.parse(f,  this.getEnv());
							if (roots != null && roots.length > 0 && roots[0] instanceof ASTNode) {
								ASTNode root = (ASTNode)roots[0];
								if !(root instanceof FileUnit) {
									FileUnit fu = FileUnit.makeFile(DumpFactory.getRelativePath(f), this.getEnv().proj, false);
									fu.ftype = psi.file_type;
									fu.current_syntax = psi.syntax;
									fu.members += root;
									cpi.fu = fu;
								} else {
									cpi.fu = (FileUnit)root;
								}
								cpi.fu.source_timestamp = f.lastModified();
							}
						}
						else if (curFile.toLowerCase().endsWith(".xml")) {
							File f = Kiev.newFile(curFile);
							INode[] roots = DumpFactory.getXMLDumper().loadFromXmlFile(this.getEnv(), f, cpi.fdata);
							if (roots != null && roots.length > 0 && roots[0] instanceof ASTNode) {
								ASTNode root = (ASTNode)roots[0];
								if !(root instanceof FileUnit) {
									FileUnit fu = FileUnit.makeFile(DumpFactory.getRelativePath(f), this.getEnv().proj, false);
									fu.ftype = "text/xml/tree-dump";
									fu.current_syntax = new ProjectSyntaxFactoryXmlDump();
									fu.members += root;
									cpi.fu = fu;
								} else {
									cpi.fu = (FileUnit)root;
								}
								cpi.fu.source_timestamp = f.lastModified();
								//Kiev.runProcessorsOn(cpi.fu);
							}
						} else {
							java.io.InputStreamReader file_reader = null;
							char[] file_chars = new char[8196];
							int file_sz = 0;
							try {
								if (cpi.fdata != null)
									file_reader = new InputStreamReader(new ByteArrayInputStream(cpi.fdata), "UTF-8");
								else
									file_reader = new InputStreamReader(new FileInputStream(curFile), "UTF-8");
								for (;;) {
									int r = file_reader.read(file_chars, file_sz, file_chars.length-file_sz);
									if (r < 0)
										break;
									file_sz += r;
									if (file_sz >= file_chars.length) {
										char[] tmp = new char[file_chars.length + 8196];
										System.arraycopy(file_chars, 0, tmp, 0, file_chars.length);
										file_chars = tmp;
									}
								}
							} finally {
								if (file_reader != null) file_reader.close();
							}
							int BOM = 0;
							if (file_chars.length > 0 && file_chars[0] == '\uFEFF')
								BOM = 1;
							java.io.CharArrayReader bis = new java.io.CharArrayReader(file_chars, BOM, file_sz-BOM);
							diff_time = curr_time = System.currentTimeMillis();
							Parser p = new Parser(bis, this.getEnv());
							cpi.fu = p.FileUnit(cpi.fname);
							cpi.fu.source_timestamp = Kiev.newFile(curFile).lastModified();
							//cpi.fu.current_syntax = "stx-fmt·syntax-for-java";
							bis.close();
						}
						diff_time = System.currentTimeMillis() - curr_time;
						Kiev.setCurFile(null);
						if( Kiev.verbose )
							Kiev.reportInfo("Parsed  file   "+cpi.fname,diff_time);
						System.out.flush();
					} catch (Exception e) {
						Kiev.reportParserError(cpi.fu,0,e);
					}
					return null;
				}};
				this.tasks.add(task);
			}
			Transaction tr = Transaction.open("Parse", this);
			try {
				executeTasks(true);
			} finally {
				tr.close(this);
			}

			this.roots = new INode[]{this.getEnv().root};

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		if (this.errCount == 0) {
			executorService.submit(new Runnable() { public void run() {
					Transaction tr = Transaction.open("Dump projectfile", WorkerThreadGroup.this);
					try {
						getEnv().dumpProjectFile();
					} catch (Throwable t) {
						t.printStackTrace();
					} finally {
						tr.close(WorkerThreadGroup.this);
					}
			}}).get();
		}
		else {
			this.roots = null;
		}
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Frontend");
	}

	private boolean containsFileName(String fname) {
		foreach (CompilerParseInfo cpi; args; cpi.fname.equals(fname))
			return true;
		return false;
	}
	
	private void addRequaredToMake() {
		foreach (FileUnit fu; this.getProject().enumerateAllCompilationUnits(); !containsFileName(fu.pname())) {
			ProjectSyntaxInfo psi = null;
			if (fu.ftype != null) {
				foreach (ProjectSyntaxInfo p; this.getProject().syntax_infos; fu.ftype.equals(p.file_type)) {
					psi = p;
					if (psi.parser != null)
						break;
				}
			}
			if( Kiev.verbose ) System.out.println("File "+fu.pname());
			args = (CompilerParseInfo[])Arrays.appendUniq(args,new CompilerParseInfo(Kiev.newFile(fu.pname()),psi,true));
		}
	}

}

public class CompilerThreadGroup extends WorkerThreadGroup {
	public CompilerThreadGroup(WorkerThreadGroup bthr) {
		super(bthr, "compiler");
		semantic_context = new SemContext(bthr.semantic_context);
		bend_context = new kiev.be.java15.JNodeContext(semantic_context);
	}

	public void runTask() {
		this.errCount = 0;
		this.warnCount = 0;
		runBackEnd();
	}

	private void runBackEnd() {
		long curr_time = 0L, diff_time = 0L;
		Transaction tr_me = Transaction.open("Compiler.java:runBackEnd(1)", this);
		try {
			////////////////////////////////////////////////////
			//	                  Midend                      //
			////////////////////////////////////////////////////

			Kiev.resetMidEndPass();
			do {
				String[] msg = new String[1];
				diff_time = curr_time = System.currentTimeMillis();
				executorService.submit(new Runnable() { public void run() {
						String s = Kiev.runCurrentMidEndProcessor(getEnv());
						if (s != null)
							msg[0] = s;
				}}).get();
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && msg[0] != null) Kiev.reportInfo(msg[0],diff_time);
				if( this.errCount > 0 ) throw new Kiev.CompilationAbortError();
			} while (Kiev.nextMidEndPass(getEnv()));
			
			tr_me.close(this);
			getEnv().cleanupHashOfFails();
			if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
				reportTotals("Midend");

			foreach(FileUnit fu; getProject().enumerateAllCompilationUnits()) {
				if (fu.isInterfaceOnly() || fu.dont_run_backend)
					continue; // don't run back-end on interface (API) files
				final int errCount = this.errCount;
				Callable<String> task = new Callable<String>() { public String call() {
					WorkerThread wth = (WorkerThread)Thread.currentThread();
					WorkerThreadGroup wthg = (WorkerThreadGroup)Thread.currentThread().getThreadGroup();
					wth.be_pass_no = 0;
					Kiev.setCurFile(fu.pname());
					foreach (BackendProcessor bp; getEnv().getBEProcessors()) {
						if (bp.isEnabled()) {
							long diff_time, curr_time;
							diff_time = curr_time = System.currentTimeMillis();
							try {
								bp.process(fu,Transaction.get());
							} catch (Exception e) {
								Kiev.reportError(e);
							}
							diff_time = System.currentTimeMillis() - curr_time;
							if( Kiev.verbose && bp.getDescr() != null) Kiev.reportInfo(bp.getDescr()+" "+fu.fname,diff_time);
						}
						wth.be_pass_no += 1;
						if (wthg.errCount > errCount)
							break;
					};
					Kiev.setCurFile("");
					return null;
				}};
				this.tasks.add(task);
			}
			Transaction tr = Transaction.open("Compiler.java:runBackEnd(2)", this);
			try {
				executeTasks(true);
			} finally {
				tr.close(this);
				tr.rollback(false);
			}
			//Env.classHashOfFails.clear();
		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				return;
			Kiev.reportError(e);
		} finally {
			executorService.submit(new Runnable() { public void run() {
					tr_me.rollback(false);
			}});
		}
		executorService.submit(new Runnable() { public void run() {
				try {
					getEnv().dumpProjectFile();
				} catch (Throwable t) {
					t.printStackTrace();
				}
		}}).get();
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Backend");
		//this.getEnv().cleanupBackendEnv();
		//java.lang.Runtime.getRuntime().gc();
	}

}

public final class WorkerThread extends Thread {
	
	// The environment of this thread (and group)
	public Env          theEnv;
	// The current semantic context
	public SemContext   semantic_context;
	// The current java backend context
	public Context      bend_context;

	final int			worker_id;
	int					be_pass_no;

	public WorkerThread(WorkerThreadGroup wthg, Runnable r, String name, int id) {
		super(wthg, r, name+"-thread-"+id);
		this.worker_id = id;
	}
	
	public void run() {
		WorkerThreadGroup wthg = (WorkerThreadGroup)getThreadGroup();
		if (this.theEnv != wthg.theEnv)
			this.theEnv = wthg.theEnv;
		if (this.semantic_context != wthg.semantic_context)
			this.semantic_context = wthg.semantic_context;
		if (this.bend_context != wthg.bend_context)
			this.bend_context = wthg.bend_context;
		Transaction tr = wthg.transaction;
		try {
			super.run();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		assert (tr == wthg.transaction);
	}
}

public abstract class WorkerThreadGroup extends ThreadGroup implements ThreadFactory {
	
	static class ProcessorRunInfo {
		final CompilationUnit cunit;
		long processor_run_bitmap;
		ProcessorRunInfo(CompilationUnit cunit) {
			this.cunit = cunit;
		}
	}
	
	private static int threadCounter;

	public ExecutorService executorService;
	
	// The environment of this thread group
	public Env				theEnv;
	// The current semantic context
	public SemContext		semantic_context;
	// The current java backend context
	public Context			bend_context;
	// The current transaction of this thread group
	public Transaction		transaction;
	
	// Error section
	public long		programm_start;
	public long		programm_end;
	public int		errCount;
	public int		warnCount;
	public boolean	reportTotals;

	private IdentityHashMap<CompilationUnit,ProcessorRunInfo> processorRunInfoMap = new IdentityHashMap<CompilationUnit,ProcessorRunInfo>(1023);

	public INode[]						roots;
	public ArrayList<Callable<String>>	tasks;
	
	public Env getEnv() {
		return theEnv;
	}

	public Project getProject() {
		return theEnv.proj;
	}
	
	public void cleanEnv() {
		theEnv = null;
		roots = null;
		processorRunInfoMap.clear();
	}

	public WorkerThreadGroup(WorkerThreadGroup parent, String name) {
		super(parent == null ? Thread.currentThread().getThreadGroup() : parent, name+"-group");
		if (parent != null)
			this.theEnv = parent.theEnv;
		try {
			setDaemon(true);
			setMaxPriority((Thread.NORM_PRIORITY+Thread.MIN_PRIORITY)/2);
		} catch (Exception e) { e.printStackTrace(); }
		if (Compiler.jobThreads <= 1)
			executorService = Executors.newSingleThreadExecutor(this);
		else
			executorService = Executors.newFixedThreadPool(Compiler.jobThreads, this);
		this.tasks = new ArrayList<Callable<String>>();
	}
	
	public Thread newThread(Runnable r) {
		String name = getName();
		int cnt = ++threadCounter;
		return new WorkerThread(this, r, name.substring(0,name.length()-"-group".length()), cnt);
	}
	
	// set information about transfer processor run on the compilation unit,
	// return true if it already ran
	public boolean setProcessorRun(CompilationUnit cunit, AbstractProcessor proc) {
		ProcessorRunInfo ri = processorRunInfoMap.get(cunit);
		if (ri == null) {
			ri = new ProcessorRunInfo(cunit);
			processorRunInfoMap.put(cunit,ri);
		}
		if ((ri.processor_run_bitmap & (1L << proc.id)) == 0L) {
			ri.processor_run_bitmap |= (1L << proc.id);
			return false;
		}
		return true;
	}
	
	public void runTask(INode[] roots) {
		this.roots = roots;
		this.errCount = 0;
		this.warnCount = 0;
		runFrontEnd();
		this.roots = null;
	}

	public void runTask(Runnable task) {
		if (Thread.currentThread().getThreadGroup() instanceof WorkerThreadGroup)
			task.run();
		else
			executorService.submit(task).get();
	}

	public <V> V runTask(Callable<V> task) {
		if (Thread.currentThread().getThreadGroup() instanceof WorkerThreadGroup)
			return task.call();
		else
			return executorService.submit(task).get();
	}

	public void runTaskLater(Runnable task) {
		executorService.submit(task);
	}
	
	void executeTasks(boolean pooled) {
		if (tasks.size() == 0)
			return;
		if (pooled && Compiler.jobThreads > 1) {
			Collection<Callable<String>⁺> x = tasks;
			executorService.invokeAll<String>(x);
		} else {
			Callable<String>[] arr = tasks.toArray(new Callable<String>[tasks.size()]);
			foreach (Callable<String> task; arr)
				executorService.submit(task).get();
		}
		tasks.clear();
	}

	class RootsEnumerator implements Enumeration<INode> {
		private int root_idx;
		private int cu_idx;
		RootsEnumerator(int cu_idx) {
			this.cu_idx = cu_idx;
		}
		public boolean hasMoreElements() {
			if (root_idx < roots.length) return true;
			if (cu_idx < getProject().compilationUnits.length) return true;
			return false;
		}
		public INode nextElement() {
			if (root_idx < roots.length)
				return roots[root_idx++];
			if (cu_idx < getProject().compilationUnits.length)
				return getProject().compilationUnits[cu_idx++];
			throw new NoSuchElementException();
		}
	}
	
	private void runFrontEnd() {
		if (roots == null || roots.length == 0)
			return;

		int cuLength = getProject().compilationUnits.length;
		
		this.programm_start = this.programm_end = System.currentTimeMillis();
		long curr_time = 0L, diff_time = 0L;
		try {
//			if( Kiev.verbose ) System.out.println(Compiler.version);
//			Runtime.getRuntime().traceMethodCalls(Compiler.methodTrace);


			Kiev.resetFrontEndPass();
			
			////////////////////////////////////////////////////
			//	                  Cleanup errors    //
			////////////////////////////////////////////////////
			if (getEnv().proj != null && getEnv().proj.errors.length > 0) {
				foreach (INode root; new RootsEnumerator(cuLength)) {
					root.walkTree(null, null, new ITreeWalker() {
						private final Env env = WorkerThreadGroup.this.getEnv();
						private ErrorInfo[] errors = this.env.proj.errors;
						public boolean pre_exec(INode n, INode parent, AttrSlot slot) {
							foreach (ErrorNodeInfo err; this.errors; err.node == n) {
								this.env.proj.errors.detach(err);
								this.errors = this.env.proj.errors;
							}
							return true;
						}
					});
				}
			}

			////////////////////////////////////////////////////
			//	                  Frontend              //
			////////////////////////////////////////////////////

			do {
				String[] msg = new String[1];
				diff_time = curr_time = System.currentTimeMillis();
				foreach (INode root; new RootsEnumerator(cuLength)) {	
					executorService.submit(new Runnable() { public void run() {
						String s = Kiev.runCurrentFrontEndProcessor(getEnv(),root);
						if (s != null)
							msg[0] = s;
					}}).get();
				}
				diff_time = System.currentTimeMillis() - curr_time;
				if( Kiev.verbose && msg[0] != null) Kiev.reportInfo(msg[0],diff_time);
				if( this.errCount > 0 ) goto stop;
			} while (Kiev.nextFrontEndPass(getEnv()));

			diff_time = curr_time = System.currentTimeMillis();
			foreach (INode root; new RootsEnumerator(cuLength)) {	
				executorService.submit(new Runnable() { public void run() {
					Kiev.runVerifyProcessors(getEnv(),root);
				}}).get();
			}
			diff_time = System.currentTimeMillis() - curr_time;
			if( Kiev.verbose) Kiev.reportInfo("Tree verification",diff_time);
			if( this.errCount > 0 ) goto stop;

		} catch( Throwable e) {
			if (e instanceof Kiev.CompilationAbortError)
				goto stop;
			Kiev.reportError(e);
			goto stop;
		}

stop:;
		executorService.submit(new Runnable() { public void run() {
				try {
					foreach (INode root; WorkerThreadGroup.this.new RootsEnumerator(cuLength))	
						Kiev.lockNodeTree(root);
					getEnv().cleanupHashOfFails();
					getEnv().dumpProjectFile();
				} catch (Throwable t) {
					t.printStackTrace();
				}
		}}).get();
		if (this.errCount > 0) {
			this.roots = null;
		}
		if( Kiev.verbose || this.reportTotals || this.errCount > 0  || this.warnCount > 0)
			reportTotals("Frontend");
	}

	public void reportTotals(String src) {
		if( errCount > 0 )
			System.out.println(errCount+" errors");
		if( warnCount > 0 )
			System.out.println(warnCount+" warnings");
		programm_end = System.currentTimeMillis();
		System.out.println(src+": total "+(programm_end-programm_start)+"ms, memory used "+getPeekMemoryInfo()+"Mb");
		if (Kiev.verbose && Compiler.report_totals) {
			dumpJITInfo();
			dumpMemoryInfo();
		}
		if (Kiev.testError != null) {
			System.out.println("FAILED: there was no expected error "+Kiev.testError+" at "+Kiev.testErrorLine+":"+Kiev.testErrorOffs);
			Kiev.systemExit(1);
		}
		this.reportTotals = true;
	}

	private static int getPeekMemoryInfo() {
		try {
			// Read MemoryMXBean
			MemoryMXBean memorymbean = ManagementFactory.getMemoryMXBean();
			List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();
			long total = 0L;
			foreach (MemoryPoolMXBean mempoolmbean; mempoolsmbeans.toArray(new MemoryPoolMXBean[0]); mempoolmbean.getType() == MemoryType.HEAP ) {
				MemoryUsage mu = mempoolmbean.getPeakUsage();
				if (mu != null)
					total += (int)mu.getUsed();
			}
			return (int)(total / 1048576);
		}
		catch( Exception e ) {
			return 0;
		}
	}
	
	private static void dumpJITInfo() {
		try {
			System.out.println( "\nDUMPING JIT INFO\n" );
			CompilationMXBean cmpbean = ManagementFactory.getCompilationMXBean();
			System.out.println( "Compiler Name: " + cmpbean.getName() );
			System.out.println( "Compilation Time: " + cmpbean.getTotalCompilationTime()+"ms" );
		}
		catch( Exception e ) {}
	}
	private static String printMemoryUsage(MemoryUsage mu) {
		return "used="+(mu.getUsed()/1024)+"Kb, comm="+(mu.getCommitted()/1024)+"Kb";
	}
	private static void dumpMemoryInfo() {
		try {
			System.out.println( "\nDUMPING MEMORY INFO\n" );
			// Read MemoryMXBean
			MemoryMXBean memorymbean = ManagementFactory.getMemoryMXBean();
			System.out.println( "Heap Memory Usage: " + printMemoryUsage(memorymbean.getHeapMemoryUsage()) );
			System.out.println( "Non-Heap Memory Usage: " + printMemoryUsage(memorymbean.getNonHeapMemoryUsage()) );
			
			// Read Garbage Collection information
			List<GarbageCollectorMXBean> gcmbeans = ManagementFactory.getGarbageCollectorMXBeans();
			foreach (GarbageCollectorMXBean gcmbean; gcmbeans.toArray(new GarbageCollectorMXBean[0])) {
				System.out.println( "\nGC Name: " + gcmbean.getName() );
				System.out.println( "\tGC count: " + gcmbean.getCollectionCount() );
				System.out.println( "\tGC time: " + gcmbean.getCollectionTime()+"ms" );
			}
			
			// Read Memory Pool Information
			System.out.println( "\nMemory Pools Info" );
			List<MemoryPoolMXBean> mempoolsmbeans = ManagementFactory.getMemoryPoolMXBeans();
			foreach (MemoryPoolMXBean mempoolmbean; mempoolsmbeans.toArray(new MemoryPoolMXBean[0])) {
				System.out.println( "\nPool Name: " + mempoolmbean.getName() );
				System.out.println( "\tUsage: " + printMemoryUsage(mempoolmbean.getUsage()) );
				System.out.println( "\tPeak:  " + printMemoryUsage(mempoolmbean.getPeakUsage()) );
				System.out.println( "\tType:  " + mempoolmbean.getType() );
			}
		}
		catch( Exception e ) {}
	}
}

public final class CompilerParseInfo {
	// either of file or fname (with optional fdata) must be specified
	final String	fname;
	final byte[]	fdata;
	// add or not add to the project file
	final boolean	add_to_project;
	// ProjectSyntaxInfo to extract parser and assign ftype
	final ProjectSyntaxInfo fsyntax;
	// resulting FileUnit
	public FileUnit	fu;
	
	public CompilerParseInfo(File file, ProjectSyntaxInfo fsyntax, boolean add_to_project) {
		this.fname = file.getPath().replace(File.separatorChar, '/').intern();
		this.fdata = null;
		this.fsyntax = fsyntax;
		this.add_to_project = add_to_project;
	}
	public CompilerParseInfo(String fname, byte[] fdata, boolean add_to_project) {
		if (fname != null)
			this.fname = fname.replace(File.separatorChar, '/').intern();
		this.fdata = fdata;
		this.add_to_project = add_to_project;
	}
	
	public ProjectSyntaxInfo getProjectSyntaxInfo() {
		if (fsyntax != null)
			return fsyntax;
		if (fname != null) {
			if (fname.toLowerCase().endsWith(".xml")) {
				ProjectSyntaxInfo psi = new ProjectSyntaxInfo();
				psi.file_type = "text/xml/tree-dump";
				psi.parser = new ProjectSyntaxFactoryXmlDump();
				return psi;
			}
			if (fname.toLowerCase().endsWith(".btd")) {
				ProjectSyntaxInfo psi = new ProjectSyntaxInfo();
				psi.file_type = "binary/tree-dump";
				psi.parser = new ProjectSyntaxFactoryBinDump();
				return psi;
			}
			if (fname.toLowerCase().endsWith(".java") || fname.toLowerCase().endsWith(".kj")) {
				ProjectSyntaxInfo psi = new ProjectSyntaxInfo();
				psi.file_type = "text/java/1.6";
				return psi;
			}
		}
		return null;
	}
}

public class Compiler {
	public static ServerSocket		server;
	public static Socket			socket;
	public static InputStream		system_in = System.in;
	public static PrintStream		system_out = System.out;
	public static PrintStream		system_err = System.err;

	public static final String version = "SymADE (v 0.6), (C) UAB MAKSINETA, 1997-2008, http://symade.com";

	public static boolean debug					= false;
	public static boolean debugStatGen			= false;
	public static boolean debugInstrGen			= false;
	public static boolean debugBytecodeRead		= false;
	public static boolean debugResolve			= false;
	public static boolean debugOperators		= false;
	public static boolean debugMembers			= false;
	public static boolean debugCreation			= false;
	public static boolean debugRules			= false;
	public static boolean debugMultiMethod		= false;
	public static boolean debugNodeTypes		= false;

	public static boolean methodTrace			= false;

	public static boolean verbose				= false;
	public static boolean verify				= true;
	public static boolean safe					= true;
	public static boolean fast_gen				= false;
	public static boolean debugOutputA			= false;
	public static boolean debugOutputT			= false;
	public static boolean debugOutputC			= false;
	public static boolean debugOutputL			= false;
	public static boolean debugOutputV			= false;
	public static boolean debugOutputR			= false;

	public static boolean errorPrompt			= false;

	public static boolean run_gui				= false;
	public static boolean run_gui_swing			= false;
	public static boolean run_gui_swt			= false;
	public static boolean interface_only		= false;
	public static boolean prefer_source			= false;

	public static int    target					= 0;
	public static String root_dir				= null; // System.getProperty("user.dir")
	public static String project_file			= null;
	public static String output_dir				= "classes";
	public static String btd_dir				= ".btd";
	public static String dump_src_dir			= null; //".src";
	public static String compiler_classpath		= null;
	public static boolean javacerrors			= false;
	public static boolean report_totals			= false;
	public static boolean nowarn				= false;
	public static boolean code_nowarn			= true;
	public static boolean run_from_ide			= false;
	public static boolean system_exit			= true; // System.exit() or throw exception if disabled

	public static Map<String, Set<String>> sourceToClassMapping = null;
	public static int errorCount                 = 0;

	public static KievBackend useBackend = KievBackend.Java15;

	public static CError testError				= null;
	public static int    testErrorLine			= 0;
	public static int    testErrorOffs			= 0;

	public static int    jobThreads				= 0;

	public static long gc_mem					= (long)(1024*1024);

	private static boolean[] command_line_disabled_extensions	= new boolean[KievExt.values().length];

	static {
		Compiler.setExtension(false, "view");
		Compiler.setExtension(false, "vnode");
		Compiler.setExtension(false, "dflow");
		Compiler.setExtension(false, "xpath");
	}
	
	public static boolean[] getCmdLineExtSet() {
		return (boolean[])command_line_disabled_extensions.clone();
	}
	
	public static void setExtension(boolean enabled, String s) {
		KievExt ext;
		try {
			ext = KievExt.fromString(s);
		} catch(RuntimeException e) {
			Kiev.reportWarning("Unknown pragma '"+s+"'");
			return;
		}
		int i = (int)ext;
		if (i == 0) {
			for (int i=1; i < command_line_disabled_extensions.length; i++)
				command_line_disabled_extensions[i] = !enabled;
		} else {
			command_line_disabled_extensions[i] = !enabled;
		}
	}
	
	public static void runServer(InetAddress addr, int port) {
		server = new ServerSocket(port,2,addr);
		System.out.println("Server mode: "+server);
		for(;;) {
			socket = server.accept();
			System.out.println("Incoming connection "+socket);
			System.setIn(new DataInputStream(socket.getInputStream()));
			System.setOut(new PrintStream(socket.getOutputStream()));
			System.setErr(new PrintStream(socket.getOutputStream()));
			String[] args = getCommandLine();
			run(args);
			socket.close();
			System.setIn(system_in);
			System.setOut(system_out);
			System.setErr(system_err);
			System.out.println("Done.");
		}
	}

	public static void runIncremental(String[] args) {
		System.out.println("Incremental mode");
		System.setIn(new DataInputStream(System.in));
		for(;;) {
			args = getCommandLine();
			printargs(args);
			run(args);
		}
	}

	public static void printargs(String[] args) {
		StringBuffer sb = new StringBuffer();
		foreach(String arg; args; arg != null) {
			sb.append(arg).append(' ');
		}
		System.out.println(sb.toString());
	}

	public static String[] getCommandLine() {
	start:;
		String[] args;
		System.out.print("kiev> ");
		System.out.flush();
		System.gc();
		String cmd = ((DataInputStream)System.in).readLine();
		System.out.println("Command "+cmd);
		if( cmd == null ) {
			args = new String[]{"-q"};
			return args;
		} else if( cmd.length() == 0 ) {
			args = new String[0];
			return args;
		}
		StringTokenizer st = new StringTokenizer(cmd);
		args = new String[st.countTokens()];
		int i=0;
		foreach(Object s; st) args[i++] = (String)s;
		if( args[0].startsWith("-q") ) {
			System.out.println("Bye.");
			if( server != null ) server.close();
			if( socket != null ) socket.close();
			Kiev.systemExit(0);
		}
		else if( args[0].equals("?") ) {
			System.out.println(
				"[options] file.java ...\t - set options and compile file(s)\n"+
				"-q\t\t - quit\n"+
				"?\t\t - this help\n"+
				"{Enter}\t\t - make project\n"
			);
			goto start;
		}
		return args;
	}

	private static String makePath(String path) throws java.io.IOException {
		File file = new File(path);
		if (root_dir != null && !file.isAbsolute())
			file = new File(root_dir, path);
		return file.getCanonicalPath();
	}

	private static String[] parseArgs(String[] args) throws Exception {
		int a = 0;
		if( args==null ) args = new String[0];
		int alen = args.length;
		for(a=0; a < alen ;a++) {
			if (args[a].equals("-debug"))
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
			if (args[a].equals("-no-system-exit")) {
				System.out.println(Compiler.version);
				System.out.print("compiler args: ");
				foreach (String a; args ) {
					System.out.print(" "+a);
				}
				System.out.println();
			}
		}
		try {
			for(a=0; a < alen ;a++) {
				boolean onoff = true;
				if( args[a] == null ) continue;
				if( args[a].equals("--") ) {
					args[a] == null;
					break;
				}
				if( args[a].equals("-pipe") || args[a].equals("-server") ) {
					args[a] = null;
					if( args[a+1].charAt(0) == '-' ) continue;
					if( args[a+1].endsWith(".java") || args[a+1].endsWith(".kj") ) continue;
					args[++a] = null;
					continue;
				}
				else if( args[a].equals("-?") || args[a].equals("-h") || args[a].equals("-help") ) {
					printHelp();
					return new String[0];
				}

				if( args[a].startsWith("-no-") ) {
					args[a] = args[a].substring(3);
					onoff = false;
				}

				if( args[a].equals("-disable") ) {
					args[a] = null;
					Compiler.setExtension(!onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-enable") ) {
					args[a] = null;
					Compiler.setExtension(onoff,args[++a]);
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-prompt")) {
					Compiler.errorPrompt = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-g") ) {
					Compiler.debugOutputA = onoff;
					Compiler.debugOutputT = onoff;
					Compiler.debugOutputC = onoff;
					Compiler.debugOutputL = onoff;
					Compiler.debugOutputV = onoff;
					Compiler.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].startsWith("-g:")) {
					if( args[a].indexOf('a') > 0 )
						Compiler.debugOutputA = onoff;
					if( args[a].indexOf('t') > 0 )
						Compiler.debugOutputT = onoff;
					if( args[a].indexOf('c') > 0 )
						Compiler.debugOutputC = onoff;
					if( args[a].indexOf('l') > 0 )
						Compiler.debugOutputL = onoff;
					if( args[a].indexOf('v') > 0 )
						Compiler.debugOutputV = onoff;
					if( args[a].indexOf('r') > 0 )
						Compiler.debugOutputR = onoff;
					args[a] = null;
				}
				else if( args[a].equals("-debug")) {
					args[a] = null;
					String dbg = args[++a];
					args[a] = null;
					System.out.println("Debugging: "+onoff);
					if( dbg.indexOf("stat",0) >= 0 ) {
						System.out.println("\tstatement generation");
						Compiler.debugStatGen	= onoff;
					}
					if( dbg.indexOf("asm",0) >= 0 ) {
						System.out.println("\tassembler generation");
						Compiler.debugInstrGen	= onoff;
					}
					if( dbg.indexOf("instr",0) >= 0 ) {
						System.out.println("\tinstractions generation");
						Compiler.debugInstrGen	= onoff;
					}
					if( dbg.indexOf("bcread",0) >= 0 ) {
						System.out.println("\tbytecode .class reading");
						Compiler.debugBytecodeRead = onoff;
						kiev.bytecode.Clazz.traceRead = onoff;
					}
					if( dbg.indexOf("bcwrite",0) >= 0 ) {
						System.out.println("\tbytecode .class writing");
						kiev.bytecode.Clazz.traceWrite = onoff;
					}
					if( dbg.indexOf("bcpatch",0) >= 0 ) {
						System.out.println("\tbytecode .class patching and rules");
						kiev.bytecode.Clazz.traceRules = onoff;
					}
					if( dbg.indexOf("resolv",0) >= 0 ) {
						System.out.println("\tidentifier resolving");
						Compiler.debugResolve	= onoff;
					}
					if( dbg.indexOf("operat",0) >= 0 ) {
						System.out.println("\toperator resolving");
						Compiler.debugOperators	= onoff;
					}
					if( dbg.indexOf("member",0) >= 0 ) {
						System.out.println("\tmembers attaching (AST generation)");
						Compiler.debugMembers	= onoff;
					}
					if( dbg.indexOf("create",0) >= 0 ) {
						System.out.println("\tmembers creation");
						Compiler.debugCreation	= onoff;
					}
					if( dbg.indexOf("methodtrace",0) >= 0 ) {
						System.out.println("\tMethod tracing");
						Compiler.methodTrace	= onoff;
					}
					if( dbg.indexOf("multimethod",0) >= 0 ) {
						System.out.println("\tMultiMethod generation");
						Compiler.debugMultiMethod	= onoff;
					}
					if( dbg.indexOf("rule",0) >= 0 ) {
						System.out.println("\trules");
						Compiler.debugRules		= onoff;
					}
					if( dbg.indexOf("types",0) >= 0 ) {
						System.out.println("\tvar/field types");
						Compiler.debugNodeTypes		= onoff;
					}
					Compiler.debug = onoff;
					if (onoff)
						Compiler.verbose = true;
					continue;
				}
				else if( args[a].equals("-verbose") || args[a].equals("-v") ) {
					Compiler.verbose = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-verify") ) {
					Compiler.verify = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-safe") ) {
					Compiler.safe = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-target") ) {
					args[a] = null;
					if (onoff && a+1 < args.length) {
						Compiler.target = Integer.parseInt(args[a+1]);
						a++;
						args[a] = null;
					} else {
						Compiler.target = 0;
					}
				}
				else if( args[a].equals("-f") || args[a].equals("-fastgen") ) {
					Compiler.fast_gen = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-root")) {
					args[a] = null;
					if( onoff ) {
						File f = new File(args[++a]);
						Compiler.root_dir = f.getCanonicalPath();
						args[a] = null;
					} else {
						Compiler.root_dir = null;
					}
					continue;
				}
				else if( args[a].equals("-d")) {
					args[a] = null;
					if( onoff ) {
						Compiler.output_dir = makePath(args[++a]);
						args[a] = null;
					} else {
						Compiler.output_dir = null;
					}
					continue;
				}
				else if( args[a].equals("-ds")) {
					args[a] = null;
					if( onoff ) {
						Compiler.dump_src_dir = makePath(args[++a]);
						args[a] = null;
					} else {
						Compiler.dump_src_dir = null;
					}
					continue;
				}
				else if( args[a].equals("-btd")) {
					args[a] = null;
					if( onoff ) {
						Compiler.btd_dir = makePath(args[++a]);
						args[a] = null;
					} else {
						Compiler.btd_dir = null;
						Compiler.prefer_source = true;
					}
					continue;
				}
				else if( args[a].equals("-p") || args[a].equals("-project") ) {
					args[a] = null;
					if( onoff ) {
						Compiler.project_file = makePath(args[++a]);
						args[a] = null;
					} else
						Compiler.project_file = null;
					continue;
				}
				else if( args[a].equals("-makedep") ) {
					args[a] = null;
					Compiler.interface_only = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-classpath")) {
					args[a] = null;
					if( onoff ) {
						Compiler.compiler_classpath = args[++a];
						args[a] = null;
					} else
						Compiler.compiler_classpath = null;
					continue;
				}
				else if( args[a].startsWith("-prefer:")) {
					String prefer = args[a].substring(8);
					args[a] = null;
					if (prefer.equals("source"))
						Compiler.prefer_source = onoff;
					else if (prefer.equals("newer"))
						Compiler.prefer_source = !onoff;
					else
						Kiev.reportError("Must be either -prefer:source or -prefer:newer");
					continue;
				}
				else if( args[a].startsWith("-ps")) {
					args[a] = null;
					Compiler.prefer_source = onoff;
					continue;
				}
				else if( args[a].equals("-gc")) {
					args[a] = null;
					if( onoff ) {
						Compiler.gc_mem = (long)(Double.valueOf(args[++a]).doubleValue()*1024.D);
						args[a] = null;
					} else
						Compiler.gc_mem = (long)(1024*1024.D);
					continue;
				}
				else if( args[a].equals("-gui")) {
					args[a] = null;
					Compiler.run_gui   = onoff;
					if (onoff)
						System.setProperty("symade.unversioned","false");
					if (a+1 < args.length) {
						if (args[a+1].equalsIgnoreCase("swing")) {
							Compiler.run_gui_swing = onoff;
							a++;
							args[a] = null;
						}
						else if (args[a+1].equalsIgnoreCase("swt")) {
							Compiler.run_gui_swt = onoff;
							a++;
							args[a] = null;
						}
					}
					if (onoff && !Compiler.run_gui_swt && !Compiler.run_gui_swing)
						Compiler.run_gui_swing = true;
					continue;
				}
				else if( args[a].startsWith("-gui:")) {
					String toolkit = args[a].substring(5);
					args[a] = null;
					Compiler.run_gui   = onoff;
					if (onoff)
						System.setProperty("symade.unversioned","false");
					if (toolkit.equalsIgnoreCase("swing"))
						Compiler.run_gui_swing = onoff;
					else if (toolkit.equalsIgnoreCase("swt"))
						Compiler.run_gui_swt = onoff;
					else
						Kiev.reportError("GUI toolkit must be swt or swing, but found "+toolkit);
					if (onoff && !Compiler.run_gui_swt && !Compiler.run_gui_swing)
						Compiler.run_gui_swing = true;
					continue;
				}
				else if( args[a].equals("-batch") ) {
					args[a] = null;
					System.setProperty("symade.unversioned",String.valueOf(onoff));
					if (onoff)
						Compiler.run_gui = false;
					continue;
				}
				else if( args[a].equals("-javacerrors")) {
					Compiler.javacerrors = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-totals")) {
					Compiler.report_totals = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-ide")) {
					Compiler.run_from_ide = onoff;
					Compiler.system_exit = !onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-system-exit")) {
					Compiler.system_exit = onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-j") || args[a].equals("-jobs") ) {
					args[a] = null;
					if (onoff && a+1 < args.length) {
						Compiler.jobThreads = Integer.parseInt(args[a+1]);
						a++;
						args[a] = null;
					} else {
						Compiler.jobThreads = 0;
					}
					continue;
				}
				else if( args[a].equals("-warn")) {
					Compiler.nowarn = !onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-codewarn")) {
					Compiler.code_nowarn = !onoff;
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-test")) {
					args[a++] = null;
					String[] errs = args[a].split(":");
					args[a] = null;
					Compiler.testError = CError.valueOf(errs[0]);
					Compiler.testErrorLine = Integer.parseInt(errs[1]);
					Compiler.testErrorOffs = Integer.parseInt(errs[2]);
					continue;
				}
				else if( args[a].startsWith("-D") ) {
					Properties ps = System.getProperties();
					String prop, value;
					args[a] = args[a].substring(2);
					int indx = args[a].indexOf("=");
					if( indx < 0 ) {
						prop = args[a];
						value = "";
					} else {
						prop = args[a].substring(0,indx);
						value = args[a].substring(indx+1);
					}
					if( onoff ) {
						System.getProperties().setProperty(prop,value);
					} else {
						System.getProperties().remove(prop);
					}
					args[a] = null;
					continue;
				}
				else if( args[a].equals("-prop")) {
					args[a] = null;
					String fname = args[++a];
					args[a] = null;
					try {
						File file = new File(fname);
						if (!file.isAbsolute() && root_dir != null)
							file = new File(root_dir, fname);
						InputStream inp = new FileInputStream(file);
						//System.getProperties().load(new InputStreamReader(inp, "UTF-8"));
						System.getProperties().load(inp);
					} catch( IOException e ) {
						Kiev.reportError("Error opening property file: "+e);
					}
					continue;
				}
				else if( args[a].startsWith("@") ) {
					String fname = args[a].substring(1);
					args[a] = null;
					RandomAccessFile f = new RandomAccessFile(fname, "r");
					byte[] buf = new byte[(int)f.length()];
					f.readFully(buf);
					f.close();
					StringTokenizer st = new StringTokenizer(new String(buf));
					String[] arr = new String[st.countTokens()];
					for (int i=0; i < arr.length; i++)
						arr[i] = st.nextToken();
					arr = parseArgs(arr);
					for (int i=0; i < arr.length; i++)
						args = (String[])Arrays.insert(args,arr[i],a+i+1);
					alen = args.length;
					continue;
				}
				else if( hasWildcards(args[a]) ) {
					try {
						args = addExpansion(args,a);
					} catch( IOException e ) {
						Kiev.reportError("Error in arguments: "+e);
					}
				}
			}
			
			if (Compiler.target == 0) {
				String jv = System.getProperty("java.specification.version");
				if ("1.6".equals(jv))
					Compiler.target = 6;
				else if ("1.8".equals(jv))
					Compiler.target = 8;
				else {
					Compiler.target = 8;
					Kiev.reportWarning("Java version '"+jv+"' not known, default: -target 8 (JVM 1.8)");
				}
			}
			
			switch (Compiler.target) {
			case 6: case 8:
				break;
			default:
				Kiev.reportError("JVM target version '"+Compiler.target+"' not supported");
			}

			String[] args1 = new String[0];
			for(int i=0; i < args.length; i++) {
				if( args[i] == null ) continue;
				String fn = args[i].replace('/', File.separatorChar).replace('\\', File.separatorChar);
				if (fn.startsWith("./"))
					fn = fn.substring(2);
				args1 = (String[])Arrays.appendUniq(args1,fn);
			}
			args = args1;
		} catch( ArrayIndexOutOfBoundsException e) {
			args = new String[0];
		}
		return args;
	}

	// returns exit code
	public static int run(String[] args) {
		Compiler.interface_only = false;

		try {
			args = parseArgs(args);
		} catch (Exception e) {
			Kiev.reportError(e);
			return 1;
		}

		errorCount = 0;
		FrontendThreadGroup fe_thrg = FrontendThreadGroup.THE_GROUP;
		if (fe_thrg.theEnv != null && fe_thrg.theEnv.proj != null) {
			foreach(FileUnit fu; fe_thrg.theEnv.proj.enumerateAllCompilationUnits())
			fu.generated_files = null;
		}

		Vector cargs = new Vector();
		foreach (String arg; args)
			cargs.add(new CompilerParseInfo(arg, null, true));
		CompilerParseInfo[] argsArr = (CompilerParseInfo[])cargs.toArray(new CompilerParseInfo[cargs.size()]);
		runFrontEnd(argsArr);
		errorCount = fe_thrg.errCount;
		if (fe_thrg.errCount > 0) {
			if (Kiev.run_from_ide)
				return 1;
			Kiev.systemExit(1);
		}

		CompilerThreadGroup cmp_thrg = new CompilerThreadGroup(fe_thrg);
		runFrontEnd(cmp_thrg, fe_thrg.roots);
		errorCount = cmp_thrg.errCount;

		if (Kiev.run_gui) {
			java.lang.reflect.Constructor ctor = Class.forName("kiev.gui.Main").getConstructor(WorkerThreadGroup.class);
			ctor.newInstance(cmp_thrg);
			for(;;) Thread.sleep(10*1000);
		} else {
			if (cmp_thrg.errCount == 0 && !Kiev.interface_only)
				runBackEnd(cmp_thrg, Compiler.useBackend);
			errorCount = cmp_thrg.errCount;
			//if (Kiev.verbose) dumpGeneratedFiles(fe_thrg.theEnv, argsArr);
			if (Kiev.run_from_ide) {
				generateSourceToClassMapping();
				fe_thrg.cleanEnv();
				java.lang.Runtime.getRuntime().gc();
				//for(;;) Thread.sleep(10*1000);
				return errorCount > 0 ? 1 : 0;
			}
			Kiev.systemExit(cmp_thrg.errCount > 0 ? 1 : 0);
		}
		return errorCount > 0 ? 1 : 0;
	}
	
	public static void runFrontEnd(CompilerParseInfo[] args) {
		FrontendThreadGroup.THE_GROUP.runTask(args);
	}
	
	public static void runFrontEnd(WorkerThreadGroup thrg, INode[] roots) {
		thrg.runTask(roots);
	}
	
	public static void runBackEnd(CompilerThreadGroup thrg, KievBackend be) {
		if (be != null)
			Kiev.useBackend = be;
		else
			be = Kiev.useBackend;
		if( Kiev.verbose ) Kiev.reportInfo("Running back-end "+be,0);

		thrg.runTask();
	}

	private static void generateSourceToClassMapping() {
		sourceToClassMapping = new HashMap<String, Set<String>>();
		FrontendThreadGroup fe_thrg = FrontendThreadGroup.THE_GROUP;
		if (fe_thrg.theEnv == null || fe_thrg.theEnv.proj == null)
			return;
		foreach (FileUnit fu; fe_thrg.theEnv.proj.enumerateAllCompilationUnits()) {
			Set<String> set = new HashSet<String>();
			sourceToClassMapping.put(fu.pname(), set);
			if (fu.generated_files == null || fu.generated_files.length == 0) {
				continue;
			} else {
				set.addAll(java.util.Arrays.asList(fu.generated_files));
			}
		}
	}

	private static void dumpGeneratedFiles(Env env, CompilerParseInfo[] args) {
		System.out.println("Generated files:");
		foreach (FileUnit fu; env.proj.enumerateAllCompilationUnits()) {
			if (fu.generated_files == null || fu.generated_files.length == 0) {
				System.out.println("    " + fu.pname() + ": none");
			} else {
				java.util.Arrays.sort(fu.generated_files);
				System.out.println("    " + fu.pname() + ": " + fu.generated_files.length);
				foreach (String f; fu.generated_files)
				System.out.println("        " + f);
			}
		}
//		foreach (CompilerParseInfo cpi; args) {
//			FileUnit fu = cpi.fu;
//			if (fu.generated_files == null || fu.generated_files.length == 0) {
//				System.out.println("    " + cpi.fname + ": none");
//			} else {
//				System.out.println("    " + cpi.fname + ": " + fu.generated_files.length);
//				foreach (String f; fu.generated_files)
//					System.out.println("        " + f);
//			}
//		}
	}

	public static void printHelp() {
		StringBuffer exts = new StringBuffer();
		int N = Compiler.getCmdLineExtSet().length;
		for (int i=0, sz=0; i < N; i++) {
			try {
				KievExt ext = (KievExt)i;
				String s = ext.toString();
				if (sz + s.length() + 3 >= 70) {
					exts.append("\n\t");
					sz = 0;
				}
				sz += s.length() + 3;
				exts.append('\"').append(s).append("\" ");
			} catch (ClassCastException e) {}
		}
		System.out.println(
 "Usage:  java kiev.Main [-g] [-debug x,y,z,..] [-prompt] [-i]\n"
+"           [-pipe] [-server]\n"
+"           [-gc N] [-d output_dir] [-verbose] [-classpath search_path]\n"
+"           [-D=var] [-verify] file.java ...\n"
+"\n"
+"  Each switch may be prepended by '-no' to change the default value.\n"
+"      For example, -no-project defeats the project feature.\n"
+"\n"
+" --  End of switches.  The allows the compiler to treat the\n"
+"     remainder of the command line as input files to compile.\n"
+"     This is necessary due to the syntax of certain options.\n"
+"\n"
+" -gui       Run the UI environment (default is Swing).\n"
+" -gui:swing run the Swing UI environment.\n"
+" -gui:swt   run the UI environment.\n"
+"        Default: run no GUI, only batch compilation.\n"
+"\n"
+" -g[:atclvr] Generate classes with debug info and other checks.  Parameters:\n"
+"		       Assert, Trace, Condition, Linetable, Vartable, Rule tracing\n"
+" -debug opt1,opt2,...	- debug output, options are:\n"
+"    stat     statement generation\n"
+"    asm      assembler generation\n"
+"    instr    instractions generation\n"
+"    bcread   bytecode .class reading\n"
+"    bcwrite  bytecode .class generation\n"
+"    bcrules  bytecode .class patching and rules\n"
+"    resolv   identifier resolving\n"
+"    flags    flags change for members\n"
+"    member   members attaching (AST generation)\n"
+"    create   members creation\n"
+"    ast      AST tree\n"
+"    types    var/field types\n"
+"\n"
+" -v or -verbose         Verbose operation.\n"
+" -warn                  Show warnings.\n"
+" -codewarn              Show codegeneration warnings.\n"
+" -prompt                Interactive compilation.\n"
+" -pipe [host][:port]    Connect to server at host:port\n"
+" -server [host][:port]  Kiev compiler is server at host:port;\n"
+"                        default is 127.0.0.1:1966\n"
+"\n"
+" -p or -project xxx     Sets the project file.  Default is no project.\n"
+"\n"
+" -classpath xxx   Sets the CLASSPATH; if not specified, the environment\n"
+"                  variable is used.\n"
+" -d               Output root directory.  Default: \"classes\".\n"
+"\n"
+" -verify          Generate verifiable code.\n"
+" -safe            Do not generate class files with compiler errors.\n"
+"\n"
+" -Dvar[=value]    Set compile-time constant.  Default value is \"\" or true.\n"
+"      To unset the value, use -no-Dvar (sets to null or false).\n"
+"      All periods ('.') in $Dvar are replaced with underscores ('_').\n"
+"      System properties provide predefined variables,	as supported by\n"
+"      (System.getProperties()).\n"
+"      The compile-time constants $FILE, $METHOD, $LINENO, and $DEBUG\n"
+"      are also defined.\n"
+" -disable \"extension\" completly disable the specified extension,\n"
+"      also you can manage extensions by pragmas in file headers,\n"
+"      complete list of extensions to enable/disable is:\n\t"
+       exts.toString()
			);
	}

/** is string not an option and has it embedded '*' or '?' characters?
 */
    static boolean hasWildcards(String s) {
        return
            s.length() > 0 &&
            s.charAt(0) != '-' &&
            (s.indexOf('*') >= 0 || s.indexOf("?") >= 0);
    }

/** does string s[soffset..] match pattern p[poffset..]?
 *  p can contain wildcards.
 */
    static boolean matches(String s, int soffset,
                           String p, int poffset) {
        if (poffset >= p.length())
            return soffset >= s.length();
        else if (p.charAt(poffset) == '*')
            return
                matches(s, soffset, p, poffset + 1) ||
                (soffset < s.length() &&
                 matches(s, soffset + 1, p, poffset));
        else
            return
                soffset < s.length() &&
                (p.charAt(poffset) == '?' ||
                 Character.toUpperCase(s.charAt(soffset)) ==
                 Character.toUpperCase(p.charAt(poffset))) &&
                matches(s, soffset + 1, p, poffset + 1);
    }

	/** add all files matching pattern string s to buffer expargs.
	*/
	static String[] addExpansion(String[] args, int pos) throws IOException {
		String s = args[pos].replace('/', File.separatorChar).replace('\\', File.separatorChar);
		File f = Kiev.newFile(s);
		String path = f.getParent();
		String name = f.getName();
		if (path == null)
			if (s.indexOf(File.separatorChar) == 0) path = "";
			else path = ".";
		File dir = new File(path + File.separatorChar);
		if (dir == null) return args;
		String[] files = dir.list();
		args[pos] = null;
		if (files != null) {
			for (int j = 0; j < files.length; j++) {
				if (matches(files[j], 0, name, 0)) {
					args = (String[])Arrays.appendUniq(args,path + File.separator + files[j]);
				}
			}
		}
		return args;
	}
}

