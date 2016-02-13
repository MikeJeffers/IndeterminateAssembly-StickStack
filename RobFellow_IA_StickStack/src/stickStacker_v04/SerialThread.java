package stickStacker_v04;

public class SerialThread extends Thread {

	SerialComms serial;
	private String readOut = "";
	private boolean flagForReset = false;
	private boolean isReady = false;

	public SerialThread(SerialComms _serial) {
		serial = _serial;
	}

	public void start() {
		while (!serial.open()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		super.start();
		isReady=true;
	}

	public void run() {
		while (true) {
			if (flagForReset) {
				serial.closeSerial();
				isReady=false;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while (!serial.open()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				flagForReset = false;
				isReady = true;
			}
			detectDepthValue();
		}
	}

	void detectDepthValue() {
		String data = "";
		while (data.equals("")) {
			try {
				data = serial.readInput();
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.readOut = data;

	}

	// Getter
	public synchronized String getDepthValue() {
		return this.readOut;
	}
	
	public synchronized boolean isReady(){
		return isReady;
	}
	

	// Setter
	public synchronized void resetSerial() {
		flagForReset = true;
	}
}