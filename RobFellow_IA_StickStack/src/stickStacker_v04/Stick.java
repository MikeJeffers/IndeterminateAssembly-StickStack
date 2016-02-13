package stickStacker_v04;

import java.util.ArrayList;
import processing.core.PApplet;
import toxi.geom.*;

public class Stick {

	StickStackMain p;
	Vec2D[] endPts = new Vec2D[2];
	Vec2D[] endPtsWtol = new Vec2D[2];
	Vec2D gripPt = new Vec2D();
	float gripPosFactor = 0.5f;
	Line2D line;
	Line2D centerLine;
	Line2D[] profileLines = new Line2D[4];
	Line2D[] gripperProfile = new Line2D[4];
	Vec2D[] quads = new Vec2D[4];
	int id;
	float length;
	float w;
	float tol;
	int level;
	boolean buildSuccessflag = false;
	boolean scanSuccessflag = false;

	public Stick(StickStackMain parent, int ID, float lengthOfStick,
			float gripLocFac) {
		p = parent;
		id = ID;
		length = lengthOfStick;
		level = 0;

		gripPosFactor = gripLocFac;

		w = StickStackMain.STICK_DIM;
		tol = p.tol;
	}

	void buildAlgorithm1() {
		PApplet.println("Algo 1 init");
		if (pickStart()) {
			PApplet.println("pick start success");
			this.clearRestParams();
			if (fillRestParameters()) {
				PApplet.println("params prepped for final check");
				attemptBuild();
			}
		} else {
			PApplet.println("pick start FAILED?!");
			randomPlacementOverride();
		}
	}

	boolean pickStart() {
		int onLevel = p.maxLevel;
		ArrayList<Stick> sticksAtLevel = this.checkLevel(onLevel);
		while (onLevel > -1) {
			PApplet.println("checking level = " + onLevel + "with "
					+ sticksAtLevel.size() + " sticks found");
			if (solveForBuild(sticksAtLevel)) {
				PApplet.println("Solved for level" + onLevel);
				return true;
			}
			onLevel--;
			sticksAtLevel = this.checkLevel(onLevel);
		}
		return false;
	}

	boolean solveForBuild(ArrayList<Stick> sticksAtLevel) {
		if (sticksAtLevel.size() > 1) {
			ArrayList<Vec2D[]> pairsToTest = new ArrayList<Vec2D[]>();
			for (int i = 0; i < sticksAtLevel.size(); i++) {
				PApplet.print(" i=" + i + " ");
				Stick s = sticksAtLevel.get(i);
				for (int j = 0; j < sticksAtLevel.size(); j++) {
					PApplet.print(" j=" + j + " ");
					if (i == j) {
						PApplet.println();
						continue;
					}
					Stick t = sticksAtLevel.get(j);
					float minDist = MJ_Math.minDistOfLines(s.centerLine,
							t.centerLine, 25);
					PApplet.print(" minDist=" + minDist + " ");
					if ((this.length - (tol * 5)) > minDist) {
						PApplet.print(" minDist < " + this.length + "! yay! ");
						Vec2D[] pair = new Vec2D[2];
						float maxDist = -1f;
						boolean found = false;
						int res = 50;
						for (int k = 0; k < res; k++) {
							float f = k * (s.centerLine.copy().scale(0.85f).getLength() / res);
							Vec2D ab = s.centerLine.getDirection();
							ab = ab.getNormalizedTo(f);
							Vec2D start = s.centerLine.a.add(ab);//TODO UPDATE THIS ONCE CONFIRMED ON STICKTEST
							Vec2D end = t.centerLine.copy().scale(0.85f).closestPointTo(start);
							float d = start.distanceTo(end);
							if (d > maxDist&&(this.length - (tol * 5))>d) {
								// now test viability
								endPts = solveFor2PtsAndLength(start, end,
										this.length);
								if (fillRestParameters()) {
									if (attemptBuildTest()) {
										PApplet.print(" found successful build location! ");
										found = true;
										maxDist = d;
										pair[0] = start;
										pair[1] = end;
									}
								}
								this.clearRestParams();
							}
						}
						if (found) {
							PApplet.print(" Adding Pair to list! ");
							pairsToTest.add(solveFor2PtsAndLength(pair[0],
									pair[1], this.length));
						}
					}
					PApplet.println();
				}
			}
			ArrayList<Integer> successfulBuilds = new ArrayList<Integer>();
			int index = 0;
			for (Vec2D[] pair : pairsToTest) {
				endPts = pair;
				if (fillRestParameters()) {
					if (attemptBuildTest()) {
						successfulBuilds.add(index);
					}
				}
				this.clearRestParams();
				index++;
			}
			PApplet.println("num of found locations: " + index);
			if (successfulBuilds.size() > 0) {
				int rand = (int) Math.round(Math.random()
						* (successfulBuilds.size() - 1));
				PApplet.println("num of successful build locations: "
						+ successfulBuilds.size());
				PApplet.println("Choosing #" + rand);
				endPts = pairsToTest.get(successfulBuilds.get(rand));
				return true;
			}
			return false;
		}
		return false;
	}

	boolean attemptBuildTest() {
		// GIVEN CURRENT ENDPTS, CHECK IF POSSIBLE AND ON WHAT LEVEL
		boolean isSuccess = false;
		boolean isFailure = false;
		boolean flag = true;
		this.level = 0;

		ArrayList<Stick> currentLevel = checkLevel(this.level);
		ArrayList<Stick> levelBelow = checkBelow(this.level);
		ArrayList<Stick> levelsAtAndAbove = atAndAbove(this.level);
		while (flag) {
			isSuccess = false;
			isFailure = false;
			levelBelow.clear();
			levelsAtAndAbove.clear();
			currentLevel.clear();

			levelBelow = checkBelow(this.level);
			currentLevel = checkLevel(this.level);
			levelsAtAndAbove = atAndAbove(this.level);
			if (this.level > 0) {
				int numberBelow = 0;
				ArrayList<Vec2D> buildPts = new ArrayList<Vec2D>();
				for (Stick s : levelBelow) {
					if (this.isValidStack(this, s)) {
						numberBelow++;
						Vec2D intPt = getIntersection(this.centerLine,
								s.centerLine);
						if (intPt != null) {
							buildPts.add(intPt);
						}
					}
				}
				if (numberBelow > 1) {
					if (isValidBuild(buildPts, this.centerLine)) {
						isSuccess = true;
					}
				}
			} else {
				isSuccess = true;
			}
			for (Stick s : levelsAtAndAbove) {
				if (isIntersection(this, s)) {
					isFailure = true;
				}
			}
			if (this.level > p.maxLevel) {
				return false;
			} else if (isSuccess && !isFailure) {
				return true;
			}
			this.level++;
		}
		return false;
	}

	Vec2D[] solveFor2PtsAndLength(Vec2D a, Vec2D b, float len) {
		Vec2D mid = new Vec2D((a.x + b.x) / 2.0f, (a.y + b.y) / 2.0f);
		Vec2D[] ends = new Vec2D[2];
		Vec2D diff = a.sub(b);
		diff = diff.getNormalizedTo(len / 2.0f);
		ends[0] = mid.copy().sub(diff.copy());
		ends[1] = mid.copy().add(diff.copy());
		return ends;
	}

	void randomPlacementOverride() {
		// try random Stick location based on input length
		PApplet.println("Random placement Override");
		randomStart();
		randomPlaceEnd();
		if (fillRestParameters()) {
			attemptBuild();
		}
	}

	void manualOverride() {
		if (fillRestParameters()) {
			attemptBuild();
		}
	}

	void randomStart() {
		endPts[0] = new Vec2D(p.random(p.BZ_Xdom[0] + (15 * tol), p.BZ_Xdom[1]
				- (15 * tol)), p.random(p.BZ_Ydom[0] + (15 * tol), p.BZ_Ydom[1]
				- (15 * tol)));
	}

	boolean checkBoundsWithTol(Vec2D v) {
		if (v.x < p.BZ_Xdom[1] - (15 * tol) && v.x > p.BZ_Xdom[0] + (15 * tol)) {
			if (v.y < p.BZ_Ydom[1] - (15 * tol)
					&& v.y > p.BZ_Ydom[0] + (15 * tol)) {
				return true;
			}
		}
		return false;
	}

	boolean checkBounds(Vec2D v) {
		if (v.x < p.BZ_Xdom[1] - tol && v.x > p.BZ_Xdom[0] + tol) {
			if (v.y < p.BZ_Ydom[1] - tol && v.y > p.BZ_Ydom[0] + tol) {
				return true;
			}
		}
		return false;
	}

	void randomPlaceEnd() {
		boolean isValidPt = false;
		Vec2D test = new Vec2D();
		while (!isValidPt) {
			Vec2D randVec = Vec2D.randomVector();
			randVec = randVec.getNormalizedTo(length);
			test = endPts[0].add(randVec);
			isValidPt = checkBoundsWithTol(test);
		}
		endPts[1] = test;
	}

	boolean fillRestParameters() {
		// THIS METHOD ENSURES ALL PARAMETERS ARE SET AND POPULATES REST
		if (endPts != null) {
			// normalize directionality to be +y by flipping ends
			Vec2D temp = endPts[0];
			if (endPts[1].y < endPts[0].y) {
				endPts[0] = endPts[1];
				endPts[1] = temp;
			}
			for (Vec2D pt : endPts) {
				if (!checkBounds(pt)) {
					return false;
				}
			}
			this.length = endPts[0].distanceTo(endPts[1]);
			this.line = new Line2D(endPts[0], endPts[1]);
			makeProfileLines();
			makeCenterLine();
			calcGripLoc();
			makeGripperRect();
			makeQuadVerts();
			return true;
		} else {
			return false;
		}
	}

	void clearRestParams() {
		this.line = null;
		this.centerLine = null;
		this.profileLines = new Line2D[4];
		this.gripperProfile = new Line2D[4];
		this.quads = new Vec2D[4];
	}

	void calcGripLoc() {
		Vec2D diff = endPts[0].sub(endPts[1]);
		diff = diff.normalizeTo(gripPosFactor * this.length);
		this.gripPt = endPts[0].sub(diff);

	}

	void makeCenterLine() {
		Vec2D diff = endPts[0].sub(endPts[1]);
		diff = diff.normalizeTo((this.w / 2.0f) + tol);
		this.centerLine = new Line2D(endPts[0].sub(diff), endPts[1].add(diff));
		endPtsWtol[0] = this.centerLine.a;
		endPtsWtol[1] = this.centerLine.b;
	}

	void makeGripperRect() {
		Vec2D normal = this.line.getNormal().getNormalizedTo(
				(StickStackMain.TOOL_DIM / 1.55f) + (3*tol));
		Vec2D tan = this.line.getDirection().getNormalizedTo(
				StickStackMain.TOOL_DIM + (2*tol));
		Vec2D[] verts = new Vec2D[4];
		verts[0] = gripPt.add(normal).add(tan);
		verts[1] = gripPt.add(normal).sub(tan);
		verts[2] = gripPt.sub(normal).sub(tan);
		verts[3] = gripPt.sub(normal).add(tan);

		for (int i = 0; i < verts.length; i++) {
			this.gripperProfile[i] = new Line2D(verts[i], verts[(i + 1)
					% verts.length]);
		}
	}

	void makeProfileLines() {
		// This is quad with tolerance and line2D for intersections
		Vec2D normal = this.line.getNormal().getNormalizedTo(
				(this.w / 2.0f) + (tol * 1.5f));

		Vec2D[] verts = new Vec2D[4];
		verts[0] = endPts[0].add(normal);
		verts[1] = endPts[1].add(normal);
		verts[2] = endPts[1].sub(normal);
		verts[3] = endPts[0].sub(normal);
		for (int i = 0; i < verts.length; i++) {
			this.profileLines[i] = new Line2D(verts[i], verts[(i + 1)
					% verts.length]);
		}
	}

	void makeQuadVerts() {
		// this is profile line verts for drawing only!
		Vec2D normal = this.line.getNormal().getNormalizedTo(this.w / 2.0f);

		Vec2D[] v = new Vec2D[4];
		v[0] = endPts[0].add(normal);
		v[1] = endPts[1].add(normal);
		v[2] = endPts[1].sub(normal);
		v[3] = endPts[0].sub(normal);
		this.quads = v;
	}

	void attemptBuild() {
		// GIVEN CURRENT ENDPTS, CHECK IF POSSIBLE AND ON WHAT LEVEL
		boolean isSuccess = false;
		boolean isFailure = false;
		boolean flag = true;
		this.level = 0;

		ArrayList<Stick> currentLevel = checkLevel(this.level);
		ArrayList<Stick> levelBelow = checkBelow(this.level);
		ArrayList<Stick> levelsAtAndAbove = atAndAbove(this.level);
		while (flag) {
			isSuccess = false;
			isFailure = false;
			levelBelow.clear();
			levelsAtAndAbove.clear();
			currentLevel.clear();

			levelBelow = checkBelow(this.level);
			currentLevel = checkLevel(this.level);
			levelsAtAndAbove = atAndAbove(this.level);
			if (this.level > 0) {
				int numberBelow = 0;
				ArrayList<Vec2D> buildPts = new ArrayList<Vec2D>();
				for (Stick s : levelBelow) {
					if (this.isValidStack(this, s)) {
						numberBelow++;
						Vec2D intPt = getIntersection(this.centerLine,
								s.centerLine);
						if (intPt != null) {
							buildPts.add(intPt);
						}
					}
				}
				if (numberBelow > 1) {
					if (isValidBuild(buildPts, this.centerLine)) {
						isSuccess = true;
					}
				}
			} else {
				isSuccess = true;
			}
			for (Stick s : levelsAtAndAbove) {
				if (isIntersection(this, s)) {
					isFailure = true;
				}
			}

			if (this.level > p.maxLevel) {
				p.stroke(255, 0, 0);
				p.failures.add(this);
				PApplet.println("(FinalCheck)Could not find suitable build for stickPos");
				break;
			} else if (isSuccess && !isFailure) {
				this.buildSuccessflag = true;
				p.currentBuild.add(this);
				PApplet.println("(FinalCheck)Build Success!");
				break;
			} else if (isFailure) {
				p.failures.add(this);
			}
			this.level++;
		}
	}

	ArrayList<Stick> checkLevel(int atLevel) {
		int levelToCheck = atLevel;
		ArrayList<Stick> levelsToCollide = new ArrayList<Stick>();
		for (Stick s : p.currentBuild) {
			if (s.level == levelToCheck) {
				levelsToCollide.add(s);
			}
		}
		return levelsToCollide;
	}

	ArrayList<Stick> checkAbove(int atLevel) {
		int levelToCheck = atLevel;
		ArrayList<Stick> levelsToCollide = new ArrayList<Stick>();
		for (Stick s : p.currentBuild) {
			if (s.level > levelToCheck) {
				levelsToCollide.add(s);
			}
		}
		return levelsToCollide;
	}

	ArrayList<Stick> atAndAbove(int atLevel) {
		int levelToCheck = atLevel;
		ArrayList<Stick> levelsToCollide = new ArrayList<Stick>();
		for (Stick s : p.currentBuild) {
			if (s.level >= levelToCheck) {
				levelsToCollide.add(s);
			}
		}
		return levelsToCollide;
	}

	ArrayList<Stick> checkBelow(int atLevel) {
		int levelToCheck = atLevel - 1;
		ArrayList<Stick> levelBelow = new ArrayList<Stick>();
		for (Stick s : p.currentBuild) {
			if (s.level == levelToCheck) {
				levelBelow.add(s);
			}
		}
		return levelBelow;
	}

	boolean isValidStack(Stick a, Stick b) {
		if (checkIntersection(a.centerLine, b.centerLine)) {
			return true;
		}
		return false;
	}

	boolean isIntersection(Stick a, Stick b) {
		for (int i = 0; i < a.profileLines.length; i++) {
			for (int j = 0; j < b.profileLines.length; j++) {
				if (checkIntersection(a.profileLines[i], b.profileLines[j])) {
					return true;
				} else if (checkIntersection(a.gripperProfile[i],
						b.profileLines[j])) {
					return true;
				}
			}
		}
		return false;
	}

	boolean checkIntersection(Line2D a, Line2D b) {
		Line2D.LineIntersection inters = a.intersectLine(b);
		if (inters.getType() == Line2D.LineIntersection.Type.INTERSECTING) {
			Vec2D intersPt = inters.getPos();
			Vec2D check = a.closestPointTo(intersPt);
			if (check.distanceTo(intersPt) < tol) {
				return true;
			}
			return false;
		}
		return false;
	}

	Vec2D getIntersection(Line2D a, Line2D b) {
		Line2D.LineIntersection inters = a.intersectLine(b);
		if (inters.getType() == Line2D.LineIntersection.Type.INTERSECTING) {
			Vec2D intersPt = inters.getPos();
			Vec2D check = a.closestPointTo(intersPt);
			if (check.distanceTo(intersPt) < tol) {
				return intersPt;
			}
			return null;
		}
		return null;
	}

	boolean isValidBuild(ArrayList<Vec2D> pointsOfBearing, Line2D StickCLine) {
		// a valid build requires that at least two points exist, each on
		// opposing sides of the midpt beyond some tolerance
		boolean[] bothSides = { false, false };
		float vecTolerance = 0.001f;
		Vec2D tan = StickCLine.getDirection().normalize();
		Vec2D midPt = StickCLine.getMidPoint();
		for (Vec2D v : pointsOfBearing) {
			Vec2D onLine = StickCLine.closestPointTo(v);
			Vec2D dir = midPt.sub(onLine);
			dir = dir.normalize();
			if (dir.equalsWithTolerance(tan, vecTolerance)) {
				bothSides[0] = true;
			} else if (dir.equalsWithTolerance(tan.getInverted(), vecTolerance)) {
				bothSides[1] = true;
			}
		}

		// the check
		for (int i = 0; i < bothSides.length; i++) {
			if (!bothSides[i]) {
				return false;
			}
		}
		return true;
	}

	// ////SETTERS

	public void reviseEndPts(Vec2D[] newEndPts) {
		this.endPts = solveFor2PtsAndLength(newEndPts[0], newEndPts[1],
				this.length);
		this.clearRestParams();
		this.buildSuccessflag=false;
		if (this.fillRestParameters()) {
			attemptBuild();
			if (this.buildSuccessflag) {
				p.commsText += "Local: StickRevision Successful for StickID = "
						+ this.id + "\n";
			} else {
				System.out.println("__stick revision failed on rebuild-attempt!");
				this.endPts = null;
				p.commsText += "ERROR: Stick revision failed on rebuild-attempt!" + "\n";
				p.SYSTEM_FAILURE = true;
			}
		} else {
			System.out.println("__stick revision failed on params!");
			p.commsText += "ERROR: StickRevision FAILURE for StickID = "
					+ this.id + "\n";
			this.endPts = null;
			p.SYSTEM_FAILURE = true;
		}
	}

	// //GETTERS

	Stick copy(Stick toCopy) {
		Stick copy = new Stick(toCopy.p, -1, toCopy.length,
				toCopy.gripPosFactor);
		copy.w = StickStackMain.STICK_DIM;
		copy.level = toCopy.level;
		copy.tol = p.tol;
		copy.endPts[0] = toCopy.endPts[0].copy();
		copy.endPts[1] = toCopy.endPts[1].copy();
		copy.fillRestParameters();
		return copy;
	}

	int getLevel() {
		if (buildSuccessflag) {
			return this.level;
		}
		return -1;// if not ready, level may be unsolved
	}

	float getRotation() {
		float rot = 0;
		if (buildSuccessflag) {
			Vec2D diff = endPts[0].sub(endPts[1]);
			rot = Vec2D.Y_AXIS.angleBetween(diff, true);
			float cross = Vec2D.Y_AXIS.cross(diff.normalize());
			int sign = (int) Math.signum(cross);
			rot = sign * rot;
			if (rot == Float.NaN) {
				return 0;
			} else {
				rot = (float) Math.toDegrees(rot);
				if (PApplet.abs(rot) > 90) {
					rot = (180 - PApplet.abs(rot)) * (sign * -1);
					return rot;
				}
			}
		}
		return rot;
	}

	Vec2D getGripPt() {
		if (buildSuccessflag) {
			return this.gripPt;
		}
		return null;// if not ready, endpts may be null themselves
	}

	Vec2D getMidPt() {
		if (buildSuccessflag) {
			return this.line.getMidPoint();
		}
		return null;// if not ready, endpts may be null themselves
	}

	void drawGhostStick() {
		if (endPts != null && profileLines != null) {
			float levelFactor = (this.getLevel() + 1.0f) / (p.maxLevel + 1.0f);
			p.fill(255 * levelFactor, 135);
			p.stroke(155, 200);
			p.strokeWeight(1);
			Vec2D[] v = this.quads;
			p.quad(v[0].x, v[0].y, v[1].x, v[1].y, v[2].x, v[2].y, v[3].x,
					v[3].y);
		}
	}

	void drawStick() {
		if (buildSuccessflag) {
			if (endPts != null && profileLines != null) {
				float levelFactor = (this.getLevel() + 1.0f)
						/ (p.maxLevel + 1.0f);
				p.fill(255 * levelFactor * 0.35f, 135);
				p.stroke(255 * levelFactor, 155, 255 / levelFactor);
				p.strokeWeight(1.5f + (0.75f * this.getLevel()));
				Vec2D[] v = this.quads;
				p.quad(v[0].x, v[0].y, v[1].x, v[1].y, v[2].x, v[2].y, v[3].x,
						v[3].y);
				if (!scanSuccessflag) {
					p.stroke(155);
					p.strokeWeight(0.5f);
					for (Line2D g : this.gripperProfile) {
						p.gfx.line(g);
					}
				} else {
					p.stroke(155);
					p.strokeWeight(0.85f);
					p.crossHairs(this.gripPt, w * 0.75f);
				}
				p.stroke(255);
				p.strokeWeight(1.5f);
				p.gfx.line(centerLine);
				Vec2D midpt = this.line.getMidPoint();
				p.fill(255);

				p.text(this.level, midpt.x, midpt.y);
			}
		}
	}

	void drawStick3D(ViewPort_3D q) {
		if (buildSuccessflag) {
			q.pushMatrix();
			if (scanSuccessflag) {
				q.fill(175, 255, 175, 95);
			} else {
				q.fill(255, 155, 155, 105);
			}

			Vec2D mid2D = this.getMidPt();
			float zVal = (this.level * StickStackMain.STICK_DIM)
					+ (StickStackMain.STICK_DIM / 2.0f);
			Vec3D cp = new Vec3D(mid2D.x, mid2D.y, zVal);
			float rotation = (float) (Math.toRadians(this.getRotation()) + Math.PI / 2.0f);

			q.translate(cp.x, cp.y, cp.z);
			q.rotate(rotation);
			q.box(this.length, StickStackMain.STICK_DIM,
					StickStackMain.STICK_DIM);
			q.popMatrix();
		}
	}

}
