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
package org.isf.exatype.gui;

/*------------------------------------------
 * ExamTypeEdit - inset/edit an exam type.
 * -----------------------------------------
 * modification history
 * ??/??/2005 - first beta version (former name ExamTypeBrowserEdit)
 * 03/11/2006 - ross - version is now 1.0
 *------------------------------------------*/

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EventListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.EventListenerList;

import org.isf.exatype.manager.ExamTypeBrowserManager;
import org.isf.exatype.model.ExamType;
import org.isf.generaldata.MessageBundle;
import org.isf.menu.manager.Context;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.utils.jobjects.VoLimitedTextField;

public class ExamTypeEdit extends JDialog{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String VERSION="v1.2"; 

    private EventListenerList examTypeListeners = new EventListenerList();

    public interface ExamTypeListener extends EventListener {
        void examTypeUpdated(AWTEvent e);
        void examTypeInserted(AWTEvent e);
    }

    public void addExamTypeListener(ExamTypeListener l) {
        examTypeListeners.add(ExamTypeListener.class, l);
    }

    public void removeExamTypeListener(ExamTypeListener listener) {
        examTypeListeners.remove(ExamTypeListener.class, listener);
    }

    private void fireExamTypeInserted() {
		AWTEvent event = new AWTEvent(new Object(), AWTEvent.RESERVED_ID_MAX + 1) {
			private static final long serialVersionUID = 1L;
		};

		EventListener[] listeners = examTypeListeners.getListeners(ExamTypeListener.class);
		Arrays.stream(listeners).forEach(listener -> ((ExamTypeListener) listener).examTypeInserted(event));
    }

	private void fireExamTypeUpdated() {
		AWTEvent event = new AWTEvent(new Object(), AWTEvent.RESERVED_ID_MAX + 1) {
			private static final long serialVersionUID = 1L;
		};

		EventListener[] listeners = examTypeListeners.getListeners(ExamTypeListener.class);
		Arrays.stream(listeners).forEach(listener -> ((ExamTypeListener) listener).examTypeUpdated(event));
	}
    
	private JPanel jContentPane = null;
	private JPanel dataPanel = null;
	private JPanel buttonPanel = null;
	private JButton cancelButton = null;
	private JButton okButton = null;
	private JTextField descriptionTextField = null;
	private VoLimitedTextField codeTextField = null;
	private String lastdescription;
	private ExamType examType = null;
	private boolean insert;
	private JPanel jDataPanel = null;
	private JLabel jCodeLabel = null;
	private JPanel jCodeLabelPanel = null;
	private JPanel jDescriptionLabelPanel = null;
	private JLabel jDescripitonLabel = null;
	/**
     * 
	 * This is the default constructor; we pass the arraylist and the selectedrow
     * because we need to update them
	 */
	public ExamTypeEdit(JFrame owner,ExamType old,boolean inserting) {
		super(owner,true);
		insert = inserting;
		examType = old;//disease will be used for every operation
		lastdescription= examType.getDescription();
		initialize();
	}


	private void initialize() {
		this.setContentPane(getJContentPane());
		if (insert) {
			this.setTitle(MessageBundle.getMessage("angal.exatype.newexamtype")+"  ("+VERSION+")");
		} else {
			this.setTitle(MessageBundle.getMessage("angal.exatype.editexamtype")+"  ("+VERSION+")");
		}
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		this.pack();
		this.setLocationRelativeTo(null);
	}

	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout());
			jContentPane.add(getDataPanel(), java.awt.BorderLayout.NORTH);  // Generated
			jContentPane.add(getButtonPanel(), java.awt.BorderLayout.SOUTH);  // Generated
		}
		return jContentPane;
	}

	private JPanel getDataPanel() {
		if (dataPanel == null) {
			dataPanel = new JPanel();
			//dataPanel.setLayout(new BoxLayout(getDataPanel(), BoxLayout.Y_AXIS));  // Generated
			dataPanel.add(getJDataPanel(), null);
		}
		return dataPanel;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.add(getOkButton(), null);  // Generated
			buttonPanel.add(getCancelButton(), null);  // Generated
		}
		return buttonPanel;
	}

	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(MessageBundle.getMessage("angal.common.cancel"));  // Generated
			cancelButton.setMnemonic(KeyEvent.VK_C);
			cancelButton.addActionListener(e -> dispose());
		}
		return cancelButton;
	}

	/**
	 * This method initializes okButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(MessageBundle.getMessage("angal.common.ok"));  // Generated
			okButton.setMnemonic(KeyEvent.VK_O);
			okButton.addActionListener(e -> {
				ExamTypeBrowserManager manager = Context.getApplicationContext().getBean(ExamTypeBrowserManager.class);
				try {
					examType.setDescription(descriptionTextField.getText());
					examType.setCode(codeTextField.getText());

					if (insert) {     // inserting
						if (manager.newExamType(examType)) {
							fireExamTypeInserted();
							dispose();
						} else
							JOptionPane.showMessageDialog(null, MessageBundle.getMessage("angal.sql.thedatacouldnotbesaved"));
					} else {            // updating
						if (descriptionTextField.getText().equals(lastdescription)) {
							dispose();
						} else {
							if (manager.updateExamType(examType)) {
								fireExamTypeUpdated();
								dispose();
							} else
								JOptionPane.showMessageDialog(null, MessageBundle.getMessage("angal.sql.thedatacouldnotbesaved"));
						}
					}
				} catch (OHServiceException ex) {
					if (ex.getMessages() != null) {
						for (OHExceptionMessage msg : ex.getMessages()) {
							JOptionPane.showMessageDialog(null, msg.getMessage(), msg.getTitle() == null ? "" : msg.getTitle(), msg.getLevel().getSwingSeverity());
						}
					}
				}
			});
		}
		return okButton;
	}


	private JTextField getDescriptionTextField() {
		if (descriptionTextField == null) {
			descriptionTextField = new JTextField(20);
			if (!insert) {
				descriptionTextField.setText(examType.getDescription());
				lastdescription=examType.getDescription();
			} 
		}
		return descriptionTextField;
	}
	
	private JTextField getCodeTextField() {
		if (codeTextField == null) {
			codeTextField = new VoLimitedTextField(2);
			if (!insert) {
				codeTextField.setText(examType.getCode());
				codeTextField.setEnabled(false);
			}
		}
		return codeTextField;
	}

	private JPanel getJDataPanel() {
		if (jDataPanel == null) {
			jDataPanel = new JPanel();
			jDataPanel.setLayout(new BoxLayout(getJDataPanel(),BoxLayout.Y_AXIS));
			jDataPanel.add(getJCodeLabelPanel(), null);
			jDataPanel.add(getCodeTextField(), null);
			jDataPanel.add(getJDescriptionLabelPanel(), null);
			jDataPanel.add(getDescriptionTextField(), null);
		}
		return jDataPanel;
	}

	private JLabel getJCodeLabel() {
		if (jCodeLabel == null) {
			jCodeLabel = new JLabel();
			jCodeLabel.setText(MessageBundle.getMessage("angal.exatype.codemaxchars"));
		}
		return jCodeLabel;
	}

	private JPanel getJCodeLabelPanel() {
		if (jCodeLabelPanel == null) {
			jCodeLabelPanel = new JPanel();
			//jCodeLabelPanel.setLayout(new BorderLayout());
			jCodeLabelPanel.add(getJCodeLabel(), BorderLayout.CENTER);
		}
		return jCodeLabelPanel;
	}

	private JPanel getJDescriptionLabelPanel() {
		if (jDescriptionLabelPanel == null) {
			jDescripitonLabel = new JLabel();
			jDescripitonLabel.setText(MessageBundle.getMessage("angal.common.description"));
			jDescriptionLabelPanel = new JPanel();
			jDescriptionLabelPanel.add(jDescripitonLabel, null);
		}
		return jDescriptionLabelPanel;
	}
	


}


