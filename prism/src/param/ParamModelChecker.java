//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

/*
 * TODO
 * - lumpers should start convert directly from ParamModel plus scheduler and rewards
 *   rather than from AlterablePMC as is done currently
 * - could print to log for which parameter values results will be valid
 * - could implement steady-state properties for models w.o.
 *   nondeterminism but not needed at least not for next paper
 * - could implement DAG-like functions + probabilistic equality
 *   - for each function num and den would each be pointers into DAG
 *   - then either exact equality (expensive)
 *   - or probabilistic equality (Schwartz-Zippel)
 * - also, DAG-like regexp representation possible
 * - for comparism with previous work, use 
 * - could implement other types of regions apart from boxes
 * - could later improve support for optimisation over parameters
 *   - using apache math commons
 *   - or ipopt (zip file with java support for linux, windows, mac os x exists)
 * - libraries used should be loaded by classloader to make easier to use in
 *   projects where we cannot use GPLed code (just delete library and that's it)
 * - could later add support for abstraction of functions
 * - could integrate in GUI (student project?)
 * - if time left, add JUnit tests at least for BigRational and maybe functions and regions
 *   basically for all classes where interface is more or less fixed
 * - could try to bind to Ginac for comparability, but probably not much difference
 * - should integrate binding to solvers (RAHD and the like) at some point
 */

package param;

import java.io.File;
import java.util.*;

import common.IterableBitSet;
import common.StopWatch;
import common.iterable.FunctionalIterable;
import explicit.DTMC;
import explicit.SCCConsumerStore;
import explicit.Utils;
import param.Lumper.BisimType;
import param.StateEliminator.EliminationOrder;
import parser.State;
import parser.Values;
import parser.ast.*;
import parser.ast.ExpressionFilter.FilterOperator;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import parser.type.TypePathBool;
import parser.type.TypePathDouble;
import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismSettings;
import prism.PrismNotSupportedException;
import prism.Result;
import prism.SteadyStateProbs;
import prism.SteadyStateCache;
import edu.jas.kern.ComputerThreads;
import explicit.Model;

/**
 * Model checker for parametric Markov models.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final public class ParamModelChecker extends PrismComponent
{
	// Model file (for reward structures, etc.)
	private ModulesFile modulesFile = null;

	// Properties file (for labels, constants, etc.)
	private PropertiesFile propertiesFile = null;

	// Constants (extracted from model/properties)
	private Values constantValues;

	// The result of model checking will be stored here
	private Result result;
	
	// Flags/settings

	/** The mode (parametric or exact)? */
	private ParamMode mode;

	// Verbosity level
	private int verbosity = 0;
	
	private BigRational[] paramLower;
	private BigRational[] paramUpper;

	private FunctionFactory functionFactory;
	private RegionFactory regionFactory;
	private ConstraintChecker constraintChecker;
	private ValueComputer valueComputer;
	
	private BigRational precision;
	private int splitMethod;
	private EliminationOrder eliminationOrder;
	private int numRandomPoints;
	private Lumper.BisimType bisimType;
	private boolean simplifyRegions;

	private ModelBuilder modelBuilder;

	// Use precomputation algorithms in model checking?
	protected boolean precomp = true;
	protected boolean prob0 = true;
	protected boolean prob1 = true;

	/**
	 * Constructor
	 */
	public ParamModelChecker(PrismComponent parent, ParamMode mode) throws PrismException
	{
		super(parent);
		this.mode = mode;

		// If present, initialise settings from PrismSettings
		if (settings != null) {
		verbosity = settings.getBoolean(PrismSettings.PRISM_VERBOSE) ? 10 : 1;
		precision = new BigRational(settings.getString(PrismSettings.PRISM_PARAM_PRECISION));
		String splitMethodString = settings.getString(PrismSettings.PRISM_PARAM_SPLIT);
		if (splitMethodString.equals("Longest")) {
			splitMethod = BoxRegion.SPLIT_LONGEST;
		} else if (splitMethodString.equals("All")) {
			splitMethod = BoxRegion.SPLIT_ALL;
		} else {
			throw new PrismException("unknown region splitting method " + splitMethodString);				
		}
		String eliminationOrderString = settings.getString(PrismSettings.PRISM_PARAM_ELIM_ORDER);
		if (eliminationOrderString.equals("Arbitrary")) {
			eliminationOrder = EliminationOrder.ARBITRARY;
		} else if (eliminationOrderString.equals("Forward")) {
			eliminationOrder = EliminationOrder.FORWARD;
		} else if (eliminationOrderString.equals("Forward-reversed")) {
			eliminationOrder = EliminationOrder.FORWARD_REVERSED;
		} else if (eliminationOrderString.equals("Backward")) {
			eliminationOrder = EliminationOrder.BACKWARD;
		} else if (eliminationOrderString.equals("Backward-reversed")) {
			eliminationOrder = EliminationOrder.BACKWARD_REVERSED;
		} else if (eliminationOrderString.equals("Random")) {
			eliminationOrder = EliminationOrder.RANDOM;
		} else {
			throw new PrismException("unknown state elimination order " + eliminationOrderString);				
		}
		numRandomPoints = settings.getInteger(PrismSettings.PRISM_PARAM_RANDOM_POINTS);
		String bisimTypeString = settings.getString(PrismSettings.PRISM_PARAM_BISIM);
		if (bisimTypeString.equals("Weak")) {
			bisimType = BisimType.WEAK;
		} else if (bisimTypeString.equals("Strong")) {
			bisimType = BisimType.STRONG;
		} else if (bisimTypeString.equals("None")) {
			bisimType = BisimType.NULL;
		} else {
			throw new PrismException("unknown bisimulation type " + bisimTypeString);							
		}
		simplifyRegions = settings.getBoolean(PrismSettings.PRISM_PARAM_SUBSUME_REGIONS);
		}
	}
	
	// Setters/getters

	/**
	 * Set the attached model file (for e.g. reward structures when model checking)
	 * and the attached properties file (for e.g. constants/labels when model checking)
	 */
	public void setModulesFileAndPropertiesFile(ModulesFile modulesFile, PropertiesFile propertiesFile)
	{
		this.modulesFile = modulesFile;
		this.propertiesFile = propertiesFile;
		// Get combined constant values from model/properties
		constantValues = new Values();
		constantValues.addValues(modulesFile.getConstantValues());
		if (propertiesFile != null)
			constantValues.addValues(propertiesFile.getConstantValues());
	}

	public ParamMode getMode()
	{
		return mode;
	}

	// Model checking functions

	/**
	 * Model check an expression, process and return the result.
	 * Information about states and model constants should be attached to the model.
	 * For other required info (labels, reward structures, etc.), use the methods
	 * {@link #setModulesFile} and {@link #setPropertiesFile}
	 * to attach the original model/properties files.
	 */
	public Result check(Model model, Expression expr) throws PrismException
	{
		ParamModel paramModel = (ParamModel) model;
		functionFactory = paramModel.getFunctionFactory();
		constraintChecker = new ConstraintChecker(numRandomPoints);
		regionFactory = new BoxRegionFactory(functionFactory, constraintChecker, precision,
				model.getNumStates(), model.getFirstInitialState(), simplifyRegions, splitMethod);
		valueComputer = new ValueComputer(this, mode, paramModel, regionFactory, precision, eliminationOrder, bisimType);
		
		long timer = 0;
		
		// Remove labels from property, using combined label list (on a copy of the expression)
		// This is done now so that we can handle labels nested below operators that are not
		// handled natively by the model checker yet (just evaluate()ed in a loop).
		expr = (Expression) expr.deepCopy().expandLabels(propertiesFile.getCombinedLabelList());

		// Also evaluate/replace any constants
		//expr = (Expression) expr.replaceConstants(constantValues);

		// Wrap a filter round the property, if needed
		// (in order to extract the final result of model checking) 
		expr = ExpressionFilter.addDefaultFilterIfNeeded(expr, model.getNumInitialStates() == 1);

		// Do model checking and store result vector
		timer = System.currentTimeMillis();
		BitSet needStates = new BitSet(model.getNumStates());
		needStates.set(0, model.getNumStates());
		RegionValues vals = checkExpression(paramModel, expr, needStates);
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking: " + timer / 1000.0 + " seconds.");

		if (constraintChecker.unsoundCheckWasUsed()) {
			mainLog.printWarning("Computation of Boolean values / parameter regions used heuristic sampling, results are potentially inaccurate.");
		}

		// Store result
		result = new Result();
		vals.clearExceptInit();
		result.setResult(new ParamResult(mode, vals, modelBuilder, functionFactory));
		
		/* // Output plot to tex file
		if (paramLower.length == 2) {
			try {
				FileOutputStream file = new FileOutputStream("out.tex");
				ResultExporter printer = new ResultExporter();
				printer.setOutputStream(file);
				printer.setRegionValues(vals);
				printer.setPointsPerDimension(19);
				printer.print();
				file.close();
			} catch (Exception e) {
				throw new PrismException("file could not be written");
			}
		}*/
		
		return result;
	}
	
	private int parserBinaryOpToRegionOp(int parserOp) throws PrismException
	{
		int regionOp;
		switch (parserOp) {
		case ExpressionBinaryOp.IMPLIES:
			regionOp = Region.IMPLIES;
			break;
		case ExpressionBinaryOp.IFF:
			regionOp = Region.IMPLIES;
			break;
		case ExpressionBinaryOp.OR:
			regionOp = Region.OR;
			break;
		case ExpressionBinaryOp.AND:
			regionOp = Region.AND;
			break;
		case ExpressionBinaryOp.EQ:
			regionOp = Region.EQ;
			break;
		case ExpressionBinaryOp.NE:
			regionOp = Region.NE;
			break;
		case ExpressionBinaryOp.GT:
			regionOp = Region.GT;
			break;
		case ExpressionBinaryOp.GE:
			regionOp = Region.GE;
			break;
		case ExpressionBinaryOp.LT:
			regionOp = Region.LT;
			break;
		case ExpressionBinaryOp.LE:
			regionOp = Region.LE;
			break;
		case ExpressionBinaryOp.PLUS:
			regionOp = Region.PLUS;
			break;
		case ExpressionBinaryOp.MINUS:
			regionOp = Region.MINUS;
			break;
		case ExpressionBinaryOp.TIMES:
			regionOp = Region.TIMES;
			break;
		case ExpressionBinaryOp.DIVIDE:
			regionOp = Region.DIVIDE;
			break;
		default:
			throw new PrismNotSupportedException("operator \"" + ExpressionBinaryOp.opSymbols[parserOp]
					+ "\" not currently supported for " + mode + " analyses");
		}
		return regionOp;
	}

	private int parserUnaryOpToRegionOp(int parserOp) throws PrismException
	{
		int regionOp;
		switch (parserOp) {
		case ExpressionUnaryOp.MINUS:
			regionOp = Region.UMINUS;
			break;
		case ExpressionUnaryOp.NOT:
			regionOp = Region.NOT;
			break;
		case ExpressionUnaryOp.PARENTH:
			regionOp = Region.PARENTH;
			break;
		default:
			throw new PrismNotSupportedException("operator \"" + ExpressionBinaryOp.opSymbols[parserOp]
					+ "\" not currently supported for " + mode + " analyses");
		}
		return regionOp;
	}

	/**
	 * Model check an expression and return a vector result values over all states.
	 * Information about states and model constants should be attached to the model.
	 * For other required info (labels, reward structures, etc.), use the methods
	 * {@link #setModulesFile} and {@link #setPropertiesFile}
	 * to attach the original model/properties files.
	 */
	RegionValues checkExpression(ParamModel model, Expression expr, BitSet needStates) throws PrismException
	{
		RegionValues res;
		if (expr instanceof ExpressionUnaryOp) {
			res = checkExpressionUnaryOp(model, (ExpressionUnaryOp) expr, needStates);
		} else if (expr instanceof ExpressionBinaryOp) {
			res = checkExpressionBinaryOp(model, (ExpressionBinaryOp) expr, needStates);
		} else if (expr instanceof ExpressionITE) {
			res = checkExpressionITE(model, (ExpressionITE) expr, needStates);
		} else if (expr instanceof ExpressionLabel) {
			res = checkExpressionLabel(model, (ExpressionLabel) expr, needStates);
		} else if (expr instanceof ExpressionFormula) {
			// This should have been defined or expanded by now.
			if (((ExpressionFormula) expr).getDefinition() != null)
				res = checkExpression(model, ((ExpressionFormula) expr).getDefinition(), needStates);
			else
				throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula) expr).getName() + "\"");
		} else if (expr instanceof ExpressionProp) {
			res = checkExpressionProp(model, (ExpressionProp) expr, needStates);
		} else if (expr instanceof ExpressionFilter) {
			if (((ExpressionFilter) expr).isParam()) {
				res = checkExpressionFilterParam(model, (ExpressionFilter) expr, needStates);
			} else {
				res = checkExpressionFilter(model, (ExpressionFilter) expr, needStates);
			}
		} else if (expr instanceof ExpressionProb) {
			res = checkExpressionProb(model, (ExpressionProb) expr, needStates);
		} else if (expr instanceof ExpressionReward) {
			res = checkExpressionReward(model, (ExpressionReward) expr, needStates);
		} else if (expr instanceof ExpressionSS) {
			res = checkExpressionSteadyState(model, (ExpressionSS) expr, needStates);
		} else if (expr instanceof ExpressionForAll || expr instanceof ExpressionExists) {
			throw new PrismNotSupportedException("Non-probabilistic CTL model checking is currently not supported in the " + mode.engine());
		} else if (expr instanceof ExpressionFunc && ((ExpressionFunc)expr).getNameCode() == ExpressionFunc.MULTI) {
			throw new PrismNotSupportedException("Multi-objective model checking is not supported in the " + mode.engine());
		} else {
			res = checkExpressionAtomic(model, expr, needStates);
		}
		return res;
	}

	private RegionValues checkExpressionAtomic(ParamModel model, Expression expr, BitSet needStates) throws PrismException
	{
		expr = (Expression) expr.deepCopy().replaceConstants(constantValues);
		
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
		int[] varMap = new int[statesList.get(0).varValues.length];
		for (int var = 0; var < varMap.length; var++) {
			varMap[var] = var;
		}
		for (int state = 0; state < numStates; state++) {
			Expression exprVar = (Expression) expr.deepCopy().evaluatePartially(statesList.get(state), varMap);
			if (needStates.get(state)) {
				if (exprVar instanceof ExpressionLiteral) {
					ExpressionLiteral exprLit = (ExpressionLiteral) exprVar;
					if (exprLit.getType() instanceof TypeBool) {
						stateValues.setStateValue(state, exprLit.evaluateBoolean());
					} else if (exprLit.getType() instanceof TypeInt || exprLit.getType() instanceof TypeDouble) {
						String exprStr = exprLit.getString();
						BigRational exprRat = new BigRational(exprStr);
						stateValues.setStateValue(state, functionFactory.fromBigRational(exprRat));
					} else {
						throw new PrismNotSupportedException("model checking expresssion " + expr + " not supported for " + mode + " models");
					}
				} else if (exprVar instanceof ExpressionConstant) {
					ExpressionConstant exprConst = (ExpressionConstant) exprVar;
					stateValues.setStateValue(state, functionFactory.getVar(exprConst.getName()));
				} else {
					throw new PrismNotSupportedException("cannot handle expression " + expr + " in " + mode + " analysis");
				}
			} else {
				if (exprVar.getType() instanceof TypeBool) {
					stateValues.setStateValue(state, false);
				} else {
					stateValues.setStateValue(state, functionFactory.getZero());						
				}
			}
		}	
		return regionFactory.completeCover(stateValues);
	}

	protected RegionValues checkExpressionUnaryOp(ParamModel model, ExpressionUnaryOp expr, BitSet needStates) throws PrismException
	{
		RegionValues resInner = checkExpression(model, expr.getOperand(), needStates);
		resInner.clearNotNeeded(needStates);

		return resInner.unaryOp(parserUnaryOpToRegionOp(expr.getOperator()));
	}

	/**
	 * Model check a binary operator.
	 */
	protected RegionValues checkExpressionBinaryOp(ParamModel model, ExpressionBinaryOp expr, BitSet needStates) throws PrismException
	{
		RegionValues res1 = checkExpression(model, expr.getOperand1(), needStates);
		RegionValues res2 = checkExpression(model, expr.getOperand2(), needStates);
		res1.clearNotNeeded(needStates);
		res2.clearNotNeeded(needStates);

		return res1.binaryOp(parserBinaryOpToRegionOp(expr.getOperator()), res2);
	}

	/**
	 * Model check an If-Then-Else operator.
	 */
	protected RegionValues checkExpressionITE(ParamModel model, ExpressionITE expr, BitSet needStates) throws PrismException
	{
		RegionValues resI = checkExpression(model, expr.getOperand1(), needStates);
		RegionValues resT = checkExpression(model, expr.getOperand2(), needStates);
		RegionValues resE = checkExpression(model, expr.getOperand3(), needStates);
		resI.clearNotNeeded(needStates);
		resT.clearNotNeeded(needStates);
		resE.clearNotNeeded(needStates);

		return resI.ITE(resT, resE);
	}

	/**
	 * Model check a label.
	 */
	protected RegionValues checkExpressionLabel(ParamModel model, ExpressionLabel expr, BitSet needStates) throws PrismException
	{
		LabelList ll;
		int i;
		
		// treat special cases
		if (expr.isDeadlockLabel()) {
			int numStates = model.getNumStates();
			StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
			for (i = 0; i < numStates; i++) {
				stateValues.setStateValue(i, model.isDeadlockState(i));
			}
			return regionFactory.completeCover(stateValues);
		} else if (expr.isInitLabel()) {
			int numStates = model.getNumStates();
			StateValues stateValues = new StateValues(numStates, model.getFirstInitialState());
			for (i = 0; i < numStates; i++) {
				stateValues.setStateValue(i, model.isInitialState(i));
			}
			return regionFactory.completeCover(stateValues);
		} else {
			ll = propertiesFile.getCombinedLabelList();
			i = ll.getLabelIndex(expr.getName());
			if (i == -1)
				throw new PrismException("Unknown label \"" + expr.getName() + "\" in property");
			// check recursively
			return checkExpression(model, ll.getLabel(i), needStates);
		}
	}

	// Check property ref

	protected RegionValues checkExpressionProp(ParamModel model, ExpressionProp expr, BitSet needStates) throws PrismException
	{
		// Look up property and check recursively
		Property prop = propertiesFile.lookUpPropertyObjectByName(expr.getName());
		if (prop != null) {
			mainLog.println("\nModel checking : " + prop);
			return checkExpression(model, prop.getExpression(), needStates);
		} else {
			throw new PrismException("Unknown property reference " + expr);
		}
	}

	// Check filter

	protected RegionValues checkExpressionFilter(ParamModel model, ExpressionFilter expr, BitSet needStates) throws PrismException
	{
		Expression filter = expr.getFilter();
		if (filter == null) {
			filter = Expression.True();
		}
		boolean filterTrue = Expression.isTrue(filter);
		
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		RegionValues rvFilter = checkExpression(model, filter, needStatesInner);
		if (!rvFilter.parameterIndependent()) {
			throw new PrismException("currently, parameter-dependent filters are not supported");
		}
		BitSet bsFilter = rvFilter.getStateValues().toBitSet();
		// Check filter satisfied by exactly one state
		FilterOperator op = expr.getOperatorType();
		if (op == FilterOperator.STATE && bsFilter.cardinality() != 1) {
			String s = "Filter should be satisfied in exactly 1 state";
			s += " (but \"" + filter + "\" is true in " + bsFilter.cardinality() + " states)";
			throw new PrismException(s);
		}
		if (op == FilterOperator.FIRST) {
			// only first state is of interest
			bsFilter.clear(bsFilter.nextSetBit(0) + 1, bsFilter.length());
		}
		RegionValues vals = checkExpression(model, expr.getOperand(), bsFilter);

		// Check if filter state set is empty; we treat this as an error
		if (bsFilter.isEmpty()) {
			throw new PrismException("Filter satisfies no states");
		}
		
		// Remember whether filter is for the initial state and, if so, whether there's just one
		boolean filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).isInitLabel());
		// Print out number of states satisfying filter
		if (!filterInit) {
			mainLog.println("\nStates satisfying filter " + filter + ": " + bsFilter.cardinality());
		}
			
		// Compute result according to filter type
		RegionValues resVals = null;
		switch (op) {
		case PRINT:
		case PRINTALL:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
			} else {
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
			}

			vals.printFiltered(mainLog, mode, expr.getType(), bsFilter,
				               model.getStatesList(),
				               op == FilterOperator.PRINT, // printSparse if PRINT
				               true,  // print state values
				               true); // print state index

			resVals = vals;
			break;
		case MIN:
		case MAX:
		case ARGMIN:
		case ARGMAX:
			throw new PrismNotSupportedException("operation not implemented for " + mode + " models");
		case COUNT:
			resVals = vals.op(Region.COUNT, bsFilter);
			break;
		case SUM:
			resVals = vals.op(Region.PLUS, bsFilter);
			break;
		case AVG:
			resVals = vals.op(Region.AVG, bsFilter);
			break;
		case FIRST:
			if (bsFilter.cardinality() < 1) {
				throw new PrismException("Filter should be satisfied in at least 1 state.");
			}
			resVals = vals.op(Region.FIRST, bsFilter);
			break;
		case RANGE:
			throw new PrismNotSupportedException("operation not implemented for " + mode + " models");
		case FORALL:
			resVals = vals.op(Region.FORALL, bsFilter);
			break;
		case EXISTS:
			resVals = vals.op(Region.EXISTS, bsFilter);
			break;
		case STATE:
			resVals = vals.op(Region.FIRST, bsFilter);
			break;
		default:
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		return resVals;
	}

	// check filter over parameters
	
	protected RegionValues checkExpressionFilterParam(ParamModel model, ExpressionFilter expr, BitSet needStates) throws PrismException
	{
		// Filter info
		Expression filter = expr.getFilter();
		// Create default filter (true) if none given
		if (filter == null) {
			filter = Expression.True();
		}
		RegionValues rvFilter = checkExpression(model, filter, needStates);
		RegionValues vals = checkExpression(model, expr.getOperand(), needStates);

		Optimiser opt = new Optimiser(vals, rvFilter, expr.getOperatorType() == FilterOperator.MIN);
		System.out.println("\n" + opt.optimise());
		
		return null;

		/*
		// Remember whether filter is for the initial state and, if so, whether there's just one
		filterInit = (filter instanceof ExpressionLabel && ((ExpressionLabel) filter).isInitLabel());
		filterInitSingle = filterInit & model.getNumInitialStates() == 1;
		// Print out number of states satisfying filter
		if (!filterInit)
			mainLog.println("\nStates satisfying filter " + filter + ": " + bsFilter.cardinality());

		// Compute result according to filter type
		op = expr.getOperatorType();
		switch (op) {
		case PRINT:
			// Format of print-out depends on type
			if (expr.getType() instanceof TypeBool) {
				// NB: 'usual' case for filter(print,...) on Booleans is to use no filter
				mainLog.print("\nSatisfying states");
				mainLog.println(filterTrue ? ":" : " that are also in filter " + filter + ":");
				vals.printFiltered(mainLog, bsFilter);
			} else {
				mainLog.println("\nResults (non-zero only) for filter " + filter + ":");
				vals.printFiltered(mainLog, bsFilter);
			}
			// Result vector is unchanged; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			break;
		case MIN:
			// Compute min
			// Store as object/vector
			resObj = vals.minOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Minimum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case MAX:
			// Compute max
			// Store as object/vector
			resObj = vals.maxOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Maximum value over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			// Also find states that (are close to) selected value for display to log
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			break;
		case ARGMIN:
			// Compute/display min
			resObj = vals.minOverBitSet(bsFilter);
			mainLog.print("\nMinimum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			// TODO: un-hard-code precision once RegionValues knows hoe precise it is
			bsMatch = vals.getBitSetFromCloseValue(resObj, 1e-5, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMIN, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = RegionValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with minimum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case ARGMAX:
			// Compute/display max
			resObj = vals.maxOverBitSet(bsFilter);
			mainLog.print("\nMaximum value over " + filterStatesString + ": " + resObj);
			// Find states that (are close to) selected value
			bsMatch = vals.getBitSetFromCloseValue(resObj, precision, false);
			bsMatch.and(bsFilter);
			// Store states in vector; for ARGMAX, don't store a single value (in resObj)
			// Also, don't bother with explanation string
			resVals = RegionValues.createFromBitSet(bsMatch, model);
			// Print out number of matching states, but not the actual states
			mainLog.println("\nNumber of states with maximum value: " + bsMatch.cardinality());
			bsMatch = null;
			break;
		case COUNT:
			// Compute count
			int count = vals.countOverBitSet(bsFilter);
			// Store as object/vector
			resObj = Integer.valueOf(count);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = filterTrue ? "Count of satisfying states" : "Count of satisfying states also in filter";
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case SUM:
			// Compute sum
			// Store as object/vector
			resObj = vals.sumOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Sum over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case AVG:
			// Compute average
			// Store as object/vector
			resObj = vals.averageOverBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Average over " + filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FIRST:
			// Find first value
			resObj = vals.firstFromBitSet(bsFilter);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Value in ";
			if (filterInit) {
				resultExpl += filterInitSingle ? "the initial state" : "first initial state";
			} else {
				resultExpl += filterTrue ? "the first state" : "first state satisfying filter";
			}
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case RANGE:
			// Find range of values
			resObj = new prism.Interval(vals.minOverBitSet(bsFilter), vals.maxOverBitSet(bsFilter));
			// Leave result vector unchanged: for a range, result is only available from Result object
			resVals = vals;
			// Set vals to null to stop it being cleared below
			vals = null;
			// Create explanation of result and print some details to log
			resultExpl = "Range of values over ";
			resultExpl += filterInit ? "initial states" : filterStatesString;
			mainLog.println("\n" + resultExpl + ": " + resObj);
			break;
		case FORALL:
			// Get access to BitSet for this
			if(paras == null) {
				bs = vals.getBitSet();
				// Print some info to log
				mainLog.print("\nNumber of states satisfying " + expr.getOperand() + ": ");
				mainLog.print(bs.cardinality());
				mainLog.println(bs.cardinality() == model.getNumStates() ? " (all in model)" : "");
				// Check "for all" over filter
				b = vals.forallOverBitSet(bsFilter);
				// Store as object/vector
				resObj = Boolean.valueOf(b);
				resVals = new RegionValues(expr.getType(), resObj, model); 
				// Create explanation of result and print some details to log
				resultExpl = "Property " + (b ? "" : "not ") + "satisfied in ";
				mainLog.print("\nProperty satisfied in " + vals.countOverBitSet(bsFilter));
				if (filterInit) {
					if (filterInitSingle) {
						resultExpl += "the initial state";
					} else {
						resultExpl += "all initial states";
					}
					mainLog.println(" of " + model.getNumInitialStates() + " initial states.");
				} else {
					if (filterTrue) {
						resultExpl += "all states";
						mainLog.println(" of all " + model.getNumStates() + " states.");
					} else {
						resultExpl += "all filter states";
						mainLog.println(" of " + bsFilter.cardinality() + " filter states.");
					}
				}
			}
			break;
		case EXISTS:
			// Get access to BitSet for this
			bs = vals.getBitSet();
			// Check "there exists" over filter
			b = vals.existsOverBitSet(bsFilter);
			// Store as object/vector
			resObj = Boolean.valueOf(b);
			resVals = new RegionValues(expr.getType(), resObj, model); 
			// Create explanation of result and print some details to log
			resultExpl = "Property satisfied in ";
			if (filterTrue) {
				resultExpl += b ? "at least one state" : "no states";
			} else {
				resultExpl += b ? "at least one filter state" : "no filter states";
			}
			mainLog.println("\n" + resultExpl);
			break;
		case STATE:
			if(paras == null) {
				// Check filter satisfied by exactly one state
				if (bsFilter.cardinality() != 1) {
					String s = "Filter should be satisfied in exactly 1 state";
					s += " (but \"" + filter + "\" is true in " + bsFilter.cardinality() + " states)";
					throw new PrismException(s);
				}
				// Find first (only) value
				// Store as object/vector
				resObj = vals.firstFromBitSet(bsFilter);
				resVals = new RegionValues(expr.getType(), resObj, model); 
				// Create explanation of result and print some details to log
				resultExpl = "Value in ";
				if (filterInit) {
					resultExpl += "the initial state";
				} else {
					resultExpl += "the filter state";
				}
				mainLog.println("\n" + resultExpl + ": " + resObj);
			}
			break;
		default:
			throw new PrismException("Unrecognised filter type \"" + expr.getOperatorName() + "\"");
		}

		// For some operators, print out some matching states
		if (bsMatch != null) {
			states = RegionValues.createFromBitSet(bsMatch, model);
			mainLog.print("\nThere are " + bsMatch.cardinality() + " states with ");
			mainLog.print(expr.getType() instanceof TypeDouble ? "(approximately) " : "" + "this value");
			boolean verbose = verbosity > 0; // TODO
			if (!verbose && bsMatch.cardinality() > 10) {
				mainLog.print(".\nThe first 10 states are displayed below. To view them all, enable verbose mode or use a print filter.\n");
				states.print(mainLog, 10);
			} else {
				mainLog.print(":\n");
				states.print(mainLog);
			}
			
		}

		// Store result
		result.setResult(resObj);
		// Set result explanation (if none or disabled, clear)
		if (expr.getExplanationEnabled() && resultExpl != null) {
			result.setExplanation(resultExpl.toLowerCase());
		} else {
			result.setExplanation(null);
		}

		// Clear up
		if (vals != null)
			vals.clear();

		return resVals;
		*/
	}
	
	/**
	 * Model check a P operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionProb(ParamModel model, ExpressionProb expr, BitSet needStates) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		BigRational p = null; // Probability bound (actual value)
		//String relOp; // Relational operator
		//boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) probs
		ModelType modelType = model.getModelType();
		RelOp relOp;
		boolean min = false;

		RegionValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateExact(constantValues);
			if (p.compareTo(0) == -1 || p.compareTo(1) == 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		// Compute probabilities
		if (!expr.getExpression().isSimplePathFormula()) {
			throw new PrismNotSupportedException(mode.Engine() + " does not yet handle LTL-style path formulas");
		}
		probs = checkProbPathFormulaSimple(model, expr.getExpression(), min, needStates);
		probs.clearNotNeeded(needStates);

		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(probs);
		}
		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return probs.binaryOp(Region.getOp(relOp.toString()), p);
		}
	}
	
	private RegionValues checkProbPathFormulaSimple(ParamModel model, Expression expr, boolean min, BitSet needStates) throws PrismException
	{
		boolean negated = false;
		RegionValues probs = null;
		
		expr = Expression.convertSimplePathFormulaToCanonicalForm(expr);
		
		// Negation
		if (expr instanceof ExpressionUnaryOp &&
		    ((ExpressionUnaryOp)expr).getOperator() == ExpressionUnaryOp.NOT) {
			negated = true;
			min = !min;
			expr = ((ExpressionUnaryOp)expr).getOperand();
		}
			
		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			
			// Next
			if (exprTemp.getOperator() == ExpressionTemporal.P_X) {
				throw new PrismNotSupportedException("Next operator not supported by " + mode + " engine");
			}
			// Until
			else if (exprTemp.getOperator() == ExpressionTemporal.P_U) {
				BitSet needStatesInner = new BitSet(model.getNumStates());
				needStatesInner.set(0, model.getNumStates());
				RegionValues b1 = checkExpression(model, exprTemp.getOperand1(), needStatesInner);
				RegionValues b2 = checkExpression(model, exprTemp.getOperand2(), needStatesInner);
				if (exprTemp.hasBounds()) {
					probs = checkProbBoundedUntil(model, b1, b2, min);
				} else {
					probs = checkProbUntil(model, b1, b2, min, needStates);
				}
			}
		}
		
		if (probs == null)
			throw new PrismException("Unrecognised path operator in P operator");

		if (negated) {
			// Subtract from 1 for negation
			probs = probs.binaryOp(new BigRational(1, 1), parserBinaryOpToRegionOp(ExpressionBinaryOp.MINUS));
		}
		
		return probs;
	}

	private RegionValues checkProbUntil(ParamModel model, RegionValues b1, RegionValues b2, boolean min, BitSet needStates) throws PrismException {
		return valueComputer.computeUnbounded(b1, b2, min, null);
	}
		
	private RegionValues checkProbBoundedUntil(ParamModel model, RegionValues b1, RegionValues b2, boolean min) throws PrismException {
		ModelType modelType = model.getModelType();
		//RegionValues probs;
		switch (modelType) {
		case CTMC:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		case DTMC:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		case MDP:
			throw new PrismNotSupportedException("Bounded until operator not supported by " + mode + " engine");
		default:
			throw new PrismNotSupportedException("Cannot model check for a " + modelType);
		}

		//return probs;
	}

	/**
	 * Model check an R operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionReward(ParamModel model, ExpressionReward expr, BitSet needStates) throws PrismException
	{
		Expression rb; // Reward bound (expression)
		BigRational r = null; // Reward bound (actual value)
		RegionValues rews = null;
		boolean min = false;

		// Get info from reward operator
		
		RewardStruct rewStruct = modulesFile.getRewardStruct(expr.getRewardStructIndexByIndexObject(modulesFile.getRewardStructNames(), constantValues));
		RelOp relOp = expr.getRelOp();
		rb = expr.getReward();
		if (rb != null) {
			r = rb.evaluateExact(constantValues);
			if (r.compareTo(0) == -1)
				throw new PrismException("Invalid reward bound " + r + " in R[] formula");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		ParamRewardStruct rew = constructRewards(model, rewStruct, constantValues);
		mainLog.println("Building reward structure...");
		rews = checkRewardFormula(model, rew, expr.getExpression(), min, needStates);
		rews.clearNotNeeded(needStates);

		// Print out probabilities
		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(rews);
		}

		// For =? properties, just return values
		if (rb == null) {
			return rews;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return rews.binaryOp(Region.getOp(relOp.toString()), r);
		}
	}
	
	private RegionValues checkRewardFormula(ParamModel model,
			ParamRewardStruct rew, Expression expr, boolean min, BitSet needStates) throws PrismException {
		RegionValues rewards = null;

		if (expr.getType() instanceof TypePathDouble) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			switch (exprTemp.getOperator()) {
			case ExpressionTemporal.R_S:
				rewards = checkRewardSteady(model, rew, exprTemp, min, needStates);				
				break;
			default:
				throw new PrismNotSupportedException(mode.Engine() + " does not yet handle the " + exprTemp.getOperatorSymbol() + " operator in the R operator");
			}
		} else if (expr.getType() instanceof TypePathBool || expr.getType() instanceof TypeBool) {
			rewards = checkRewardPathFormula(model, rew, expr, min, needStates);
		}

		if (rewards == null) {
			throw new PrismException("Unrecognised operator in R operator");
		}
		
		return rewards;
	}

	/**
	 * Compute rewards for a path formula in a reward operator.
	 */
	private RegionValues checkRewardPathFormula(ParamModel model, ParamRewardStruct rew, Expression expr, boolean min, BitSet needStates) throws PrismException
	{
		if (Expression.isReach(expr)) {
			return checkRewardReach(model, rew, (ExpressionTemporal) expr, min, needStates);
		} else if (Expression.isCoSafeLTLSyntactic(expr, true)) {
			throw new PrismNotSupportedException(mode.Engine() + " does not yet support co-safe reward computation");
		} else {
			throw new PrismException("R operator contains a path formula that is not syntactically co-safe: " + expr);
		}
	}

	private RegionValues checkRewardReach(ParamModel model,
			ParamRewardStruct rew, ExpressionTemporal expr, boolean min, BitSet needStates) throws PrismException {
		RegionValues allTrue = regionFactory.completeCover(true);
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		RegionValues reachSet = checkExpression(model, expr.getOperand2(), needStatesInner);
		return valueComputer.computeUnbounded(allTrue, reachSet, min, rew);
	}
	
	private RegionValues checkRewardSteady(ParamModel model,
			ParamRewardStruct rew, ExpressionTemporal expr, boolean min, BitSet needStates) throws PrismException {
		if (model.getModelType() != ModelType.DTMC && model.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException(mode.Engine() + " long-run average rewards are only supported for DTMCs and CTMCs");
		}
		RegionValues allTrue = regionFactory.completeCover(true);
		BitSet needStatesInner = new BitSet(needStates.size());
		needStatesInner.set(0, needStates.size());
		return valueComputer.computeSteadyState(allTrue, min, rew);
	}

	private ParamRewardStruct constructRewards(ParamModel model, RewardStruct rewStruct, Values constantValues2)
			throws PrismException {
		int numStates = model.getNumStates();
		List<State> statesList = model.getStatesList();
		ParamRewardStruct rewSimple = new ParamRewardStruct(functionFactory, model.getNumChoices());
		int numRewItems = rewStruct.getNumItems();
		for (int rewItem = 0; rewItem < numRewItems; rewItem++) {
			Expression expr = rewStruct.getReward(rewItem);
			expr = (Expression) expr.deepCopy().replaceConstants(constantValues);
			Expression guard = rewStruct.getStates(rewItem);
			String action = rewStruct.getSynch(rewItem);
			boolean isTransitionReward = rewStruct.getRewardStructItem(rewItem).isTransitionReward();
			for (int state = 0; state < numStates; state++) {
				if (isTransitionReward && model.isDeadlockState(state)) {
					// As state is a deadlock state, any outgoing transition
					// was added to "fix" the deadlock and thus does not get a reward.
					// Skip to next state
					continue;
				}
				if (guard.evaluateExact(constantValues, statesList.get(state)).toBoolean()) {
					int[] varMap = new int[statesList.get(0).varValues.length];
					for (int i = 0; i < varMap.length; i++) {
						varMap[i] = i;
					}
					Expression exprState = (Expression) expr.deepCopy().evaluatePartially(statesList.get(state), varMap);
					Function newReward = modelBuilder.expr2function(functionFactory, exprState);
					for (int choice = model.stateBegin(state); choice < model.stateEnd(state); choice++) {
						Function sumOut = model.sumLeaving(choice);
						Function choiceReward;
						if (!isTransitionReward) {
							// for state reward, scale by sumOut
							// For DTMC/MDP, this changes nothing;
							// for CTMC this takes the expected duration
							// in this state into account
							choiceReward = newReward.divide(sumOut);
						} else {
							choiceReward = functionFactory.getZero();
							for (int succ = model.choiceBegin(choice); succ < model.choiceEnd(choice); succ++) {
								String mdpAction = model.getLabel(succ);
								if ((isTransitionReward && (mdpAction == null ? (action.isEmpty()) : mdpAction.equals(action)))) {
									choiceReward = choiceReward.add(newReward.multiply(model.succProb(succ)));
								}
							}
							// does not get scaled by sumOut
						}
						rewSimple.addReward(choice, choiceReward);
					}
				}
			}
		}
		return rewSimple;
	}
	
	/**
	 * Model check an S operator expression and return the values for all states.
	 */
	protected RegionValues checkExpressionSteadyState(ParamModel model, ExpressionSS expr, BitSet needStates) throws PrismException
	{
		Expression pb; // Probability bound (expression)
		BigRational p = null; // Probability bound (actual value)
		//String relOp; // Relational operator
		//boolean min = false; // For nondeterministic models, are we finding min (true) or max (false) probs
		ModelType modelType = model.getModelType();
		RelOp relOp;
		boolean min = false;

		RegionValues probs = null;

		// Get info from prob operator
		relOp = expr.getRelOp();
		pb = expr.getProb();
		if (pb != null) {
			p = pb.evaluateExact(constantValues);
			if (p.compareTo(0) == -1 || p.compareTo(1) == 1)
				throw new PrismException("Invalid probability bound " + p + " in P operator");
		}
		min = relOp.isLowerBound() || relOp.isMin();

		// Compute probabilities
		probs = checkProbSteadyState(model, expr.getExpression(), min, needStates);
		probs.clearNotNeeded(needStates);

		if (verbosity > 5) {
			mainLog.print("\nProbabilities (non-zero only) for all states:\n");
			mainLog.print(probs);
		}
		// For =? properties, just return values
		if (pb == null) {
			return probs;
		}
		// Otherwise, compare against bound to get set of satisfying states
		else {
			return probs.binaryOp(Region.getOp(relOp.toString()), p);
		}
	}

	private RegionValues checkProbSteadyState(ParamModel model, Expression expr, boolean min, BitSet needStates)
	throws PrismException
	{
		BitSet needStatesInner = new BitSet(model.getNumStates());
		needStatesInner.set(0, model.getNumStates());
		RegionValues b = checkExpression(model,expr, needStatesInner);
		if (model.getModelType() != ModelType.DTMC
				&& model.getModelType() != ModelType.CTMC) {
			throw new PrismNotSupportedException(mode.Engine() + " currently only implements steady state for DTMCs and CTMCs.");
		}
		return valueComputer.computeSteadyState(b, min, null);
	}

	/**
	 * Set parameters for parametric analysis.
	 * 
	 * @param paramNames names of parameters
	 * @param lower lower bounds of parameters
	 * @param upper upper bounds of parameters
	 */
	public void setParameters(String[] paramNames, String[] lower, String[] upper) {
		if (paramNames == null || lower == null || upper == null) {
			throw new IllegalArgumentException("all arguments of this functions must be non-null");
		}
		if (paramNames.length != lower.length || lower.length != upper.length) {
			throw new IllegalArgumentException("all arguments of this function must have the same length");
		}
		
		paramLower = new BigRational[lower.length];
		paramUpper = new BigRational[upper.length];
		
		for (int i = 0; i < paramNames.length; i++) {
			if (paramNames[i] == null || lower[i] == null || upper[i] == null)  {
				throw new IllegalArgumentException("all entries in arguments of this function must be non-null");
			}
			paramLower[i] = new BigRational(lower[i]);
			paramUpper[i] = new BigRational(upper[i]);
		}
	}	
			
	public static void closeDown() {
		ComputerThreads.terminate();
	}

	public void setModelBuilder(ModelBuilder builder)
	{
		this.modelBuilder = builder;
	}

	// Steady-state computation

	/**
	 * Compute steady-state probability
	 * @param model The ParamModel
	 * @return Steady-state probabilities as StateValues
	 * @throws PrismException
	 */
	public StateValues doSteadyState(ParamModel model) throws PrismException
	{
		return doSteadyState(model, (explicit.StateValues) null);
	}

	/**
	 * Compute steady-state probability with initial state distribution
	 * @param model The ParamModel
	 * @param initDistFile Initial state distribution file
	 * @return Steady-state probabilities as StateValues
	 * @throws PrismException
	 */
	public StateValues doSteadyState(ParamModel model, File initDistFile) throws PrismException
	{
		explicit.StateValues initDist = readDistributionFromFile(initDistFile, model);
		return doSteadyState(model, initDist);
	}

	/**
	 * Compute steady-state probability with initial state distribution
	 * @param model The ParamModel
	 * @param initDist Initial state distribution as explicit StateValues
	 * @return Steady-state probabilities as StateValues
	 * @throws PrismException
	 */
	public StateValues doSteadyState(ParamModel model, explicit.StateValues initDist) throws PrismException
	{
		explicit.StateValues initDistNew = (initDist == null) ? buildInitialDistribution(model) : initDist;

		return computeSteadyStateProbs(model, initDistNew.getDoubleArray());
	}

	/**
	 * Generate a probability distribution, stored as a explicit StateValues object, from a file.
	 * If {@code distFile} is null, so is the return value.
	 * @param distFile Initial state distribution file
	 * @param model The ParamModel
	 * @return Initial state distribution as explicit StateValues
	 * @throws PrismException
	 */
	public explicit.StateValues readDistributionFromFile(File distFile, ParamModel model) throws PrismException
	{
		explicit.StateValues dist = null;

		if (distFile != null) {
			mainLog.println("\nImporting probability distribution from file \"" + distFile + "\"...");
			dist = explicit.StateValues.createFromFile(TypeDouble.getInstance(), distFile, model);
		}
		return dist;
	}

	/**
	 * Build a probability distribution, stored as a explict StateValues object,
	 * from the initial states info of the current model: either probability 1 for
	 * the (single) initial state or equiprobable over multiple initial states.
	 * @param model The ParamModel
	 * @return Initial state distribution as explicit StateValues
	 */
	private explicit.StateValues buildInitialDistribution(ParamModel model) throws PrismException
	{

		int numInitStates = model.getNumInitialStates();
		if (numInitStates == 1) {
			int sInit = model.getFirstInitialState();
			return explicit.StateValues.create(TypeDouble.getInstance(), s -> s == sInit ? 1.0 : 0.0, model);
		} else {
			double pInit = 1.0 / numInitStates;
			return explicit.StateValues.create(TypeDouble.getInstance(), s -> model.isInitialState(s) ? pInit : 0.0, model);
		}
	}

	/**
	 * Compute steady-state probabilities
	 * @param model The ParamModel
	 * @param initDist Initial state distribution
	 * @return Steady-state probabilities as StateValues
	 * @throws PrismException
	 */
	public StateValues computeSteadyStateProbs(ParamModel model, double initDist[]) throws PrismException
	{
		int numStates = model.getNumStates();


		if(functionFactory == null) functionFactory = model.getFunctionFactory();

		Function[] solnProbs = functionFactory.createFunctionArray(numStates,functionFactory.getZero());

		// compute steady-state probabilities or fetch from cache
		SteadyStateProbs.SteadyStateProbsParam steadyStateProbsBscc;

		if (SteadyStateCache.getInstance().isEnabled()) {
			SteadyStateCache cache = SteadyStateCache.getInstance();
			if (cache.containsSteadyStateProbs(model)) {
				mainLog.println("\nTaking steady-state probabilities from cache.");
				steadyStateProbsBscc = cache.getSteadyStateProbs(model);
			} else {
				mainLog.println("\nComputing steady-state probabilities.");
				steadyStateProbsBscc = SteadyStateProbs.computeCompactParam(this, model);
				mainLog.println("\nCaching steady-state probabilities.");
				cache.storeSteadyStateProbs(model, steadyStateProbsBscc, settings);
			}
		} else {
			steadyStateProbsBscc = SteadyStateProbs.computeSimpleParam(this,model);
		}

		List<BitSet> bsccs = new ArrayList<>();

		// get BSCCs from steadyStateProbsBscc
		for (BitSet bscc : steadyStateProbsBscc.getBSCCs()){
			bsccs.add(bscc);
		}
		BitSet notInBSCCs = steadyStateProbsBscc.getNonBsccStates();
		int numBSCCs = bsccs.size();

		// Compute support of initial distribution
		int numInit = 0;
		BitSet init = new BitSet();
		for (int i = 0; i < numStates; i++) {
			if (initDist[i] > 0)
				init.set(i);
			numInit++;
		}

		// Determine whether initial states are all in the same BSCC
		int initInOneBSCC = -1;
		for (int b = 0; b < numBSCCs; b++) {
			// test subset via setminus
			BitSet notInB = (BitSet) init.clone();
			notInB.andNot(bsccs.get(b));
			if (notInB.isEmpty()) {
				// all init states in b
				// >> finish
				initInOneBSCC = b;
				break;
			} else if (notInB.cardinality() < numInit) {
				// some init states in b and some not
				// >> abort
				break;
			}
			// no init state in b
			// >> try next BSCC
		}

		Function[] bsccProbs = steadyStateProbsBscc.getSteadyStateProbabilities();

		// If all initial states are in the same BSCC, it's easy...
		// Just return steady-state probabilities for the BSCC
		if (initInOneBSCC > -1) {
			mainLog.println("\nInitial states are all in one BSCC (so no reachability probabilities computed)");
			BitSet bscc = bsccs.get(initInOneBSCC);

			for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
				solnProbs[i] =  ((CachedFunctionFactory) functionFactory).checkUnique((CachedFunction) bsccProbs[i]);
			}
		}

		// Otherwise, have to consider all the BSCCs
		else {
			// Compute probability of reaching each BSCC from initial distribution
			Function[] probBSCCs = new Function[numBSCCs];
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing probability of reaching BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				// Compute probabilities
				Function [] reachProbs = computeUntilProbs(model, notInBSCCs, bscc);

				probBSCCs[b] = functionFactory.getZero();

				for (int i = 0; i < numStates; i++) {
					// probBSCC[b] += initDist[i] * reachProb[i]
					Function tmp = functionFactory.fromBigRational(new BigRational(initDist[i]));
					probBSCCs[b] = probBSCCs[b].add(tmp.multiply(reachProbs[i]));
				}
				mainLog.print("\nProbability of reaching BSCC " + (b + 1) + ": " + probBSCCs[b] + "\n");
			}

			// Multiply each BSCC probability with reachebility probability
			for (int b = 0; b < numBSCCs; b++) {
				mainLog.println("\nComputing steady-state probabilities for BSCC " + (b + 1));
				BitSet bscc = bsccs.get(b);
				for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
					solnProbs[i] = ((CachedFunctionFactory) functionFactory).checkUnique((CachedFunction) bsccProbs[i]);
					solnProbs[i] = solnProbs[i].multiply(probBSCCs[b]);
				}
			}
		}

		// solnProbsSV is the result
		StateValues solnProbsSV = new StateValues(solnProbs.length, model.getFirstInitialState(),functionFactory.getZero());
		for (int i = 0; i < solnProbs.length; i++) {
			solnProbsSV.setStateValue(i, solnProbs[i]);
		}
		return solnProbsSV;
	}

	/**
	 * Compute steady-state probabilities for a BSCC
	 * @param model The ParamModel
	 * @param states BitSet of states in BSCC
	 * @return Steady-state probabilities as Function array
	 * @throws PrismException
	 */
	public Function[] computeSteadyStateProbsForBSCC(ParamModel model, BitSet states) throws PrismException
	{

		mainLog.println("Starting gauss elemination ...");
		StopWatch watch = new StopWatch(mainLog).start();

		// construct matrices
		int numOfStates = states.cardinality();

		// transition matrix of markov chain
		Function[][] transitionMatrix = new Function[numOfStates][numOfStates];

		Function[][] identityMatrix = new Function[numOfStates][numOfStates];
		Function[] b = new Function[numOfStates];

		// fill transition matrix

		IterableBitSet bscc = new IterableBitSet(states);

		List<Integer> bsccStates = new ArrayList<Integer>();

		for (PrimitiveIterator.OfInt iter = bscc.iterator(); iter.hasNext(); ) {
			bsccStates.add(iter.nextInt());

		}

		// fill transition matrix with zeros
		for (int k = 0; k < numOfStates; k++) {
			for (int i = 0; i < numOfStates; i++) {
				transitionMatrix[k][i] = functionFactory.getZero();
			}
		}

		//fill in transitions

		int index = 0;
		for (int state : bsccStates) {
			Iterator<Map.Entry<Integer, Function>> iterator = model.getTransitionsIterator(state, 0);
			while (iterator.hasNext()) {
				Map.Entry<Integer, Function> entry = iterator.next();
				int a = bsccStates.indexOf(entry.getKey());
				transitionMatrix[index][a] = transitionMatrix[index][a].add(entry.getValue());
			}
			index++;
		}

		// fill identityMatrix

		for (int k = 0; k < numOfStates; k++) {
			for (int i = 0; i < numOfStates; i++) {
				if (i == k)
					identityMatrix[k][i] = functionFactory.getOne();
				else
					identityMatrix[k][i] = functionFactory.getZero();
			}
		}

		// transitionMatrix - identityMatrix

		for (int k = 0; k < numOfStates; k++) {
			for (int i = 0; i < numOfStates; i++) {
				transitionMatrix[k][i] = transitionMatrix[k][i].subtract(identityMatrix[k][i]);
			}
		}

		// transpose transition matrix

		for (int k = 0; k < numOfStates; k++) {
			for (int i = k + 1; i < numOfStates; i++) {
				Function tmp = transitionMatrix[i][k];
				transitionMatrix[i][k] = transitionMatrix[k][i];
				transitionMatrix[k][i] = tmp;
			}
		}

		// when model is ctmc, then multiply columns with exit rate of corresponding state

		if (model.getModelType() == ModelType.CTMC) {
			int k = 0;
			for (int state : bsccStates) {
				for (int i = 0; i < transitionMatrix.length; i++) {
					transitionMatrix[i][k] = transitionMatrix[i][k].multiply(model.sumLeaving(state));
				}
				k++;
			}
		}

		// Change the first equation to:
		// x1 + ... + xk = 1 |for k = num of states

		// fill up vector b with all zero exept b0
		// b0 = 1

		b = functionFactory.createFunctionArray(numOfStates,functionFactory.getZero());
		b[0] = functionFactory.getOne();

		// change first row of transition to 1
		transitionMatrix[0] = functionFactory.createFunctionArray(numOfStates,functionFactory.getOne());

		// solve linear system
		GaussElemination gaussElemination = new GaussElemination();
		Function[] result = gaussElemination.solve(transitionMatrix, b, functionFactory);

		watch.stop();
		mainLog.println("Gauss elemination finished succesfully in " + watch.elapsedSeconds() + " seconds.");

		return result;
	}

	/**
	 * Compute reachability/until probabilities.
	 * i.e. compute the min/max probability of reaching a state in {@code target},
	 * while remaining in those in {@code remain}.
	 * @param model The ParamModel
	 * @param remain Remain in these states (optional: null means "all")
	 * @param target Target states
	 * @param initDist Distribution of init states
	 * @return Reach probability of target
	 * @throws PrismException
	 */
	public Function [] computeUntilProbs(ParamModel model, BitSet remain, BitSet target) throws PrismException
	{
		Function[] reachProb = new Function[model.getNumStates()];
		// Check for deadlocks in non-target state (because breaks e.g. prob1)
		// TODO: implement checkForDeadlocks
		//model.checkForDeadlocks(target);

		// Start probabilistic reachability
		StopWatch watch = new StopWatch(mainLog).start();
		mainLog.println("\nStarting probabilistic reachability...");

		BitSet no, yes;
		int n, numYes, numNo;
		n = model.getNumStates();

		// Precomputation
		if (precomp && prob0) {
			no = prob0(model, remain, target);
		} else {
			no = new BitSet();
		}

		if (precomp && prob1) {
			yes = prob1(model, remain, target);

		} else {
			yes = (BitSet) target.clone();
		}

		// Print results of precomputation
		numYes = yes.cardinality();
		numNo = no.cardinality();
		mainLog.println("target=" + target.cardinality() + ", yes=" + numYes + ", no=" + numNo + ", maybe=" + (n - (numYes + numNo)));

		// Compute probabilities (if needed)
		if (numYes + numNo < n) {
			constraintChecker = new ConstraintChecker(numRandomPoints);
			for (int i = 0; i < model.getNumStates(); i++) {
				// compute reach prob for every init state

				if (yes.get(i)) {
					reachProb[i] = functionFactory.getOne();
				} else if (no.get(i)) {
					reachProb[i] = functionFactory.getZero();
				} else {
					regionFactory = new BoxRegionFactory(functionFactory, constraintChecker, precision, model.getNumStates(), i, simplifyRegions, splitMethod);
					valueComputer = new ValueComputer(this, mode, model, regionFactory, precision, eliminationOrder, bisimType);

					RegionValues b1 = new RegionValues(regionFactory);
					RegionValues b2 = new RegionValues(regionFactory);
					StateValues st1 = new StateValues(model.getNumStates(), i, true);
					BoxRegion br = new BoxRegion((BoxRegionFactory) regionFactory, paramLower, paramUpper);
					b1.add(br, st1);

					StateValues st2 = new StateValues(model.getNumStates(), i, false);

					IterableBitSet iterator = new IterableBitSet(target);

					for (PrimitiveIterator.OfInt iter = iterator.iterator(); iter.hasNext(); ) {
						st2.setStateValue(iter.nextInt(), true);
					}
					b2.add(br, st2);

					RegionValues regionValues = valueComputer.computeUnbounded(b1, b2, false, null);
					StateValues stateValues = regionValues.getResult(0);

					reachProb[i] = stateValues.getStateValueAsFunction(i);
				}

			}
		} else {
			double[] tmp = Utils.bitsetToDoubleArray(yes, n);
			for (int i = 0; i < model.getNumStates(); i++) {
				if(yes.get(i)){
					reachProb[i] = functionFactory.getOne();
				} else {
					reachProb[i] = functionFactory.getZero();
				}
			}
		}

		// Finished probabilistic reachability
		watch.stop();
		mainLog.println("Probabilistic reachability took " + watch.elapsedSeconds() + " seconds.");

		return reachProb;
	}

	/**
	 * Prob0 precomputation algorithm (using a fixed-point computation),
	 * i.e. determine the states of a ParamModel which, with probability 0,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param model The ParamModel
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob0(ParamModel model, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, soln, unknown;
		boolean u_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();

		mainLog.println("Starting Prob0...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			soln = new BitSet(model.getNumStates());
			soln.set(0, model.getNumStates());
			return soln;
		}

		// Initialise vectors
		n = model.getNumStates();
		u = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Fixed point loop
		iters = 0;
		u_done = false;
		// Least fixed point - should start from 0 but we optimise by
		// starting from 'target', thus bypassing first iteration
		u.or(target);
		soln.or(target);
		while (!u_done) {
			iters++;
			// Single step of Prob0
			model.prob0step(unknown, u, true, soln);

			// Check termination
			u_done = soln.equals(u);
			// u = soln
			u.clear();
			u.or(soln);
		}

		// Negate
		u.flip(0, n);

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;

		mainLog.print("Prob0");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

	/**
	 * Prob1 precomputation algorithm (using a fixed-point computation)
	 * i.e. determine the states of a ParamModel which, with probability 1,
	 * reach a state in {@code target}, while remaining in those in {@code remain}.
	 * @param model The ParamModel
	 * @param remain Remain in these states (optional: {@code null} means "all")
	 * @param target Target states
	 */
	public BitSet prob1(ParamModel model, BitSet remain, BitSet target)
	{
		int n, iters;
		BitSet u, v, soln, unknown;
		boolean u_done, v_done;
		long timer;

		// Start precomputation
		timer = System.currentTimeMillis();

		mainLog.println("Starting Prob1...");

		// Special case: no target states
		if (target.cardinality() == 0) {
			return new BitSet(model.getNumStates());
		}

		// Initialise vectors
		n = model.getNumStates();
		u = new BitSet(n);
		v = new BitSet(n);
		soln = new BitSet(n);

		// Determine set of states actually need to perform computation for
		unknown = new BitSet();
		unknown.set(0, n);
		unknown.andNot(target);
		if (remain != null)
			unknown.and(remain);

		// Nested fixed point loop
		iters = 0;
		u_done = false;
		// Greatest fixed point
		u.set(0, n);
		while (!u_done) {
			v_done = false;
			// Least fixed point - should start from 0 but we optimise by
			// starting from 'target', thus bypassing first iteration
			v.clear();
			v.or(target);
			soln.clear();
			soln.or(target);
			while (!v_done) {
				iters++;
				// Single step of Prob1
				model.prob1step(unknown, u, v, true, soln);
				// Check termination (inner)
				v_done = soln.equals(v);
				// v = soln
				v.clear();
				v.or(soln);
			}
			// Check termination (outer)
			u_done = v.equals(u);
			// u = v
			u.clear();
			u.or(v);
		}

		// Finished precomputation
		timer = System.currentTimeMillis() - timer;

		mainLog.print("Prob1");
		mainLog.println(" took " + iters + " iterations and " + timer / 1000.0 + " seconds.");

		return u;
	}

}
