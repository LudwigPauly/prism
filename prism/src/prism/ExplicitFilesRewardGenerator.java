//==============================================================================
//	
//	Copyright (c) 2019-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

package prism;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import csv.BasicReader;
import csv.CsvFormatException;
import csv.CsvReader;
import parser.ParseException;
import parser.PrismParser;
import parser.State;

import static csv.BasicReader.LF;

/**
 * This abstract class implements the import and storage of state reward structures.
 *
 * It is possible to import the state rewards structure with a header.
 *
 *	Header format with reward struct name:
 *		# Reward structure: "$1"
 * 		# State rewards
 * 	$1 is the reward struct name
 *
 * 	Header format without reward struct name:
 * 		# State rewards
 */
public abstract class ExplicitFilesRewardGenerator extends PrismComponent implements RewardGenerator
{
	// File(s) to read in rewards from
	protected List<File> stateRewardsFile = new ArrayList<>();
	// Model info
	protected int numStates;
	// State list (optionally)
	protected List<State> statesList = null;

	protected boolean stateRewardsLoaded = false;
	protected String[] stateRewardNames;

	protected static final Pattern Header_pattern = Pattern.compile("# Reward structure: (\"([a-zA-Z0-9]*)\")$");

	/**
	 * Constructor
	 */
	public ExplicitFilesRewardGenerator(PrismComponent parent)
	{
		super(parent);
	}

	/**
	 * Constructor
	 */
	public ExplicitFilesRewardGenerator(PrismComponent parent, List<File> stateRewardsFile, int numStates) throws PrismException
	{
		this(parent);
		this.stateRewardsFile = stateRewardsFile;
		this.numStates = numStates;
	}

	/**
	 * Optionally, provide a list of model states,
	 * so that rewards can be looked up by State object, as well as state index.
	 */
	public void setStatesList(List<State> statesList)
	{
		this.statesList = statesList;
	}

	protected abstract void storeReward(int rewardStructIndex, int i, double d);

	protected abstract void initStorage();

	/**
	 * Extract the state rewards from the files and initialize storage for reward structures.
	 *
	 * @throws PrismException can be thrown by reward extraction
	 */
	protected void extractStateRewards() throws PrismException
	{
		initStorage();
		stateRewardNames = new String[stateRewardsFile.size()];

		// read files and extract state rewards
		for (int k = 0; k < stateRewardsFile.size(); k++) {
			extractStateRewardsFromFile(stateRewardsFile.get(k), k);
		}

		// check if more than one state rewards file is imported
		if (stateRewardsFile.size() > 1) {
			// check if there are unnamed reward structures
			for (String name : stateRewardNames) {
				if (name == null) {
					mainLog.printWarning("Import of multiple state reward structure with no name! Correct indexes are not guaranteed!");
					break;
				}
			}
		}

		stateRewardsLoaded = true;
	}

	/**
	 * Extract the state rewards from the file and store locally.
	 *
	 * @param file state rewards file to extract
	 * @param rewardStructIndex index of reward struct in which the extracted rewards are stored
	 * @throws PrismException will be thrown if state index invalid
	 */
	protected void extractStateRewardsFromFile(File file, int rewardStructIndex) throws PrismException
	{
		if (file == null) {
			return;
		}

		int lineNum = 1;
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {

			// check if reward structure has named
			Pair extractedHeader = extractStateRewardsHeader(in);
			stateRewardNames[rewardStructIndex] = (String) extractedHeader.getValue();
			// get quantity of lines used by header
			lineNum = (int) extractedHeader.getKey();

			// init csv reader
			BasicReader reader = BasicReader.wrap(in).normalizeLineEndings();
			CsvReader csv = new CsvReader(reader, false, true, false, ' ', LF);

			// read state rewards
			for (String[] record : csv) {
				lineNum++;
				if (record[0].equals(""))
					break;
				int i = Integer.parseInt(record[0]);
				if (i < 0 || i >= numStates) {
					throw new PrismException("Invalid state index " + i);
				}
				double d = Double.parseDouble(record[1]);

				storeReward(rewardStructIndex, i, d);
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + stateRewardsFile.get(rewardStructIndex) + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + stateRewardsFile.get(rewardStructIndex) + "\"");
		} catch (CsvFormatException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + stateRewardsFile.get(rewardStructIndex) + "\"");
		} catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of state rewards file \"" + stateRewardsFile.get(rewardStructIndex) + "\"");
		}
	}

	/**
	 * Extract header from state rewards and return their name, when present.
	 *
	 * @param bufferedReader
	 * @return pair of reward struct name and quantity of header lines
	 * @throws IOException can be thrown by buffered reader
	 * @throws PrismException will be thrown if reward structure name is not unique
	 */
	protected Pair<Integer, String> extractStateRewardsHeader(BufferedReader bufferedReader) throws IOException, PrismException
	{
		// set linenum to 1, because it's the minimal quantity of lines read in this method
		int linenum = 1;
		String rewardStructName = null;
		String s = bufferedReader.readLine();
		// Regex for header
		Matcher matcher;
		while (s.matches("#.*")) {
			matcher = Header_pattern.matcher(s);
			// check if reward struct name present
			if (matcher.matches()) {
				// check if reward struct name is identifier
				checkRewardName(matcher.group(2));
				// check if reward struct name unique
				for (String name : stateRewardNames) {
					if (matcher.group(2).equals(name)) {
						throw new PrismException("Reward structure name is not unique");
					}
				}
				rewardStructName = matcher.group(2);
			}
			s = bufferedReader.readLine();
			linenum++;
		}
		return new Pair<>(linenum, rewardStructName);
	}

	/**
	 * Verify that the imported reward struct name is not null and is an identifier.
	 *
	 * @param rewardStructName reward struct name that should be verified
	 * @throws PrismException if name is null or no identifier
	 */
	protected void checkRewardName(String rewardStructName) throws PrismException
	{
		if (rewardStructName == null)
			return;

		ByteArrayInputStream stream = new ByteArrayInputStream(rewardStructName.getBytes());
		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			Prism.getPrismParser();
			try {
				// (Re)start parser
				PrismParser.ReInit(stream);
				// Parse
				boolean success = true;
				try {
					success = rewardStructName.equals(PrismParser.Identifier());
				} catch (ParseException e) {
					success = false;
				}
				if (!success) {
					throw new PrismLangException("Expected identifier but got: " + rewardStructName);
				}
			} finally {
				// release prism parser
				Prism.releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
	}

	// Methods to implement RewardGenerator

	/**
	 * Method to return the reward structure names as list of strings.
	 *
	 * @return list of reward structure names
	 * @throws PrismException can be thrown by extractStateRewards()
	 */

	@Override
	public List<String> getRewardStructNames() throws PrismException
	{
		// extract state rewards if not present
		if (!stateRewardsLoaded) {
			extractStateRewards();
		}
		List<String> RewardStructNames = new ArrayList<>();

		for (int i = 0; i < stateRewardNames.length; i++) {
			RewardStructNames.add(stateRewardNames[i]);
		}
		return RewardStructNames;
	}

	/**
	 * Method to return the reward structure names as string array.
	 *
	 * @return string array of reward structure names
	 * @throws PrismException can be thrown by extractStateRewards()
	 */
	public String[] getStateRewardNames() throws PrismException
	{
		if (!stateRewardsLoaded) {
			extractStateRewards();
		}
		return stateRewardNames;
	}

	@Override
	public int getNumRewardStructs() throws PrismException
	{
		if (!stateRewardsLoaded) {
			extractStateRewards();
		}
		try {
			return stateRewardNames.length;
		} catch (NullPointerException e) {
			return 0;
		}
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int r)
	{
		return false;
	}

	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return (lookup == RewardLookup.BY_STATE_INDEX) || (lookup == RewardLookup.BY_STATE && statesList != null);
	}

	@Override
	public double getStateReward(int r, State state) throws PrismException
	{
		if (statesList == null) {
			throw new PrismException("Reward lookup by State not possible since state list is missing");
		}
		int s = statesList.indexOf(state);
		if (s == -1) {
			throw new PrismException("Unknown state " + state);
		}
		return getStateReward(r, s);
	}
}