/**
 * 
 */
package at.laborg.briss.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class DrawableCropRect extends Rectangle {

	static final int CORNER_DIMENSION = 20;

	private boolean selected = false;

	/**
	 * Copy constructor.
	 * 
	 * @param crop
	 */
	public DrawableCropRect(final DrawableCropRect crop) {
		super();
		this.x = crop.x;
		this.y = crop.y;
		this.height = crop.height;
		this.width = crop.width;
	}

	public DrawableCropRect() {
		super();
	}
	
	public DrawableCropRect(int x, int y, int w, int h) {
		super(x, y, w, h);
	}
	
	public final List<DrawableCropRect> split (List<Integer> at, boolean onX) {
		List<DrawableCropRect> result = new ArrayList<DrawableCropRect> ();
		at = new ArrayList<Integer> (at);
		at.add(onX ? x + width : y + height);
		int last = onX ? x : y;
		for (int p : at) {
			DrawableCropRect r = new DrawableCropRect (this);
			int d = p - last;
			if (onX) { r.x = last; r.width = d; }
			else { r.y = last; r.height = d; }
			last = p;
			result.add (r);
		}
		return result;
	}

	public final boolean isSelected() {
		return selected;
	}

	public final void setSelected(final boolean selected) {
		this.selected = selected;
	}

	public final void setNewHotCornerUL(final Point p) {
		int xLR = (int) getMaxX();
		int yLR = (int) getMaxY();
		setSize(xLR - p.x, yLR - p.y);
		x = p.x;
		y = p.y;
	}

	public final void setNewHotCornerLR(final Point p) {
		setSize(p.x - x, p.y - y);
	}

	public final boolean containsInHotCornerUL(final Point p) {
		return ((p.x > getX() && p.x <= getX() + CORNER_DIMENSION) && (p.y > getY() && p.y <= getY()
				+ CORNER_DIMENSION));
	}

	public final boolean containsInHotCornerLR(final Point p) {
		return ((p.x < getMaxX() && p.x > getMaxX() - CORNER_DIMENSION) && (p.y < getMaxY() && p.y > getMaxY()
				- CORNER_DIMENSION));
	}
}