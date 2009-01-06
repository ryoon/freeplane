/*************************************************************************** * FreeplaneSplash, taken from GanttSplash.java. Copyright (C) 2002 by Thomas * Alexandre (alexthomas(at)ganttproject.org) Copyright (C) 2005-2008 by * Christian Foltin and Daniel Polansky ***************************************************************************//*************************************************************************** * * This program is free software; you can redistribute it and/or modify * it * under the terms of the GNU General Public License as published by * the Free * Software Foundation; either version 2 of the License, or * (at your option) * any later version. * * ***************************************************************************/package org.freeplane.startup;import java.awt.BorderLayout;import java.awt.Color;import java.awt.Dimension;import java.awt.Font;import java.awt.Graphics;import java.awt.Graphics2D;import java.awt.GraphicsEnvironment;import java.awt.Rectangle;import java.awt.RenderingHints;import java.awt.Toolkit;import java.util.Arrays;import javax.swing.ImageIcon;import javax.swing.JFrame;import javax.swing.JLabel;import javax.swing.JProgressBar;import javax.swing.JRootPane;import javax.swing.SwingUtilities;import org.freeplane.core.controller.Controller;/** * Class to put a splash during launching the application. */public class FreeplaneSplashModern extends JFrame implements IFreeplaneSplash {	private class FeedBackImpl implements IFeedBack {		private long mActualTimeStamp = System.currentTimeMillis();		private int mActualValue;		private JLabel mImageJLabel = null;		private long mTotalTime = 0;		public int getActualValue() {			return mActualValue;		}		public void increase(final String messageId) {			progress(getActualValue() + 1, messageId);		}		public void progress(final int act, final String messageId) {			final String progressString = Controller.getText(messageId);			mActualValue = act;			final long timeDifference = System.currentTimeMillis() - mActualTimeStamp;			mActualTimeStamp = System.currentTimeMillis();			mTotalTime += timeDifference;			SwingUtilities.invokeLater(new Runnable() {				public void run() {					mProgressBar.setValue(act);					final double percent = act * 1.0 / mProgressBar.getMaximum();					mProgressBar.setString(progressString);					if (mImageJLabel != null) {						mImageJLabel.putClientProperty("progressString", progressString);						mImageJLabel.putClientProperty("progressPercent", new Double(percent));						mImageJLabel.repaint();					}				}			});		}		public void setImageJLabel(final JLabel imageJLabel) {			mImageJLabel = imageJLabel;		}		public void setMaximumValue(final int max) {			mProgressBar.setMaximum(max);			mProgressBar.setIndeterminate(false);		}	}	final private FeedBackImpl feedBack;	final private ImageIcon mIcon;	final private JProgressBar mProgressBar;	public FreeplaneSplashModern() {		super("Freeplane");		feedBack = new FeedBackImpl();		mIcon = new ImageIcon(Controller.getResourceController().getResource(		    "/images/Freeplane_frame_icon.png"));		setIconImage(mIcon.getImage());		JFrame.setDefaultLookAndFeelDecorated(false);		setUndecorated(true);		getRootPane().setWindowDecorationStyle(JRootPane.NONE);		final ImageIcon splashImage = new ImageIcon(Controller.getResourceController().getResource(		    "/images/Freeplane_splash.png"));		final JLabel splashImageLabel = new JLabel(splashImage) {			private Integer mWidth = null;			final private Font progressFont = new Font("SansSerif", Font.PLAIN, 10);			private Font versionTextFont = null;			{				final GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();				final String[] envFonts = gEnv.getAvailableFontFamilyNames();				final String[] availableFontFamilyNames = envFonts;				versionTextFont = Arrays.asList(availableFontFamilyNames)				    .contains("Century Gothic") ? new Font("Century Gothic", Font.BOLD, 14)				        : new Font("Arial", Font.BOLD, 12);			}			@Override			public void paint(final Graphics g) {				super.paint(g);				final Graphics2D g2 = (Graphics2D) g;				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,				    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);				g2.setFont(versionTextFont);				final String freeplaneVersion = Controller.getController().getFreeplaneVersion()				    .toVersionNumberString();				if (mWidth == null) {					mWidth = new Integer(g2.getFontMetrics().stringWidth(freeplaneVersion));				}				final int yCoordinate = 60;				final int xCoordinate = (int) (getSize().getWidth() - mWidth.intValue() - 20);				g2.setColor(new Color(0, 0, 0));				g2.drawString(freeplaneVersion, xCoordinate, yCoordinate);				final String progressString = (String) getClientProperty("progressString");				if (progressString != null) {					final Double percent = (Double) getClientProperty("progressPercent");					final int xBase = 30;					final int yBase = 227;					final int width = 300;					g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,					    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);					g2.setFont(progressFont);					g2.setColor(new Color(0xff, 0xff, 0xff));					g2.drawString(progressString, xBase + 1, yBase - 4);					g2.setColor(new Color(0xc8, 0xdf, 0x8b));					g2.draw(new Rectangle(xBase + 2, yBase, width, 3));					g2.setColor(new Color(0xff, 0xff, 0xff));					g2.fill(new Rectangle(xBase + 1, yBase + 1, (int) (width * percent					    .doubleValue()), 2));				}			}		};		feedBack.setImageJLabel(splashImageLabel);		getContentPane().add(splashImageLabel, BorderLayout.CENTER);		mProgressBar = new JProgressBar();		mProgressBar.setIndeterminate(true);		mProgressBar.setStringPainted(true);		pack();		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();		final Dimension labelSize = splashImageLabel.getPreferredSize();		setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2		        - (labelSize.height / 2));	}	public void close() {		setVisible(false);		dispose();	}	public IFeedBack getFeedBack() {		return feedBack;	}	public ImageIcon getWindowIcon() {		return mIcon;	}}