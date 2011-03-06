package at.laborg.briss.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ClusterImageData {

	private final static int MAX_PAGE_HEIGHT = 900;
	private final static int MAX_IMAGE_RENDER_SIZE = 2000 * 2000;

	private final boolean renderable;
	private BufferedImage previewImage;
	private WritableRaster raster = null;
	private short[][][] imgdata;
	private int imageCnt = 0;
	private final int totalImages;

	public ClusterImageData(int pageWidth, int pageHeight, int nrOfImages) {
		this.renderable = pageWidth * pageHeight < MAX_IMAGE_RENDER_SIZE;
		totalImages = nrOfImages;
	}

	public boolean isRenderable() {
		return renderable;
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
			imgdata = new short[previewImage.getWidth()][previewImage
					.getHeight()][totalImages];

		}

		add(scaleImage(imageToAdd, previewImage.getWidth(), previewImage
				.getHeight()));

	}

	private void add(BufferedImage image) {
		int[] tmp = null;
		int height = image.getHeight();
		int width = image.getWidth();
		for (int i = 0; i < width; ++i) {
			for (int j = 0; j < height; ++j) {
				imgdata[i][j][imageCnt] = (short) image.getRaster().getPixel(i,
						j, tmp)[0];
			}
		}
		imageCnt++;
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
		if (totalImages == 1) {
			for (int i = 0; i < previewImage.getWidth(); ++i) {
				for (int j = 0; j < previewImage.getHeight(); ++j) {
					raster.setSample(i, j, 0, imgdata[i][j][0]);
				}
			}
			previewImage.setData(raster);
			return previewImage;
		}
		int[][] sdvalue = calculateSdOfImages();
		for (int i = 0; i < previewImage.getWidth(); ++i) {
			for (int j = 0; j < previewImage.getHeight(); ++j) {
				raster.setSample(i, j, 0, sdvalue[i][j]);
			}
		}
		previewImage.setData(raster);
		return previewImage;
	}

	private int[][] calculateSdOfImages() {
		int width = imgdata.length;
		int height = imgdata[0].length;
		int[][] sum = new int[width][height];
		int[][] mean = new int[width][height];
		int[][] sd = new int[width][height];

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < imageCnt; k++) {
					sum[i][j] += imgdata[i][j][k];
				}
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				mean[i][j] = sum[i][j] / imageCnt;
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				sum[i][j] = 0;
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				for (int k = 0; k < imageCnt; k++) {
					sum[i][j] += (imgdata[i][j][k] - mean[i][j])
							* (imgdata[i][j][k] - mean[i][j]);
				}
			}
		}
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				sd[i][j] = 255 - (int) Math.sqrt(sum[i][j] / imageCnt);
			}
		}
		return sd;
	}
}
