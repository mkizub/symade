package kiev.ir.java15;

import kiev.Kiev;
import kiev.Kiev.Ext;
import kiev.stdlib.*;
import kiev.vlang.*;
import kiev.vlang.types.*;
import kiev.parser.*;

import static kiev.stdlib.Debug.*;
import syntax kiev.Syntax;

/**
 * @author Maxim Kizub
 *
 */

@nodeview
public final view RRuleMethod of RuleMethod extends RMethod {
	public:ro	Var[]				localvars;
	public		int					base;
	public		int					max_depth;
	public		int					state_depth;
	public		int					max_vars;
	public		int					index;		// index counter for RuleNode.idx

	public boolean preGenerate() {
		this.open();
		Var penv = params[0];
		assert(penv.u_name == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
		if( body instanceof RuleBlock ) {
			body.preGenerate();
			Kiev.runProcessorsOn(body);
			body.cleanDFlow();
		}
		return true;
	}

	public void resolveDecl() {
		trace(Kiev.debugResolve,"Resolving rule "+this);
		try {
			Var penv = params[0];
			assert(penv.u_name == namePEnv && penv.getType() ≡ Type.tpRule, "Expected to find 'rule $env' but found "+penv.getType()+" "+penv);
			if( body != null ) {
				if( type.ret() ≡ Type.tpVoid ) body.setAutoReturnable(true);
				body.resolve(Type.tpVoid);
			}
			if( body != null && !body.isMethodAbrupted() ) {
				if( type.ret() ≡ Type.tpVoid ) {
					block.stats.append(new ReturnStat(pos,null));
					body.setAbrupted(true);
				} else {
					Kiev.reportError(body,"Return requared");
				}
			}
		} catch(Exception e ) {
			Kiev.reportError(body,e);
		}
		((RuleMethod)this).cleanDFlow();
	}
}

@nodeview
public final view RRuleBlock of RuleBlock extends RENode {
	public ASTRuleNode		node;
	public StringBuffer		fields_buf;

	public boolean preGenerate() {
		this.open();
		node.rnResolve();
		fields_buf = new StringBuffer();
		node.resolve1(new JumpNodes(false,null,false,null,false));
		StringBuffer sb = new StringBuffer(256);
		sb.append("{ ");
		// Declare private method frame class
		String tmpClassName = "frame$$";
		sb.append("static class ").append(tmpClassName).append(" extends rule{\n");
		sb.append("int bt$;\n");
		RuleMethod rule_method = (RuleMethod)ctx_method;
		// Backtrace holders
		for (int i=0; i < rule_method.max_depth; i++)
			sb.append("int bt$").append(i).append(";\n");
		// Local variables
		foreach(Var v; rule_method.localvars) {
			String tp = Kiev.reparseType(v.type);
			sb.append(tp+' '+v.u_name+";\n");
		}
		// tmp variables inserted here
		sb.append(fields_buf.toString());
		fields_buf = null;
		sb.append("}\n");
		// Create new method frame or hash values from
		// existing one
		sb.append(tmpClassName).append(" $env;\n");
		sb.append("int bt$;\n");
		sb.append("if("+namePEnv+"==null) {\n");
		sb.append(" $env=new ").append(tmpClassName).append("(); bt$=0;\n");
		sb.append(" goto enter$1;\n");
		sb.append("}\n");
		if (rule_method.base != 1) {
			sb.append("else{\n");
			sb.append(" $env=($cast ").append(tmpClassName).append(")"+namePEnv+";\n");
			sb.append(" bt$=$env.bt$;\n");
			sb.append("}\n");
			sb.append("switch(bt$) {\ncase 0:\n");
		} else {
			// BUG!!!
			sb.append("else{\n$env=($cast ").append(tmpClassName).append(")"+namePEnv+";}\n");
		}
		sb.append("return null;\n");
		node.createText(sb);
		// Close method
		if (rule_method.base != 1)
			sb.append("}\nreturn null;\n");
		sb.append("}\n");
		trace(Kiev.debugRules,"Rule text generated:\n"+sb);
		Block mbody = Kiev.parseBlock((RuleBlock)this,sb);
		ctx_method.body = mbody;
		return false;
	}
}

