package common.methods;

import java.util.Collection;

import common.functions.AbstractMapping;
import common.functions.AbstractPairPredicate;
import common.functions.AbstractPredicate;
import common.functions.Predicate;

public class CallCollection
{
	@SuppressWarnings("rawtypes")
	private static final Contains CONTAINS = new Contains();
	private static final Size SIZE = new Size();

	@SuppressWarnings("unchecked")
	public static <T> Contains<T> contains()
	{
		return CONTAINS;
	}

	public static Size size()
	{
		return SIZE;
	}

	public static final class Contains<T> extends AbstractPairPredicate<Collection<T>, T> implements UnaryMethod<Collection<T>, T, Boolean>
	{
		@Override
		public Predicate<T> on(final Collection<T> collection)
		{
			return curry(collection);
		}

		@Override
		public Predicate<T> curry(final Collection<T> collection)
		{
			return new AbstractPredicate<T>()
			{
				@Override
				public boolean getBoolean(final T element)
				{
					return collection.contains(element);
				}
			};
		}

		@Override
		public boolean getBoolean(final Collection<T> collection, final T element)
		{
			return collection.contains(element);
		}
	}

	public static final class Size extends AbstractMapping<Collection<?>, Integer> implements Method<Collection<?>, Integer>
	{
		@Override
		public Integer on(final Collection<?> collection)
		{
			return collection.size();
		}

		@Override
		public Integer get(final Collection<?> collection)
		{
			return collection.size();
		}
	}
}