package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.IteratorTools;
import common.iterable.EmptyIterator;
import common.iterable.FilteringIterable;
import common.iterable.IterableInt;
import common.iterable.IterableStateSet;
import common.iterable.MappingIterable;
import common.iterable.MappingIterator;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.DTMC;
import explicit.DTMCSimple;

public class DTMCRestricted extends DTMCView
{
	private DTMC model;
	// FIXME ALG: consider using a predicate instead
	private BitSet states;
	private Restriction restriction;
	// FIXME ALG: consider using a mapping function instead
	private int[] mappingToOriginalModel;
	private Integer[] mappingToRestrictedModel;



	public DTMCRestricted(final DTMC model, final Iterable<Integer> states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final Iterable<Integer> states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(states), restriction);
	}

	public DTMCRestricted(final DTMC model, final BitSet states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final IntPredicate states)
	{
		this(model, states, Restriction.TRANSITIVE_CLOSURE);
	}

	public DTMCRestricted(final DTMC model, final IntPredicate states, final Restriction restriction)
	{
		this(model, BitSetTools.asBitSet(new IterableStateSet(states, model.getNumStates())), restriction);
	}

	public DTMCRestricted(final DTMC model, final BitSet include, final Restriction restriction)
	{
		assert include.length() <= model.getNumStates();

		this.model = model;
		this.restriction = restriction;
		this.states = restriction.getStateSet(model, include);

		mappingToRestrictedModel = new Integer[model.getNumStates()];
		mappingToOriginalModel = new int[states.cardinality()];
		for (int state = states.nextSetBit(0), index = 0; state >= 0; state = states.nextSetBit(state+1)) {
			mappingToRestrictedModel[state] = index;
			mappingToOriginalModel[index] = state;
			index++;
		}
	}

	public DTMCRestricted(final DTMCRestricted restricted)
	{
		super(restricted);
		model = restricted.model;
		states = restricted.states;
		restriction = restricted.restriction;
		mappingToOriginalModel = restricted.mappingToOriginalModel;
		mappingToRestrictedModel = restricted.mappingToRestrictedModel;
	}



	//--- Cloneable ---

	@Override
	public DTMCRestricted clone()
	{
		return new DTMCRestricted(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return states.cardinality();
	}

	@Override
	public int getNumInitialStates()
	{
		return IteratorTools.count(getInitialStates());
	}

	@Override
	public IterableInt getInitialStates()
	{
		final FilteringIterable<Integer> initialStates = new FilteringIterable.Of<>(model.getInitialStates(), states::get);
		return new MappingIterable.ToInt<>(initialStates, this::mapStateToRestrictedModel);
	}

	@Override
	public int getFirstInitialState()
	{
		final Iterator<Integer> initials = getInitialStates().iterator();
		return initials.hasNext() ? initials.next() : -1;
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return model.isInitialState(mapStateToOriginalModel(state));
	}

	@Override
	public List<State> getStatesList()
	{
		final List<State> originalStates = model.getStatesList();
		if (originalStates == null) {
			return null;
		}
		final List<State> states = new ArrayList<State>(getNumStates());
		for (int state = 0, numStates = getNumStates(); state < numStates; state++) {
			states.add(originalStates.get(mappingToOriginalModel[state]));
		}
		return states;
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
	public BitSet getLabelStates(String name)
	{
		final BitSet labelStates = model.getLabelStates(name);
		return (labelStates == null) ? null : mapStatesToRestrictedModel(labelStates);
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

	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		if (restriction == Restriction.STRICT) {
			return super.getSuccessorsIterator(state);
		}
		return model.getSuccessorsIterator(mapStateToOriginalModel(state));
	}



	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		final int originalState = mapStateToOriginalModel(state);
		if (restriction == Restriction.STRICT && ! allSuccessorsIncluded(originalState)) {
			return EmptyIterator.Of();
		}
		return new MappingIterator.From<>(model.getTransitionsIterator(originalState), this::mapTransitionToRestrictedModel);
	}

	private boolean allSuccessorsIncluded(final int originalState)
	{
		// FIXME ALG: consider memoizing
		return model.allSuccessorsInSet(originalState, states);
	}



	//--- DTMCView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = DTMCAlteredDistributions.fixDeadlocks(this.clone());
		int numStates = model.getNumStates();
		states = new BitSet(numStates);
		states.set(0, numStates);
		restriction = Restriction.TRANSITIVE_CLOSURE_SAFE;
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[numStates];
		mappingToRestrictedModel = new Integer[numStates];
		for (int state = 0; state < numStates; state++) {
			mappingToOriginalModel[state] = state;
			mappingToRestrictedModel[state] = state;
		}
	}



	//--- instance methods ---

	public int mapStateToOriginalModel(final int state)
	{
		return mappingToOriginalModel[state];
	}

	public BitSet mapStatesToOriginalModel(final BitSet restrictedStates)
	{
		Objects.requireNonNull(restrictedStates);

		final int length = restrictedStates.length();
		if (length == 0){
			return new BitSet();
		}
		final BitSet originalStates = new BitSet(mappingToOriginalModel[length-1]+1);
		for (int restrictedState : new IterableStateSet(restrictedStates, mappingToOriginalModel.length)) {
			originalStates.set(mappingToOriginalModel[restrictedState]);
		}
		return originalStates;
	}

	public Integer mapStateToRestrictedModel(final int state)
	{
		return mappingToRestrictedModel[state];
	}

	public BitSet mapStatesToRestrictedModel(final BitSet originalStates)
	{
		Objects.requireNonNull(originalStates);

		final int length = originalStates.length();
		if (length == 0){
			return new BitSet();
		}
		//XXX: consider allocating a BitSet in a suited size
		final BitSet mappedStates = new BitSet();
		for (int originalState : new IterableStateSet(originalStates, model.getNumStates())) {
			final Integer state = mappingToRestrictedModel[originalState];
			if (state != null) {
				mappedStates.set(state);
			}
		}
		return mappedStates;
	}

	public Entry<Integer, Double> mapTransitionToRestrictedModel(final Entry<Integer, Double> transition)
	{
		final Integer state = mapStateToRestrictedModel(transition.getKey());
		final Double probability = transition.getValue();
		return new AbstractMap.SimpleImmutableEntry<>(state, probability);
	}



	//--- static methods ---

	public static void main(final String[] args) throws PrismException
	{
		final DTMCSimple original = new DTMCSimple(4);
		original.addInitialState(1);
		original.setProbability(0, 1, 0.1);
		original.setProbability(0, 2, 0.9);
		original.setProbability(1, 2, 0.2);
		original.setProbability(1, 3, 0.8);
		original.setProbability(2, 1, 0.3);
		original.setProbability(2, 2, 0.7);
		original.findDeadlocks(false);

		DTMCRestricted restricted;

		System.out.println("Original Model:");
		System.out.print(original.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original.getDeadlockStates()));
		System.out.println(original);

		System.out.println();

		final BitSet include = new BitSet();
		include.set(1);

		System.out.println("Restricted Model " + include + " without Reachability:");
		restricted = new DTMCRestricted(original, include, Restriction.STRICT);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		BitSet restrictedStates = new BitSet();
		restrictedStates.set(0);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		BitSet originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));

		System.out.println();

		System.out.println("Restricted Model " + include + " with Reachability:");
		restricted = new DTMCRestricted(original, include);
		restricted.findDeadlocks(true);
		System.out.print(restricted.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(restricted.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(restricted.getDeadlockStates()));
		System.out.println(restricted);
		restrictedStates = new BitSet();
		restrictedStates.set(1);
		System.out.println("original states to " + restrictedStates + ": " + restricted.mapStatesToOriginalModel(restrictedStates));
		originalStates = new BitSet();
		originalStates.set(0);
		originalStates.set(1);
		originalStates.set(3);
		System.out.println("restricted states to " + originalStates + ": " + restricted.mapStatesToRestrictedModel(originalStates));
	}
}