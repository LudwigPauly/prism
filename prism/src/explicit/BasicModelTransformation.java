//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import java.util.BitSet;

import prism.PrismException;

/**
 * Base class for model transformations, providing support for mapping between states.
 */
public class BasicModelTransformation<OM extends Model<?>, TM extends Model<?>> implements ModelTransformation<OM, TM>
{
	public static final IntUnaryOperator IDENTITY = Integer::valueOf;

	protected final OM originalModel;
	protected final TM transformedModel;
	protected BitSet transformedStatesOfInterest;
	protected final IntUnaryOperator mapToTransformedModel;

	protected final int numberOfStates;

	/** Constructor for a model transformation that maps states one-on-one. */
	public BasicModelTransformation(final OM originalModel, final TM transformedModel)
	{
		this(originalModel, transformedModel, IDENTITY);
	}

	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest)
	{
		this(originalModel, transformedModel, transformedStatesOfInterest, IDENTITY);
	}

	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest, int[] mapToTransformedModel)
	{
		this(originalModel, transformedModel, transformedStatesOfInterest, state -> (state == UNDEF) ? UNDEF : mapToTransformedModel[state]);
	}

	/**
	 * Constructor for a model transformation where the state mapping is given by a function
	 */
	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final IntUnaryOperator mapToTransformedModel)
	{
		this.originalModel               = originalModel;
		this.transformedModel            = transformedModel;
		this.numberOfStates              = originalModel.getNumStates();
		this.mapToTransformedModel       = mapToTransformedModel;
	}

	public BasicModelTransformation(final OM originalModel, final TM transformedModel, final BitSet transformedStatesOfInterest, final IntUnaryOperator mapToTransformedModel)
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


	public BasicModelTransformation<OM, TM> setTransformedStatesOfInterest(BitSet transformedStatesOfInterest)
	{
		if (transformedStatesOfInterest != null && transformedStatesOfInterest.length() > transformedModel.getNumStates()) {
			throw new IndexOutOfBoundsException("State set must be subset of transformed model's state space");
		}
		this.transformedStatesOfInterest = transformedStatesOfInterest;
		return this;
	}

	@Override
	public int mapToTransformedModel(final int state)
	{
		if (state >= numberOfStates) {
			throw new IndexOutOfBoundsException("State index does not belong to original model.");
		}
		return mapToTransformedModel.applyAsInt(state);
	}

	@Override
	public StateValues projectToOriginalModel(final StateValues sv) throws PrismException
	{
		return sv.mapToNewModel(originalModel, mapToTransformedModel);
	}

	public <M extends Model<?>> BasicModelTransformation<M, TM> compose(final ModelTransformation<M, ? extends OM> inner)
	{
		IntUnaryOperator innerMapping;
		if (inner instanceof BasicModelTransformation) {
			innerMapping = ((BasicModelTransformation<?,?>) inner).mapToTransformedModel;
		} else {
			innerMapping = inner::mapToTransformedModel;
		}
		IntUnaryOperator composed = mapToTransformedModel. compose(innerMapping);
		return new BasicModelTransformation<>(inner.getOriginalModel(), transformedModel, transformedStatesOfInterest, composed);
	}
}
