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

import java.io.File;

public class BrissCMD {

	public static void autoCrop(String[] args) {

		CommandValues.parseToJob(args);

		// try {
		// cropJob = CropManager.createCropJob(pCV.sourceFile);
		// } catch (IOException e) {
		// TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// clusterJob = workflowClusterAndRender(clusterJob, pCV);

		// workflowRectFinding(clusterJob);

		// workflowCrop(clusterJob, cropJob, pCV);

	}

	// private static ClusterJob workflowClusterAndRender(ClusterJob clusterJob,
	// CommandValues pCV) {
	// System.out.println("Starting workflow: Cluster and Render (Source: "
	// + pCV.getSourceFile().getName() + ").");
	//
	// try {
	// clusterJob = ClusterManager.createClusterJob(pCV.getSourceFile());
	// ClusterManager.clusterPages(clusterJob);
	// } catch (IOException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// } catch (PdfException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	//
	// ClusterManager.ClusterRenderWorker wT = new
	// ClusterManager.ClusterRenderWorker(
	// clusterJob);
	// wT.start();
	//
	// System.out.print("Clustering.");
	// while (wT.isAlive()) {
	// System.out.print(".");
	// try {
	// Thread.sleep(500);
	// } catch (InterruptedException e) {
	// }
	// }
	// System.out.println("finished!");
	//
	// return clusterJob;
	// }
	//
	// private static void workflowRectFinding(ClusterJob clusterJob) {
	// System.out
	// .println("Starting workflow: Finding crop rectangle (Number of cluster: "
	// + clusterJob.getClusterCollection().getAsList().size() + ").");
	// // for (SingleCluster cluster : clusterJob.getClusters().getAsList()) {
	// // Float[] ratios = calcCropAutomatic(cluster.getPreviewImage());
	// // cluster.addRatios(ratios);
	// // }
	// }
	//
	// private static void workflowCrop(ClusterJob clusterJob, CropDocument
	// cropDoc,
	// CommandValues pCV) {
	//
	// // cropping start
	// System.out.println("Starting workflow: Crop the file (Destination: "
	// + pCV.getDestFile().getName() + ").");
	//
	// try {
	//
	// // file exists and can't be overwritten
	// if (pCV.getDestFile().exists() && !pCV.isOverwrite()) {
	// System.out
	// .println("File destination: "
	// + pCV.getDestFile().getName()
	// +
	// " already exists and overwritting parameter (\"-o\") wasn't supplied. Exiting...");
	// return;
	// }
	//
	// // file exists and should be overwritten
	// if (pCV.getDestFile().exists() && pCV.isOverwrite()) {
	// if (pCV.getDestFile().delete()) {
	// pCV.getDestFile().createNewFile();
	// } else {
	// System.out.println("File: " + pCV.getDestFile().getName()
	// + " couldn't be deleted!");
	// return;
	// }
	// }
	//
	// // file doesn't exist, create it
	// if (!pCV.getDestFile().createNewFile()) {
	// System.out.println("File: " + pCV.getDestFile().getName()
	// + " couldn't be created!");
	// }
	//
	// // cropJob.setAndCreateDestinationFile(pCV.getDestFile());
	// // cropJob.setClusterCollection(clusterJob.getClusterCollection());
	//
	// // CropManager.crop(cropJob);
	//
	// System.out.println("Finished successfully !");
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// // } catch (DocumentException e) {
	// // TODO Auto-generated catch block
	// // e.printStackTrace();
	// }
	// }

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

		public void setOverwrite(boolean overwrite) {
			this.overwrite = overwrite;
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
