package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.Map.Entry;

import common.BitSetTools;
import common.functions.Mapping;
import common.iterable.*;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;

public class MDPDisjointUnion extends MDPView
{
	private MDP<Double> model1;
	private MDP<Double> model2;
	private final int offset;
	private final Function<Integer, Integer> shiftStateUp;
	private final TransitionShift shiftTransitionUp;



	public MDPDisjointUnion(final MDP<Double> model1, final MDP<Double> model2)
	{
		this.model1 = model1;
		this.model2 = model2;
		offset = model1.getNumStates();
		shiftStateUp = x -> x + offset;
		shiftTransitionUp = new TransitionShift(offset);
	}

	public MDPDisjointUnion(final MDPDisjointUnion union)
	{
		super(union);
		model1 = union.model1;
		model2 = union.model2;
		offset = union.offset;
		shiftStateUp = union.shiftStateUp;
		shiftTransitionUp = union.shiftTransitionUp;
	}



	//--- Cloneable ---

	@Override
	public MDPDisjointUnion clone()
	{
		return new MDPDisjointUnion(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model1.getNumStates() + model2.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return model1.getNumInitialStates() + model2.getNumInitialStates();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<Integer> getInitialStates()
	{
		final FunctionalIterable<Integer> initials1 = Reducible.extend(model1.getInitialStates());
		final FunctionalIterable<Integer> initials2 = Reducible.extend(model2.getInitialStates());
		return initials1.concat(initials2.map(shiftStateUp));
	}

	@Override
	public int getFirstInitialState()
	{
		final int state = model1.getFirstInitialState();
		return state > 0 ? state : model2.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state)
	{
		return (state < offset) ? model1.isInitialState(state) : model2.isInitialState(state - offset);
	}

	@Override
	public List<State> getStatesList()
	{
		final List<State> states1 = model1.getStatesList();
		final List<State> states2 = model2.getStatesList();
		if (states1 == null || states2 == null) {
			return null;
		}
		return new ChainedList<>(states1, states2);
	}

	@Override
	public VarList getVarList()
	{
		// FIXME ALG: Can we be more efficient than potentially recomputing the VarList?
		return null;
	}

	@Override
	public Values getConstantValues()
	{
		final Values constantValues1 = model1.getConstantValues();
		final Values constantValues2 = model2.getConstantValues();
		if (constantValues1 == null || constantValues2 == null) {
			return null;
		}

		final Values constantValues = new Values(constantValues1);
		final int numValues = constantValues2.getNumValues();
		for (int constant = 0; constant < numValues; constant++) {
			final String name = constantValues2.getName(constant);
			final Object value = constantValues2.getValue(constant);
			final int index = constantValues1.getIndexOf(name);
			if (index == -1) {
				constantValues.addValue(name, value);
			} else {
				assert constantValues.getValue(index).equals(value) : "consistent values expeçted";
			}
		}
		return constantValues;
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		if (super.hasLabel(name)) {
			return super.getLabelStates(name);
		}
		BitSet states1 = model1.getLabelStates(name);
		BitSet states2 = model2.getLabelStates(name);
		if (states2 == null) {
			return states1;
		}
		BitSet states = BitSetTools.shiftUp(states2, offset);
		if (states1 != null) {
			states.or(states1);
		}
		return states;
	}

	@Override
	public Set<String> getLabels()
	{
		return UnionSet.of(super.getLabels(), model1.getLabels(), model2.getLabels());
	}

	@Override
	public boolean hasLabel(String name)
	{
		return super.hasLabel(name) || model1.hasLabel(name) || model2.hasLabel(name);
	}

	@Override
	public int getNumTransitions()
	{
		return model1.getNumTransitions() + model2.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return (state < offset) ? model1.getSuccessorsIterator(state) : new MappingIterator.ObjToObj<>(model2.getSuccessorsIterator(state - offset), shiftStateUp);
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		if (s1 < offset && s2 < offset) {
			return model1.isSuccessor(s1, s2);
		}
		if (s1 >= offset && s2 >= offset) {
			return model2.isSuccessor(offset + s1, offset + s2);
		}
		return false;
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return (state < offset) ? model1.getNumChoices(state) : model2.getNumChoices(state - offset);
	}

	@Override
	public int getMaxNumChoices()
	{
		return Math.max(model1.getMaxNumChoices(), model2.getMaxNumChoices());
	}

	@Override
	public int getNumChoices()
	{
		return model1.getNumChoices() + model2.getNumChoices();
	}

	@Override
	public Object getAction(int state, int choice)
	{
		return (state < offset) ? model1.getAction(state, choice) : model2.getAction(state - offset, choice);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model1.areAllChoiceActionsUnique() && model2.areAllChoiceActionsUnique();
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return (state < offset) ? model1.getNumTransitions(state, choice) : model2.getNumTransitions(state - offset, choice);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state, int choice)
	{
		return (state < offset) ? model1.getSuccessorsIterator(state, choice)
				: new MappingIterator.ObjToObj<>(model2.getSuccessorsIterator(state - offset, choice), shiftStateUp);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		return (state < offset) ? model1.getTransitionsIterator(state, choice)
				: new MappingIterator.ObjToObj<>(model2.getTransitionsIterator(state - offset, choice), shiftTransitionUp);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		try {
			model1.findDeadlocks(false);
			model2.findDeadlocks(false);
		} catch (final PrismException e) {
			assert false : "no attempt to fix deadlocks";
		}
		model1 = MDPAdditionalChoices.fixDeadlocks(model1);
		model2 = MDPAdditionalChoices.fixDeadlocks(model2);
	}



	//--- static methods ---

	public static MDP<Double> union(final MDP<Double>... models)
	{
		return union(Arrays.asList(models));
	}

	public static MDP<Double> union(final Iterable<? extends MDP<Double>> models)
	{
		return union(models.iterator());
	}

	public static MDP<Double> union(final Iterator<? extends MDP<Double>> models)
	{
		if (!models.hasNext()) {
			throw new IllegalArgumentException("at least one model expected");
		}
		MDP<Double> union = models.next();
		while (models.hasNext()) {
			union = new MDPDisjointUnion(union, models.next());
		}

		return union;
	}

	@Deprecated
	public static MDP<Double> MDPUnion(final MDP<Double> model1, final MDP<Double> model2, final Map<Integer, Integer> identify)
	{
		MDPDisjointUnion union = new MDPDisjointUnion(model1, model2);
		Mapping<Entry<Integer, Integer>, BitSet> equivalenceClass = new Mapping<Entry<Integer, Integer>, BitSet>()
		{
			@Override
			public final BitSet apply(final Entry<Integer, Integer> id)
			{
				final BitSet equivalentStates = new BitSet();
				equivalentStates.set(id.getKey());
				equivalentStates.set(union.offset + id.getValue());
				return equivalentStates;
			}
		};
		Iterable<BitSet> equivalenceClasses = Reducible.extend(identify.entrySet()).map(equivalenceClass);
		EquivalenceRelationInteger equivalence = new EquivalenceRelationInteger(equivalenceClasses);
		return MDPEquiv.transform(union, equivalence, true).getTransformedModel();
	}
}