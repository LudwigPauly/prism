package explicit.conditional;

import java.util.BitSet;

import acceptance.AcceptanceType;
import common.BitSetTools;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker.LTLProduct;
import explicit.MDPModelChecker;
import explicit.MinMax;
import explicit.Model;
import explicit.ProbModelChecker;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.NewGoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.NewGoalFailStopTransformer.ProbabilisticRedistribution;
import explicit.conditional.SimplePathProperty.Finally;
import explicit.conditional.SimplePathProperty.Reach;
import explicit.conditional.SimplePathProperty.Until;
import explicit.conditional.transformer.LTLProductTransformer.LabeledDA;
import explicit.conditional.transformer.UndefinedTransformationException;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import prism.Pair;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismSettings;



// FIXME ALG: add comment
public interface NewFinallyLtlTransformer<M extends Model, MC extends ProbModelChecker> extends NewNormalFormTransformer<M, MC>
{
	// FIXME ALG: Generalize acceptance types: all
	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.REACH, AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};
//	public static final AcceptanceType[] ACCEPTANCE_TYPES = {AcceptanceType.RABIN, AcceptanceType.GENERALIZED_RABIN, AcceptanceType.STREETT};



	@Override
	default boolean canHandleObjective(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		if (! NewNormalFormTransformer.super.canHandleObjective(model, expression)) {
			return false;
		}
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression normalized    = ExpressionInspector.normalizeExpression(objective.getExpression());
		Expression until         = ExpressionInspector.removeNegation(normalized);
		return ExpressionInspector.isUnboundedSimpleUntilFormula(until);
	}

	@Override
	default boolean canHandleCondition(Model model, ExpressionConditional expression)
			throws PrismLangException
	{
		return getLtlTransformer().canHandle(model, expression.getCondition());
	}

	@Override
	default NormalFormTransformation<M> transformNormalForm(M model, ExpressionConditional expression, BitSet statesOfInterest)
			throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Objective: compute simple path property
		ExpressionProb objective = (ExpressionProb) expression.getObjective();
		Expression objectiveTmp  = objective.getExpression();
		Reach<M> objectivePath   = (Reach<M>) computeSimplePathProperty(model, objectiveTmp);

		// 2) Condition: build omega automaton
		Expression conditionTmp = expression.getCondition();
		LabeledDA conditionDA   = getLtlTransformer().constructDA(model, conditionTmp, ACCEPTANCE_TYPES);

		// 3) Transform model
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(objectivePath, conditionDA, statesOfInterest);
		GoalFailStopTransformation<M> transformation = result.first;
		ExpressionConditional transformedExpression  = result.second;

		// 4) Inherit bounds
		ExpressionProb transformedObjective        = (ExpressionProb) transformedExpression.getObjective();
		ExpressionProb transformedObjectiveBounded = new ExpressionProb(transformedObjective.getExpression(), objective.getMinMax(), objective.getRelOp().toString(), objective.getBound());
		ExpressionConditional transformedExpressionBounded = new ExpressionConditional(transformedObjectiveBounded, transformedExpression.getCondition());

		return new NormalFormTransformation<>(transformation, expression, transformedExpressionBounded, transformation.getFailLabel(), transformation.getBadLabel());
	}

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(Reach<M> objectivePath, LabeledDA conditionDA, BitSet statesOfInterest)
			throws PrismException
	{
		// 1) LTL Product Transformation for Condition
		LTLProduct<M> product = getLtlTransformer().constructProduct(objectivePath.getModel(), conditionDA, statesOfInterest);
		//    Lift state sets to product model
		Reach<M> objectivePathProduct = objectivePath.lift(product);

		// 2) Normal-Form Transformation
		Pair<GoalFailStopTransformation<M>, ExpressionConditional> result = transformNormalForm(product, objectivePathProduct);

		// 3) Compose Transformations
		GoalFailStopTransformation<M> transformation = result.first.chain(product);
		ExpressionConditional transformedExpression  = result.second;

		return new Pair<>(transformation, transformedExpression);
	}

	Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalForm(LTLProduct<M> product, Reach<M> objectivePath)
			throws PrismException, UndefinedTransformationException;

	default Pair<GoalFailStopTransformation<M>, ExpressionConditional> transformNormalFormReach(LTLProduct<M> product, Reach<M> objectivePath)
			throws PrismException
	{
		objectivePath.requireSameModel(product.getProductModel());

		// FIXME ALG: remove redundancy?
		M productModel           = product.getProductModel();
		BitSet statesOfInterest  = product.getTransformedStatesOfInterest();
		BitSet acceptStates      = getLtlTransformer().findAcceptingStates(product);
		Finally<M> conditionPath = new Finally<M>(productModel, acceptStates);

		NewFinallyUntilTransformer<M, MC> finallyUntilTransformer = getFinallyUntilTransformer();
		getLog().println("\nDelegating to " + finallyUntilTransformer.getName());
		return finallyUntilTransformer.transformNormalForm(objectivePath, conditionPath, statesOfInterest);
	}

	NewFinallyUntilTransformer<M, MC> getFinallyUntilTransformer();



	public static class DTMC extends NewNormalFormTransformer.DTMC implements NewFinallyLtlTransformer<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.DTMC>, ExpressionConditional> transformNormalForm(LTLProduct<explicit.DTMC> product, Reach<explicit.DTMC> objectivePath)
				throws PrismException, UndefinedTransformationException
		{
			// Since each BSCC is either accepting or not, it suffices to reach the set of accepting states
			return transformNormalFormReach(product, objectivePath);
		}

		@Override
		public NewFinallyUntilTransformer.DTMC getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.DTMC(getModelChecker());
		}
	}



	public static class MDP extends NewNormalFormTransformer.MDP implements NewFinallyLtlTransformer<explicit.MDP, MDPModelChecker>
	{
		public MDP(MDPModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public Pair<GoalFailStopTransformation<explicit.MDP>, ExpressionConditional> transformNormalForm(LTLProduct<explicit.MDP> product, Reach<explicit.MDP> objectivePath)
				throws PrismException, UndefinedTransformationException
		{
			objectivePath.requireSameModel(product.getProductModel());

			if (product.getAcceptance().getType() == AcceptanceType.REACH) {
				return transformNormalFormReach(product, objectivePath);
			}

			explicit.MDP productModel           = product.getProductModel();
			BitSet statesOfInterest             = product.getTransformedStatesOfInterest();

			// compute accepting states  (ECs or REACH states)
			BitSet acceptStates                 = getLtlTransformer().findAcceptingStates(product);
			Finally<explicit.MDP> conditionPath = new Finally<>(productModel, acceptStates);

			// FIXME ALG: consider whether this is actually an error in a normal-form transformation
			BitSet conditionFalsifiedStates = checkSatisfiability(conditionPath, statesOfInterest);

			// compute bad states
			BitSet badStates = computeBadStates(product, conditionFalsifiedStates);

			// compute redistribution for satisfied objective
			ProbabilisticRedistribution objectiveSatisfied = redistributeProb1(objectivePath, conditionPath);

			// compute redistribution for falsified objective
			ProbabilisticRedistribution objectiveFalsified = redistributeProb0Objective(objectivePath, conditionFalsifiedStates, badStates);

			// compute states where objective and condition can be satisfied
			BitSet instantGoalStates = computeInstantGoalStates(product, objectivePath, objectiveSatisfied.getStates(), objectiveFalsified.getStates(), acceptStates, conditionFalsifiedStates);

			// transform goal-fail-stop
			NewGoalFailStopTransformer<explicit.MDP> transformer    = getGoalFailStopTransformer();
			ProbabilisticRedistribution conditionSatisfied          = new ProbabilisticRedistribution();
			GoalFailStopTransformation<explicit.MDP> transformation = transformer.transformModel(productModel, objectiveSatisfied, conditionSatisfied, objectiveFalsified, instantGoalStates, conditionFalsifiedStates, badStates, statesOfInterest);

			// build expression
			ExpressionLabel goal                = new ExpressionLabel(transformation.getGoalLabel());
			Expression transformedObjectiveTmp  = Expression.Finally(goal);
			ExpressionProb transformedObjective = new ExpressionProb(transformedObjectiveTmp, MinMax.max(), "=", null);
			// All paths satisfying the condition eventually reach goal or an accepting EC.
			// The label accept_condition is artificial and stands for the automaton's acceptance condition.
			Expression transformedCondition     = Expression.Or(Expression.Finally(goal), new ExpressionLabel("accept_condition"));
			ExpressionConditional transformedExpression = new ExpressionConditional(transformedObjective, transformedCondition);

			return new Pair<>(transformation, transformedExpression);
		}

		/**
		 * Compute redistribution for falsified objective.
		 * For efficiency, do not minimizing the probability to satisfy the condition, but
		 * maximize the probability to reach badStates | conditionFalsifiedStates, which is equivalent.
		 */
		public ProbabilisticRedistribution redistributeProb0Objective(Reach<explicit.MDP> objectivePath, BitSet conditionFalsified, BitSet conditionMaybeFalsified)
				throws PrismException
		{
			// Do we have to reset once a state violates the objective?
			if (objectivePath.hasToRemain() || settings.getBoolean(PrismSettings.CONDITIONAL_RESET_MDP_MINIMIZE)) {
				// path to non-accepting states
				BitSet conditionFalsifiedStates = BitSetTools.union(conditionFalsified, conditionMaybeFalsified);
				Finally<explicit.MDP> conditionFalsifiedPath = new Finally<>(objectivePath.getModel(), conditionFalsifiedStates);
				// compute redistribution
				return redistributeProb0Complement(objectivePath, conditionFalsifiedPath);
			}
			// Skip costly normalization
			return new ProbabilisticRedistribution();
		}

		public BitSet computeInstantGoalStates(LTLProduct<explicit.MDP> product, Reach<explicit.MDP> objectivePath, BitSet objectiveSatisfiedStates, BitSet objectiveFalsifiedStates, BitSet conditionAcceptStates, BitSet conditionFalsifiedStates)
				throws PrismException
		{
			// FIXME ALG: compare to symbolic approach
			explicit.MDP productModel = product.getProductModel();
			objectivePath.requireSameModel(productModel);

			BitSet instantGoalStates = BitSetTools.intersect(objectiveSatisfiedStates, conditionAcceptStates);

			// exclude objective/condition falsified states
			BitSet falsifiedStates    = BitSetTools.union(objectiveFalsifiedStates, conditionFalsifiedStates);
			BitSet notFalsifiedStates = BitSetTools.complement(productModel.getNumStates(), falsifiedStates);

			// Does the condition, specify behavior in the limit?
			if (!objectivePath.isCoSafe()) {
				// Find accepting ECs that do not already include a normalize state
				BitSet remain     = BitSetTools.minus(conditionAcceptStates, objectiveSatisfiedStates, falsifiedStates);
				instantGoalStates = getLtlTransformer().findAcceptingStates(product, remain);
			}
			return computeProb1E(new Until<>(productModel, notFalsifiedStates, instantGoalStates));
		}

		@Override
		public NewFinallyUntilTransformer.MDP getFinallyUntilTransformer()
		{
			return new NewFinallyUntilTransformer.MDP(getModelChecker());
		}
	}
}
