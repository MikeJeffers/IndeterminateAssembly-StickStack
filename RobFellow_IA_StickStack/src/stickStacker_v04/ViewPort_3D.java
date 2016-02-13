package stickStacker_v04;


import java.util.Enumeration;
import java.util.Vector;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PImage;

public class ViewPort_3D extends PApplet implements Runnable {
	private static final long serialVersionUID = 1L;
	PeasyCam cam;
	boolean viewToggle = false;
	Vector<Stick> currentStack = new Vector<Stick>();
	StickStackMain p;
	PImage currentFrame;

	public ViewPort_3D(StickStackMain parent) {
		p = parent;
	}

	public void setup() {
		size(640, 480, OPENGL);
		background(0);
		smooth();
		frameRate(8);
		textSize(12);

		cam = new PeasyCam(this, (p.BZ_Xdom[0] + p.BZ_Xdom[1]) / 2.0f,
				(p.BZ_Ydom[0] + p.BZ_Ydom[1]) / 2.0f, 0, 1200);
		cam.setActive(true);
		cam.rotateX(-Math.PI / 4);
		stroke(255);

	}

	public void draw() {
		background(0);
		if(frameCount%2==1){
			cam.rotateZ(Math.PI / 64);
			cam.rotateY(-Math.PI / 64);
		}
		currentStack = p.getStack();
		Enumeration<Stick> e = currentStack.elements();
		while(e.hasMoreElements()){
			e.nextElement().drawStick3D(this);
		}
		noFill();
		rect(p.BZ_Xdom[0], p.BZ_Ydom[0], p.BZ_Xdom[1] - p.BZ_Xdom[0],
				p.BZ_Ydom[1] - p.BZ_Ydom[0]);
		currentFrame = this.get();
	}

	// Getter
	PImage get3DView() {
		return currentFrame;
	}

	// Setter
	public void resetPCam() {
		cam.reset();
	}

	public void setCamRotation(int i) {
		int sign = 1;
		if (i > 3) {
			i -= 3;
			sign = -sign;
		}
		if (i == 1) {
			cam.rotateX(sign * Math.PI / 8);
		} else if (i == 2) {
			cam.rotateY(sign * Math.PI / 8);
		} else if (i == 3) {
			cam.rotateZ(sign * Math.PI / 8);
		}
	}
}
