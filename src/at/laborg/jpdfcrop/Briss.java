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
import java.util.concurrent.ExecutionException;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
	private MergedPanel evenPanel, oddPanel;
	private JProgressBar progressBar;
	private JMenu menu;
	private LoadImageFromPDFTask task;

	private final String MIRROR_MODE_TEXT = "Mirror Mode";
	private final String LOAD_FILE_TEXT = "Open Pdf for cropping";

	public Briss() {
		super("JpdfCrop");
		init();

	}

	private File getPDF() {
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

		evenPanel = new MergedPanel();
		evenPanel.setEnabled(false);
		oddPanel = new MergedPanel();
		oddPanel.setEnabled(false);
		mirrorMode = new JCheckBox(MIRROR_MODE_TEXT);
		mirrorMode.setEnabled(false);
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setEnabled(true);

		mirrorMode.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		add(evenPanel, c);
		c.gridx = 1;
		c.gridy = 0;
		add(oddPanel, c);
		c.gridx = 0;
		c.gridy = 1;

		add(mirrorMode, c);
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
				evenPanel.setConnectedMerge(oddPanel);
				oddPanel.setConnectedMerge(evenPanel);
			} else {
				// disconnect them
				evenPanel.setConnectedMerge(null);
				oddPanel.setConnectedMerge(null);
			}
		} else if (aE.getActionCommand().equals(LOAD_FILE_TEXT)) {
			mirrorMode.setEnabled(false);
			evenPanel.setEnabled(false);
			oddPanel.setEnabled(false);
			menu.setEnabled(false);
			progressBar.setString("loading PDF");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			File origPdf = getPDF();
			task = new LoadImageFromPDFTask(origPdf);
			task.addPropertyChangeListener(this);
			task.execute();
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

	private class LoadImageFromPDFTask extends
			SwingWorker<BufferedImage[], Void> {

		private File pdfFile;

		public LoadImageFromPDFTask(File pdfFile) {
			super();
			this.pdfFile = pdfFile;
		}

		@Override
		protected void done() {

			BufferedImage[] evenAndOdd;
			try {
				evenAndOdd = task.get();
				evenPanel.setImage(evenAndOdd[0]);
				oddPanel.setImage(evenAndOdd[1]);
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			progressBar.setValue(0);
			mirrorMode.setEnabled(true);
			evenPanel.setEnabled(true);
			oddPanel.setEnabled(true);
			menu.setEnabled(true);
			pack();
		}

		@Override
		protected BufferedImage[] doInBackground() throws Exception {
			BufferedImage[] evenAndOdd = null;
			try {
				evenAndOdd = new BufferedImage[2];
				PDF pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF",
						null, null, null);
				pdf.setInput(pdfFile);
				Document doc = new Document("doc", null, null);
				pdf.parse(doc);
				doc.clear();
				int iPageCount = pdf.getReader().getPageCnt();
				iPageCount = 10;
				WritableRaster rasterEven = null;
				WritableRaster rasterOdd = null;
				double[][] odd = null;
				double[][] even = null;

				for (int i = 2; i < iPageCount; i++) {
					pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF",
							null, null, null);
					pdf.setInput(pdfFile);
					doc = new Document("doc", null, null);
					pdf.parse(doc);
					doc.clear();
					doc.putAttr(Document.ATTR_PAGE, Integer.toString(i));
					pdf.parse(doc);

					Node top = doc.childAt(0);

					doc.formatBeforeAfter(600, 600, null);
					int w = top.bbox.width;
					int h = top.bbox.height;
					BufferedImage img = new BufferedImage(w, h,
							BufferedImage.TYPE_INT_RGB);
					Graphics2D g = img.createGraphics();
					g.setClip(0, 0, w, h);

					g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
					g.setRenderingHint(RenderingHints.KEY_RENDERING,
							RenderingHints.VALUE_RENDER_QUALITY);

					Context cx = doc.getStyleSheet().getContext(g, null);
					top.paintBeforeAfter(g.getClipBounds(), cx);
					if (evenAndOdd[0] == null) {
						evenAndOdd[0] = new BufferedImage(img.getWidth(), img
								.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
						evenAndOdd[1] = new BufferedImage(img.getWidth(), img
								.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
						even = new double[img.getWidth()][img.getHeight()];
						odd = new double[img.getWidth()][img.getHeight()];
						rasterEven = evenAndOdd[0].getRaster()
								.createCompatibleWritableRaster();
						rasterOdd = evenAndOdd[1].getRaster()
								.createCompatibleWritableRaster();
					}

					if (i % 2 == 0) {
						average(img, even);
					} else {
						average(img, odd);
					}
					doc.removeAllChildren();
					cx.reset();
					g.dispose();
					pdf.getReader().close();
					doc = null;
					setProgress(0);
					int percent = (int) ((i / (float) iPageCount) * 100);
					setProgress(percent);
				}
				// average.setData(raster);
				for (int k = 0; k < evenAndOdd[0].getHeight(); ++k) {
					for (int j = 0; j < evenAndOdd[0].getWidth(); ++j) {
						rasterEven.setSample(j, k, 0, Math.round(even[j][k]
								/ (iPageCount / 2)));
						rasterOdd.setSample(j, k, 0, Math.round(odd[j][k]
								/ (iPageCount / 2)));
					}
				}
				evenAndOdd[0].setData(rasterEven);
				evenAndOdd[1].setData(rasterOdd);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return evenAndOdd;
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
