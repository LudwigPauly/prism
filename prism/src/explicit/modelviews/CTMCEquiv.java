package explicit.modelviews;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

import common.BitSetTools;
import common.IterableBitSet;
import common.iterable.*;
import explicit.BasicModelTransformation;
import explicit.CTMC;
import explicit.CTMCSimple;
import explicit.Distribution;
import explicit.ReachabilityComputer;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.Evaluator;
import prism.PrismException;

/**
 * An CTMCEquiv is the quotient CTMC with respect to an equivalence relation.
 * For efficiency, non-representative states are not removed but deadlocked.
 */
public class CTMCEquiv extends CTMCView
{
	public static final boolean COMPLEMENT = true;

	protected CTMC<Double> model;
	protected EquivalenceRelationInteger identify;
	protected boolean normalize;
	protected BitSet hasTransitionToNonRepresentative;

	public CTMCEquiv(final CTMC<Double> model, final EquivalenceRelationInteger identify, boolean normalize)
	{
		this.model = model;
		this.identify = identify;
		this.normalize = normalize;
		assert new Range(model.getNumStates()).allMatch((int s) -> (identify.getRepresentative(s) == identify.getEquivalenceClass(s).nextSetBit(0)));
		// Expensive if predecessor relation hasn't been computed yet
		hasTransitionToNonRepresentative = new ReachabilityComputer(model).computePre(identify.getNonRepresentatives());
	}

	public CTMCEquiv(CTMCEquiv CTMCEquiv)
	{
		super(CTMCEquiv);
		model = CTMCEquiv.model;
		identify = CTMCEquiv.identify;
		normalize = CTMCEquiv.normalize;
		hasTransitionToNonRepresentative = CTMCEquiv.hasTransitionToNonRepresentative;
	}



	//--- Cloneable ---

	@Override
	public CTMCEquiv clone()
	{
		return new CTMCEquiv(this);
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
	public boolean isInitialState(int state)
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

	@Override
	public Iterator<Integer> getSuccessorsIterator(int state)
	{
		if (! identify.isRepresentative(state)) {
			return Collections.emptyIterator();
		}
		BitSet eqClass = identify.getEquivalenceClassOrNull(state);
		if (eqClass == null) {
			Iterator<Integer> successors = model.getSuccessorsIterator(state);
			if (! hasTransitionToNonRepresentative.get(state)) {
				return successors;
			}
			return Reducible.extend(successors).map((Function<Integer,Integer>) s-> identify.getRepresentative(s)).distinct();
		}
		return new IterableBitSet(eqClass).iterator().flatMap((int s) -> mapSuccessorsToRepresentative(s)).distinct();
	}



	//--- DTMC ---

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int state)
	{
		Evaluator <Double> evaluator = getEvaluator();
		if (! identify.isRepresentative(state)) {
			return Collections.emptyIterator();
		}
		BitSet eqClass = identify.getEquivalenceClass(state);
		if (eqClass == null) {
			Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(state);
			if (! hasTransitionToNonRepresentative.get(state)) {
				return transitions;
			}
			Reducible.extend(transitions).map(this::mapToRepresentative);
			// Dedupe using Distribution
			return new Distribution(transitions, evaluator).iterator();
		}
		// Dedupe using Distribution
		// Do not use stream>>flatMap, since it preallocates memory unnecessarily
		Distribution <Double> distribution = new Distribution<>(new IterableBitSet(eqClass).iterator().flatMap((int s) -> mapTransitionsToRepresentative(s)), evaluator);
		if (normalize) {
			double sum = distribution.sum();
			if (sum > 1.0) {
				Function<Entry<Integer, Double>, Entry<Integer, Double>> scale = (trans -> new SimpleImmutableEntry<>(trans.getKey(), trans.getValue()/sum));
				return Reducible.extend(distribution).map(scale).iterator();
			}
		}
		return distribution.iterator();
	}



	//--- CTMC ---

	@Override
	public void uniformise(Double q)
	{
		model = uniformised(this.clone(), q);
		identify = new EquivalenceRelationInteger();
		normalize = false;
		hasTransitionToNonRepresentative = new BitSet();
	}



	//--- ModelView ---

	@Override
	protected void fixDeadlocks()
	{
		assert !fixedDeadlocks : "deadlocks already fixed";
		model = CTMCAlteredDistributions.fixDeadlocks(this.clone());
		identify = new EquivalenceRelationInteger();
		normalize = false;
		hasTransitionToNonRepresentative = new BitSet();
	}



	//--- instance methods ---

	public Iterator<Integer> mapSuccessorsToRepresentative(int state)
	{
		Iterator<Integer> successors = model.getSuccessorsIterator(state);
		if (! hasTransitionToNonRepresentative.get(state)) {
			return successors;
		}
		return Reducible.extend(successors).map((Function<Integer,Integer>) s -> identify.getRepresentative(s));
	}

	public Iterator<Entry<Integer, Double>> mapTransitionsToRepresentative(int state)
	{
		Iterator<Entry<Integer, Double>> transitions = model.getTransitionsIterator(state);
		if (! hasTransitionToNonRepresentative.get(state)) {
			return transitions;
		}
		return Reducible.extend(transitions).map(this::mapToRepresentative);
	}

	public Entry<Integer, Double> mapToRepresentative(Entry<Integer, Double> transition)
	{
		int state          = transition.getKey();
		int representative = identify.getRepresentative(state);
		if (state == representative) {
			return transition;
		}
		double probability = transition.getValue();
		return new SimpleImmutableEntry<>(representative, probability);
	}



	//--- static methods ---

	public static BasicModelTransformation<CTMC<Double>, ? extends CTMC<Double>> transform(CTMC<Double> model, EquivalenceRelationInteger identify)
	{
		return transform(model, identify, true);
	}

	public static BasicModelTransformation<CTMC<Double>, ? extends CTMC<Double>> transform(CTMC<Double> model, EquivalenceRelationInteger identify, boolean normalize)
	{
		return transform(model, identify, normalize, false);
	}

	public static BasicModelTransformation<CTMC<Double>, ? extends CTMC<Double>> transform(CTMC<Double> model, EquivalenceRelationInteger identify, boolean normalize, boolean removeNonRepresentatives)
	{
		BasicModelTransformation<CTMC<Double>, CTMCEquiv> quotient = new BasicModelTransformation<>(model, new CTMCEquiv(model, identify, normalize));
		if (! removeNonRepresentatives) {
			return quotient;
		}
		BitSet representatives = identify.getRepresentatives(model.getNumStates());
		BasicModelTransformation<CTMC<Double>, CTMCRestricted> restriction = CTMCRestricted.transform(quotient.getTransformedModel(), representatives, Restriction.TRANSITIVE_CLOSURE_SAFE);
		return restriction.compose(quotient);
	}
}