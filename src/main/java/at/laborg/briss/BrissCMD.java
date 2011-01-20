package at.laborg.briss;

import java.io.File;
import java.io.IOException;

import org.jpedal.exception.PdfException;

public class BrissCMD {

	public static void autoCrop(String[] args) {

		ParsedCMDValues parsedCommandValues = ParsedCMDValues.parseToJob(args);
		ClusterJobData clusterJobData = null;
		try {
			clusterJobData = ClusterManager
					.createClusterJob(parsedCommandValues.getSourceFile());
			try {
				ClusterManager.clusterPages(clusterJobData);

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			ClusterManager.ClusterRenderWorker wT = new ClusterManager.ClusterRenderWorker(
					clusterJobData);
			wT.start();

			System.out.print("Starting to cluster");
			while (wT.isAlive()) {
				System.out.print(".");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PdfException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("finished!");
		// clustering finished

		// TODO create rectangles

		for (PageCluster cluster : clusterJobData.getClusterAsList()) {
			Float[] crop = new Float[4];
			crop[0] = 0.2f;
			crop[1] = 0.2f;
			crop[2] = 0.2f;
			crop[3] = 0.2f;
			cluster.addRatios(crop);
		}

		// cropping start
		System.out.print("Starting to crop:");

		String origName = clusterJobData.getFile().getAbsolutePath();
		String recommendedName = origName.substring(0, origName.length() - 4)
				+ "_cropped.pdf";
		File cropDest = new File(recommendedName);
		if (cropDest == null)
			return;
		if (!cropDest.exists()) {
			try {
				cropDest.createNewFile();
			} catch (IOException e) {
				// TODO show dialog
			}
		} else {
			System.out.println("Will not overwrite file...");
		}
		CropManager.crop(clusterJobData.getFile(), cropDest, clusterJobData);

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
			while (i < args.length - 1) {
				if (args[i].trim().equalsIgnoreCase(SOURCE_FILE_CMD)) {
					job.setSourceFile(new File(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(MERGE_PAGE_CMD)) {
					job.setPageMergeNumber(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(HORIZ_DIV_CMD)) {
					job.setHorizDivision(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(VERT_DIV_CMD)) {
					job.setVertDivision(Integer.valueOf(args[i + 1]));
				} else if (args[i].trim().equalsIgnoreCase(DEST_FILE_CMD)) {
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
