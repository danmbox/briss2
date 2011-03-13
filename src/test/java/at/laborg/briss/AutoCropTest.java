package at.laborg.briss;

import java.io.File;
import java.io.FileFilter;
import java.util.Date;

import at.laborg.briss.utils.BrissFileHandling;

public class AutoCropTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File wd = new File(System.getProperty("user.dir") + File.separatorChar
				+ "pdftests");
		File outputDirectory = new File(wd.getAbsolutePath()
				+ File.separatorChar + new Date().toString());
		outputDirectory.mkdir();

		for (File file : wd.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return arg0.getAbsolutePath().toLowerCase().endsWith(".pdf");
			}

		})) {
			String[] jobargs = new String[4];
			jobargs[0] = "-s";
			jobargs[1] = file.getAbsolutePath();
			jobargs[2] = "-d";
			File recommended = BrissFileHandling
					.getRecommendedDestination(file);

			String output = outputDirectory.getAbsolutePath()
					+ File.separatorChar + recommended.getName();
			jobargs[3] = output;
			BrissCMD.autoCrop(jobargs);
		}
	}

}
