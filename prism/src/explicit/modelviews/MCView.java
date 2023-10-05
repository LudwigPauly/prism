package explicit.modelviews;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntFunction;

import common.IterableStateSet;
import common.iterable.FunctionalIterator;
import common.iterable.MappingIterator;
import common.iterable.Reducible;
import explicit.DTMC;
import explicit.DTMCExplicit;
import explicit.Distribution;
import explicit.SuccessorsIterator;
import explicit.rewards.MCRewards;
import prism.*;

public abstract class MCView<Value> extends ModelView<Value> implements DTMC<Value>, Cloneable
{
	public MCView()
	{
		super();
	}

	public MCView(final ModelView<Value> model)
	{
		super(model);
	}



	//--- Object ---

	@Override
	public String toString()
	{
		final IntFunction<Entry<Integer, Distribution<Value>>> getDistribution = new IntFunction<Entry<Integer, Distribution<Value>>>()
		{
			@Override
			public final Entry<Integer, Distribution<Value>> apply(final int state)
			{
				final Distribution<Value> distribution = new Distribution<>(getTransitionsIterator(state),getEvaluator());
				return new AbstractMap.SimpleImmutableEntry<>(state, distribution);
			}
		};
		String s = "trans: [ ";
		final IterableStateSet states = new IterableStateSet(getNumStates());
		final Iterator<Entry<Integer, Distribution<Value>>> distributions = states.iterator().map(getDistribution);
		while (distributions.hasNext()) {
			final Entry<Integer, Distribution<Value>> dist = distributions.next();
			s += dist.getKey() + ": " + dist.getValue();
			if (distributions.hasNext()) {
				s += ", ";
			}
		}
		return s + " ]";
	}



	//--- Model ---

	@Override
	public int getNumTransitions()
	{
		// FIXME ALG: use sum abstraction ?
		int numTransitions = 0;
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			numTransitions += getNumTransitions(state);
		}
		return numTransitions;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state);
		return new MappingIterator.ObjToInt<>(transitions, Entry::getKey);
	}

	@Override
	public void exportToPrismExplicitTra(final PrismLog log)
	{
		// Output transitions to .tra file
		Evaluator <Value> evaluator = getEvaluator();
		log.print(getNumStates() + " " + getNumTransitions() + "\n");
		final TreeMap<Integer, Value> sorted = new TreeMap<>();
		for (int state = 0, max = getNumStates(); state < max; state++) {
			// Extract transitions and sort by destination state index (to match PRISM-exported files)
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Value> transition = transitions.next();
				sorted.put(transition.getKey(), transition.getValue());
			}
			// Print out (sorted) transitions
			for (Entry<Integer, Value> transition : sorted.entrySet()) {
				// Note use of PrismUtils.formatDouble to match PRISM-exported files
				log.print(state + " " + transition.getKey() + " " + evaluator.toStringPrism(transition.getValue()) + "\n");
			}
			sorted.clear();
		}
	}

	@Override
	public void exportToPrismLanguage(final String filename) throws PrismException
	{
		Evaluator <Value> evaluator = getEvaluator();
		try (FileWriter out = new FileWriter(filename)) {
			out.write(getModelType().keyword() + "\n");
			out.write("module M\nx : [0.." + (getNumStates() - 1) + "];\n");
			final TreeMap<Integer, Value> sorted = new TreeMap<Integer, Value>();
			for (int state = 0, max = getNumStates(); state < max; state++) {
				// Extract transitions and sort by destination state index (to match PRISM-exported files)
				for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
					final Entry<Integer, Value> transition = transitions.next();
					sorted.put(transition.getKey(), transition.getValue());
				}
				// Print out (sorted) transitions
				out.write("[]x=" + state + "->");
				boolean first = true;
				for (Entry<Integer, Value> transition : sorted.entrySet()) {
					if (first)
						first = false;
					else
						out.write("+");
					// Note use of PrismUtils.formatDouble to match PRISM-exported files
					out.write(evaluator.toStringPrism(transition.getValue()) + ":(x'=" + transition.getKey() + ")");
				}
				out.write(";\n");
				sorted.clear();
			}
			out.write("endmodule\n");
		} catch (IOException e) {
			throw new PrismException("Could not export " + getModelType() + " to file \"" + filename + "\"" + e);
		}
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += getNumStates() + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions";
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + getNumStates() + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	



	//--- DTMC ---

	@Override
	public int getNumTransitions(final int state)
	{
		return (int) Reducible.extend(getTransitionsIterator(state)).count();
	}

	@Override
	public Iterator<Entry<Integer, Pair<Value, Object>>> getTransitionsAndActionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state);
		return new MappingIterator.ObjToObj<>(transitions, transition -> DTMC.attachAction(transition, null));
	}

	@Override
	public void prob0step(final BitSet subset, final BitSet u, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean hasTransitionToU = false;
			for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
				if (u.get(successors.next())) {
					hasTransitionToU = true;
					break;
				}
			}
			result.set(state, hasTransitionToU);
		}
	}

	@Override
	public void prob1step(final BitSet subset, final BitSet u, final BitSet v, final BitSet result)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates()).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			boolean allTransitionsToU = true;
			boolean hasTransitionToV = false;
			for (Iterator<Integer> successors = getSuccessorsIterator(state); successors.hasNext();) {
				final int successor = successors.next();
				if (!u.get(successor)) {
					allTransitionsToU = false;
					break;
				}
				hasTransitionToV = hasTransitionToV || v.get(successor);
			}
			result.set(state, allTransitionsToU && hasTransitionToV);
		}
	}

	public void mvMult(final Value[] vect, final Value[] result, final BitSet subset, final boolean complement)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultSingle(state, vect);
		}
	}

	public Value mvMultSingle(final int state, final Value[] vect)
	{
		Evaluator<Value> evaluator = getEvaluator();
		Value d = evaluator.zero();
		for (final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Value> transition = transitions.next();
			final int target = transition.getKey();
			final Value probability = transition.getValue();
			d = evaluator.add(d,evaluator.multiply(probability, vect[target]));
		}
		return d;
	}

	public Value mvMultGS(final Value[] vect, final BitSet subset, final boolean complement, final boolean absolute)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value maxDiff = evaluator.zero();
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			final Value d = mvMultJacSingle(state, vect);
			final Value diff = absolute ? evaluator.absolute(evaluator.subtract(d,vect[state])) : evaluator.divide(evaluator.absolute(evaluator.subtract(d, vect[state])) , d);
			maxDiff = evaluator.gt(diff, maxDiff) ? diff : maxDiff;
			vect[state] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (int state : new IterableStateSet(subset, getNumStates(), complement).backwards()) {
			final double d = mvMultJacSingle(state, vect);
			final double diff = absolute ? (Math.abs(d - vect[state])) : (Math.abs(d - vect[state]) / d);
			maxDiff = diff > maxDiff ? diff : maxDiff;
			vect[state] = d;
		}*/
		return maxDiff;
	}

	public Value mvMultGS(Value vect[], IterableStateSet subset, boolean absolute)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value maxDiff = evaluator.zero();
		for (OfInt states = subset.iterator(); states.hasNext();) {
			int s = states.nextInt();
			Value d = mvMultJacSingle(s, vect);
			Value diff = absolute ? (evaluator.absolute(evaluator.subtract(d,vect[s]))) : evaluator.divide(evaluator.absolute(evaluator.subtract(d, vect[s])),d);
			maxDiff = evaluator.gt(diff, maxDiff) ? diff : maxDiff;
			vect[s] = d;
		}
		// Use this code instead for backwards Gauss-Seidel
		/*for (s = numStates - 1; s >= 0; s--) {
			if (subset.get(s)) {
				d = mvMultJacSingle(s, vect);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}*/
		return maxDiff;
	}

	public Value mvMultJacSingle(final int state, final Value[] vect)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value diag = evaluator.one();
		Value d = evaluator.zero();
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Value> transition = transitions.next();
			final int target = transition.getKey();
			final Value probability = transition.getValue();
			if (target != state) {
				d = evaluator.add(d, evaluator.multiply(probability,vect[target]));
			} else {
				diag = evaluator.subtract(diag,probability);
			}
		}
		if (evaluator.gt(diag, evaluator.zero())) {
			d = evaluator.divide(d, diag);
		}
		return d;
	}

	@Override
	public void mvMultRew(final double[] vect, final MCRewards mcRewards, final double[] result, final BitSet subset, final boolean complement)
	{
		for (OfInt states = new IterableStateSet(subset, getNumStates(), complement).iterator(); states.hasNext();) {
			final int state = states.nextInt();
			result[state] = mvMultRewSingle(state, vect, mcRewards);
		}
	}

	public Value mvMultRewSingle(final int state, final Value[] vect, final MCRewards<Value> mcRewards)
	{
		Evaluator <Value> evaluator = getEvaluator();
		Value d = mcRewards.getStateReward(state);
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Value> transition = transitions.next();
			int target = transition.getKey();
			Value probability = transition.getValue();
			d = evaluator.add(d, evaluator.multiply(probability, vect[target]));
		}
		return d;
	}

	public void vmMult(final Value[] vect, final Value[] result)
	{
		Evaluator <Value> evaluator = getEvaluator();
		// Initialise result to 0
		Arrays.fill(result, evaluator.zero());
		// Go through matrix elements (by row)
		for (int state = 0, max = getNumStates(); state < max; state++) {
			for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
				final Entry<Integer, Value> transition = transitions.next();
				final int target = transition.getKey();
				final Value prob = transition.getValue();
				result[target] = evaluator.add(result[target], evaluator.multiply(prob,vect[state]));
			}
		}
	}



	//--- ModelView ---

	/**
	 * @see explicit.DTMCExplicit#exportTransitionsToDotFile(int, PrismLog) DTMCExplicit
	 **/
	@Override
	protected void exportTransitionsToDotFile(final int state, final PrismLog out)
	{
		for (Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state); transitions.hasNext();) {
			final Entry<Integer, Value> transition = transitions.next();
			out.print(state + " -> " + transition.getKey() + " [ label=\"");
			out.print(transition.getValue() + "\" ];\n");
		}
	}

	@Override
	public SuccessorsIterator getSuccessors(final int state)
	{
		final Iterator<Entry<Integer, Value>> transitions = getTransitionsIterator(state);

		return SuccessorsIterator.from(new PrimitiveIterator.OfInt() {
			public boolean hasNext() {return transitions.hasNext();}
			public int nextInt() {return transitions.next().getKey();}
		}, true);
	}
}