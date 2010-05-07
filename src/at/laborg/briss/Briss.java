package at.laborg.briss;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.Node;
import multivalent.ParseException;
import multivalent.std.adaptor.pdf.PDF;

/**
 * even = 0, odd =1
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
	private JButton actionBtn;
	private ClusterPagesTask clusterTask;
	private CropPDFTask cropTask;
	private File origFile = null;

	private final static int MAX_DOC_X = 30;
	private final static int MAX_DOC_Y = 30;

	public Briss() {
		super("BRISS - BRigt Snippet Sire");
		init();
	}

	private File loadPDF() {
		JFileChooser fc = new JFileChooser();
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
		int retval = fc.showOpenDialog(this);
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

		actionBtn = new JButton(LOAD);
		actionBtn.setEnabled(true);
		actionBtn.setPreferredSize(new Dimension(300, 30));
		actionBtn.addActionListener(this);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(300, 30));
		progressBar.setEnabled(true);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.anchor = GridBagConstraints.CENTER;
		add(new JScrollPane(previewPanel), c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		add(actionBtn, c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
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
			origFile = loadPDF();
			if (origFile != null) {
				actionBtn.setEnabled(false);
				progressBar.setString("loading PDF");
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				clusterTask = new ClusterPagesTask(origFile);
				clusterTask.addPropertyChangeListener(this);
				clusterTask.execute();
			}
		} else if (aE.getActionCommand().equals(CROP)) {
			actionBtn.setEnabled(false);
			progressBar.setString("loading PDF");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			cropTask = new CropPDFTask(origFile);
			cropTask.addPropertyChangeListener(this);
			cropTask.execute();
		}
	}

	private class CropPDFTask extends SwingWorker<Void, Void> {
		private File pdfFile;

		public CropPDFTask(File pdfFile) {
			super();
			this.pdfFile = pdfFile;
		}

		@Override
		protected void done() {
			actionBtn.setEnabled(true);
			actionBtn.setText(LOAD);
			progressBar.setValue(0);
			progressBar.setString("");
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}

		@Override
		protected Void doInBackground() throws Exception {

			PdfReader reader;
			try {
				reader = new PdfReader(pdfFile.getAbsolutePath());

				PdfStamper stamper = new PdfStamper(reader,
						new FileOutputStream("out.pdf"));

				int pageCount = reader.getNumberOfPages();
				PdfDictionary pageDict;
				PdfArray old_mediabox, new_mediabox, old_cropbox, new_cropbox;
				PdfNumber value;
				PdfNumber x1, y1, x2, y2;
				for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
					PDFPageClusterInfo clusterInfo = clustersMapping[pageNumber - 1];
					// /(x1, y1, x2, y2) left bottom, right top

					pageDict = reader.getPageN(pageNumber);
					new_mediabox = new PdfArray();
					old_mediabox = pageDict.getAsArray(PdfName.MEDIABOX);
					x1 = (PdfNumber) old_mediabox.getAsNumber(0);
					y1 = (PdfNumber) old_mediabox.getAsNumber(1);
					x2 = (PdfNumber) old_mediabox.getAsNumber(2);
					y2 = (PdfNumber) old_mediabox.getAsNumber(3);
					int mediaWidth = x2.intValue() - x1.intValue();
					int mediaHeight = y2.intValue() - y1.intValue();

					if (clusterInfo.getRatios() != null) {
						int x1n = (int) (x1.intValue() + (mediaWidth * clusterInfo
								.getRatios()[0]));
						int y1n = (int) (y1.intValue() + (mediaHeight * clusterInfo
								.getRatios()[2]));
						int x2n = (int) (x1.intValue() + (mediaWidth * clusterInfo
								.getRatios()[1]));
						int y2n = (int) (y1.intValue() + (mediaHeight * clusterInfo
								.getRatios()[3]));
						new_mediabox.add(new PdfNumber(x1n));
						new_mediabox.add(new PdfNumber(y1n));
						new_mediabox.add(new PdfNumber(x2n));
						new_mediabox.add(new PdfNumber(y2n));
						pageDict.put(PdfName.MEDIABOX, new_mediabox);

					}

					new_cropbox = new PdfArray();
					old_cropbox = pageDict.getAsArray(PdfName.CROPBOX);
					if (old_cropbox != null) {
						value = (PdfNumber) old_cropbox.getAsNumber(0);
						new_cropbox.add(new PdfNumber(value.floatValue() + 50));
						value = (PdfNumber) old_cropbox.getAsNumber(1);
						new_cropbox.add(new PdfNumber(value.floatValue() + 50));
						value = (PdfNumber) old_cropbox.getAsNumber(2);
						new_cropbox.add(new PdfNumber(value.floatValue() - 50));
						value = (PdfNumber) old_cropbox.getAsNumber(3);
						new_cropbox.add(new PdfNumber(value.floatValue() - 50));
						pageDict.put(PdfName.CROPBOX, new_cropbox);
					}

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

	private class ClusterPagesTask extends
			SwingWorker<PDFPageClusterInfo[], Void> {

		private File pdfFile;

		private int pageCount;

		public ClusterPagesTask(File pdfFile) {
			super();
			this.pdfFile = pdfFile;
			PDF pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF", null,
					null, null);
			progressBar.setString("Analysing PDF pages");
			try {
				pdf.setInput(pdfFile);
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
				previewPanel.add(p, c);
			}
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			actionBtn.setEnabled(true);
			progressBar.setValue(0);
			actionBtn.setText(CROP);
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
					pdf.setInput(pdfFile);
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
					pdf.setInput(pdfFile);
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
