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
package at.laborg.briss;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class MergedPanel extends JPanel implements MouseMotionListener,
		MouseListener {

	private static final int MINIMUM_WIDTH = 30;
	private static final int MINIMUM_HEIGHT = 40;
	private BufferedImage img;
	private int zoomXStart, zoomXEnd, zoomYStart, zoomYEnd;
	private static Composite composite = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, .2f);
	private MergedPanel connectedMerge;
	private PDFPageClusterInfo cluster;

	public MergedPanel(PDFPageClusterInfo cluster) {
		super();
		this.cluster = cluster;
		this.img = cluster.getPreviewImage();
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		addMouseMotionListener(this);
		addMouseListener(this);
		setToolTipText(createInfoString(cluster));
	}

	private String createInfoString(PDFPageClusterInfo cluster) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(cluster.isEvenPage() ? "Even " : "Odd ").append("page<br>");
		sb.append(cluster.getAllPages().size() + " pages: ");
		int pagecounter = 0;
		for (Integer pageNumber : cluster.getAllPages()) {
			sb.append(pageNumber + " ");
			if (pagecounter++ > 10) {
				pagecounter = 0;
				sb.append("<br>");
			}
		}
		sb.append("</html>");
		return sb.toString();
	}

	@Override
	public void paint(Graphics g) {
		update(g);
	}

	@Override
	public void update(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(img, null, 0, 0);
		if (isEnabled()) {
			g2.setComposite(composite);
			g2.setColor(Color.BLUE);
			if (zoomXStart < zoomXEnd) {
				if (zoomYStart < zoomYEnd) {
					g2.fillRect(zoomXStart, zoomYStart, zoomXEnd - zoomXStart,
							zoomYEnd - zoomYStart);
				} else {
					g2.fillRect(zoomXStart, zoomYEnd, zoomXEnd - zoomXStart,
							zoomYStart - zoomYEnd);
				}
			} else {
				if (zoomYStart < zoomYEnd) {
					g2.fillRect(zoomXEnd, zoomYStart, zoomXStart - zoomXEnd,
							zoomYEnd - zoomYStart);
				} else {
					g2.fillRect(zoomXEnd, zoomYEnd, zoomXStart - zoomXEnd,
							zoomYStart - zoomYEnd);
				}
			}
		}
		g2.dispose();
	}

	@Override
	public void mouseDragged(MouseEvent mE) {
		zoomXEnd = mE.getX();
		zoomYEnd = mE.getY();
		if (connectedMerge != null) {
			connectedMerge.drawResizeWindow(zoomXStart, zoomXEnd, zoomYStart,
					zoomYEnd);
		}
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
	}

	@Override
	public void mouseClicked(MouseEvent mE) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent mE) {
		zoomXStart = mE.getX();
		zoomYStart = mE.getY();
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		cluster.setRatios(getCutRatiosForPdf());
	}

	public void setConnectedMerge(MergedPanel connectedMerge) {
		this.connectedMerge = connectedMerge;
	}

	public void drawResizeWindow(int zoomXStart, int zoomXEnd, int zoomYStart,
			int zoomYEnd) {
		this.zoomXStart = zoomXStart;
		this.zoomXEnd = zoomXEnd;
		this.zoomYStart = zoomYStart;
		this.zoomYEnd = zoomYEnd;
		repaint();
	}

	/**
	 * creates the crop ratios from the user selection. 0 = xStart 1 = xEnd 2 =
	 * yStart 3 = yEnd
	 * 
	 * @return the cropped ratios or null if to small
	 */
	private float[] getCutRatiosForPdf() {
		int x1, x2, y1, y2;
		// x1 should always be smaller than x2
		if (zoomXStart > zoomXEnd) {
			x1 = zoomXEnd;
			x2 = zoomXStart;
		} else {
			x1 = zoomXStart;
			x2 = zoomXEnd;
		}
		// y1 should always be smaller than y2
		if (zoomYStart > zoomYEnd) {
			y1 = zoomYEnd;
			y2 = zoomYStart;
		} else {
			y1 = zoomYStart;
			y2 = zoomYEnd;
		}

		// TODO check for maximum and minimum

		if ((x2 - x1) < MINIMUM_WIDTH) {
			return null;
		}
		if ((y2 - y1) < MINIMUM_HEIGHT) {
			return null;
		}

		float[] ratios = new float[4];
		// x bottom left
		ratios[0] = (float) x1 / img.getWidth();
		// y bottom left
		ratios[1] = (float) (img.getHeight() - y2) / img.getHeight();
		// x top right
		ratios[2] = (float) x2 / img.getWidth();
		// y top right
		ratios[3] = (float) (img.getHeight() - y1) / img.getHeight();

		return ratios;
	}

}
