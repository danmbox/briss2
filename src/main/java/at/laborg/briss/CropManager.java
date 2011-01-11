package at.laborg.briss;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.SimpleBookmark;

public class CropManager {

	public static void crop(File origFile, File croppedFile, ClusterManager cM) {
		PdfReader reader;
		try {

			// first make a copy containing the right amount of pages
			reader = new PdfReader(origFile.getAbsolutePath());
			Document document = new Document();
			HashMap<String, String> metaInfo = reader.getInfo();

			File tmpFile = File.createTempFile("cropped", ".pdf");
			PdfSmartCopy pdfCopy = new PdfSmartCopy(document,
					new FileOutputStream(tmpFile));
			document.open();
			int origPageCount = reader.getNumberOfPages();
			PdfImportedPage page;
			List<HashMap<String, Object>> bookmarks = null;

			for (int pageNumber = 1; pageNumber <= origPageCount; pageNumber++) {
				PDFPageCluster currentCluster = cM.getCluster(pageNumber);
				page = pdfCopy.getImportedPage(reader, pageNumber);
				bookmarks = SimpleBookmark.getBookmark(reader);
				pdfCopy.addPage(page);
				for (int j = 1; j < currentCluster.getRatiosList().size(); j++) {
					pdfCopy.addPage(page);
				}
			}
			document.close();
			pdfCopy.close();

			// now crop all pages according to their ratios

			reader = new PdfReader(tmpFile.getAbsolutePath());

			PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(
					croppedFile));
			stamper.setMoreInfo(metaInfo);

			PdfDictionary pageDict;
			int newPageNumber = 1;
			for (int origPageNumber = 1; origPageNumber <= origPageCount; origPageNumber++) {
				PDFPageCluster currentCluster = cM.getCluster(origPageNumber);

				// if no crop was selected do nothing
				if (currentCluster.getRatiosList().size() == 0) {
					newPageNumber++;
					continue;
				}

				for (Float[] ratios : currentCluster.getRatiosList()) {

					pageDict = reader.getPageN(newPageNumber);

					List<Rectangle> boxes = new ArrayList<Rectangle>();
					boxes.add(reader.getBoxSize(newPageNumber, "media"));
					boxes.add(reader.getBoxSize(newPageNumber, "crop"));
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
					// increment the pagenumber
					newPageNumber++;
				}
				int[] range = new int[2];
				range[0] = newPageNumber - 1;
				range[1] = origPageCount + (newPageNumber - origPageNumber);
				SimpleBookmark.shiftPageNumbers(bookmarks, currentCluster
						.getRatiosList().size() - 1, range);
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

	}

	private static Rectangle calculateScaledRectangle(List<Rectangle> boxes,
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
	private static float[] rotateRatios(Float[] ratios, int rotation) {
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
}
