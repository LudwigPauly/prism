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
import explicit.DiracDistribution;
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
public class DTMCAlteredDistributions extends DTMCView
{
	private DTMC<Double> model;
	private IntFunction<Iterator<Entry<Integer, Double>>> mapping;

	private Predicate<Entry<Integer, Double>> nonZero;
	
	/**
	 * If {@code mapping} returns {@code null} for a state, the original transitions are preserved.
	 *
	 * @param model a DTMC
	 * @param mapping from states to (new) distributions or null
	 */
	public DTMCAlteredDistributions(final DTMC<Double> model, final IntFunction<Iterator<Entry<Integer, Double>>> mapping)
	{
		this.model = model;
		this.mapping = mapping;
		nonZero = (Entry<Integer, Double> e) -> { return e.getValue() > 0.0; };
	}

	public DTMCAlteredDistributions(final DTMCAlteredDistributions altered)
	{
		super(altered);
		model = altered.model;
		mapping = altered.mapping;
		nonZero = (Entry<Integer, Double> e) -> { return e.getValue() > 0.0; };
	}



	//--- Cloneable ---

	@Override
	public DTMCAlteredDistributions clone()
	{
		return new DTMCAlteredDistributions(this);
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
		return super.hasLabel(name) ? super.getLabelStates(name) : model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return new UnionSet<>(super.getLabels(), model.getLabels());
	}

	@Override
	public boolean hasLabel(String name)
	{
		return super.hasLabel(name) || model.hasLabel(name);
	}

	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		final Iterator<Entry<Integer, Double>> transitions = mapping.apply(state);
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

	public static DTMCAlteredDistributions fixDeadlocks(final DTMC<Double> model)
	{
		final BitSet deadlockStates = BitSetTools.asBitSet(model.getDeadlockStates().iterator());
		final DTMCAlteredDistributions fixed = trapStates(model, deadlockStates);
		fixed.deadlockStates = deadlockStates;
		fixed.fixedDeadlocks = true;
		return fixed;
	}

	@Deprecated
	public static DTMCAlteredDistributions addSelfLoops(final DTMC model, final BitSet states)
	{
		return trapStates(model, states);
	}

	public static DTMCAlteredDistributions deadlockStates(final DTMC model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Double>>> deadlocks = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (states.get(state)) {
					Entry<Integer,Double> transition = new AbstractMap.SimpleImmutableEntry<>(state, 1.0);
					return new SingletonIterator.Of<>(transition);
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions(model, deadlocks);
	}

	public static DTMCAlteredDistributions trapStates(final DTMC model, final BitSet states)
	{
		final IntFunction<Iterator<Entry<Integer, Double>>> traps = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (states.get(state)) {
					return DiracDistribution.iterator(state);
				}
				return null;
			}
		};
		return new DTMCAlteredDistributions(model, traps);
	}

	@Deprecated
	public static DTMC<Double> identifyStates(final DTMC<Double> model, final Iterable<BitSet> equivalenceClasses)
	{
		final EquivalenceRelationInteger identify = new EquivalenceRelationInteger(equivalenceClasses);
		final BitSet representatives = identify.getRepresentatives(model.getNumStates());

		// 1. attach all transitions of an equivalence class to its representative
		final IntFunction<Iterator<Entry<Integer, Double>>> reattach = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (! identify.isRepresentative(state)) {
					return EmptyIterator.of();
				}
				final BitSet equivalenceClass = identify.getEquivalenceClassOrNull(state);
				if (equivalenceClass == null) {
					return null;
				}
				final FunctionalIterable<Iterator<Entry<Integer, Double>>> transitionIterators =
						new IterableBitSet(equivalenceClass).map((int s) -> model.getTransitionsIterator(s));
				return new ChainedIterator.Of<>(transitionIterators.iterator()).distinct();
			}
		};
		final DTMC<Double> reattached = new DTMCAlteredDistributions(model, reattach);

		// 2. redirect transitions to representatives
		final Function<Entry<Integer, Double>, Entry<Integer, Double>> redirectTransition = new Function<Entry<Integer, Double>, Entry<Integer, Double>>()
		{
			@Override
			public final Entry<Integer, Double> apply(final Entry<Integer, Double> transition)
			{
				final int target = transition.getKey();
				if (identify.isRepresentative(target)) {
					return transition;
				}
				final int representative = identify.getRepresentative(target);
				final double probability = transition.getValue();
				return new AbstractMap.SimpleImmutableEntry<>(representative, probability);
			}
		};
		final IntFunction<Iterator<Entry<Integer, Double>>> redirectDistribution = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(final int state)
			{
				if (reattached.allSuccessorsInSet(state, representatives)) {
					return null;
				}
				final Iterator<Entry<Integer, Double>> transitions = reattached.getTransitionsIterator(state);
				final Iterator<Entry<Integer, Double>> redirected = new MappingIterator.ObjToObj<>(transitions, redirectTransition);
				// use Distribution to dedupe successors
				return new Distribution<>(redirected, model.getEvaluator()).iterator();
			}
		};
		final DTMC<Double> redirected = new DTMCAlteredDistributions(reattached, redirectDistribution);

		// 3. drop equivalence classes except for the representatives
		return new DTMCRestricted(redirected, representatives, Restriction.STRICT);
	}

}