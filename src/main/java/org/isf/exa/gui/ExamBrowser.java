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
package org.isf.exa.gui;

/*------------------------------------------
 * ExamBrowser - list all exams. let the user select an exam to edit
 * -----------------------------------------
 * modification history
 * 11/12/2005 - bob  - first beta version 
 * 03/11/2006 - ross - changed button Show into Results 
 * 			         - version is now 1.0 
 * 10/11/2006 - ross - corretto eliminazione esame, prima non si cancellava mai
 *------------------------------------------*/

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.isf.exa.gui.ExamEdit.ExamListener;
import org.isf.exa.manager.ExamBrowsingManager;
import org.isf.exa.model.Exam;
import org.isf.exatype.model.ExamType;
import org.isf.generaldata.MessageBundle;
import org.isf.menu.manager.Context;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.ModalJFrame;

public class ExamBrowser extends ModalJFrame implements ExamListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String VERSION="v1.2"; 
	
	private int selectedrow;
	private JComboBox pbox;
	private ArrayList<Exam> pExam;
	private String[] pColums = {
			MessageBundle.getMessage("angal.common.codem"),
			MessageBundle.getMessage("angal.exa.typem"),
			MessageBundle.getMessage("angal.common.descriptionm"),
			MessageBundle.getMessage("angal.exa.procm"),
			MessageBundle.getMessage("angal.exa.defaultm")
	};
	private int[] pColumwidth = {60,330,160,60,130};
	private Exam exam;
	private DefaultTableModel model ;
	private JTable table;
	private final JFrame myFrame;
	private String pSelection;
	private JButton jButtonNew;
	private JButton jButtonEdit;
	private JButton jButtonClose;
	private JButton jButtonShow;
	private JButton jButtonDelete;
	private JPanel jContentPanel;
	private JPanel buttonPanel;
	private JTextField searchTextField;
	private ExamBrowsingManager manager = Context.getApplicationContext().getBean(ExamBrowsingManager.class);
	
	public ExamBrowser() {
		myFrame=this;
		setTitle(MessageBundle.getMessage("angal.exa.exambrowsing") +" ("+VERSION+")");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screensize = kit.getScreenSize();
        final int pfrmBase = 20;
        final int pfrmWidth = 15;
        final int pfrmHeight = 8;
        this.setBounds((screensize.width - screensize.width * pfrmWidth / pfrmBase ) / 2, (screensize.height - screensize.height * pfrmHeight / pfrmBase)/2, 
                screensize.width * pfrmWidth / pfrmBase, screensize.height * pfrmHeight / pfrmBase);
		
        
        this.setContentPane(getJContentPanel());
		setVisible(true);
	}

	private JPanel getJContentPanel() {
		if (jContentPanel == null) {
			jContentPanel = new JPanel();
			jContentPanel.setLayout(new BorderLayout());
			jContentPanel.add(getJButtonPanel(), java.awt.BorderLayout.SOUTH);
			jContentPanel.add(new JScrollPane(getJTable()),BorderLayout.CENTER);
			
			JPanel panelSearch = new JPanel();
			jContentPanel.add(panelSearch, BorderLayout.NORTH);
			
			JLabel searchLabel = new JLabel(MessageBundle.getMessage("angal.exams.find"));
			panelSearch.add(searchLabel);
			
			searchTextField = new JTextField();
			searchTextField.setColumns(20);
			searchTextField.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					filterExam();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					filterExam();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					filterExam();
				}
			});
			panelSearch.add(searchTextField);
			validate();
		}
		return jContentPanel;
	}

	
	private JPanel getJButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.add(new JLabel(MessageBundle.getMessage("angal.exa.selecttype")));
			buttonPanel.add(getJComboBoxExamType());
			buttonPanel.add(getJButtonNew());
			buttonPanel.add(getJButtonEdit());
			buttonPanel.add(getJButtonDelete());
			buttonPanel.add(getJButtonShow());
			buttonPanel.add(getJButtonClose());
		}
		return buttonPanel;
	}

	private JComboBox getJComboBoxExamType() {
		if (pbox == null) {
			pbox = new JComboBox();
			pbox.addItem(MessageBundle.getMessage("angal.exa.all"));
			try {
				ArrayList<ExamType> types = manager.getExamType();	//for efficiency in the sequent for
				types.forEach(examType -> pbox.addItem(examType));
			} catch (OHServiceException e1) {
				OHServiceExceptionUtil.showMessages(e1);
			}
			pbox.addActionListener(arg0 -> reloadTable());
		}
		return pbox;
	}

	private TableRowSorter<TableModel> sorter;
	private JTable getJTable() {
		if (table == null) {
			model = new ExamBrowsingModel();
			table = new JTable(model);
			table.setAutoCreateColumnsFromModel(false);
			sorter = new TableRowSorter<TableModel>(model);
		    table.setRowSorter(sorter);
			table.getColumnModel().getColumn(0).setMinWidth(pColumwidth[0]);
			table.getColumnModel().getColumn(1).setMinWidth(pColumwidth[1]);
			table.getColumnModel().getColumn(2).setMinWidth(pColumwidth[2]);
			table.getColumnModel().getColumn(3).setMinWidth(pColumwidth[3]);
			table.getColumnModel().getColumn(4).setMinWidth(pColumwidth[4]);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			table.getSelectionModel().addListSelectionListener(e -> {
				if (!e.getValueIsAdjusting()) {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					exam = (Exam) (model.getValueAt(selectedrow, -1));
					if (exam.getProcedure() == 3) {
						jButtonShow.setEnabled(false);
					} else {
						jButtonShow.setEnabled(true);
					}
				}
			});
		}
		return table;
	}

	private JButton getJButtonDelete() {
		jButtonDelete = new JButton(MessageBundle.getMessage("angal.common.delete"));
		jButtonDelete.setMnemonic(KeyEvent.VK_D);
		jButtonDelete.addActionListener(event -> {
			if (table.getSelectedRow() < 0) {
				JOptionPane.showMessageDialog(
						ExamBrowser.this,
						MessageBundle.getMessage("angal.common.pleaseselectarow"),
						MessageBundle.getMessage("angal.hospital"),
						JOptionPane.PLAIN_MESSAGE);
				return;
			}
			selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
			Exam examToDelete = (Exam) (model.getValueAt(selectedrow, -1));
			StringBuilder message = new StringBuilder(MessageBundle.getMessage("angal.exa.deletefolowingexam"))
					.append(" :")
					.append("\n")
					.append(MessageBundle.getMessage("angal.common.code"))
					.append("= ")
					.append(examToDelete.getCode())
					.append("\n")
					.append(MessageBundle.getMessage("angal.common.description"))
					.append("= ")
					.append(examToDelete.getDescription())
					.append("\n?");
			int n = JOptionPane.showConfirmDialog(
					null,
					message.toString(),
					MessageBundle.getMessage("angal.hospital"),
					JOptionPane.YES_NO_OPTION);
			if ((n == JOptionPane.YES_OPTION)) {
				boolean deleted;

				try {
					deleted = manager.deleteExam(examToDelete);
				} catch (OHServiceException e1) {
					deleted = false;
					OHServiceExceptionUtil.showMessages(e1);
				}

				if (true == deleted) {
					reloadTable();
				}
			}
		});
		return jButtonDelete;
	}

	private JButton getJButtonNew() {
            
		if (jButtonNew == null) {
			jButtonNew = new JButton(MessageBundle.getMessage("angal.common.new"));
			jButtonNew.setMnemonic(KeyEvent.VK_N);
			jButtonNew.addActionListener(event -> {
				exam = new Exam("", "", new ExamType("", ""), 0, "");
				ExamEdit newrecord = new ExamEdit(myFrame, exam, true);
				newrecord.addExamListener(ExamBrowser.this);
				newrecord.setVisible(true);
			});
		}
		return jButtonNew;
	}

	private JButton getJButtonEdit() {
		if (jButtonEdit == null) {
			jButtonEdit = new JButton(MessageBundle.getMessage("angal.common.edit"));
			jButtonEdit.setMnemonic(KeyEvent.VK_E);
			jButtonEdit.addActionListener(event -> {
				if (table.getSelectedRow() < 0) {
					JOptionPane.showMessageDialog(
							ExamBrowser.this,
							MessageBundle.getMessage("angal.common.pleaseselectarow"),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.PLAIN_MESSAGE);
				} else {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					exam = (Exam) (model.getValueAt(selectedrow, -1));
					ExamEdit editrecord = new ExamEdit(myFrame, exam, false);
					editrecord.addExamListener(ExamBrowser.this);
					editrecord.setVisible(true);
				}
			});
		}
		return jButtonEdit;
	}
	
	private JButton getJButtonShow() {
		if (jButtonShow == null) {
			jButtonShow = new JButton(MessageBundle.getMessage("angal.exa.results"));
			jButtonShow.setMnemonic(KeyEvent.VK_S);
			jButtonShow.addActionListener(event -> {
				if (table.getSelectedRow() < 0) {
					JOptionPane.showMessageDialog(
							ExamBrowser.this,
							MessageBundle.getMessage("angal.common.pleaseselectarow"),
							MessageBundle.getMessage("angal.hospital"),
							JOptionPane.PLAIN_MESSAGE);
				} else {
					selectedrow = table.convertRowIndexToModel(table.getSelectedRow());
					exam = (Exam) (model.getValueAt(selectedrow, -1));
					new ExamShow(myFrame, exam);
				}
			});
		}
		return jButtonShow;
	}
	
	private JButton getJButtonClose() {
		if (jButtonClose == null) {
			jButtonClose = new JButton(MessageBundle.getMessage("angal.common.close"));
			jButtonClose.setMnemonic(KeyEvent.VK_C);
			jButtonClose.addActionListener(arg0 -> dispose());
		}
		return jButtonClose;
	}

	class ExamBrowsingModel extends DefaultTableModel {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public ExamBrowsingModel(String s) {
			try {
				pExam = manager.getExamsByTypeDescription(s);
                                
			} catch (OHServiceException e) {
				pExam = null;
				OHServiceExceptionUtil.showMessages(e);
			}
		}
		public ExamBrowsingModel() {
			try {
				pExam = manager.getExams();
			} catch (OHServiceException e) {
				pExam = null;
				OHServiceExceptionUtil.showMessages(e);
			}
		}
		public int getRowCount() {
			if (pExam == null)
				return 0;
			return pExam.size();
		}
		
		public String getColumnName(int c) {
			return pColums[c];
		}

		public int getColumnCount() {
			return pColums.length;
		}

		public Object getValueAt(int r, int c) {
			Exam exam = pExam.get(r);
			if(c==-1){
				return exam;
			}
			else if (c == 0) {
				return exam.getCode();
			} else if (c == 1) {
				return exam.getExamtype().getDescription();
			} else if (c == 2) {
				return exam.getDescription();
			} else if (c == 3) {
				return exam.getProcedure();
			} else if (c == 4) {
				return exam.getDefaultResult();
			}
			return null;
		}
		
		@Override
		public boolean isCellEditable(int arg0, int arg1) {
			return false;
		}
	}
	
	@Override
	public void examUpdated(AWTEvent e) {
		reloadTable();
		if ((table.getRowCount() > 0) && selectedrow > -1)
			table.setRowSelectionInterval(selectedrow, selectedrow);
	}

	@Override
	public void examInserted(AWTEvent e) {
		reloadTable();
		if (table.getRowCount() > 0)
			table.setRowSelectionInterval(0, 0);
	}
	
	private void filterExam() {
		String s = searchTextField.getText().trim();
		List<RowFilter<Object, Object>> filters = new ExamFilterFactory().buildFilters(s);
		if(!filters.isEmpty()) {
			sorter.setRowFilter(RowFilter.andFilter(filters));
		}
	}
	
	private void reloadTable() {
		pSelection=pbox.getSelectedItem().toString();
		if (pSelection.compareTo(MessageBundle.getMessage("angal.exa.all")) == 0)
			model = new ExamBrowsingModel();
		else
			model = new ExamBrowsingModel(pSelection);
		model.fireTableDataChanged();
		table.updateUI();
	}

}
