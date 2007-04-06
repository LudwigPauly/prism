//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package userinterface;
import parser.*;
import prism.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.event.*;
/**
 *
 * @author  ug60axh
 */
public class GUIConstantsPicker extends javax.swing.JDialog
{
	public static final int NO_VALUES = 0;
	public static final int VALUES_DONE = 1;
	public static final int CANCELLED = 2;
	
	private boolean cancelled = true;
	
	private JTable propTable;
	private DefineConstantTable propTableModel;
	private JTable modelTable;
	private DefineConstantTable modelTableModel;
	
	private boolean areModel, areProp;
	
	private Action okAction;
	private Action cancelAction;
	
	private javax.swing.JButton okayButton;
	
	private UndefinedConstants undef;
	
	private GUIPrism gui;
	
	// Variables declaration - do not modify//GEN-BEGIN:variables
	javax.swing.JPanel topPanel;
	// End of variables declaration//GEN-END:variables

	/** Creates new form GUIConstantsPicker */
	public GUIConstantsPicker(GUIPrism parent, UndefinedConstants undef, boolean areModel, boolean areProp, Values modelDefaults, Values propDefaults)
	{
		super(parent, "Define Constants", true);
		this.areModel = areModel;
		this.areProp  = areProp;
		this.undef = undef;
		this.gui = parent;
		
		//setup tables
		propTableModel = new DefineConstantTable();
		modelTableModel = new DefineConstantTable();
		propTable = new JTable();
		modelTable = new JTable();
		propTable.setModel(propTableModel);
		modelTable.setModel(modelTableModel);
		propTable.setSelectionMode(DefaultListSelectionModel.SINGLE_INTERVAL_SELECTION);
		modelTable.setSelectionMode(DefaultListSelectionModel.SINGLE_INTERVAL_SELECTION);
		propTable.setCellSelectionEnabled(true);
		modelTable.setCellSelectionEnabled(true);
		
		//initialise
		initComponents();
		this.getRootPane().setDefaultButton(okayButton);
		initTables(areModel, areProp);
		initValues(undef, modelDefaults, propDefaults);
		
		setResizable(false);
		
		pack();
		setLocationRelativeTo(getParent()); // centre
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents()//GEN-BEGIN:initComponents
	{
		javax.swing.JButton cancelButton;
		java.awt.GridBagConstraints gridBagConstraints;
		javax.swing.JLabel jLabel1;
		javax.swing.JPanel jPanel1;
		javax.swing.JPanel jPanel2;
		javax.swing.JPanel jPanel3;
		javax.swing.JPanel jPanel4;
		javax.swing.JPanel jPanel5;
		javax.swing.JPanel jPanel6;
		
		
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		topPanel = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		okayButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
			closeDialog(evt);
			}
		});
		
		jPanel1.setLayout(new java.awt.GridBagLayout());
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel2, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel3, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		jPanel1.add(jPanel4, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		jPanel1.add(jPanel5, gridBagConstraints);
		
		jLabel1.setText("Please define the following constants:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel1.add(jLabel1, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel1.add(topPanel, gridBagConstraints);
		
		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
		
		jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		
		okayButton.setText("Okay");
		okayButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
			okayButtonActionPerformed(evt);
			}
		});
		
		jPanel6.add(okayButton);
		
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
			cancelButtonActionPerformed(evt);
			}
		});
		
		jPanel6.add(cancelButton);
		
		getContentPane().add(jPanel6, java.awt.BorderLayout.SOUTH);
		
		pack();
	}//GEN-END:initComponents

	private void initTables(boolean areModel, boolean areProp)
	{
		if(areModel && areProp)
			{
				topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
				JPanel topTopPanel = new JPanel();
				topTopPanel.setBorder(new TitledBorder("Model Constants"));
				topTopPanel.setLayout(new BorderLayout());
				JScrollPane sp = new JScrollPane();
				
				sp.setViewportView(modelTable);
				topTopPanel.add(sp);
				JPanel bottomTopPanel = new JPanel();
				bottomTopPanel.setBorder(new TitledBorder("Property Constants"));
				bottomTopPanel.setLayout(new BorderLayout());
				JScrollPane sp2 = new JScrollPane();
				
				sp2.setViewportView(propTable);
				bottomTopPanel.add(sp2);
				topPanel.add(topTopPanel);
				topPanel.add(bottomTopPanel);
			}
			else if(areModel)
			{
				topPanel.setBorder(new TitledBorder("Model Constants"));
				topPanel.setLayout(new BorderLayout());
				JScrollPane sp = new JScrollPane();
				sp.setViewportView(modelTable);
				topPanel.add(sp);
			}
			else if(areProp)
			{
				topPanel.setBorder(new TitledBorder("Property Constants"));
				topPanel.setLayout(new BorderLayout());
				JScrollPane sp = new JScrollPane();
				sp.setViewportView(propTable);
				topPanel.add(sp);
			}
			
			topPanel.setPreferredSize(new Dimension(500,200));
	}

	private void initValues(UndefinedConstants undef, Values modelDef, Values propDef)
	{
		for(int i = 0; i < undef.getMFNumUndefined(); i++)
		{
			Constant c = new Constant(undef.getMFUndefinedName(i), undef.getMFUndefinedType(i), "");
			if(modelDef != null)
			{
			try
			{
				Object o = modelDef.getValueOf(c.name);
				c.value = o;
			}
			catch(Exception e)
			{}
			}
			modelTableModel.addConstant(c);
		}
		for(int i = 0; i < undef.getPFNumUndefined(); i++)
		{
			Constant c = new Constant(undef.getPFUndefinedName(i), undef.getPFUndefinedType(i), "");
			if(propDef != null)
			{
			try
			{
				Object o = propDef.getValueOf(c.name);
				c.value = o;
			}
			catch(Exception e)
			{}
			}
			propTableModel.addConstant(c);
		}
	}

	/** Call this static method to construct a new GUIConstantsPicker to define
	 *  undef.  If you don't want any default values, then pass in null for
	 *  modelDefaults and propDefaults
	 */
	public static int defineConstantsWithDialog(GUIPrism parent, UndefinedConstants undef, Values modelDefaults, Values propDefaults)
	{
		boolean areModel = undef.getMFNumUndefined() > 0;
		boolean areProp  = undef.getPFNumUndefined() > 0;
		if(areModel || areProp)
		{
			return new GUIConstantsPicker(parent, undef, areModel, areProp, modelDefaults, propDefaults).defineValues();
		}
		else return NO_VALUES;
	}

	public int defineValues()
	{
		show();
		if(cancelled) return CANCELLED;
		else return VALUES_DONE;
	}

	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed
	int i, n;
	Constant c;
	
	if(areProp && propTable.getCellEditor() != null)propTable.getCellEditor().stopCellEditing();
	if(areModel && modelTable.getCellEditor() != null)modelTable.getCellEditor().stopCellEditing();
	try
	{
		//undef.defineUsingConstSwitch(getConstSwitch());
		
		// passing info to UndefinedConstants object
		n = modelTableModel.getNumConstants();
		for (i = 0; i < n; i++)
		{
		c = modelTableModel.getConstant(i);
		undef.defineConstant(c.name, ""+c.value);
		}
		n = propTableModel.getNumConstants();
		for (i = 0; i < n; i++)
		{
		c = propTableModel.getConstant(i);
		undef.defineConstant(c.name, ""+c.value);
		}
		undef.checkAllDefined();
		undef.initialiseIterators();
		
		cancelled = false;
		dispose();
	}
	catch(PrismException e)
	{
		gui.errorDialog(e.getMessage());
	}
	}//GEN-LAST:event_okayButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
	dispose();
	}//GEN-LAST:event_cancelButtonActionPerformed
	
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
	setVisible(false);
	dispose();
	}//GEN-LAST:event_closeDialog

	class DefineConstantTable extends AbstractTableModel
	{
	ArrayList constants;
	
	public DefineConstantTable()
	{
		constants = new ArrayList();
	}
	
	public void addConstant(Constant c)
	{
		constants.add(c);
		fireTableRowsInserted(constants.size()-1, constants.size()-1);
	}
	
	public int getNumConstants()
	{
		return constants.size();
	}
	
	public Constant getConstant(int i)
	{
		return (Constant)constants.get(i);
	}
	
	public int getColumnCount()
	{
		return 3;
	}
	
	public int getRowCount()
	{
		return constants.size();
	}
	
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		
		Constant c = (Constant)constants.get(rowIndex);
		switch(columnIndex)
		{
		case 0: return c.name;
		case 1:
		{
			switch(c.type)
			{
			case Expression.INT: return "int";
			case Expression.DOUBLE: return "double";
			case Expression.BOOLEAN: return "boolean";
			default: return "";
			}
		}
		case 2: return c.value.toString();
		default: return "";
		}
	}
	
	public String getColumnName(int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: return "Name";
		case 1: return "Type";
		case 2: return "Value";
		default: return "";
		}
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		if(columnIndex == 2) return true;
		else return false;
	}
	
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
		if(columnIndex == 2)
		{
		Constant c = (Constant)constants.get(rowIndex);
		String s = (String)aValue;
		c.value = s;
		fireTableCellUpdated(rowIndex, columnIndex);
		}
	}
	
	public String toString()
	{
		String str = "";
		for(int i = 0 ; i < constants.size(); i++)
		{
		Constant c = (Constant)constants.get(i);
		str+=c.toString();
		if(i!= constants.size()-1) str+=",";
		}
		return str;
	}
	
	}
	
	class Constant
	{
	String name;
	int type;
	Object value;
	
	public Constant(String name, int type, Object value)
	{
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	public String toString()
	{
		return name+"="+value.toString();
	}
	}
}
