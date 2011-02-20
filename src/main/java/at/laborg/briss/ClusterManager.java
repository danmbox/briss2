package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import at.laborg.briss.model.ClusterJob;
import at.laborg.briss.model.ClusterSet;
import at.laborg.briss.model.SingleCluster;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

public class ClusterManager {

	public static ClusterJob createClusterJob(File origFile)
			throws IOException, PdfException {

		PdfReader reader = new PdfReader(origFile.getAbsolutePath());
		ClusterJob clusterJob = new ClusterJob(reader.getNumberOfPages(),
				origFile);
		reader.close();
		return clusterJob;
	}

	public static void clusterPages(ClusterJob clusterJob) throws IOException {
		PdfReader reader = new PdfReader(clusterJob.getSource().getAbsolutePath());
		for (int i = 1; i <= clusterJob.getClusters().getPageCount(); i++) {
			Rectangle layoutBox = reader.getBoxSize(i, "crop");

			if (layoutBox == null) {
				layoutBox = reader.getBoxSize(i, "media");
			}

			// create Cluster
			// if the pagenumber should be excluded then use it as a
			// discriminating parameter, else use default value

			int pageNumber = -1;
			if (clusterJob.getExcludedPageSet() != null
					&& clusterJob.getExcludedPageSet().contains(i)) {
				pageNumber = i;
			}

			SingleCluster tmpCluster = new SingleCluster(i % 2 == 0,
					(int) layoutBox.getWidth(), (int) layoutBox.getHeight(),
					pageNumber);

			clusterJob.getClusters().addPageToCluster(tmpCluster, i);
		}
		// now render the pages and create the preview images
		// for every cluster create a set of pages on which the preview will
		// be based
		for (SingleCluster cluster : clusterJob.getClusters().getClustersToPages()
				.keySet()) {
			cluster.choosePagesToMerge(clusterJob.getClusters().getClustersToPages()
					.get(cluster));
		}
		reader.close();
	}

	public static class ClusterRenderWorker extends Thread {

		public int workerUnitCounter = 1;
		private final ClusterJob clusterJob;

		public ClusterRenderWorker(ClusterJob clusterJob) {
			this.clusterJob = clusterJob;
		}

		@Override
		public void run() {
			PdfDecoder pdfDecoder = new PdfDecoder(true);
			try {
				pdfDecoder
						.openPdfFile(clusterJob.getSource().getAbsolutePath());
			} catch (PdfException e1) {
				e1.printStackTrace();
			}

			for (SingleCluster cluster : clusterJob.getClusters()
					.getAsList()) {
				for (Integer pageNumber : cluster.getPagesToMerge()) {
					// TODO jpedal isn't able to render big images
					// correctly, so let's check if the image is big an
					// throw it away
					try {
						if (cluster.isFunctional()) {
							BufferedImage renderedPage = pdfDecoder
									.getPageAsImage(pageNumber);
							cluster.addImageToPreview(renderedPage);
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

	public static Map<Integer, List<Float[]>> getCropRectangles(
			ClusterSet clusters) {
		Map<Integer, List<Float[]>> res = new HashMap<Integer, List<Float[]>>();
		for (int page = 1; page <= clusters.getPageCount(); page++) {
			res.put(page, clusters.getSingleCluster(page).getRatiosList());
		}
		return res;
	}
}
