/**
 * Copyright (C) 2009 Michael A. MacDonald
 */
package android.androidVNC;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.DrawableContainer;

/**
 * @author Michael A. MacDonald
 *
 */
public abstract class AbstractBitmapDrawable extends DrawableContainer {
	Rect cursorRect;
	Rect clipRect;
	
	AbstractBitmapData data;
	
	static final Paint _defaultPaint;
	static final Paint _whitePaint;
	static final Paint _blackPaint;
	
	static {
		_defaultPaint = new Paint();
		_whitePaint = new Paint();
		_whitePaint.setColor(0xffffffff);
		_blackPaint = new Paint();
		_blackPaint.setColor(0xff000000);
	}

	AbstractBitmapDrawable(AbstractBitmapData data)
	{
		this.data = data;
		cursorRect = new Rect();
		clipRect = new Rect();
	}

	@Override
	public final void draw(Canvas canvas) {
		int checkpoint = canvas.save();
		try {
			// FIT_XY changes drawable bounds instead of applying an ImageView matrix.
			// Our framebuffer drawing uses remote pixels, so apply that transform here.
			if (data.vncCanvas.getScaleType() == android.widget.ImageView.ScaleType.FIT_XY
					&& data.framebufferwidth > 0 && data.framebufferheight > 0) {
				Rect bounds = getBounds();
				canvas.translate(bounds.left, bounds.top);
				canvas.scale((float) bounds.width() / data.framebufferwidth,
						(float) bounds.height() / data.framebufferheight);
			}
			drawFrame(canvas);
		} finally {
			canvas.restoreToCount(checkpoint);
		}
	}

	protected abstract void drawFrame(Canvas canvas);
	
	void draw(Canvas canvas, int xoff, int yoff)
	{
		canvas.drawBitmap(data.mbitmap, xoff, yoff, _defaultPaint);
		if(data.vncCanvas.connection.getUseLocalCursor())
		{
			setCursorRect(data.vncCanvas.mouseX, data.vncCanvas.mouseY);
			clipRect.set(cursorRect);
			if (canvas.clipRect(cursorRect))
			{
				drawCursor(canvas);
			}
		}
	}
	
	void drawCursor(Canvas canvas)
	{
		canvas.drawRect(cursorRect,_whitePaint);
		canvas.drawRect((float)cursorRect.left + 1, (float)cursorRect.top + 1, (float)cursorRect.right - 1, (float)cursorRect.bottom - 1, _blackPaint);
	}
	
	void setCursorRect(int mouseX, int mouseY)
	{
		cursorRect.left = mouseX - 2;
		cursorRect.right = cursorRect.left + 4;
		cursorRect.top = mouseY - 2;
		cursorRect.bottom = cursorRect.top + 4;			
	}

	/* (non-Javadoc)
	 * @see android.graphics.drawable.DrawableContainer#getIntrinsicHeight()
	 */
	@Override
	public int getIntrinsicHeight() {
		return data.framebufferheight;
	}

	/* (non-Javadoc)
	 * @see android.graphics.drawable.DrawableContainer#getIntrinsicWidth()
	 */
	@Override
	public int getIntrinsicWidth() {
		return data.framebufferwidth;
	}

	/* (non-Javadoc)
	 * @see android.graphics.drawable.DrawableContainer#getOpacity()
	 */
	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}

	/* (non-Javadoc)
	 * @see android.graphics.drawable.DrawableContainer#isStateful()
	 */
	@Override
	public boolean isStateful() {
		return false;
	}
}
