package prism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import common.iterable.FunctionalIterable;
import common.iterable.FunctionalPrimitiveIterable.OfInt;
import common.iterable.IterableArray;
import common.iterable.Range;
import common.iterable.Reducible;
import explicit.*;
import explicit.SCCComputer;
import jdd.JDD;
import jdd.JDDNode;
import param.Function;
import param.ParamModel;
import param.ParamModelChecker;

/**
 * Storage for the steady-state probabilities for each BSCC of a model.
 */
public interface SteadyStateProbs<S, V>
{
	S addBSCC(S bscc);

	SteadyStateProbs<S, V> clear();

	FunctionalIterable<S> getBSCCs();

	S getBsccStates();

	S getNonBsccStates();

	int getNumberOfBSCCs();

	V getSteadyStateProbabilities();

	SteadyStateProbs<S, V> trimToSize();



	public abstract class SteadyStateProbsExplicit implements SteadyStateProbs<BitSet, double[]>
	{
		protected int numberOfBSCCs;
		protected int numberOfStates;
		protected double[] bsccProbs;  // steady-state probabilities for all BSCCs

		public SteadyStateProbsExplicit(DTMC model, double[] bsccProbs)
		{
			this.bsccProbs = bsccProbs;
			numberOfBSCCs  = 0;
			numberOfStates = model.getNumStates();
		}

		@Override
		public SteadyStateProbsExplicit clear()
		{
			numberOfBSCCs  = 0;
			numberOfStates = 0;
			bsccProbs = new double[0];
			return this;
		}

		@Override
		public BitSet getNonBsccStates()
		{
			BitSet nonBsccStates = getBsccStates();
			nonBsccStates.flip(0, numberOfStates);
			return nonBsccStates;
		}
	}

	public abstract class SteadyStateProbsParam implements SteadyStateProbs<BitSet, Function[]>
	{
		protected int numberOfBSCCs;
		protected int numberOfStates;
		protected Function[] bsccProbs;  // steady-state probabilities for all BSCCs

		public SteadyStateProbsParam(ParamModel model, Function[] bsccProbs)
		{
			this.bsccProbs = bsccProbs;
			numberOfBSCCs  = 0;
			numberOfStates = model.getNumStates();
		}

		@Override
		public SteadyStateProbsParam clear()
		{
			numberOfBSCCs  = 0;
			numberOfStates = 0;
			bsccProbs = new Function[0];
			return this;
		}

		@Override
		public BitSet getNonBsccStates()
		{
			BitSet nonBsccStates = getBsccStates();
			nonBsccStates.flip(0, numberOfStates);
			return nonBsccStates;
		}
	}

	public static class SteadyStateProbsCompact extends SteadyStateProbsExplicit
	{

		protected int[] bsccIndices;   // states of all BSCCs, descending order
		protected int[] bsccOffsets;   // offsets bscc i: bsccOffsets[i], [bsccOffsets[i+1]

		public SteadyStateProbsCompact(DTMC model, double[] bsccProbs)
		{
			super(model, bsccProbs);
			bsccIndices   = new int[numberOfStates];
			bsccOffsets   = new int[numberOfStates + 1];
		}

		@Override
		public BitSet addBSCC(BitSet bscc)
		{
			int lower = bsccOffsets[numberOfBSCCs];
			int upper = lower + bscc.cardinality();
			numberOfBSCCs++;
			bsccOffsets[numberOfBSCCs] = upper;
			int state = -1;
			// copy bscc in decending order
			for (int i = upper - 1; i >= lower; i--) {
				state = bscc.nextSetBit(state + 1);
				bsccIndices[i] = state;
			}
			return bscc;
		}

		@Override
		public SteadyStateProbsCompact clear()
		{
			bsccIndices = new int[0];
			bsccOffsets = new int[1];
			return this;
		}

		@Override
		public FunctionalIterable<BitSet> getBSCCs()
		{
			Range indices = new Range(numberOfBSCCs);
			FunctionalIterable<OfInt> bsccs = indices.map((int i) -> new IterableArray.OfInt(bsccIndices, bsccOffsets[i], bsccOffsets[i+1]));
			return bsccs.map(bscc -> bscc.collect(new BitSet()));
		}

		@Override
		public BitSet getBsccStates()
		{
			return new IterableArray.OfInt(bsccIndices).collect(new BitSet(numberOfStates));
		}

		@Override
		public int getNumberOfBSCCs()
		{
			return numberOfBSCCs;
		}

		@Override
		public double[] getSteadyStateProbabilities()
		{
			return bsccProbs;
		}

		@Override
		public SteadyStateProbsCompact trimToSize()
		{
			bsccIndices = Arrays.copyOf(bsccIndices, bsccOffsets[numberOfBSCCs]);
			bsccOffsets = Arrays.copyOf(bsccOffsets, numberOfBSCCs + 1);
			return this;
		}
	}

	public static class SteadyStateProbsCompactParam extends SteadyStateProbsParam
	{

		protected int[] bsccIndices;   // states of all BSCCs, descending order
		protected int[] bsccOffsets;   // offsets bscc i: bsccOffsets[i], [bsccOffsets[i+1]

		public SteadyStateProbsCompactParam(ParamModel model, Function[] bsccProbs)
		{
			super(model, bsccProbs);
			bsccIndices   = new int[numberOfStates];
			bsccOffsets   = new int[numberOfStates + 1];
		}

		@Override
		public BitSet addBSCC(BitSet bscc)
		{
			int lower = bsccOffsets[numberOfBSCCs];
			int upper = lower + bscc.cardinality();
			numberOfBSCCs++;
			bsccOffsets[numberOfBSCCs] = upper;
			int state = -1;
			// copy bscc in decending order
			for (int i = upper - 1; i >= lower; i--) {
				state = bscc.nextSetBit(state + 1);
				bsccIndices[i] = state;
			}
			return bscc;
		}

		@Override
		public SteadyStateProbsCompactParam clear()
		{
			bsccIndices = new int[0];
			bsccOffsets = new int[1];
			return this;
		}

		@Override
		public FunctionalIterable<BitSet> getBSCCs()
		{
			Range indices = new Range(numberOfBSCCs);
			FunctionalIterable<OfInt> bsccs = indices.map((int i) -> new IterableArray.OfInt(bsccIndices, bsccOffsets[i], bsccOffsets[i+1]));
			return bsccs.map(bscc -> bscc.collect(new BitSet()));
		}

		@Override
		public BitSet getBsccStates()
		{
			return new IterableArray.OfInt(bsccIndices).collect(new BitSet(numberOfStates));
		}

		@Override
		public int getNumberOfBSCCs()
		{
			return numberOfBSCCs;
		}

		@Override
		public Function[] getSteadyStateProbabilities()
		{
			return bsccProbs;
		}

		@Override
		public SteadyStateProbsCompactParam trimToSize()
		{
			bsccIndices = Arrays.copyOf(bsccIndices, bsccOffsets[numberOfBSCCs]);
			bsccOffsets = Arrays.copyOf(bsccOffsets, numberOfBSCCs + 1);
			return this;
		}
	}



	public static class SteadyStateProbsSimple extends SteadyStateProbsExplicit
	{
		protected ArrayList<BitSet> bsccs;  // list of BSCCs

		public SteadyStateProbsSimple(DTMC model, double[] bsccProbs)
		{
			super(model, bsccProbs);
			bsccs          = new ArrayList<>();
		}

		@Override
		public BitSet addBSCC(BitSet bscc)
		{
			numberOfBSCCs++;
			bsccs.add(bscc);
			return bscc;
		}

		@Override
		public SteadyStateProbsSimple clear()
		{
			super.clear();
			bsccs.clear();
			return this;
		}

		@Override
		public BitSet getBsccStates()
		{
			BitSet bsccStates = new BitSet(numberOfStates);
			for (BitSet bscc : bsccs) {
				bsccStates.or(bscc);
			}
			return bsccStates;
		}

		@Override
		public int getNumberOfBSCCs()
		{
			return numberOfBSCCs;
		}

		@Override
		public double[] getSteadyStateProbabilities()
		{
			return bsccProbs;
		}

		@Override
		public FunctionalIterable<BitSet> getBSCCs()
		{
			return Reducible.extend(bsccs);
		}

		@Override
		public SteadyStateProbsSimple trimToSize()
		{
			bsccs.trimToSize();
			return this;
		}
	}

	public static class SteadyStateProbsSimpleParam extends SteadyStateProbsParam
	{
		protected ArrayList<BitSet> bsccs;  // list of BSCCs

		public SteadyStateProbsSimpleParam(ParamModel model, Function[] bsccProbs)
		{
			super(model, bsccProbs);
			bsccs = new ArrayList<>();
		}

		@Override
		public BitSet addBSCC(BitSet bscc)
		{
			numberOfBSCCs++;
			bsccs.add(bscc);
			return bscc;
		}

		@Override
		public SteadyStateProbsSimpleParam clear()
		{
			super.clear();
			bsccs.clear();
			return this;
		}

		@Override
		public BitSet getBsccStates()
		{
			BitSet bsccStates = new BitSet(numberOfStates);
			for (BitSet bscc : bsccs) {
				bsccStates.or(bscc);
			}
			return bsccStates;
		}

		@Override
		public int getNumberOfBSCCs()
		{
			return numberOfBSCCs;
		}

		@Override
		public Function[] getSteadyStateProbabilities()
		{
			return bsccProbs;
		}

		@Override
		public FunctionalIterable<BitSet> getBSCCs()
		{
			return Reducible.extend(bsccs);
		}

		@Override
		public SteadyStateProbsSimpleParam trimToSize()
		{
			bsccs.trimToSize();
			return this;
		}
	}

	public class SteadyStateProbsSymbolic implements SteadyStateProbs<JDDNode, prism.StateValues>, Cloneable
	{
		protected int numberOfBSCCs;
		protected prism.StateValues bsccProbs;  // steady-state probabilities for all BSCCs
		protected ArrayList<JDDNode> bsccs;     // list of BSCCs
		protected JDDNode nonBsccStates;

		public SteadyStateProbsSymbolic(prism.StateValues bsccProbs)
		{
			this.bsccProbs = bsccProbs;
			numberOfBSCCs  = 0;
			bsccs          = new ArrayList<>();
		}

		@Override
		public JDDNode addBSCC(JDDNode bscc)
		{
			numberOfBSCCs++;
			bsccs.add(bscc);
			return bscc;
		}

		@Override
		public SteadyStateProbsSymbolic clear()
		{
			if (bsccProbs != null) {
				bsccProbs.clear();
				bsccProbs = null;
			}
			bsccs.forEach(JDD::Deref);
			bsccs.clear();
			if (nonBsccStates != null) {
				JDD.Deref(nonBsccStates);
				nonBsccStates = null;
			}
			return this;
		}

		@SuppressWarnings("unchecked")
		@Override
		public SteadyStateProbsSymbolic clone()
		{
			try {
				SteadyStateProbsSymbolic clone = (SteadyStateProbsSymbolic) super.clone();
				clone.bsccs = (ArrayList<JDDNode>) bsccs.clone();
				return clone;
			} catch (CloneNotSupportedException e) {
				throw new InternalError("Object#clone is expected to work for Cloneable objects.", e);
			}
		}

		public SteadyStateProbsSymbolic deepCopy() throws PrismException
		{
			SteadyStateProbsSymbolic clone = clone();
			clone.bsccProbs = bsccProbs.deepCopy();
			clone.bsccs.replaceAll(JDDNode::copy);
			clone.nonBsccStates = nonBsccStates.copy();
			return clone;
		}

		@Override
		public JDDNode getBsccStates()
		{
			JDDNode bsccStates = JDD.Constant(0);
			for (JDDNode bscc : bsccs) {
				JDD.Or(bsccStates, bscc.copy());
			}
			return bsccStates;
		}

		@Override
		public JDDNode getNonBsccStates()
		{
			return nonBsccStates;
		}

		@Override
		public int getNumberOfBSCCs()
		{
			return numberOfBSCCs;
		}

		@Override
		public prism.StateValues getSteadyStateProbabilities()
		{
			return bsccProbs;
		}

		@Override
		public FunctionalIterable<JDDNode> getBSCCs()
		{
			return Reducible.extend(bsccs);
		}

		@Override
		public SteadyStateProbsSymbolic trimToSize()
		{
			bsccs.trimToSize();
			return this;
		}

		public SteadyStateProbsSymbolic setNonBsccStates(JDDNode nonBsccStates)
		{
			this.nonBsccStates = nonBsccStates;
			return this;
		}
	}

	public static SteadyStateProbsCompact computeCompact(MCModelChecker mc, DTMC dtmc) throws PrismException
	{
		double[] probs = new double[dtmc.getNumStates()];
		return compute(mc, dtmc, new SteadyStateProbsCompact(dtmc, probs));
	}
	public static SteadyStateProbsCompactParam computeCompactParam(ParamModelChecker mc, ParamModel model) throws PrismException
	{
		Function[] probs = new Function[model.getNumStates()];
		return computeParam(mc, model, new SteadyStateProbsCompactParam(model, probs));
	}

	public static SteadyStateProbsSimple computeSimple(MCModelChecker mc, DTMC dtmc) throws PrismException
	{
		double[] probs = new double[dtmc.getNumStates()];
		return compute(mc, dtmc, new SteadyStateProbsSimple(dtmc, probs));
	}

	public static SteadyStateProbsSimpleParam computeSimpleParam(ParamModelChecker mc, ParamModel model) throws PrismException
	{
		Function[] probs = new Function[model.getNumStates()];
		return computeParam(mc, model, new SteadyStateProbsSimpleParam(model, probs));
	}

	@SuppressWarnings("unchecked")
	public static <T extends SteadyStateProbsExplicit> T compute(MCModelChecker mc, DTMC dtmc, T steadyStateProbs) throws PrismException
	{
		BSCCConsumer consumer = new BSCCConsumer((PrismComponent) mc, dtmc)
		{
			double[] probs = steadyStateProbs.getSteadyStateProbabilities();
			int i = 0;

			@Override
			public void notifyNextBSCC(BitSet bscc) throws PrismException
			{
				steadyStateProbs.addBSCC(bscc);
				mainLog.println("\nComputing steady state probabilities for BSCC " + i);
				mc.computeSteadyStateProbsForBSCC(dtmc, bscc, probs);
				i++;
			}
		};
		SCCComputer.createSCCComputer((PrismComponent) mc, dtmc, consumer).computeSCCs();

		return (T) steadyStateProbs.trimToSize();
	}

	public static <T extends SteadyStateProbsParam> T computeParam(ParamModelChecker mc, ParamModel model, T steadyStateProbs) throws PrismException
	{
		// Compute BSCCs
		SCCConsumerStore sccStore = new SCCConsumerStore();
		explicit.SCCComputer sccComputer = explicit.SCCComputer.createSCCComputer((PrismComponent) mc, model, sccStore);
		sccComputer.computeSCCs();
		List<BitSet> bsccs = sccStore.getBSCCs();
		int numBSCCs = bsccs.size();


		// Compute steady-state probabilities for each BSCC
		for (int b = 0; b < numBSCCs; b++) {
			BitSet bscc = bsccs.get(b);
			// Compute steady-state probabilities for the BSCC
			Function[] tmp = mc.computeSteadyStateProbsForBSCC(model, bscc);
			int k = 0;
			for (int i = bscc.nextSetBit(0); i >= 0; i = bscc.nextSetBit(i + 1)) {
				steadyStateProbs.bsccProbs[i] = tmp[k];
				k++;
			}
			steadyStateProbs.addBSCC(bscc);
		}

		return (T) steadyStateProbs.trimToSize();
	}

	public static SteadyStateProbsSymbolic computeSymbolic(ProbModelChecker mc) throws PrismException
	{
		prism.StateValues probs = mc.createVector(0);
		SteadyStateProbsSymbolic steadyStateProbs = new SteadyStateProbsSymbolic(probs);

		int i = 0;
		prism.SCCComputer sccComputer = mc.getSccComputer();
		sccComputer.computeBSCCs();
		for (JDDNode bscc : sccComputer.getBSCCs()) {
			steadyStateProbs.addBSCC(bscc);
			mc.getLog().println("\nComputing steady state probabilities for BSCC " + i);
			prism.StateValues bsccProbs = mc.computeSteadyStateProbsForBSCC(bscc);
			probs.add(bsccProbs);
			bsccProbs.clear();
			i++;
		}
		steadyStateProbs.setNonBsccStates(sccComputer.getNotInBSCCs());

		return steadyStateProbs.trimToSize();
	}
}