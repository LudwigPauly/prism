package explicit.modelviews.methods;

import java.util.Iterator;
import java.util.Map.Entry;

import common.functions.AbstractPairMapping;
import common.functions.AbstractTripleMapping;
import common.functions.Mapping;
import common.functions.PairMapping;
import common.functions.primitive.AbstractMappingFromInteger;
import common.functions.primitive.MappingFromInteger;
import common.methods.BinaryMethod;
import common.methods.UnaryMethod;
import explicit.MDP;

public class CallMDP
{
	private static final MDPGetNumChoices GET_NUM_CHOICES = new MDPGetNumChoices();
	private static final MDPGetTransitionsIterator GET_TRANSITIONS_ITERATOR = new MDPGetTransitionsIterator();

	public static MDPGetNumChoices getNumChoices()
	{
		return GET_NUM_CHOICES;
	}
	public static MDPGetTransitionsIterator getTransitionsIterator()
	{
		return GET_TRANSITIONS_ITERATOR;
	}

	public static final class MDPGetNumChoices
			extends AbstractPairMapping<MDP, Integer, Integer>
			implements UnaryMethod<MDP, Integer, Integer>
	{
		@Override
		public Integer get(final MDP model, final Integer state)
		{
			return model.getNumChoices(state);
		}

		@Override
		public MappingFromInteger<Integer> curry(final MDP model)
		{
			new AbstractMappingFromInteger<Integer>()
			{
				@Override
				public Integer get(int state)
				{
					return model.getNumChoices(state);
				}
			};
			return curry(model);
		}

		@Override
		public Mapping<Integer, Integer> on(MDP model)
		{
			return curry(model);
		}
	}

	public static final class MDPGetTransitionsIterator
			extends AbstractTripleMapping<MDP, Integer, Integer, Iterator<Entry<Integer, Double>>>
			implements BinaryMethod<MDP, Integer, Integer, Iterator<Entry<Integer, Double>>>
	{
		public PairMapping<Integer, Integer, Iterator<Entry<Integer, Double>>> on(final MDP model)
		{
			return this.curry(model);
		}

		@Override
		public Iterator<Entry<Integer, Double>> get(final MDP model, final Integer state, final Integer choice)
		{
			return model.getTransitionsIterator(state, choice);
		}
	}
}