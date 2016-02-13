package stickStacker_v04;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import processing.core.PApplet;

public class ViewPortFrame extends Frame {
	private static final long serialVersionUID = 1L;

	public ViewPortFrame(String name, PApplet embed, int x, int y, int wid,
			int hi) {
		super(name);
		setLayout(new BorderLayout());
		add(embed, BorderLayout.CENTER);

		embed.init();

		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowActivated(WindowEvent e) {
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}
		});

		setSize(wid, hi);
		setLocation(x, y);
		setVisible(true);

	}
}
