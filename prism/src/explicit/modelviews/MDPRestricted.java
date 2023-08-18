package explicit.modelviews;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.IterableBitSet;
import common.IterableStateSet;
import common.iterable.FunctionalIterable;
import common.iterable.FunctionalPrimitiveIterable;
import common.iterable.FunctionalPrimitiveIterator.OfInt;
import common.iterable.MappingIterator;
import common.iterable.Reducible;
import common.iterable.UnionSet;
import explicit.BasicModelTransformation;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.ModelTransformation;
import explicit.ReachabilityComputer;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPRestricted<Value> extends MDPView<Value>
{
	private static final Restriction STANDARD_RESTRICTION = Restriction.TRANSITIVE_CLOSURE;
	protected MDP model;
	// FIXME ALG: consider not storing state set at all
	protected BitSet states;
	protected int numStates;
	protected Restriction restriction;
	// FIXME ALG: consider using a mapping function instead
	protected int[] mappingToOriginalModel;
	protected int[] mappingToRestrictedModel;
	protected BitSet redirectTransitions;



	public MDPRestricted(final MDP model, final BitSet states)
	{
		this(model, states, STANDARD_RESTRICTION);
	}

	public MDPRestricted(final MDP model, final BitSet include, final Restriction restriction)
	{
		assert include.length() <= model.getNumStates();

		this.model = model;
		this.restriction = restriction;
		this.states = restriction.getStateSet(model, include);
		numStates = states.cardinality();

		mappingToOriginalModel = new int[numStates];
		mappingToRestrictedModel = new int[model.getNumStates()];
		Arrays.fill(mappingToRestrictedModel, ModelTransformation.UNDEF);
		int firstModified = 0;
		for (int state = states.nextSetBit(0), index = 0; state >= 0; state = states.nextSetBit(state+1)) {
			mappingToRestrictedModel[state] = index;
			mappingToOriginalModel[index] = state;
			index++;
			if (state < index) {
				firstModified = index;
			}
		}
		redirectTransitions = new BitSet(numStates);
		redirectTransitions.set(firstModified, model.getNumStates());
		redirectTransitions = new ReachabilityComputer(model).computePre(redirectTransitions);
	}

	public MDPRestricted(final MDPRestricted restricted)
	{
		super(restricted);
		model = restricted.model;
		states = restricted.states;
		numStates = restricted.numStates;
		restriction = restricted.restriction;
		mappingToOriginalModel = restricted.mappingToOriginalModel;
		mappingToRestrictedModel = restricted.mappingToRestrictedModel;
		redirectTransitions = restricted.redirectTransitions;
	}



	//--- Cloneable ---

	@Override
	public MDPRestricted clone()
	{
		return new MDPRestricted(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return numStates;
	}

	@Override
	public int getNumInitialStates()
	{
		return (int) getInitialStates().count();
	}

	@Override
	public FunctionalPrimitiveIterable.OfInt getInitialStates()
	{
		FunctionalIterable<Integer> initialStates = Reducible.extend(model.getInitialStates());
		return initialStates.filter(states::get).mapToInt(this::mapStateToRestrictedModel);
	}

	@Override
	public int getFirstInitialState()
	{
		final OfInt initials = getInitialStates().iterator();
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
			states.add(originalStates.get(mapStateToOriginalModel(state)));
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
		if (super.hasLabel(name)) {
			return super.getLabelStates(name);
		}
		if (model.hasLabel(name)) {
			return mapStatesToRestrictedModel(model.getLabelStates(name));
		}
		return null;
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

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		if (restriction == Restriction.STRICT) {
			return super.getSuccessorsIterator(state);
		}
		int originalState = mapStateToOriginalModel(state);
		Iterator<Integer> successors = model.getSuccessorsIterator(originalState);
		if (redirectTransitions.get(originalState)) {
			successors = new MappingIterator.ObjToObj<>(successors, this::mapStateToRestrictedModel);
		}
		return successors;
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		final int originalState = mapStateToOriginalModel(state);
		if (restriction != Restriction.STRICT) {
			return model.getNumChoices(originalState);
		}
		// FIXME ALG: consider caching
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(originalState); originalChoice < numOriginalChoices; originalChoice++) {
			if (model.allSuccessorsInSet(originalState, originalChoice, states)) {
				countChoices++;
			}
		}
		return countChoices;
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		return model.getAction(mapStateToOriginalModel(state), mapChoiceToOriginalModel(state, choice));
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() ? true : super.areAllChoiceActionsUnique();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		if (restriction == Restriction.STRICT) {
			return super.getSuccessorsIterator(state, choice);
		}
		int originalState = mapStateToOriginalModel(state);
		Iterator<Integer> successors = model.getSuccessorsIterator(originalState, choice);
		if (redirectTransitions.get(originalState)) {
			successors = new MappingIterator.ObjToObj<>(successors, this::mapStateToRestrictedModel);
		}
		return successors;
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(final int state, final int choice)
	{
		int originalState = mapStateToOriginalModel(state);
		int originalChoice = mapChoiceToOriginalModel(state, choice);
		Iterator<Entry<Integer, Value>> transitions = model.getTransitionsIterator(originalState, originalChoice);
		if (redirectTransitions.get(originalState)) {
			transitions = new MappingIterator.ObjToObj<>(transitions, this::mapTransitionToRestrictedModel);
		}
		return transitions;
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		int numStates = model.getNumStates();
		states = new BitSet(numStates);
		states.set(0, numStates);
		restriction = Restriction.TRANSITIVE_CLOSURE_SAFE;
		// FIXME ALG: extract identity array generation
		mappingToOriginalModel = new int[numStates];
		mappingToRestrictedModel = new int[mappingToRestrictedModel.length];
		Arrays.fill(mappingToRestrictedModel, ModelTransformation.UNDEF);
		int state = 0;
		for (; state < numStates; state++) {
			mappingToOriginalModel[state] = state;
			mappingToRestrictedModel[state] = state;
		}
		for (; state < mappingToRestrictedModel.length; state++) {
			mappingToRestrictedModel[state] = state;
		}
	}



	//--- instance methods ---

	// FIXME ALG: similar method in MDPAlteredDistributions
	public int mapChoiceToOriginalModel(final int state, final int choice)
	{
		if (restriction != Restriction.STRICT) {
			return choice;
		}
		// FIXME ALG: consider caching
		final int originalState = mapStateToOriginalModel(state);
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(originalState); originalChoice < numOriginalChoices; originalChoice++) {
			if (model.allSuccessorsInSet(originalState, originalChoice, states)) {
				if (countChoices == choice) {
					return originalChoice;
				}
				countChoices++;
			}
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

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

	public int mapStateToRestrictedModel(final int state)
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
		//FIXME ALG: consider allocating a BitSet in a suited size
		final BitSet mappedStates = new BitSet();
		for (int originalState : new IterableBitSet(originalStates)) {
			final int state = mappingToRestrictedModel[originalState];
			if (state != ModelTransformation.UNDEF) {
				mappedStates.set(state);
			}
		}
		return mappedStates;
	}

	public Entry<Integer, Value> mapTransitionToRestrictedModel(final Entry<Integer, Value> transition)
	{
		final Integer state = mapStateToRestrictedModel(transition.getKey());
		final Value probability = transition.getValue();
		return new AbstractMap.SimpleImmutableEntry<>(state, probability);
	}



	//--- static methods ---

	public static <Value> BasicModelTransformation<MDP<Value>, MDPRestricted<Value>> transform(final MDP model, final IntPredicate states)
	{
		return transform(model, Reducible.unboxInt(new IterableStateSet(states, model.getNumStates())).collect(new BitSet()));
	}

	public static <Value> BasicModelTransformation<MDP<Value>, MDPRestricted<Value>> transform(final MDP model, final BitSet states)
	{
		return transform(model, states, STANDARD_RESTRICTION);
	}

	public static <Value> BasicModelTransformation<MDP<Value>, MDPRestricted<Value>> transform(final MDP model, final BitSet states, final Restriction restriction)
	{
		final MDPRestricted restricted = new MDPRestricted(model, states, restriction);
		final BitSet transformedStates = restricted.mapStatesToRestrictedModel(states);
		return new BasicModelTransformation<>(model, restricted, transformedStates, restricted.mappingToRestrictedModel);
	}
}