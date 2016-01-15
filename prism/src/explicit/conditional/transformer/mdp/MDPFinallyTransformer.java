package explicit.conditional.transformer.mdp;

import java.util.BitSet;

import common.BitSetTools;
import explicit.MDP;
import explicit.MDPModelChecker;
import explicit.conditional.ExpressionInspector;
import explicit.conditional.transformer.mdp.GoalFailStopTransformer.GoalFailStopTransformation;
import explicit.conditional.transformer.mdp.MDPResetTransformer.ResetTransformation;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionTemporal;
import prism.PrismException;
import prism.PrismLangException;

public class MDPFinallyTransformer extends MDPConditionalTransformer
{
	public MDPFinallyTransformer(final MDPModelChecker modelChecker)
	{
		super(modelChecker);
	}

	@Override
	protected boolean canHandleCondition(final MDP model, final ExpressionConditional expression)
	{
		final Expression normalized = ExpressionInspector.normalizeExpression(expression.getCondition());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	protected boolean canHandleObjective(final MDP model, final ExpressionConditional expression) throws PrismLangException
	{
		if (!super.canHandleObjective(model, expression)) {
			return false;
		}
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression normalized = ExpressionInspector.normalizeExpression(objective.getExpression());
		return ExpressionInspector.isSimpleFinallyFormula(normalized);
	}

	@Override
	public ConditionalMDPTransformation transform(final MDP model, final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		checkCanHandle(model, expression);

		// 1) Condition: compute "condition goal states"
		final Expression conditionGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(expression.getCondition())).getOperand2();
		final BitSet conditionGoalStates = modelChecker.checkExpression(model, conditionGoal, null).getBitSet();

		// 2) Objective: extract objective
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();

		return transform(model, objective, conditionGoalStates, statesOfInterest);
	}

	protected ConditionalMDPTransformation transform(final MDP model, final ExpressionProb objective, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		// 1) Objective: compute "objective goal states"
		final Expression objectiveGoal = ((ExpressionTemporal) ExpressionInspector.normalizeExpression(objective.getExpression())).getOperand2();
		final BitSet objectiveGoalStates = modelChecker.checkExpression(model, objectiveGoal, null).getBitSet();

		return transform(model, objectiveGoalStates, conditionGoalStates, statesOfInterest);
	}

	protected ConditionalMDPTransformation transform(final MDP model, final BitSet objectiveGoalStates, final BitSet conditionGoalStates,
			final BitSet statesOfInterest) throws PrismException
	{
		MDPResetTransformer.checkStatesOfInterest(statesOfInterest);
		checkSatisfiability(model, conditionGoalStates, statesOfInterest);

		// 1) Normal Form Transformation
		final GoalFailStopTransformer normalFormTransformer = new GoalFailStopTransformer(modelChecker);
		final GoalFailStopTransformation normalFormTransformation = normalFormTransformer.transformModel(model, objectiveGoalStates,
				conditionGoalStates);

		//    compute "bad states" == {s | Pmin=0[<> (Obj or Cond)]}
		// FIXME ALG: prove simplification: bad states == {s | Pmin=0[<> (Cond)]}
		final BitSet badStates = modelChecker.prob0(model, null, conditionGoalStates, true, null);
		badStates.set(normalFormTransformation.getFailState());

		//    reset transformation
		final BitSet normalFormStatesOfInterest = normalFormTransformation.mapToTransformedModel(statesOfInterest);
		final MDPResetTransformer resetTransformer = new MDPResetStateTransformer(modelChecker);
		final ResetTransformation<MDP> resetTransformation = resetTransformer.transformModel(normalFormTransformation.getTransformedModel(),
				badStates, normalFormStatesOfInterest);

		// FIXME ALG: consider restriction to part reachable from states of interest

		// 2) Create Mapping
		// FIXME ALG: consider ModelExpressionTransformationNested
		// FIXME ALG: refactor to remove tedious code duplication
		final Integer[] mapping = new Integer[model.getNumStates()];
		for (int state = 0; state < mapping.length; state++) {
			final Integer normalFormState = normalFormTransformation.mapToTransformedModel(state);
			final Integer resetState = resetTransformation.mapToTransformedModel(normalFormState);
			mapping[state] = resetState;
		}

		final int goalState = normalFormTransformation.getGoalState();
		final BitSet goalStates = BitSetTools.asBitSet(goalState);

		return new ConditionalMDPTransformation(model, resetTransformation.getTransformedModel(), mapping, goalStates);
	}
}