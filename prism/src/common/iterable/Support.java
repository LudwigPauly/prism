package common.iterable;

import java.util.BitSet;
import java.util.Iterator;

import common.BitSetTools;
import common.IteratorTools;
import common.functions.Relation;
import common.functions.primitive.AbstractPredicateInteger;
import common.functions.primitive.PredicateDouble;

// FIXME ALG: consider using e.g. Support(values) is equal to
// support = Relation.LEQ.curry(0)
//				.and(Relation.GEQ.curry(values.length))
//				.and(Relation.GEQ.curry(0).compose(values::get)); 
// new FilteredIterable(values, support)
public class Support extends AbstractPredicateInteger implements Iterable<Integer>
{
	private final double[] values;
	private final PredicateDouble predicate;

	// FIXME ALG: check common mathematical definition/terminology
	public Support(final double[] values)
	{
		this(values, 0);
	}

	public Support(final double[] values, final double threshold)
	{
		this(values, Relation.GT, threshold);
	}

	public Support(final double[] values, final Relation relation, final double threshold)
	{
		this(values, relation.inverse().curry(threshold));
	}

	public Support(final double[] values, final PredicateDouble predicate)
	{
		this.values = values;
		this.predicate = predicate;
	}

	@Override
	public final boolean getBoolean(final int index)
	{
		return 0 <= index && index <= values.length && predicate.get(values[index]);
	}

	public BitSet asBitSet()
	{
		return BitSetTools.asBitSet(this);
	}

	public Iterator<Integer> iterator()
	{
		return new FilteringIterator<>(new Interval(values.length), this);
	}

	public static void main(final String[] args)
	{
		final Support support = new Support(new double[] { 0.98, 0.8, 1.0, 0.0 }, Relation.GEQ, 1.0);
		IteratorTools.printIterator("support", support.iterator());

		final Support support1 = new Support(new double[] { 1, 0, 1 });
		final Support support2 = new Support(new double[] { 0, 1, 1 });
		System.out.println(support1.and(support2).get(0));
		System.out.println(support1.and(support2).get(1));
		System.out.println(support1.and(support2).get(2));
	}
}