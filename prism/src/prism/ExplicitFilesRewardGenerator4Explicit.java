package prism;

import java.io.File;
import java.util.List;

/**
 * This Class extends the ExplicitFilesRewardGenerator for the explicit engine.
 */

public class ExplicitFilesRewardGenerator4Explicit extends ExplicitFilesRewardGenerator
{
	protected double[][] stateRewards; // state reward structures

	public ExplicitFilesRewardGenerator4Explicit(PrismComponent parent)
	{
		super(parent);
	}

	public ExplicitFilesRewardGenerator4Explicit(PrismComponent parent, List<File> stateRewardsFile, int numStates) throws PrismException
	{
		super(parent, stateRewardsFile, numStates);
	}

	/**
	 * Initialize reward structure storage for explicit format.
	 */
	protected void initStorage()
	{
		// init state reward structure storage
		stateRewards = new double[stateRewardsFile.size()][numStates];
	}

	/**
	 * Stores the state rewards in the required format for explicit.
	 *
	 * @param rewardStructIndex reward structure index
	 * @param i state index
	 * @param d reward value
	 */
	protected void storeReward(int rewardStructIndex, int i, double d)
	{
		stateRewards[rewardStructIndex][i] = d;
	}

	/**
	 * Getter of state reward in explicit format.
	 *
	 * @param r The index of the reward structure to use
	 * @param s The index of the state in which to evaluate the rewards
	 * @return state reward
	 * @throws PrismException can be thrown by extractStateRewards()
	 */
	@Override
	public double getStateReward(int r, int s) throws PrismException
	{
		if (!stateRewardsLoaded) {
			extractStateRewards();
		}
		return stateRewards[r][s];
	}
}