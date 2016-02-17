package common.functions.primitive;

import common.functions.AbstractPairPredicate;

public abstract class AbstractPairPredicateIntegerInteger extends AbstractPairPredicate<Integer, Integer>implements PairPredicateIntegerInteger
{
	@Override
	public boolean getBoolean(final Integer element1, final Integer element2)
	{
		return getBoolean(element1.intValue(), element2.intValue());
	}

	public abstract boolean getBoolean(final int element1, final int element2);

	@Override
	public PredicateInteger curry(final Integer element1)
	{
		return curry(element1.intValue());
	}

	public PredicateInteger curry(final int element1)
	{
		return new AbstractPredicateInteger()
		{
			@Override
			public boolean getBoolean(final int element2)
			{
				return AbstractPairPredicateIntegerInteger.this.getBoolean(element1, element2);
			}
		};
	}

	@Override
	public PairPredicateIntegerInteger not()
	{
		return new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public final boolean getBoolean(final int element1, final int element2)
			{
				return !AbstractPairPredicateIntegerInteger.this.getBoolean(element1, element2);
			}

			@Override
			public PairPredicateIntegerInteger not()
			{
				return AbstractPairPredicateIntegerInteger.this;
			}
		};
	}

	public PairPredicateIntegerInteger and(final PairPredicateIntegerInteger predicate)
	{
		return new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public final boolean getBoolean(final int element1, final int element2)
			{
				return AbstractPairPredicateIntegerInteger.this.getBoolean(element1, element2) && predicate.getBoolean(element1, element2);
			}
		};
	}

	public PairPredicateIntegerInteger or(final PairPredicateIntegerInteger predicate)
	{
		return new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public final boolean getBoolean(final int element1, final int element2)
			{
				return AbstractPairPredicateIntegerInteger.this.getBoolean(element1, element2) || predicate.getBoolean(element1, element2);
			}
		};
	}

	public PairPredicateIntegerInteger implies(final PairPredicateIntegerInteger predicate)
	{
		return new AbstractPairPredicateIntegerInteger()
		{
			@Override
			public final boolean getBoolean(final int element1, final int element2)
			{
				return (!AbstractPairPredicateIntegerInteger.this.getBoolean(element1, element2)) || predicate.getBoolean(element1, element2);
			}
		};
	}
}