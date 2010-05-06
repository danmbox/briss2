package at.laborg.jpdfcrop;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PDFPageClusterInfo {

	private final static int MAX_MERGE_PAGES = 10;
	private List<Integer> pagesToMerge;
	private BufferedImage previewImage;

	private boolean evenPage;
	private int pageWidth;
	private int pageHeight;

	public PDFPageClusterInfo(boolean isEvenPage, int pageWidth, int pageHeight) {
		super();
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.evenPage = isEvenPage;
		this.pagesToMerge = new ArrayList<Integer>();
	}

	public void setPreviewImage(BufferedImage previewImage) {
		this.previewImage = previewImage;
	}

	public BufferedImage getPreviewImage() {
		return previewImage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (evenPage ? 1231 : 1237);
		result = prime * result + pageHeight;
		result = prime * result + pageWidth;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PDFPageClusterInfo other = (PDFPageClusterInfo) obj;
		if (evenPage != other.evenPage)
			return false;
		if (pageHeight != other.pageHeight)
			return false;
		if (pageWidth != other.pageWidth)
			return false;
		return true;
	}

	public boolean isEvenPage() {
		return evenPage;
	}

	public int getPageWidth() {
		return pageWidth;
	}

	public int getPageHeight() {
		return pageHeight;
	}

	public void choosePagesToMerge(List<Integer> pages) {
		if (pages.size() < MAX_MERGE_PAGES) {
			// use all pages
			pagesToMerge = pages;
		} else {
			// use an equal distribution
			float stepWidth = (float) pages.size() / MAX_MERGE_PAGES;
			float totalStepped = 0;
			for (int i = 0; i < MAX_MERGE_PAGES; i++) {
				pagesToMerge.add(pages.get(new Double(Math.floor(totalStepped))
						.intValue()));
				totalStepped += stepWidth;
			}
		}

	}

	public List<Integer> getPagesToMerge() {
		return pagesToMerge;
	}


}
