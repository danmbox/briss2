import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import multivalent.Behavior;
import multivalent.Context;
import multivalent.Document;
import multivalent.Node;
import multivalent.std.adaptor.pdf.PDF;

public class PDFToImageConverter {

	private File inputFile;
	private String inputFileName;

	public PDFToImageConverter(String input, String output) {
		inputFile = new File(input);
		this.convert(output);
	}

	public PDFToImageConverter() {

	}

	public static void main(String args[]) {

		PDFToImageConverter converter = new PDFToImageConverter();
		// pdf file name
		converter.setInputFileName("test4.pdf");

		converter.convert("converted");

	}

	public void convert(String strOutputFileName) {

		inputFile = new File(inputFileName);
		// File outfile = new File(outputFileName);
		StringBuffer sbFileName = new StringBuffer();

		try {

			PDF pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF", null,
					null, null);

			pdf.setInput(inputFile);

			Document doc = new Document("doc", null, null);
			pdf.parse(doc);
			doc.clear();
			int iPageCount = pdf.getReader().getPageCnt();
			iPageCount = 120;
			// BufferedImage[] images = new BufferedImage[iPageCount-1];
			BufferedImage evenImage = null;
			BufferedImage oddImage = null;
			WritableRaster rasterEven = null;
			WritableRaster rasterOdd = null;
			double[][] odd = null;
			double[][] even = null;
			Graphics2D gA = null;
			for (int i = 2; i < iPageCount; i++) {
				pdf = (PDF) Behavior.getInstance("AdobePDF", "AdobePDF", null,
						null, null);

				pdf.setInput(inputFile);

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
				if (evenImage == null) {
					evenImage = new BufferedImage(img.getWidth(), img
							.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
					oddImage = new BufferedImage(img.getWidth(), img
							.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
					even = new double[img.getWidth()][img.getHeight()];
					odd = new double[img.getWidth()][img.getHeight()];
					rasterEven = evenImage.getRaster()
							.createCompatibleWritableRaster();
					rasterOdd = oddImage.getRaster()
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
			}
			// average.setData(raster);
			for (int k = 0; k < evenImage.getHeight(); ++k) {
				for (int j = 0; j < evenImage.getWidth(); ++j) {
					rasterEven.setSample(j, k, 0, Math.round(even[j][k]
							/ (iPageCount / 2)));
					rasterOdd.setSample(j, k, 0, Math.round(odd[j][k]
							/ (iPageCount / 2)));
				}
			}
			evenImage.setData(rasterEven);
			ImageIO.write(evenImage, "jpg", new File("even.jpg"));
			oddImage.setData(rasterOdd);
			ImageIO.write(oddImage, "jpg", new File("odd.jpg"));
			// ImageIO.write(img, "jpg", outfile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static double meanValue(BufferedImage image, int width, int height) {
		Raster raster = image.getRaster();
		double sum = 0.0;

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				sum += raster.getSample(x, y, 0);
			}
		}
		return sum / (width * height);
	}
	static void average(BufferedImage image, double[][] values) {
		for (int k = 0; k < image.getHeight(); ++k) {
			for (int j = 0; j < image.getWidth(); ++j) {
				values[j][k] += image.getRaster().getSample(j, k, 0);
			}
		}
	}
	public String getInputFileName() {
		return inputFileName;
	}

	

	public void setInputFileName(String inputFileName) {
		this.inputFileName = inputFileName;
	}

}
