// $Id$
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
package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PDFPageClusterInfo {

	private final static int MAX_MERGE_PAGES = 20;
	private List<Integer> pagesToMerge;
	private List<Integer> allPages;
	private BufferedImage previewImage;
	private float[] ratios;

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

	/**
	 * returns the ratio to crop the page x1,y1,x2,y2, origin = bottom left
	 * 
	 * @return
	 */
	public float[] getRatios() {
		return ratios;
	}

	public void setRatios(float[] ratios) {
		this.ratios = ratios;
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
		allPages = pages;
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

	public List<Integer> getAllPages() {
		return allPages;
	}

	public List<Integer> getPagesToMerge() {
		return pagesToMerge;
	}

}
