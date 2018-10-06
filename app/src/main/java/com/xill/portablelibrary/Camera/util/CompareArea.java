package com.xill.portablelibrary.Camera.util;

import android.util.Size;

import java.util.Comparator;

/**
 * Created by Sami on 7/24/2017.
 */

public class CompareArea implements Comparator<Size> {
	@Override
	public int compare(Size lhs, Size rhs) {
		// We cast here to ensure the multiplications won't overflow
		return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
				(long) rhs.getWidth() * rhs.getHeight());
	}
}
