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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

public class PDFPageCluster {

	private final static int MERGE_VARIABILITY = 30;
	private final static int MAX_MERGE_PAGES = 20;
	private final static int MAX_PAGE_HEIGHT = 600;
	private List<Integer> pagesToMerge;
	private List<Integer> allPages;
	private BufferedImage previewImage;
	private WritableRaster raster = null;
	private double[][] imageData = null;
	private List<Float[]> cropRatiosList = new ArrayList<Float[]>();

	private boolean evenPage;
	private int pageWidth;
	private int pageHeight;

	public PDFPageCluster(boolean isEvenPage, int pageWidth, int pageHeight) {
		super();
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.evenPage = isEvenPage;
		this.pagesToMerge = new ArrayList<Integer>();
	}

	public void addImageToPreview(BufferedImage imageToAdd) {
		if (previewImage == null) {
			int pageHeight = imageToAdd.getHeight() > MAX_PAGE_HEIGHT ? MAX_PAGE_HEIGHT : imageToAdd.getHeight();
			float scaleFactor = (float)pageHeight/imageToAdd.getHeight();
			int pageWidth = (int) (imageToAdd.getWidth() * scaleFactor);
			// create the first preview image
			previewImage = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_BYTE_GRAY);
			previewImage.getGraphics().drawImage(scaleImage(imageToAdd,pageWidth,pageHeight), 0, 0, null);
			raster = previewImage.getRaster().createCompatibleWritableRaster();
			imageData = new double[previewImage.getWidth()][previewImage
					.getHeight()];
		}
		// scale image to the first added
		average(scaleImage(imageToAdd, previewImage.getWidth(), previewImage
				.getHeight()), imageData);
	}

	private static void average(BufferedImage image, double[][] values) {
		for (int k = 0; k < image.getHeight(); ++k) {
			for (int j = 0; j < image.getWidth(); ++j) {
				values[j][k] += image.getRaster().getSample(j, k, 0);
			}
		}
	}

	private static BufferedImage scaleImage(BufferedImage bsrc, int width,
			int height) {
		BufferedImage bdest = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = bdest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance((double) bdest
				.getWidth()
				/ bsrc.getWidth(), (double) bdest.getHeight()
				/ bsrc.getHeight());
		g.drawRenderedImage(bsrc, at);
		g.dispose();
		return bdest;
	}

	public BufferedImage getPreviewImage() {
		for (int k = 0; k < previewImage.getHeight(); ++k) {
			for (int j = 0; j < previewImage.getWidth(); ++j) {
				raster.setSample(j, k, 0, Math.round(imageData[j][k]
						/ (getPagesToMerge().size())));
			}
		}
		previewImage.setData(raster);
		return previewImage;
	}

	/**
	 * returns the ratio to crop the page x1,y1,x2,y2, origin = bottom left
	 * 
	 * @return
	 */
	public List<Float[]> getRatiosList() {
		return cropRatiosList;
	}

	public void clearRatios() {
		cropRatiosList.clear();
	}

	public void addRatios(Float[] ratios) {
		cropRatiosList.add(ratios);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (evenPage ? 1231 : 1237);
		result = prime * result + getRoundedPageHeight();
		result = prime * result + getRoundedPageWidth();
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
		PDFPageCluster other = (PDFPageCluster) obj;
		if (evenPage != other.evenPage)
			return false;
		if (getRoundedPageHeight() != other.getRoundedPageHeight())
			return false;
		if (getRoundedPageWidth() != other.getRoundedPageWidth())
			return false;
		return true;
	}

	public boolean isEvenPage() {
		return evenPage;
	}

	public int getRoundedPageHeight() {
		int tmp = pageHeight / MERGE_VARIABILITY;
		return tmp * MERGE_VARIABILITY;
	}

	public int getRoundedPageWidth() {
		int tmp = pageWidth / MERGE_VARIABILITY;
		return tmp * MERGE_VARIABILITY;
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
