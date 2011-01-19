package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

public class ClusterManager {

	public static ClusterJobData createClusterJob(File origFile)
			throws IOException, PdfException {

		PdfReader reader = new PdfReader(origFile.getAbsolutePath());
		ClusterJobData pdfCluster = new ClusterJobData(reader
				.getNumberOfPages(), origFile);
		reader.close();
		return pdfCluster;
	}

	public static PageCluster getPageCluster(int pageNumber,
			ClusterJobData pdfCluster) {
		if (pdfCluster.isDirty()) {
			for (PageCluster cluster : pdfCluster.getClustersToPages().keySet()) {
				for (Integer page : pdfCluster.getClustersToPages()
						.get(cluster)) {
					pdfCluster.getPagesToClusters().put(page - 1, cluster);
				}
			}
			pdfCluster.setDirty(false);
		}
		return pdfCluster.getPagesToClusters().get(pageNumber - 1);
	}

	public static void addPageToCluster(PageCluster tmpCluster, int pageNumber,
			ClusterJobData pdfCluster) {
		if (pdfCluster.getClustersToPages().containsKey(tmpCluster)) {
			// cluster exists
			List<Integer> pageNumbers = pdfCluster.getClustersToPages().get(
					tmpCluster);
			pageNumbers.add(pageNumber);

		} else {
			// new Cluster
			List<Integer> pageNumbers = new ArrayList<Integer>();
			pageNumbers.add(pageNumber);
			pdfCluster.getClustersToPages().put(tmpCluster, pageNumbers);
		}
		// whenever a page was added the pagesToClustersMapping isn't useful
		// anymore. This musst be handled when reading the pages
		pdfCluster.setDirty(true);
	}

	public static void clusterPages(ClusterJobData pdfCluster)
			throws IOException {
		PdfReader reader = new PdfReader(pdfCluster.getFile().getAbsolutePath());
		for (int i = 1; i <= pdfCluster.getPageCount(); i++) {
			Rectangle layoutBox = reader.getBoxSize(i, "crop");

			if (layoutBox == null) {
				layoutBox = reader.getBoxSize(i, "media");
			}

			// create Cluster
			// if the pagenumber should be excluded then use it as a
			// discriminating parameter, else use default value

			int pageNumber = -1;
			if (pdfCluster.getExcludedPageSet() != null
					&& pdfCluster.getExcludedPageSet().contains(i)) {
				pageNumber = i;
			}

			PageCluster tmpCluster = new PageCluster(i % 2 == 0,
					(int) layoutBox.getWidth(), (int) layoutBox.getHeight(),
					pageNumber);

			addPageToCluster(tmpCluster, i, pdfCluster);
		}
		// now render the pages and create the preview images
		// for every cluster create a set of pages on which the preview will
		// be based
		for (PageCluster cluster : pdfCluster.getClustersToPages().keySet()) {
			cluster.choosePagesToMerge(pdfCluster.getClustersToPages().get(
					cluster));
		}
		reader.close();
	}

	public static class ClusterRenderWorker extends Thread {

		public int workerUnitCounter = 1;
		private final ClusterJobData clusterJobData;

		public ClusterRenderWorker(ClusterJobData pdfCluster) {
			this.clusterJobData = pdfCluster;
		}

		@Override
		public void run() {
			PdfDecoder pdfDecoder = new PdfDecoder(true);
			try {
				pdfDecoder.openPdfFile(clusterJobData.getFile()
						.getAbsolutePath());
			} catch (PdfException e1) {
				e1.printStackTrace();
			}

			for (PageCluster cluster : clusterJobData.getClusters()) {
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
			ClusterJobData clusterJobData) {
		Map<Integer, List<Float[]>> res = new HashMap<Integer, List<Float[]>>();
		for (int page = 1; page <= clusterJobData.getPageCount(); page++) {
			res.put(page, getPageCluster(page, clusterJobData).getRatiosList());
		}
		return res;
	}
}
