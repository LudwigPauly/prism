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

public class MDPAlteredDistributions<Value> extends MDPView<Value>
{
	private MDP<Value> model;
	private PairMapping<Integer, Integer, Iterator<Entry<Integer, Value>>> choices;
	private PairMapping<Integer, Integer, Object> actions;


	/**
	 * If {@code choices} returns {@code null} for a state and a choice, the original transitions are preserved.
	 *
	 * @param model
	 * @param choiceMapping
	 */
	public MDPAlteredDistributions(final MDP<Value> model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Value>>> choiceMapping) {
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
	public MDPAlteredDistributions(final MDP<Value> model, final PairMapping<Integer, Integer, Iterator<Entry<Integer, Value>>> choices,
								   final PairMapping<Integer, Integer, Object> actions) {
		this.model = model;
		this.choices = choices;
		this.actions = actions;
	}

	public MDPAlteredDistributions(final MDPAlteredDistributions<Value> altered) {
		super(altered);
		model = altered.model;
		choices = altered.choices;
		actions = altered.actions;
	}


	//--- Cloneable ---

	@Override
	public MDPAlteredDistributions<Value> clone() {
		return new MDPAlteredDistributions<>(this);
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
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(final int state, final int choice) {
		final Iterator<Entry<Integer, Value>> transitions = choices.apply(state, choice);
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

	public static <Value> MDPAlteredDistributions<Value> normalizeDistributions(final MDP<Value> model) {
		final PairMapping<Integer, Integer, Iterator<Entry<Integer, Value>>> normalize = new PairMapping<Integer, Integer, Iterator<Entry<Integer, Value>>>() {
			@Override
			public Iterator<Entry<Integer, Value>> apply(final Integer state, final Integer choice) {
				Evaluator<Value> evaluator = model.getEvaluator();
				final Iterator<Entry<Integer, Value>> transitions = model.getTransitionsIterator(state, choice);
				if (!transitions.hasNext()) {
					return transitions;
				}
				final Distribution <Value> distribution = new Distribution(transitions, evaluator);
				final Value sum = distribution.sum();
				if (!evaluator.isOne(sum)) {
					for (Entry<Integer, Value> trans : distribution) {
						distribution.set(trans.getKey(), evaluator.divide(trans.getValue(), sum));
					}
				}
				return distribution.iterator();
			}
		};

		return new MDPAlteredDistributions<>(model, normalize, model::getAction);
	}
}