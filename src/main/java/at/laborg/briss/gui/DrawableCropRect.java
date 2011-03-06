/**
 * 
 */
package at.laborg.briss.gui;

import java.awt.Rectangle;

@SuppressWarnings("serial")
public class DrawableCropRect extends Rectangle {
	private boolean selected = false;

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}