package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jpedal.PdfDecoder;
import org.jpedal.exception.PdfException;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

public class ClusterManager {
	private HashMap<Integer, PDFPageCluster> pagesToClustersMapping;
	private HashMap<PDFPageCluster, List<Integer>> clustersToPagesMapping;

	private boolean dirty = true;

	private int pageCount;
	private Set<Integer> excludedPageSet;
	private PdfReader reader;
	private PdfDecoder pdfDecoder;

	public void reset() {
		pagesToClustersMapping = null;
		clustersToPagesMapping = null;
		dirty = true;
	}

	public PDFPageCluster getCluster(int pageNumber) {
		if (dirty) {
			for (PDFPageCluster cluster : clustersToPagesMapping.keySet()) {
				for (Integer page : clustersToPagesMapping.get(cluster)) {
					pagesToClustersMapping.put(page - 1, cluster);
				}
			}
			dirty = false;
		}
		return pagesToClustersMapping.get(pageNumber - 1);
	}

	public void init(int pageCount) {
		pagesToClustersMapping = new HashMap<Integer, PDFPageCluster>();
		clustersToPagesMapping = new HashMap<PDFPageCluster, List<Integer>>();
		dirty = true;
	}

	public void init(File pdfFile, Set<Integer> excludedPages)
			throws IOException, PdfException {
		reader = new PdfReader(pdfFile.getAbsolutePath());
		excludedPageSet = excludedPages;
		pageCount = reader.getNumberOfPages();
		pagesToClustersMapping = new HashMap<Integer, PDFPageCluster>();
		clustersToPagesMapping = new HashMap<PDFPageCluster, List<Integer>>();
		dirty = true;
		pdfDecoder = new PdfDecoder(true);
		pdfDecoder.openPdfFile(pdfFile.getAbsolutePath());

	}

	private <T extends Comparable<? super T>> List<T> asSortedList(
			Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public List<PDFPageCluster> getClusterAsList() {
		return asSortedList(clustersToPagesMapping.keySet());
	}

	public void addPage(PDFPageCluster tmpCluster, int pageNumber) {
		if (clustersToPagesMapping.containsKey(tmpCluster)) {
			// cluster exists
			List<Integer> pageNumbers = clustersToPagesMapping.get(tmpCluster);
			pageNumbers.add(pageNumber);

		} else {
			// new Cluster
			List<Integer> pageNumbers = new ArrayList<Integer>();
			pageNumbers.add(pageNumber);
			clustersToPagesMapping.put(tmpCluster, pageNumbers);
		}
		// whenever a page was added the pagesToClustersMapping isn't useful
		// anymore. This musst be handled when reading the pages
		dirty = true;
	}

	public Set<PDFPageCluster> getClusters() {
		return clustersToPagesMapping.keySet();
	}

	public void clusterPages() {
		for (int i = 1; i <= pageCount; i++) {
			Rectangle layoutBox = reader.getBoxSize(i, "crop");

			if (layoutBox == null) {
				layoutBox = reader.getBoxSize(i, "media");
			}

			// create Cluster
			// if the pagenumber should be excluded then use it as a
			// discriminating parameter, else use default value

			int pageNumber = -1;
			if (excludedPageSet != null && excludedPageSet.contains(i)) {
				pageNumber = i;
			}

			PDFPageCluster tmpCluster = new PDFPageCluster(i % 2 == 0,
					(int) layoutBox.getWidth(), (int) layoutBox.getHeight(),
					pageNumber);

			addPage(tmpCluster, i);
		}
		// now render the pages and create the preview images
		// for every cluster create a set of pages on which the preview will
		// be based
		for (PDFPageCluster cluster : clustersToPagesMapping.keySet()) {
			cluster.choosePagesToMerge(clustersToPagesMapping.get(cluster));
		}
	}

	public int getTotWorkUnits() {
		int size = 0;
		for (PDFPageCluster cluster : clustersToPagesMapping.keySet()) {
			size += cluster.getPagesToMerge().size();
		}
		return size;
	}

	public class WorkerThread extends Thread {

		public int workerUnitCounter = 1;

		@Override
		public void run() {
			for (PDFPageCluster cluster : getClusters()) {
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
		}

	}

}
