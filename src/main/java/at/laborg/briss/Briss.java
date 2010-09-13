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
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import org.jpedal.PdfDecoder;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSmartCopy;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.SimpleBookmark;

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
	private static final String MAXIMIZE_WIDTH = "Maximize to width";
	private static final String MAXIMIZE_HEIGHT = "Maximize to height";

	private PDFPageCluster[] clustersMapping;
	private HashMap<PDFPageCluster, List<Integer>> clusterToPageSet;

	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JButton loadBtn, cropBtn, maxWBtn, maxHBtn;
	private ClusterPagesTask clusterTask;
	private CropPDFTask cropTask;
	private File lastOpenDir;
	private File origFile = null;
	private File croppedFile = null;
	private List<MergedPanel> mergedPanels = null;

	public Briss() {
		super("BRISS - BRigt Snippet Sire");
		init();
	}

	private File loadPDF(String recommendation, boolean saveDialog) {

		JFileChooser fc = lastOpenDir == null ? new JFileChooser()
				: new JFileChooser(lastOpenDir);

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
			if (fc.getSelectedFile() != null)
				lastOpenDir = fc.getSelectedFile().getParentFile();
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
		cropBtn.setPreferredSize(new Dimension(300, 30));
		cropBtn.addActionListener(this);
		cropBtn.setEnabled(false);

		maxWBtn = new JButton(MAXIMIZE_WIDTH);
		maxWBtn.setEnabled(false);
		maxWBtn.setPreferredSize(new Dimension(300, 30));
		maxWBtn.addActionListener(this);

		maxHBtn = new JButton(MAXIMIZE_HEIGHT);
		maxHBtn.setEnabled(false);
		maxHBtn.setPreferredSize(new Dimension(300, 30));
		maxHBtn.addActionListener(this);

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
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		add(maxWBtn, c);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.5;
		add(maxHBtn, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
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
		if (aE.getActionCommand().equals(MAXIMIZE_HEIGHT)) {
			// maximize to height
			// search for maximum height
			int maxHeight = -1;
			for (MergedPanel mp : mergedPanels) {
				int mpMax = mp.getHeighestRect();
				if (maxHeight < mpMax) {
					maxHeight = mpMax;
				}
			}
			// set maximum height to all rectangles
			if (maxHeight == -1)
				return;
			for (MergedPanel mp : mergedPanels) {
				mp.setSelCropHeight(maxHeight);
			}

		} else if (aE.getActionCommand().equals(MAXIMIZE_WIDTH)) {
			// maximize to width
			// search for maximum width
			int maxWidth = -1;
			for (MergedPanel mp : mergedPanels) {
				int mpMax = mp.getWidestRect();
				if (maxWidth < mpMax) {
					maxWidth = mpMax;
				}
			}
			// set maximum widt to all rectangles
			if (maxWidth == -1)
				return;
			for (MergedPanel mp : mergedPanels) {
				mp.setSelCropWidth(maxWidth);
			}

		} else if (aE.getActionCommand().equals(LOAD)) {
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
			if (croppedFile == null)
				return;
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

		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground() {

			PdfReader reader;
			try {

				// first make a copy containing the right amount of pages
				reader = new PdfReader(origFile.getAbsolutePath());
				Document document = new Document();
				HashMap metaInfo = reader.getInfo();

				File tmpFile = File.createTempFile("cropped", ".pdf");
				PdfSmartCopy pdfCopy = new PdfSmartCopy(document,
						new FileOutputStream(tmpFile));
				document.open();
				int origPageCount = reader.getNumberOfPages();
				PdfImportedPage page;
				List bookmarks = null;

				for (int pageNumber = 1; pageNumber <= origPageCount; pageNumber++) {
					PDFPageCluster clusterInfo = clustersMapping[pageNumber - 1];
					page = pdfCopy.getImportedPage(reader, pageNumber);
					bookmarks = SimpleBookmark.getBookmark(reader);
					// SimpleBookmark.s
					pdfCopy.addPage(page);
					for (int j = 1; j < clusterInfo.getRatiosList().size(); j++) {
						pdfCopy.addPage(page);
					}
				}
				document.close();
				pdfCopy.close();

				// no crop all pages according to their ratios

				reader = new PdfReader(tmpFile.getAbsolutePath());

				PdfStamper stamper = new PdfStamper(reader,
						new FileOutputStream(croppedFile));
				stamper.setMoreInfo(metaInfo);

				PdfDictionary pageDict;
				int newPageNumber = 1;
				for (int origPageNumber = 1; origPageNumber <= origPageCount; origPageNumber++) {
					PDFPageCluster clusterInfo = clustersMapping[origPageNumber - 1];

					// if no cop was selected do nothing
					if (clusterInfo.getRatiosList().size() == 0) {
						newPageNumber++;
						continue;
					}

					for (Float[] ratios : clusterInfo.getRatiosList()) {

						pageDict = reader.getPageN(newPageNumber);

						List<Rectangle> boxes = new ArrayList<Rectangle>();
						boxes.add(reader.getBoxSize(newPageNumber, "media"));
						boxes.add(reader.getBoxSize(newPageNumber, "crop"));
						boxes.add(reader.getBoxSize(newPageNumber, "trim"));
						boxes.add(reader.getBoxSize(newPageNumber, "bleed"));
						int rotation = reader.getPageRotation(newPageNumber);

						Rectangle scaledBox = calculateScaledRectangle(boxes,
								ratios, rotation);

						PdfArray scaleBoxArray = new PdfArray();
						scaleBoxArray.add(new PdfNumber(scaledBox.getLeft()));
						scaleBoxArray.add(new PdfNumber(scaledBox.getBottom()));
						scaleBoxArray.add(new PdfNumber(scaledBox.getRight()));
						scaleBoxArray.add(new PdfNumber(scaledBox.getTop()));

						pageDict.put(PdfName.CROPBOX, scaleBoxArray);
						pageDict.put(PdfName.MEDIABOX, scaleBoxArray);
						pageDict.put(PdfName.TRIMBOX, scaleBoxArray);
						pageDict.put(PdfName.BLEEDBOX, scaleBoxArray);
						// increment the pagenumber
						newPageNumber++;
					}
					int[] range = new int[2];
					range[0] = newPageNumber - 1;
					range[1] = origPageCount + (newPageNumber - origPageNumber);
					SimpleBookmark.shiftPageNumbers(bookmarks, clusterInfo
							.getRatiosList().size() - 1, range);

					int percent = (int) ((origPageNumber / (float) origPageCount) * 100);
					setProgress(percent);
				}
				stamper.setOutlines(bookmarks);
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
			Float[] ratios, int rotation) {
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
	private float[] rotateRatios(Float[] ratios, int rotation) {
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
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = yposition++;
			c.anchor = GridBagConstraints.CENTER;
			c.insets = new Insets(2, 0, 2, 0);
			JTextArea text = new JTextArea(
					" Draw multiple crop rectangles for merged pages by clicking+holding the left mouse button down."
							+ " The number reflects the ordering of the new pages. "
							+ "Clear the rectangles for a page-cluster by pressing the right mouse button.");
			text.setLineWrap(true);
			text.setForeground(Color.YELLOW);
			text.setBackground(Color.BLACK);
			previewPanel.add(text, c);

			mergedPanels = new ArrayList<MergedPanel>();
			for (PDFPageCluster cluster : clusterToPageSet.keySet()) {
				MergedPanel p = new MergedPanel(cluster);
				c = new GridBagConstraints();
				c.gridx = 0;
				c.gridy = yposition++;
				c.anchor = GridBagConstraints.CENTER;
				c.insets = new Insets(4, 0, 4, 0);
				previewPanel.add(p, c);
				mergedPanels.add(p);
			}
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			cropBtn.setEnabled(true);
			maxWBtn.setEnabled(true);
			maxHBtn.setEnabled(true);
			setProgress(0);
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
				setProgress(0);

				int pageCounter = 0;
				for (Integer pageNumber : cluster.getPagesToMerge()) {
					BufferedImage renderedPage = decode_pdf
							.getPageAsImage(pageNumber);
					cluster.addImageToPreview(renderedPage);

					int percent = (int) ((++pageCounter / (float) cluster
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
