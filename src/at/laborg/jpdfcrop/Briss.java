package at.laborg.jpdfcrop;

import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
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

	private JCheckBox mirrorMode;
	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JButton cropBtn;
	private JMenu menu;
	private ClusterPagesTask clusterTask;
	
	private final static int MAX_DOC_X = 30;
	private final static int MAX_DOC_Y = 30;

	private final String MIRROR_MODE_TEXT = "Mirror Mode";
	private final String LOAD_FILE_TEXT = "Open Pdf for cropping";

	public Briss() {
		super("JpdfCrop");
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

		JMenuBar menuBar = new JMenuBar();
		menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		JMenuItem loadFileItem = new JMenuItem(LOAD_FILE_TEXT);
		loadFileItem.addActionListener(this);
		menu.add(loadFileItem);

		menuBar.add(menu);
		this.setJMenuBar(menuBar);

		previewPanel = new JPanel();
		previewPanel.setLayout(new GridBagLayout());
		previewPanel.setEnabled(true);

		mirrorMode = new JCheckBox(MIRROR_MODE_TEXT);
		mirrorMode.setEnabled(false);

		cropBtn = new JButton("Crop");
		cropBtn.setEnabled(false);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setEnabled(true);

		mirrorMode.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		add(previewPanel, c);
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(mirrorMode, c);
		c.gridx = 1;
		c.gridy = 1;
		add(cropBtn, c);
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
		if (aE.getActionCommand().equals(MIRROR_MODE_TEXT)) {
			if (mirrorMode.isSelected()) {
				// connect both panels for mirror mode
				// evenPanel.setConnectedMerge(oddPanel);
				// oddPanel.setConnectedMerge(evenPanel);
			} else {
				// disconnect them
				// evenPanel.setConnectedMerge(null);
				// oddPanel.setConnectedMerge(null);
			}
		} else if (aE.getActionCommand().equals(LOAD_FILE_TEXT)) {
			mirrorMode.setEnabled(false);
			// evenPanel.setEnabled(false);
			// oddPanel.setEnabled(false);
			menu.setEnabled(false);
			progressBar.setString("loading PDF");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			File origPdf = loadPDF();
			clusterTask = new ClusterPagesTask(origPdf);
			clusterTask.addPropertyChangeListener(this);
			clusterTask.execute();
		}
	}

	private class CropPDFTask extends SwingWorker<Void, Void> {
		private File pdfFile;
		float[] ratiosEven;
		float[] ratiosOdd;

		public CropPDFTask(File pdfFile, float[] ratiosEven, float[] ratiosOdd) {
			super();
			this.pdfFile = pdfFile;
			this.ratiosEven = ratiosEven;
			this.ratiosOdd = ratiosOdd;
		}

		@Override
		protected void done() {
			cropBtn.setEnabled(true);
		}

		@Override
		protected Void doInBackground() throws Exception {

			PdfReader reader;
			try {
				reader = new PdfReader(pdfFile.getAbsolutePath());

				PdfStamper stamper = new PdfStamper(reader,
						new FileOutputStream("out.pdf"));

				int n = reader.getNumberOfPages();
				PdfDictionary pageDict;
				PdfArray old_mediabox, new_mediabox, old_cropbox, new_cropbox;
				PdfNumber value;
				BaseFont font = BaseFont.createFont(BaseFont.HELVETICA,
						BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
				PdfContentByte directcontent;
				for (int i = 1; i <= n; i++) {
					pageDict = reader.getPageN(i);
					new_mediabox = new PdfArray();
					old_mediabox = pageDict.getAsArray(PdfName.MEDIABOX);
					value = (PdfNumber) old_mediabox.getAsNumber(0);
					new_mediabox.add(new PdfNumber(value.floatValue() - 300));
					value = (PdfNumber) old_mediabox.getAsNumber(1);
					new_mediabox.add(new PdfNumber(value.floatValue() - 300));
					value = (PdfNumber) old_mediabox.getAsNumber(2);
					new_mediabox.add(new PdfNumber(value.floatValue() + 300));
					value = (PdfNumber) old_mediabox.getAsNumber(3);
					new_mediabox.add(new PdfNumber(value.floatValue() + 300));
					pageDict.put(PdfName.MEDIABOX, new_mediabox);

					new_cropbox = new PdfArray();
					old_cropbox = pageDict.getAsArray(PdfName.CROPBOX);
					value = (PdfNumber) old_cropbox.getAsNumber(0);
					new_cropbox.add(new PdfNumber(value.floatValue() + 50));
					value = (PdfNumber) old_cropbox.getAsNumber(1);
					new_cropbox.add(new PdfNumber(value.floatValue() + 50));
					value = (PdfNumber) old_cropbox.getAsNumber(2);
					new_cropbox.add(new PdfNumber(value.floatValue() - 50));
					value = (PdfNumber) old_cropbox.getAsNumber(3);
					new_cropbox.add(new PdfNumber(value.floatValue() - 50));
					pageDict.put(PdfName.CROPBOX, new_cropbox);

					//
					// If Not
					// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX)
					// Is
					// Nothing
					// Then
					// newCropBox = New iTextSharp.text.pdf.PdfArray
					// oldCropBox =
					// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX).ArrayList
					// value = CType(oldCropBox(0),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
					// value = CType(oldCropBox(1),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
					// value = CType(oldCropBox(2),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
					// value = CType(oldCropBox(3),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
					//
					// pageDict.Put(iTextSharp.text.pdf.PdfName.CROPBOX,
					// newCropBox)
					// End If
					//
					// If Not
					// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX)
					// Is Nothing
					// Then
					// newCropBox = New iTextSharp.text.pdf.PdfArray
					// oldCropBox =
					// pageDict.GetAsArray(iTextSharp.text.pdf.PdfName.CROPBOX).ArrayList
					// value = CType(oldCropBox(0),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
					// value = CType(oldCropBox(1),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() - 36))
					// value = CType(oldCropBox(2),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
					// value = CType(oldCropBox(3),
					// iTextSharp.text.pdf.PdfNumber)
					// newCropBox.Add(New PdfNumber(value.FloatValue() + 36))
					//
					// pageDict.Put(iTextSharp.text.pdf.PdfName.CROPBOX,
					// newCropBox)
					// End If

					directcontent = stamper.getOverContent(i);
					directcontent.beginText();
					directcontent.setFontAndSize(font, 12);
					directcontent.showTextAligned(Element.ALIGN_LEFT, "TEST",
							0, -18, 0);
					directcontent.endText();
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
		private PDFPageClusterInfo[] clustersMapping;
		private HashMap<PDFPageClusterInfo, List<Integer>> clusterToPageSet;
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
				MergedPanel p = new MergedPanel();
				p.setImage(cluster.getPreviewImage());
				GridBagConstraints c = new GridBagConstraints();
				c.gridx = yposition++;
				c.gridy = 0;
				add(p, c);
			}
			previewPanel.setVisible(true);
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			progressBar.setValue(0);
			mirrorMode.setEnabled(true);
			menu.setEnabled(true);
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
					clustersMapping[i-1] = tmpCluster;

					pdf.getReader().close();
					doc = null;
					setProgress(0);
					int percent = (int) ((i / (float) pageCount) * 100);
					setProgress(percent);
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
								/ (cluster.getPagesToMerge().size() / 2)));
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
