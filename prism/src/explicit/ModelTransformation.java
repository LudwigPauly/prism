//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
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

import prism.PrismException;

/**
 * Interface for a model transformation.
 */
public interface ModelTransformation<OriginalModel extends Model<?>, TransformedModel extends Model<?>> {

	public static final int UNDEF = -1;

	/** Get the original model. */
	public OriginalModel getOriginalModel();

	/** Get the transformed model. */
	public TransformedModel getTransformedModel();

	/**
	 * Get the corresponding index of a {@code state} in the transformed model.
	 * This is the index from which a result may be projected to the original model.
	 * If no such state exists, return {@link ModelTransformation#UNDEF}.
	 * Returns the argument if it is {@link ModelTransformation#UNDEF}.
	 *
	 * @param state index in the original model
	 * @return corresponding index in the transformed model or {@link ModelTransformation#UNDEF}
	 */
	public int mapToTransformedModel(int state);

	/**
	 * Take a {@code StateValues} object for the transformed model and
	 * project the values to the original model.
	 * @param svTransformedModel a {@code StateValues} object for the transformed model
	 * @return a corresponding {@code StateValues} object for the original model.
	 **/
	public StateValues projectToOriginalModel(StateValues svTransformedModel) throws PrismException;
}
