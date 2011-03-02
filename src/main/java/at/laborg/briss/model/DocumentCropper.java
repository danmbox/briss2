// $Id: SingleCluster.java 55 2011-02-22 21:45:59Z laborg $
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
package at.laborg.briss.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.laborg.briss.utils.BrissFileHandling;
import at.laborg.briss.utils.CropDocument;
import at.laborg.briss.utils.RectangleHandler;

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

public class DocumentCropper {

	public static File crop(CropDocument cropDoc, File destination)
			throws IOException, DocumentException {

		if (!BrissFileHandling.checkValidStateAndCreate(destination))
			throw new IOException("Destination file not valid");

		// first make a copy containing the right amount of pages
		File multipliedTmpFile = copyToMultiplePages(cropDoc);

		// now crop all pages according to their ratios
		cropMultipliedFile(cropDoc, multipliedTmpFile, destination);
		return destination;
	}

	private static File copyToMultiplePages(CropDocument cropDoc)
			throws IOException, DocumentException {

		PdfReader reader = new PdfReader(cropDoc.getSourceFile()
				.getAbsolutePath());
		Document document = new Document();

		File resultFile = File.createTempFile("cropped", ".pdf");
		PdfSmartCopy pdfCopy = new PdfSmartCopy(document, new FileOutputStream(
				resultFile));
		document.open();
		PdfImportedPage page;

		for (int pageNumber = 1; pageNumber <= cropDoc.getSourcePageCount(); pageNumber++) {
			SingleCluster cluster = cropDoc.getClusterCollection()
					.getSingleCluster(pageNumber);
			page = pdfCopy.getImportedPage(reader, pageNumber);
			pdfCopy.addPage(page);
			for (int j = 1; j < cluster.getRatiosList().size(); j++) {
				pdfCopy.addPage(page);
			}
		}
		document.close();
		pdfCopy.close();
		reader.close();
		return resultFile;
	}

	private static File cropMultipliedFile(CropDocument cropDoc,
			File multipliedDocument, File cropDestination)
			throws FileNotFoundException, DocumentException, IOException {

		PdfReader reader = new PdfReader(multipliedDocument.getAbsolutePath());
		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(
				cropDestination));
		stamper.setMoreInfo(cropDoc.getSourceMetaInfo());

		PdfDictionary pageDict;
		int newPageNumber = 1;
		for (int origPageNumber = 1; origPageNumber <= cropDoc
				.getSourcePageCount(); origPageNumber++) {
			SingleCluster cluster = cropDoc.getClusterCollection()
					.getSingleCluster(origPageNumber);

			// if no crop was selected do nothing
			if (cluster.getRatiosList().size() == 0) {
				newPageNumber++;
				continue;
			}

			for (Float[] ratios : cluster.getRatiosList()) {

				pageDict = reader.getPageN(newPageNumber);

				List<Rectangle> boxes = new ArrayList<Rectangle>();
				boxes.add(reader.getBoxSize(newPageNumber, "media"));
				boxes.add(reader.getBoxSize(newPageNumber, "crop"));
				int rotation = reader.getPageRotation(newPageNumber);

				Rectangle scaledBox = RectangleHandler
						.calculateScaledRectangle(boxes, ratios, rotation);

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
			range[1] = cropDoc.getSourcePageCount()
					+ (newPageNumber - origPageNumber);
			SimpleBookmark.shiftPageNumbers(cropDoc.getSourceBookmarks(),
					cluster.getRatiosList().size() - 1, range);
		}
		stamper.setOutlines(cropDoc.getSourceBookmarks());
		stamper.close();
		reader.close();
		return cropDestination;
	}
}
