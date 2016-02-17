package common.functions.primitive;

import common.functions.AbstractPredicate;
import common.functions.Mapping;
import common.functions.Predicate;

public abstract class AbstractPredicateDouble extends AbstractPredicate<Double>implements PredicateDouble
{
	@Override
	public boolean getBoolean(final Double element)
	{
		return getBoolean(element.doubleValue());
	}

	public abstract boolean getBoolean(final double element);

	@Override
	public PredicateDouble not()
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean getBoolean(final double element)
			{
				return !AbstractPredicateDouble.this.getBoolean(element);
			}

			@Override
			public AbstractPredicateDouble not()
			{
				return AbstractPredicateDouble.this;
			}
		};
	}

	public PredicateDouble and(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean getBoolean(final double element)
			{
				return AbstractPredicateDouble.this.getBoolean(element) && predicate.getBoolean(element);
			}
		};
	}

	public PredicateDouble or(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean getBoolean(final double element)
			{
				return AbstractPredicateDouble.this.getBoolean(element) || predicate.getBoolean(element);
			}
		};
	}

	public PredicateDouble implies(final PredicateDouble predicate)
	{
		return new AbstractPredicateDouble()
		{
			@Override
			public final boolean getBoolean(final double element)
			{
				return (!AbstractPredicateDouble.this.getBoolean(element)) || predicate.getBoolean(element);
			}
		};
	}

	public <S> Predicate<S> compose(final Mapping<S, ? extends Double> mapping)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public final boolean getBoolean(final S element)
			{
				return AbstractPredicateDouble.this.getBoolean(mapping.get(element).doubleValue());
			}
		};
	}
}