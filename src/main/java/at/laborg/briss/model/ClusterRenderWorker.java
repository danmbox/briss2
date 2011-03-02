/**
 * 
 */
package at.laborg.briss.model;

import java.awt.image.BufferedImage;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import at.laborg.briss.utils.CropDocument;

public class ClusterRenderWorker extends Thread {

	public int workerUnitCounter = 1;
	private final CropDocument cropDoc;

	public ClusterRenderWorker(CropDocument cropDoc) {
		this.cropDoc = cropDoc;
	}

	@Override
	public void run() {
		PdfDecoder pdfDecoder = new PdfDecoder(true);
		try {
			pdfDecoder.openPdfFile(cropDoc.getSourceFile().getAbsolutePath());
		} catch (PdfException e1) {
			e1.printStackTrace();
		}

		for (SingleCluster cluster : cropDoc.getClusterCollection()
				.getClusters()) {
			for (Integer pageNumber : cluster.getPagesToMerge()) {
				// TODO jpedal isn't able to render big images
				// correctly, so let's check if the image is big an
				// throw it away
				try {
					if (cluster.getImageData().isRenderable()) {
						BufferedImage renderedPage = pdfDecoder
								.getPageAsImage(pageNumber);
						cluster.getImageData().addImageToPreview(renderedPage);
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