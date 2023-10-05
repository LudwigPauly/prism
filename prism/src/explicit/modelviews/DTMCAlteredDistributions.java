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

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import common.BitSetTools;
import common.IterableBitSet;
import common.iterable.*;
import explicit.DTMCSimple;
import explicit.Distribution;
import parser.State;
import parser.Values;
import parser.VarList;
import explicit.DTMC;
import prism.Evaluator;
import prism.PrismException;
import prism.PrismLog;

/**
 * A view of a DTMC where for selected states the transitions are changed.
 * <br>
 * The new transitions are given by a function (int state) -> Iterator<Entry<Integer, Value>,
 * i.e., providing an iterator over the outgoing transitions. A return value of {@code null}
 * is interpreted as "keep the original transitions".
 */
public class DTMCAlteredDistributions<Value> extends DTMCView<Value>
{
	private DTMC<Value> model;
	private IntFunction<Iterator<Entry<Integer, Value>>> mapping;

	private Predicate<Entry<Integer, Value>> nonZero;
	
	/**
	 * If {@code mapping} returns {@code null} for a state, the original transitions are preserved.
	 *
	 * @param model a DTMC
	 * @param mapping from states to (new) distributions or null
	 */
	public DTMCAlteredDistributions(final DTMC<Value> model, final IntFunction<Iterator<Entry<Integer, Value>>> mapping)
	{
		this.model = model;
		this.mapping = mapping;
		Evaluator<Value> evaluator = getEvaluator();
		nonZero = (Entry<Integer, Value> e) -> { return evaluator.gt(e.getValue(), evaluator.zero()); };
	}

	public DTMCAlteredDistributions(final DTMCAlteredDistributions<Value> altered)
	{
		super(altered);
		model = altered.model;
		mapping = altered.mapping;
		Evaluator <Value> evaluator = getEvaluator();
		nonZero = (Entry<Integer, Value> e) -> { return evaluator.gt(e.getValue(), evaluator.zero()); };
	}



	//--- Cloneable ---

	@Override
	public DTMCAlteredDistributions<Value> clone()
	{
		return new DTMCAlteredDistributions<>(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return model.getStatesList();
	}

	@Override
	public VarList getVarList()
	{
		return model.getVarList();
	}

	@Override
	public Values getConstantValues()
	{
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(final String name)
	{
		return model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return model.getLabels();
	}

	@Override
	public boolean hasLabel(String name)
	{
		return model.hasLabel(name);
	}

	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Value>> transitions = mapping.apply(state);
		if (transitions == null) {
			return model.getTransitionsIterator(state);
		}
		return Reducible.extend(transitions).filter(nonZero);
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = fixDeadlocks(this.clone());
		mapping = state -> null;
	}



	//--- static methods ---

	public static <Value> DTMCAlteredDistributions<Value> fixDeadlocks(final DTMC<Value> model)
	{
		final BitSet deadlockStates = new BitSet();
		model.getDeadlockStates().forEach(deadlockStates::set);
		final DTMCAlteredDistributions<Value> fixed = addSelfLoops(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	/**
	 * Return a view where the outgoing transitions for all states in the given set
	 * are replaced by probability 1 self-loops.
	 */
	public static <Value> DTMCAlteredDistributions<Value> addSelfLoops(final DTMC<Value> model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Value>>> addLoops = new IntFunction<Iterator<Entry<Integer, Value>>>()
		{
			@Override
			public Iterator<Entry<Integer, Value>> apply(final int state)
			{
				if (states.get(state)) {
					Entry<Integer,Value> transition = new AbstractMap.SimpleImmutableEntry<>(state, model.getEvaluator().one());
					return new SingletonIterator.Of<>(transition);
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions<>(model, addLoops);
	}

	public static DTMCAlteredDistributions deadlockStates(final DTMC model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Double>>> deadlocks = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (states.get(state)) {
					return Collections.emptyIterator();
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions(model, deadlocks);
	}

	@Deprecated
	public static<Value> DTMC<Value> identifyStates(final DTMC<Value> model, final Iterable<BitSet> equivalenceClasses)
	{
		final EquivalenceRelationInteger identify = new EquivalenceRelationInteger(equivalenceClasses);
		final BitSet representatives = identify.getRepresentatives(model.getNumStates());

		// 1. attach all transitions of an equivalence class to its representative
		final IntFunction<Iterator<Entry<Integer, Value>>> reattach = new IntFunction<Iterator<Entry<Integer, Value>>>()
		{
			@Override
			public Iterator<Entry<Integer, Value>> apply(final int state)
			{
				if (! identify.isRepresentative(state)) {
					return EmptyIterator.of();
				}
				final BitSet equivalenceClass = identify.getEquivalenceClassOrNull(state);
				if (equivalenceClass == null) {
					return null;
				}
				final FunctionalIterable<Iterator<Entry<Integer, Value>>> transitionIterators =
						new IterableBitSet(equivalenceClass).map((int s) -> model.getTransitionsIterator(s));
				return new ChainedIterator.Of<>(transitionIterators.iterator()).distinct();
			}
		};
		final DTMC<Value> reattached = new DTMCAlteredDistributions<>(model, reattach);

		// 2. redirect transitions to representatives
		final Function<Entry<Integer, Value>, Entry<Integer, Value>> redirectTransition = new Function<Entry<Integer, Value>, Entry<Integer, Value>>()
		{
			@Override
			public final Entry<Integer, Value> apply(final Entry<Integer, Value> transition)
			{
				final int target = transition.getKey();
				if (identify.isRepresentative(target)) {
					return transition;
				}
				final int representative = identify.getRepresentative(target);
				final Value probability = transition.getValue();
				return new AbstractMap.SimpleImmutableEntry<>(representative, probability);
			}
		};
		final IntFunction<Iterator<Entry<Integer, Value>>> redirectDistribution = new IntFunction<Iterator<Entry<Integer, Value>>>()
		{
			@Override
			public Iterator<Entry<Integer, Value>> apply(final int state)
			{
				if (reattached.allSuccessorsInSet(state, representatives)) {
					return null;
				}
				final Iterator<Entry<Integer, Value>> transitions = reattached.getTransitionsIterator(state);
				final Iterator<Entry<Integer, Value>> redirected = new MappingIterator.ObjToObj<>(transitions, redirectTransition);
				// use Distribution to dedupe successors
				return new Distribution<>(redirected, model.getEvaluator()).iterator();
			}
		};
		final DTMC<Value> redirected = new DTMCAlteredDistributions<>(reattached, redirectDistribution);

		// 3. drop equivalence classes except for the representatives
		return new DTMCRestricted<>(redirected, representatives, Restriction.STRICT);
	}

}