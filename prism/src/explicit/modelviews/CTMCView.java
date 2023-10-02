package explicit.modelviews;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.function.IntFunction;

import common.IterableBitSet;
import common.iterable.*;
import explicit.CTMC;
import explicit.DTMC;
import explicit.DTMCEmbeddedSimple;
import explicit.DTMCSimple;
import explicit.DTMCUniformisedSimple;
import param.Function;
import prism.Evaluator;
import prism.ModelType;

public abstract class CTMCView<Value> extends MCView<Value> implements CTMC<Value>, Cloneable
{
	public CTMCView()
	{
		super();
	}

	public CTMCView(final ModelView model)
	{
		super(model);
	}



	//--- Object ---



	//--- Model ---

	@Override
	public ModelType getModelType()
	{
		return ModelType.CTMC;
	}
	


	//--- DTMC ---



	//--- CTMC ---

	@Override
	public Value getExitRate(int i)
	{
		FunctionalIterator<Entry<Integer, Value>> transitions = Reducible.extend(getTransitionsIterator(i));
		Evaluator <Value> evaluator = getEvaluator();
		return transitions.map(Entry::getValue).reduce(((x,y) -> evaluator.add(x,y))).get();
	}
	@Override
	public Value getMaxExitRate()
	{
		FunctionalIterable<Value> exitRates = new Range(getNumStates()).map((int s) -> getExitRate(s));
		Evaluator <Value> evaluator = getEvaluator();
		return exitRates.reduce((x,y)-> evaluator.max(x,y)).orElse(evaluator.negative_infinity());
	}

	@Override
	public Value getMaxExitRate(BitSet subset)
	{
		FunctionalIterable<Value>  exitRates = new IterableBitSet(subset).map((int s) -> getExitRate(s));
		Evaluator <Value> evaluator = getEvaluator();
		return exitRates.reduce((x,y)-> evaluator.max(x,y)).orElse(evaluator.negative_infinity());
	}

	@Override
	public Value getDefaultUniformisationRate()
	{
		Evaluator <Value> evaluator = getEvaluator();
		return evaluator.multiply(evaluator.fromString("1.02"), getMaxExitRate());
	}

	@Override
	public Value getDefaultUniformisationRate(BitSet nonAbs)
	{
		Evaluator <Value> evaluator = getEvaluator();
		return evaluator.multiply(evaluator.fromString("1.02"), getMaxExitRate(nonAbs));
	}

	@Override
	public DTMC buildImplicitEmbeddedDTMC()
	{
		return new DTMCEmbeddedSimple(this);
	}

	@Override
	public DTMC getImplicitEmbeddedDTMC()
	{
		return buildImplicitEmbeddedDTMC();
	}

	@Override
	public DTMCSimple buildEmbeddedDTMC()
	{
		Evaluator <Value> evaluator = getEvaluator();
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Value>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
			Value d = transitions.map(Entry::getValue).reduce((x,y)->evaluator.add(x,y)).get();
			if (evaluator.isZero(d)) {
				dtmc.setProbability(i, i, evaluator.one());
			} else {
				for (Map.Entry<Integer, Value> e : transitions) {
					dtmc.setProbability(i, e.getKey(), evaluator.divide(e.getValue(), d));
				}
			}
		}
		return dtmc;
	}

	@Override
	public DTMC<Value> buildImplicitUniformisedDTMC(Value q)
	{
		return new DTMCUniformisedSimple(this, q);
	}

	@Override
	public DTMCSimple buildUniformisedDTMC(Value q)
	{
		Evaluator <Value> evaluator = getEvaluator();
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Value>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
			// Add scaled off-diagonal entries
			for (Entry<Integer, Value> e : transitions) {
				dtmc.setProbability(i, e.getKey(), evaluator.divide(e.getValue(), q));
			}
			// Add diagonal, if needed
			Value d = transitions.filter(e -> e.getKey() != s).map(Entry::getValue).reduce((x,y) -> evaluator.add(x,y)).get();
			if (evaluator.gt(q,d)) {
				dtmc.setProbability(i, i, evaluator.subtract(evaluator.one(), evaluator.divide(d,q)));
			}
		}
		return dtmc;
	}



	//--- ModelView ---



	//--- instance methods ---

	protected CTMC<Value> uniformised(CTMC<Value> ctmc, Value q)
	{
		IntFunction<Iterator<Entry<Integer, Value>>> uniformise = new IntFunction<Iterator<Entry<Integer, Value>>>()
		{
			@Override
			public Iterator<Entry<Integer, Value>> apply(int s)
			{
				Evaluator <Value> evaluator = getEvaluator();
				FunctionalIterable<Entry<Integer, Value>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
				Value sum = transitions.filter(e -> e.getKey() != s).map(Entry::getValue).reduce((x,y)->evaluator.add(x,y)).get();
				SimpleImmutableEntry<Integer, Value> diagonale = new SimpleImmutableEntry<>(s, evaluator.subtract(q,sum));
				return transitions.map((Entry<Integer, Value> e) -> e.getKey() == s ? diagonale : e).iterator();
			}

		};
		return new CTMCAlteredDistributions<Value>(ctmc, uniformise);
	}
}