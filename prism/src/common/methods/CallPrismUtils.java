package common.methods;

import prism.PrismUtils;
import common.functions.primitive.PairPredicateDoubleDouble;
import common.functions.primitive.TriplePredicateDoubleDoubleDouble;

/**
 * @deprecated
 * Use J8 Functions instead.
 */
@Deprecated
public class CallPrismUtils
{
	public static class Static
	{

		private static final TriplePredicateDoubleDoubleDouble DOUBLES_ARE_CLOSE_ABS = new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public boolean getBoolean(final double d1, final double d2, final double epsilon)
			{
				return PrismUtils.doublesAreCloseAbs(d1, d2, epsilon);
			}
		};

		private static final TriplePredicateDoubleDoubleDouble DOUBLES_ARE_CLOSE_REL = new TriplePredicateDoubleDoubleDouble()
		{
			@Override
			public boolean getBoolean(final double d1, final double d2, final double epsilon)
			{
				return PrismUtils.doublesAreCloseRel(d1, d2, epsilon);
			}
		};

		public static TriplePredicateDoubleDoubleDouble doublesAreCloseAbs()
		{
			return DOUBLES_ARE_CLOSE_ABS;
		}

		public static TriplePredicateDoubleDoubleDouble doublesAreCloseRel()
		{
			return DOUBLES_ARE_CLOSE_REL;
		}

		public static PairPredicateDoubleDouble doublesAreCloseAbs(final double epsilon)
		{
			return new PairPredicateDoubleDouble()
			{
				@Override
				public final boolean test(final double d1, final double d2)
				{
					return DOUBLES_ARE_CLOSE_ABS.apply(d1, d2, epsilon);
				}
			};
		}

		public static PairPredicateDoubleDouble doublesAreCloseRel(final double epsilon)
		{
			return new PairPredicateDoubleDouble()
			{
				@Override
				public final boolean test(final double d1, final double d2)
				{
					return DOUBLES_ARE_CLOSE_REL.apply(d1, d2, epsilon);
				}
			};
		}
	}
}