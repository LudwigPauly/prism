package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import common.BitSetTools;
import common.functions.PairMapping;
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

public class MDPAlteredDistributions extends MDPView
{
	private MDP<Double> model;
	private PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices;
	private PairMapping<Integer, Integer, Object> actions;


	/**
	 * If {@code choices} returns {@code null} for a state and a choice, the original transitions are preserved.
	 *
	 * @param model
	 * @param choiceMapping
	 */
	public MDPAlteredDistributions(final MDP<Double> model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choiceMapping) {
		this(model, choiceMapping, model::getAction);
	}

	/**
	 * If {@code choices} returns {@code null} for a state and a choice, the original transitions are preserved.
	 * If {@code actions} is {@code null}, the original actions are preserved.
	 *
	 * @param model
	 * @param choices
	 * @param actions
	 */
	public MDPAlteredDistributions(final MDP<Double> model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> choices,
								   final PairMapping<Integer, Integer, Object> actions) {
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAlteredDistributions(final MDPAlteredDistributions altered) {
		super(altered);
		model = altered.model;
		choices = altered.choices;
		actions = altered.actions;
	}


	//--- Cloneable ---

	@Override
	public MDPAlteredDistributions clone() {
		return new MDPAlteredDistributions(this);
	}


	//--- Model ---

	@Override
	public int getNumStates() {
		return model.getNumStates();
	}

	@Override
	public int getNumInitialStates() {
		return model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates() {
		return model.getInitialStates();
	}

	@Override
	public int getFirstInitialState() {
		return model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(final int state) {
		return model.isInitialState(state);
	}

	@Override
	public List<State> getStatesList() {
		return model.getStatesList();
	}

	@Override
	public VarList getVarList() {
		return model.getVarList();
	}

	@Override
	public Values getConstantValues() {
		return model.getConstantValues();
	}

	@Override
	public BitSet getLabelStates(String name) {
		return super.hasLabel(name) ? super.getLabelStates(name) : model.getLabelStates(name);
	}

	@Override
	public Set<String> getLabels() {
		return new UnionSet<>(super.getLabels(), model.getLabels());
	}

	@Override
	public boolean hasLabel(String name) {
		return super.hasLabel(name) || model.hasLabel(name);
	}


	//--- NondetModel ---

	@Override
	public int getNumChoices(final int state) {
		return model.getNumChoices(state);
	}

	@Override
	public Object getAction(final int state, final int choice) {
		return actions == null ? model.getAction(state, choice) : actions.apply(state, choice);
	}


	//--- MDP ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state, final int choice) {
		final Iterator<Entry<Integer, Double>> transitions = choices.apply(state, choice);
		if (transitions == null) {
			return model.getTransitionsIterator(state, choice);
		}
		return transitions;
	}


	//--- MDPView ---

	@Override
	protected void fixDeadlocks() {
		assert !fixedDeadlocks : "deadlocks already fixed";

		model = MDPAdditionalChoices.fixDeadlocks(this.clone());
		choices = PairMapping.constant(null);
		actions = null;
	}


	//--- static methods ---

	public static MDPAlteredDistributions normalizeDistributions(final MDP<Double> model) {
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> normalize = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>>() {
			@Override
			public Iterator<Entry<Integer, Double>> apply(final Integer state, final Integer choice) {
				Evaluator<Double> evaluator = model.getEvaluator();
				final Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(state, choice);
				if (!transitions.hasNext()) {
					return transitions;
				}
				final Distribution <Double> distribution = new Distribution(transitions, evaluator);
				final Double sum = distribution.sum();
				if (!evaluator.isOne(sum)) {
					for (Entry<Integer, Double> trans : distribution) {
						distribution.set(trans.getKey(), evaluator.divide(trans.getValue(), sum));
					}
				}
				return distribution.iterator();
			}
		};

		return new MDPAlteredDistributions(model, normalize, model::getAction);
	}
}