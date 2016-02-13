package stickStacker_v04;

import java.util.ArrayList;
import java.util.List;

import toxi.geom.Line2D;
import toxi.geom.Vec2D;

public class MJ_Math {
	private static final double t = 0.000001;

	public MJ_Math() {

	}

	public static float minDistOfLines(Line2D s, Line2D t, int resolution) {
		float minDist = Float.MAX_VALUE;
		for (int k = 0; k < resolution; k++) {
			float f = k * (s.getLength() / resolution);
			Vec2D ab = s.b.sub(s.a);
			ab = ab.getNormalizedTo(f);
			Vec2D start = s.a.add(ab);
			Vec2D end = t.closestPointTo(start);
			float d = start.distanceTo(end);
			if (d < minDist) {
				minDist = d;
			}
		}
		return minDist;
	}

	public static Line2D linearRegressor(List<Vec2D> data) {
		float sX = 0;
		float sY = 0;
		float sXY = 0;
		float sXX = 0;
		int n = data.size();
		float minX = 100000;
		float maxX = -100000;
		for (Vec2D p : data) {
			float x = p.x();
			if (x < minX) {
				minX = x;
			}
			if (x > maxX) {
				maxX = x;
			}
			float y = p.y();
			sX += x;
			sY += y;
			sXY += (x * y);
			sXX += Math.pow(x, 2);
		}
		float m = (float) ((sXY * n - (sX * sY)) / ((sXX * n) - (Math
				.pow(sX, 2))));
		float b = (sY - (m * sX)) / n;
		Vec2D start = new Vec2D(minX, m * minX + b);
		Vec2D end = new Vec2D(maxX, m * maxX + b);
		return new Line2D(start, end);
	}

	public static Line2D linearRegressorXY(List<Vec2D> data) {
		float sX = 0;
		float sY = 0;
		float sXY = 0;
		float sYY = 0;
		int n = data.size();
		float minY = 100000;
		float maxY = -100000;
		for (Vec2D p : data) {
			float x = p.x();

			float y = p.y();
			if (y < minY) {
				minY = y;
			}
			if (y > maxY) {
				maxY = y;
			}
			sX += x;
			sY += y;
			sXY += (x * y);
			sYY += Math.pow(y, 2);
		}
		float m = (float) ((sXY * n - (sX * sY)) / ((sYY * n) - (Math
				.pow(sY, 2))));
		float b = (sX - (m * sY)) / n;
		Vec2D start = new Vec2D(m * minY + b, minY);
		Vec2D end = new Vec2D(m * maxY + b, maxY);
		return new Line2D(start, end);
	}

	public static float findMinAngleOf2Lines(Line2D p, Line2D q) {
		Vec2D pD = p.getDirection().normalize();
		Vec2D qD = q.getDirection().normalize();
		float[] angles = new float[4];
		angles[0] = pD.getInverted().angleBetween(qD);
		angles[1] = pD.getInverted().angleBetween(qD.getInverted());
		angles[2] = pD.angleBetween(qD.getInverted());
		angles[3] = pD.angleBetween(qD);
		return getMin(angles);
	}

	public static <E> ArrayList<E> copyList(ArrayList<E> original) {
		ArrayList<E> copy = new ArrayList<E>();
		for (E element : original) {
			copy.add(element);
		}
		return copy;
	}

	public static Vec2D getAverageVec(ArrayList<Vec2D> vecs,
			float stdDevRangeFactor) {
		Vec2D average = new Vec2D();
		float[] xValues = new float[vecs.size()];
		ArrayList<Float> xAdjusted = new ArrayList<Float>();
		float[] yValues = new float[vecs.size()];
		ArrayList<Float> yAdjusted = new ArrayList<Float>();
		for (int i = 0; i < vecs.size(); i++) {
			xValues[i] = vecs.get(i).x;
			yValues[i] = vecs.get(i).y;
		}
		float Xave = getAverage(xValues);
		float XstdDev = getDeviation(Xave, xValues);
		for (int i = 0; i < xValues.length; i++) {
			if (Math.abs(xValues[i] - Xave) < XstdDev * stdDevRangeFactor) {
				xAdjusted.add(xValues[i]);
			}
		}
		float XaveAdjusted = getAverage(toArrayFloats(xAdjusted));
		float Yave = getAverage(yValues);
		float YstdDev = getDeviation(Yave, yValues);
		for (int i = 0; i < yValues.length; i++) {
			if (Math.abs(yValues[i] - Yave) < YstdDev * stdDevRangeFactor) {
				yAdjusted.add(yValues[i]);
			}
		}
		float YaveAdjusted = getAverage(toArrayFloats(yAdjusted));
		average = new Vec2D(XaveAdjusted, YaveAdjusted);
		return average;
	}

	// STATS FUNCTIIONS

	public static float getAverageWithSDrange(Float[] arr,
			float stdDevRangeFactor) {
		ArrayList<Float> adjusted = new ArrayList<Float>();
		float ave = getAverage(arr);
		float SD = getDeviation(ave, arr);
		for (int i = 0; i < arr.length; i++) {
			if (Math.abs(arr[i] - ave) < SD * stdDevRangeFactor) {
				adjusted.add(arr[i]);
			}
		}
		float returnValue = getAverage(toArrayFloats(adjusted));
		if (adjusted.isEmpty()) {
			returnValue = ave;
		}
		return returnValue;
	}

	public static float getDeviation(float average, Float[] arr) {
		Float[] sqdiffs = new Float[arr.length];
		for (int i = 0; i < arr.length; i++) {
			sqdiffs[i] = (float) Math.pow((arr[i] - average + t), 2);
		}
		float dev = (float) Math.sqrt(Math.abs(getAverage(sqdiffs)));
		return dev;
	}

	public static float getAverage(Float[] floats) {
		float sum = 0;
		for (int i = 0; i < floats.length; i++) {
			sum += floats[i];
		}
		float average = sum / floats.length;
		return average;
	}

	public static float getMin(Float[] vals) {
		float min = vals[0];
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] < min) {
				min = vals[i];
			}
		}
		return min;
	}

	public static float getMax(Float[] vals) {
		float max = vals[0];
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] > max) {
				max = vals[i];
			}
		}
		return max;
	}

	public static float getDeviation(float average, float[] values) {
		float[] sqdiffs = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			sqdiffs[i] = (float) Math.pow((values[i] - average + t), 2);
		}
		float dev = (float) Math.sqrt(Math.abs(getAverage(sqdiffs)));
		return dev;
	}

	public static float[] toArrayFloats(ArrayList<Float> input) {
		float[] fArray = new float[input.size()];
		for (int i = 0; i < input.size(); i++) {
			fArray[i] = input.get(i);
		}
		return fArray;
	}

	public static Float[] toArrayFloats_alt(ArrayList<Float> input) {
		Float[] fArray = new Float[input.size()];
		for (int i = 0; i < input.size(); i++) {
			fArray[i] = input.get(i);
		}
		return fArray;
	}

	public static float getAverage(float[] values) {
		float average = 0;
		float sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		average = sum / values.length;
		return average;
	}

	public static float getMin(float[] vals) {
		float min = vals[0];
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] < min) {
				min = vals[i];
			}
		}
		return min;
	}

	public static float getMax(float[] fs) {
		float max = fs[0];
		for (int i = 0; i < fs.length; i++) {
			if (fs[i] > max) {
				max = fs[i];
			}
		}
		return max;
	}

	public static int getMinIndex(float[] vals) {
		float min = vals[0];
		int index = 0;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] < min) {
				min = vals[i];
				index = i;
			}
		}
		return index;
	}

}
