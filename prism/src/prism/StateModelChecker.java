//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package prism;

import jdd.*;
import odd.*;
import dv.*;
import mtbdd.*;
import sparse.*;
import hybrid.*;
import parser.*;
import parser.ast.*;

// Model checker for DTMCs

public class StateModelChecker implements ModelChecker
{
	// PRISM stuff
	protected Prism prism;
	protected PrismLog mainLog;
	protected PrismLog techLog;

	// options

	// which engine to use
	protected int engine;
	// parameter for termination criterion
	protected double termCritParam;
	// flags
	protected boolean precomp; // use 0,1 precomputation algorithms?

	// properties file
	protected PropertiesFile propertiesFile;

	// constant values
	protected Values constantValues;

	// class-wide storage for any numerical result returned
	protected double numericalRes;

	// model info
	protected ProbModel model;
	protected VarList varList;
	protected JDDNode trans;
	protected JDDNode trans01;
	protected JDDNode start;
	protected JDDNode reach;
	protected ODDNode odd;
	protected JDDVars allDDRowVars;
	protected JDDVars allDDColVars;
	protected JDDVars[] varDDRowVars;

	// constructor - set some defaults

	public StateModelChecker(Prism prism, Model m, PropertiesFile pf) throws PrismException
	{
		// initialise
		this.prism = prism;
		mainLog = prism.getMainLog();
		techLog = prism.getTechLog();
		if (!(m instanceof ProbModel)) {
			throw new PrismException("Wrong model type passed to ProbModelChecker.");
		}
		model = (ProbModel) m;
		propertiesFile = pf;
		varList = model.getVarList();
		trans = model.getTrans();
		trans01 = model.getTrans01();
		start = model.getStart();
		reach = model.getReach();
		odd = model.getODD();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		varDDRowVars = model.getVarDDRowVars();

		// create list of all constant values needed
		constantValues = new Values();
		constantValues.addValues(model.getConstantValues());
		if (pf != null)
			constantValues.addValues(pf.getConstantValues());

		// set up some default options
		// (although all should be overridden before model checking)
		engine = Prism.HYBRID;
		termCritParam = 1e-6;
		precomp = true;
	}

	// set engine

	public void setEngine(int e)
	{
		engine = e;
	}

	// set options (generic)

	public void setOption(String option, boolean b)
	{
		if (option.equals("precomp")) {
			precomp = b;
		} else if (option.equals("compact")) {
			PrismSparse.setCompact(b);
			PrismHybrid.setCompact(b);
		} else {
			mainLog.println("Warning: option \"" + option + "\" not supported by ProbModelChecker.");
		}
	}

	public void setOption(String option, int i)
	{
		if (option.equals("lineqmethod")) {
			PrismMTBDD.setLinEqMethod(i);
			PrismSparse.setLinEqMethod(i);
			PrismHybrid.setLinEqMethod(i);
		} else if (option.equals("termcrit")) {
			PrismMTBDD.setTermCrit(i);
			PrismSparse.setTermCrit(i);
			PrismHybrid.setTermCrit(i);
		} else if (option.equals("maxiters")) {
			PrismMTBDD.setMaxIters(i);
			PrismSparse.setMaxIters(i);
			PrismHybrid.setMaxIters(i);
		} else if (option.equals("sbmaxmem")) {
			PrismHybrid.setSBMaxMem(i);
		} else if (option.equals("numsblevels")) {
			PrismHybrid.setNumSBLevels(i);
		} else if (option.equals("sormaxmem")) {
			PrismHybrid.setSORMaxMem(i);
		} else if (option.equals("numsorlevels")) {
			PrismHybrid.setNumSORLevels(i);
		} else {
			mainLog.println("Warning: option \"" + option + "\" not supported by ProbModelChecker.");
		}
	}

	public void setOption(String option, double d)
	{
		if (option.equals("lineqmethodparam")) {
			PrismMTBDD.setLinEqMethodParam(d);
			PrismSparse.setLinEqMethodParam(d);
			PrismHybrid.setLinEqMethodParam(d);
		} else if (option.equals("termcritparam")) {
			termCritParam = d;
			PrismMTBDD.setTermCritParam(d);
			PrismSparse.setTermCritParam(d);
			PrismHybrid.setTermCritParam(d);
		} else {
			mainLog.println("Warning: option \"" + option + "\" not supported by ProbModelChecker.");
		}
	}

	public void setOption(String option, String s)
	{
		mainLog.println("Warning: option \"" + option + "\" not supported by ProbModelChecker.");
	}

	// -----------------------------------------------------------------------------------
	// Check a property, i.e. an expression
	// -----------------------------------------------------------------------------------

	public Object check(Expression expr) throws PrismException
	{
		long timer = 0;
		JDDNode dd;
		StateList states;
		boolean b;
		StateProbs res;
		Object singleRes;

		// start timer
		timer = System.currentTimeMillis();

		// do model checking and store result
		res = checkExpression(expr);
		
		// stop timer
		timer = System.currentTimeMillis() - timer;

		// print out model checking time
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		// output and result of model checking depend on return type
		if (expr.getType() == Expression.BOOLEAN) {

			states = new StateListMTBDD(((StateProbsMTBDD)res).getJDDNode(), model);
			JDD.Ref(((StateProbsMTBDD)res).getJDDNode());
			
			// print out number of satisfying states
			mainLog.print("\nNumber of satisfying states: ");
			mainLog.print(states.size());
			if (states.size() == model.getNumStates()) {
				mainLog.print(" (all)");
			} else if (states.includes(model.getStart())) {
				mainLog.print((model.getNumStartStates() == 1) ? " (including initial state)"
						: " (including all initial states)");
			} else {
				mainLog.print((model.getNumStartStates() == 1) ? " (initial state not satisfied)"
						: " (initial states not all satisfied)");
			}
			mainLog.print("\n");

			// if "verbose", print out satisfying states
			if (prism.getVerbose()) {
				mainLog.print("\nSatisfying states:");
				if (states.size() > 0) {
					mainLog.print("\n");
					states.print(mainLog);
				} else {
					mainLog.print(" (none)\n");
				}
			}

			// result is true if all states satisfy, false otherwise
			b = (states.size() == model.getNumStates());
			singleRes = b ? new Boolean(true) : new Boolean(false);
			states.clear();

			// print result
			mainLog.print("\nResult: " + b + " (property " + (b ? "" : "not ") + "satisfied in all states)\n");
		} else {
			singleRes = new Double(res.firstFromBDD(start));

			// print result
			mainLog.print("\nResult: " + singleRes + "\n");
		}

		// finished with memory
		res.clear();

		// return result
		return singleRes;
	}

	// Check expression (recursive)

	public StateProbs checkExpression(Expression expr) throws PrismException
	{
		StateProbs res;

		// If-then-else
		if (expr instanceof ExpressionITE) {
			res = checkExpressionITE((ExpressionITE)expr);
		}
		// Binary ops
		else if (expr instanceof ExpressionBinaryOp) {
			res = checkExpressionBinaryOp((ExpressionBinaryOp)expr);
		}
		// Unary ops
		else if (expr instanceof ExpressionUnaryOp) {
			res = checkExpressionUnaryOp((ExpressionUnaryOp)expr);
		}
		// Functions
		else if (expr instanceof ExpressionFunc) {
			res = checkExpressionFunc((ExpressionFunc)expr);
		}
		// Identifiers
		else if (expr instanceof ExpressionIdent) {
			// Should never happen
			throw new PrismException("Unknown identifier \"" + ((ExpressionIdent)expr).getName() + "\"");
		}
		// Literals
		else if (expr instanceof ExpressionLiteral) {
			res = checkExpressionLiteral((ExpressionLiteral)expr);
		}
		// Constants
		else if (expr instanceof ExpressionConstant) {
			res = checkExpressionConstant((ExpressionConstant)expr);
		}
		// Formulas
		else if (expr instanceof ExpressionFormula) {
			// Should never happen
			throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula)expr).getName() + "\"");
		}
		// Variables
		else if (expr instanceof ExpressionVar) {
			res = checkExpressionVar((ExpressionVar)expr);
		}
		// Labels
		else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel((ExpressionLabel) expr);
		}
		else {
			throw new PrismException("Couldn't check " + expr.getClass());
		}
		
		// Filter out non-reachable states from solution (TODO: not for dv?)
		res.filter(reach);

		return res;
	}

	public JDDNode checkExpressionDD(Expression expr) throws PrismException
	{
		return ((StateProbsMTBDD)checkExpression(expr)).getJDDNode();
	}
	
	// -----------------------------------------------------------------------------------
	// Check method for each operator
	// -----------------------------------------------------------------------------------

	// Check an 'if-then-else'
	
	private StateProbs checkExpressionITE(ExpressionITE expr) throws PrismException
	{
		JDDNode dd, dd1, dd2, dd3;
		
		dd1 = checkExpressionDD(expr.getOperand1());
		dd2 = checkExpressionDD(expr.getOperand2());
		dd3 = checkExpressionDD(expr.getOperand3());
		dd = JDD.ITE(dd1, dd2, dd3);
		
		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a binary operator
	
	private StateProbs checkExpressionBinaryOp(ExpressionBinaryOp expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;
		int op = expr.getOperator();
		
		// Optimisations are possible for relational operators
		// (note dubious use of knowledge that op IDs are consecutive)
		if (op >= ExpressionBinaryOp.EQ && op <= ExpressionBinaryOp.LE) {
			return checkExpressionRelOp(op, expr.getOperand1(), expr.getOperand2());
		}
		
		// Check operands
		tmp1 = checkExpressionDD(expr.getOperand1());
		try {
			tmp2 = checkExpressionDD(expr.getOperand2());
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		// Apply operation
		switch (op) {
		case ExpressionBinaryOp.IMPLIES: dd = JDD.Or(JDD.Not(tmp1), tmp2); break;
		case ExpressionBinaryOp.OR: dd = JDD.Or(tmp1, tmp2); break;
		case ExpressionBinaryOp.AND: dd = JDD.And(tmp1, tmp2); break;
		case ExpressionBinaryOp.PLUS: dd = JDD.Apply(JDD.PLUS, tmp1, tmp2); break;
		case ExpressionBinaryOp.MINUS: dd = JDD.Apply(JDD.MINUS, tmp1, tmp2); break;
		case ExpressionBinaryOp.TIMES: dd = JDD.Apply(JDD.TIMES, tmp1, tmp2); break;
		case ExpressionBinaryOp.DIVIDE: dd = JDD.Apply(JDD.DIVIDE, tmp1, tmp2); break;
		default: throw new PrismException("Unknown binary operator");
		}

		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a relational operator (=, !=, >, >=, < <=)
	
	private StateProbs checkExpressionRelOp(int op, Expression expr1, Expression expr2) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;
		String s;

		// check for some easy (and common) special cases before resorting to the general case
		
		// var relop int
		if (expr1 instanceof ExpressionVar && expr2.isConstant() && expr2.getType()==Expression.INT) {
			ExpressionVar e1;
			Expression e2;
			int i, j, l, h, v;
			e1 = (ExpressionVar)expr1;
			e2 = expr2;
			// get var's index
			s = e1.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e2.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDRowVars[v], i-l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDRowVars[v], i-l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			default: throw new PrismException("Unknown relational operator");
			}
			return new StateProbsMTBDD(dd, model);
		}
		// int relop var
		else if (expr1.isConstant() && expr1.getType()==Expression.INT && expr2 instanceof ExpressionVar) {
			Expression e1;
			ExpressionVar e2;
			int i, j, l, h, v;
			e1 = expr1;
			e2 = (ExpressionVar)expr2;
			// get var's index
			s = e2.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e1.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDRowVars[v], i-l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDRowVars[v], i-l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDRowVars[v], j-l, 1);
				break;
			default: throw new PrismException("Unknown relational operator");
			}
			return new StateProbsMTBDD(dd, model);
		}

		// general case
		tmp1 = checkExpressionDD(expr1);
		tmp2 = checkExpressionDD(expr2);
		switch (op) {
		case ExpressionBinaryOp.EQ:
			dd = JDD.Apply(JDD.EQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.NE:
			dd = JDD.Apply(JDD.NOTEQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.GT:
			dd = JDD.Apply(JDD.GREATERTHAN, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.GE:
			dd = JDD.Apply(JDD.GREATERTHANEQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.LT:
			dd = JDD.Apply(JDD.LESSTHAN, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.LE:
			dd = JDD.Apply(JDD.LESSTHANEQUALS, tmp1, tmp2);
			break;
		default: throw new PrismException("Unknown relational operator");
		}
		
		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a unary operator
	
	private StateProbs checkExpressionUnaryOp(ExpressionUnaryOp expr) throws PrismException
	{
		JDDNode dd, tmp;
		int op = expr.getOperator();
		
		// Check operand
		tmp = checkExpressionDD(expr.getOperand());
		
		// Apply operation
		switch (op) {
		case ExpressionUnaryOp.NOT: dd = JDD.Not(tmp); break;
		case ExpressionUnaryOp.MINUS: dd = JDD.Apply(JDD.MINUS, JDD.Constant(0), tmp); break;
		case ExpressionUnaryOp.PARENTH: dd = tmp; break;
		default: throw new PrismException("Unknown unary operator");
		}

		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a 'function'
	
	private StateProbs checkExpressionFunc(ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
			case ExpressionFunc.MIN: return checkExpressionFuncMin(expr);
			case ExpressionFunc.MAX: return checkExpressionFuncMax(expr);
			case ExpressionFunc.FLOOR: return checkExpressionFuncFloor(expr);
			case ExpressionFunc.CEIL: return checkExpressionFuncCeil(expr);
			case ExpressionFunc.POW: return checkExpressionFuncPow(expr);
			case ExpressionFunc.MOD: return checkExpressionFuncMod(expr);
			case ExpressionFunc.LOG: return checkExpressionFuncLog(expr);
			default: throw new PrismException("Unrecognised function \"" + expr.getName() + "\"");
		}
	}
	
	private StateProbs checkExpressionFuncMin(ExpressionFunc expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;
		
		dd = checkExpressionDD(expr.getOperand(0));
		n = expr.getNumOperands();
		for (i = 1; i < n; i++) {
			try {
				tmp = checkExpressionDD(expr.getOperand(i));
				dd = JDD.Apply(JDD.MIN, dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return new StateProbsMTBDD(dd, model);
	}
	
	private StateProbs checkExpressionFuncMax(ExpressionFunc expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;
		
		dd = checkExpressionDD(expr.getOperand(0));
		n = expr.getNumOperands();
		for (i = 1; i < n; i++) {
			try {
				tmp = checkExpressionDD(expr.getOperand(i));
				dd = JDD.Apply(JDD.MAX, dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return new StateProbsMTBDD(dd, model);
	}
	
	private StateProbs checkExpressionFuncFloor(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd;
		
		dd = checkExpressionDD(expr.getOperand(0));
		dd = JDD.MonadicApply(JDD.FLOOR, dd);
		
		return new StateProbsMTBDD(dd, model);
	}
	
	private StateProbs checkExpressionFuncCeil(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd;
		
		dd = checkExpressionDD(expr.getOperand(0));
		dd = JDD.MonadicApply(JDD.CEIL, dd);
		
		return new StateProbsMTBDD(dd, model);
	}
	
	private StateProbs checkExpressionFuncPow(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.POW, dd1, dd2);
		
		return new StateProbsMTBDD(dd, model);
	}
	
	private StateProbs checkExpressionFuncMod(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.MOD, dd1, dd2);
		
		return new StateProbsMTBDD(dd, model);
	}

	private StateProbs checkExpressionFuncLog(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = checkExpressionDD(expr.getOperand(0));
		dd2 = checkExpressionDD(expr.getOperand(1));
		dd = JDD.Apply(JDD.LOGXY, dd1, dd2);
		
		return new StateProbsMTBDD(dd, model);
	}

	// Check a literal
	
	private StateProbs checkExpressionLiteral(ExpressionLiteral expr) throws PrismException
	{
		JDDNode dd;
		switch (expr.getType()) {
			case Expression.BOOLEAN: dd = JDD.Constant(expr.evaluateBoolean(null, null) ? 1.0 : 0.0); break;
			case Expression.INT: dd = JDD.Constant(expr.evaluateInt(null, null)); break;
			case Expression.DOUBLE: dd = JDD.Constant(expr.evaluateDouble(null, null)); break;
			default: throw new PrismException("Unknown literal type");
		}
		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a constant
	
	private StateProbs checkExpressionConstant(ExpressionConstant expr) throws PrismException
	{
		int i;
		JDDNode dd;
		
		i = constantValues.getIndexOf(expr.getName());
		if (i == -1) throw new PrismException("Couldn't evaluate constant \"" + expr.getName() + "\"");
		switch (constantValues.getType(i)) {
			case Expression.INT: dd = JDD.Constant(constantValues.getIntValue(i)); break;
			case Expression.DOUBLE: dd = JDD.Constant(constantValues.getDoubleValue(i)); break;
			case Expression.BOOLEAN: dd = JDD.Constant(constantValues.getBooleanValue(i) ? 1.0 : 0.0); break;
			default: throw new PrismException("Unknown type for constant \"" + expr.getName() + "\"");
		}
		
		return new StateProbsMTBDD(dd, model);
	}
	
	// Check a variable reference
	
	private StateProbs checkExpressionVar(ExpressionVar expr) throws PrismException
	{
		String s;
		int v, l, h, i;
		JDDNode dd;
		
		s = expr.getName();
		// get the variable's index
		v = varList.getIndex(s);
		if (v == -1) {
			throw new PrismException("Unknown variable \"" + s + "\"");
		}
		// get some info on the variable
		l = varList.getLow(v);
		h = varList.getHigh(v);
		// create dd
		dd = JDD.Constant(0);
		for (i = l; i <= h; i++) {
			dd = JDD.SetVectorElement(dd, varDDRowVars[v], i-l, i);
		}
				
		return new StateProbsMTBDD(dd, model);
	}
	
	// Check label

	private StateProbs checkExpressionLabel(ExpressionLabel expr) throws PrismException
	{
		LabelList ll;
		JDDNode dd;
		int i;

		// treat special cases
		if (expr.getName().equals("deadlock")) {
			dd = model.getFixedDeadlocks();
			JDD.Ref(dd);
		}
		else if (expr.getName().equals("init")) {
			dd = start;
			JDD.Ref(dd);
		}
		else {
			// get expression associated with label
			ll = propertiesFile.getCombinedLabelList();
			i = ll.getLabelIndex(expr.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + expr.getName() + "\" in property");
			// check recursively
			dd = checkExpressionDD(ll.getLabel(i));
		}
		
		return new StateProbsMTBDD(dd, model);
	}
}

// ------------------------------------------------------------------------------
