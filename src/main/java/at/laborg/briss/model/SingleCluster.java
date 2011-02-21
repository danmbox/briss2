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
package at.laborg.briss.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

public class SingleCluster implements Comparable<SingleCluster> {

	private final static int MERGE_VARIABILITY = 20;
	private final static int MAX_MERGE_PAGES = 20;
	private final static int MAX_PAGE_HEIGHT = 900;
	private final static int MAX_IMAGE_RENDER_SIZE = 2000 * 2000;

	private List<Integer> pagesToMerge;
	private List<Integer> allPages;
	private final List<Float[]> cropRatiosList = new ArrayList<Float[]>();

	private BufferedImage previewImage;
	private WritableRaster raster = null;
	private double[][] imageData = null;
	public long[][] diffImageData = null;
	public long[][] lastImageData = null;

	private int excludedPageNumber = -1;
	private final boolean renderable;
	private final boolean evenPage;
	private final int pageWidth;
	private final int pageHeight;

	public SingleCluster(boolean isEvenPage, int pageWidth, int pageHeight,
			int excludedPageNumber) {
		super();
		this.pageWidth = pageWidth;
		this.pageHeight = pageHeight;
		this.renderable = pageWidth * pageHeight < MAX_IMAGE_RENDER_SIZE;
		this.evenPage = isEvenPage;
		this.excludedPageNumber = excludedPageNumber;
		this.pagesToMerge = new ArrayList<Integer>();
	}

	public void addImageToPreview(BufferedImage imageToAdd) {
		if (!renderable)
			return;
		if (previewImage == null) {
			int pageHeight = imageToAdd.getHeight() > MAX_PAGE_HEIGHT ? MAX_PAGE_HEIGHT
					: imageToAdd.getHeight();
			float scaleFactor = (float) pageHeight / imageToAdd.getHeight();
			int pageWidth = (int) (imageToAdd.getWidth() * scaleFactor);
			// create the first preview image
			previewImage = new BufferedImage(pageWidth, pageHeight,
					BufferedImage.TYPE_BYTE_GRAY);
			previewImage.getGraphics().drawImage(
					scaleImage(imageToAdd, pageWidth, pageHeight), 0, 0, null);
			raster = previewImage.getRaster().createCompatibleWritableRaster();
			imageData = new double[previewImage.getWidth()][previewImage
					.getHeight()];
			// TODO remove if useless
			lastImageData = new long[previewImage.getWidth()][previewImage
					.getHeight()];
			diffImageData = new long[previewImage.getWidth()][previewImage
					.getHeight()];
			int[] tmp = null;
			for (int k = 0; k < previewImage.getHeight(); ++k) {
				for (int j = 0; j < previewImage.getWidth(); ++j) {
					lastImageData[j][k] += previewImage.getRaster().getPixel(j,
							k, tmp)[0];
				}
			}
			TMPC.imgdata = new int[previewImage.getWidth()][previewImage
					.getHeight()][pagesToMerge.size()];
			TMPC.cnt = 0;

		}
		// scale image to the first added
		average(scaleImage(imageToAdd, previewImage.getWidth(), previewImage
				.getHeight()), imageData);
		adddiff(scaleImage(imageToAdd, previewImage.getWidth(), previewImage
				.getHeight()), lastImageData, diffImageData);
	}

	private static void adddiff(BufferedImage curImage, long[][] oldImage,
			long[][] diffValues) {
		int[] tmp = null;
		for (int k = 0; k < curImage.getHeight(); ++k) {
			for (int j = 0; j < curImage.getWidth(); ++j) {
				int curP = curImage.getRaster().getPixel(j, k, tmp)[0];
				if (curP != oldImage[j][k]) {
					TMPC.imgdata[j][k][TMPC.cnt]++;
				}
				diffValues[j][k] += Math.abs(curP - oldImage[j][k]);
				oldImage[j][k] = curP;

			}
		}
		TMPC.cnt++;
	}

	private static void average(BufferedImage image, double[][] values) {
		for (int k = 0; k < image.getHeight(); ++k) {
			for (int j = 0; j < image.getWidth(); ++j) {
				// values[j][k] += image.getRaster().getSample(j, k, 0);
				// if (TMPC.cnt > 0) {

				// }
				// TMPC.imgdata[j][k][TMPC.cnt] = image.getRaster().getPixel(j,
				// k, tmp)[0];
			}
		}
		// TMPC.cnt++;
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

	private static BufferedImage getUnrenderableImage() {
		int width = 200;
		int height = 200;

		// Create buffered image that does not support transparency
		BufferedImage bimage = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);

		Graphics2D g2d = bimage.createGraphics();

		// Draw on the image
		g2d.setColor(Color.WHITE);
		g2d.drawRect(5, 5, 190, 190);

		Font font = new Font("Sansserif", Font.BOLD | Font.PLAIN, 22);
		g2d.setFont(font);

		g2d.setColor(Color.WHITE);
		g2d.drawString("Image to Big!", 10, 110);

		g2d.dispose();
		return bimage;
	}

	public BufferedImage getPreviewImage() {

		if (!renderable)
			return getUnrenderableImage();
		for (int k = 0; k < previewImage.getHeight(); ++k) {
			for (int j = 0; j < previewImage.getWidth(); ++j) {
				raster.setSample(j, k, 0, Math.round(imageData[j][k]
						/ (getPagesToMerge().size())));
				// TODO REMOVE
				raster.setSample(j, k, 0, Math.round(diffImageData[j][k]
						/ (getPagesToMerge().size())));
			}
		}
		previewImage.setData(raster);
		return previewImage;
	}

	/**
	 * returns the ratio to crop the page x1,y1,x2,y2, origin = bottom left x1:
	 * from left edge to left edge of crop rectange y1: from lower edge to lower
	 * edge of crop rectange x2: from right edge to right edge of crop rectange
	 * y2: from top edge to top edge of crop rectange
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
		// check if already in
		if (!cropRatiosList.contains(ratios)) {
			cropRatiosList.add(ratios);
		}

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (evenPage ? 1231 : 1237);
		result = prime * result + excludedPageNumber;
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
		SingleCluster other = (SingleCluster) obj;
		if (evenPage != other.evenPage)
			return false;
		if (excludedPageNumber != other.excludedPageNumber)
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

	public boolean isFunctional() {
		return renderable;
	}

	public int compareTo(SingleCluster that) {

		return this.getFirstPage() - that.getFirstPage();
	}

	private int getFirstPage() {
		int small = Integer.MAX_VALUE;
		for (Integer tmp : allPages) {
			if (tmp < small) {
				small = tmp;
			}
		}
		return small;
	}

}
