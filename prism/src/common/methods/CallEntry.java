package common.methods;

import java.util.Map.Entry;

import common.functions.Mapping;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallEntry
{
	@SuppressWarnings("rawtypes")
	private static final Mapping GET_KEY = new GetKey();

	@SuppressWarnings("rawtypes")
	private static final Mapping GET_VALUE = new GetValue();

	@SuppressWarnings("unchecked")
	public static <S, T> Mapping<Entry<S, T>, S> getKey()
	{
		return GET_KEY;
	}

	@SuppressWarnings("unchecked")
	public static <S, T> Mapping<Entry<S, T>, T> getValue()
	{
		return GET_VALUE;
	}

	public static final class GetKey implements Mapping<Entry<Object, ?>, Object>, Method<Entry<Object, ?>, Object>
	{
		@Override
		public Object apply(final Entry<Object, ?> entry)
		{
			return entry.getKey();
		}
	}

	public static final class GetValue implements Mapping<Entry<?, Object>, Object>, Method<Entry<?, Object>, Object>
	{
		@Override
		public Object apply(final Entry<?, Object> entry)
		{
			return entry.getValue();
		}
	}
}