import java.awt.Dimension;
import java.awt.Graphics;
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
	private Point mousePos;
	private SuperPoint startingPoint;
	private int botTransparency;
	private JFrame jframe;
	private SuperMenu menu;
	private JPopupMenu snapMenu;
	private boolean obstaclesVisible = true;
	public static boolean relativeAngles =false;

	public SuperPanel() {
		field = new ImageIcon(SuperGUI.FIELD_IMAGE).getImage().getScaledInstance((int) (SuperGUI.FIELD_LENGTH*SuperGUI.SCALE), -1, Image.SCALE_DEFAULT);
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
		g.drawImage(field, 0, 0, null);
		
		g.setColor(SuperGUI.obstacleColor);
		if(obstaclesVisible) {
			for(SuperObstacle o : SuperObstacle.values()) {
				g.fillRect(o.shape.x, o.shape.y, o.shape.width, o.shape.height);
			}			
		}

		if (startingPoint != null) {
			if (followCursor && !menu.isVisible()) startingPoint.point(mousePos);
			startingPoint.draw(g, botTransparency);
		}

		g.setColor(SuperGUI.cursorColor);
		g.drawOval(mousePos.x - cursorRadius, mousePos.y - cursorRadius, cursorRadius * 2, cursorRadius * 2);
	}

	private void quit() {
		System.exit(0);
	}

	@Override
	public void keyPressed(KeyEvent k) {
		if(k.getKeyCode() == deleteAllKey){
			startingPoint = null;
		}
		if(k.getKeyCode() == deleteLastKey){
			if(startingPoint!= null){
				startingPoint.removeFinalSuperPoint();
			}
		}
		if(k.getKeyCode() == openSnapMenuKey){

			snapMenu.show(k.getComponent(),mousePos.x,mousePos.y);
			
		}
		if (k.getKeyCode() == toggleFollowCursorKey) followCursor = !followCursor;
		if (k.getKeyCode() == toggleObstacleVisbilityKey) obstaclesVisible = !obstaclesVisible;
		if (k.getKeyCode() == relativeAngleToggleKey) relativeAngles = !relativeAngles;
		if (k.getKeyCode() == openMapKey) {
			final JFileChooser fc = new JFileChooser(SuperGUI.MAPS_DIRECTORY);
			int i = fc.showOpenDialog(fc);
			if(i == JFileChooser.APPROVE_OPTION) {
				File selectedFile = fc.getSelectedFile();
				startingPoint = SuperReader.readCourse(selectedFile);
			}
		}
		if (k.getKeyCode() == printCourseKey) {
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
						commandWriter.write("package org.usfirst.frc.team2537.autocommands;\n\n");
						commandWriter.write("import org.usfirst.frc.team2537.robot.auto.AutoRotateCommand;\n");
						commandWriter.write("import org.usfirst.frc.team2537.robot.auto.CourseCorrect;\n");
						commandWriter.write("import org.usfirst.frc.team2537.robot.auto.GearCommand;\n\n");
						commandWriter.write("import edu.wpi.first.wpilibj.command.CommandGroup;\n\n");
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
		}
		if (k.getKeyCode() == exitKey) quit();
		repaint();
	}

	@Override
	public void keyReleased(KeyEvent k) {
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent m) {
		if(startingPoint != null && !startingPoint.isValidMove(m.getPoint(), followCursor)) return;
		
		mousePos.x = m.getX();
		mousePos.y = m.getY();

		if (startingPoint != null && !followCursor){
			mousePos = snap(mousePos);
		}

		if(startingPoint == null){
			if(mousePos.x < SuperGUI.FIELD_LENGTH*SuperGUI.SCALE/2) mousePos.x = (int) (SuperGUI.ROBOT_LENGTH*SuperGUI.SCALE/2);
			else mousePos.x = (int) (SuperGUI.FIELD_LENGTH*SuperGUI.SCALE - SuperGUI.ROBOT_LENGTH*SuperGUI.SCALE/2);
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
		double slope = Math.tan(startingPoint.getFinalAngle()); // slope of final point
		double x;
		double y;
		Point result;
		if(slope == 0){
			result = new Point(p.x, startingPoint.getFinalPoint().y);
		} else {
			double invslope = -1 / slope; // slope of line perpendicular

			// y-intercept of perpendicular line
			double b_perp = startingPoint.getFinalPoint().y - p.y - invslope * (p.x - startingPoint.getFinalPoint().x); // of

			x = (b_perp - 0) / (slope - invslope);
			y = -slope * x + 0;
			result = new Point((int) (x + startingPoint.getFinalPoint().x), (int) (y + startingPoint.getFinalPoint().y));
		}
		return result;
	}

	@Override
	public void mouseClicked(MouseEvent m) {
		if (SwingUtilities.isRightMouseButton(m)) {
			menu.show(m.getComponent(), m.getX(), m.getY());
		} else {
			if (startingPoint == null) {
				startingPoint = new SuperPoint(mousePos);
				startingPoint.point(new Point(m.getX() + 5, m.getY()));
			}
			else {
				startingPoint.add(mousePos);
				followCursor = true;
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
			double angle = Math.atan2(startingPoint.getFinalPoint().y - mousePos.y, mousePos.x - startingPoint.getFinalPoint().x);
			for(int i = 0 ; i<SuperEnum.values().length;i++){
				if(e.getActionCommand().equals(SuperEnum.values()[i].name)){
					startingPoint.addAction(new SuperAction(SuperEnum.values()[i], angle));
				}
			}
		}
		for(int i = 0 ; i<SuperSnapEnum.values().length;i++){
			if(e.getActionCommand().equals(SuperSnapEnum.values()[i].name)){
				if (startingPoint == null)
					startingPoint = new SuperPoint(SuperSnapEnum.values()[i].p);
				else {
					startingPoint.point(SuperSnapEnum.values()[i].p);
					startingPoint.add(SuperSnapEnum.values()[i].p);
					followCursor = true;
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
