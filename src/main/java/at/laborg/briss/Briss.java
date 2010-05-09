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
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.Node;
import multivalent.ParseException;
import multivalent.std.adaptor.pdf.PDF;

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

	private PDFPageClusterInfo[] clustersMapping;
	private HashMap<PDFPageClusterInfo, List<Integer>> clusterToPageSet;

	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JButton loadBtn, cropBtn;
	private ClusterPagesTask clusterTask;
	private CropPDFTask cropTask;
	private File origFile = null;
	private File croppedFile = null;

	private final static int MAX_DOC_X = 30;
	private final static int MAX_DOC_Y = 30;

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
		add(new JScrollPane(previewPanel), c);
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
			// TODO clear all previously used things
			origFile = loadPDF(null, false);
			if (origFile != null) {
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
					PDFPageClusterInfo clusterInfo = clustersMapping[pageNumber - 1];

					pageDict = reader.getPageN(pageNumber);

					List<PdfArray> boxList = new ArrayList<PdfArray>();
					boxList.add(pageDict.getAsArray(PdfName.MEDIABOX));
					boxList.add(pageDict.getAsArray(PdfName.CROPBOX));
					boxList.add(pageDict.getAsArray(PdfName.TRIMBOX));
					boxList.add(pageDict.getAsArray(PdfName.BLEEDBOX));
					int rotation = pageDict.getAsNumber(PdfName.ROTATE)
							.intValue();

					PdfArray scaledBox = calculateScaledBox(boxList,
							clusterInfo.getRatios(), rotation);

					pageDict.put(PdfName.CROPBOX, scaledBox);
					pageDict.put(PdfName.MEDIABOX, scaledBox);
					pageDict.put(PdfName.TRIMBOX, scaledBox);
					pageDict.put(PdfName.BLEEDBOX, scaledBox);

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

	// TODO change ratios to pdf format
	/**
	 * ratios = x1,x2, y1,y2 pdf orientation (x1, y1, x2, y2) left bottom, right
	 * top
	 * 
	 * @param boxes
	 * @param ratios
	 * @param rotation
	 * @return
	 */
	private PdfArray calculateScaledBox(List<PdfArray> boxes, float[] ratios,
			int rotation) {
		if (ratios == null || boxes.size() == 0) {
			return null;
		}
		// find smallest box
		int smallestIndex = -1;
		float smallestSquare = Float.MAX_VALUE;
		for (PdfArray box : boxes) {
			if (box != null) {
				PdfNumber x1 = (PdfNumber) box.getAsNumber(0);
				PdfNumber y1 = (PdfNumber) box.getAsNumber(1);
				PdfNumber x2 = (PdfNumber) box.getAsNumber(2);
				PdfNumber y2 = (PdfNumber) box.getAsNumber(3);
				int boxWidth = x2.intValue() - x1.intValue();
				int boxHeight = y2.intValue() - y1.intValue();
				if (smallestSquare > boxWidth * boxHeight) {
					// set new smallest box
					smallestSquare = boxWidth * boxHeight;
					smallestIndex = boxes.indexOf(box);
				}
			}
		}
		if (smallestIndex == -1) {
			return null; // now useable box was found
		}

		// pdf pages can be rotated, so adjust the cutratios according to the
		// ROTATE from the pdf pagedict

		// use smallest box as basis for calculation
		PdfNumber x1 = (PdfNumber) boxes.get(smallestIndex).getAsNumber(0);
		PdfNumber y1 = (PdfNumber) boxes.get(smallestIndex).getAsNumber(1);
		PdfNumber x2 = (PdfNumber) boxes.get(smallestIndex).getAsNumber(2);
		PdfNumber y2 = (PdfNumber) boxes.get(smallestIndex).getAsNumber(3);
		int boxWidth = x2.intValue() - x1.intValue();
		int boxHeight = y2.intValue() - y1.intValue();

		// create a new pdfbox to return
		int x1Scaled = (int) (x1.intValue() + (boxWidth * ratios[0]));
		int y1Scaled = (int) (y1.intValue() + (boxHeight * ratios[1]));
		int x2Scaled = (int) (x1.intValue() + (boxWidth * ratios[2]));
		int y2Scaled = (int) (y1.intValue() + (boxHeight * ratios[3]));

		PdfArray scaledBox = new PdfArray();
		scaledBox.add(new PdfNumber(x1Scaled));
		scaledBox.add(new PdfNumber(y1Scaled));
		scaledBox.add(new PdfNumber(x2Scaled));
		scaledBox.add(new PdfNumber(y2Scaled));

		return scaledBox;
	}

	private class ClusterPagesTask extends
			SwingWorker<PDFPageClusterInfo[], Void> {

		private int pageCount;

		public ClusterPagesTask() {
			super();
			PDF pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF", null,
					null, null);
			progressBar.setString("Analysing PDF pages");
			try {
				pdf.setInput(origFile);
				Document doc = new Document("doc", null, null);
				pdf.parse(doc);
				doc.clear();
				pageCount = pdf.getReader().getPageCnt();
				clustersMapping = new PDFPageClusterInfo[pageCount];
				clusterToPageSet = new HashMap<PDFPageClusterInfo, List<Integer>>();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		@Override
		protected void done() {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			int yposition = 0;
			for (PDFPageClusterInfo cluster : clusterToPageSet.keySet()) {
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
		protected PDFPageClusterInfo[] doInBackground() throws Exception {
			PDF pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF", null,
					null, null);
			Document doc = new Document("doc", null, null);
			try {

				for (int i = 1; i <= pageCount; i++) {
					pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF",
							null, null, null);
					pdf.setInput(origFile);
					doc = new Document("doc", null, null);
					pdf.parse(doc);
					doc.clear();
					doc.putAttr(Document.ATTR_PAGE, Integer.toString(i));
					pdf.parse(doc);
					Node top = doc.childAt(0);
					doc.formatBeforeAfter(MAX_DOC_X, MAX_DOC_Y, null);
					int w = top.bbox.width;
					int h = top.bbox.height;

					// create Cluster
					PDFPageClusterInfo tmpCluster = new PDFPageClusterInfo(
							i % 2 == 0, w, h);

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

					pdf.getReader().close();
					doc = null;
					setProgress(0);
					int percent = (int) ((i / (float) pageCount) * 100);
					setProgress(percent);
				}
				for (PDFPageClusterInfo key : clusterToPageSet.keySet()) {
					for (Integer pageNumber : clusterToPageSet.get(key)) {
						clustersMapping[pageNumber - 1] = key;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// for every cluster create a set of pages on which the preview will
			// be based
			for (PDFPageClusterInfo cluster : clusterToPageSet.keySet()) {
				cluster.choosePagesToMerge(clusterToPageSet.get(cluster));
			}

			int clusterCounter = 1;
			for (PDFPageClusterInfo cluster : clusterToPageSet.keySet()) {
				WritableRaster raster = null;
				double[][] imageData = null;
				progressBar.setString("PDF analysed - creating cluster:"
						+ clusterCounter++);
				progressBar.setValue(0);

				int pageCounter = 0;
				for (Integer pageNumber : cluster.getPagesToMerge()) {
					pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF",
							null, null, null);
					pdf.setInput(origFile);
					doc = new Document("doc", null, null);
					pdf.parse(doc);
					doc.clear();
					doc.putAttr(Document.ATTR_PAGE, Integer
							.toString(pageNumber));
					pdf.parse(doc);

					Node top = doc.childAt(0);

					doc.formatBeforeAfter(MAX_DOC_X, MAX_DOC_Y, null);
					BufferedImage img = new BufferedImage(cluster
							.getPageWidth(), cluster.getPageHeight(),
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g = img.createGraphics();
					g.setClip(0, 0, cluster.getPageWidth(), cluster
							.getPageHeight());

					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_RENDERING,
							RenderingHints.VALUE_RENDER_QUALITY);

					Context cx = doc.getStyleSheet().getContext(g, null);
					top.paintBeforeAfter(g.getClipBounds(), cx);

					BufferedImage preview = cluster.getPreviewImage();
					if (preview == null) {
						preview = new BufferedImage(img.getWidth(), img
								.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
						imageData = new double[img.getWidth()][img.getHeight()];
						raster = preview.getRaster()
								.createCompatibleWritableRaster();
						cluster.setPreviewImage(preview);
					}
					average(img, imageData);

					doc.removeAllChildren();
					cx.reset();
					g.dispose();
					pdf.getReader().close();
					doc = null;
					int percent = (int) ((pageCounter++ / (float) cluster
							.getPagesToMerge().size()) * 100);
					setProgress(percent);
				}
				for (int k = 0; k < cluster.getPreviewImage().getHeight(); ++k) {
					for (int j = 0; j < cluster.getPreviewImage().getWidth(); ++j) {
						raster.setSample(j, k, 0, Math.round(imageData[j][k]
								/ (cluster.getPagesToMerge().size())));
					}
				}
				cluster.getPreviewImage().setData(raster);
			}

			return clustersMapping;
		}
	}

	static void average(BufferedImage image, double[][] values) {
		for (int k = 0; k < image.getHeight(); ++k) {
			for (int j = 0; j < image.getWidth(); ++j) {
				values[j][k] += image.getRaster().getSample(j, k, 0);
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			progressBar.setValue((Integer) evt.getNewValue());
		}
	}
}
