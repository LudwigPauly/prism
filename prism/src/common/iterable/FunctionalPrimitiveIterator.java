package common.iterable;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;

// FIXME ALG: consider removing specialized methods to make type inference easier
public interface FunctionalPrimitiveIterator<E, E_CONS> extends FunctionalIterator<E>, PrimitiveIterator<E, E_CONS>
{
	default FunctionalIterator<E> boxed()
	{
		return this;
	}



	public interface OfDouble extends FunctionalPrimitiveIterator<Double, DoubleConsumer>, PrimitiveIterator.OfDouble
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfDouble chain(PrimitiveIterator.OfDouble... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfDouble(unwrap(), iterators);
		}

		@Override
		default FunctionalPrimitiveIterator.OfDouble dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		default FunctionalPrimitiveIterator.OfDouble filter(DoublePredicate predicate)
		{
			return new FilteringIterator.OfDouble(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(DoubleFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(DoubleFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(DoubleFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(DoubleFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(DoubleFunction<? extends T> function)
		{
			return new MappingIterator.FromDouble<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(DoubleUnaryOperator function)
		{
			return new MappingIterator.FromDoubleToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(DoubleToIntFunction function)
		{
			return new MappingIterator.FromDoubleToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(DoubleToLongFunction function)
		{
			return new MappingIterator.FromDoubleToLong(unwrap(), function);
		}

		@Override
		default PrimitiveIterator.OfDouble unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(DoublePredicate predicate)
		{
			return anyMatch(predicate.negate());
		}

		default boolean anyMatch(DoublePredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default double[] collect(double[] array)
		{
			return collect(array, 0);
		}

		default double[] collect(double[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default int collectAndCount(double[] array)
		{
			return collectAndCount(array, 0);
		}

		// FIXME ALG: consider override in subclasses
		default int collectAndCount(double[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextDouble();
			}
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Double) && contains(((Double) obj).doubleValue());
		}

		// FIXME ALG: consider override in subclasses
		default boolean contains(double d)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (d == local.nextDouble()) {
					return true;
				}
			}
			return false;
		}

		@Override
		// FIXME ALG: consider override in subclasses
		default int count()
		{
			// do not use reduce to avoid auto-boxing of count variable
			PrimitiveIterator.OfDouble local = unwrap();
			// exploit arithmetic operator
			int count = 0;
			while (local.hasNext()) {
				// just consume, avoid auto-boxing
				local.nextDouble();
				count++;
			}
			return count;
		}

		default int count(DoublePredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalDouble detect(DoublePredicate predicate)
		{
			FunctionalPrimitiveIterator.OfDouble filtered = filter(predicate);
			return filtered.hasNext() ? OptionalDouble.of(filtered.next()) : OptionalDouble.empty();
		}

		default OptionalDouble max()
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			double max = nextDouble();
			while (hasNext()) {
				double next = nextDouble();
				max         = next > max ? next : max;
			}
			return OptionalDouble.of(max);
		}

		default OptionalDouble min()
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			double min = nextDouble();
			while (hasNext()) {
				double next = nextDouble();
				min         = next < min ? next : min;
			}
			return OptionalDouble.of(min);
		}

		// FIXME ALG: consider override in subclasses
		default <T> T reduce(T identity, ObjDoubleFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			T result                         = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextDouble());
			}
			return result;
		}

		default OptionalDouble reduce(DoubleBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalDouble.empty();
			}
			return OptionalDouble.of(reduce(nextDouble(), accumulator));
		}

		// FIXME ALG: consider override in subclasses
		default double reduce(double identity, DoubleBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			double result                    = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsDouble(result, local.nextDouble());
			}
			return result;
		}

		// FIXME ALG: consider override in subclasses
		default double sum()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfDouble local = unwrap();
			// exploit arithmetic operator
			double sum = 0;
			while (local.hasNext()) {
				sum += local.nextDouble();
			}
			return sum;
		}
	}



	public interface OfInt extends FunctionalPrimitiveIterator<Integer, IntConsumer>, PrimitiveIterator.OfInt
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfInt chain(PrimitiveIterator.OfInt... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfInt(unwrap(), iterators);
		}

		@Override
		default FunctionalPrimitiveIterator.OfInt dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		default FunctionalPrimitiveIterator.OfInt filter(IntPredicate predicate)
		{
			return new FilteringIterator.OfInt(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(IntFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(IntFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(IntFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(IntFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(IntFunction<? extends T> function)
		{
			return new MappingIterator.FromInt<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(IntToDoubleFunction function)
		{
			return new MappingIterator.FromIntToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(IntUnaryOperator function)
		{
			return new MappingIterator.FromIntToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(IntToLongFunction function)
		{
			return new MappingIterator.FromIntToLong(unwrap(), function);
		}

		@Override
		default PrimitiveIterator.OfInt unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(IntPredicate predicate)
		{
			return anyMatch(predicate.negate());
		}

		default boolean anyMatch(IntPredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default int[] collect(int[] array)
		{
			return collect(array, 0);
		}

		default int[] collect(int[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default BitSet collect(BitSet indices)
		{
			forEachRemaining((IntConsumer) indices::set);
			return indices;
		}

		default int collectAndCount(int[] array)
		{
			return collectAndCount(array, 0);
		}

		// FIXME ALG: consider override in subclasses
		default int collectAndCount(int[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextInt();
			}
			return count - offset;
		}

		default int collectAndCount(BitSet indices)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// avoid auto-boxing of array index
			int count = 0;
			while (local.hasNext()) {
				indices.set(local.nextInt());
				count++;
			}
			return count;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Integer) && contains(((Integer) obj).intValue());
		}

		// FIXME ALG: consider override in subclasses
		default boolean contains(int i)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (i == local.nextInt()) {
					return true;
				}
			}
			return false;
		}

		@Override
		// FIXME ALG: consider override in subclasses
		default int count()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// exploit arithmetic operator
			int count = 0;
			while (local.hasNext()) {
				local.nextInt();
				count++;
			}
			return count;
		}

		default int count(IntPredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalInt detect(IntPredicate predicate)
		{
			FunctionalPrimitiveIterator.OfInt filtered = filter(predicate);
			return filtered.hasNext() ? OptionalInt.of(filtered.next()) : OptionalInt.empty();
		}

		default OptionalInt max()
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			int max = nextInt();
			while (hasNext()) {
				int next = nextInt();
				max      = next > max ? next : max;
			}
			return OptionalInt.of(max);
		}

		default OptionalInt min()
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			int min = nextInt();
			while (hasNext()) {
				int next = nextInt();
				min      = next < min ? next : min;
			}
			return OptionalInt.of(min);
		}

		// FIXME ALG: consider override in subclasses
		default <T> T reduce(T identity, ObjIntFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			T result                      = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextInt());
			}
			return result;
		}

		default OptionalInt reduce(IntBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalInt.empty();
			}
			return OptionalInt.of(reduce(nextInt(), accumulator));
		}

		// FIXME ALG: consider override in subclasses
		default int reduce(int identity, IntBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			int result                    = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsInt(result, local.nextInt());
			}
			return result;
		}

		// FIXME ALG: consider override in subclasses
		default int sum()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfInt local = unwrap();
			// exploit arithmetic operator
			int sum = 0;
			while (local.hasNext()) {
				sum += local.nextInt();
			}
			return sum;
		}
	}



	public interface OfLong extends FunctionalPrimitiveIterator<Long, LongConsumer>, PrimitiveIterator.OfLong
	{
		// Transforming Methods

		default FunctionalPrimitiveIterator.OfLong chain(PrimitiveIterator.OfLong... iterators)
		{
			if (iterators.length == 0) {
				return this;
			}
			return new ChainedIterator.OfLong(unwrap(), iterators);
		}

		@Override
		default FunctionalPrimitiveIterator.OfLong dedupe()
		{
			return FilteringIterator.dedupe(unwrap());
		}

		default FunctionalPrimitiveIterator.OfLong filter(LongPredicate predicate)
		{
			return new FilteringIterator.OfLong(unwrap(), predicate);
		}

		default <T> FunctionalIterator<T> flatMap(LongFunction<? extends Iterator<? extends T>> function)
		{
			return new ChainedIterator.Of<>(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfDouble flatMapToDouble(LongFunction<PrimitiveIterator.OfDouble> function)
		{
			return new ChainedIterator.OfDouble(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfInt flatMapToInt(LongFunction<PrimitiveIterator.OfInt> function)
		{
			return new ChainedIterator.OfInt(map(function));
		}

		default <T> FunctionalPrimitiveIterator.OfLong flatMapToLong(LongFunction<PrimitiveIterator.OfLong> function)
		{
			return new ChainedIterator.OfLong(map(function));
		}

		default <T> FunctionalIterator<T> map(LongFunction<? extends T> function)
		{
			return new MappingIterator.FromLong<>(unwrap(), function);
		}

		default PrimitiveIterator.OfDouble map(LongToDoubleFunction function)
		{
			return new MappingIterator.FromLongToDouble(unwrap(), function);
		}

		default PrimitiveIterator.OfInt map(LongToIntFunction function)
		{
			return new MappingIterator.FromLongToInt(unwrap(), function);
		}

		default PrimitiveIterator.OfLong map(LongUnaryOperator function)
		{
			return new MappingIterator.FromLongToLong(unwrap(), function);
		}

		@Override
		default PrimitiveIterator.OfLong unwrap()
		{
			return this;
		}



		// Accumulations Methods (Consuming)

		default boolean allMatch(LongPredicate predicate)
		{
			return anyMatch(predicate.negate());
		}

		default boolean anyMatch(LongPredicate predicate)
		{
			return detect(predicate).isPresent();
		}

		default long[] collect(long[] array)
		{
			return collect(array, 0);
		}

		default long[] collect(long[] array, int offset)
		{
			collectAndCount(array, offset);
			return array;
		}

		default int collectAndCount(long[] array)
		{
			return collectAndCount(array, 0);
		}

		// FIXME ALG: consider override in subclasses
		default int collectAndCount(long[] array, int offset)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// avoid auto-boxing of array index
			int count = offset;
			while (local.hasNext()) {
				array[count++] = local.nextLong();
			}
			return count - offset;
		}

		@Override
		default boolean contains(Object obj)
		{
			// exploit identity test
			return (obj instanceof Long) && contains(((Long) obj).longValue());
		}

		// FIXME ALG: consider override in subclasses
		default boolean contains(long l)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// exploit identity test
			while (local.hasNext()) {
				if (l == local.nextLong()) {
					return true;
				}
			}
			return false;
		}

		@Override
		// FIXME ALG: consider override in subclasses
		default int count()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// exploit arithmetic operator
			int count = 0;
			while (local.hasNext()) {
				local.nextLong();
				count++;
			}
			return count;
		}

		default int count(LongPredicate predicate)
		{
			return filter(predicate).count();
		}

		default OptionalLong detect(LongPredicate predicate)
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			while (local.hasNext()) {
				long next = local.nextLong();
				if (predicate.test(next)) {
					return OptionalLong.of(next);
				}
			}
			return OptionalLong.empty();
		}

		default OptionalLong max()
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			long max = nextLong();
			while (hasNext()) {
				long next = nextLong();
				max       = next > max ? next : max;
			}
			return OptionalLong.of(max);
		}

		default OptionalLong min()
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			long min = nextLong();
			while (hasNext()) {
				long next = nextLong();
				min       = next < min ? next : min;
			}
			return OptionalLong.of(min);
		}

		// FIXME ALG: consider override in subclasses
		default <T> T reduce(T identity, ObjLongFunction<T, T> accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			T result                       = identity;
			while(local.hasNext()) {
				result = accumulator.apply(result, local.nextLong());
			}
			return result;
		}

		default OptionalLong reduce(LongBinaryOperator accumulator)
		{
			if (! hasNext()) {
				return OptionalLong.empty();
			}
			return OptionalLong.of(reduce(nextLong(), accumulator));
		}

		// FIXME ALG: consider override in subclasses
		default long reduce(long identity, LongBinaryOperator accumulator)
		{
			Objects.requireNonNull(accumulator);
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			long result = identity;
			while(local.hasNext()) {
				result = accumulator.applyAsLong(result, local.nextLong());
			}
			return result;
		}

		// FIXME ALG: consider override in subclasses
		default long sum()
		{
			// avoid redirection in wrappers
			PrimitiveIterator.OfLong local = unwrap();
			// exploit arithmetic operator
			long sum = 0;
			while (local.hasNext()) {
				sum += local.nextLong();
			}
			return sum;
		}
	}
}
