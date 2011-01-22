package at.laborg.briss;

import java.io.File;
import java.io.FileFilter;

public class AutoCropTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File wd = new File(System.getProperty("user.dir") + File.separatorChar
				+ "pdftests");
		for (File file : wd.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return arg0.getAbsolutePath().toLowerCase().endsWith(".pdf");
			}

		})) {
			String[] jobargs = new String[2];
			jobargs[0] = "-s";
			jobargs[1] = file.getAbsolutePath();
			BrissCMD.autoCrop(jobargs);
		}
	}

}
