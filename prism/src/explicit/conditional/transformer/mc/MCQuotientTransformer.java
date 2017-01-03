package explicit.conditional.transformer.mc;

import java.util.BitSet;

import common.iterable.Support;
import parser.ast.Expression;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.DTMCModelChecker;
import explicit.LTLModelChecker;
import explicit.Model;
import explicit.conditional.transformer.UndefinedTransformationException;

// FIXME ALG: add comment
public class MCQuotientTransformer extends MCConditionalTransformer
{
	public MCQuotientTransformer(final DTMCModelChecker mc)
	{
		super(mc);
	}

	@Override
	public boolean canHandleCondition(final Model model, final ExpressionConditional expression) throws PrismLangException
	{
		// FIXME ALG: Should check whether formula can be turned into ExpressionProb
		return LTLModelChecker.isSupportedLTLFormula(ModelType.DTMC, expression.getCondition());
	}

	@Override
	public boolean canHandleObjective(final Model model, final ExpressionConditional expression)
	{
		// only prob formulae without bounds
		if (!(expression.getObjective() instanceof ExpressionProb)) {
			return false;
		}
		return ((ExpressionProb) expression.getObjective()).getRelOp() == RelOp.COMPUTE_VALUES;
	}

	@Override
	public ConditionalQuotientTransformation transform(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		final double[] probabilities = computeProbability(model, expression.getCondition());
		final BitSet support = new Support(probabilities).asBitSet();
		support.and(statesOfInterest);
		if (support.isEmpty()) {
			throw new UndefinedTransformationException("condition is not satisfiable");
		}
		final BasicModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> transformation = super.transform(model, expression, support);
		return new ConditionalQuotientTransformation(transformation, probabilities);
	}

	@Override
	protected BasicModelTransformation<explicit.DTMC, explicit.DTMC> transformModel(final explicit.DTMC model, final ExpressionConditional expression, final BitSet statesOfInterest)
			throws PrismException
	{
		return new BasicModelTransformation<explicit.DTMC, explicit.DTMC>(model, model, statesOfInterest);
	}

	@Override
	protected ExpressionProb transformExpression(final ExpressionConditional expression)
	{
		final ExpressionProb objective = (ExpressionProb) expression.getObjective();
		final Expression condition = expression.getCondition();

		return new ExpressionProb(Expression.And(objective.getExpression(), condition), RelOp.COMPUTE_VALUES.toString(), null);
	}

	protected double[] computeProbability(final explicit.DTMC model, final Expression pathFormula) throws PrismException
	{
		final ExpressionProb expression = new ExpressionProb(pathFormula, "=", null);

		return modelChecker.checkExpression(model, expression, null).getDoubleArray();
	}
}