//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.util.Vector;

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;
// class to store list of constants

public class ConstantList extends ASTElement
{
	// Name/expression/type triples to define constants
	private Vector<String> names;
	private Vector<Expression> constants; // these can be null, i.e. undefined
	private Vector<Type> types;
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private Vector<ExpressionIdent> nameIdents;
	
	// Constructor
	
	public ConstantList()
	{
		// initialise
		names = new Vector<String>();
		constants = new Vector<Expression>();
		types = new Vector<Type>();
		nameIdents = new Vector<ExpressionIdent>();
	}
	
	// Set methods
	
	public void addConstant(ExpressionIdent n, Expression c, Type t)
	{
		names.addElement(n.getName());
		constants.addElement(c);
		types.addElement(t);
		nameIdents.addElement(n);
	}
	
	public void setConstant(int i, Expression c)
	{
		constants.setElementAt(c, i);
	}
	
	// Get methods

	public int size()
	{
		return constants.size();
	}

	public String getConstantName(int i)
	{
		return names.elementAt(i);
	}
	
	public Expression getConstant(int i)
	{
		return constants.elementAt(i);
	}
	
	public Type getConstantType(int i)
	{
		return types.elementAt(i);
	}
	
	public ExpressionIdent getConstantNameIdent(int i)
	{
		return nameIdents.elementAt(i);
	}

	/**
	 * Get the index of a constant by its name (returns -1 if it does not exist).
	 */
	public int getConstantIndex(String s)
	{
		return names.indexOf(s);
	}

	/**
	 * Find cyclic dependencies.
	*/
	public void findCycles() throws PrismLangException
	{
		int i, j, k, l, n, firstCycle = -1;
		Vector<String> v;
		boolean matrix[][];
		boolean foundCycle = false;
		Expression e;
		
		// initialise boolean matrix
		n = constants.size();
		matrix = new boolean[n][n];
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				matrix[i][j] = false;
			}
		}
		
		// determine which constants contain which other constants
		// and store this info in boolean matrix
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e != null) {
				v = e.getAllConstants();
				for (j = 0; j < v.size(); j++) {
					k = getConstantIndex(v.elementAt(j));
					if (k != -1) {
						matrix[i][k] = true;
					}
				}
			}
		}
		
		// check for dependencies
		// (loop a maximum of n times)
		// (n = max length of possible cycle)
		for (i = 0 ; i < n; i++) {
			// see if there is a cycle yet
			for (j = 0; j < n; j++) {
				if (matrix[j][j]) {
					foundCycle = true;
					firstCycle = j;
					break;
				}
			}
			// if so, stop
			if (foundCycle) break;
			// extend dependencies
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					if (matrix[j][k]) {
						for (l = 0; l < n; l++) {
							matrix[j][l] |= matrix[k][l];
						}
					}
				}
			}
		}
		
		// report dependency
		if (foundCycle) {
			String s = "Cyclic dependency in definition of constant \"" + getConstantName(firstCycle) + "\"";
			throw new PrismLangException(s, getConstant(firstCycle));
		}
	}
	
	// get number of undefined constants
	
	public int getNumUndefined()
	{
		int i, n, res;
		Expression e;
		
		res = 0;
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				res++;
			}
		}
		
		return res;
	}
	
	// get undefined constants
	
	public Vector<String> getUndefinedConstants()
	{
		int i, n;
		Expression e;
		Vector<String> v;
		
		v = new Vector<String>();
		n = constants.size();
		for (i = 0; i < n; i++) {
			e = getConstant(i);
			if (e == null) {
				v.addElement(getConstantName(i));
			}
		}
		
		return v;
	}
	
	// Set values for undefined constants, evaluate all and return.
	// Argument 'someValues' contains values for undefined ones, can be null if all already defined
	// Argument 'otherValues' contains any other values which may be needed, null if none
	
	public Values evaluateConstants(Values someValues, Values otherValues) throws PrismLangException
	{
		ConstantList cl;
		Expression e;
		Values allValues;
		int i, j, n;
		Type t = null;
		ExpressionIdent s;
		
		// create new copy of this ConstantList
		// (copy existing constant definitions, add new ones where undefined)
		cl = new ConstantList();
		n = constants.size();
		for (i = 0; i < n; i++) {
			s = getConstantNameIdent(i);
			e = getConstant(i);
			t = getConstantType(i);
			if (e != null) {
				cl.addConstant((ExpressionIdent)s.deepCopy(), e.deepCopy(), t);
			}
			else 
			{
				// create new literal expression using values passed in
				j = someValues.getIndexOf(s.getName());
				if (j == -1) {
					throw new PrismLangException("No value specified for constant", s);
				}
				else 
				{
					if (t instanceof TypeInt)
						cl.addConstant((ExpressionIdent)s.deepCopy(), new ExpressionLiteral(TypeInt.getInstance(), someValues.getIntValue(j)), TypeInt.getInstance()); 
					else if (t instanceof TypeDouble)
						cl.addConstant((ExpressionIdent)s.deepCopy(), new ExpressionLiteral(TypeDouble.getInstance(), someValues.getDoubleValue(j)), TypeDouble.getInstance()); 
					else if (t instanceof TypeBool)
						cl.addConstant((ExpressionIdent)s.deepCopy(), new ExpressionLiteral(TypeBool.getInstance(), someValues.getBooleanValue(j)), TypeBool.getInstance());
				}
			}
		}
		
		// now add constants corresponding to the 'otherValues' argument to the new constant list
		if (otherValues != null) {
			n = otherValues.getNumValues();
			for (i = 0; i < n; i++) {
				Type iType = otherValues.getType(i);
				if (iType instanceof TypeInt)
					cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(TypeInt.getInstance(), otherValues.getIntValue(i)), TypeInt.getInstance());
				else if (iType instanceof TypeDouble)
					cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(TypeDouble.getInstance(), otherValues.getDoubleValue(i)), TypeDouble.getInstance()); 
				else if (iType instanceof TypeBool)
					cl.addConstant(new ExpressionIdent(otherValues.getName(i)), new ExpressionLiteral(TypeBool.getInstance(), otherValues.getBooleanValue(i)), TypeBool.getInstance());
			}
		}
		
		// go thru and expand definition of each constant
		// (i.e. replace other constant references with their definitions)
		// (working with new copy of constant list)
		// (and ignoring extra constants added on the end which are all defined)		
		n = constants.size();
		for (i = 0; i < n; i++) {
			cl.setConstant(i, (Expression)cl.getConstant(i).expandConstants(cl));
		}
		
		// evaluate constants and store in new Values object
		// (again, ignoring extra constants added on the end)		
		allValues = new Values();
		n = constants.size();
		for (i = 0; i < n; i++) {
			allValues.addValue(cl.getConstantName(i), cl.getConstant(i).evaluate(null, otherValues));
		}
		
		return allValues;
	}

	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		int i, n;
		Expression e;
		
		n = constants.size();
		for (i = 0; i < n; i++) {
			s += "const ";
			s += getConstantType(i).getTypeString() + " ";
			s += getConstantName(i);
			e = getConstant(i);
			if (e != null) {
				s += " = " + e;
			}
			s += ";\n";
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		ConstantList ret = new ConstantList();
		n = size();
		for (i = 0; i < n; i++) {
			Expression constantNew = (getConstant(i) == null) ? null : getConstant(i).deepCopy();
			ret.addConstant((ExpressionIdent)getConstantNameIdent(i).deepCopy(), constantNew, getConstantType(i));
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
