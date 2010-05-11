// $Id$
/**
 * Copyright 2010 Gerhard Aigner
 * 
 * This file is part of BRISS.
 * 
 * BRISS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * BRISS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * BRISS. If not, see http://www.gnu.org/licenses/.
 */
package at.laborg.briss;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.jpedal.PdfDecoder;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * 
 * @author gerhard
 * 
 */
@SuppressWarnings("serial")
public class Briss extends JFrame implements ActionListener,
		PropertyChangeListener {

	private static final String LOAD = "Load File";
	private static final String CROP = "Crop PDF";

	private PDFPageCluster[] clustersMapping;
	private HashMap<PDFPageCluster, List<Integer>> clusterToPageSet;

	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JButton loadBtn, cropBtn;
	private ClusterPagesTask clusterTask;
	private CropPDFTask cropTask;
	private File origFile = null;
	private File croppedFile = null;

	public Briss() {
		super("BRISS - BRigt Snippet Sire");
		init();
	}

	private File loadPDF(String recommendation, boolean saveDialog) {

		JFileChooser fc = new JFileChooser();
		if (recommendation != null) {
			File recommendedFile = new File(recommendation);
			fc.setSelectedFile(recommendedFile);
		}
		fc.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				if (pathname.isDirectory()) {
					return true;
				}
				return pathname.toString().toLowerCase().endsWith(".pdf");
			}

			@Override
			public String getDescription() {
				return null;
			}
		});
		int retval;
		if (saveDialog) {
			retval = fc.showSaveDialog(this);
		} else {
			retval = fc.showOpenDialog(this);
		}

		if (retval == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	private void init() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLayout(new GridBagLayout());

		previewPanel = new JPanel();
		previewPanel.setLayout(new GridBagLayout());
		previewPanel.setEnabled(true);
		previewPanel.setBackground(Color.BLACK);

		loadBtn = new JButton(LOAD);
		loadBtn.setEnabled(true);
		loadBtn.setPreferredSize(new Dimension(300, 30));
		loadBtn.addActionListener(this);

		cropBtn = new JButton(CROP);
		cropBtn.setEnabled(true);
		cropBtn.setPreferredSize(new Dimension(300, 30));
		cropBtn.addActionListener(this);
		cropBtn.setEnabled(false);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(300, 30));
		progressBar.setEnabled(true);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.CENTER;
		JScrollPane scrollPane = new JScrollPane(previewPanel,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		add(loadBtn, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		add(cropBtn, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		add(progressBar, c);
		pack();
		setVisible(true);

	}

	public static void main(String args[]) {
		new Briss();
	}

	@Override
	public void actionPerformed(ActionEvent aE) {
		if (aE.getActionCommand().equals(LOAD)) {
			File loadFile = loadPDF(null, false);
			if (loadFile != null) {
				origFile = loadFile;
				clustersMapping = null;
				clusterToPageSet = null;
				previewPanel.removeAll();
				progressBar.setString("loading PDF");
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				clusterTask = new ClusterPagesTask();
				clusterTask.addPropertyChangeListener(this);
				clusterTask.execute();
			}
		} else if (aE.getActionCommand().equals(CROP)) {
			// create file recommendation
			String origName = origFile.getAbsolutePath();
			String recommendedName = origName.substring(0,
					origName.length() - 4)
					+ "_cropped.pdf";
			croppedFile = loadPDF(recommendedName, true);
			if (!croppedFile.exists()) {
				try {
					croppedFile.createNewFile();
				} catch (IOException e) {
					// TODO show dialog
				}
			}
			progressBar.setString("loading PDF");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			cropTask = new CropPDFTask();
			cropTask.addPropertyChangeListener(this);
			cropTask.execute();
		}
	}

	private class CropPDFTask extends SwingWorker<Void, Void> {

		public CropPDFTask() {
			super();
		}

		@Override
		protected void done() {
			progressBar.setValue(0);
			progressBar.setString("");
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(croppedFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		protected Void doInBackground() {

			PdfReader reader;
			try {
				reader = new PdfReader(origFile.getAbsolutePath());

				PdfStamper stamper = new PdfStamper(reader,
						new FileOutputStream(croppedFile));

				int pageCount = reader.getNumberOfPages();
				PdfDictionary pageDict;
				for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
					PDFPageCluster clusterInfo = clustersMapping[pageNumber - 1];
					// if no cop was selected do nothing
					if (clusterInfo.getRatios() == null)
						continue;

					pageDict = reader.getPageN(pageNumber);

					List<Rectangle> boxes = new ArrayList<Rectangle>();
					boxes.add(reader.getBoxSize(pageNumber, "media"));
					boxes.add(reader.getBoxSize(pageNumber, "crop"));
					boxes.add(reader.getBoxSize(pageNumber, "trim"));
					boxes.add(reader.getBoxSize(pageNumber, "bleed"));
					int rotation = reader.getPageRotation(pageNumber);

					Rectangle scaledBox = calculateScaledRectangle(boxes,
							clusterInfo.getRatios(), rotation);

					PdfArray scaleBoxArray = new PdfArray();
					scaleBoxArray.add(new PdfNumber(scaledBox.getLeft()));
					scaleBoxArray.add(new PdfNumber(scaledBox.getBottom()));
					scaleBoxArray.add(new PdfNumber(scaledBox.getRight()));
					scaleBoxArray.add(new PdfNumber(scaledBox.getTop()));

					pageDict.put(PdfName.CROPBOX, scaleBoxArray);
					pageDict.put(PdfName.MEDIABOX, scaleBoxArray);
					pageDict.put(PdfName.TRIMBOX, scaleBoxArray);
					pageDict.put(PdfName.BLEEDBOX, scaleBoxArray);

					int percent = (int) ((pageNumber / (float) clustersMapping.length) * 100);
					setProgress(percent);
				}
				stamper.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}

	private Rectangle calculateScaledRectangle(List<Rectangle> boxes,
			float[] ratios, int rotation) {
		if (ratios == null || boxes.size() == 0) {
			return null;
		}
		Rectangle sBox = null;
		// find smallest box
		float smallestSquare = Float.MAX_VALUE;
		for (Rectangle box : boxes) {
			if (box != null) {
				if (sBox == null) {
					sBox = box;
				}
				if (smallestSquare > box.getWidth() * box.getHeight()) {
					// set new smallest box
					smallestSquare = box.getWidth() * box.getHeight();
					sBox = box;
				}
			}
		}
		if (sBox == null) {
			return null; // no useable box was found
		}

		// rotate the ratios according to the rotation of the page
		float[] rotRatios = rotateRatios(ratios, rotation);

		// use smallest box as basis for calculation
		Rectangle scaledBox = new Rectangle(sBox);

		scaledBox.setLeft(sBox.getLeft() + (sBox.getWidth() * rotRatios[0]));
		scaledBox.setBottom(sBox.getBottom()
				+ (sBox.getHeight() * rotRatios[1]));
		scaledBox.setRight(sBox.getLeft()
				+ (sBox.getWidth() * (1 - rotRatios[2])));
		scaledBox.setTop(sBox.getBottom()
				+ (sBox.getHeight() * (1 - rotRatios[3])));

		return scaledBox;
	}

	/**
	 * Rotates the ratios counter clockwise until its at 0
	 * 
	 * @param ratios
	 * @param rotation
	 * @return
	 */
	private float[] rotateRatios(float[] ratios, int rotation) {
		float[] tmpRatios = new float[4];
		for (int i = 0; i < 4; i++) {
			tmpRatios[i] = ratios[i];
		}
		while (rotation > 0 && rotation < 360) {
			float tmpValue = tmpRatios[0];
			// left
			tmpRatios[0] = tmpRatios[1];
			// bottom
			tmpRatios[1] = tmpRatios[2];
			// right
			tmpRatios[2] = tmpRatios[3];
			// top
			tmpRatios[3] = tmpValue;
			rotation += 90;
		}
		return tmpRatios;
	}

	private class ClusterPagesTask extends SwingWorker<PDFPageCluster[], Void> {

		private int pageCount;

		public ClusterPagesTask() {
			super();
			try {
				PdfReader reader = new PdfReader(origFile.getAbsolutePath());
				progressBar.setString("Analysing PDF pages");
				pageCount = reader.getNumberOfPages();
				clustersMapping = new PDFPageCluster[pageCount];
				clusterToPageSet = new HashMap<PDFPageCluster, List<Integer>>();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		@Override
		protected void done() {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			int yposition = 0;
			for (PDFPageCluster cluster : clusterToPageSet.keySet()) {
				MergedPanel p = new MergedPanel(cluster);
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = yposition++;
				c.anchor = GridBagConstraints.CENTER;
				c.insets = new Insets(4, 0, 4, 0);
				previewPanel.add(p, c);
			}
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			cropBtn.setEnabled(true);
			progressBar.setValue(0);
			pack();
		}

		@Override
		protected PDFPageCluster[] doInBackground() throws Exception {

			PdfReader reader = new PdfReader(origFile.getAbsolutePath());

			for (int i = 1; i <= pageCount; i++) {
				Rectangle layoutBox = reader.getBoxSize(i, "crop");
				if (layoutBox == null) {
					layoutBox = reader.getBoxSize(i, "media");
				}
				// create Cluster
				PDFPageCluster tmpCluster = new PDFPageCluster(i % 2 == 0,
						(int) layoutBox.getWidth(), (int) layoutBox.getHeight());

				if (clusterToPageSet.containsKey(tmpCluster)) {
					// cluster exists
					List<Integer> pageNumbers = clusterToPageSet
							.get(tmpCluster);
					pageNumbers.add(i);
				} else {
					// new Cluster
					List<Integer> pageNumbers = new ArrayList<Integer>();
					pageNumbers.add(i);
					clusterToPageSet.put(tmpCluster, pageNumbers);
				}
				setProgress(0);
				int percent = (int) ((i / (float) pageCount) * 100);
				setProgress(percent);
			}
			for (PDFPageCluster key : clusterToPageSet.keySet()) {
				for (Integer pageNumber : clusterToPageSet.get(key)) {
					clustersMapping[pageNumber - 1] = key;
				}
			}

			// now render the pages and create the preview images
			// for every cluster create a set of pages on which the preview will
			// be based
			for (PDFPageCluster cluster : clusterToPageSet.keySet()) {
				cluster.choosePagesToMerge(clusterToPageSet.get(cluster));
			}

			int clusterCounter = 1;
			// create a PdfDecoder using Jpedal library
			PdfDecoder decode_pdf = new PdfDecoder(true);
			decode_pdf.openPdfFile(origFile.getAbsolutePath());
			for (PDFPageCluster cluster : clusterToPageSet.keySet()) {
				progressBar.setString("PDF analysed - creating cluster:"
						+ clusterCounter++);
				progressBar.setValue(0);

				int pageCounter = 0;
				for (Integer pageNumber : cluster.getPagesToMerge()) {
					BufferedImage renderedPage = decode_pdf
							.getPageAsImage(pageNumber);
					cluster.addImageToPreview(renderedPage);

					int percent = (int) ((pageCounter++ / (float) cluster
							.getPagesToMerge().size()) * 100);
					setProgress(percent);
				}

			}
			return clustersMapping;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			progressBar.setValue((Integer) evt.getNewValue());
		}
	}
}
