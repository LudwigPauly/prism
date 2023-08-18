package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.iterable.*;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;

public class MDPAdditionalStates extends MDPView
{
	private MDP<Double> model;
	private int numStates;
	private Range indices;
	private List<State> states;

	public MDPAdditionalStates(final MDP<Double> model, final int numStates)
	{
		this(model, numStates, true);
	}

	public MDPAdditionalStates(final MDP<Double> model, final int numStates, final boolean fill)
	{
		this.model = model;
		this.numStates = numStates;
		final int originalNumStates = model.getNumStates();
		indices = new Range(originalNumStates, originalNumStates + numStates);

		if (fill) {
			final List<State> statesList = model.getStatesList();
			if (statesList != null && !statesList.isEmpty()) {
				states = Collections.nCopies(numStates, statesList.get(0));
				return;
			}
		}
		this.states = null;
	}

	public MDPAdditionalStates(final MDP<Double> model, final State ... states)
	{
		this(model, Arrays.asList(states));
	}

	public MDPAdditionalStates(final MDP<Double> model, final List<State> states)
	{
		this.model = model;
		this.numStates = (states == null) ? 0 : states.size();
		final int originalNumStates = model.getNumStates();
		indices = new Range(originalNumStates, originalNumStates + numStates);
		this.states = states;
	}

	public MDPAdditionalStates(final MDPAdditionalStates additional)
	{
		super(additional);
		model = additional.model;
		numStates = additional.numStates;
		indices = additional.indices;
		states = additional.states;
	}



	//--- Cloneable ---

	@Override
	public MDPAdditionalStates clone()
	{
		return new MDPAdditionalStates(this);
	}



	//--- Model ---

	@Override
	public int getNumStates()
	{
		return model.getNumStates() + numStates;
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
		return (state < model.getNumStates()) && model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList()
	{
		return new ChainedList<>(model.getStatesList(), states);
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

	@Override
	public int getNumTransitions()
	{
		return model.getNumTransitions();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state)
	{
		return (state < model.getNumStates()) ? model.getSuccessorsIterator(state) : EmptyIterator.of();
	}

	@Override
	public boolean isSuccessor(final int s1, final int s2)
	{
		return (s1 < model.getNumStates()) && (s2 < model.getNumStates()) && model.isSuccessor(s1, s2);
	}



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		return (state < model.getNumStates()) ? model.getNumChoices(state) : 0;
	}

	@Override
	public int getMaxNumChoices()
	{
		return model.getMaxNumChoices();
	}

	@Override
	public int getNumChoices()
	{
		return model.getNumChoices();
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		if (state < model.getNumStates()) {
			return model.getAction(state, choice);
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique();
	}

	@Override
	public int getNumTransitions(final int state, final int choice)
	{
		return (state < model.getNumStates()) ? model.getNumTransitions(state, choice) : 0;
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state, int choice)
	{
		return (state < model.getNumStates()) ? model.getSuccessorsIterator(state, choice) : EmptyIterator.of();
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		if (state < model.getNumStates()) {
			return model.getTransitionsIterator(state, choice);
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(clone());
		states = null;
		numStates = 0;
	}



	//--- instance methods ---

	public Range getAdditionalStateIndices()
	{
		return indices;
	}

}