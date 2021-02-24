import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

public class GUI extends JFrame {

	final JFileChooser fc = new JFileChooser();
	String fileString = "";
	String legend;
	ArrayList<Integer> allValues = new ArrayList<Integer>();
	private int maxVal;
	private DrawGraph graph;

	public GUI() {
		super("Data Visualiser");
		setPreferredSize(new Dimension(600, 400));
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		final JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);

		final JMenuItem openAction = new JMenuItem("Open", new ImageIcon("images/Open-icon.png"));
		final JMenuItem saveAction = new JMenuItem("Save", new ImageIcon("images/save-file.png"));
		final JMenuItem exitAction = new JMenuItem("Exit", new ImageIcon("images/exit-icon.png"));
		final JMenuItem aboutAction = new JMenuItem("About", new ImageIcon("images/about-us.png"));

		final JTextArea textArea = new JTextArea(8, 16);
		textArea.setFont(new Font("Serif", Font.BOLD, 16));
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		JScrollPane textScrollPane = new JScrollPane(textArea);
		textScrollPane.setBorder(
				BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Textual Representation"),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(openAction);
		fileMenu.add(saveAction);
		fileMenu.add(exitAction);
		exitAction.setMnemonic(KeyEvent.VK_X);
		exitAction.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getSource().equals(exitAction)) {
					System.exit(0);
				}
			}
		});
		fileMenu.addSeparator();
		helpMenu.addSeparator();
		helpMenu.add(aboutAction);
		aboutAction.setMnemonic(KeyEvent.VK_A);
		aboutAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(null, "This should be self explanatory to be honest...", "Help",
						JOptionPane.PLAIN_MESSAGE);
			}
		});
		saveAction.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(null, "save", "debug", JOptionPane.PLAIN_MESSAGE);
			}
		});
		openAction.addActionListener(new ActionListener() {
			private BufferedReader br;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int returnVal = fc.showOpenDialog(null);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					System.out.println("Opening: " + file);
					if (file.isDirectory()) {
						file = null;
					}
					if (file.canRead()) {
						FileReader fr = null;
						StringBuilder sb = new StringBuilder();
						fileString = "";
						try {
							fr = new FileReader(file);

							br = new BufferedReader(fr);

							String sCurrentLine;
							// remove the first line from the stringbuilder
							if ((sCurrentLine = br.readLine()) != null) {
								legend = sCurrentLine;
							}

							while ((sCurrentLine = br.readLine()) != null) {
								sb.append(sCurrentLine);
								sb.append('\n');
							}
							fileString = sb.toString();
							extractData();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					System.out.println("Open command cancelled by user./n");
				}
			}
		});
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void extractData() {
		System.out.println(legend);
		allValues.clear();
		String[] str = fileString.split("[,\n]");
		for (String s : str) {
			String tmpStr = s.replaceAll("[ \n]", "");
			try {
				int i = Integer.parseInt(tmpStr);
				allValues.add(i);
				if (i > maxVal) {
					maxVal = i;
				}
			} catch (NumberFormatException e) {
				System.out.println(tmpStr + " error");
				// e.printStackTrace();
			}
		}
		if (graph != null)
			remove(graph);
		graph = new DrawGraph(allValues, maxVal, this);
		add(graph);
		// TODO: workaround for the repaint method not working
		if (getWidth() == 800)
			setSize(700, 650);
		setSize(800, 650);
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			new GUI();
		});
	}

	@SuppressWarnings("serial")
	public class DrawGraph extends JPanel implements MouseWheelListener {
		private int maxScore = 500;
		private static final int BORDER_GAP = 20;
		private static final int MAX_POINTS = 1500;
		private static final int ALL_SENSORS = 8;
		private static final int RESOLUTION = 1;
		private int visiblePoints = 100;
		private final Color GRAPH_COLOR = Color.green;
		private final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
		private final Stroke GRAPH_STROKE = new BasicStroke(3f);
		private static final int GRAPH_POINT_WIDTH = 12;
		private int yHatchCount = 100;
		private ArrayList<Integer> scores;
		private GUI gui;
		private ArrayList<Point> graphPoints;
		public double scale = 1;
		private int zoomX;
		private int zoomY;
		private Point mousePt;
		private double scaleY = 1;
		boolean dragging = false;
		private long interval;

		public DrawGraph(ArrayList<Integer> scores, int maxVal, GUI gui) {
			this.scores = scores;
			maxScore = maxVal;
			yHatchCount = maxScore;
			this.gui = gui;
			interval = scores.get(ALL_SENSORS + 1);
			addMouseWheelListener(this);

			this.addMouseListener(new MouseAdapter() {

				@Override
				public void mousePressed(MouseEvent e) {
					dragging = true;
					mousePt = e.getPoint();
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					dragging = false;
					repaint();
				}
			});
			this.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					int dx = e.getX() - mousePt.x;
					int dy = e.getY() - mousePt.y;
					zoomX -= dx;
					zoomY -= dy;
					mousePt = e.getPoint();
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawString("samples: " + scores.get(ALL_SENSORS) / 100, getWidth() - BORDER_GAP - 100, BORDER_GAP);
			g.drawString("interval: " + interval + " ms", getWidth() - BORDER_GAP - 100, 2 * BORDER_GAP);
			g.drawString("zoom x: " + scale, getWidth() - BORDER_GAP - 100, 3 * BORDER_GAP);
			g.drawString("zoom y: " + (scale * scaleY), getWidth() - BORDER_GAP - 100, 4 * BORDER_GAP);
			g.drawString("x position: " + zoomX / (scale), getWidth() - BORDER_GAP - 100, 5 * BORDER_GAP);
			g.drawString("y position: " + zoomY / (scaleY * scale), getWidth() - BORDER_GAP - 100, 6 * BORDER_GAP);

			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// draw cross lines
			if (dragging) {
				int x = MouseInfo.getPointerInfo().getLocation().x - gui.getLocation().x;
				int y = MouseInfo.getPointerInfo().getLocation().y - gui.getLocation().y;
				g2.drawLine(x, 0, x, getHeight());
				g2.drawLine(0, y, getWidth(), y);
			}

			graphPoints = new ArrayList<Point>();
			makePointList();

			// create x and y axes
			g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, BORDER_GAP, BORDER_GAP);
			g2.drawLine(BORDER_GAP, getHeight() - BORDER_GAP, getWidth() - BORDER_GAP, getHeight() - BORDER_GAP);

			// create hatch marks for y axis.
			for (long i = 0; i < yHatchCount; i += 10) {
				int x0 = BORDER_GAP;
				int x1 = GRAPH_POINT_WIDTH + BORDER_GAP;
				int y0 = (int) (getHeight()
						- (((i + 1) * (scaleY * scale * getHeight() - BORDER_GAP * 2)) / yHatchCount + BORDER_GAP));
				int distanceBetweenMarks = (int) ((scaleY * scale * getHeight()) / (yHatchCount / 10));
				y0 -= zoomY % distanceBetweenMarks;
				int y1 = y0;

				if (y0 < getHeight()) {
					g2.drawLine(x0, y0, x1, y1);
					
					// WHAT IS WRONG WITH THIS
					// CALCULATION???????????????????????????????????????????????????????????????????????????????????
					//???????????????????????????????????????????????????????????????????????????????????????????????
					//???????????????????????????????????????????????????????????????????????????????????????????????
					//???????????????????????????????????????????????????????????????????????????????????????????????
					double offset = yHatchCount * (1 - (zoomY + getHeight()) / (scale * scaleY * getHeight()));
					offset = (int) offset;
					if (distanceBetweenMarks > 50) {
						int fac = distanceBetweenMarks > 200 ? 1 : 5;
						if (i % (fac * 10) == 0) {
							// * 4.8828125
							g.drawString((offset + i) + "mV", x1, y1);
							g.drawString(offset + " off", x1 + 300, y1);
						}
					}
				}
			}

			// and for x axis
			for (long i = 0; i < scores.size() - 1; i += 1000 * (ALL_SENSORS + 2)) {
				int x0 = (int) ((i + 1) * (scale * getWidth() - BORDER_GAP * 2) / (scores.size() - 1) + BORDER_GAP);
				int distanceBetweenMarks = (int) ((scale * getWidth()) / (scores.size() / (1000 * (ALL_SENSORS + 2))));
				x0 -= zoomX % distanceBetweenMarks;
				int x1 = x0;
				int y0 = getHeight() - BORDER_GAP;
				int y1 = y0 - GRAPH_POINT_WIDTH;

				if (x0 < getWidth()) {
					g2.drawLine(x0, y0, x1, y1);
					double offset = zoomX / ((scale * getWidth()) / (scores.size() / (1000.0 * (ALL_SENSORS + 2))));
					offset = (int) offset;
					if (distanceBetweenMarks > 50) {
						int fac = distanceBetweenMarks > 200 ? 1 : 5;
						if (i % (fac * 1000 * (ALL_SENSORS + 2)) == 0) {
							g.drawString(
									(int) (offset * interval + (i / ((1000.0 / interval) * (ALL_SENSORS + 2)))) + "s",
									x0, y1);
						}
						// additional small lines
						if (distanceBetweenMarks > 200) {
							for (int j = 0; j < 9; j++) {
								x0 += distanceBetweenMarks / 10;
								x1 = x0;
								g2.drawLine(x0, y0, x1, y1 + (GRAPH_POINT_WIDTH / 2));
							}
						}
					}
				}
			}

			Stroke oldStroke = g2.getStroke();
			g2.setColor(GRAPH_COLOR);
			g2.setStroke(GRAPH_STROKE);
			for (int i = 0; i < graphPoints.size() - 1; i++) {
				int x1 = graphPoints.get(i).x;
				int y1 = graphPoints.get(i).y;
				int x2 = graphPoints.get(i + 1).x;
				int y2 = graphPoints.get(i + 1).y;
				if (x2 > x1)
					g2.drawLine(x1, y1, x2, y2);
			}
			if (visiblePoints < MAX_POINTS) {
				g2.setStroke(oldStroke);
				g2.setColor(GRAPH_POINT_COLOR);
				for (int i = 0; i < graphPoints.size(); i++) {
					int x = graphPoints.get(i).x - GRAPH_POINT_WIDTH / 2;
					int y = graphPoints.get(i).y - GRAPH_POINT_WIDTH / 2;
					int ovalW = GRAPH_POINT_WIDTH;
					int ovalH = GRAPH_POINT_WIDTH;
					g2.fillOval(x, y, ovalW, ovalH);
				}
			}
		}

		private void makePointList() {
			double xScale = scale * ((double) getWidth() - 2 * BORDER_GAP) / (scores.size() - 1);
			double yScale = scaleY * scale * ((double) getHeight() - 2 * BORDER_GAP) / (maxScore - 1);

			int numberOfPoints = scores.size();
			int jumps = (int) ((numberOfPoints / (getWidth() * scale)) / (ALL_SENSORS + 2));
			if (jumps < 1)
				jumps = 1;
			else
				jumps *= RESOLUTION;

			int startVisible = (int) Math.round((zoomX / xScale) / (ALL_SENSORS + 2.0));
			startVisible *= (ALL_SENSORS + 2);
			int endVisible = (int) Math.round(((zoomX + getWidth()) / xScale) / (ALL_SENSORS + 2.0));
			endVisible *= (ALL_SENSORS + 2);
			endVisible += (ALL_SENSORS + 2);
			if (scale > 1)
				visiblePoints = endVisible - startVisible;
			else
				visiblePoints = MAX_POINTS + 1;
			// create the points for 8 graphs
			for (int j = 0; j < ALL_SENSORS; j++) {
				if (scale > 1) {
					// zoomed in
					int count = 0;
					for (int i = j; i < numberOfPoints; i += (ALL_SENSORS + 2)) {
						count++;
						if (count % jumps == 0) {
							if (i > startVisible && i < endVisible) {
								count = 0;
								int x1 = (int) ((i - j) * xScale + BORDER_GAP) - zoomX;
								int y1 = (int) ((maxScore - scores.get(i)) * yScale + BORDER_GAP) - zoomY;
								graphPoints.add(new Point(x1, y1));
							}
						}
					}
				} else {
					int count = 0;
					for (int i = j; i < numberOfPoints; i += (ALL_SENSORS + 2)) {
						count++;
						if (count % jumps == 0) {
							count = 0;
							int x1 = (int) ((i - j) * xScale + BORDER_GAP) - zoomX;
							int y1 = (int) ((maxScore - scores.get(i)) * yScale + BORDER_GAP) - zoomY;
							graphPoints.add(new Point(x1, y1));
						}
					}
				}
			}
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int x = MouseInfo.getPointerInfo().getLocation().x - gui.getLocation().x;
			int y = MouseInfo.getPointerInfo().getLocation().y - gui.getLocation().y;
			if (e.isControlDown()) {
				if (e.getWheelRotation() < 0) {
					scaleY *= 1.1;
					zoomY = (int) ((y) * (1.1 - 1) + 1.1 * zoomY);
				} else {
					double scalar = 1 / 1.1;
					scaleY *= scalar;
					zoomY = (int) ((y) * (scalar - 1) + scalar * zoomY);
				}
			} else {
				if (e.getWheelRotation() < 0) {
					scale *= 1.1;
					zoomX = (int) ((x) * (1.1 - 1) + 1.1 * zoomX);
					zoomY = (int) ((y) * (1.1 - 1) + 1.1 * zoomY);
				} else {
					double scalar = 1 / 1.1;
					scale *= scalar;
					if (scale < 1)
						scale = 1;
					else {
						zoomX = (int) ((x) * (scalar - 1) + scalar * zoomX);
						zoomY = (int) ((y) * (scalar - 1) + scalar * zoomY);
					}
				}
			}
			repaint();
		}
	}
}
