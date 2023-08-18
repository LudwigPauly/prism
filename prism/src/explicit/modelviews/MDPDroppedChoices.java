package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntPredicate;

import common.BitSetTools;
import common.functions.PairPredicateInt;
import common.iterable.Range;
import common.iterable.Reducible;
import common.iterable.UnionSet;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.Evaluator;
import prism.PrismException;

public class MDPDroppedChoices extends MDPView
{
	private MDP<Double> model;
	private PairPredicateInt preserved;



	public MDPDroppedChoices(final MDP<Double> model, final PairPredicateInt dropped)
	{
		this.model = model;
		// FIXME ALG: consider using preserved instead of dropped
		this.preserved = dropped.negate();
	}

	public MDPDroppedChoices(final MDPDroppedChoices dropped)
	{
		super(dropped);
		model = dropped.model;
		preserved = dropped.preserved;
	}



	//--- Cloneable ---

	@Override
	public MDPDroppedChoices clone()
	{
		return new MDPDroppedChoices(this);
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



	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state)
	{
		// FIXME ALG: consider loop instead of Interval for performance
		return (int) new Range(model.getNumChoices(state)).filter((IntPredicate)preserved.curry(state)).count();
	}

	@Override
	public Object getAction(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getAction(state, originalChoice);
	}

	@Override
	public boolean areAllChoiceActionsUnique()
	{
		return model.areAllChoiceActionsUnique() ? true : super.areAllChoiceActionsUnique();
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getSuccessorsIterator(state, originalChoice);
	}



	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice)
	{
		final int originalChoice = mapChoiceToOriginalModel(state, choice);
		return model.getTransitionsIterator(state, originalChoice);
	}



	//--- MDPView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks((MDP) this.clone());
		preserved = new PairPredicateInt()
		{
			@Override
			public boolean test(int element1, int element2)
			{
				return true;
			}
		};
	}



	//--- static methods

	public static MDPDroppedChoices dropDenormalizedDistributions(final MDP<Double> model)
	{
		final PairPredicateInt denormalizedChoices = new PairPredicateInt()
		{
			@Override
			public boolean test(int state, int choice)
			{
				Evaluator<Double> evaluator = model.getEvaluator();
				final Distribution<Double> distribution = new Distribution<>(model.getTransitionsIterator(state, choice), evaluator);
				return evaluator.gt(evaluator.one(), distribution.sum());
			}
		};
		return new MDPDroppedChoices(model, denormalizedChoices);
	}

	// FIXME ALG: similar method in MDPAlteredDistributions
	public int mapChoiceToOriginalModel(final int state, final int choice)
	{
		int countChoices = 0;
		for (int originalChoice = 0, numOriginalChoices = model.getNumChoices(state); originalChoice < numOriginalChoices; originalChoice++) {
			if (preserved.test(state, originalChoice)) {
				if (countChoices == choice) {
					return originalChoice;
				}
				countChoices++;
			}
		}
		throw new IndexOutOfBoundsException("choice index out of bounds");
	}

}