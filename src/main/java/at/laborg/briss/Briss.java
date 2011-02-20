package at.laborg.briss;

public class Briss {

	public static void main(String args[]) {
		// check if args are present, if so try to start commandline briss
		if (args.length > 0) {
			BrissCMD.autoCrop(args);
		} else {
			new BrissGUI();
		}
	}
}
