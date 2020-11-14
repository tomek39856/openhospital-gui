/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2020 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isf.agetype.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.isf.agetype.manager.AgeTypeBrowserManager;
import org.isf.agetype.model.AgeType;
import org.isf.generaldata.MessageBundle;
import org.isf.menu.manager.Context;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.ModalJFrame;

/**
 * Browsing of table AgeType
 * 
 * @author Alessandro
 * 
 */

public class AgeTypeBrowser extends ModalJFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<AgeType> pAgeType;
	private String[] pColums = {
			MessageBundle.getMessage("angal.common.code"),
			MessageBundle.getMessage("angal.agetype.from"),
			MessageBundle.getMessage("angal.agetype.to"),
			MessageBundle.getMessage("angal.common.description")
	};
	private int[] pColumwidth = { 80, 80, 80, 200 };
	private JPanel jContainPanel = null;
	private JPanel jButtonPanel = null;
	private JButton jEditSaveButton = null;
	private JButton jCloseButton = null;
	private JTable jTable = null;
	private AgeTypeBrowserModel model;
	private boolean edit = false;

	/**
	 * This method initializes
	 * 
	 */
	public AgeTypeBrowser() {
		super();
		initialize();
		setVisible(true);
	}

	private void initialize() {
		this.setTitle(MessageBundle.getMessage("angal.agetype.agetypebrowsing"));
		this.setContentPane(getJContainPanel());
		this.pack();
		this.setLocationRelativeTo(null);
	}

	private JPanel getJContainPanel() {
		if (jContainPanel == null) {
			jContainPanel = new JPanel();
			jContainPanel.setLayout(new BorderLayout());
			jContainPanel.add(getJButtonPanel(), java.awt.BorderLayout.SOUTH);
			jContainPanel.add(getJTable(), BorderLayout.CENTER);
			validate();
		}
		return jContainPanel;
	}

	private JPanel getJButtonPanel() {
		if (jButtonPanel == null) {
			jButtonPanel = new JPanel();
			jButtonPanel.add(getJEditSaveButton(), null);
			jButtonPanel.add(getJCloseButton(), null);
		}
		return jButtonPanel;
	}

	/**
	 * This method initializes jEditButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJEditSaveButton() {
		if (jEditSaveButton == null) {
			jEditSaveButton = new JButton();
			jEditSaveButton.setText(MessageBundle.getMessage("angal.common.edit"));
			jEditSaveButton.setMnemonic(KeyEvent.VK_E);
			jEditSaveButton.addActionListener(event -> {
				if (!edit) {
					edit = true;
					jEditSaveButton.setText(MessageBundle.getMessage("angal.common.save"));
					jEditSaveButton.setMnemonic(KeyEvent.VK_S);
					jTable.updateUI();

				} else {
					if (jTable.isEditing()) {
						jTable.getCellEditor().stopCellEditing();
					}
					AgeTypeBrowserManager manager = Context.getApplicationContext().getBean(AgeTypeBrowserManager.class);
					try {
						manager.updateAgeType(pAgeType);
					} catch (OHServiceException e) {
						OHServiceExceptionUtil.showMessages(e);
					}
					edit = false;
					jTable.updateUI();
					jEditSaveButton.setText(MessageBundle.getMessage("angal.common.edit"));
					jEditSaveButton.setMnemonic(KeyEvent.VK_E);
				}
			});
		}
		return jEditSaveButton;
	}
	
	/**
	 * This method initializes jCloseButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJCloseButton() {
		if (jCloseButton == null) {
			jCloseButton = new JButton();
			jCloseButton.setText(MessageBundle.getMessage("angal.common.close"));
			jCloseButton.setMnemonic(KeyEvent.VK_C);
			jCloseButton.addActionListener(arg0 -> dispose());
		}
		return jCloseButton;
	}

	public JTable getJTable() {
		if (jTable == null) {
			model = new AgeTypeBrowserModel();
			jTable = new JTable(model);
			IntStream.range(0, pColums.length)
					.forEachOrdered(i -> jTable.getColumnModel().getColumn(i).setMinWidth(pColumwidth[i]));
			jTable.setDefaultRenderer(Object.class,new ColorTableCellRenderer());
		}
		return jTable;
	}

	class AgeTypeBrowserModel extends DefaultTableModel {

		/**
	     * 
	     */
		private static final long serialVersionUID = 1L;
		private AgeTypeBrowserManager manager = Context.getApplicationContext().getBean(AgeTypeBrowserManager.class);
		
		public AgeTypeBrowserModel() {
			try {
				pAgeType = manager.getAgeType();
			}catch(OHServiceException e){
				pAgeType = new ArrayList<>();
				OHServiceExceptionUtil.showMessages(e);
			}
		}

		public int getRowCount() {
			if (pAgeType == null)
				return 0;
			return pAgeType.size();
		}

		public String getColumnName(int c) {
			return pColums[c];
		}

		public int getColumnCount() {
			return pColums.length;
		}

		public Object getValueAt(int r, int c) {
			if (c == 0) {
				return pAgeType.get(r).getCode();
			} else if (c == -1) {
				return pAgeType.get(r);
			} else if (c == 1) {
				return pAgeType.get(r).getFrom();
			} else if (c == 2) {
				return pAgeType.get(r).getTo();
			} else if (c == 3) {
				return MessageBundle.getMessage(pAgeType.get(r).getDescription());
			}
			return null;
		}
		
		public void setValueAt(Object value, int row, int col) {
			int number;
			try {
				number = Integer.valueOf((String) value);
			} catch (NumberFormatException e) {
				JOptionPane.showMessageDialog(AgeTypeBrowser.this, 
						MessageBundle.getMessage("angal.agetype.insertvalidage"));
				return;
			}
			
			if (col == 1) {
				pAgeType.get(row).setFrom(number);
			} else if (col == 2) {
				pAgeType.get(row).setTo(number);
			}
	        fireTableCellUpdated(row, col);
	    }

		@Override
		public boolean isCellEditable(int r, int c) {
			if (edit) {
				if (c == 1 || c == 2)
					return true;
				else
					return false;
			} else
				return false;
		}
	}
	
	class ColorTableCellRenderer extends DefaultTableCellRenderer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (edit) {
				if (column == 0 || column == 3)
					cell.setBackground(Color.LIGHT_GRAY);
				else cell.setBackground(Color.WHITE);
			} else cell.setBackground(Color.WHITE);
			return cell;
		}
	}
}
