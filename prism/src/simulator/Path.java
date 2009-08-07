//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import java.util.ArrayList;

import parser.*;
import parser.ast.*;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLog;

/**
 * Stores and manipulates a path though a model.
 */
public class Path
{
	class Step
	{
		public Step()
		{
			stateRewards = new double[numRewardStructs];
		}

		public State state;
		public double stateRewards[];
		public double time;
		public int choice;
		public double transitionRewards[];
	}

	// Parent simulator engine
	protected SimulatorEngine engine;
	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	// Model info/stats
	protected int numRewardStructs;
	// The path, i.e. list of states, etc.
	protected ArrayList<Step> path;
	// The path length
	protected int size;

	// Path totals (time/rewards)
	protected double totalTime;
	protected double totalReward[];
	protected double totalStateReward[];
	protected double totalTransitionReward[];

	// ACCESSORS

	public int size()
	{
		return size;
	}

	public State getState(int i)
	{
		return path.get(i).state;
	}

	public double getStateReward(int i, int j)
	{
		return path.get(i).stateRewards[j];
	}

	public State getCurrentState()
	{
		return getState(size - 1);
	}

	public double getTime(int i)
	{
		return path.get(i).time;
	}

	public int getChoice(int i)
	{
		return path.get(i).choice;
	}

	public double getTransitionReward(int i, int j)
	{
		return path.get(i).transitionRewards[j];
	}

	/**
	* Constructor: creates a new (empty) Path object for a specific model.
	*/
	public Path(SimulatorEngine engine, ModulesFile modulesFile)
	{
		// Store ptr to engine
		this.engine = engine;
		// Store model and info
		this.modulesFile = modulesFile;
		numRewardStructs = modulesFile.getNumRewardStructs();
		// Create arrays to store totals
		totalReward = new double[numRewardStructs];
		totalStateReward = new double[numRewardStructs];
		totalTransitionReward = new double[numRewardStructs];
		// Initialise variables
		clear();
	}

	/**
	 * Clear the path
	 */
	protected void clear()
	{
		// Initialise path totals
		size = 0;
		totalTime = 0.0;
		for (int i = 0; i < numRewardStructs; i++) {
			totalReward[i] = 0.0;
			totalStateReward[i] = 0.0;
			totalTransitionReward[i] = 0.0;
		}
	}

	/**
	 * Initialise the path with an initial state and rewards.
	 */
	public void initialise(State initialState, double[] initialStateRewards)
	{
		clear();
		path = new ArrayList<Step>(100);
		// Add new step item to the path
		Step step = new Step();
		path.add(step);
		size++;
		// Add (copies of) initial state and state rewards to new step
		step.state = new State(initialState);
		step.stateRewards = initialStateRewards.clone();
	}

	/**
	 * Add step...
	 */
	public void addStep(int choice, double[] transRewards, State newState, double[] newStateRewards)
	{
		Step step;
		// Add info to last existing step
		step = path.get(path.size() - 1);
		step.choice = choice;
		step.transitionRewards = transRewards.clone();
		// Add new step item to the path
		step = new Step();
		path.add(step);
		size++;
		// Add (copies of) new state and state rewards to new step
		step.state = new State(newState);
		step.stateRewards = newStateRewards.clone();
	}

	/**
	 * Exports path to a file.
	 * @param log: PrismLog to which the path should be exported to.
	 * @param timeCumul: Show time in cumulative form?
	 * @param colSep: String used to separate columns in display
	 * @param vars: Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportToLog(PrismLog log, boolean timeCumul, String colSep, ArrayList<Integer> vars)
			throws PrismException
	{
		int i, j, n, nv;
		double d, t;
		boolean stochastic = (modulesFile.getModelType() == ModelType.CTMC);
		boolean changed;
		int varsNum = 0, varsIndices[] = null;

		if (modulesFile == null) {
			log.flush();
			log.close();
			return;
		}

		// Get sizes
		n = size();
		nv = engine.getNumVariables();

		// if necessary, store info about which vars to display
		if (vars != null) {
			varsNum = vars.size();
			varsIndices = new int[varsNum];
			for (i = 0; i < varsNum; i++)
				varsIndices[i] = vars.get(i);
		}

		// Write header
		log.print("step");
		if (vars == null)
			for (j = 0; j < nv; j++)
				log.print(colSep + engine.getVariableName(j));
		else
			for (j = 0; j < varsNum; j++)
				log.print(colSep + engine.getVariableName(varsIndices[j]));
		if (stochastic)
			log.print(colSep + (timeCumul ? "time" : "time_in_state"));
		if (numRewardStructs == 1) {
			log.print(colSep + "state_reward" + colSep + "transition_reward");
		} else {
			for (j = 0; j < numRewardStructs; j++)
				log.print(colSep + "state_reward" + (j + 1) + colSep + "transition_reward" + (j + 1));
		}
		log.println();

		// Write path
		t = 0.0;
		for (i = 0; i < n; i++) {
			// (if required) see if relevant vars have changed
			if (vars != null && i > 0) {
				changed = false;
				for (j = 0; j < varsNum; j++) {
					if (!getState(i).varValues[j].equals(getState(i - 1).varValues[j]))
						changed = true;
				}
				if (!changed) {
					d = (i < n - 1) ? getTime(i) : 0.0;
					t += d;
					continue;
				}
			}
			// write state index
			log.print(i);
			// write vars
			if (vars == null) {
				for (j = 0; j < nv; j++) {
					log.print(colSep);
					log.print(getState(i).varValues[j]);
				}
			} else {
				for (j = 0; j < varsNum; j++) {
					log.print(colSep);
					log.print(getState(i).varValues[varsIndices[j]]);
				}
			}
			if (stochastic) {
				d = (i < n - 1) ? getTime(i) : 0.0;
				log.print(colSep + (timeCumul ? t : d));
				t += d;
			}
			for (j = 0; j < numRewardStructs; j++) {
				log.print(colSep + ((i < n - 1) ? getStateReward(i, j) : 0.0));
				log.print(colSep + ((i < n - 1) ? getTransitionReward(i, j) : 0.0));
			}
			log.println();
		}

		log.flush();
		log.close();
	}

	/**
	 * Generate string representation.
	 */
	public String toString()
	{
		int i;
		String s = "";
		for (i = 0; i < size; i++) {
			s += getState(i) + "\n";
		}
		return s;
	}
}
