package explicit.conditional;

import java.util.BitSet;
import java.util.SortedSet;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConditional;
import parser.ast.ExpressionProb;
import parser.ast.RelOp;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismSettings;
import explicit.BasicModelExpressionTransformation;
import explicit.BasicModelTransformation;
import explicit.CTMCModelChecker;
import explicit.CTMCSparse;
import explicit.DTMCModelChecker;
import explicit.DTMCSimple;
import explicit.DTMCSparse;
import explicit.ModelExpressionTransformation;
import explicit.ProbModelChecker;
import explicit.StateValues;
import explicit.conditional.prototype.virtual.FinallyLtlTransformer;
import explicit.conditional.prototype.virtual.FinallyUntilTransformer;
import explicit.conditional.prototype.virtual.LtlLtlTransformer;
import explicit.conditional.prototype.virtual.LtlUntilTransformer;
import explicit.conditional.prototype.virtual.MCLTLTransformer;
import explicit.conditional.prototype.virtual.MCNextTransformer;
import explicit.conditional.prototype.virtual.MCUntilTransformer;
import explicit.conditional.transformer.DtmcTransformerType;
import explicit.conditional.transformer.MdpTransformerType;
import explicit.conditional.transformer.mc.MCQuotientTransformer;
import explicit.conditional.transformer.mc.NewMcLtlTransformer;
import explicit.conditional.transformer.mc.NewMcNextTransformer;
import explicit.conditional.transformer.mc.NewMcUntilTransformer;
import explicit.modelviews.MCView;

// FIXME ALG: add comment
public abstract class ConditionalMCModelChecker<M extends explicit.DTMC, C extends ProbModelChecker> extends ConditionalModelChecker<M>
{
	protected C modelChecker;

	public ConditionalMCModelChecker(C modelChecker)
	{
		super(modelChecker);
		this.modelChecker = modelChecker;
	}

	@Override
	public StateValues checkExpression(M model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
	{
		NewConditionalTransformer<M, C> transformer = selectModelTransformer(model, expression);
		if (transformer == null) {
			throw new PrismNotSupportedException("Cannot model check " + expression);
		}

		ModelExpressionTransformation<M, ? extends M> transformation = transformModel(transformer, model, expression, statesOfInterest);
		StateValues result = checkExpressionTransformedModel(transformation);
		return transformation.projectToOriginalModel(result);
	}

	public ModelExpressionTransformation<M, ? extends M> transformModel(final NewConditionalTransformer<M, C> transformer, final M model,
			final ExpressionConditional expression, final BitSet statesOfInterest) throws PrismException
	{
		mainLog.println("\nTransforming model (using " + transformer.getName() + ") for condition: " + expression);
		long overallTime = System.currentTimeMillis();
		ModelExpressionTransformation<M, ? extends M> transformation = transformer.transform(model, expression, statesOfInterest);
		long transformationTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nTime for model transformation: " + transformationTime / 1000.0 + " seconds.");

		M transformedModel = transformation.getTransformedModel();
		if (isVirtualModel(transformedModel) || transformedModel instanceof DTMCSimple) {
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_VIRTUAL_MODELS)) {
				mainLog.println("Using simple/virtual model");
			} else {
				transformation = convertVirtualModel(transformation);
			}
		}
		overallTime = System.currentTimeMillis() - overallTime;
		mainLog.println("\nOverall time for model transformation: " + overallTime / 1000.0 + " seconds.");
		mainLog.print("Transformed model has ");
		mainLog.println(transformation.getTransformedModel().infoString());
		return transformation;
	}

	public boolean isVirtualModel(M model)
	{
		return (model instanceof MCView) && ((MCView) model).isVirtual();
	}

	public NewConditionalTransformer<M, C> selectModelTransformer(final M model, final ExpressionConditional expression) throws PrismException
	{
		NewConditionalTransformer<M, C> transformer;
		if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_RESET_FOR_MC)) {
			String specification                = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_RESET);
			SortedSet<MdpTransformerType> types = MdpTransformerType.getValuesOf(specification);
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				for (MdpTransformerType type : types) {
					transformer = getResetTransformerTacas14(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
				for (MdpTransformerType type : types) {
					transformer = getResetTransformerPrototype(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else {
				for (MdpTransformerType type : types) {
					transformer = getResetTransformer(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}
		} else {
			String specification                 = settings.getString(PrismSettings.CONDITIONAL_PATTERNS_SCALE);
			SortedSet<DtmcTransformerType> types = DtmcTransformerType.getValuesOf(specification);
			if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_TACAS14_PROTOTYPE)) {
				for (DtmcTransformerType type : types) {
					transformer = getScaleTransformerTacas14(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else if (settings.getBoolean(PrismSettings.CONDITIONAL_USE_PROTOTYPE)) {
				for (DtmcTransformerType type : types) {
					transformer = getScaleTransformerPrototype(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			} else {
				for (DtmcTransformerType type : types) {
					transformer = getScaleTransformer(type);
					if (transformer != null && transformer.canHandle(model, expression)) {
						return transformer;
					}
				}
			}
		}
		return null;
	}

	public StateValues checkExpressionTransformedModel(final ModelExpressionTransformation<M, ? extends M> transformation) throws PrismException
	{
		M transformedModel                 = transformation.getTransformedModel();
		Expression transformedExpression   = transformation.getTransformedExpression();
		BitSet transformedStatesOfInterest = transformation.getTransformedStatesOfInterest();

		mainLog.println("\nChecking transformed property in transformed model: " + transformedExpression);
		long timer     = System.currentTimeMillis();
		StateValues sv = modelChecker.checkExpression(transformedModel, transformedExpression, transformedStatesOfInterest);
		timer          = System.currentTimeMillis() - timer;
		mainLog.println("\nTime for model checking in transformed model: " + timer / 1000.0 + " seconds.");

		return sv;
	}

	protected NewConditionalTransformer<M, C> getResetTransformerTacas14(MdpTransformerType type) throws PrismNotSupportedException
	{
		throw new PrismNotSupportedException("There is no explicit TACAS'14 prototype for the reset method in MCs");
	}

	protected abstract NewConditionalTransformer<M, C> getResetTransformerPrototype(MdpTransformerType type) throws PrismNotSupportedException;

	protected abstract NewConditionalTransformer<M, C> getResetTransformer(MdpTransformerType type);

	protected abstract NewConditionalTransformer<M, C> getScaleTransformerTacas14(DtmcTransformerType type) throws PrismNotSupportedException;

	protected abstract NewConditionalTransformer<M, C> getScaleTransformerPrototype(DtmcTransformerType type) throws PrismNotSupportedException;

	protected abstract NewConditionalTransformer<M, C> getScaleTransformer(DtmcTransformerType type);

	protected abstract ModelExpressionTransformation<M, ? extends M> convertVirtualModel(ModelExpressionTransformation<M,? extends M> transformation);



	public static class CTMC extends ConditionalMCModelChecker<explicit.CTMC, CTMCModelChecker>
	{
		public CTMC(CTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		public StateValues checkExpression(explicit.CTMC model, ExpressionConditional expression, BitSet statesOfInterest) throws PrismException
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
			getLog().println();
			getLog().println("Condition contains time bounds, trying Bayes' rule.");
			getLog().println("Checking transformed expression: " + transformed);
			return modelChecker.checkExpression(model, transformed, statesOfInterest);
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
		protected NewConditionalTransformer<explicit.CTMC, CTMCModelChecker> getResetTransformerPrototype(MdpTransformerType type) throws PrismNotSupportedException
		{
			throw new PrismNotSupportedException("There is no explicit prototype for the reset method in CTMCs");
		}
	
		@Override
		protected NewConditionalTransformer<explicit.CTMC, CTMCModelChecker> getResetTransformer(MdpTransformerType type)
		{
			switch (type) {
			case FinallyFinally:
				return new NewFinallyUntilTransformer.CTMC(modelChecker);
			case LtlFinally:
				return new NewLtlUntilTransformer.CTMC(modelChecker);
			case FinallyLtl:
				return new NewFinallyLtlTransformer.CTMC(modelChecker);
			case LtlLtl:
				return  new NewLtlLtlTransformer.CTMC(modelChecker);
			default:
				return null;
			}
		}
	
		@Override
		protected NewConditionalTransformer<explicit.CTMC, CTMCModelChecker> getScaleTransformerTacas14(DtmcTransformerType type) throws PrismNotSupportedException
		{
			throw new PrismNotSupportedException("There is no explicit TACAS'14 prototype for the scale method in CTMCs");
		}
	
		@Override
		protected NewConditionalTransformer<explicit.CTMC, CTMCModelChecker> getScaleTransformerPrototype(DtmcTransformerType type) throws PrismNotSupportedException
		{
			throw new PrismNotSupportedException("There is no explicit prototype for the scale method in CTMCs");
		}
	
		@Override
		protected NewConditionalTransformer<explicit.CTMC, CTMCModelChecker> getScaleTransformer(DtmcTransformerType type)
		{
			switch (type) {
			case Quotient:
				return new MCQuotientTransformer.CTMC(modelChecker);
			case Until:
				return new NewMcUntilTransformer.CTMC(modelChecker);
			case Next:
				return new NewMcNextTransformer.CTMC(modelChecker);
			case Ltl:
				return new NewMcLtlTransformer.CTMC(modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected ModelExpressionTransformation<explicit.CTMC, ? extends explicit.CTMC> convertVirtualModel(ModelExpressionTransformation<explicit.CTMC, ? extends explicit.CTMC> transformation)
		{
			mainLog.println("\nConverting simple/virtual model to " + CTMCSparse.class.getSimpleName());
			long buildTime = System.currentTimeMillis();
			explicit.CTMC transformedModel = transformation.getTransformedModel();
			CTMCSparse transformedModelSparse = new CTMCSparse(transformedModel);
			buildTime = System.currentTimeMillis() - buildTime;
			mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			// build transformation
			BasicModelTransformation<explicit.CTMC, explicit.CTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse);
			sparseTransformation = sparseTransformation.compose(transformation);
			// attach transformed expression
			Expression originalExpression    = transformation.getOriginalExpression();
			Expression transformedExpression = transformation.getTransformedExpression();
			return new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
		}
	}



	public static class DTMC extends ConditionalMCModelChecker<explicit.DTMC, DTMCModelChecker>
	{
		public DTMC(DTMCModelChecker modelChecker)
		{
			super(modelChecker);
		}

		@Override
		protected NewConditionalTransformer<explicit.DTMC, DTMCModelChecker> getResetTransformerPrototype(MdpTransformerType type)
		{
			switch (type) {
			case FinallyFinally:
				return new FinallyUntilTransformer.DTMC(modelChecker);
			case LtlFinally:
				return new LtlUntilTransformer.DTMC(modelChecker);
			case FinallyLtl:
				return new FinallyLtlTransformer.DTMC(modelChecker);
			case LtlLtl:
				return new LtlLtlTransformer.DTMC(modelChecker);
			default:
				return null;
			}
		}
	
		@Override
		protected NewConditionalTransformer<explicit.DTMC, DTMCModelChecker> getResetTransformer(MdpTransformerType type)
		{
			switch (type) {
			case FinallyFinally:
				return new NewFinallyUntilTransformer.DTMC(modelChecker);
			case LtlFinally:
				return new NewLtlUntilTransformer.DTMC(modelChecker);
			case FinallyLtl:
				return new NewFinallyLtlTransformer.DTMC(modelChecker);
			case LtlLtl:
				return  new NewLtlLtlTransformer.DTMC(modelChecker);
			default:
				return null;
			}
		}
	
		@Override
		protected NewConditionalTransformer<explicit.DTMC, DTMCModelChecker> getScaleTransformerTacas14(DtmcTransformerType type)
		{
			switch (type) {
			case Finally:
				return new explicit.conditional.prototype.tacas14.MCMatchingFinallyTransformer(modelChecker);
			case Until:
				return new explicit.conditional.prototype.tacas14.MCUntilTransformer(modelChecker);
			case Next:
				return new explicit.conditional.prototype.tacas14.MCNextTransformer(modelChecker);
			case Ltl:
				return new explicit.conditional.prototype.tacas14.MCLTLTransformer(modelChecker);
			default:
				return null;
			}
		}
	
		@Override
		protected NewConditionalTransformer<explicit.DTMC, DTMCModelChecker> getScaleTransformerPrototype(DtmcTransformerType type)
		{
			switch (type) {
			case Quotient:
				return new MCQuotientTransformer.DTMC(modelChecker);
			case Until:
				return new MCUntilTransformer(modelChecker);
			case Next:
				return new MCNextTransformer(modelChecker);
			case Ltl:
				return new MCLTLTransformer(modelChecker);
			default:
				return null;
			}
		}
	
		@Override
		protected NewConditionalTransformer<explicit.DTMC, DTMCModelChecker> getScaleTransformer(DtmcTransformerType type)
		{
			switch (type) {
			case Quotient:
				return new MCQuotientTransformer.DTMC(modelChecker);
			case Until:
				return new NewMcUntilTransformer.DTMC(modelChecker);
			case Next:
				return new NewMcNextTransformer.DTMC(modelChecker);
			case Ltl:
				return new NewMcLtlTransformer.DTMC(modelChecker);
			default:
				return null;
			}
		}

		@Override
		protected ModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> convertVirtualModel(ModelExpressionTransformation<explicit.DTMC, ? extends explicit.DTMC> transformation)
		{
			mainLog.println("\nConverting simple/virtual model to " + DTMCSparse.class.getSimpleName());
			long buildTime = System.currentTimeMillis();
			explicit.DTMC transformedModel = transformation.getTransformedModel();
			DTMCSparse transformedModelSparse = new DTMCSparse(transformedModel);
			buildTime = System.currentTimeMillis() - buildTime;
			mainLog.println("Time for converting: " + buildTime / 1000.0 + " seconds.");
			// build transformation
			BasicModelTransformation<explicit.DTMC, explicit.DTMC> sparseTransformation = new BasicModelTransformation<>(transformation.getTransformedModel(), transformedModelSparse);
			sparseTransformation = sparseTransformation.compose(transformation);
			// attach transformed expression
			Expression originalExpression    = transformation.getOriginalExpression();
			Expression transformedExpression = transformation.getTransformedExpression();
			return new BasicModelExpressionTransformation<>(sparseTransformation, originalExpression, transformedExpression);
		}
	}
}