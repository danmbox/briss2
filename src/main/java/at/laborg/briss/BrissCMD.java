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
import java.io.IOException;

import at.laborg.briss.model.ClusterDefinition;
import at.laborg.briss.model.CropDefinition;
import at.laborg.briss.model.CropFinder;
import at.laborg.briss.model.PageCluster;
import at.laborg.briss.utils.BrissFileHandling;
import at.laborg.briss.utils.ClusterCreator;
import at.laborg.briss.utils.ClusterRenderWorker;
import at.laborg.briss.utils.DocumentCropper;

import com.itextpdf.text.DocumentException;

public class BrissCMD {

	public static void autoCrop(String[] args) {

		CommandValues workDescription = CommandValues
				.parseToWorkDescription(args);
		ClusterDefinition clusterDefinition = null;
		try {
			clusterDefinition = ClusterCreator.clusterPages(
					workDescription.getSourceFile(), null);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ClusterRenderWorker cRW = new ClusterRenderWorker(
				workDescription.getSourceFile(), clusterDefinition);
		cRW.start();

		System.out.print("Clustering.");
		while (cRW.isAlive()) {
			System.out.print(".");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("finished!");
		try {

			for (PageCluster cluster : clusterDefinition.getClusterList()) {
				Float[] auto = CropFinder.getAutoCropFloats(cluster
						.getImageData().getPreviewImage());
				cluster.addRatios(auto);
			}
			CropDefinition cropDefintion = CropDefinition.createCropDefinition(
					workDescription.getSourceFile(),
					workDescription.getDestFile(), clusterDefinition);
			DocumentCropper.crop(cropDefintion);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

	private static class CommandValues {

		private final static String SOURCE_FILE_CMD = "-s";
		private final static String DEST_FILE_CMD = "-d";

		private File sourceFile;
		private File destFile;

		static CommandValues parseToWorkDescription(String[] args) {
			CommandValues commandValues = new CommandValues();
			int i = 0;
			while (i < args.length) {
				if (args[i].trim().equalsIgnoreCase(SOURCE_FILE_CMD)) {
					if (i < (args.length - 1)) {
						commandValues.setSourceFile(new File(args[i + 1]));
					}
				} else if (args[i].trim().equalsIgnoreCase(DEST_FILE_CMD)) {
					if (i < (args.length - 1)) {
						commandValues.setDestFile(new File(args[i + 1]));
					}
				}
				i++;
			}

			if (!isValidJob(commandValues)) {
				System.exit(-1);
			}

			return commandValues;
		}

		private static boolean isValidJob(CommandValues job) {
			if (job.getSourceFile() == null) {
				System.out
						.println("No source file submitted: try \"java -jar Briss.0.0.13 -s filename.pdf\"");
				return false;
			}
			if (!job.getSourceFile().exists()) {
				System.out.println("File: " + job.getSourceFile()
						+ " doesn't exist");
				return false;
			}
			if (job.getDestFile() == null) {
				File recommendedDest = BrissFileHandling
						.getRecommendedDestination(job.getSourceFile());
				job.setDestFile(recommendedDest);
				System.out
						.println("Since no destination was provided destination will be set to  : "
								+ recommendedDest.getAbsolutePath());
			}
			try {
				BrissFileHandling.checkValidStateAndCreate(job.getDestFile());
			} catch (IllegalArgumentException e) {
				System.out.println("Destination file couldn't be created!");
				return false;
			} catch (IOException e) {
				System.out.println("IO Error while creating destination file."
						+ e.getStackTrace());
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

		public File getDestFile() {
			return destFile;
		}

		public void setDestFile(File destFile) {
			this.destFile = destFile;
		}

	}
}
