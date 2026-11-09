/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package android.androidVNC;

import android.widget.ImageView.ScaleType;

/**
 * Scales/stretches the remote desktop to fill 100% of the screen without letterboxing.
 */
class FitToFillScaling extends AbstractScaling {

	FitToFillScaling() {
		super(R.id.itemFitToFill, ScaleType.FIT_XY);
	}

	@Override
	boolean isAbleToPan() {
		return false;
	}

	@Override
	boolean isValidInputMode(int mode) {
		return mode == R.id.itemInputFitToScreen;
	}

	@Override
	int getDefaultHandlerId() {
		return R.id.itemInputFitToScreen;
	}

	@Override
	void setScaleTypeForActivity(VncCanvasActivity activity) {
		super.setScaleTypeForActivity(activity);
		activity.vncCanvas.absoluteXPosition = activity.vncCanvas.absoluteYPosition = 0;
		activity.vncCanvas.scrollTo(0, 0);
	}

	@Override
	void onConfigurationChanged(VncCanvasActivity activity) {
		if (activity.vncCanvas == null || activity.vncCanvas.bitmapData == null)
			return;
		activity.vncCanvas.absoluteXPosition = activity.vncCanvas.absoluteYPosition = 0;
		activity.vncCanvas.scrollTo(0, 0);
		activity.vncCanvas.invalidate();
	}

}
