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

public abstract class CTMCView extends MCView implements CTMC<Double>, Cloneable
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
	public Double getExitRate(int i)
	{
		FunctionalIterator<Entry<Integer, Double>> transitions = Reducible.extend(getTransitionsIterator(i));
		return transitions.mapToDouble(Entry::getValue).sum();
	}
	@Override
	public Double getMaxExitRate()
	{
		FunctionalIterable<Double> exitRates = new Range(getNumStates()).map((int s) -> getExitRate(s));
		return exitRates.reduce((x,y)-> Double.max(x,y)).orElse(Double.NEGATIVE_INFINITY);
	}

	@Override
	public Double getMaxExitRate(BitSet subset)
	{
		FunctionalIterable<Double>  exitRates = new IterableBitSet(subset).map((int s) -> getExitRate(s));
		return exitRates.reduce((x,y)-> Double.max(x,y)).orElse(Double.NEGATIVE_INFINITY);
	}

	@Override
	public Double getDefaultUniformisationRate()
	{
		return 1.02 * getMaxExitRate();
	}

	@Override
	public Double getDefaultUniformisationRate(BitSet nonAbs)
	{
		return 1.02 * getMaxExitRate(nonAbs);
	}

	@Override
	public DTMC<Double> buildImplicitEmbeddedDTMC()
	{
		return new DTMCEmbeddedSimple(this);
	}

	@Override
	public DTMC<Double> getImplicitEmbeddedDTMC()
	{
		return buildImplicitEmbeddedDTMC();
	}

	@Override
	public DTMCSimple<Double> buildEmbeddedDTMC()
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
			double d = transitions.mapToDouble(Entry::getValue).sum();
			if (d == 0) {
				dtmc.setProbability(i, i, 1.0);
			} else {
				for (Map.Entry<Integer, Double> e : transitions) {
					dtmc.setProbability(i, e.getKey(), e.getValue() / d);
				}
			}
		}
		return dtmc;
	}

	@Override
	public DTMC<Double> buildImplicitUniformisedDTMC(Double q)
	{
		return new DTMCUniformisedSimple(this, q);
	}

	@Override
	public DTMCSimple<Double> buildUniformisedDTMC(Double q)
	{
		int numStates = getNumStates();
		DTMCSimple dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (int i = 0; i < numStates; i++) {
			final int s = i;
			FunctionalIterable<Entry<Integer,Double>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
			// Add scaled off-diagonal entries
			for (Entry<Integer, Double> e : transitions) {
				dtmc.setProbability(i, e.getKey(), e.getValue() / q);
			}
			// Add diagonal, if needed
			double d = transitions.filter(e -> e.getKey() != s).mapToDouble(Entry::getValue).sum();
			if (d < q) {
				dtmc.setProbability(i, i, 1 - (d / q));
			}
		}
		return dtmc;
	}



	//--- ModelView ---



	//--- instance methods ---

	protected CTMC<Double> uniformised(CTMC<Double> ctmc, double q)
	{
		IntFunction<Iterator<Entry<Integer, Double>>> uniformise = new IntFunction<Iterator<Entry<Integer, Double>>>()
		{
			@Override
			public Iterator<Entry<Integer, Double>> apply(int s)
			{
				FunctionalIterable<Entry<Integer, Double>> transitions = Reducible.extend(() -> getTransitionsIterator(s));
				double sum = transitions.filter(e -> e.getKey() != s).mapToDouble(Entry::getValue).sum();
				SimpleImmutableEntry<Integer, Double> diagonale = new SimpleImmutableEntry<>(s, q - sum);
				return transitions.map((Entry<Integer, Double> e) -> e.getKey() == s ? diagonale : e).iterator();
			}

		};
		return new CTMCAlteredDistributions(ctmc, uniformise);
	}
}