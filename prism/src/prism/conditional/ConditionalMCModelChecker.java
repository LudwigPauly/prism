package prism.conditional;

import java.util.SortedSet;

import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.MdpTransformerType;
import jdd.JDD;
import jdd.JDDNode;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.ModelChecker;
import prism.ModelExpressionTransformation;
import prism.Prism;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import prism.ProbModel;
import prism.ProbModelChecker;
import prism.StateValues;
import prism.StochModel;
import prism.StochModelChecker;

public abstract class ConditionalMCModelChecker<M extends ProbModel, C extends ProbModelChecker> extends ConditionalModelChecker<M>
{
	protected C modelChecker;

	public ConditionalMCModelChecker(Prism prism, C modelChecker)
	{
		super(prism);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(M model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		final NewConditionalTransformer.MC<M,C> transformer = selectModelTransformer(model, expression);
		final ModelExpressionTransformation<M, ? extends M> transformation = transformModel(transformer, model, expression, statesOfInterest);
		final StateValues resultTransformed = checkExpressionTransformedModel(transformation, expression);

		final StateValues resultOriginal = transformation.projectToOriginalModel(resultTransformed);
		transformation.clear();

		return resultOriginal;
	}

	protected ModelExpressionTransformation<M, ? extends M> transformModel(final NewConditionalTransformer.MC<M,C> transformer, final M model, final ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
	{
		prism.getLog().println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression.getCondition());
		long timer = System.currentTimeMillis();
		final ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().println("\nOverall time for model transformation: " + timer / 1000.0 + " seconds.");
		prism.getLog().print("Transformed model has ");
		prism.getLog().println(transformation.getTransformedModel().infoString());
		prism.getLog().print("Transformed matrix has ");
		prism.getLog().println(transformation.getTransformedModel().matrixInfoString());

		return transformation;
	}

	protected abstract NewConditionalTransformer.MC<M,C> selectModelTransformer(final M model, final ExpressionConditional expression) throws PrismException;

	protected StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation, final ExpressionConditional expression) throws PrismException
	{
		final ProbModel transformedModel = transformation.getTransformedModel();
		final Expression transformedExpression;
		transformedExpression = ((ModelExpressionTransformation<?,?>) transformation).getTransformedExpression();

		prism.getLog().println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer = System.currentTimeMillis();

		ModelChecker mcTransformed = modelChecker.createModelChecker(transformedModel);

		final StateValues result = mcTransformed.checkExpression(transformedExpression, JDD.Constant(1));
		timer = System.currentTimeMillis() - timer;
		prism.getLog().println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return result;
	}



	public static class CTMC extends ConditionalMCModelChecker<StochModel, StochModelChecker>
	{
		public CTMC(Prism prism, StochModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		public StateValues checkExpression(StochModel model, ExpressionConditional expression, JDDNode statesOfInterest) throws PrismException
		{
			if (! Expression.containsTemporalTimeBounds(expression.getCondition())) {
				return super.checkExpression(model, expression, statesOfInterest);
			}
			if (!(expression.getObjective() instanceof ExpressionProb)) {
				throw new PrismException("Cannot model check " + expression);
			}
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			if (Expression.containsTemporalTimeBounds(objective.getExpression())) {
				throw new PrismException("Cannot model check " + expression);
			}
			Expression transformed = transformExpression(expression);
			prism.getLog().println();
			prism.getLog().println("Condition contains time bounds, trying Bayes' rule.");
			prism.getLog().println("Checking transformed expression: " + transformed);
			return modelChecker.checkExpression(transformed, statesOfInterest);
		}

		protected Expression transformExpression(ExpressionConditional expression)
				throws PrismNotSupportedException
		{
			ExpressionProb objective = (ExpressionProb) expression.getObjective();
			Expression condition     = expression.getCondition();

			// Bayes' rule: P(A|B) = P(B|A) * P(A) / P(B)
			String computeValues        = RelOp.COMPUTE_VALUES.toString();
			ExpressionProb newObjective = new ExpressionProb(condition.deepCopy(), computeValues, null);
			Expression newCondition     = objective.getExpression().deepCopy();
			ExpressionConditional pAB   = new ExpressionConditional(newObjective, newCondition);
			ExpressionProb pA           = new ExpressionProb(objective.getExpression().deepCopy(), computeValues, null);
			ExpressionProb pB           = new ExpressionProb(condition.deepCopy(), computeValues, null);
			ExpressionBinaryOp fraction = Expression.Divide(Expression.Times(pAB, pA), pB);

			// translate bounds if necessary
			if (objective.getBound() == null) {
				return fraction;
			}
			int binOp = convertToBinaryOp(objective.getRelOp());
			return new ExpressionBinaryOp(binOp, Expression.Parenth(fraction), objective.getBound().deepCopy());
		}

		protected int convertToBinaryOp(RelOp relop) throws PrismNotSupportedException
		{
			switch (relop) {
			case GT:
				return ExpressionBinaryOp.GT;
			case GEQ:
				return ExpressionBinaryOp.GE;
			case LT:
				return ExpressionBinaryOp.LT;
			case LEQ:
				return ExpressionBinaryOp.LE;
			default:
				throw new PrismNotSupportedException("Unsupported comparison operator: " + relop);
			}
		}

		@Override
		protected NewConditionalTransformer.MC<StochModel, StochModelChecker> selectModelTransformer(final StochModel model, final ExpressionConditional expression) throws PrismException
		{
			PrismSettings settings = prism.getSettings();
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				throw new PrismException("There is no symbolic TACAS'14 prototype");
			}
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
				throw new PrismException("There is no symbolic prototype for the scale method in MCs");
			}

			NewConditionalTransformer.MC<StochModel, StochModelChecker> transformer;
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_RESET_FOR_MC)) {
				final String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
				final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
				for (MdpTransformerType type : types) {
					switch (type) {
					case FinallyFinally:
						transformer = new NewFinallyUntilTransformer.CTMC(prism, modelChecker);
						break;
					case LtlFinally:
						transformer = new NewLtlUntilTransformer.CTMC(prism, modelChecker);
						break;
					case FinallyLtl:
						transformer = new NewFinallyLtlTransformer.CTMC(prism, modelChecker);
						break;
					case LtlLtl:
						transformer = new NewLtlLtlTransformer.CTMC(prism, modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else {
				final String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_SCALE);
				final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
				for (DtmcTransformerType type : types) {
					switch (type) {
					case Quotient:
						transformer = new MCQuotientTransformer.CTMC(prism, modelChecker);
						break;
					case Until:
						transformer = new MCUntilTransformer.CTMC(prism, modelChecker);
						break;
					case Next:
						transformer = new MCNextTransformer.CTMC(prism, modelChecker);
						break;
					case Ltl:
						transformer = new MCLTLTransformer.CTMC(prism, modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}

			throw new PrismException("Cannot model check " + expression);
		}
	}



	public static class DTMC extends ConditionalMCModelChecker<ProbModel, ProbModelChecker>
	{
		public DTMC(Prism prism, ProbModelChecker modelChecker)
		{
			super(prism, modelChecker);
		}

		@Override
		protected NewConditionalTransformer.MC<ProbModel, ProbModelChecker> selectModelTransformer(final ProbModel model, final ExpressionConditional expression) throws PrismException
		{
			PrismSettings settings = prism.getSettings();
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				throw new PrismException("There is no symbolic TACAS'14 prototype");
			}
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
				throw new PrismException("There is no symbolic prototype for the scale method in MCs");
			}

			NewConditionalTransformer.MC<ProbModel, ProbModelChecker> transformer;
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_RESET_FOR_MC)) {
				final String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
				final SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
				for (MdpTransformerType type : types) {
					switch (type) {
					case FinallyFinally:
						transformer = new NewFinallyUntilTransformer.DTMC(prism, modelChecker);
						break;
					case LtlFinally:
						transformer = new NewLtlUntilTransformer.DTMC(prism, modelChecker);
						break;
					case FinallyLtl:
						transformer = new NewFinallyLtlTransformer.DTMC(prism, modelChecker);
						break;
					case LtlLtl:
						transformer = new NewLtlLtlTransformer.DTMC(prism, modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else {
				final String specification = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_SCALE);
				final SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
				for (DtmcTransformerType type : types) {
					switch (type) {
					case Quotient:
						transformer = new MCQuotientTransformer.DTMC(prism, modelChecker);
						break;
					case Until:
						transformer = new MCUntilTransformer.DTMC(prism, modelChecker);
						break;
					case Next:
						transformer = new MCNextTransformer.DTMC(prism, modelChecker);
						break;
					case Ltl:
						transformer = new MCLTLTransformer.DTMC(prism, modelChecker);
						break;
					default:
						continue;
					}
					if (transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}

			throw new PrismException("Cannot model check " + expression);
		}
	}
}
