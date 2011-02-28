package at.laborg.briss.model;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

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

public class CropDocument {

	private static final String RECOMMENDED_ENDING = "_cropped.pdf";

	private final File sourceFile;
	private final int sourcePageCount;
	private final HashMap<String, String> sourceMetaInfo;
	private final List<HashMap<String, Object>> sourceBookmarks;
	private final ClusterCollection clusterCollection;

	private PageExcludes pageExcludes;

	private CropDocument(File source, int pageCount,
			HashMap<String, String> metaInfo,
			List<HashMap<String, Object>> bookmarks,
			ClusterCollection clusterCollection) {
		this.sourceFile = source;
		this.sourcePageCount = pageCount;
		this.sourceMetaInfo = metaInfo;
		this.sourceBookmarks = bookmarks;
		this.clusterCollection = clusterCollection;
	}

	public static CropDocument createCropDoc(File source) throws IOException {
		if (source == null)
			throw new IllegalArgumentException("Source must be provided");
		if (!source.exists())
			throw new IllegalArgumentException("Source("
					+ source.getAbsolutePath() + ") file doesn't exist");

		PdfReader reader = new PdfReader(source.getAbsolutePath());
		CropDocument result = new CropDocument(source, reader
				.getNumberOfPages(), reader.getInfo(), SimpleBookmark
				.getBookmark(reader), new ClusterCollection());
		reader.close();
		return result;
	}

	private boolean isDestinationFileValid(File destinationFile)
			throws IOException, IllegalArgumentException {
		if (destinationFile == null)
			throw new IllegalArgumentException("Destination File musst be set!");
		if (!destinationFile.exists()) {
			return destinationFile.createNewFile();
		}
		return true;
	}

	public File getRecommendedDestination() {
		// create file recommendation
		String origName = sourceFile.getAbsolutePath();
		String recommendedName = origName.substring(0, origName.length() - 4)
				+ RECOMMENDED_ENDING;
		return new File(recommendedName);
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public ClusterCollection getClusterCollection() {
		return clusterCollection;
	}

	public void setPageExcludes(PageExcludes pageExcludes) {
		this.pageExcludes = pageExcludes;
	}

	public File crop(File destination) throws IOException, DocumentException {

		if (!isDestinationFileValid(destination)) {
			throw new IOException("Destination file not valid");
		}

		// first make a copy containing the right amount of pages
		File multipliedTmpFile = copyToMultiplePages();

		// now crop all pages according to their ratios
		cropMultipliedFile(multipliedTmpFile, destination);
		return destination;
	}

	private File copyToMultiplePages() throws IOException, DocumentException {

		PdfReader reader = new PdfReader(sourceFile.getAbsolutePath());
		Document document = new Document();

		File resultFile = File.createTempFile("cropped", ".pdf");
		PdfSmartCopy pdfCopy = new PdfSmartCopy(document, new FileOutputStream(
				resultFile));
		document.open();
		PdfImportedPage page;

		for (int pageNumber = 1; pageNumber <= sourcePageCount; pageNumber++) {
			SingleCluster cluster = clusterCollection
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

	private File cropMultipliedFile(File multipliedDocument,
			File cropDestination) throws FileNotFoundException,
			DocumentException, IOException {

		PdfReader reader = new PdfReader(multipliedDocument.getAbsolutePath());
		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(
				cropDestination));
		stamper.setMoreInfo(sourceMetaInfo);

		PdfDictionary pageDict;
		int newPageNumber = 1;
		for (int origPageNumber = 1; origPageNumber <= sourcePageCount; origPageNumber++) {
			SingleCluster cluster = clusterCollection
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

				Rectangle scaledBox = calculateScaledRectangle(boxes, ratios,
						rotation);

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
			range[1] = sourcePageCount + (newPageNumber - origPageNumber);
			SimpleBookmark.shiftPageNumbers(sourceBookmarks, cluster
					.getRatiosList().size() - 1, range);
		}
		stamper.setOutlines(sourceBookmarks);
		stamper.close();
		reader.close();
		return cropDestination;
	}

	private static Rectangle calculateScaledRectangle(List<Rectangle> boxes,
			Float[] ratios, int rotation) {
		if (ratios == null || boxes.size() == 0)
			return null;
		Rectangle smallestBox = null;
		// find smallest box
		float smallestSquare = Float.MAX_VALUE;
		for (Rectangle box : boxes) {
			if (box != null) {
				if (smallestBox == null) {
					smallestBox = box;
				}
				if (smallestSquare > box.getWidth() * box.getHeight()) {
					// set new smallest box
					smallestSquare = box.getWidth() * box.getHeight();
					smallestBox = box;
				}
			}
		}
		if (smallestBox == null)
			return null; // no useable box was found

		// rotate the ratios according to the rotation of the page
		float[] rotRatios = rotateRatios(ratios, rotation);

		// use smallest box as basis for calculation
		Rectangle scaledBox = new Rectangle(smallestBox);

		scaledBox.setLeft(smallestBox.getLeft()
				+ (smallestBox.getWidth() * rotRatios[0]));
		scaledBox.setBottom(smallestBox.getBottom()
				+ (smallestBox.getHeight() * rotRatios[1]));
		scaledBox.setRight(smallestBox.getLeft()
				+ (smallestBox.getWidth() * (1 - rotRatios[2])));
		scaledBox.setTop(smallestBox.getBottom()
				+ (smallestBox.getHeight() * (1 - rotRatios[3])));

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

	public void clusterPages() throws IOException {
		PdfReader reader = new PdfReader(sourceFile.getAbsolutePath());

		for (int page = 1; page <= reader.getNumberOfPages(); page++) {
			Rectangle layoutBox = reader.getBoxSize(page, "crop");

			if (layoutBox == null) {
				layoutBox = reader.getBoxSize(page, "media");
			}

			// create Cluster
			// if the pagenumber should be excluded then use it as a
			// discriminating parameter, else use default value

			int pageNumber = -1;
			if (pageExcludes != null
					&& pageExcludes.getExcludedPageSet() != null
					&& pageExcludes.getExcludedPageSet().contains(page)) {
				pageNumber = page;
			}

			SingleCluster tmpCluster = new SingleCluster(page % 2 == 0,
					(int) layoutBox.getWidth(), (int) layoutBox.getHeight(),
					pageNumber);

			clusterCollection.addPageToCluster(tmpCluster, page);
		}

		// for every cluster create a set of pages on which the preview will
		// be based
		for (SingleCluster cluster : clusterCollection
				.getClusterToPagesMapping().keySet()) {
			cluster.choosePagesToMerge(clusterCollection
					.getClusterToPagesMapping().get(cluster));
		}
		reader.close();
	}

	public static class ClusterRenderWorker extends Thread {

		public int workerUnitCounter = 1;
		private final CropDocument cropDoc;

		public ClusterRenderWorker(CropDocument cropDoc) {
			this.cropDoc = cropDoc;
		}

		@Override
		public void run() {
			PdfDecoder pdfDecoder = new PdfDecoder(true);
			try {
				pdfDecoder.openPdfFile(cropDoc.getSourceFile()
						.getAbsolutePath());
			} catch (PdfException e1) {
				e1.printStackTrace();
			}

			for (SingleCluster cluster : cropDoc.getClusterCollection()
					.getAsList()) {
				for (Integer pageNumber : cluster.getPagesToMerge()) {
					// TODO jpedal isn't able to render big images
					// correctly, so let's check if the image is big an
					// throw it away
					try {
						if (cluster.getImageData().isRenderable()) {
							BufferedImage renderedPage = pdfDecoder
									.getPageAsImage(pageNumber);
							cluster.getImageData().addImageToPreview(
									renderedPage);
							workerUnitCounter++;
						}
					} catch (PdfException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			// now close the reader as it's not used anymore
			pdfDecoder.closePdfFile();
		}
	}

	public int getTotalWorkUnits() {
		int size = 0;
		for (SingleCluster cluster : clusterCollection
				.getClusterToPagesMapping().keySet()) {
			size += cluster.getPagesToMerge().size();
		}
		return size;
	}

}
