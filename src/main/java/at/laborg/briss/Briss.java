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

public final class Briss {

	private Briss() {
	};

	public static void main(final String[] args) {

		// this needs to be set in order to cope with jp2000 images
		System.setProperty("org.jpedal.jai", "true");

		// check if args are present, if so try to start commandline briss
		boolean gui = true;
		boolean customcrop = false;

		for(String a : args) {
			if(a.equalsIgnoreCase("-h") || a.equalsIgnoreCase("--help")) {
				System.out.println("Usage:\n\tbriss [-s SOURCE] [-d DESTINATION] [-c CROPARGS]");
				System.out.println("CROPARGS are in the format: part1_page1,part2_page1,...!part1_page2,part2_page2 \n where each part consists of 4 numbers: top/left/bottom/right");
				System.out.println("You can use the GUI to get these (use File/Show Crop Command)");
				System.out.println("split an a4 page into 2 a5:\n -c 0/0/0.5/0,0.5/0/0/0:0/0/0.5/0,0.5/0/0/0");
				return;
			}
			else
			if(a.equals("-c")) {
				customcrop = true;
			}
			else
			if(a.equals("-d")) {
				gui = false;
			}
		}


		if (!gui) {
			if(customcrop)
				BrissCMD.customCrop(args);
			else
			BrissCMD.autoCrop(args);
		} else {
			new BrissGUI(args);
		}
	}
}
