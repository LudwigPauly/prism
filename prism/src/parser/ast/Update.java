//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package parser.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.BitSet;

import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.EvaluateContextState;
import parser.EvaluateContextSubstate;
import parser.State;
import parser.VarList;
import parser.type.Type;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;

/**
 * Class to store a single update, i.e. a list of assignments of variables to expressions, e.g. (s'=1)&amp;(x'=x+1)
 */
public class Update extends ASTElement implements Iterable<UpdateElement>
{
	// Individual elements of update
	private ArrayList<UpdateElement> elements;

	// Parent Updates object
	private Updates parent;

	/**
	 * Create an empty update.
	 */
	public Update()
	{
		elements = new ArrayList<>();
	}

	// Set methods

	/**
	 * Add a variable assignment ({@code v}'={@code e}) to this update.
	 * @param v The AST element corresponding to the variable being updated
	 * @param e The expression which will be assigned to the variable
	 */
	public void addElement(ExpressionIdent v, Expression e)
	{
		elements.add(new UpdateElement(v, e));
	}

	/**
	 * Add a variable assignment (encoded as an UpdateElement) to this update.
	 */
	public void addElement(UpdateElement e)
	{
		elements.add(e);
	}

	/**
	 * Set the ith variable assignment (encoded as an UpdateElement) to this update.
	 */
	public void setElement(int i, UpdateElement e)
	{
		elements.set(i, e);
	}

	/**
	 * Set the variable {@code v} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param v The AST element corresponding to the variable being updated
	 */
	public void setVar(int i, ExpressionIdent v)
	{
		elements.get(i).setVarIdent(v);
	}

	/**
	 * Set the expression {@code e} for the {@code i}th variable assignment of this update.
	 * @param i The index of the variable assignment within the update
	 * @param e The expression which will be assigned to the variable
	 */
	public void setExpression(int i, Expression e)
	{
		elements.get(i).setExpression(e);
	}

	/**
	 * Set the type of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The variable's type
	 */
	public void setType(int i, Type t)
	{
		elements.get(i).setType(t);
	}

	/**
	 * Set the index (wrt the model) of the {@code i}th variable assigned to by this update.
	 * @param i The index of the variable assignment within the update
	 * @param t The index of the variable within the model to which it belongs
	 */
	public void setVarIndex(int i, int index)
	{
		elements.get(i).setVarIndex(index);
	}

	/**
	 * Set the {@link parser.ast.Updates} object containing this update.
	 */
	public void setParent(Updates u)
	{
		parent = u;
	}

	// Get methods

	/**
	 * Get the number of variables assigned values by this update.
	 */
	public int getNumElements()
	{
		return elements.size();
	}

	/** Get the update element (individual assignment) with the given index. */
	public UpdateElement getElement(int index)
	{
		return elements.get(index);
	}
	
	/**
	 * Get the name of the {@code i}th variable in this update.
	 */
	public String getVar(int i)
	{
		return elements.get(i).getVar();
	}

	/**
	 * Get the expression used to update the {@code i}th variable in this update.
	 */
	public Expression getExpression(int i)
	{
		return elements.get(i).getExpression();
	}

	/**
	 * Get the type of the {@code i}th variable in this update.
	 */
	public Type getType(int i)
	{
		return elements.get(i).getType();
	}

	/**
	 * Get the ASTElement corresponding to the {@code i}th variable in this update.
	 */
	public ExpressionIdent getVarIdent(int i)
	{
		return elements.get(i).getVarIdent();
	}

	/**
	 * Get the index (wrt the model) of the {@code i}th variable in this update.
	 */
	public int getVarIndex(int i)
	{
		return elements.get(i).getVarIndex();
	}

	/**
	 * Get the {@link parser.ast.Updates} object containing this update.
	 */
	public Updates getParent()
	{
		return parent;
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in)
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param exact evaluate arithmetic expressions exactly?
	 * @param varList VarList for info about state variables
	 */
	public void update(State oldState, State newState, boolean exact, VarList varList) throws PrismLangException
	{
		EvaluateContext ec = new EvaluateContextState(oldState);
		ec.setEvaluationMode(exact ? EvalMode.EXACT : EvalMode.FP);
		for (UpdateElement e : this) {
			e.update(ec, newState, varList);
		}
	}

	/**
	 * Execute this update, based on variable values specified as a State object.
	 * Apply changes in variables to a provided copy of the State object.
	 * (i.e. oldState and newState should be equal when passed in.) 
	 * Both State objects represent only a subset of the total set of variables,
	 * with this subset being defined by the mapping varMap.
	 * Only variables in this subset are updated.
	 * But if doing so requires old values for variables outside the subset, this will cause an exception. 
	 * It is assumed that any constants have already been defined.
	 * @param oldState Variable values in current state
	 * @param newState State object to apply changes to
	 * @param varMap A mapping from indices (over all variables) to the subset (-1 if not in subset). 
	 */
	public void updatePartially(State oldState, State newState, int[] varMap) throws PrismLangException
	{
		int i, j, n;
		n = elements.size();
		for (i = 0; i < n; i++) {
			j = varMap[getVarIndex(i)];
			if (j != -1) {
				newState.setValue(j, getExpression(i).evaluate(new EvaluateContextSubstate(oldState, varMap)));
			}
		}
	}

	/**
	 * Check whether this update (from a particular state) would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public State checkUpdate(State oldState, VarList varList) throws PrismLangException
	{
		State res;
		res = new State(oldState);
		for (UpdateElement e : this) {
			e.checkUpdate(oldState, varList);
		}
		return res;
	}

	// Methods required for ASTElement:

	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public Update deepCopy(DeepCopy copier) throws PrismLangException
	{
		copier.copyAll(elements);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Update clone()
	{
		Update clone = (Update) super.clone();

		clone.elements = (ArrayList<UpdateElement>) elements.clone();

		return clone;
	}
	
	// Other methods:
	
	@Override
	public Iterator<UpdateElement> iterator()
	{
		return elements.iterator();
	}
	
	@Override
	public String toString()
	{
		// Normal case
		if (elements.size() > 0) {
			return elements.stream().map(UpdateElement::toString).collect(Collectors.joining(" & "));
		}
		// Special (empty) case
		else {
			return "true";
		}
	}


	/**
	 * @return The set of variables written by this update.
	 */
	public BitSet getWrittenVariables() {
		final BitSet variables = new BitSet();
		for (int variable = getNumElements() - 1; variable >= 0; variable--) {
			variables.set(getVarIndex(variable));
		}
		return variables;
	}

	/**
	 * @param index of a variable in the model
	 * @return the variable's ExpressionIdent or null if this update does not write the variable
	 */
	public ExpressionIdent getVarIdentFromIndex(final int index)
	{
		int variable = getLocalVarIndex(index);

		return variable < 0 ? null : getVarIdent(variable);
	}

	/**
	 * @param index of a variable in the model
	 * @return local index of the variable of -1 if this update does not write the variable
	 */
	public int getLocalVarIndex(final int index) {
		for (int variable = elements.size() - 1; variable >= 0; variable--) {
			if (getVarIndex(variable) == index) {
				return variable;
			}
		}
		return -1;
	}

	public Update split(final int index) {
		int variable = getLocalVarIndex(index);

		if (variable < 0) {
			return null;
		}
		final Update update = new Update();
		update.setPosition(this);
		// copy assignment to new update
		update.addElement((ExpressionIdent) getVarIdent(variable).deepCopy(), getExpression(variable).deepCopy());
		update.setType(0, getType(variable));
		update.setVarIndex(0, getVarIndex(variable));
		update.setParent(parent);
		// remove assignment from this update
		elements.remove(variable);
		return update;
	}

	public Update cummulateUpdatesForVariable(final Update other, final int index) throws PrismLangException
	{
		assert this.getNumElements() == 1 : "one and only one variable update expected";
		assert other.getNumElements() == 1 : "one and only one variable update expected";
		assert this.getVarIndex(0) == index : "update expected to write variable #" + index;
		assert other.getVarIndex(0) == index : "update expected to write variable #" + index;

		ExpressionBinaryOp expression;
		ExpressionBinaryOp otherExpression;
		if ((getExpression(0) instanceof ExpressionBinaryOp) && (other.getExpression(0) instanceof ExpressionBinaryOp)) {
			expression = (ExpressionBinaryOp) getExpression(0);
			otherExpression = (ExpressionBinaryOp) other.getExpression(0);
		} else {
			throw new PrismLangException("updates do not use binary operators");
		}

		final Integer[] supported = new Integer[] {ExpressionBinaryOp.PLUS, ExpressionBinaryOp.MINUS, ExpressionBinaryOp.TIMES};
		int operator = expression.getOperator();
		if (! Arrays.asList(supported).contains(operator)) {
			throw new PrismLangException("unsupported operator " + expression.getOperatorSymbol());
		}
		if (operator == ExpressionBinaryOp.MINUS) {
			operator = ExpressionBinaryOp.PLUS;
			expression = convertMinusToPlus(expression, index);
		}
		if (otherExpression.getOperator() == ExpressionBinaryOp.MINUS) {
			otherExpression = convertMinusToPlus(otherExpression, index);
		}
		if (operator != otherExpression.getOperator()) {
			throw new PrismLangException("incompatible top level opertors " + expression.getOperatorSymbol() + " and " + otherExpression.getOperatorSymbol());
		}

		final Tuple<ExpressionVar, Expression> thisSplit = splitExpression(expression, index);
		final Tuple<ExpressionVar, Expression> otherSplit = splitExpression(otherExpression, index);

		final ExpressionBinaryOp joined = new ExpressionBinaryOp(operator, thisSplit.first, new ExpressionBinaryOp(operator, thisSplit.second, otherSplit.second));
		final Update update = (Update) this.deepCopy();
		update.setExpression(0, joined);
		update.setParent(parent);
		return update;
	}

	private ExpressionBinaryOp convertMinusToPlus(ExpressionBinaryOp expression, final int index) throws PrismLangException
	{
		if(!isVariable(expression.getOperand1(), index)) {
			throw new PrismLangException("variable has to be the minuend", expression);
		}
		return (ExpressionBinaryOp) Expression.Plus(expression.getOperand1(), new ExpressionUnaryOp(ExpressionUnaryOp.MINUS, expression.getOperand2()));
	}

	private Tuple<ExpressionVar, Expression> splitExpression(final ExpressionBinaryOp expression, final int index) throws PrismLangException
	{
		assert index >= 0 : "variable index has to be be positive";

		final Tuple<ExpressionVar, Expression> result;

		if (isVariable(expression.getOperand1(), index)) {
			result = new Tuple<ExpressionVar, Expression>((ExpressionVar) expression.getOperand1(), expression.getOperand2());
		} else if (isVariable(expression.getOperand2(), index)) {
			result = new Tuple<ExpressionVar, Expression>((ExpressionVar) expression.getOperand2(), expression.getOperand1());
		} else {
			throw new PrismLangException("variable #" + index + " does not occur as top level operand", expression);
		}
		SearchVariable search = new SearchVariable(index);
		result.second.accept(search);
		if (search.isSuccessful()) {
			throw new PrismLangException("both operands depend on variable #" + index, expression);
		}
		return result;
	}

	private boolean isVariable(final Expression expression, final int index) {
		assert index >= 0 : "variable index has to be be positive";

		if (!(expression instanceof ExpressionVar)) {
			return false;
		}
		return ((ExpressionVar) expression).getIndex() == index;
	}


	public class SearchVariable extends ASTTraverse
	{
		final private int variable;
		private boolean successful = false;

		public SearchVariable(final int index) {
			this.variable = index;
		}

		public boolean isSuccessful()
		{
			return successful;
		}

		public void visitPost(final ExpressionVar e) throws PrismLangException
		{
			int index = e.getIndex();
			if (index < 0) {
				throw new PrismLangException("Index of variable not yet set.", e);
			}
			successful = successful || (index == variable);
		}
	}

	private class Tuple<S, T>
	{
		public final S first;
		public final T second;

		public Tuple(S first, T second)
		{
			this.first = first;
			this.second = second;
		}
	}


}

//------------------------------------------------------------------------------
