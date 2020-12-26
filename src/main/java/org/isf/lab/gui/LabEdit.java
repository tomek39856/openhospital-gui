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
package org.isf.lab.gui;

/*------------------------------------------
 * LabEdit - Add/edit a laboratory exam
 * -----------------------------------------
 * modification history
 * 02/03/2006 - Davide - first beta version
 * 03/11/2006 - ross - changed title, enlarged window
 *                   - changed note from textfield to textarea
 * 			         - version is now 1.0 
 * 08/11/2006 - ross - added age, sex, exam date, material
 *                   - added editing capability 
 * 18/08/2008 - Teo  - Add scroll capabilities at note JTextArea
 * 13/02/2009 - Alex - add calendar
 *------------------------------------------*/

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.EventListenerList;

import org.isf.admission.manager.AdmissionBrowserManager;
import org.isf.admission.model.Admission;
import org.isf.exa.manager.ExamBrowsingManager;
import org.isf.exa.manager.ExamRowBrowsingManager;
import org.isf.exa.model.Exam;
import org.isf.exa.model.ExamRow;
import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.lab.gui.elements.ExamComboBox;
import org.isf.lab.gui.elements.MatComboBox;
import org.isf.lab.gui.elements.PatientComboBox;
import org.isf.lab.manager.LabManager;
import org.isf.lab.manager.LabRowManager;
import org.isf.lab.model.Laboratory;
import org.isf.lab.model.LaboratoryForPrint;
import org.isf.lab.model.LaboratoryRow;
import org.isf.lab.service.LabIoOperations;
import org.isf.menu.manager.Context;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.serviceprinting.manager.PrintManager;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.gui.OHServiceExceptionUtil;
import org.isf.utils.jobjects.CustomJDateChooser;
import org.isf.utils.jobjects.ModalJFrame;
import org.isf.utils.jobjects.VoLimitedTextField;
import org.isf.utils.time.Converters;
import org.isf.utils.time.RememberDates;

public class LabEdit extends ModalJFrame {

	private static final long serialVersionUID = 1055379190540460482L;

	private EventListenerList labEditListener = new EventListenerList();
	
	public interface LabEditListener extends EventListener {
		void labUpdated();
	}
	
	public void addLabEditListener(LabEditListener l) {
		labEditListener.add(LabEditListener.class, l);
		
	}
	
	private void fireLabUpdated() {
		new AWTEvent(new Object(), AWTEvent.RESERVED_ID_MAX + 1) {
			private static final long serialVersionUID = 1L;
		};

		EventListener[] listeners = labEditListener.getListeners(LabEditListener.class);
		Arrays.stream(listeners)
				.forEach(listener -> ((LabEditListener) listener).labUpdated());
	}

	private static final String VERSION = "v1.1"; 
	
	private boolean insert = false;

	private Laboratory lab = null;
	private JPanel jContentPane = null;
	private JPanel buttonPanel = null;
	private JPanel dataPanel = null;
	private JPanel resultPanel = null;
	private JLabel examLabel = null;
	private JLabel noteLabel = null;
	private JLabel patientLabel = null;
	private JCheckBox inPatientCheckBox = null;
	private JLabel nameLabel = null;
	private JLabel ageLabel = null;
	private JLabel sexLabel = null;
	private JLabel examDateLabel = null;
	private JLabel matLabel = null;
	private JButton okButton = null;
	private JButton cancelButton = null;
	private JButton printButton = null;
	private JComboBox matComboBox = null;
	private ExamComboBox examComboBox = null;
	private JComboBox examRowComboBox = null;
	private PatientComboBox patientComboBox = null;
	private Exam examSelected = null;
	private JScrollPane noteScrollPane = null;

	private JTextArea noteTextArea = null;

	private VoLimitedTextField patTextField = null;
	private VoLimitedTextField ageTextField = null;
	private VoLimitedTextField sexTextField = null;

//	private VoDateTextField examDateField = null;
	private CustomJDateChooser examDateFieldCal = null;
	private LocalDateTime dateIn = null;

	
	private static final Integer panelWidth=500; 
	private static final Integer labelWidth=50; 
	private static final Integer dataPanelHeight=150; 
	private static final Integer resultPanelHeight=350; 
	private static final Integer buttonPanelHeight=40; 
	
	private ExamRowBrowsingManager rowManager = Context.getApplicationContext().getBean(ExamRowBrowsingManager.class);
	private LabManager labManager = Context.getApplicationContext().getBean(LabManager.class,Context.getApplicationContext().getBean(LabIoOperations.class));
	private LabRowManager lRowManager = Context.getApplicationContext().getBean(LabRowManager.class);

	
	private ArrayList<ExamRow> eRows = null;
	private PrintManager printManager = Context.getApplicationContext().getBean(PrintManager.class);
	private Patient patSelected;

	private JTextField examRowTextField;

	private boolean examChanged;

	public LabEdit(JFrame owner, Laboratory laboratory, boolean inserting) {
		insert = inserting;
		lab = laboratory;
		initialize();
	}

	private void initialize() {

		this.setBounds(30,30,panelWidth+20,dataPanelHeight+resultPanelHeight+buttonPanelHeight+30);
		this.setContentPane(getJContentPane());
		this.setResizable(false);
		if (insert) {
			this.setTitle(MessageBundle.getMessage("angal.lab.newlaboratoryexam")+"("+VERSION+")");
		} else {
			this.setTitle(MessageBundle.getMessage("angal.lab.editlaboratoryexam")+"("+VERSION+")");
		}
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}


	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(null);
			// data panel
			jContentPane.add(getDataPanel());
			resultPanel = new JPanel();
			resultPanel.setBounds(0, dataPanelHeight, panelWidth, resultPanelHeight);
			if (!insert) {
				examSelected = lab.getExam();
				if (examSelected.getProcedure() == 1)
					resultPanel = getFirstPanel();
				else if (examSelected.getProcedure() == 2)
					resultPanel = getSecondPanel();
				else if (examSelected.getProcedure() == 3)
					resultPanel = getThirdPanel();
			
			}
			resultPanel.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createLineBorder(Color.GRAY), MessageBundle.getMessage("angal.lab.result")));
			jContentPane.add(resultPanel);
			jContentPane.add(getButtonPanel()); // Generated
		}
		return jContentPane;
	}

	private JPanel getDataPanel() {
		if (dataPanel == null) {
			//initialize data panel
			dataPanel = new JPanel();
			dataPanel.setLayout(null);
			dataPanel.setBounds(0, 0, panelWidth, dataPanelHeight);
			//exam date
			examDateLabel = new JLabel(MessageBundle.getMessage("angal.common.date"));
			examDateLabel.setBounds(5, 10, labelWidth, 20);
			//examDateField=getExamDateField();
			//examDateField.setBounds(labelWidth+5, 10, 70, 20);
			examDateFieldCal = getExamDateFieldCal();
			examDateFieldCal.setLocale(new Locale(GeneralData.LANGUAGE));
			examDateFieldCal.setDateFormatString("dd/MM/yy");
			examDateFieldCal.setBounds(labelWidth+5, 10, 90, 20);
			//material
			matLabel = new JLabel(MessageBundle.getMessage("angal.lab.material"));
			matLabel.setBounds(155, 10, labelWidth, 20);
			matComboBox= getMatComboBox();
			matComboBox.setBounds(215, 10, 280, 20);
			//exam combo
			examLabel = new JLabel(MessageBundle.getMessage("angal.lab.exam"));
			examLabel.setBounds(5, 35, labelWidth, 20);
			examComboBox=getExamComboBox();
			examComboBox.setBounds(labelWidth+5, 35, 440, 20);

			//patient (in or out) data
			patientLabel = new JLabel(MessageBundle.getMessage("angal.lab.patient"));
			patientLabel.setBounds(5, 60, labelWidth, 20);
			inPatientCheckBox = getInPatientCheckBox();
			inPatientCheckBox.setBounds(labelWidth+5, 60, labelWidth, 20);
			patientComboBox = getPatientComboBox();
			patientComboBox.setBounds((labelWidth+5)*2, 60, 385, 20);

			nameLabel = new JLabel(MessageBundle.getMessage("angal.lab.name"));
			nameLabel.setBounds(5, 85, labelWidth, 20);
			patTextField=getPatientTextField();
			patTextField.setBounds(labelWidth+5, 85, 180, 20);
			ageLabel = new JLabel(MessageBundle.getMessage("angal.lab.age"));
			ageLabel.setBounds(250, 85, 35, 20);
			ageTextField=getAgeTextField();
			ageTextField.setBounds(290, 85, 50, 20);
			sexLabel = new JLabel(MessageBundle.getMessage("angal.lab.sexmf"));
			sexLabel.setBounds(375, 85, 80, 20);
			sexTextField=getSexTextField();
			sexTextField.setBounds(445, 85, 50, 20);
			//note			
			noteLabel = new JLabel(MessageBundle.getMessage("angal.lab.note"));
			noteLabel.setBounds(5, 110, labelWidth, 20);
			noteTextArea=getNoteTextArea();
			noteTextArea.setBounds(labelWidth+5, 110, 440, 40);
			noteTextArea.setEditable(true);
			noteTextArea.setWrapStyleWord(true);
			noteTextArea.setAutoscrolls(true);
			
			//add all to the data panel
			dataPanel.add(examDateLabel, null);
			dataPanel.add(examDateFieldCal, null);
			dataPanel.add(matLabel, null);
			dataPanel.add(matComboBox, null);
			dataPanel.add(examLabel, null);
			dataPanel.add(examComboBox, null);
			dataPanel.add(patientLabel, null);
			dataPanel.add(inPatientCheckBox,null);
			dataPanel.add(patientComboBox,null);
			dataPanel.add(nameLabel, null);
			dataPanel.add(patTextField);
			dataPanel.add(ageLabel, null);
			dataPanel.add(ageTextField);
			dataPanel.add(sexLabel, null);
			dataPanel.add(sexTextField);
			dataPanel.add(noteLabel, null);
			
			dataPanel.setPreferredSize(new Dimension(150,200));
			
			/*
			 * Teo : Adding scroll capabilities at note textArea
			 */
			if(noteScrollPane == null)
			{
				noteScrollPane = new JScrollPane(noteTextArea);
				noteScrollPane.setBounds(labelWidth+5, 110, 440, 40);
				noteScrollPane.createVerticalScrollBar();
				noteScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				noteScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				noteScrollPane.setAutoscrolls(true);
				dataPanel.add(noteScrollPane);
			}
			
		}
		return dataPanel;
	}

	private CustomJDateChooser getExamDateFieldCal() {
		LocalDateTime myDate = null;
		if (insert) {
			dateIn = Converters.convertToLocalDateTime(RememberDates.getLastLabExamDateGregorian().getTime());
		} else { 
			dateIn = lab.getExamDate().atStartOfDay();
		}
		if (dateIn != null) {
			myDate = dateIn;
		}
		return (new CustomJDateChooser(myDate, "dd/MM/yy"));
	}
	
	private JCheckBox getInPatientCheckBox() {
		if (inPatientCheckBox == null) {
			inPatientCheckBox = new JCheckBox(MessageBundle.getMessage("angal.lab.in"));
			if (!insert)
				inPatientCheckBox.setSelected(lab.getInOutPatient().equalsIgnoreCase("I"));
			lab.setInOutPatient((inPatientCheckBox.isSelected()?"I":"R"));
		}
		return inPatientCheckBox;
	}

	/*
	 * TODO: Patient Selection like in LabNew
	 * with the difference that here will be optional
	 * If no patient is chosen only Name, Age and Sex will be saved
	 * in LABORATORY table (Name can be empty)
	 */
	private PatientComboBox getPatientComboBox() {
		if (patientComboBox == null) {

			PatientBrowserManager patBrowser = Context.getApplicationContext().getBean(PatientBrowserManager.class);
			ArrayList<Patient> pat = null;
			try {
				pat = patBrowser.getPatient();
			} catch (OHServiceException e) {
				OHServiceExceptionUtil.showMessages(e);
			}
			patientComboBox = PatientComboBox.withPatientsAndPatientFromLaboratorySelected(pat, lab, insert);
			patSelected = patientComboBox.getSelectedPatient().orElse(null);

			patientComboBox.addActionListener(arg0 -> {
				if (patientComboBox.getSelectedIndex()>0) {
					AdmissionBrowserManager admMan = Context.getApplicationContext().getBean(AdmissionBrowserManager.class);
					patSelected = (Patient)patientComboBox.getSelectedItem();
					patTextField.setText(patSelected.getName());
					ageTextField.setText(patSelected.getAge()+"");
					sexTextField.setText(patSelected.getSex()+"");
					Admission admission = null;
					try {
						admission = admMan.getCurrentAdmission(patSelected);
					}catch(OHServiceException e){
						OHServiceExceptionUtil.showMessages(e);
					}
					inPatientCheckBox.setSelected(admission != null ? true : false);
				}
			});
		}
		return patientComboBox;
	}

	private JPanel getButtonPanel() {
		if (buttonPanel == null) {
			buttonPanel = new JPanel();
			buttonPanel.setBounds(0, dataPanelHeight+resultPanelHeight, panelWidth, buttonPanelHeight);
			buttonPanel.add(getOkButton(), null);
			buttonPanel.add(getCancelButton(), null);
			buttonPanel.add(getPrintButton(),null);
		}
		return buttonPanel;
	}

	private ExamComboBox getExamComboBox() {
		if (examComboBox == null) {
			ExamBrowsingManager manager = Context.getApplicationContext().getBean(ExamBrowsingManager.class);
			ArrayList<Exam> exams;
			try {
				exams = manager.getExams();
			} catch (OHServiceException e) {
				exams = null;
				OHServiceExceptionUtil.showMessages(e);
			}
			examComboBox = ExamComboBox.withExamsAndExamFromLaboratorySelected(exams, lab, insert);
			examSelected = examComboBox.getSelectedExam().orElse(null);

			examComboBox.addActionListener(arg0 -> {
				examComboBox.getSelectedExam().ifPresent(exam -> {
					examSelected = exam;

					if (examSelected.getProcedure() == 1)
						resultPanel = getFirstPanel();
					else if (examSelected.getProcedure() == 2)
						resultPanel = getSecondPanel();
					else if (examSelected.getProcedure() == 3)
						resultPanel = getThirdPanel();


					validate();
					repaint();
				});
			});
			resultPanel = null;
		}
		return examComboBox;
	}

	
	private JComboBox getMatComboBox() {
		if (matComboBox == null) {
			LabManager labMan = Context.getApplicationContext().getBean(LabManager.class);
			ArrayList<String> materialList = labMan.getMaterialList();
			matComboBox = MatComboBox.withMaterialsAndMaterialFromLabSelected(materialList, lab, insert, labManager::getMaterialTranslated);
		}
		return matComboBox;
	}

	private JTextArea getNoteTextArea() {
		if (noteTextArea == null) {
			noteTextArea = new JTextArea(10,30);
			if (!insert){
				noteTextArea.setText(lab.getNote());
			}
			noteTextArea.setLineWrap(true);
			noteTextArea.setPreferredSize(new Dimension(10,30));
			noteTextArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		}
		return noteTextArea;
	}
	
	private VoLimitedTextField getPatientTextField() {
		if (patTextField == null) {
			patTextField = new VoLimitedTextField(100);
			if (!insert) {
				patTextField.setText(lab.getPatName());
			}
		}
		return patTextField;
	}
	
	private VoLimitedTextField getAgeTextField() {
		if (ageTextField == null) {
			ageTextField = new VoLimitedTextField(3);
			if (insert) {
				ageTextField.setText("");
				}
			else {
				try {	
					Integer intAge=lab.getAge();
					ageTextField.setText(intAge.toString());
					}
				catch (Exception e) {
					ageTextField.setText("");
					}
				}
			}
		return ageTextField;
	}
	
	private VoLimitedTextField getSexTextField() {
		if (sexTextField == null) {
			sexTextField = new VoLimitedTextField(1);
			if (!insert) {
				sexTextField.setText(lab.getSex());
			}
		}
		return sexTextField;
	}
	
	private JButton getPrintButton() {
		if (printButton == null) {
			printButton = new JButton(MessageBundle.getMessage("angal.lab.print"));
			printButton.setMnemonic(KeyEvent.VK_P);
			printButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {					
					try {
						ArrayList<LaboratoryForPrint> labs = new ArrayList<LaboratoryForPrint>();
						
						labs.add(new LaboratoryForPrint(
								lab.getCode(),
								lab.getExam(),
								lab.getDate(),
								lab.getResult()
							)
						);
						if (!labs.isEmpty()) {
							
							printManager.print("Laboratory",labs,0);
						}
					} catch (OHServiceException e) {
						OHServiceExceptionUtil.showMessages(e);
					}
					
				}

			});
		}
		return printButton;
	}
	
	private JButton getCancelButton() {
		if (cancelButton == null) {
			cancelButton = new JButton();
			cancelButton.setText(MessageBundle.getMessage("angal.common.cancel"));
			cancelButton.setMnemonic(KeyEvent.VK_C);
			cancelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					dispose();
				}
			});
		}
		return cancelButton;
	}

	private JButton getOkButton() {
		if (okButton == null) {
			okButton = new JButton();
			okButton.setText(MessageBundle.getMessage("angal.common.ok"));
			okButton.setMnemonic(KeyEvent.VK_O);
			okButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (examComboBox.getSelectedIndex() == 0) {
						JOptionPane.showMessageDialog(null,MessageBundle.getMessage("angal.lab.pleaseselectanexam"));
						return;
					}
					String matSelected=(String)matComboBox.getSelectedItem();
					examSelected=(Exam)examComboBox.getSelectedItem();
					
					Integer patId=-1;
					if (patientComboBox.getSelectedIndex()>0)
						patId=((Patient)(patientComboBox.getSelectedItem())).getCode();
					String sex=sexTextField.getText().toUpperCase();
					if (!(sex.equals("M") || sex.equals("F"))) {
						JOptionPane.showMessageDialog(null, MessageBundle.getMessage("angal.lab.pleaseinsertmformaleorfforfemale"));
						return;
					}
						
					if (examSelected.getProcedure() == 3 && examRowTextField.getText().isEmpty()) {
						JOptionPane.showMessageDialog(null, MessageBundle.getMessage("angal.labnew.pleaseinsertavalidvalue"));
							return;
					}
					// exam date	
					LocalDateTime examDate = LocalDateTime.now();
					ArrayList<String> labRow = new ArrayList<String>();
					RememberDates.setLastLabExamDate(Converters.toCalendar(examDate));
					
					lab.setDate(LocalDateTime.now());
					lab.setExamDate(examDate.toLocalDate());
					lab.setMaterial(labManager.getMaterialKey(matSelected));
					lab.setExam(examSelected);
					lab.setNote(noteTextArea.getText());
					lab.setInOutPatient((inPatientCheckBox.isSelected()?"I":"O"));
					lab.setPatient(patSelected);
					lab.setPatName(patTextField.getText());
					int tmpAge=0;
					try {
						tmpAge=Integer.parseInt(ageTextField.getText());
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(LabEdit.this, MessageBundle.getMessage("angal.lab.insertvalidage"));
					}
					lab.setSex(sexTextField.getText().toUpperCase());
					
					if (examSelected.getProcedure() == 1)
						lab.setResult(examRowComboBox.getSelectedItem().toString());
					else if (examSelected.getProcedure() == 2) {
						lab.setResult(MessageBundle.getMessage("angal.lab.multipleresults"));
						labRow = IntStream.range(0, resultPanel.getComponentCount())
									.filter(i -> ((SubPanel) resultPanel.getComponent(i)).getSelectedResult().equalsIgnoreCase("P"))
									.mapToObj(i -> eRows.get(i).getDescription())
									.collect(Collectors.toCollection(ArrayList::new));
					}else if (examSelected.getProcedure() == 3) {
						lab.setResult(examRowTextField.getText());
					}
					boolean result = false;
					if (insert) {
						lab.setAge(tmpAge);
						try {
							result = labManager.newLaboratory(lab,	labRow);
						} catch (OHServiceException e1) {
							result = false;
							OHServiceExceptionUtil.showMessages(e1);
						}
					} else {
						try {
							result = labManager.updateLaboratory(lab, labRow);
						} catch (OHServiceException e1) {
							result = false;
							OHServiceExceptionUtil.showMessages(e1);
						}
					}
					if (!result)
						JOptionPane.showMessageDialog(null,
								MessageBundle.getMessage("angal.sql.thedatacouldnotbesaved"));
					else {
						fireLabUpdated();
						dispose();
					}
				}
			});
		}
		return okButton;
	}

	private JPanel getFirstPanel() {
		resultPanel.removeAll();
		String result="";
		examRowComboBox = new JComboBox();
		examRowComboBox.setMaximumSize(new Dimension(200, 25));
		examRowComboBox.setMinimumSize(new Dimension(200, 25));
		examRowComboBox.setPreferredSize(new Dimension(200, 25));
		if (insert) {
			result=examSelected.getDefaultResult();
		} else {
			result=lab.getResult();
		}
		examRowComboBox.addItem(result);

		try {
			List<ExamRow> rows = rowManager.getExamRowByExamCode(examSelected.getCode());
			String finalResult = result;
			rows.stream()
					.filter(r -> !r.getDescription().equals(finalResult))
					.forEach(r -> examRowComboBox.addItem(r.getDescription()));
		} catch (OHServiceException e) {
			OHServiceExceptionUtil.showMessages(e);
		}
		if (examRowComboBox.getItemCount() > 0) resultPanel.add(examRowComboBox);

		return resultPanel;
	}

	private JPanel getSecondPanel() {
		resultPanel.removeAll();
		resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));
		String examId = examSelected.getCode();
		eRows = null;
		try {
			eRows = rowManager.getExamRowByExamCode(examId);
		} catch (OHServiceException e1) {
			OHServiceExceptionUtil.showMessages(e1);
		}
		
		if (insert) {
			if (null != eRows) {
				eRows.forEach(r -> resultPanel.add(new SubPanel(r, "N")));
			}
		} else {

			ArrayList<LaboratoryRow> lRows;
			try {
				lRows = lRowManager.getLabRowByLabId(lab.getCode());
			} catch (OHServiceException e) {
				lRows = new ArrayList<LaboratoryRow>();
				OHServiceExceptionUtil.showMessages(e);
			}
			boolean find;
			if (null != eRows) {
				for (ExamRow r : eRows) {
					find = false;
					for (LaboratoryRow lR : lRows) {
						if (r.getDescription()
								.equalsIgnoreCase(lR.getDescription()))
							find = true;
					}
					if (find) {
						resultPanel.add(new SubPanel(r, "P"));
					} else {
						resultPanel.add(new SubPanel(r, "N"));
					}
				}
			}
		}
		return resultPanel;
	}
	private JPanel getThirdPanel() {
		resultPanel.removeAll();
		String result="";
		examRowTextField = new JTextField();
		examRowTextField.setMaximumSize(new Dimension(200, 25));
		examRowTextField.setMinimumSize(new Dimension(200, 25));
		examRowTextField.setPreferredSize(new Dimension(200, 25));
		if (insert | examChanged) {
			result=examSelected.getDefaultResult();
			examChanged = false;
		} else {
			result=lab.getResult();
		}
		examRowTextField.setText(result);

		resultPanel.add(examRowTextField);

		return resultPanel;
	}
	class SubPanel extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private JLabel label = null;

		private JRadioButton radioPos = null;

		private JRadioButton radioNeg = null;

		private ButtonGroup group = null;

		public SubPanel(ExamRow row, String result) {
			this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			label = new JLabel(row.getDescription());
			this.add(label);

			group = new ButtonGroup();
			radioPos = new JRadioButton(MessageBundle.getMessage("angal.lab.p"));
			radioNeg = new JRadioButton(MessageBundle.getMessage("angal.lab.n"));
			group.add(radioPos);
			group.add(radioNeg);

			this.add(radioPos);
			this.add(radioNeg);
			if (result.equals(MessageBundle.getMessage("angal.lab.p")))
				radioPos.setSelected(true);
			else
				radioNeg.setSelected(true);
		}

		public String getSelectedResult() {
			if (radioPos.isSelected())
				return MessageBundle.getMessage("angal.lab.p");
			else
				return MessageBundle.getMessage("angal.lab.n");
		}

	}

}
