package org.isf.dicom.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.isf.dicom.manager.AbstractThumbnailViewGui;
import org.isf.dicom.manager.DicomManagerFactory;
import org.isf.dicom.model.FileDicom;
import org.isf.generaldata.GeneralData;
import org.isf.generaldata.MessageBundle;
import org.isf.utils.exception.OHServiceException;
import org.isf.utils.exception.model.OHExceptionMessage;
import org.isf.utils.time.TimeTools;

/**
 * Component for DICOM thumbnails composizion and visualizzation
 * 
 * @author Pietro Castellucci
 * @version 1.0.0
 * 
 */
public class ThumbnailViewGui extends AbstractThumbnailViewGui {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int patID = -1;
	private DicomGui dicomViewer = null;
	private DicomThumbsModel dicomThumbsModel;
	boolean thumbnailViewEnabled = true;
	boolean thumbnails = false;

	/**
	 * Initialize Component
	 * 
	 * @param patID
	 */
	public ThumbnailViewGui(int patID, DicomGui owner) {
		super();
		this.dicomViewer = owner;
		this.patID = patID;
		this.thumbnails = GeneralData.DICOMTHUMBNAILS;

		dicomThumbsModel = new DicomThumbsModel();
		setModel(dicomThumbsModel);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setBackground(Color.DARK_GRAY);
		if (thumbnails) {
			setCellRenderer(new ImageListCellRender());
			setLayoutOrientation(JList.VERTICAL);
		}
		else {
			setCellRenderer(new CellListCellRender());
		}

		getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {

				if (thumbnailViewEnabled && e.getValueIsAdjusting() == false) {
					DefaultListSelectionModel sel = (DefaultListSelectionModel) e.getSource();

					if (sel.isSelectionEmpty())
						disableDeleteButton();
					else
						enableDeleteButton((FileDicom) getModel().getElementAt(sel.getLeadSelectionIndex()));

				}
			}
		});
		addMouseListener(new MouseListener() {

			public void mouseClicked(MouseEvent e) {
				if (thumbnailViewEnabled && 2 == e.getClickCount()) {
					// double click
					detail();
				}
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}
		});
	}

	public void initialize() {
		loadDicomFromDB();
		dicomViewer.enableLoadButton();
		thumbnailViewEnabled = true;
		dicomViewer.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	public void disableLoadButton() {
		thumbnailViewEnabled = false;
		dicomViewer.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		dicomViewer.disableLoadButton();
	}

	private void disableDeleteButton() {
		dicomViewer.disableDeleteButton();
	}

	private void enableDeleteButton(FileDicom selectedDicom) {
		dicomViewer.enableDeleteButton(selectedDicom);
	}

	private void detail() {
		dicomViewer.detail();
	}

	private void loadDicomFromDB() {
		FileDicom[] fdb = null;
		try {
			fdb = DicomManagerFactory.getManager().loadPatientFiles(patID);
		}catch(OHServiceException ex){
			if(ex.getMessages() != null){
				for(OHExceptionMessage msg : ex.getMessages()){
					JOptionPane.showMessageDialog(null, msg.getMessage(), msg.getTitle() == null ? "" : msg.getTitle(), msg.getLevel().getSwingSeverity());
				}
			}
		}
		if (fdb == null)
			fdb = new FileDicom[0];

		dicomThumbsModel.clear();

		for (int i = 0; i < fdb.length; i++)
			dicomThumbsModel.addInstance(fdb[i]);
		;

	}

	public static class DicomThumbsModel extends AbstractListModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private LinkedList<FileDicom> thumbnailList;

		public DicomThumbsModel() {

			thumbnailList = new LinkedList<FileDicom>();

		}

		public Object getElementAt(int index) {
			if (index < 0) {
				return null;
			} else {
				return thumbnailList.get(index);
			}
		}

		public int getSize() {
			return thumbnailList.size();
		}

		public void addInstance(FileDicom instance) {
			thumbnailList.addLast(instance);
			int size = thumbnailList.size();
			fireIntervalAdded(this, size, size);
		}

		public void clear() {
			int size = thumbnailList.size();
			if (size > 0) {
				thumbnailList.clear();
				fireIntervalRemoved(this, 0, size - 1);
			}
		}

	}
	
	private class CellListCellRender implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

			FileDicom instance = (FileDicom) value;
			
			JPanel panel = new JPanel(new BorderLayout(), false);
			panel.setPreferredSize(new Dimension(list.getWidth(), 50));
			//panel.setBackground(Color.DARK_GRAY);
			panel.setToolTipText(getTooltipText(instance));
			
			// Header of thumbnail
			JPanel header = new JPanel(new BorderLayout(), false);
			JLabel date = new JLabel(TimeTools.formatDateTime(instance.getDicomStudyDate(), "dd-MM-yyyy HH:mm"));
			date.setForeground(Color.LIGHT_GRAY);
			JLabel type = new JLabel(instance.getDicomType() == null ? MessageBundle.getMessage("angal.common.notdefined") : instance.getDicomType().toString());
			type.setForeground(Color.LIGHT_GRAY);
			header.add(date, BorderLayout.NORTH);
			header.add(type, BorderLayout.CENTER);
			header.setOpaque(false);
			panel.add(header, BorderLayout.NORTH);
			
			// Center
			JLabel center = new JLabel(instance.getDicomSeriesDescription().toUpperCase());
			center.setForeground(Color.WHITE);
			panel.add(center, BorderLayout.CENTER);

			// Footer of thumbnail
			int frameCount = instance.getFrameCount();
			if (frameCount > 1) {
				JLabel frames = new JLabel("[1/" + instance.getFrameCount() + "]");
				frames.setForeground(Color.YELLOW);
				panel.add(frames, BorderLayout.SOUTH);
			}

			// Colors of thumbnail
			if (isSelected) {
				header.setBackground(Color.BLUE);
				panel.setBackground(Color.BLUE);
				panel.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
				panel.setForeground(Color.WHITE);
			} else {
				header.setBackground(Color.DARK_GRAY);
				panel.setBackground(Color.DARK_GRAY);
				panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
				panel.setForeground(Color.LIGHT_GRAY);
			}

			return panel;

		}
	}

	private class ImageListCellRender implements ListCellRenderer {

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

			FileDicom instance = (FileDicom) value;
			Dimension dim = new Dimension(130, 110);
			
			// Image Cell Panel
			JPanel panel = new JPanel(new BorderLayout(), true);
			//panel.setBounds(0, 0, list.getWidth(), 130);
			panel.setBackground(Color.DARK_GRAY);
			panel.setToolTipText(getTooltipText(instance));

			// Header of thumbnail
			JPanel header = new JPanel(new BorderLayout(), false);
			JLabel date = new JLabel(TimeTools.formatDateTime(instance.getDicomStudyDate(), "dd-MM-yyyy HH:mm"));
			date.setForeground(Color.LIGHT_GRAY);
			JLabel type = new JLabel(instance.getDicomType() == null? MessageBundle.getMessage("angal.common.notdefined") : instance.getDicomType().toString());
			type.setForeground(Color.LIGHT_GRAY);
			JLabel top = new JLabel(instance.getDicomSeriesDescription().toUpperCase());
			top.setForeground(Color.LIGHT_GRAY);
			header.add(date, BorderLayout.NORTH);
			header.add(type, BorderLayout.CENTER);
			header.add(top, BorderLayout.SOUTH);
			panel.add(header, BorderLayout.NORTH);
			
			// Image
			BufferedImage immagine = instance.getDicomThumbnailAsImage();
			JLabel jLab = new JLabel(new ImageIcon(immagine));
			jLab.setPreferredSize(dim);
			jLab.setMaximumSize(dim);
			jLab.setVerticalTextPosition(SwingConstants.BOTTOM);
			jLab.setHorizontalTextPosition(SwingConstants.CENTER);
			panel.add(jLab, BorderLayout.CENTER);

			// Footer of thumbnail
			int frameCount = instance.getFrameCount();
			if (frameCount > 1) {
				JLabel frames = new JLabel("[1/" + instance.getFrameCount() + "]");
				frames.setForeground(Color.YELLOW);
				panel.add(frames, BorderLayout.SOUTH);
			}

			// Colors of thumbnail
			if (isSelected) {
				header.setBackground(Color.BLUE);
				panel.setBackground(Color.BLUE);
				panel.setBorder(BorderFactory.createLineBorder(Color.YELLOW));
				panel.setForeground(Color.WHITE);
			} else {
				header.setBackground(Color.DARK_GRAY);
				panel.setBackground(Color.DARK_GRAY);
				panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
				panel.setForeground(Color.LIGHT_GRAY);
			}

			return panel;

		}
	}

	public FileDicom getSelectedInstance() {
		DicomThumbsModel dicomThumbsModel = (DicomThumbsModel) getModel();
		FileDicom selected = (FileDicom) dicomThumbsModel.getElementAt(getSelectionModel().getMinSelectionIndex());
		return selected;
	}

	public DicomThumbsModel getDicomThumbsModel() {
		return dicomThumbsModel;
	}

	private String getTooltipText(FileDicom dicomFile) {
		String separator = ": ";
		String newline = " <br>";
		StringBuilder rv = new StringBuilder("<html>");
		rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.patient")).append(separator).append(dicomFile.getDicomPatientName());
		if (isValorized(dicomFile.getDicomPatientAge()))
			rv.append("[").append(MessageBundle.getMessage("angal.dicom.thumbnail.age")).append(separator).append(sanitize(dicomFile.getDicomPatientAge())).append("]");
		rv.append(newline);
		rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.modality")).append(separator).append(sanitize(dicomFile.getModality()));
		rv.append(" <br>");
		rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.sernum")).append(separator).append(sanitize(dicomFile.getDicomSeriesNumber()));
		rv.append(" <br>");
		rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.study")).append(separator).append(sanitize(dicomFile.getDicomStudyDescription()));
		rv.append(" <br>");
		rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.series")).append(separator).append(sanitize(dicomFile.getDicomSeriesDescription()));
		rv.append(" <br>");
		rv.append(MessageBundle.getMessage("angal.common.date")).append(separator).append(sanitize(TimeTools.formatDateTime(dicomFile.getDicomSeriesDate(), "dd-MM-yyyy")));
		rv.append(" <br>");
		if (dicomFile.getDicomType() != null)
			rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.category")).append(separator).append(sanitize(dicomFile.getDicomType().getDicomTypeDescription()));
		else
			rv.append(MessageBundle.getMessage("angal.dicom.thumbnail.category")).append(separator).append("N/D");
		rv.append(" <br>");
		rv.append("</html>");
		return rv.toString();
	}

	private String sanitize(String val) {
		if (isValorized(val))
			return val;
		else
			return "";
	}

	private boolean isValorized(String val) {
		return (val != null && val.trim().length() > 0);
	}
}
