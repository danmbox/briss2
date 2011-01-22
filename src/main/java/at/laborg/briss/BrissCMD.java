package at.laborg.briss;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import org.jpedal.exception.PdfException;

public class BrissCMD {

	public static void autoCrop(String[] args) {

		ParsedCMDValues pCV = ParsedCMDValues.parseToJob(args);
		ClusterJobData clusterJobData = null;

		clusterJobData = workflowClusterAndRender(clusterJobData, pCV);

		workflowRectFinding(clusterJobData);

		workflowCrop(clusterJobData, pCV);

	}

	private static ClusterJobData workflowClusterAndRender(
			ClusterJobData clusterJobData, ParsedCMDValues pCV) {
		System.out.println("Starting workflow: Cluster and Render (Source: "
				+ pCV.getSourceFile().getName() + ").");

		try {
			clusterJobData = ClusterManager.createClusterJob(pCV
					.getSourceFile());
			ClusterManager.clusterPages(clusterJobData);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (PdfException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ClusterManager.ClusterRenderWorker wT = new ClusterManager.ClusterRenderWorker(
				clusterJobData);
		wT.start();

		System.out.print("Clustering.");
		while (wT.isAlive()) {
			System.out.print(".");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("finished!");
		return clusterJobData;
	}

	private static void workflowRectFinding(ClusterJobData clusterJobData) {
		System.out
				.println("Starting workflow: Finding crop rectangle (Number of cluster: "
						+ clusterJobData.getClusterAsList().size() + ").");
		for (PageCluster cluster : clusterJobData.getClusterAsList()) {
			Float[] ratios = calcCropAutomatic(cluster.getPreviewImage());
			cluster.addRatios(ratios);
		}
	}

	private static void workflowCrop(ClusterJobData clusterJobData,
			ParsedCMDValues pCV) {

		// cropping start
		System.out.println("Starting workflow: Crop the file (Destination: "
				+ pCV.getDestFile().getName() + ").");

		try {

			// file exists and can't be overwritten
			if (pCV.getDestFile().exists() && !pCV.isOverwrite()) {
				System.out
						.println("File destination: "
								+ pCV.getDestFile().getName()
								+ " already exists and overwritting parameter (\"-o\") wasn't supplied. Exiting...");
				return;
			}

			// file exists and should be overwritten
			if (pCV.getDestFile().exists() && pCV.isOverwrite()) {
				if (pCV.getDestFile().delete()) {
					pCV.getDestFile().createNewFile();
				} else {
					System.out.println("File: " + pCV.getDestFile().getName()
							+ " couldn't be deleted!");
					return;
				}
			}

			// file doesn't exist, create it
			if (!pCV.getDestFile().createNewFile()) {
				System.out.println("File: " + pCV.getDestFile().getName()
						+ " couldn't be created!");
			}

			// now we can assure that the file is
			CropManager.crop(clusterJobData.getFile(), pCV.getDestFile(),
					clusterJobData);
			System.out.println("Finished successfully !");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Float[] calcCropAutomatic(BufferedImage previewImage) {

		Raster raster = previewImage.getData();
		// TODO check if image is big enough to crop! smallest usefull size:
		// 40x40
		// TODO do x and y directional cropping after each other FIRST y THEN x
		int[] tmp = null;

		long[][] rdx = new long[raster.getWidth() - 1][raster.getHeight()];
		long[][] rdy = new long[raster.getHeight() - 1][raster.getWidth()];

		for (int i = 0; i < raster.getWidth() - 1; i++) {
			for (int j = 0; j < raster.getHeight(); j++) {
				rdx[i][j] = raster.getPixel(i + 1, j, tmp)[0]
						- raster.getPixel(i, j, tmp)[0];
			}
		}

		for (int j = 0; j < raster.getHeight() - 1; j++) {
			for (int i = 0; i < raster.getWidth(); i++) {
				rdy[j][i] = raster.getPixel(i, j + 1, tmp)[0]
						- raster.getPixel(i, j, tmp)[0];
			}
		}

		float[] xd = new float[rdx.length];
		float[] yd = new float[rdy.length];

		for (int i = 0; i < rdx.length; i++) {
			xd[i] = rauschmas(rdx[i], 0, rdx[0].length);
		}

		for (int j = 0; j < rdy.length; j++) {
			yd[j] = rauschmas(rdy[j], 0, rdy[0].length);
		}

		float l, r, t, b; // left, right, top, bottom

		// TODO seedcenter can be somwhere like 40% and 60%
		int seedCenterX = raster.getWidth() / 2;
		int seedCenterY = raster.getHeight() / 2;

		// TODO create method for edge finding
		// find right edge
		boolean rightEdgeFound = false;
		int i = seedCenterX;
		while (!rightEdgeFound) {
			if (mean(xd, i, i + 5) < 0.3) {
				rightEdgeFound = true;
			} else {
				if (i >= (xd.length - 5))
					rightEdgeFound = true;
				i++;
			}
		}
		r = (float) (xd.length - i) / xd.length;

		// find left edge
		boolean leftEdgeFound = false;
		i = seedCenterX;
		while (!leftEdgeFound) {
			if (mean(xd, i - 5, i) < 0.3) {
				leftEdgeFound = true;
			} else {
				if (i <= 5)
					leftEdgeFound = true;
				i--;
			}
		}
		l = (float) i / xd.length;

		// find bottom edge
		boolean bottomEdgeFound = false;
		i = seedCenterY;
		while (!bottomEdgeFound) {
			if (mean(yd, i, i + 5) < 0.3) {
				bottomEdgeFound = true;
			} else {
				if (i >= (yd.length - 5))
					bottomEdgeFound = true;
				i++;
			}
		}
		b = (float) (yd.length - i) / yd.length;

		// find top edge
		boolean topEdgeFound = false;
		i = seedCenterY;
		while (!topEdgeFound) {
			if (mean(yd, i - 5, i) < 0.3) {
				topEdgeFound = true;
			} else {
				if (i <= 5)
					topEdgeFound = true;
				i--;
			}
		}
		t = (float) i / yd.length;

		System.out
				.printf(
						"Crop rectangle (Left: %2.1f%%, Bottom: %2.1f%%, Right: %2.1f%%, Top: %2.1f%%) \n",
						l * 100, b * 100, r * 100, t * 100);

		Float[] res = new Float[4];
		res[0] = l;
		res[1] = b;
		res[2] = r;
		res[3] = t;

		return res;
	}

	private static float mean(float[] x, int startIndex, int endIndex) {
		float sum = 0;
		for (int i = startIndex; i < endIndex; i++) {
			sum += x[i];
		}
		return sum / (endIndex - startIndex);
	}

	private static float rauschmas(long[] x, int startIndex, int endIndex) {
		float sum = 0;
		for (int i = startIndex; i < endIndex; i++) {
			sum += Math.abs(x[i]);
		}
		return sum / (endIndex - startIndex);
	}

	private static class ParsedCMDValues {

		private final static String SOURCE_FILE_CMD = "-s";
		private final static String DEST_FILE_CMD = "-d";
		private final static String MERGE_PAGE_CMD = "-p";
		private final static String HORIZ_DIV_CMD = "-h";
		private final static String VERT_DIV_CMD = "-v";
		private final static String OVERWRITE = "-o";

		private File sourceFile;
		private File destFile;
		private int pageMergeNumber = 20;
		private int horizDivision = -1;
		private int vertDivision = -1;
		private boolean overwrite = false;

		static ParsedCMDValues parseToJob(String[] args) {
			ParsedCMDValues job = new ParsedCMDValues();
			int i = 0;
			while (i < args.length) {
				if (args[i].trim().equalsIgnoreCase(SOURCE_FILE_CMD)) {
					if (i < (args.length - 1))
						job.setSourceFile(new File(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(MERGE_PAGE_CMD)) {
					if (i < (args.length - 1))
						job.setPageMergeNumber(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(HORIZ_DIV_CMD)) {
					if (i < (args.length - 1))
						job.setHorizDivision(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(VERT_DIV_CMD)) {
					if (i < (args.length - 1))
						job.setVertDivision(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(DEST_FILE_CMD)) {
					if (i < (args.length - 1))
						job.setDestFile(new File(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(OVERWRITE)) {
					job.setOverwrite(true);
				}
				i++;
			}

			if (!isValidJob(job)) {
				System.exit(-1);
			}
			return job;
		}

		private static boolean isValidJob(ParsedCMDValues job) {
			if (job.getSourceFile() == null) {
				System.out
						.println("No File submitted: try \"java -jar Briss.0.0.13 -s filename.pdf\"");
				return false;
			}
			if (!job.getSourceFile().exists()) {
				System.out.println("File: " + job.getSourceFile()
						+ " doesn't exist");
				return false;
			}
			return true;
		}

		public File getSourceFile() {
			return sourceFile;
		}

		public void setSourceFile(File sourceFile) {
			this.sourceFile = sourceFile;
		}

		public boolean isOverwrite() {
			return overwrite;
		}

		public void setOverwrite(boolean overwrite) {
			this.overwrite = overwrite;
		}

		public File getDestFile() {
			if (destFile == null) {
				String srcString = getSourceFile().getAbsolutePath();
				String recommendedName = srcString.substring(0, srcString
						.length() - 4)
						+ "_cropped.pdf";
				destFile = new File(recommendedName);
			}
			return destFile;
		}

		public void setDestFile(File destFile) {
			this.destFile = destFile;
		}

		public int getPageMergeNumber() {
			return pageMergeNumber;
		}

		public void setPageMergeNumber(int pageMergeNumber) {
			this.pageMergeNumber = pageMergeNumber;
		}

		public int getHorizDivision() {
			return horizDivision;
		}

		public void setHorizDivision(int horizDivision) {
			this.horizDivision = horizDivision;
		}

		public int getVertDivision() {
			return vertDivision;
		}

		public void setVertDivision(int vertDivision) {
			this.vertDivision = vertDivision;
		}

	}
}
