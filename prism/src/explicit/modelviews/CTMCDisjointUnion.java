package explicit.modelviews;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import common.BitSetTools;
import common.iterable.FunctionalIterable;
import common.iterable.MappingIterator;
import common.iterable.collections.ChainedList;
import common.iterable.collections.UnionSet;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.PrismException;
import explicit.CTMC;
import explicit.CTMCSimple;

public class CTMCDisjointUnion extends CTMCView
{
	private CTMC model1;
	private CTMC model2;
	public final int offset;
	private final ToIntFunction<Integer> shiftStateUp;
	private final Function<Entry<Integer, Double>, Entry<Integer, Double>> shiftTransitionUp;



	public CTMCDisjointUnion(final CTMC model1, final CTMC model2)
	{
		this.model1 = model1;
		this.model2 = model2;
		offset = model1.getNumStates();
		shiftStateUp = x -> x + offset;
		shiftTransitionUp = new TransitionShift(offset);
	}

	public CTMCDisjointUnion(final CTMCDisjointUnion union)
	{
		super(union);
		this.model1 = union.model1;
		this.model2 = union.model2;
		this.offset = union.offset;
		this.shiftStateUp = union.shiftStateUp;
		this.shiftTransitionUp = union.shiftTransitionUp;

	}



	//--- Cloneable ---

	@Override
	public CTMCDisjointUnion clone()
	{
		return new CTMCDisjointUnion(this);
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
		final FunctionalIterable<Integer> initials1 = FunctionalIterable.extend(model1.getInitialStates());
		final FunctionalIterable<Integer> initials2 = FunctionalIterable.extend(model2.getInitialStates());
		return initials1.concat(initials2.mapToInt(shiftStateUp));
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
		return (state < offset) ? model1.getSuccessorsIterator(state) : new MappingIterator.ToInt<>(model2.getSuccessorsIterator(state - offset), shiftStateUp);
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



	//--- DTMC ---

	@Override
	public int getNumTransitions(final int state)
	{
		return (state < offset) ? model1.getNumTransitions(state) : model2.getNumTransitions(state - offset);
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(final int state)
	{
		return (state < offset) ? model1.getTransitionsIterator(state)
				: new MappingIterator.From<>(model2.getTransitionsIterator(state - offset), shiftTransitionUp);
	}



	//--- CTMC ---

	@Override
	public void uniformise(double q)
	{
		model1 = uniformised(model1, q);
		model1 = uniformised(model2, q);
	}



	//--- ModelView ---

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
		model1 = CTMCAlteredDistributions.fixDeadlocks(model1);
		model2 = CTMCAlteredDistributions.fixDeadlocks(model2);
	}



	//--- static methods ---

	public static CTMC union(final CTMC... models)
	{
		return union(Arrays.asList(models));
	}

	public static CTMC union(final Iterable<? extends CTMC> models)
	{
		return union(models.iterator());
	}

	public static CTMC union(final Iterator<? extends CTMC> models)
	{
		if (!models.hasNext()) {
			throw new IllegalArgumentException("at least one model expected");
		}
		CTMC union = models.next();
		while (models.hasNext()) {
			union = new CTMCDisjointUnion(union, models.next());
		}

		return union;
	}

	@Deprecated
	public static CTMC CTMCUnion(final CTMC model1, final CTMC model2, final Map<Integer, Integer> identify)
	{
		final CTMCDisjointUnion union = new CTMCDisjointUnion(model1, model2);

		final HashMap<Integer, BitSet> equivalenceClasses = new HashMap<>();
		for (Entry<Integer, Integer> entry : identify.entrySet()) {
			int lifted = union.offset + entry.getValue();
			if (equivalenceClasses.containsKey(lifted)) {
				// add state from mode 1 to eq class of state in mode 2
				equivalenceClasses.get(lifted).set(entry.getKey());
			} else {
				// create new eq class and add state from mode 1 and mode 2
				equivalenceClasses.put(lifted, BitSetTools.asBitSet(entry.getKey(), lifted));
			}
		}
		return CTMCAlteredDistributions.identifyStates(union, equivalenceClasses.values());
	}

	public static void main(final String[] args) throws PrismException
	{
		final CTMCSimple original1 = new CTMCSimple(2);
		original1.addInitialState(0);
		original1.setProbability(0, 0, 0.1);
		original1.setProbability(0, 1, 0.9);
		original1.setProbability(1, 0, 1.0);

		final CTMCSimple original2 = new CTMCSimple(3);
		original2.addInitialState(0);
		original2.setProbability(0, 1, 0.1);
		original2.setProbability(0, 2, 0.9);
		original2.setProbability(1, 2, 0.2);
		original2.setProbability(1, 1, 0.8);

		CTMC union;

		System.out.println("Original Model 1:");
		System.out.print(original1.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original1.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original1.getDeadlockStates()));
		System.out.println(original1);

		System.out.println();

		System.out.println("Original Model 2:");
		System.out.print(original2.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(original2.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(original2.getDeadlockStates()));
		System.out.println(original2);

		System.out.println();

		System.out.println("Disjoint Union Model:");
		union = new CTMCDisjointUnion(original1, original2);
		union.findDeadlocks(true);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println(union);

		System.out.println();

		final Map<Integer, Integer> identify = new HashMap<>();
		identify.put(0, 1);
		System.out.println("Union Model " + identify + ":");
		union = CTMCUnion(original1, original2, identify);
		union.findDeadlocks(true);
		System.out.print(union.infoStringTable());
		System.out.println("Initials:    " + BitSetTools.asBitSet(union.getInitialStates()));
		System.out.println("Deadlocks:   " + BitSetTools.asBitSet(union.getDeadlockStates()));
		System.out.println(union);
	}
}