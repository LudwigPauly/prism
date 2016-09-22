package explicit;

import java.util.BitSet;
import java.util.function.IntFunction;

import common.iterable.IterableBitSet;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.PrismException;

//FIXME ALG: add comment
public class BasicModelTransformation<OM extends Model, TM extends Model> implements ModelTransformation<OM, TM>
{
	public static final IntFunction<Integer> IDENTITY = Integer::valueOf;

	public static final boolean DEFAULT_BOOLEAN = false;
	public static final double DEFAULT_DOUBLE   = Double.NaN;
	public static final int DEFAULT_INTEGER     = -1;

	protected final OM originalModel;
	protected final TM transformedModel;
	protected BitSet transformedStatesOfInterest;
	protected final IntFunction<Integer> mapToTransformedModel;

	protected final int numberOfStates;

	public BasicModelTransformation(final OM originalModel, final TM transformedModel)
	{
		this(originalModel, transformedModel, null);
	}

	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest)
	{
		this(originalModel, transformedModel, transformedStatesOfInterest, IDENTITY);
	}

	public BasicModelTransformation(final ModelTransformation<? extends OM, ? extends TM> transformation)
	{
		originalModel               = transformation.getOriginalModel();
		transformedModel            = transformation.getTransformedModel();
		transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();
		if (transformation instanceof BasicModelTransformation) {
			mapToTransformedModel   = ((BasicModelTransformation<?,?>) transformation).mapToTransformedModel;
		} else {
			mapToTransformedModel   = transformation::mapToTransformedModel;
		}
		numberOfStates              = originalModel.getNumStates();
	}

	@Deprecated
	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest, Integer[] mapToTransformedModel)
	{
		this(originalModel, transformedModel, transformedStatesOfInterest, state -> mapToTransformedModel[state]);
	}

	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest, final IntFunction<Integer> mapToTransformedModel)
	{
		this.originalModel               = originalModel;
		this.transformedModel            = transformedModel;
		this.numberOfStates              = originalModel.getNumStates();
		this.mapToTransformedModel       = mapToTransformedModel;
		setTransformedStatesOfInterest(transformedStatesOfInterest);
	}

	@Override
	public OM getOriginalModel()
	{
		return originalModel;
	}

	@Override
	public TM getTransformedModel()
	{
		return transformedModel;
	}

	@Override
	public BitSet getTransformedStatesOfInterest()
	{
		return transformedStatesOfInterest;
	}

	public BasicModelTransformation<OM, TM> setTransformedStatesOfInterest(BitSet transformedStatesOfInterest)
	{
		if (transformedStatesOfInterest != null && transformedStatesOfInterest.length() > transformedModel.getNumStates()) {
			throw new IndexOutOfBoundsException("State set must be subset of transformed model's state space");
		}
		this.transformedStatesOfInterest = transformedStatesOfInterest;
		return this;
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
	{
		if (sv.getType() instanceof TypeBool) {
			assert(sv.getBitSet() != null) : "State values are undefined.";

			final BitSet mapped = projectToOriginalModel(sv.getBitSet());
			return StateValues.createFromBitSet(mapped, originalModel);
		}
		if (sv.getType() instanceof TypeDouble) {
			assert(sv.getDoubleArray() != null) : "State values are undefined.";

			final double[] mapped = projectToOriginalModel(sv.getDoubleArray());
			return StateValues.createFromDoubleArray(mapped, originalModel);
		}
		if (sv.getType() instanceof TypeInt) {
			assert(sv.getIntArray() != null) : "State values are undefined.";

			final int[] mapped = projectToOriginalModel(sv.getIntArray());
			return StateValues.createFromIntegerArray(mapped, originalModel);
		}
		throw new PrismException("Unsupported type of state values");
	}

	public BitSet projectToOriginalModel(final BitSet values)
	{
		final BitSet result = new BitSet(numberOfStates);

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final boolean mappedValue = (mappedState == null) ? DEFAULT_BOOLEAN : values.get(mappedState);
			result.set(state, mappedValue);
		}
		return result;
	}

	public double[] projectToOriginalModel(final double[] values)
	{
		final double[] result = new double[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final double mappedValue = (mappedState == null) ? DEFAULT_DOUBLE : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	public int[] projectToOriginalModel(final int[] values)
	{
		final int[] result = new int[numberOfStates];

		for (int state = 0; state < numberOfStates; state++) {
			final Integer mappedState = mapToTransformedModel(state);
			final int mappedValue = (mappedState == null) ? DEFAULT_INTEGER : values[mappedState];
			result[state] = mappedValue;
		}
		return result;
	}

	@Override
	public Integer mapToTransformedModel(final int state)
	{
		if (state >= numberOfStates) {
			throw new IndexOutOfBoundsException("State index does not belong to original model.");
		}
		return mapToTransformedModel.apply(state);
	}

	@Override
	public BitSet mapToTransformedModel(final BitSet states)
	{
		final BitSet result = new BitSet();

		for (int state : new IterableBitSet(states)) {
			final Integer mappedState = mapToTransformedModel(state);
			if (mappedState != null) {
				result.set(mappedState);
			}
		}
		return result;
	}

	public <M extends Model> BasicModelTransformation<M, TM> compose(final ModelTransformation<M, ? extends OM> inner)
	{
		IntFunction<Integer> innerMapping;
		if (inner instanceof BasicModelTransformation) {
			innerMapping   = ((BasicModelTransformation<?,?>) inner).mapToTransformedModel;
		} else {
			innerMapping   = inner::mapToTransformedModel;
		}
		IntFunction<Integer> composed = new IntFunction<Integer>()
		{
			@Override
			public Integer apply(int state) {
				Integer intermediate = innerMapping.apply(state);
				return (intermediate == null) ? null : mapToTransformedModel.apply(intermediate);
			}
		};
		return new BasicModelTransformation<>(inner.getOriginalModel(), transformedModel, transformedStatesOfInterest, composed);
	}
}