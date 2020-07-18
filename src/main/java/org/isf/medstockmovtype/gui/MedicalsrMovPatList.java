package org.isf.medstockmovtype.gui;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.isf.medicalstockward.manager.MovWardBrowserManager;
import org.isf.medicalstockward.model.MovementWard;
import org.isf.menu.manager.Context;
import org.isf.patient.model.Patient;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.jobjects.OhDefaultCellRenderer;
import org.isf.utils.jobjects.OhTableDrugsModel;

public class MedicalsrMovPatList extends JPanel {
	private Patient myPatient;
	private ArrayList<MovementWard> drugsData;
	private JDialog dialogDrug;
	private JTable JtableData;
	private OhTableDrugsModel<MovementWard> modelMedWard;
	private OhDefaultCellRenderer cellRenderer = new OhDefaultCellRenderer();
	private MovWardBrowserManager movManager = Context.getApplicationContext().getBean(MovWardBrowserManager.class);
	public MedicalsrMovPatList(Object object) {
		
		setLayout(new BorderLayout(0, 0));
		JPanel panelData = new JPanel();
		add(panelData);
		panelData.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPaneData = new JScrollPane();
		panelData.add(scrollPaneData);
	
		if (object instanceof Patient) {
			myPatient = (Patient) object;
		}
		
		if (myPatient != null) {
			MovWardBrowserManager movManager = Context.getApplicationContext().getBean(MovWardBrowserManager.class);
			try {
				ArrayList<MovementWard> movPat = movManager.getMovementToPatient(myPatient);
				drugsData = new ArrayList<MovementWard>();
				for (MovementWard mov : movPat) {
					drugsData.add(mov);
				}
			} catch (OHServiceException ex) {
				ex.printStackTrace();
			} 
			
		}
		JtableData = new JTable();
		scrollPaneData.setViewportView(JtableData);
		/*** apply default oh cellRender *****/
		JtableData.setDefaultRenderer(Object.class, cellRenderer);
		JtableData.setDefaultRenderer(Double.class, cellRenderer);
		
		
		modelMedWard = new OhTableDrugsModel<MovementWard>(drugsData);

		JtableData.setModel(modelMedWard);
		dialogDrug = new JDialog();
		dialogDrug.setLocationRelativeTo(null);
		dialogDrug.setSize(450, 280);
		dialogDrug.setLocationRelativeTo(null);
		dialogDrug.setModal(true);
	}
	
	public List<MovementWard> getDrugsData() {
		return drugsData;
	}
}
