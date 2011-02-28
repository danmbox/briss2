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
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

import org.jpedal.exception.PdfException;

import at.laborg.briss.model.CropDocument;

import com.itextpdf.text.DocumentException;

public class BrissCMD {

	public static void autoCrop(String[] args) {

		CommandValues pCV = CommandValues.parseToJob(args);
//		ClusterJob clusterJob = null;
		CropDocument cropDoc = null;

//		try {
//			cropJob = CropManager.createCropJob(pCV.sourceFile);
//		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		clusterJob = workflowClusterAndRender(clusterJob, pCV);

//		workflowRectFinding(clusterJob);

//		workflowCrop(clusterJob, cropJob, pCV);

	}

//	private static ClusterJob workflowClusterAndRender(ClusterJob clusterJob,
//			CommandValues pCV) {
//		System.out.println("Starting workflow: Cluster and Render (Source: "
//				+ pCV.getSourceFile().getName() + ").");
//
//		try {
//			clusterJob = ClusterManager.createClusterJob(pCV.getSourceFile());
//			ClusterManager.clusterPages(clusterJob);
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (PdfException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		ClusterManager.ClusterRenderWorker wT = new ClusterManager.ClusterRenderWorker(
//				clusterJob);
//		wT.start();
//
//		System.out.print("Clustering.");
//		while (wT.isAlive()) {
//			System.out.print(".");
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//			}
//		}
//		System.out.println("finished!");
//
//		return clusterJob;
//	}
//
//	private static void workflowRectFinding(ClusterJob clusterJob) {
//		System.out
//				.println("Starting workflow: Finding crop rectangle (Number of cluster: "
//						+ clusterJob.getClusterCollection().getAsList().size() + ").");
//		// for (SingleCluster cluster : clusterJob.getClusters().getAsList()) {
//		// Float[] ratios = calcCropAutomatic(cluster.getPreviewImage());
//		// cluster.addRatios(ratios);
//		// }
//	}
//
//	private static void workflowCrop(ClusterJob clusterJob, CropDocument cropDoc,
//			CommandValues pCV) {
//
//		// cropping start
//		System.out.println("Starting workflow: Crop the file (Destination: "
//				+ pCV.getDestFile().getName() + ").");
//
//		try {
//
//			// file exists and can't be overwritten
//			if (pCV.getDestFile().exists() && !pCV.isOverwrite()) {
//				System.out
//						.println("File destination: "
//								+ pCV.getDestFile().getName()
//								+ " already exists and overwritting parameter (\"-o\") wasn't supplied. Exiting...");
//				return;
//			}
//
//			// file exists and should be overwritten
//			if (pCV.getDestFile().exists() && pCV.isOverwrite()) {
//				if (pCV.getDestFile().delete()) {
//					pCV.getDestFile().createNewFile();
//				} else {
//					System.out.println("File: " + pCV.getDestFile().getName()
//							+ " couldn't be deleted!");
//					return;
//				}
//			}
//
//			// file doesn't exist, create it
//			if (!pCV.getDestFile().createNewFile()) {
//				System.out.println("File: " + pCV.getDestFile().getName()
//						+ " couldn't be created!");
//			}
//
////			cropJob.setAndCreateDestinationFile(pCV.getDestFile());
////			cropJob.setClusterCollection(clusterJob.getClusterCollection());
//
////			CropManager.crop(cropJob);
//
//			System.out.println("Finished successfully !");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
////		} catch (DocumentException e) {
//			// TODO Auto-generated catch block
////			e.printStackTrace();
//		}
//	}

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
				if (i >= (xd.length - 5)) {
					rightEdgeFound = true;
				}
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
				if (i <= 5) {
					leftEdgeFound = true;
				}
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
				if (i >= (yd.length - 5)) {
					bottomEdgeFound = true;
				}
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
				if (i <= 5) {
					topEdgeFound = true;
				}
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

	private static class CommandValues {

		private final static String SOURCE_FILE_CMD = "-s";
		private final static String DEST_FILE_CMD = "-d";
		private final static String MERGE_PAGE_CMD = "-p";
		private final static String HORIZ_DIV_CMD = "-h";
		private final static String VERT_DIV_CMD = "-v";
		private final static String OVERWRITE = "-o";

		private File sourceFile;
		private File destFile;
		private boolean overwrite = false;

		static CommandValues parseToJob(String[] args) {
			CommandValues job = new CommandValues();
			int i = 0;
			while (i < args.length) {
				if (args[i].trim().equalsIgnoreCase(SOURCE_FILE_CMD)) {
					if (i < (args.length - 1)) {
						job.setSourceFile(new File(args[i + 1]));
					}
				} else if (args[i].trim().equalsIgnoreCase(MERGE_PAGE_CMD)) {
					if (i < (args.length - 1)) {
						job.setPageMergeNumber(Integer.valueOf(args[i + 1]));
					}
				} else if (args[i].trim().equalsIgnoreCase(HORIZ_DIV_CMD)) {
					if (i < (args.length - 1)) {
						job.setHorizDivision(Integer.valueOf(args[i + 1]));
					}
				} else if (args[i].trim().equalsIgnoreCase(VERT_DIV_CMD)) {
					if (i < (args.length - 1)) {
						job.setVertDivision(Integer.valueOf(args[i + 1]));
					}
				} else if (args[i].trim().equalsIgnoreCase(DEST_FILE_CMD)) {
					if (i < (args.length - 1)) {
						job.setDestFile(new File(args[i + 1]));
					}
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

		private static boolean isValidJob(CommandValues job) {
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

		public void setPageMergeNumber(int pageMergeNumber) {
		}

		public void setHorizDivision(int horizDivision) {
		}

		public void setVertDivision(int vertDivision) {
		}

	}
}
