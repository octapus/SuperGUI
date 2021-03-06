import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * Handles input
 *
 */
@SuppressWarnings("serial")
public class SuperPanel extends JPanel implements KeyListener, MouseMotionListener, MouseListener, MouseWheelListener, ActionListener {

	private static final int cursorRadius = (int) (SuperGUI.ROBOT_DIAMETER/2*SuperGUI.SCALE); // pixels
	private static final int toggleFollowCursorKey = KeyEvent.VK_SPACE;
	private static final int toggleObstacleVisbilityKey = KeyEvent.VK_H;
	private static final int exitKey = KeyEvent.VK_ESCAPE;
	private static final int relativeAngleToggleKey = KeyEvent.VK_R;
	private static final int openSnapMenuKey = KeyEvent.VK_S;
	private static final int deleteAllKey = KeyEvent.VK_C;
	private static final int openMapKey = KeyEvent.VK_O;
	private static final int printCourseKey = KeyEvent.VK_ENTER;
	private static final int deleteLastKey = KeyEvent.VK_BACK_SPACE;

	private Image field;
	private boolean followCursor = false;
	private Double alignBot;
	private Point trueMousePos;
	private Point mousePos;
	private SuperPoint startingPoint;
	private int botTransparency;
	private JFrame jframe;
	private SuperMenu menu;
	private JPopupMenu snapMenu;
	private boolean obstaclesVisible = true;
	public static boolean relativeAngles =false;

	public SuperPanel() {
		field = new ImageIcon(SuperGUI.class.getResource("/resources/FIELD.png")).getImage().getScaledInstance((int) (SuperGUI.FIELD_LENGTH*SuperGUI.SCALE), -1, Image.SCALE_DEFAULT);
		addKeyListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		addMouseWheelListener(this);
		setPreferredSize(new Dimension((int) (SuperGUI.FIELD_LENGTH * SuperGUI.SCALE),
				(int) (SuperGUI.FIELD_WIDTH * SuperGUI.SCALE)));
		mousePos = new Point(0, 0);
		botTransparency = 255;
		jframe = new JFrame();
		menu = new SuperMenu(this);
		snapMenu = new SuperSnapMenu(this);
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(field, 0, 0, null);

		g2.setColor(SuperGUI.obstacleColor);
		if(obstaclesVisible) {
			for(SuperObstacle o : SuperObstacle.values()) {
				g2.fillPolygon(o.scaledShape);
			}
		}

		if (startingPoint != null) {
			if (followCursor && !menu.isVisible()) startingPoint.point(mousePos);
			if(alignBot != null) {
				g2.setColor(new Color(0, 0, 0, 25));
				g2.setStroke(new BasicStroke((float) (SuperGUI.ROBOT_WIDTH*SuperGUI.SCALE)));
				g2.drawLine(mousePos.x, mousePos.y, trueMousePos.x, trueMousePos.y);
			}
			startingPoint.draw(g2, botTransparency);
		}

		g2.setColor(SuperGUI.cursorColor);
		if(obstaclesVisible) g2.drawOval(mousePos.x - cursorRadius, mousePos.y - cursorRadius, cursorRadius * 2, cursorRadius * 2);
		if(alignBot != null) g2.drawOval(trueMousePos.x - cursorRadius, trueMousePos.y - cursorRadius, cursorRadius * 2, cursorRadius * 2);
	}

	private void quit() {
		System.exit(0);
	}

	@SuppressWarnings("unused")
	@Override
	public void keyPressed(KeyEvent k) {
		switch(k.getKeyCode()) {
		case deleteAllKey:
			startingPoint = null;
			break;
		case deleteLastKey:
			if(startingPoint != null){
				if(startingPoint.getNext() == null) startingPoint = null;
				else startingPoint.removeFinalSuperPoint();
			}
			break;
		case openSnapMenuKey:
			if(startingPoint == null) {
				for(SuperSnapEnum s : SuperSnapEnum.values()) {
					if(s.isStartingPos) {
						snapMenu.show(k.getComponent(),mousePos.x,mousePos.y);
						break;
					}
				}
			} else {
				snapMenu.show(k.getComponent(),mousePos.x,mousePos.y);
			}
			break;
		case toggleFollowCursorKey:
			followCursor = !followCursor;
			break;
		case toggleObstacleVisbilityKey:
			obstaclesVisible = !obstaclesVisible;
			break;
		case relativeAngleToggleKey:
			relativeAngles = !relativeAngles;
			break;
		case openMapKey:
			final JFileChooser fc = new JFileChooser(SuperGUI.MAPS_DIRECTORY);
			int i = fc.showOpenDialog(fc);
			if(i == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fc.getSelectedFile();
				startingPoint = SuperReader.readCourse(selectedFile);
				followCursor = true;
			}
			break;
		case printCourseKey:
			System.out.println("Course================" + startingPoint.getNumBots());
			String mapName;
			if(SuperGUI.WRITE_COMMAND || SuperGUI.WRITE_MAP) {
				mapName = (String) JOptionPane.showInputDialog(jframe, "Enter map name:\n", "File Name",
						JOptionPane.PLAIN_MESSAGE, null, null, "");
			}

			BufferedWriter mapWriter = null;
			if(SuperGUI.WRITE_MAP && mapName != null && mapName.length() > 0) {
				File mapFile = new File(SuperGUI.MAPS_DIRECTORY + mapName + ".txt");
				try {
					mapWriter = new BufferedWriter(new FileWriter(mapFile));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if(SuperGUI.WRITE_COMMAND) {
				if (mapName != null) {
					File commandFile = new File(SuperGUI.COMMANDS_DIRECTORY + mapName + ".java");
					try {
						BufferedWriter commandWriter = new BufferedWriter(new FileWriter(commandFile));
						commandWriter.write("package " + SuperGUI.COMMANDS_DIRECTORY.substring(4, SuperGUI.COMMANDS_DIRECTORY.length() - 1).replace('/', '.') + ";\n\n");
						commandWriter.write("import " + SuperGUI.AUTOROTATE_COMMAND + ";\n");
						commandWriter.write("import " + SuperGUI.AUTODRIVE_COMMAND + ";\n");
						for(SuperEnum e : SuperEnum.values()) {
							commandWriter.write("import " + e.command + ";\n");
						}
						commandWriter.write("\nimport edu.wpi.first.wpilibj.command.CommandGroup;\n\n");
						commandWriter.write("public class " + mapName + " extends CommandGroup {\n");
						commandWriter.write("\tpublic " + mapName + "() {\n");

						if(startingPoint.getPoint().x < SuperGUI.FIELD_LENGTH*SuperGUI.SCALE/2)
							SuperPrinter.printCourse(startingPoint, 0, commandWriter, mapWriter);
						else
							SuperPrinter.printCourse(startingPoint, 180, commandWriter, mapWriter);

						commandWriter.write("\t}\n");
						commandWriter.write("}\n");
						commandWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				if(startingPoint.getPoint().x < SuperGUI.FIELD_LENGTH*SuperGUI.SCALE/2)
					SuperPrinter.printCourse(startingPoint, 0, null, mapWriter);
				else
					SuperPrinter.printCourse(startingPoint, 180, null, mapWriter);
			}

			try {
				if(mapWriter != null) mapWriter.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			break;

		case KeyEvent.VK_RIGHT:
			if(startingPoint != null && !followCursor) alignBot = 0d;
			break;
		case KeyEvent.VK_1:
			if(startingPoint != null && !followCursor) alignBot = Math.PI/6;
			break;
		case KeyEvent.VK_2:
			if(startingPoint != null && !followCursor) alignBot = Math.PI/4;
			break;
		case KeyEvent.VK_3:
			if(startingPoint != null && !followCursor) alignBot = Math.PI/3;
			break;
		case KeyEvent.VK_UP:
			if(startingPoint != null && !followCursor) alignBot = Math.PI/2;
			break;
		case KeyEvent.VK_4:
			if(startingPoint != null && !followCursor) alignBot = -Math.PI/6;
			break;
		case KeyEvent.VK_5:
			if(startingPoint != null && !followCursor) alignBot = -Math.PI/4;
			break;
		case KeyEvent.VK_6:
			if(startingPoint != null && !followCursor) alignBot = -Math.PI/3;
			break;

		case exitKey:
			quit();
			break;
		}
		repaint();
	}

	private Point align(Point p) {
		Point2D.Double downscaledP = new Point2D.Double(p.x/SuperGUI.SCALE, p.y/SuperGUI.SCALE);
		double slope = Math.tan(startingPoint.getFinalAngle()); // slope of final point
		double x;
		double y;
		Point2D.Double result;
		double angleSlope = Math.tan(alignBot); // slope of angle

		// y-intercept of angle line
		double b_angle = startingPoint.getFinalPoint().y - downscaledP.y - angleSlope * (downscaledP.x - startingPoint.getFinalPoint().x);

		x = (b_angle - 0) / (slope - angleSlope);
		y = -slope * x + 0;
		result = new Point2D.Double(x + startingPoint.getFinalPoint().x, y + startingPoint.getFinalPoint().y);

		return new Point((int) (result.x * SuperGUI.SCALE), (int) (result.y * SuperGUI.SCALE));
	}

	@Override
	public void keyReleased(KeyEvent k) {
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent m) {
		trueMousePos = m.getPoint();
		Point currentCursorPos = m.getPoint();
		if (startingPoint != null && !followCursor){
			if(alignBot != null) currentCursorPos = align(m.getPoint());
			else currentCursorPos = snap(m.getPoint());
		}

		if(startingPoint != null && !startingPoint.isValidMove(currentCursorPos, followCursor, !obstaclesVisible)) return;

		mousePos.x = currentCursorPos.x;
		mousePos.y = currentCursorPos.y;

		if(startingPoint == null){
			// Start with back to alliance station wall
			if(mousePos.x < SuperGUI.FIELD_LENGTH*SuperGUI.SCALE/2) mousePos.x = (int) (SuperGUI.ROBOT_LENGTH*SuperGUI.SCALE/2);
			else mousePos.x = (int) (SuperGUI.FIELD_LENGTH*SuperGUI.SCALE - SuperGUI.ROBOT_LENGTH*SuperGUI.SCALE/2);

			// Avoid corners
			if(mousePos.y < (SuperGUI.CORNER_WIDTH + SuperGUI.ROBOT_WIDTH/2)*SuperGUI.SCALE) mousePos.y = (int) ((SuperGUI.CORNER_WIDTH + SuperGUI.ROBOT_WIDTH/2)*SuperGUI.SCALE);
			if(mousePos.y > (SuperGUI.FIELD_WIDTH - SuperGUI.CORNER_WIDTH - SuperGUI.ROBOT_WIDTH/2)*SuperGUI.SCALE) mousePos.y = (int) ((SuperGUI.FIELD_WIDTH - SuperGUI.CORNER_WIDTH - SuperGUI.ROBOT_WIDTH/2)*SuperGUI.SCALE);
		}


		if(startingPoint != null && !menu.isVisible()){
			startingPoint.updateFinalDistance(mousePos);
		}

		repaint();
	}

	/**
	 * Snaps a point to the line that the SuperPoint is currently pointing to
	 *
	 * @param p
	 *            - point to snap to the angle of SuperPoint
	 * @return the point on the SuperPoint direction line closest to the inputted point
	 */
	private Point snap(Point p) {
		Point2D.Double downscaledP = new Point2D.Double(p.x/SuperGUI.SCALE, p.y/SuperGUI.SCALE);
		double slope = Math.tan(startingPoint.getFinalAngle()); // slope of final point
		double x;
		double y;
		Point2D.Double result;
		if(slope == 0){
			result = new Point2D.Double(downscaledP.x, startingPoint.getFinalPoint().y);
		} else {
			double invslope = -1 / slope; // slope of line perpendicular

			// y-intercept of perpendicular line
			double b_perp = startingPoint.getFinalPoint().y - downscaledP.y - invslope * (downscaledP.x - startingPoint.getFinalPoint().x);

			x = (b_perp - 0) / (slope - invslope);
			y = -slope * x + 0;
			result = new Point2D.Double(x + startingPoint.getFinalPoint().x, y + startingPoint.getFinalPoint().y);
		}

		return new Point((int) (result.x * SuperGUI.SCALE), (int) (result.y * SuperGUI.SCALE));
	}

	@Override
	public void mouseClicked(MouseEvent m) {
		if (SwingUtilities.isRightMouseButton(m)) {
			menu.show(m.getComponent(), m.getX(), m.getY());
		} else {
			if (startingPoint == null) {
				followCursor = false;
				startingPoint = new SuperPoint(mousePos);
				if(mousePos.x < SuperGUI.FIELD_LENGTH*SuperGUI.SCALE/2) startingPoint.point(new Point(mousePos.x + 5, mousePos.y));
				else startingPoint.point(new Point(mousePos.x - 5, mousePos.y));
			}
			else if (startingPoint.isValidMove(mousePos, followCursor, false)){
				startingPoint.add(mousePos);
				followCursor = true;
			}

			if(alignBot != null) {
				followCursor = false;
				startingPoint.point(trueMousePos);
				alignBot = null;
			}
		}
		repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent m) {
		botTransparency -= 10 * m.getPreciseWheelRotation();
		if (botTransparency > 255) botTransparency = 255;
		if (botTransparency < 0) botTransparency = 0;
		repaint();
	}

	@Override
	public void keyTyped(KeyEvent k) {}

	@Override
	public void mousePressed(MouseEvent m) {}

	@Override
	public void mouseReleased(MouseEvent m) {}

	@Override
	public void mouseDragged(MouseEvent m) {
		mouseMoved(m);
	}

	@Override
	public void mouseEntered(MouseEvent m) {}

	@Override
	public void mouseExited(MouseEvent m) {}

	@Override
	public void actionPerformed(ActionEvent e){
		if(startingPoint != null){
			double angle = Math.atan2(startingPoint.getFinalPoint().y - mousePos.y/SuperGUI.SCALE, mousePos.x/SuperGUI.SCALE - startingPoint.getFinalPoint().x);
			for(int i = 0 ; i<SuperEnum.values().length;i++){
				if(e.getActionCommand().equals(SuperEnum.values()[i].name)){
					startingPoint.addAction(new SuperAction(SuperEnum.values()[i], angle));
					repaint();
					return;
				}
			}
		}

		for(SuperSnapEnum s : SuperSnapEnum.values()){
			if(e.getActionCommand().equals(s.name)){
				if (startingPoint == null) {
					if(s.isStartingPos)	startingPoint = new SuperPoint(s.point);
					else break;
				} else {
					if(startingPoint.isValidMove(s.point, true, false)) {
						startingPoint.point(s.point);
						startingPoint.add(s.point);
						followCursor = true;
					} else {
						break;
					}
				}
			}
		}
		repaint();
	}

	public boolean isRelativeAngles() {
		return relativeAngles;
	}

	public void setRelativeAngles(boolean relativeAngles) {
		SuperPanel.relativeAngles = relativeAngles;
	}
}
