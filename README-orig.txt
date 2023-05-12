// $Id$
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

################################################
BRISS - BRight Snippet Sire
################################################

This is a small application to crop PDF files. It helps the user to decide what
should be cropped by creating a overlay of similar pages (=>all pages within a pdf
 having the same size, orientation(even/odd)).


########################
General
########################
 * Homepage : http://sourceforge.net/projects/briss/
 * License: GPLv3
 * Author: Gerhard Aigner (gerhard.aigner@gmail.com
 * Requirements: Java
 * Operating systems: Windows, Linux, MacOSX
 * This software uses two libraries to render and crop PDF files:
  * itext (AGPLv3) http://itextpdf.com/
  * jpedal (LGPL) http://www.jpedal.org/


########################
Starting the application
########################
You can run the application by executing following command in terminal:

java -jar briss-0.9.jar
or
java -jar briss-0.9.jar cropthis.pdf

(The second line comes in handy if you want shortlinks for pdf editing)


########################
Commandline
########################

If you prefer command line and trust the basic automatic detection algorithm
use it this way (can be batched!):

java -jar briss-0.9.jar -s [SOURCEFILE] [-d [DESTINATIONFILE]]

Example:
java -jar briss-0.9.jar -s dogeatdog.pdf -d dogcrop.pdf
java -jar briss-0.9.jar -s dogeatdog.pdf

the second line will create the cropped pdf into dogeatdog_cropped.pdf



########################
Instructions
########################
1) Load a pdf by pressing "Load"
2) Create multiple crop rectangles for all page cluster: Each crop rectangle will
   result in a cropped page.
 2.1) Press the left mouse button on a corner where you want to start
 2.2) Draw the rectangle
 2.3) Release the mouse button
 * [OPTIONAL] Drag around crop rectangles (press and hold mouse button down)
 * [OPTIONAL] Set width/height to maximum: Select the crop rectangles by holding
    down CTRL + left click into rectangle to select. All crop rectangles will be
    resized to the biggest one, either on width or height.
3) Start the cropping by pressing "Crop" or preview and specify the destination of the cropped pdf.

[OPTIONAL FEATURES]
* Select a Rectangle with ctrl + mouse click. Then copy (ctrl-c) and paste it into another cluster.
* Use hotcorners to make a crop rectangle smaller or bigger


########################
Problems
########################
* If you want to crop really big files it might be necessary to start briss with
an additional parameter: "-Xms128m -Xmx1024m" (complete call would look like:
"java -Xms128m -Xmx1024m -jar briss-0.9.jar")
