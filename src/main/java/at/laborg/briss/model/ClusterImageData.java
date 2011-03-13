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
	private BufferedImage outputImage = null;
	private int outputImageHeight = -1;
	private int outputImageWidth = -1;
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
		if (outputImageHeight == -1) {
			initializeOutputImage(imageToAdd);
		}
		add(scaleImage(imageToAdd, outputImageWidth, outputImageHeight));
	}

	private void initializeOutputImage(BufferedImage imageToAdd) {
		outputImageHeight = imageToAdd.getHeight() > MAX_PAGE_HEIGHT ? MAX_PAGE_HEIGHT
				: imageToAdd.getHeight();
		float scaleFactor = (float) outputImageHeight / imageToAdd.getHeight();
		outputImageWidth = (int) (imageToAdd.getWidth() * scaleFactor);
		imgdata = new short[outputImageWidth][outputImageHeight][totalImages];
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

	public BufferedImage getPreviewImage() {

		if (!renderable)
			return getUnrenderableImage();
		if (outputImage == null) {
			outputImage = renderOutputImage();
		}
		return outputImage;
	}

	private BufferedImage renderOutputImage() {
		BufferedImage outputImage = new BufferedImage(outputImageWidth,
				outputImageHeight, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = outputImage.getRaster()
				.createCompatibleWritableRaster();

		if (totalImages == 1) {
			for (int i = 0; i < outputImage.getWidth(); ++i) {
				for (int j = 0; j < outputImage.getHeight(); ++j) {
					raster.setSample(i, j, 0, imgdata[i][j][0]);
				}
			}
			outputImage.setData(raster);
			return outputImage;
		}
		int[][] sdvalue = calculateSdOfImages(imgdata, imageCnt);
		for (int i = 0; i < outputImage.getWidth(); ++i) {
			for (int j = 0; j < outputImage.getHeight(); ++j) {
				raster.setSample(i, j, 0, sdvalue[i][j]);
			}
		}
		outputImage.setData(raster);
		return outputImage;
	}

	private static BufferedImage scaleImage(BufferedImage bsrc, int width,
			int height) {

		BufferedImage bdest = new BufferedImage(width, height,
				BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = bdest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance(
				(double) bdest.getWidth() / bsrc.getWidth(),
				(double) bdest.getHeight() / bsrc.getHeight());
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

	private static int[][] calculateSdOfImages(short[][][] imgdata, int imageCnt) {
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
