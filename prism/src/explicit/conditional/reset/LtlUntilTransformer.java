package explicit.conditional.reset;

import java.util.BitSet;

import acceptance.AcceptanceType;
import common.BitSetTools;
import explicit.CTMCModelChecker;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.reset.GoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.reset.GoalFailStopTransformer.ProbabilisticRedistribution;
import explicit.conditional.transformer.LtlProductTransformer.LabeledDA;
import explicit.statebased.SimplePathEvent.Finally;
import explicit.statebased.SimplePathEvent.Reach;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;

// FIXME ALG: add comment
public interface LtlUntilTransformer<M extends Model, C extends ProbModelChecker> extends NormalFormTransformer<M, C>
{
	// FIXME ALG: Generalize acceptance types: all
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		return getLtlTransformer().canHandle(model, objective.getExpression());
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
	{
		Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		Expression until = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: build omega automaton
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTemp = objective.getExpression();
		LabeledDA objectiveDA    = getLtlTransformer().constructDA(model, objectiveTemp, ACCEPTANCE_TYPES);

		// 2) Condition: compute simple path property
		Expression conditionTemp = expression.getCondition();
		Reach<M> conditionPath   = (Reach<M>) computeSimplePathProperty(model, conditionTemp);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>,ExpressionConditional> result = transformNormalForm(objectiveDA, conditionPath, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded);
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LabeledDA objectiveDA, Reach<M> conditionPath, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Objective
		LTLProduct<M> product = getLtlTransformer().constructProduct(conditionPath.getModel(), objectiveDA, statesOfInterest);
		//    Lift state sets to product model
		Reach<M> conditionPathProduct = conditionPath.lift(product);

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(product, conditionPathProduct);

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LTLProduct<M> product, Reach<M> conditionPath)
			throws PrismException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReach(LTLProduct<M> product, Reach<M> conditionPath)
			throws PrismException
	{
		M productModel           = product.getProductModel();
		BitSet statesOfInterest  = product.getTransformedStatesOfInterest();
		BitSet acceptStates      = getLtlTransformer().findAcceptingStates(product);
		Finally<M> objectivePath = new Finally<M>(productModel, acceptStates);

		// FIXME ALG: reuse computation of conditionSatisfied?
		FinallyUntilTransformer<M, C> finallyUntilTransformer         = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = finallyUntilTransformer.transformNormalForm(objectivePath, conditionPath, statesOfInterest);
		finallyUntilTransformer.clear();
		return result;
	}

	FinallyUntilTransformer<M, C> getFinallyUntilTransformer();



	public static class CTMC extends NormalFormTransformer.CTMC implements LtlUntilTransformer<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.CTMC>, ExpressionConditional> transformNormalForm(LTLProduct<explicit.CTMC> product, Reach<explicit.CTMC> conditionPath)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, conditionPath);
		}

		@Override
		public FinallyUntilTransformer.CTMC getFinallyUntilTransformer()
		{
			return new FinallyUntilTransformer.CTMC(getModelChecker());
		}
	}



	public static class DTMC extends NormalFormTransformer.DTMC implements LtlUntilTransformer<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.DTMC>, ExpressionConditional> transformNormalForm(LTLProduct<explicit.DTMC> product, Reach<explicit.DTMC> conditionPath)
				throws PrismException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, conditionPath);
		}

		@Override
		public FinallyUntilTransformer.DTMC getFinallyUntilTransformer()
		{
			return new FinallyUntilTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NormalFormTransformer.MDP implements LtlUntilTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		/**
		 * 1) search all accepting ECs and<br>
		 * 2) refine in {@code AccEC \ (Pmax=0(condition) | Pmin=1(condition))}.
		 */
		@Override
		public Pair<GoalFailStopTransformation<explicit.MDP>, ExpressionConditional> transformNormalForm(LTLProduct<explicit.MDP> product, Reach<explicit.MDP> conditionPath)
				throws PrismException
		{
			conditionPath.requireSameModel(product.getProductModel());

			if (product.getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReach(product, conditionPath);
			}

			explicit.MDP productModel = product.getProductModel();
			BitSet statesOfInterest   = product.getTransformedStatesOfInterest();

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			BitSet conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

			// compute badStates
			BitSet badStates = computeBadStates(conditionPath, conditionFalsifiedStates);

			// compute accepting states  (ECs or REACH states)
			BitSet acceptStates                 = getLtlTransformer().findAcceptingStates(product);
			Finally<explicit.MDP> objectivePath = new Finally<>(productModel, acceptStates);

			// compute redistribution for satisfied condition
			ProbabilisticRedistribution conditionSatisfied = redistributeProb1(conditionPath, objectivePath);

			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionPath);

			// compute states where objective and condition can be satisfied
			BitSet instantGoalStates = computeInstantGoalStates(product, acceptStates, objectiveFalsified.getStates(), conditionPath, conditionSatisfied.getStates(), conditionFalsifiedStates);

			// transform goal-fail-stop
			ProbabilisticRedistribution objectiveSatisfied          = new ProbabilisticRedistribution();
			GoalFailStopTransformation<explicit.MDP> transformation = transformGoalFailStop(productModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, badStates, statesOfInterest);

			// transform expression
			ExpressionLabel goal                        = new ExpressionLabel(transformation.getGoalLabel());
			ExpressionTemporal transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective         = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			ExpressionTemporal transformedCondition;
			if (conditionPath.isCoSafe()) {
				// All paths satisfying the condition eventually reach the goal or stop state.
				ExpressionLabel stopLabel = new ExpressionLabel(transformation.getStopLabel());
				transformedCondition      = Expression.Finally(Expression.Parenth(Expression.Or(goal, stopLabel)));
			} else {
				// All paths violating the condition eventually reach the fail state.
				ExpressionLabel fail      = new ExpressionLabel(transformation.getFailLabel());
				transformedCondition      = Expression.Globally(Expression.Not(fail));
			}
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			return new Pair<>(transformation, transformedExpression);
		}

		public BitSet computeInstantGoalStates(LTLProduct<explicit.MDP> product, BitSet objectiveAcceptStates, BitSet objectiveFalsifiedStates, Reach<explicit.MDP> conditionPath, BitSet conditionSatisfiedStates, BitSet conditionFalsifiedStates)
				throws PrismException
		{
			explicit.MDP productModel = product.getProductModel();
			conditionPath.requireSameModel(productModel);

			BitSet instantGoalStates = BitSetTools.intersect(objectiveAcceptStates, conditionSatisfiedStates);

			// exclude objective/condition falsified states
			BitSet falsifiedStates    = BitSetTools.union(objectiveFalsifiedStates, conditionFalsifiedStates);
			BitSet notFalsifiedStates = BitSetTools.complement(productModel.getNumStates(), falsifiedStates);

			// Does the condition specify behavior in the limit?
			if (!conditionPath.isCoSafe()) {
				// Find accepting ECs that do not already include a normalize state
				BitSet remain       = BitSetTools.minus(objectiveAcceptStates, conditionSatisfiedStates, falsifiedStates);
				BitSet acceptStates = getLtlTransformer().findAcceptingStates(product, remain);
				instantGoalStates.or(acceptStates);
			}
			return getMDPModelChecker().computeProb1E(productModel, false, notFalsifiedStates, instantGoalStates);
		}

		@Override
		public FinallyUntilTransformer.MDP getFinallyUntilTransformer()
		{
			return new FinallyUntilTransformer.MDP(getModelChecker());
		}

		public ProbabilisticRedistribution redistributeProb0Objective(Finally<explicit.MDP> objectivePath, Reach<explicit.MDP> conditionPath)
				throws PrismException
		{
			objectivePath.requireSameModel(conditionPath);

			// Do we have to reset once a state violates the objective?
			if (objectivePath.hasToRemain() || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				return redistributeProb0(objectivePath, conditionPath);
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}
	}
}
