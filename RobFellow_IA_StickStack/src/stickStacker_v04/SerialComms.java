package stickStacker_v04;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.util.Enumeration;

public class SerialComms implements SerialPortEventListener {
	// /SERIAL VARS
	SerialPort serialPort;

	private static final String PORT_NAMES[] = { "COM3" };

	private BufferedReader input;

	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 8000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	// //

	SerialComms() {
	}

	@SuppressWarnings("rawtypes")
	boolean open() {
		System.out.print("Opening Comms ");
		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum
					.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return false;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(),
					TIME_OUT);
			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			serialPort.setOutputBufferSize(1000);

			// open the streams
			input = new BufferedReader(new InputStreamReader(
					serialPort.getInputStream()));

			serialPort.notifyOnDataAvailable(true);
			System.out.println("...Serial Comms Established");
			return true;

		} catch (Exception e) {
			System.err.println(e.toString());
			return false;
		}
	}

	String readInput() {
		try {
			if (input.ready()) {
				String toParse = "";
				char c = (char) input.read();
				toParse += String.valueOf(c);
				while (toParse.split("\n?\r").length < 3) {
					c = (char) input.read();
					toParse += String.valueOf(c);
				}
				String[] splits = toParse.split("\n?\r");
				for (int i = 0; i < splits.length; i++) {
					splits[i] = splits[i].trim();
				}
				String message = "";
				for (String s : splits) {
					if (s.contains("=")) {
						message = s;
					}
				}
				input.close();
				input = new BufferedReader(new InputStreamReader(
						serialPort.getInputStream()));
				return message;
			} else {
				return "";
			}
		} catch (IOException e) {
			return "";
		}
	}

	public synchronized void closeSerial() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
			System.out.println("Closing Serial Comms");
		}
	}

	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				String inputLine = input.readLine();
				System.out.println(inputLine);
			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
	}
}
