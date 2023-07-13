//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package simulator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import common.IterableBitSet;
import common.iterable.FunctionalPrimitiveIterator;
import parser.State;
import parser.VarList;
import parser.ast.Command;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.Update;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;

/**
 * A mutable implementation of {@link simulator.Choice},
 * i.e, a representation of a single (nondeterministic) choice in a PRISM model,
 * in the form of a list of transitions, each specified by updates to variables.
 */
public class ChoiceListFlexi<Value> implements Choice<Value>
{
	// Evaluator for values/states
	protected Evaluator<Value> eval;
	
	// Module/action info, encoded as an integer.
	// For an independent (non-synchronous) choice, this is -i,
	// where i is the 1-indexed module index.
	// For a synchronous choice, this is the 1-indexed action index.
	protected int moduleOrActionIndex;

	// List of multiple updates and associated probabilities/rates
	// Probabilities/rates are already (partially) evaluated,
	// target states are just stored as lists of updates (for efficiency)
	protected List<List<Update>> updates;
	protected List<Value> probability;
	
	// For real-time models, the clock guard,
	// i.e., an expression over clock variables
	// denoting when it can be taken.
	protected Expression clockGuard;

	/**
	 * Create empty choice.
	 */
	public ChoiceListFlexi(Evaluator<Value> eval)
	{
		// Store evaluator
		this.eval = eval;
		// Initialise
		updates = new ArrayList<List<Update>>();
		probability = new ArrayList<Value>();
		clockGuard = null;
	}

	/**
	 * Copy constructor.
	 * NB: Does a shallow, not deep, copy with respect to references to probability/update objects.
	 */
	public ChoiceListFlexi(ChoiceListFlexi<Value> ch)
	{
		eval = ch.eval;
		moduleOrActionIndex = ch.moduleOrActionIndex;
		updates = new ArrayList<List<Update>>(ch.updates.size());
		for (List<Update> list : ch.updates) {
			List<Update> listNew = new ArrayList<Update>(list.size()); 
			updates.add(listNew);
			for (Update up : list) {
				listNew.add(up);
			}
		}
		probability = new ArrayList<Value>(ch.size());
		for (Value p : ch.probability) {
			probability.add(p);
		}
		clockGuard = ch.clockGuard;
	}

	// Set methods

	/**
	 * Set the module/action for this choice, encoded as an integer
	 * (-i for independent in ith module, i for synchronous on ith action)
	 * (in both cases, modules/actions are 1-indexed)
	 */
	public void setModuleOrActionIndex(int moduleOrActionIndex)
	{
		this.moduleOrActionIndex = moduleOrActionIndex;
	}

	/**
	 * Set the clock guard
	 */
	public void setClockGuard(Expression clockGuard)
	{
		this.clockGuard = clockGuard;
	}

	/**
	 * Add a transition to this choice.
	 * @param probability Probability (or rate) of the transition
	 * @param ups List of Update objects defining transition
	 */
	public void add(Value probability, List<Update> ups)
	{
		this.updates.add(ups);
		this.probability.add(probability);
	}

	@Override
	public void scaleProbabilitiesBy(Value d)
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			probability.set(i, eval.multiply(probability.get(i), d));
		}
	}

	/**
	 * Modify this choice, constructing product of it with another.
	 */
	public void productWith(final ChoiceListFlexi<Value> other) throws PrismLangException
	{
		final List<List<Update>> oldUpdates = updates;
		final List<Value> oldProbability = probability;
		updates = new ArrayList<List<Update>>(updates.size() * other.updates.size());
		probability = new ArrayList<Value>(updates.size() * other.updates.size());

		// cross product over updates indices
		for (int i = oldUpdates.size() - 1; i >= 0; i--) {
			for (int j = other.updates.size() - 1; j >= 0; j--) {
				try {
					final List<Update> joined = joinUpdates(oldUpdates.get(i), other.updates.get(j));
					add(eval.multiply(oldProbability.get(i), other.probability.get(j)), joined);
				} catch (PrismLangException e) {
					updates = oldUpdates;
					probability = oldProbability;
					throw e;

				}
			}
		}
		if (other.clockGuard != null) {
			clockGuard = (clockGuard == null) ? other.clockGuard : Expression.And(clockGuard, other.clockGuard);
		}
	}

	private List<Update> joinUpdates(final List<Update> updatesA, final List<Update> updatesB)
			throws PrismLangException
	{
		final ArrayList<Update> joined = new ArrayList<Update>(updatesA.size() + updatesB.size());
		joined.addAll(updatesA);
		joined.addAll(updatesB);

		return resolveConflicts(joined);
	}

	private List<Update> resolveConflicts(final List<Update> updates) throws PrismLangException
	{
		BitSet conflicts = getWriteConflicts(updates);
		@SuppressWarnings("unchecked")
		List<Update>[] conflictingUpdates = new List[conflicts.length()];
		List<Update> resolvedUpdates = new ArrayList<Update>();

		// check for each update whether it writes a conflicting variable
		for (Update update : updates) {
			Update resolved = update;
			for (FunctionalPrimitiveIterator.OfInt vars = new IterableBitSet(update.getWrittenVariables()).iterator(); vars.hasNext();) {
				int var = vars.nextInt();
				if (conflicts.get(var)) {
					// create split update in conflicting and non-conflicting writes
					if (resolved == update) {
						resolved = (Update) update.deepCopy();
						resolved.setParent(update.getParent());
					}
					if (conflictingUpdates[var] == null) {
						conflictingUpdates[var] = new ArrayList<>();
					}
					conflictingUpdates[var].add(resolved.split(var));
				}
			}
			resolvedUpdates.add(resolved);
		}
		// create cumulative updates from conflicting writes
		for (int var=0, size=conflictingUpdates.length; var<size; var++) {
			if (conflictingUpdates[var] != null) {
				resolvedUpdates.add(cumulateUpdatesForVariable(conflictingUpdates[var], var));
			}
		}
		return resolvedUpdates;
	}

	private BitSet getWriteConflicts(final List<Update> updates)
	{
		final BitSet allWritten = new BitSet();
		final BitSet allConflicts = new BitSet();
		for (Update update : updates) {
			BitSet written = update.getWrittenVariables();
			BitSet conflicts = (BitSet) written.clone();
			conflicts.and(allWritten);
			allConflicts.or(conflicts);
			allWritten.or(written);
		}
		return allConflicts;
	}

	private Update cumulateUpdatesForVariable(final List<Update> updates, final int variable) throws PrismLangException

	{
		assert updates.size() > 0 : "at least one update expected";

		Update joinedUpdate = null;
		for (Update update : updates) {
			try {
				joinedUpdate = cumulateUpdatesForVariable(joinedUpdate, update, variable);
			} catch (PrismLangException e) {
				e.printStackTrace();
				final ExpressionIdent varIdent = update.getVarIdentFromIndex(variable);
				final String message = "non-cumulative conflicting updates on shared variable in synchronous transition";
				String action = update.getParent().getParent().getSynch();
				action = "".equals(action) ? "" : " [" + action + "]";
				throw new PrismLangException(message + action, varIdent);
			}
		}
		return joinedUpdate;
	}


	private Update cumulateUpdatesForVariable(final Update updateA, final Update updateB, final int variable) throws PrismLangException
	{
		return updateA == null ? updateB : updateA.cummulateUpdatesForVariable(updateB, variable);
	}

	// Get methods

	@Override
	public int getModuleOrActionIndex()
	{
		return moduleOrActionIndex;
	}

	@Override
	public String getModuleOrAction()
	{
		// Action label (or absence of) will be the same for all updates in a choice
		Update u = updates.get(0).get(0);
		Command c = u.getParent().getParent();
		if ("".equals(c.getSynch()))
			return c.getParent().getName();
		else
			return "[" + c.getSynch() + "]";
	}

	@Override
	public Expression getClockGuard()
	{
		return clockGuard;
	}
	
	@Override
	public int size()
	{
		return probability.size();
	}

	@Override
	public String getUpdateString(int i, State currentState) throws PrismLangException
	{
		int j, n;
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			n = up.getNumElements();
			for (j = 0; j < n; j++) {
				if (first)
					first = false;
				else
					s += ", ";
				s += up.getVar(j) + "'=" + up.getExpression(j).evaluate(currentState);
			}
		}
		return s;
	}

	@Override
	public String getUpdateStringFull(int i)
	{
		String s = "";
		boolean first = true;
		for (Update up : updates.get(i)) {
			if (up.getNumElements() == 0)
				continue;
			if (first)
				first = false;
			else
				s += " & ";
			s += up;
		}
		return s;
	}

	@Override
	public State computeTarget(int i, State currentState, VarList varList) throws PrismLangException
	{
		State newState = new State(currentState);
		for (Update up : updates.get(i))
			up.update(currentState, newState, eval.exact(), varList);
		return newState;
	}

	@Override
	public void computeTarget(int i, State currentState, State newState, VarList varList) throws PrismLangException
	{
		for (Update up : updates.get(i))
			up.update(currentState, newState, eval.exact(), varList);
	}

	@Override
	public Value getProbability(int i)
	{
		return probability.get(i);
	}

	@Override
	public Value getProbabilitySum()
	{
		Value sum = eval.zero();
		for (Value p : probability) {
			sum = eval.add(sum, p);
		}
		return sum;
	}

	@Override
	public int getIndexByProbabilitySum(Value x)
	{
		Value d = eval.zero();
		int n = size();
		int i;
		for (i = 0; eval.geq(x, d) && i < n; i++) {
			d = eval.add(d, probability.get(i));
		}
		return i - 1;
	}
	
	@Override
	public void checkValid(ModelType modelType) throws PrismException
	{
		// Currently nothing to do here:
		// Checks for bad probabilities/rates done earlier.
	}
	
	@Override
	public void checkForErrors(State currentState, VarList varList) throws PrismException
	{
		int i, n;
		n = size();
		for (i = 0; i < n; i++) {
			for (Update up : updates.get(i))
				up.checkUpdate(currentState, varList);
		}
	}
	
	@Override
	public String toString()
	{
		int i, n;
		boolean first = true;
		String s = "";
		if (clockGuard != null) {
			s += "(" + clockGuard + ")";
		}
		n = size();
		for (i = 0; i < n; i++) {
			if (first)
				first = false;
			else
				s += " + ";
			s += getProbability(i) + ":" + updates.get(i);
		}
		return s;
	}
}
