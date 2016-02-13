package stickStacker_v04;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import processing.core.*;

public class CameraThread extends Thread {
	//TODO 
	//AUTHORS NOTE: UPDATED WITH JAVA RAW CLASSES
	//working pretty damn fine
	//LAST UPDATE: 4/26/2015

	StickStackMain p;
	// //Camera Communication vars
	URL url;
	PImage pimg;
	BufferedImage theImage;
	String location = "http://128.2.109.101/image/jpeg.cgi";
	URLConnection uc;
	BufferedReader in;
	Authenticator author = new Authenticator() {
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication("admin", "test".toCharArray());
		}
	};

	CameraThread(StickStackMain parent) {
		p = parent;
		pimg = null;
		theImage = null;
		System.out.println("IPCAM: Initalizing Connection!");
		System.out.println("IPCAM: success?");
		Authenticator.setDefault(author);
		try {
			url = new URL(location);
			uc = url.openConnection();
			uc.setReadTimeout(1000);
			in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			System.out.println("IPCAM: YES!");
		} catch (IOException e) {
			System.out.println("IPCAM: __NO!__");
			resetConnection();
		}
		
	}
	
	private void resetConnection(){
		System.out.println("IPCAM: Resetting Connection!");
		System.out.println("IPCAM: success?");
		pimg = null;
		Authenticator.setDefault(author);
		try {
			url = new URL(location);
			uc = url.openConnection();
			uc.setReadTimeout(1000);
			in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
			System.out.println("IPCAM: YES!");
		} catch (IOException e) {
			System.out.println("IPCAM: __NO!__");
		}
	}

	public void start() {
		super.start();
	}

	public void run() {
		while (true) {
			updateCam();
		}
	}

	void updateCam() {
		try {
			File f =  new File(p.sketchPath("output/" + "ScanImg.jpg"));
			Image image = ImageIO.read(url);
			ImageIO.write((RenderedImage) image, "jpg",f);
			theImage = ImageIO.read(f);
			pimg = p.loadImage("output/" + "ScanImg.jpg");
		} catch (IOException e) {
			resetConnection();
		}
	}

	// Getter
	public synchronized PImage getImage() {
		return this.pimg;
	}

	public synchronized BufferedImage getBufferImg() {
		return this.theImage;
	}
	
}
