package stickStacker_v04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import processing.core.*;
import toxi.processing.ToxiclibsSupport;
import toxi.geom.*;

public class StickStackMain extends PApplet {
	private static final long serialVersionUID = 1L;

	/**
	 * TODO updated 5/8/2015: all previous goals implemented Test success and
	 * calibrate against real trials.
	 **/

	// DEPTH SCANNER GRID
	ArrayList<Vec2D> gridPts = new ArrayList<Vec2D>();
	int currentCell = 0;

	float[] iniPos = new float[3];
	Vec2D[] tempEndPts = new Vec2D[2];
	Line2D regressLine;
	Vec2D foundEndDraw = new Vec2D(0, 0);

	int cellDim = 50;
	int level = 0;

	SearchGrid grid;

	// STICK DATA AND VIRTUAL MODEL
	Vector<Stick> currentBuild = new Vector<Stick>();
	ArrayList<Stick> failures = new ArrayList<Stick>();
	ArrayList<Boolean> clicks = new ArrayList<Boolean>();
	boolean flagToAdd = false;
	int count = 0;
	int maxLevel = 6;
	float tol = 5;
	// Global Coordinates and constants in Millimeters
	float[] BZ_Xdom = new float[] { 900, 2000 };// old 800-2000
	float[] BZ_Ydom = new float[] { -125, 700 };// -170, 750
	public static final float STICK_DIM = 38.1f;
	public static final float BZ_MIN_Z = -325;// tool0 TCP +with
												// Gripper+material
	public static final float TOOL_DIM = 100.0f;
	public static final float LIMIT_SWITCH_POS_Z = 11.9f;
	public static final float SEARCH_Z = 241;
	public static final float TCP_SENSOR_Z = 231;
	public static final float DEPTH_EXPECT = SEARCH_Z + TCP_SENSOR_Z;

	// DRAWING
	public ToxiclibsSupport gfx;
	ViewPort_3D vis;
	ViewPortFrame vFrame;
	PImage frame3Dview;
	Vec2D trans = new Vec2D(-BZ_Xdom[0], -BZ_Ydom[0]);

	float scaleX = 640.0f / (BZ_Xdom[1] - BZ_Xdom[0]);
	float scaleY = 480.0f / (BZ_Ydom[1] - BZ_Ydom[0]);
	float uniformScale = min(scaleX, scaleY);

	boolean showDGrid = true;
	// for buildCommand
	float[] XYZ_J6 = new float[4];

	// //Serial vars
	SerialComms serial;

	// Thread classes
	SerialThread SR;
	CameraThread CU;

	PImage camImage;

	// TCP/IP vars

	String hostName = "128.2.109.20";
	int portNumber = 1025;
	public ServerSocket servSock;
	InputStream in;
	OutputStream out;
	String outMessage;
	String inMessage;

	BufferedReader inReader;

	boolean connected = false;
	boolean receivedMessage = false;
	boolean newMessage = false;

	// STAGING FOR PROCEDURAL ORDERS
	boolean stage0pickStick = true;
	boolean stage1buildStick = false;
	boolean stage2ScanBuild = false;
	boolean stage3GridScan = false;
	int sensorModeOrder = 0;
	int scanCounter = 0;
	int stageOrder = 0;

	boolean hasStick = false;// true when pick stick completed
	boolean buildPosReady = false;// True when build location determined
	boolean isStickFound = false;
	boolean waitMessageFlag = false;
	int cStickID = -1;
	int cStickIndexInBuild = -1;
	Stick cStick, ghost;
	int resolution = 5;
	float[] stickScanVerify = new float[resolution];

	// GUI stuff
	String commsText = "";
	String sensorText = "";
	String statusText = "";

	// TOOL CENTER POINT STATE
	String currentTCP = "Grip";
	// "Cam", "Sensor", "Grip"
	float[] TCPoffset = new float[] { 0, 0 };

	Vec2D currentPos = new Vec2D(0, 0);
	Vec2D nextPos = new Vec2D(0, 0);
	Vec2D TCPoff = new Vec2D(0, 0);
	String SENSOR = "IR";
	// "IR" -or- "LRF"
	float IR_yOff = 185.88f;
	float LRF_yOff = 205.88f;

	boolean toggle = true;
	String startTime = "";
	public boolean SYSTEM_FAILURE = false;

	public void setup() {
		size(1280, 960, JAVA2D);
		background(0);
		smooth();
		textSize(12);
		frameRate(4);
		startTime = Integer.toString(year()) + "_" + Integer.toString(month())
				+ "_" + Integer.toString(day()) + "_"
				+ Integer.toString(hour()) + "_" + Integer.toString(minute())
				+ "_";

		gfx = new ToxiclibsSupport(this);
		serial = new SerialComms();
		SR = new SerialThread(serial);
		SR.start();

		CU = new CameraThread(this);
		CU.start();

		vis = new ViewPort_3D(this);
		vFrame = new ViewPortFrame("3D view", vis, 0, 0, 640, 480);
		waiter();
		try {
			setupClient();
		} catch (IOException e) {
		}
	}

	public synchronized void waiter() {
		try {
			wait(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	void pickStick() {
		if (inMessage.equalsIgnoreCase("Ready To Listen") && stageOrder == 0) {
			outMessage = "PICKSTICK";
			stageOrder = 1;
			waitMessageFlag = true;
		} else if (stageOrder == 1 && !inMessage.contains("MM")) {
			if (waitMessageFlag) {
				commsText += "Local: Waiting for StickMeasure" + "\n";
				waitMessageFlag = false;
			}
		} else if (stageOrder == 1 && inMessage.contains("MM")) {
			int index = inMessage.indexOf("MM");
			if (index > -1) {
				String valStr = inMessage.substring(0, index);
				String[] sides = valStr.split(",");
				float sideA = Float.parseFloat(sides[1]) - LIMIT_SWITCH_POS_Z;
				float sideB = Float.parseFloat(sides[0]) - LIMIT_SWITCH_POS_Z;
				float stickLength = sideA + sideB;
				println(stickLength + "=" + sideA + "A +" + sideB + "B");
				float gripLocFactor = sideB / stickLength;
				println("Grip location factor: " + gripLocFactor);
				addStick(stickLength, gripLocFactor);
				hasStick = true;
				ghost = null;
				foundEndDraw = new Vec2D(0, 0);
			}
			outMessage = "Acknowledge: Stick Acquired";
			stage0pickStick = false;
			stage1buildStick = true;
			stage2ScanBuild = false;
			stage3GridScan = false;
			sensorModeOrder = 0;
			scanCounter = 0;
			stageOrder = 0;
			level = 0;

		} else if (inMessage.equalsIgnoreCase("Timed Out")) {
			outMessage = "CycleComms";
		}
	}

	void buildStick() {
		if (hasStick && buildPosReady) {
			if (inMessage.equalsIgnoreCase("Ready To Listen")
					&& stageOrder == 0) {
				sendDataArray(XYZ_J6);
				nextPos = new Vec2D(XYZ_J6[0], XYZ_J6[1]);
				SR.resetSerial();
				stageOrder = 1;
				waitMessageFlag = true;
			} else if (stageOrder == 1
					&& !inMessage.equalsIgnoreCase("BUILD SUCCESS")) {
				if (waitMessageFlag) {
					commsText += "Local: Waiting for BuildSuccess Flag" + "\n";
					waitMessageFlag = false;
				}
			} else if (stageOrder == 1
					&& inMessage.equalsIgnoreCase("BUILD SUCCESS")) {
				outMessage = "Acknowledge: Build Success";
				stage0pickStick = false;
				stage1buildStick = false;
				stage2ScanBuild = true;
				stageOrder = 0;
				hasStick = false;
				buildPosReady = false;
				isStickFound = false;
				XYZ_J6 = new float[4];
			} else if (inMessage.equalsIgnoreCase("Timed Out")) {
				outMessage = "CycleComms";
			}
		}
	}

	void scanStickVerify() {
		if (!isStickFound) {
			int resolution = 5;
			// GET STICK ID OF CURRENT STICK TO FIND/SCAN
			if (cStickID != -1 && cStickIndexInBuild == -1) {
				SR.resetSerial();
				cStickIndexInBuild = findStickIndexWithID(cStickID);
				cStick = currentBuild.get(cStickIndexInBuild);
			}// END GET STICK
			if (inMessage.equalsIgnoreCase("Ready To Listen")
					&& stageOrder == 0 && scanCounter < resolution) {
				float f = scanCounter
						* (cStick.centerLine.getLength() / (resolution - 1));
				Vec2D ab = cStick.centerLine.getDirection();
				ab = ab.getNormalizedTo(f);
				Vec2D a = cStick.centerLine.a.add(ab);
				float z = ((cStick.level + 1) * STICK_DIM) + BZ_MIN_Z
						+ SEARCH_Z;
				// TODO PERFORM TRANSLATION FOR TOOL0 VS LRF TCP!!!!
				float[] xyz = { a.x, a.y - TCPoffset[1], z };
				// -213.88 actual from tool0 tcp.y
				sendDataArray(xyz);
				nextPos = new Vec2D(xyz[0], xyz[1]);
				stageOrder = 1;
				waitMessageFlag = true;
			} else if (stageOrder == 0
					&& !inMessage.equalsIgnoreCase("Ready To Listen")) {
				if (inMessage.equalsIgnoreCase("INVALID COMMAND")) {
					outMessage = "ACKNOWLEDGE: CYCLE COMMS";
				}
			} else if (stageOrder == 1 && !inMessage.contains("CPOS")) {
				if (waitMessageFlag) {
					commsText += "Local: Waiting for MotionSuccess Flag" + "\n";
					waitMessageFlag = false;
				}
			} else if (stageOrder == 1 && inMessage.contains("CPOS")) {
				int index = inMessage.indexOf("CPOS");
				if (index > -1) {
					String valStr = inMessage.substring(0, index);
					float[] cpos = parsePosStr(valStr);
					currentPos = new Vec2D(cpos[0], cpos[1]);
					float depthReadOut = getSensorFeedback();
					sensorText += "StickDepthVerfiy: " + SENSOR + ": "
							+ depthReadOut + "mm\n";
					stickScanVerify[scanCounter] = depthReadOut;
					scanCounter++;
					outMessage = "Acknowledge: ScanVerification step#"
							+ scanCounter;
				}
				if (scanCounter < resolution) {
					stageOrder = 0;
				} else {
					boolean isScanGood = true;
					for (int i = 0; i < stickScanVerify.length; i++) {
						float result = stickScanVerify[i] - DEPTH_EXPECT;
						if (Math.abs(result) > tol * 3.1f) {
							// TODO THIS IS THE FLAG TO TRIGGER SCANGRID!!!
							isScanGood = false;
						}
					}
					currentBuild.get(cStickIndexInBuild).scanSuccessflag = isScanGood;
					isStickFound = currentBuild.get(cStickIndexInBuild).scanSuccessflag;
					stage0pickStick = isStickFound;
					stage1buildStick = false;
					stage2ScanBuild = false;
					stage3GridScan = !isStickFound;
					stageOrder = 0;
					sensorModeOrder = 0;
					scanCounter = 0;
					level = 0;
					currentCell = 0;
					hasStick = false;
					buildPosReady = false;
					isStickFound = currentBuild.get(cStickIndexInBuild).scanSuccessflag;
					if (isStickFound) {
						cStickID = -1;
						cStickIndexInBuild = -1;
						cStick = null;
					}
				}
			} else if (inMessage.equalsIgnoreCase("Timed Out")) {
				outMessage = "CycleComms";
			}
		}
	}

	void scanGrid() {
		if (!isStickFound) {
			if (inMessage.equalsIgnoreCase("Ready To Listen")
					&& stageOrder == 0 && scanCounter < 2) {
				Vec2D a = cStick.endPtsWtol[scanCounter];
				float z = ((cStick.level + 1) * STICK_DIM) + BZ_MIN_Z
						+ SEARCH_Z;
				float[] xyz = { a.x, a.y - TCPoffset[1], z };
				sendDataArray(xyz);
				nextPos = new Vec2D(xyz[0], xyz[1]);
				stageOrder = 1;
				waitMessageFlag = true;
			} else if (stageOrder == 1 && !inMessage.contains("CPOS")) {
				if (waitMessageFlag) {
					commsText += "Local: Waiting for MotionSuccess Flag" + "\n";
					waitMessageFlag = false;
				}
			} else if (stageOrder == 1 && inMessage.contains("CPOS")) {
				int index = inMessage.indexOf("CPOS");
				if (index > -1) {
					String valStr = inMessage.substring(0, index);
					float[] cpos = parsePosStr(valStr);
					currentPos = new Vec2D(cpos[0], cpos[1]);
					iniPos = cpos;
					grid = new SearchGrid(this, 135, 135, 5);
					grid.populateGrid(new Vec2D(iniPos[0], iniPos[1]));
					currentCell = 0;
					level = 0;
					outMessage = "Acknowledge: SearchGrid Initialized";
					stageOrder = 2;
				}
			} else if (stageOrder == 2
					&& !inMessage.equalsIgnoreCase("Ready To Listen")) {
				if (inMessage.equalsIgnoreCase("INVALID COMMAND")) {
					outMessage = "ACKNOWLEDGE: CYCLE COMMS";
				}
			} else if (inMessage.equalsIgnoreCase("Ready To Listen")
					&& stageOrder == 2) {
				if (currentCell < grid.cellCenters.size()) {
					Vec2D cellCent = grid.cellCenters.get(currentCell);
					float[] nXyz = { cellCent.x, cellCent.y, iniPos[2] };
					sendDataArray(nXyz);
					nextPos = new Vec2D(nXyz[0], nXyz[1]);
					stageOrder = 3;
					waitMessageFlag = true;
				} else if (currentCell >= grid.cellCenters.size()
						&& grid.cellCenters.size() > 1) {
					currentCell = 0;
					level++;
					SearchGrid gridCopy = grid.copy();
					if (level > 1) {
						Vec2D ave = computeEndPtEstimate(cStick.centerLine);
						if (ave == null) {
							commsText += "ERROR: Stick Not Found! ABORT!"+"\n";
							SYSTEM_FAILURE = true;
						}

						// GLOBAL TEMP_END_ptS
						tempEndPts[scanCounter] = new Vec2D(ave.x, ave.y
								+ TCPoffset[1]);
						foundEndDraw = new Vec2D(ave.x, ave.y + TCPoffset[1]);
						println(cStick.endPts[scanCounter] + " vs. "
								+ tempEndPts[scanCounter]);
						scanCounter++;
						if (scanCounter < 2) {
							stageOrder = 0;
						} else {
							println("Holyshit gridSearch Found the stick!");
							println("updating location in VM!");
							println("Now ReScan with LRF");
							grid.drawable = false;
							ghost = cStick.copy(cStick);
							//TODO this is new...
							currentBuild.remove(cStickIndexInBuild);
							cStick.reviseEndPts(tempEndPts);
							//currentBuild.get(cStickIndexInBuild).reviseEndPts(tempEndPts);
							stage0pickStick = false;
							stage1buildStick = false;
							stage2ScanBuild = true;
							stage3GridScan = false;
							stageOrder = 0;
							scanCounter = 0;
							sensorModeOrder = 0;
							level = 0;
							hasStick = false;
							buildPosReady = false;
							isStickFound = false;
						}
					} else {
						float tolFactor = lassoSearchGrid(gridCopy);
						grid.runRefine(DEPTH_EXPECT - (tol * tolFactor),
								DEPTH_EXPECT + (tol * tolFactor));
						if (grid.cells.size() < 1) {
							SYSTEM_FAILURE = true;
							println("ENDPT NOT FOUND! CATASTROPHIC FAILURE!");
							println("OR use camera CV to locate stick end..");
						}
						stageOrder = 2;
					}
					outMessage = "GRID LEVEL COMPLETION";
				}
			} else if (stageOrder == 3 && !inMessage.contains("CPOS")) {
				if (waitMessageFlag) {
					commsText += "Local: Waiting for MotionSuccess Flag" + "\n";
					waitMessageFlag = false;
				}
				if (inMessage.equalsIgnoreCase("INVALID COMMAND")) {
					outMessage = "ACKNOWLEDGE: CYCLE COMMS";
				}
			} else if (stageOrder == 3 && inMessage.contains("CPOS")) {
				int index = inMessage.indexOf("CPOS");
				if (index > -1) {
					String valStr = inMessage.substring(0, index);
					float[] cpos = parsePosStr(valStr);
					currentPos = new Vec2D(cpos[0], cpos[1]);
					float depthReadOut = getSensorFeedback();
					String dRead = Float.toString(depthReadOut);
					if (dRead.length() > 6) {
						dRead = dRead.substring(0, 6);
					}
					sensorText += "StickDepthVerfiy: " + SENSOR + ": " + dRead
							+ "mm\n";
					grid.cellValues.set(currentCell, depthReadOut);
					outMessage = "Acknowledge: cell#" + currentCell
							+ " scanned";
					currentCell++;
					stageOrder = 2;
				}
			} else if (inMessage.equalsIgnoreCase("Timed Out")) {
				outMessage = "CycleComms";
			}
		}
	}

	void switchSensorMode(String sensor) {
		if (inMessage.equalsIgnoreCase("Ready To Listen")
				&& sensorModeOrder == 0) {
			if (sensor.equalsIgnoreCase("LRF")) {
				outMessage = "TRUE";// set LRF to ON
			} else {
				outMessage = "FALSE";// set LRF to ON
			}
			sensorModeOrder = 1;
			waitMessageFlag = true;
		} else if (sensorModeOrder == 0
				&& !inMessage.equalsIgnoreCase("Ready To Listen")) {
			if (inMessage.equalsIgnoreCase("INVALID COMMAND")) {
				outMessage = "ACKNOWLEDGE: CYCLE COMMS";
			}
		} else if (sensorModeOrder == 1 && !inMessage.contains("isLRF")) {
			if (waitMessageFlag) {
				commsText += "Local: Waiting for SensorSwitch Success" + "\n";
				waitMessageFlag = false;
			}
		} else if (sensorModeOrder == 1 && inMessage.contains("isLRF")) {
			if (sensor.equalsIgnoreCase("LRF")) {
				SENSOR = "LRF";
				TCPoffset[1] = LRF_yOff;
				TCPoff = new Vec2D(0, LRF_yOff);
			} else {
				SENSOR = "IR";
				TCPoffset[1] = IR_yOff;
				TCPoff = new Vec2D(0, IR_yOff);
			}
			outMessage = "Acknowledge Sensor Config to " + SENSOR;
			sensorModeOrder = 0;
		}
	}

	public void draw() {
		background(0);
		fill(255);
		stroke(255);

		if (connected && !SYSTEM_FAILURE) {
			getInMessage();
			if (stage0pickStick) {
				currentTCP = "Grip";
				TCPoff = new Vec2D(0, 0);
				if (!SENSOR.equalsIgnoreCase("IR")) {
					switchSensorMode("IR");
				} else {
					pickStick();
				}
			} else if (stage1buildStick) {
				currentTCP = "Grip";
				TCPoff = new Vec2D(0, 0);
				buildStick();
			} else if (stage2ScanBuild) {
				currentTCP = "Sensor";
				if (!SENSOR.equalsIgnoreCase("LRF")) {
					switchSensorMode("LRF");
				} else {
					scanStickVerify();
				}
			} else if (stage3GridScan) {
				currentTCP = "Sensor";
				if (!SENSOR.equalsIgnoreCase("IR")) {
					switchSensorMode("IR");
				} else {
					scanGrid();
				}
			} else {
				outMessage = "CycleComms";
			}
			sendOutMessage();
		}

		// DRAWSTUFF

		// TODO FIX THE GODDAMN CAMERA
		drawCam(0, 480);

		String depthS = SR.getDepthValue();
		float dep = parseSensorData(depthS);
		if (dep == Float.NaN) {
			sensorText += SENSOR + ": ERROR\n";
		} else if (dep < 1 || dep > 9999) {
			sensorText += SENSOR + ": OutOfRange\n";
		} else {
			sensorText += SENSOR + ": " + dep + "mm\n";
		}

		pushMatrix();
		scale(uniformScale, uniformScale);
		translate(trans.x, trans.y);

		stroke(255);
		strokeWeight(1.5f);
		noFill();
		rect(BZ_Xdom[0], BZ_Ydom[0], BZ_Xdom[1] - BZ_Xdom[0], BZ_Ydom[1]
				- BZ_Ydom[0]);
		textSize(18);
		for (Stick s : currentBuild) {
			s.drawStick();
		}
		if (ghost != null) {
			ghost.drawGhostStick();
		}
		if (grid != null && grid.drawable) {
			translate(0, TCPoffset[1]);
			grid.drawCells();
			translate(0, -TCPoffset[1]);
		}
		if (!stage0pickStick) {
			drawTCP(currentPos, nextPos, currentTCP, SENSOR);
		}
		stroke(255, 255, 0, 200);
		strokeWeight(1.5f);
		crossHairs(foundEndDraw, STICK_DIM / 1.5f);
		stroke(255, 0, 255, 200);
		fill(50, 75);
		gfx.circle(foundEndDraw, STICK_DIM / 2);

		popMatrix();

		PImage temp = vis.get3DView();
		if (temp != null) {
			frame3Dview = temp;
		}
		strokeWeight(1);
		stroke(255);
		fill(0);
		if (frame3Dview != null) {
			rect(640, 480, frame3Dview.width, frame3Dview.height);
			image(frame3Dview, 640, 480);
		}

		fill(255);
		stroke(255);
		textSize(12);
		statusText = getStatus();
		commsText = limitLines(commsText);
		sensorText = limitLines(sensorText);

		text(commsText, (width / 2) + 25, 25);
		text(sensorText, width - 200, 25);
		text(statusText, (width / 2) + 25, (height / 2) + 25);

		if (frameCount % 2 == 0) {
			screenShot();
		}

		if (SYSTEM_FAILURE) {
			fill(255, 0, 0);
			stroke(255, 0, 0);
			textSize(24);
			String fail = "CATASTROPHIC FAILURE!";
			textAlign(CENTER, BOTTOM);
			text(fail, (640 / 2), (480 / 2));
			screenShot();
			this.exit();
		}
	}

	Vec2D computeEndPtEstimate(Line2D s) {
		Vec2D desiredPt = new Vec2D();
		ArrayList<Vec2D> ptsAtDepth = new ArrayList<Vec2D>();
		SearchGrid gridCopy = grid.copy();
		float rangeFactor = lassoSearchGrid(gridCopy);
		ptsAtDepth = grid.getCellCentersWithValueRange(DEPTH_EXPECT
				- (tol * rangeFactor), DEPTH_EXPECT + (tol * rangeFactor));
		println("pts to Solve for: " + ptsAtDepth.size() + "with tol at"
				+ rangeFactor);
		Line2D r1 = MJ_Math.linearRegressor(ptsAtDepth);
		Line2D r2 = MJ_Math.linearRegressorXY(ptsAtDepth);
		Line2D solution;
		double angle = Math.toDegrees(MJ_Math.findMinAngleOf2Lines(r1, r2));
		if (angle < 45) {
			if (r1.a.distanceTo(r2.a) < r1.a.distanceTo(r2.b)) {
				Vec2D start = new Line2D(r1.a, r2.a).getMidPoint();
				Vec2D end = new Line2D(r1.b, r2.b).getMidPoint();
				solution = new Line2D(start, end);
			} else {
				Vec2D start = new Line2D(r1.a, r2.b).getMidPoint();
				Vec2D end = new Line2D(r1.b, r2.a).getMidPoint();
				solution = new Line2D(start, end);
			}
		} else {
			double a1 = Math.toDegrees(MJ_Math.findMinAngleOf2Lines(r1, s));
			double a2 = Math.toDegrees(MJ_Math.findMinAngleOf2Lines(r2, s));
			if (a1 < a2) {
				solution = r1;
			} else {
				solution = r2;
			}
		}
		regressLine = solution;
		Vec2D c = grid.gridCenter;
		if (c.distanceTo(solution.a) < c.distanceTo(solution.b)) {
			desiredPt = solution.a;
		} else {
			desiredPt = solution.b;
		}
		return desiredPt;
	}

	boolean checkBounds(Vec2D v) {
		if (v.x < BZ_Xdom[1] - tol && v.x > BZ_Xdom[0] + tol) {
			if (v.y < BZ_Ydom[1] - tol && v.y > BZ_Ydom[0] + tol) {
				return true;
			}
		}
		return false;
	}

	float lassoSearchGrid(SearchGrid gridCopy) {
		float tolFactor = 0.75f;
		boolean edgeCell = false;
		while (gridCopy.cells.size() < 16 && tolFactor < 15 || !edgeCell) {
			tolFactor += 0.75f;
			gridCopy = grid.copy();
			println("Expanding search criteria: "
					+ (DEPTH_EXPECT - (tol * tolFactor)) + " to "
					+ (DEPTH_EXPECT + (tol * tolFactor)));
			gridCopy.runRefine(DEPTH_EXPECT - (tol * tolFactor), DEPTH_EXPECT
					+ (tol * tolFactor));
			Vec2D min = gridCopy.minXY;
			Vec2D max = gridCopy.maxXY;
			for (Vec2D v : gridCopy.cellCenters) {
				boolean checkV = v.x < min.x || v.x > max.x || v.y < min.y
						|| v.y > max.y;
				if (checkV) {
					edgeCell = true;
					break;
				}
			}
			println(gridCopy.cellCenters.size());
		}
		return tolFactor;
	}

	int findStickIndexWithID(int ID) {
		for (int i = 0; i < currentBuild.size(); i++) {
			Stick s = currentBuild.get(i);
			if (s.id == ID) {
				return i;
			}
		}
		return -1;
	}

	Stick findStickWithID(int ID) {
		for (Stick s : currentBuild) {
			if (s.id == ID) {
				return s;
			}
		}
		return null;
	}

	void addStick(float measuredLength, float gripLoc) {
		Stick a = new Stick(this, count, measuredLength, gripLoc);
		a.buildAlgorithm1();
		int attempts = 0;
		while (!a.buildSuccessflag && attempts < 100) {
			println("build failure, cycle stick build attempt#" + attempts);
			a = new Stick(this, count, measuredLength, gripLoc);
			a.buildAlgorithm1();
			attempts++;
		}
		cStickID = count;
		println(count);
		count++;

		Vec2D XY = a.getGripPt();
		float deltaJ6 = -1.0f * a.getRotation();// INVERT ANGLE FOR ROBOT
												// RELATIVE CONVERSION
		int level = a.getLevel();
		XYZ_J6[0] = XY.x;
		XYZ_J6[1] = XY.y;
		XYZ_J6[2] = (level * STICK_DIM) + BZ_MIN_Z + tol;
		XYZ_J6[3] = deltaJ6;
		print("Array prepped with: ");
		println(XYZ_J6);
		buildPosReady = true;
	}

	float[] parsePosStr(String s) {
		String cPos = s.replace("[", "");
		cPos = cPos.replace("]", "");
		String[] xyz = cPos.split(",");
		float[] pos = new float[3];
		for (int i = 0; i < pos.length; i++) {
			pos[i] = Float.parseFloat(xyz[i]);
		}
		return pos;
	}

	float getSensorFeedback() {
		ArrayList<Float> vals = new ArrayList<Float>();
		HashSet<Float> uniqVals = new HashSet<Float>();
		boolean isIR = false;
		int count = 0;
		int factor = 2;
		if (SENSOR.equalsIgnoreCase("IR")) {
			isIR = true;
			factor = 5;
		}
		while (vals.size() < 15 / factor) {
			float test = -1f;
			int cycles = 0;
			while (test < 1) {
				String readData = "";
				while (readData.equals("")) {
					readData = SR.getDepthValue();
				}
				test = parseSensorData(readData);
				sleeper(100 / factor);
				cycles++;
				if (cycles > 50 * factor) {
					SR.resetSerial();
					sleeper(2000);
					while (!SR.isReady()) {
						sleeper(100);
					}
					if (isIR) {
						println("ScanCycle is hanging: abort and retry");
					}else if(test<1){
						println("LRF cant find laser");
						test = DEPTH_EXPECT*1.5f;
					}
					
				}
			}
			if (isIR) {
				count++;
				if (!uniqVals.contains(test)) {
					uniqVals.add(test);
					vals.add(test);
					count = 0;
				} else {
					if (count > 200) {
						println("ScanCycle is hanging: abort and retry");
						SR.resetSerial();
						sleeper(2000);
						while (!SR.isReady()) {
							sleeper(100);
						}
						count = 0;
					}
				}
			} else {
				vals.add(test);
			}
		}
		Float[] arr = MJ_Math.toArrayFloats_alt(vals);
		return MJ_Math.getAverageWithSDrange(arr, 1.15f);
	}

	Float parseSensorData(String feed) {
		float val = 0;
		if (feed != "" && feed != null) {
			boolean isIR = false;
			if (feed.contains("IR")) {
				isIR = true;
			}
			feed = feed.toUpperCase();
			feed = feed.replaceAll("\\s", "");
			feed = feed.replaceAll("=", "");
			feed = feed.replaceAll("[a-zA-Z]", "").trim();
			try {
				val = Float.parseFloat(feed);
				if (isIR) {
					val = (float) calcIRVoltsToMM(val);
				}
			} catch (Exception e) {
			}
		}
		return val;
	}

	double calcIRVoltsToMM(double x) {
		return 6.4738575296974723e+003 * Math.pow(x, 0)
				+ -8.0428857565433503e+001 * Math.pow(x, 1)
				+ 4.2907854367515552e-001 * Math.pow(x, 2)
				+ -1.1022241876152245e-003 * Math.pow(x, 3)
				+ 1.2810399390389822e-006 * Math.pow(x, 4)
				+ -4.7181279863305592e-010 * Math.pow(x, 5);
	}

	// //DRAW STUFF

	void drawTCP(Vec2D pos, Vec2D next, String tool, String sensorMode) {
		Vec2D tcp = pos.copy();
		Vec2D nextTCP = next.copy();
		Vec2D off = new Vec2D(0, 0);
		float dim = 1;
		if (tool.equalsIgnoreCase("Sensor")) {
			if (sensorMode.equalsIgnoreCase("IR")) {
				off = new Vec2D(0, IR_yOff);
				strokeWeight(2);
				stroke(255, 255, 0, 200);
				fill(200, 0, 200, 55);
				dim = 10;
			} else if (sensorMode.equalsIgnoreCase("LRF")) {
				off = new Vec2D(0, LRF_yOff);
				strokeWeight(2);
				stroke(255, 255, 0, 200);
				fill(15, 200, 15, 55);
				dim = 25;
			}
		} else if (tool.equalsIgnoreCase("Grip")) {
			strokeWeight(2);
			stroke(255, 255, 0, 200);
			fill(200, 25);
			dim = TOOL_DIM / 2;
		} else {
			strokeWeight(2);
			stroke(255, 200);
			fill(50, 55);
		}
		tcp = tcp.add(off);
		nextTCP = nextTCP.add(off);
		if (!currentPos.equalsWithTolerance(nextTCP, tol * 0.1f)) {
			strokeWeight(1.75f);
			stroke(200, 200);
			List<Vec2D> dash = new ArrayList<Vec2D>();
			Line2D.splitIntoSegments(tcp, nextTCP, tol * 2, dash, true);
			for (int i = 0; i < dash.size() - 1; i += 3) {
				gfx.line(dash.get(i), dash.get(i + 1));
			}
		}
		crossHairs(tcp, dim * 1.15f);
		Rect r = new Rect(tcp.x - dim / 2, tcp.y - dim / 2, dim, dim);
		gfx.rect(r);
		strokeWeight(1.5f);
		stroke(255);
		crossHairs(nextTCP, dim * 1.15f);
	}

	void crossHairs(Vec2D t, float d) {
		Vec2D n = new Vec2D(t.x, t.y + d);
		Vec2D e = new Vec2D(t.x + d, t.y);
		Vec2D s = new Vec2D(t.x, t.y - d);
		Vec2D w = new Vec2D(t.x - d, t.y);
		gfx.line(e, w);
		gfx.line(n, s);
	}

	void drawCam(int x, int y) {
		camImage = CU.getImage();
		if (camImage != null) {
			image(camImage, x, y);
		}
	}

	String getStatus() {
		String stat = "Stage0: PickStick = " + stage0pickStick + "\n";
		stat += "Stage1: BuildStick = " + stage1buildStick + "\n";
		stat += "Stage2: ScanBuild = " + stage2ScanBuild + "\n";
		stat += "Stage3: GridScan = " + stage3GridScan + "\n";
		stat += "StageOrder = " + stageOrder + "\n";
		stat += "\n";
		stat += "hasStick = " + hasStick + "\n";
		stat += "buildPosReady = " + buildPosReady + "\n";
		stat += "isStickFound = " + isStickFound + "\n";
		stat += "Current Tool = " + currentTCP + "\n";
		stat += "Current SENSOR = " + SENSOR + "\n";
		return stat;
	}

	String limitLines(String txt) {
		int lim = 24;
		if (txt.split("\n").length > lim) {
			String newString = "";
			int difference = txt.split("\n").length - lim;
			for (int i = difference; i < txt.split("\n").length; i++) {
				newString += txt.split("\n")[i] + "\n";
			}
			return newString;
		}
		return txt;
	}

	// RAPID SERVER COMMS
	void sendDataArray(float[] data) {
		String[] dataStr = new String[data.length];
		for (int i = 0; i < dataStr.length; i++) {
			dataStr[i] = data[i] + "";
		}
		for (int i = 0; i < dataStr.length; i++) {
			while (dataStr[i].length() < 4) {
				dataStr[i] = dataStr[i] + "0";
			}
			if (dataStr[i].length() > 7) {
				dataStr[i] = dataStr[i].substring(0, 6);
			}
		}
		for (int i = 0; i < dataStr.length; i++) {
			if (i < dataStr.length - 1) {
				outMessage += dataStr[i] + ",";
			} else {
				outMessage += dataStr[i];
			}
		}
		if (!outMessage.contains("NaN")) {
			outMessage = "[" + outMessage + "]";
		}
	}

	void setupClient() throws IOException {
		@SuppressWarnings("resource")
		Socket sock = new Socket(hostName, portNumber);
		sock.setKeepAlive(true);
		in = sock.getInputStream();
		out = sock.getOutputStream();
		inReader = new BufferedReader(new InputStreamReader(
				sock.getInputStream()));
		outMessage = "Establish Connection";
		out.write(outMessage.getBytes());
		commsText += "Sending: " + outMessage + "\n";
		outMessage = "";
		out.flush();
		while (inMessage == null) {
			commsText += "Local: Waiting for SERVER response" + "\n";
			inMessage = inReader.readLine();
		}
		commsText += "Received: " + inMessage + "\n";
		while (!sock.isConnected()) {
			commsText += "Local: Connection Failure" + "\n";
		}
		if (sock.isConnected()) {
			connected = true;
		}
	}

	void getInMessage() {
		try {
			if (inReader.ready()) {
				inMessage = inReader.readLine();
				commsText += "Received: " + inMessage + "\n";
			}
		} catch (IOException e1) {
			commsText += "Local: " + "IOException on inMessage()" + "\n";
			e1.printStackTrace();
		}
	}

	void sendOutMessage() {
		try {
			if (outMessage != "") {
				commsText += "Sending: " + outMessage + "\n";
				out.write(outMessage.getBytes());
				outMessage = "";
				inMessage = "";
				out.flush();
			}
		} catch (IOException e) {
			commsText += "Local: " + "IOException on outMessage()" + "\n";
			e.printStackTrace();
		}
	}

	private void sleeper(int millis) {
		try {
			Thread.sleep(millis);
		} catch (Exception e) {
			System.out.println("Sleep failure!!!");
		}
	}

	// GETTER
	public synchronized Vector<Stick> getStack() {
		return this.currentBuild;
	}

	public void keyPressed() {
		if (key == 'd' || key == 'D') {
			showDGrid = !showDGrid;
		}
	}

	void screenShot() {
		// println("screen");
		save(sketchPath("output/screenShots/" + startTime + "/0" + frameCount
				+ "Stickstack.jpg"));
	}
}
