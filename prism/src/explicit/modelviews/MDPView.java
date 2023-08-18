//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit.modelviews;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import common.IterableStateSet;
import common.iterable.*;
import explicit.NondetModel;
import prism.*;
import strat.MDStrategy;
import explicit.DTMCFromMDPAndMDStrategy;
import explicit.Distribution;
import explicit.IncomingChoiceRelationSparseCombined;
import explicit.MDP;
import explicit.Model;
import explicit.SuccessorsIterator;
import strat.MDStrategy;
import explicit.PredecessorRelation;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;

/**
 * Base class for an MDP view, i.e., a virtual MDP that is obtained
 * by mapping from some other model on-the-fly.
 * <br>
 * The main job of sub-classes is to provide an appropriate
 * getTransitionsIterator() method. Several other methods, providing
 * meta-data on the model have to be provided as well. For examples,
 * see the sub-classes contained in this package.
 */
public abstract class MDPView<Value> extends ModelView<Value> implements MDP<Value>, Cloneable
{
	public MDPView()
	{
		super();
	}

	public MDPView(final MDPView<Value> model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		Evaluator <Value> evaluator = getEvaluator();
		String s = "[ ";
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (state > 0)
				s += ", ";
			s += state + ": ";
			s += "[";
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (choice > 0)
					s += ",";
				final Object action = getAction(state, choice);
				if (action != null)
					s += action + ":";
				s += new Distribution<Value>(getTransitionsIterator(state, choice), evaluator);
			}
			s += "]";
		}
		s += " ]";
		return s;
	}



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	@Override
	public int getNumTransitions()
	{
		// FIXME ALG: use sum abstraction ?
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++)
				numTransitions += getNumTransitions(state, choice);
		}
		return numTransitions;
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog out, int precision)
	{
		final int numStates = getNumStates();
		// Output transitions to .tra file
		out.print(numStates + " " + getNumChoices() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Value> sorted = new TreeMap<>();
		for (int state = 0; state < numStates; state++) {
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					final Entry<Integer, Value> trans = transitions.next();
					sorted.put(trans.getKey(), trans.getValue());
				}
				// Print out (sorted) transitions
				for (Entry<Integer, Value> e : sorted.entrySet()) {
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.print(state + " " + choice + " " + e.getKey() + " " + e.getValue().toString());
					final Object action = getAction(state, choice);
					out.print(action == null ? "\n" : (" " + action + "\n"));
				}
				sorted.clear();
			}
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		try (FileWriter out = new FileWriter(filename)) {
			Evaluator <Value> evaluator = getEvaluator();
			// Output transitions to PRISM language file
			out.write(getModelType().keyword() + "\n");
			final int numStates = getNumStates();
			out.write("module M\nx : [0.." + (numStates - 1) + "];\n");
			final TreeMap<Integer, Value> sorted = new TreeMap<Integer, Value>();
			for (int state = 0; state < numStates; state++) {
				for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
					// Extract transitions and sort by destination state index (to match PRISM-exported files)
					for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
						final Entry<Integer, Value> trans = transitions.next();
						sorted.put(trans.getKey(), trans.getValue());
					}
					// Print out (sorted) transitions
					final Object action = getAction(state, choice);
					out.write(action != null ? ("[" + action + "]") : "[]");
					out.write("x=" + state + "->");
					boolean first = true;
					for (Entry<Integer, Value> e : sorted.entrySet()) {
						if (first)
							first = false;
						else
							out.write("+");
						// Note use of PrismUtils.formatDouble to match PRISM-exported files
						out.write(PrismUtils.formatDouble(evaluator.toDouble(e.getValue())) + ":(x'=" + e.getKey() + ")");
					}
					out.write(";\n");
					sorted.clear();
				}
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		final int numStates = getNumStates();
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		s += ", " + getNumChoices() + " choices";
		s += ", dist max/avg = " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates);
		return s;
	}

	@Override
	public String infoStringTable()
	{
		final int numStates = getNumStates();
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		s += "Choices:     " + getNumChoices() + "\n";
		s += "Max/avg:     " + getMaxNumChoices() + "/" + PrismUtils.formatDouble2dp(((double) getNumChoices()) / numStates) + "\n";
		return s;
	}

	@Override
	public IncomingChoiceRelationSparseCombined getPredecessorRelation(PrismComponent parent, boolean storeIfNew)
	{
		if (predecessorRelation != null) {
			return (IncomingChoiceRelationSparseCombined) predecessorRelation;
		}
		// TODO reconsider line below
		final PredecessorRelation pre = IncomingChoiceRelationSparseCombined.forModel(parent, (Model<?>) this);

		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return (IncomingChoiceRelationSparseCombined) pre;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		final int numChoices = getNumChoices(state);
		@SuppressWarnings("unchecked")
		final Iterator<Integer>[] successorIterators = new Iterator[numChoices];
		for (int choice = 0; choice < numChoices; choice++) {
			successorIterators[choice] = getSuccessorsIterator(state, choice);
		}

		return new ChainedIterator.Of<>(Arrays.stream(successorIterators).iterator()).distinct();
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices()
	{
		// FIXME ALG: use sum abstraction ?
		int numChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numChoices += getNumChoices(state);
		}
		return numChoices;
	}

	@Override
	public int getMaxNumChoices()
	{
		// FIXME ALG: use some abstraction IteratorTools.max ?
		int maxNumChoices = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			maxNumChoices = Math.max(maxNumChoices, getNumChoices(state));
		}
		return maxNumChoices;
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		final HashSet<Object> actions = new HashSet<Object>();
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			final int numChoices = getNumChoices(state);
			if (numChoices <= 1) {
				continue;
			}
			actions.clear();
			for (int choice = 0; choice < numChoices; choice++) {
				if (!actions.add(getAction(state, choice))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return (int) Reducible.extend(getTransitionsIterator(state, choice)).count();
	}

	@Override
	public boolean allSuccessorsInSet(final int state, final int choice, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state, choice); successors.hasNext();) {
			if (!set.get(successors.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean someSuccessorsInSet(final int state, final int choice, final BitSet set)
	{
		for (Iterator<Integer> successors = getSuccessorsIterator(state, choice); successors.hasNext();) {
			if (set.get(successors.next())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice);
		return new MappingIterator.ObjToInt<>(transitions, Entry::getKey);
	}

	@Override
	public void exportToDotFileWithStrat(final PrismLog out, final BitSet mark, final int[] strat, int precision)
	{
		out.print("digraph " + getModelType() + " {\nsize=\"8,5\"\nnode [shape=box];\n");
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			if (mark != null && mark.get(state))
				out.print(state + " [style=filled  fillcolor=\"#cccccc\"]\n");
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				final String style = (strat[state] == choice) ? ",color=\"#ff0000\",fontcolor=\"#ff0000\"" : "";
				final Object action = getAction(state, choice);
				final String nij = "n" + state + "_" + choice;
				out.print(state + " -> " + nij + " [ arrowhead=none,label=\"" + choice);
				if (action != null) {
					out.print(":" + action);
				}
				out.print("\"" + style + " ];\n");
				out.print(nij + " [ shape=point,height=0.1,label=\"\"" + style + " ];\n");
				for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
					Entry<Integer, Value> trans = transitions.next();
					out.print(nij + " -> " + trans.getKey() + " [ label=\"" + trans.getValue() + "\"" + style + " ];\n");
				}
			}
		}
		out.print("}\n");
	}



	//--- MDP ---

	@Override
	public void prob0step(final BitSet subset, final BitSet u, final boolean forall, final BitSet result)
	{
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				value = someSuccessorsInSet(state, choice, u);
				if (value != forall) {
					break;
				}
			}
			result.set(state, value);
		}
	}

	@Override
	public void prob1Astep(final BitSet subset, final BitSet u, final BitSet v, final BitSet result)
	{
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = true;
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (!(allSuccessorsInSet(state, choice, u) && someSuccessorsInSet(state, choice, v))) {
					value = false;
					break;
				}
			}
			result.set(state, value);
		}
	}

	@Override
	public void prob1Estep(final BitSet subset, final BitSet u, final BitSet v, final BitSet result, final int strat[])
	{
		int stratCh = -1;
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = false;
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				if (allSuccessorsInSet(state, choice, u) && someSuccessorsInSet(state, choice, v)) {
					value = true;
					// If strategy generation is enabled, remember optimal choice
					if (strat != null) {
						stratCh = choice;
					}
					break;
				}
			}
			// If strategy generation is enabled, store optimal choice
			// (only if this the first time we add the state to S^yes)
			if (strat != null && value && !result.get(state)) {
				strat[state] = stratCh;
			}
			// Store result
			result.set(state, value);
		}
	}

	public void prob1step(final BitSet subset, final BitSet u, final BitSet v, final boolean forall, final BitSet result)
	{
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean value = forall; // there exists or for all
			for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
				value = someSuccessorsInSet(state, choice, v) && allSuccessorsInSet(state, choice, u);
				if (value != forall) {
					break;
				}
			}
			result.set(state, value);
		}
	}

	public boolean prob1stepSingle(final int state, final int choice, final BitSet u, final BitSet v)
	{
		return someSuccessorsInSet(state, choice, v) && allSuccessorsInSet(state, choice, u);
	}

	public void mvMultMinMax(final double[] vect, final boolean min, final double[] result, final BitSet subset, final boolean complement, final int[] strat)
	{
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultMinMaxSingle(state, vect, min, strat);
		}
	}

	public void mvMultMinMax(final double[] vect, final boolean min, final double[] result, final IterableStateSet subset, final int[] strat)
	{
		for (FunctionalPrimitiveIterator.OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultMinMaxSingle(state, vect, min, strat);
		}
	}

	public Value mvMultMinMaxSingle(final int state, final Value[] vect, final boolean min, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		int stratCh = -1;
		Value minmax = evaluator.zero();
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			Value d = evaluator.zero();
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
			}

			// Check whether we have exceeded min/max so far
			if (first || (min && evaluator.gt(minmax, d)) || (!min && evaluator.gt(d , minmax))) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || evaluator.gt(minmax , vect[state])) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	public List<Integer> mvMultMinMaxSingleChoices(final int state, final Value vect[], final boolean min, final Value val)
	{
		Evaluator <Value> evaluator = getEvaluator();
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();
		// One row of matrix-vector operation
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			Value d = evaluator.zero();
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
			}
			// Store strategy info if value matches
			if (evaluator.equals(val, d)) {
				result.add(choice);
			}
		}

		return result;
	}

	public Value mvMultSingle(final int state, final int choice, final Value[] vect)
	{
		Evaluator <Value> evaluator = getEvaluator();
		// Compute sum for this distribution
		Value d = evaluator.zero();
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Value> trans = transitions.next();
			final int target = trans.getKey();
			final Value probability = trans.getValue();
			d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
		}

		return d;
	}

	@Override
	public double mvMultGSMinMax(final double[] vect, final boolean min, final BitSet subset, final boolean complement, final boolean absolute,
			final int[] strat)
	{
		double maxDiff = 0.0;

		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultJacMinMaxSingle(state, vect, min, strat);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacMinMaxSingle(s, vect, min, strat);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	public double mvMultGSMinMax(final double[] vect, final boolean min, IterableStateSet subset, final boolean absolute,
			final int[] strat)
	{
		double maxDiff = 0.0;

		for (FunctionalPrimitiveIterator.OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final double d = mvMultJacMinMaxSingle(state, vect, min, strat);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacMinMaxSingle(s, vect, min, strat);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	public Value mvMultJacMinMaxSingle(final int state, final Value[] vect, final boolean min, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		int stratCh = -1;
		Value minmax = evaluator.zero();
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			Value diag = evaluator.one();
			// Compute sum for this distribution
			Value d = evaluator.zero();
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				if (target != state) {
					d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
				} else {
					diag = evaluator.subtract(diag, probability);
				}
			}
			if (evaluator.gt(diag , evaluator.zero()))
				d = evaluator.divide(d, diag);
			// Check whether we have exceeded min/max so far
			if (first || (min && evaluator.gt(minmax, d) || (!min && evaluator.gt(d, minmax)))) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || evaluator.gt(minmax, vect[state])) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	public Value mvMultJacSingle(final int state, final int choice, final Value[] vect)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value diag = evaluator.one();
		// Compute sum for this distribution
		Value d = evaluator.zero();
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Value> trans = transitions.next();
			final int target = trans.getKey();
			final Value probability = trans.getValue();
			if (target != state) {
				d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
			} else {
				diag = evaluator.subtract(diag, probability);
			}
		}
		if (evaluator.gt(diag, evaluator.zero()))
			d = evaluator.divide(d, diag);

		return d;
	}

	@Override
	public void mvMultRewMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final double[] result, final BitSet subset,
			final boolean complement, final int[] strat)
	{
		for (FunctionalPrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewMinMaxSingle(state, vect, mdpRewards, min, strat);
		}
	}

	public void mvMultRewMinMax(final double[] vect, final MDPRewards mdpRewards, final boolean min, final double[] result, final IterableStateSet subset,
			final int[] strat)
	{
		for (FunctionalPrimitiveIterator.OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewMinMaxSingle(state, vect, mdpRewards, min, strat);
		}
	}


	public Value mvMultRewMinMaxSingle(final int state, final Value[] vect, final MDPRewards<Value> mdpRewards, final boolean min, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		int stratCh = -1;
		Value minmax = evaluator.zero();
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			Value d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
			}
			// Check whether we have exceeded min/max so far
			if (first || (min && evaluator.gt(minmax, d)) || (!min && evaluator.gt(d, minmax))) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null)
					stratCh = choice;
			}
			first = false;
		}
		// Add state reward (doesn't affect min/max)
		minmax = evaluator.add(mdpRewards.getStateReward(state), minmax);
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || evaluator.gt(minmax, vect[state])) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}

	public Value mvMultRewSingle(final int state, final int choice, final Value[] vect, final MCRewards <Value> mcRewards)
	{
		Evaluator <Value> evaluator = getEvaluator();
		// Compute sum for this distribution
		// TODO: use transition rewards when added to DTMCss
		// d = mcRewards.getTransitionReward(s);
		Value d = evaluator.zero();
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
			final Entry<Integer, Value> trans = transitions.next();
			final int target = trans.getKey();
			final Value probability = trans.getValue();
			d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
		}
		d = evaluator.add(mcRewards.getStateReward(state), d);

		return d;
	}

	public Value mvMultRewGSMinMax(final Value[] vect, final MDPRewards <Value> mdpRewards, final boolean min, final BitSet subset, final boolean complement,
			final boolean absolute, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value maxDiff = evaluator.zero();

		for (PrimitiveIterator.OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final Value d = mvMultRewJacMinMaxSingle(state, vect, mdpRewards, min, strat);
			Value diff = absolute ? (evaluator.absolute(evaluator.subtract(d , vect[state]))) : (evaluator.absolute(evaluator.divide(evaluator.subtract(d, vect[state]), d)));
			maxDiff = evaluator.gt(diff, maxDiff) ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	@Override
	public SuccessorsIterator getSuccessors(final int state, final int choice)
	{
		final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}

	@Override
	public Model<Value> constructInducedModel(final MDStrategy<Value> strat)
	{
		return new DTMCFromMDPAndMDStrategy<>(this, strat);
	}

	public Value mvMultRewGSMinMax(final Value[] vect, final MDPRewards <Value> mdpRewards, final boolean min, final IterableStateSet subset,
			final boolean absolute, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value maxDiff = evaluator.zero();

		for (FunctionalPrimitiveIterator.OfInt states = subset.iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final Value d = mvMultRewJacMinMaxSingle(state, vect, mdpRewards, min, strat);
			Value diff = absolute ? (evaluator.absolute(evaluator.subtract(d , vect[state]))) : (evaluator.absolute(evaluator.divide(evaluator.subtract(d, vect[state]), d)));
			maxDiff = evaluator.gt(diff, maxDiff) ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultRewJacMinMaxSingle(s, vect, mdpRewards, min);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}


	public Value mvMultRewJacMinMaxSingle(final int state, final Value[] vect, final MDPRewards <Value> mdpRewards, final boolean min, final int[] strat)
	{
		Evaluator <Value> evaluator = getEvaluator();
		int stratCh = -1;
		Value minmax = evaluator.zero();
		boolean first = true;

		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			Value diag = evaluator.one();
			// Compute sum for this distribution
			Value d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value prob = trans.getValue();
				if (target != state) {
					d = evaluator.add(d, evaluator.multiply(prob, vect[target]));
				} else {
					diag = evaluator.subtract(diag, prob);
				}
			}
			if (evaluator.gt(diag,evaluator.zero()))
				d = evaluator.divide(d, diag);
			// Check whether we have exceeded min/max so far
			if (first || (min && evaluator.gt(minmax,d)) || (!min && evaluator.gt(d, minmax))) {
				minmax = d;
				// If strategy generation is enabled, remember optimal choice
				if (strat != null) {
					stratCh = choice;
				}
			}
			first = false;
		}
		// Add state reward (doesn't affect min/max)
		minmax = evaluator.add(minmax, mdpRewards.getStateReward(state));
		// If strategy generation is enabled, store optimal choice
		if (strat != null && !first) {
			// For max, only remember strictly better choices
			if (min) {
				strat[state] = stratCh;
			} else if (strat[state] == -1 || evaluator.gt(minmax, vect[state])) {
				strat[state] = stratCh;
			}
		}

		return minmax;
	}


	public List<Integer> mvMultRewMinMaxSingleChoices(final int state, final Value[] vect, final MDPRewards <Value> mdpRewards, final boolean min, final Value val)
	{
		Evaluator <Value> evaluator = getEvaluator();
		// Create data structures to store strategy
		final List<Integer> result = new ArrayList<Integer>();

		// One row of matrix-vector operation
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			// Compute sum for this distribution
			Value d = mdpRewards.getTransitionReward(state, choice);
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
			}
			d = evaluator.add(d, mdpRewards.getStateReward(state));
			// Store strategy info if value matches
			//if (PrismUtils.doublesAreClose(val, d, termCritParam, termCrit == TermCrit.ABSOLUTE)) {
			if (evaluator.equals(val,d)) {
				result.add(choice);
				//res.add(distrs.getAction());
			}
		}

		return result;
	}

	public void mvMultRight(final int[] states, final int[] strat, final Value[] source, final Value[] dest)
	{
		Evaluator <Value> evaluator = getEvaluator();
		for (int state : states) {
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, strat[state]); transitions.hasNext();) {
				final Entry<Integer, Value> trans = transitions.next();
				final int target = trans.getKey();
				final Value probability = trans.getValue();
				dest[target] = evaluator.add(dest[target], evaluator.multiply(probability, source[state]));
			}
		}
	}

	//--- ModelView ---

	/**
	 * @see explicit.MDPExplicit#exportTransitionsToDotFile(int, PrismLog, Iterable, int)  MDPExplicit
	 **/
	@Override
	protected void exportTransitionsToDotFile(int state, PrismLog out)
	{
		for (int choice = 0, numChoices = getNumChoices(state); choice < numChoices; choice++) {
			final Object action = getAction(state, choice);
			final String nij = "n" + state + "_" + choice;
			out.print(state + " -> " + nij + " [ arrowhead=none,label=\"" + choice);
			if (action != null)
				out.print(":" + action);
			out.print("\" ];\n");
			out.print(nij + " [ shape=point,width=0.1,height=0.1,label=\"\" ];\n");
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state, choice); transitions.hasNext();) {
				Entry<Integer, Value> trans = transitions.next();
				out.print(nij + " -> " + trans.getKey() + " [ label=\"" + trans.getValue() + "\" ];\n");
			}
		}
	}
}