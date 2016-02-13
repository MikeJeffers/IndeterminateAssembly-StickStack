package stickStacker_v04;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import toxi.geom.Rect;
import toxi.geom.Vec2D;

public class SearchGrid {
	StickStackMain p;
	
	ArrayList<Rect> cells = new ArrayList<Rect>();
	ArrayList<Rect> deadCells = new ArrayList<Rect>();
	ArrayList<Vec2D> cellCenters = new ArrayList<Vec2D>();
	ArrayList<Integer> cellStates = new ArrayList<Integer>();
	ArrayList<Float> cellValues = new ArrayList<Float>();
	Vec2D gridCenter = new Vec2D(0, 0);
	Vec2D minXY = new Vec2D(0, 0);
	Vec2D maxXY = new Vec2D(0, 0);

	int div;
	int rl;
	int wid;
	int len;
	float colDim;
	float rowDim;
	boolean drawable = false;

	SearchGrid(StickStackMain parent, int width, int height, int divisions) {
		p = parent;
		wid = width;
		len = height;
		div = divisions;
		rl = div;
		populateGrid(gridCenter);
		drawable = true;
	}

	void runRefine(float low, float hi) {
		drawable = false;
		refineGridAtValueRange(low, hi);
		subDivideGrid();
		if (!cellCenters.isEmpty()) {
			drawable = true;
		}
	}

	Vec2D getCenter() {
		Vec2D average = new Vec2D();
		float totalX = 0;
		float totalY = 0;
		for (Vec2D v : cellCenters) {
			totalX += v.x;
			totalY += v.y;
		}
		average = new Vec2D(totalX / cellCenters.size(), totalY
				/ cellCenters.size());
		return average;
	}

	void populateGrid(Vec2D offset) {
		gridCenter = offset;
		offset = new Vec2D(offset.x - (this.wid / 2.0f), offset.y
				- (this.len / 2.0f));
		
		cells.clear();
		cellCenters.clear();
		cellStates.clear();
		cellValues.clear();

		rl = div;
		colDim = wid / div;
		rowDim = len / div;
		for (int i = 0; i < rl; i++) {
			float cornerX = i * colDim;
			float x = i * colDim + (colDim / 2);
			for (int j = 0; j < rl; j++) {
				float cornerY = j * rowDim;
				float y = j * rowDim + (rowDim / 2);
				cells.add(new Rect(cornerX + offset.x, cornerY + offset.y,
						colDim, rowDim));
				cellCenters.add(new Vec2D(x + offset.x, y + offset.y));
				cellStates.add(0);
				cellValues.add(-1f);
			}
		}
		
		
		minXY = new Vec2D(gridCenter.x-(wid / 2.0f)+colDim, gridCenter.y-(len / 2.0f)+rowDim);
		maxXY = new Vec2D(gridCenter.x+(wid / 2.0f)-colDim, gridCenter.y+(len / 2.0f)-rowDim);
	}

	void subDivideGrid() {
		rowDim = rowDim / 2;
		colDim = colDim / 2;

		ArrayList<Vec2D> newCents = new ArrayList<Vec2D>();
		ArrayList<Rect> newCells = new ArrayList<Rect>();
		ArrayList<Integer> newState = new ArrayList<Integer>();

		for (int i = 0; i < cells.size(); i++) {
			Rect r = cells.get(i);

			Rect r1 = new Rect(r.x, r.y, r.width / 2, r.height / 2);
			newCents.add(r1.getCentroid());
			newCells.add(r1);
			newState.add(0);
			Rect r2 = new Rect(r.x + r.width / 2, r.y, r.width / 2,
					r.height / 2);
			newCents.add(r2.getCentroid());
			newCells.add(r2);
			newState.add(0);
			Rect r3 = new Rect(r.x + r.width / 2, r.y + r.height / 2,
					r.width / 2, r.height / 2);
			newCents.add(r3.getCentroid());
			newCells.add(r3);
			newState.add(0);
			Rect r4 = new Rect(r.x, r.y + r.height / 2, r.width / 2,
					r.height / 2);
			newCents.add(r4.getCentroid());
			newCells.add(r4);
			newState.add(0);
		}
		cells.clear();
		cellCenters.clear();
		cellStates.clear();
		cellValues.clear();

		cells = newCells;
		cellCenters = newCents;
		cellStates = newState;
		for (int i = 0; i < cells.size(); i++) {
			cellValues.add(-1f);
		}
		minXY = new Vec2D(gridCenter.x-(wid / 2.0f)+colDim, gridCenter.y-(len / 2.0f)+rowDim);
		maxXY = new Vec2D(gridCenter.x+(wid / 2.0f)-colDim, gridCenter.y+(len / 2.0f)-rowDim);
	}

	// TODO NEW REFINE AT SELECT RANGE OF DEPTH VALUE
	void refineGridAtValueRange(float low, float hi) {
		drawable = false;
		ArrayList<Vec2D> keepCents = new ArrayList<Vec2D>();
		ArrayList<Rect> keepCells = new ArrayList<Rect>();
		ArrayList<Integer> newState = new ArrayList<Integer>();
		float maxDist = (float) Math.sqrt(Math.pow(colDim, 2)
				+ Math.pow(rowDim, 2));
		for (int i = 0; i < cellCenters.size(); i++) {
			Vec2D u = cellCenters.get(i);
			ArrayList<Float> localVals = new ArrayList<Float>();
			for (int j = 0; j < cellCenters.size(); j++) {
				Vec2D v = cellCenters.get(j);
				if (u.distanceTo(v) <= maxDist) {
					localVals.add(cellValues.get(j));
				}
			}
			Float[] arr = new Float[localVals.size()];
			localVals.toArray(arr);
			int totalNeighbors = arr.length;
			int count = 0;
			boolean flagForSubdivision = false;
			for (int k = 0; k < arr.length; k++) {
				float v = arr[k];
				if (v < hi && v > low) {
					count++;
					flagForSubdivision = true;
				}
			}
			if (flagForSubdivision) {
				if (count >= totalNeighbors) {
					newState.add(1);//0 to only focus on edges; 1 for including interior
				} else if (count > 0) {
					newState.add(1);
				} else if (count == 0) {
					newState.add(0);
				}
			} else {
				newState.add(0);
			}
		}

		for (int i = 0; i < newState.size(); i++) {
			int state = newState.get(i);
			if (state == 0) {
				deadCells.add(cells.get(i));
			}
			if (state == 1) {
				keepCents.add(cellCenters.get(i));
				keepCells.add(cells.get(i));
			}
		}
		cells.clear();
		cellCenters.clear();
		cellStates.clear();
		cellValues.clear();

		cells = keepCells;
		cellCenters = keepCents;
		cellStates = newState;
		for (int i = 0; i < cells.size(); i++) {
			cellValues.add(-1f);
		}
	}

	ArrayList<Vec2D> getCellCentersWithValueRange(float low, float hi) {
		ArrayList<Vec2D> cellCenters_withValRange = new ArrayList<Vec2D>();
		for (int i = 0; i < cellValues.size(); i++) {
			float v = cellValues.get(i);
			if (v < hi && v > low) {
				cellCenters_withValRange.add(cellCenters.get(i));
			}
		}
		return cellCenters_withValRange;
	}

	public void drawCells() {
		if (drawable) {
			p.stroke(255);
			p.strokeWeight(0.75f);
			float[] dVals = new float[cellValues.size()];
			ArrayList<Float> clean = new ArrayList<Float>();
			for (int i = 0; i < dVals.length; i++) {
				float v = Math.abs(StickStackMain.DEPTH_EXPECT
						- cellValues.get(i));
				if (cellValues.get(i) > 1) {
					clean.add(v);
				}
				dVals[i] = v;
			}
			float minD = 0;
			float maxD = 510;
			if (!clean.isEmpty()) {
				float[] cleanVals = MJ_Math.toArrayFloats(clean);
				minD = MJ_Math.getMin(cleanVals);
				maxD = MJ_Math.getMax(cleanVals);
			} else {
				minD = MJ_Math.getMin(dVals);
				maxD = MJ_Math.getMax(dVals);
			}
			float scaleD = 510.0f / (maxD - minD + 1);
			for (int i = 0; i < cells.size(); i++) {
				float f = (dVals[i] - minD) * scaleD;
				f = Math.min(f, 509.9f);
				f = Math.max(f, 0.1f);
				if(f>255){
					p.fill(255, 255-(f-255), 0, 155);
				}else{
					p.fill(f, 255, 0, 155);
				}
				p.gfx.rect(cells.get(i));
				p.fill(255);
			}
			p.fill(255, 155);
			for (Rect cell : deadCells) {
				p.gfx.rect(cell);
			}
		}
	}

	public void setCellValues(ArrayList<Float> vals) {
		this.cellValues.clear();
		for (Float v : vals) {
			this.cellValues.add(v);
		}
		System.out.println("cellValues len" + cellValues.size());
		System.out.println("Vals len" + vals.size());
	}

	public List<Vec2D> convexSolve(ArrayList<Vec2D> points) {
		// @Precondition: points is not empty
		ArrayList<Vec2D> convexList = new ArrayList<Vec2D>();
		ArrayList<Vec2D> ptsCopy = new ArrayList<Vec2D>();
		TreeSet<Integer> checked = new TreeSet<Integer>();
		for (Vec2D v : points) {
			ptsCopy.add(v.copy());
		}
		// Find minX pt
		Vec2D minX = ptsCopy.get(0);
		for (int i = 0; i < ptsCopy.size(); i++) {
			Vec2D test = ptsCopy.get(i);
			if (minX.x > test.x) {
				minX = test;
			}
		}
		int count = 0;
		int current = 0;
		Vec2D next = new Vec2D();
		convexList.add(minX);
		Vec2D minCopy = minX.copy();
		Vec2D prev = new Vec2D();
		float minRotation = 3600f;
		for (int i = 0; i < ptsCopy.size(); i++) {
			Vec2D b = ptsCopy.get(i).copy();
			Vec2D diff = minCopy.sub(b);
			diff = diff.normalize();
			next = diff;
			float rot = Vec2D.Y_AXIS.angleBetween(next, true);
			float value = (float) (Math.toDegrees(rot));
			if (value + 0.1f < minRotation) {
				minRotation = value;
				prev = next;
				current = i;
			}
		}
		current = 0;
		while (count < cells.size()) {
			Vec2D keep = new Vec2D();
			Vec2D keepDiff = new Vec2D();
			prev = prev.normalize();
			Vec2D a = ptsCopy.get(current).copy();
			float mostLeftRotation = 3600f;
			for (int i = 0; i < ptsCopy.size(); i++) {
				if (checked.contains(i)) {
					continue;
				}
				Vec2D b = ptsCopy.get(i).copy();
				Vec2D diff = a.sub(b);
				diff = diff.normalize();
				next = diff;
				float rot = next.angleBetween(prev, true);
				float value = (float) (Math.toDegrees(rot));
				float dist = a.distanceTo(b);
				if (value + 0.1f < mostLeftRotation && dist > 0.01f) {
					mostLeftRotation = value;
					keep = b;
					keepDiff = next;
					current = i;
				}
			}
			if (keep.equalsWithTolerance(minX, 0.01f)) {
				break;
			} else {
				checked.add(current);
				convexList.add(keep);
				prev = keepDiff;
			}
			count++;
		}
		return convexList;
	}

	SearchGrid copy() {
		SearchGrid theCopy = new SearchGrid(p, wid, len, div);
		theCopy.gridCenter = this.gridCenter.copy();
		theCopy.cellCenters = this.getCellCenters();
		theCopy.cells = this.getCells();
		theCopy.cellStates = this.getCellStates();
		theCopy.cellValues = this.getCellValues();
		return theCopy;

	}

	// GETTERS

	ArrayList<Vec2D> getCellCenters() {
		return MJ_Math.copyList(cellCenters);
	}

	ArrayList<Rect> getCells() {
		return MJ_Math.copyList(cells);
	}

	ArrayList<Integer> getCellStates() {
		return MJ_Math.copyList(cellStates);
	}

	ArrayList<Float> getCellValues() {
		return MJ_Math.copyList(cellValues);
	}

	// SETTERS
	void setCellStates(ArrayList<Integer> newStates) {
		cellStates = MJ_Math.copyList(newStates);
	}

}
