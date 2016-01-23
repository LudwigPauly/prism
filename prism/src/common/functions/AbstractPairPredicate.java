package common.functions;

public abstract class AbstractPairPredicate<R, S> extends AbstractPairMapping<R, S, Boolean>implements PairPredicate<R, S>
{
	@Override
	public Boolean apply(final R element1, final S element2)
	{
		return getBoolean(element1, element2);
	}

	@Override
	public Predicate<S> curry(final R element1)
	{
		return new AbstractPredicate<S>()
		{
			@Override
			public boolean getBoolean(final S element2)
			{
				return AbstractPairPredicate.this.getBoolean(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> not()
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean getBoolean(final R element1, final S element2)
			{
				return !AbstractPairPredicate.this.getBoolean(element1, element2);
			}

			@Override
			public AbstractPairPredicate<R, S> not()
			{
				return AbstractPairPredicate.this;
			}
		};
	}

	@Override
	public PairPredicate<R, S> and(final PairPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean getBoolean(final R element1, final S element2)
			{
				return AbstractPairPredicate.this.getBoolean(element1, element2) && predicate.getBoolean(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> or(final PairPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean getBoolean(final R element1, final S element2)
			{
				return AbstractPairPredicate.this.getBoolean(element1, element2) || predicate.getBoolean(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<R, S> implies(final PairPredicate<? super R, ? super S> predicate)
	{
		return new AbstractPairPredicate<R, S>()
		{
			@Override
			public final boolean getBoolean(final R element1, final S element2)
			{
				return (!AbstractPairPredicate.this.getBoolean(element1, element2)) || predicate.getBoolean(element1, element2);
			}
		};
	}

	@Override
	public PairPredicate<S, R> inverse()
	{
		return new AbstractPairPredicate<S, R>()
		{
			@Override
			public final boolean getBoolean(final S element1, final R element2)
			{
				return AbstractPairPredicate.this.getBoolean(element2, element1);
			}
		};
	}
}