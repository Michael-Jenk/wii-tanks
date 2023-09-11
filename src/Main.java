//	JumpingBall import statements
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

//	MouseKeyInput imports
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseMotionListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@SuppressWarnings("serial")
public class Main extends JPanel {
	
	//dont forget to change size in Maps.java as well
	private static final int WIDTH = 1500;
	private static final int HEIGHT = 800;
	
	public static final Color cursorColor = new Color(0, 115, 255, 100);
	static final int cursorSize1 = 16;
	static final int cursorSize2 = 14;

	private BufferedImage image;
	private Graphics g;
	public Timer timer;
	private Maps map;
	private Music music;
	private Player player;
	private Enemy[] enemies;
	private boolean[] liveEnemies;
	
	private static JFrame frame;
	
	private int pTargX = WIDTH;
	private int pTargY = HEIGHT / 2;
	
	int phase = 0;
	int currentMap = 1; // starting map
	final int numberOfMaps = 6; //TODO: change depending on how many maps I could make
	int lives = 3;
	int score = 0;
	int[] scoreBreakdown = new int[5];
	
	final double lifeScreenTime = 2.3;
	final double gameplayPreviewTime = 2.0;
	final double gameplayPostviewTime = 5.0;
	double ttimer;
	
	final String font = "Arial"; // Will chance to the wii font eventually, this is a placeholder
	
	boolean debug = false;
	
	boolean drawDots = false;
	
	double dt = 0.01; // in milliseconds, multiply by 1000 to get seconds
	
	boolean setupDone = false;
	
	public Main() {
		//	Old stuff
		image =  new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		g = image.getGraphics();
		timer = new Timer((int)(dt * 1000), new TimerListener());
		timer.start();
		
		//	MouseKeyInput stuff
		addMouseListener(new Mouse());
		addMouseMotionListener(new MouseMotion());
		addMouseWheelListener(new MouseWheel());
		addKeyListener(new Keyboard());
		setFocusable(true);
		
		// New stuff
		map = new Maps(WIDTH, HEIGHT, 1);
		newMap(currentMap);
		
		// Newer stuff
		music = new Music();
		
		// dont forget to set cannon target when setting player
		player = new Player(map.getpSpawn()[0], map.getpSpawn()[1]);
		player.setCannonTarget(WIDTH, HEIGHT / 2);
		
		setupDone = true;
	}
	
	//	Old classes & methods
	
	private class TimerListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (setupDone) { // quick check 
			// phases:
			// 0 = screen with mission number & number of lives left
			// 1 = couple of paused seconds before gameplay starts
			// 2 = gameplay
			// 3 = paused seconds after last tank is destroyed
			// 4 = death screen & results
			
			if (phase == 0) {
				lifeScreenPhase();
			} else if (phase == 1) {
				gameplayPreviewPhase();
			} else if (phase == 2) {
				gameplayPhase();
			} else if (phase == 3) {
				gameplayPostviewPhase();
			} else if (phase == 4) {
				gameOverPhase();
			}
			}
			
		}
		
		public void lifeScreenPhase() {
			
			//might do: every fifth stage, add an extra life and show the short screen of that happening
			if (ttimer == lifeScreenTime) {
				music.playLifeScreenMusic();
			}
			
			g.setColor(new Color(252, 239, 164)); // background
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setColor(new Color(217, 65, 48)); // red stripe
			g.fillRect(0, 150, WIDTH, 350);
			g.setColor(new Color(219, 172, 0)); // gold stripes
			g.fillRect(0, 160, WIDTH, 10);
			g.fillRect(0, 480, WIDTH, 10);
			g.setColor(new Color(252, 239, 164)); // mission & enemy tank numbers
			g.setFont(new Font(font, 0, 80)); 
			g.drawString("Mission " + currentMap, 550, 300);
			g.setFont(new Font(font, 0, 50)); 
			g.drawString("Enemy tanks: " + map.geteSpawns().length, 545, 400);
			g.setColor(new Color(59, 163, 255)); // life counter
			g.drawString("X   " + lives, 690, 630);
			drawTank(575, 607, new Color(0, 36, 214)); // player icon w player color
			
			drawCursor();
			
			ttimer -= dt;
			if (ttimer <= 0 && !music.isPlaying()) {
				phase = 1;
				ttimer = gameplayPreviewTime;
				if (debug) {System.out.println("start gameplayPreviewPhase, mission number " + currentMap);}
			}
			
			repaint();
			
		}
		
		public void gameplayPreviewPhase() {
			if (ttimer == gameplayPreviewTime) {
				music.playPreviewMusic(enemies, liveEnemies);
			}
			
			drawDots = true;
			drawEverything();
			repaint();
			ttimer -= dt;
			
			if (ttimer <= 0 || music.getFrame() >= 177000) {
				phase = 2;
				drawDots = false;
				if (debug) {System.out.println("start gameplayPhase");}
				
				music.loopGameplayMusic();
			}
		}
		
		public void gameplayPhase() {
			
			boolean anyEnemiesLeft = false;
			
			// Move player
			player.playerMove(map.getWalls(), map.getLiveWalls(), enemies, dt);
			// Move enemy tanks, projectiles, & mines and check if all enemies are destroyed
			for (int i = 0; i < enemies.length; i++) {
				enemies[i].enemyMove(map.getWalls(), map.getLiveWalls(), map.getNodes(), enemies, player, dt);
				if (enemies[i].isLive()) {
					anyEnemiesLeft = true;
				} else {
					if (liveEnemies[i]) {
						// records the enemy tank as destroyed, adds to score
						score++;
						scoreBreakdown[enemies[i].getType()]++;
						liveEnemies[i] = false;
						
						music.updateGameplayMusic(enemies, liveEnemies);
					}
				}
			}
			
			drawEverything();
			
			repaint();
			
			if (!player.isLive() || !anyEnemiesLeft) {
				phase = 3;
				ttimer = gameplayPostviewTime;
				if (debug) {System.out.println("start gameplayPostviewPhase");}
				music.stop();
			}
		}
		
		public void gameplayPostviewPhase() {
			if (ttimer == gameplayPostviewTime) {
				music.playPostviewMusic(player.isLive(), lives);
			}
			
			drawEverything();
			
			if (player.isLive()) {
				// draws mission cleared
				g.setColor(new Color(252, 239, 164, 125)); // Transparent background
				g.fillRect(300, 250, 900, 100);
				g.setColor(new Color(122, 27, 0)); //"Mission Cleared!" layered text
				g.setFont(new Font(font, 0, 70)); 
				g.drawString("Mission cleared!", 500, 310);
				g.setColor(new Color(207, 158, 0)); 
				g.setFont(new Font(font, 0, 68));
				g.drawString("Mission cleared!", 506, 310);
				g.setColor(new Color(255, 255, 255, 150) ); // box showing how many tanks the player's destroyed
				g.fillRect(600, 400, 300, 170);
				g.setFont(new Font(font, 0, 40));
				g.setColor(new Color(122, 27, 0)); // "Destroyed" text
				g.drawString("Destroyed", 660, 450);
				g.setFont(new Font(font, 0, 73)); // displays number of tanks defeated so far
				g.setColor(new Color(0, 36, 214).brighter());
				g.drawString("" + score, 730, 540);
			}
			
			repaint();
			ttimer -= dt;
			if (ttimer <= 0 || !music.isPlaying()) {
				if (!player.isLive()) {
					if (lives > 0) {
						lives--;
						restartMap();
						phase = 0;
						ttimer = lifeScreenTime;
						if (debug) {System.out.println("restarting map");}
					} else {
						phase = 4;
						if (debug) {System.out.println("out of lives, game over");}
					}
				} else {
					currentMap++;
					if (!(currentMap > numberOfMaps)) {
						phase = 0;
						ttimer = lifeScreenTime;
						newMap(currentMap);
						if (debug) {System.out.println("next map: " + currentMap);}
					} else {
						gameOverPhase();
						
						music.loopResultsMusic();
					}
					
				}
			}
		}
		
		public void gameOverPhase() {
			
			if (!music.isPlaying()) {
				music.loopResultsMusic();
			}
			
			drawEverything();
			
			g.setColor(new Color(252, 239, 164, 125)); // Transparent background
			g.fillRect(550, 0, 400, HEIGHT);
			g.setColor(new Color(122, 27, 0)); //"Results" layered text
			g.setFont(new Font(font, 0, 80)); 
			g.drawString("Results", 610, 150);
			g.setColor(new Color(207, 158, 0)); //"Results" layered text
			g.setFont(new Font(font, 0, 78));
			g.drawString("Results", 615, 150);
			
			//list of tanks
			drawTank(650, 250, new Color(207, 127, 0));
			drawTank(650, 350, new Color(83, 83, 83));
			drawTank(650, 450, new Color(0, 194, 152).darker());
			drawTank(650, 550, new Color(240, 232, 0));
			drawTank(650, 650, new Color(247, 35, 102));
			
			//list of how many of those tanks you've destroyed
			g.setColor(new Color(122, 27, 0));
			g.setFont(new Font(font, 0, 55)); 
			for (int i = 0; i < scoreBreakdown.length; i++) {
				g.drawString("" + scoreBreakdown[i],825, 275 + (100 * i));
			}
			
			//final score
			g.setColor(new Color(0, 36, 214).brighter());
			g.drawString("" + score, 825, 760);
			
			repaint();
		}
		
	}
	
	private void drawTank(int x, int y, Color color) {
		g.setColor(color); // player icon w player color
		g.fillRect(x, y, 50, 23);
		g.setColor(color.darker()); // top half of icon, slightly darker
		g.fillRect(x + 10, y - 12, 30, 12);
		g.setColor(color.darker().darker()); // cannon
		g.fillRect(x + 40, y - 9, 25, 6);
	}
	
	private void drawEverything() {
		
		// Draw map
		map.drawMap(g);
		// Draw mines
		player.mineDraw(g);
		for (Enemy enemy: enemies) {
			enemy.mineDraw(g);
		}
		// Draw enemy tanks
		for (Enemy enemy: enemies) {
			enemy.enemyDraw(g);
		}
		// Draw player
		if (phase == 1 || phase == 2) {
			player.setCannonTarget(pTargX, pTargY);
		}
		player.draw(g);
		
		// Draw projectiles
		player.projDraw(g);
		for (Enemy enemy: enemies) {
			enemy.projDraw(g);
		}
		// Draw cursor
		drawCursor();
		
		if (debug) {map.drawNodes(g); System.out.println("nearest player node: " + player.findNearestNode(map.getNodes(), map.getWalls(), map.getLiveWalls()));}
		
	}
	
	public void newMap(int mapNumb) {
		map = new Maps(WIDTH, HEIGHT, mapNumb);
		enemies = new Enemy[map.geteSpawns().length];
		liveEnemies = new boolean[map.geteSpawns().length];
		for (int i = 0; i < liveEnemies.length; i++) {
			enemies[i] = new Enemy(map.geteSpawns()[i][0], map.geteSpawns()[i][1], map.geteSpawns()[i][2]);
			liveEnemies[i] = true;
		}
		player = new Player(map.getpSpawn()[0], map.getpSpawn()[1]);
		player.setCannonTarget(WIDTH, HEIGHT / 2);
		phase = 0;
		ttimer = lifeScreenTime;
	}
	
	public void restartMap() {
		//purposely dont recreate the liveEnemies method
		for (int i = 0; i < liveEnemies.length; i++) {
			enemies[i] = new Enemy(map.geteSpawns()[i][0], map.geteSpawns()[i][1], map.geteSpawns()[i][2]);
			if (!liveEnemies[i]) {
				enemies[i].destroy();
			}
		}
		player = new Player(map.getpSpawn()[0], map.getpSpawn()[1]);
		player.setCannonTarget(WIDTH, HEIGHT / 2);
		phase = 0;
		ttimer = lifeScreenTime;
	}
	
	public void drawCursor() {
		g.setColor(Color.BLUE);
		for (int i = 0; i < 4; i++) {
			int xd = i < 2 ? 1:-1;
			int yd = i % 2 == 0 ? 1:-1;
			g.drawLine(pTargX + (11 * xd), pTargY + (11 * yd), pTargX + ((11 + 20) * xd), pTargY + ((11 + 20) * yd));
		}
		g.setColor(cursorColor);
		if (drawDots) {
			for (int i = 1; i <= 5; i++) {
				g.fillOval(((pTargX - player.getX()) / 6) * i + player.getX() - (cursorSize2 / 2), ((pTargY - player.getY()) / 6) * i + player.getY() - (cursorSize2 / 2), cursorSize2, cursorSize2);
			}
		}
		g.setColor(Color.WHITE);
		g.fillOval(pTargX - (cursorSize1 / 2) - 2, pTargY - (cursorSize1 / 2) - 2, cursorSize1 + 4, cursorSize1 + 4);
		g.setColor(Color.BLUE);
		g.fillOval(pTargX - (cursorSize1 / 2), pTargY - (cursorSize1 / 2), cursorSize1, cursorSize1);
	}

	public void paintComponent(Graphics g) {
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
	}

	public static void main(String[] args) {
		frame = new JFrame("Wii Tanks");
		frame.setCursor( frame.getToolkit().createCustomCursor(new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB ),new Point(),null ) );
		frame.setSize(WIDTH + 18, HEIGHT + 47);
		frame.setLocation(0, 0);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new Main()); 
		frame.setVisible(true);
	}
	
	private class Mouse implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (!setupDone) {
				return;
			}
			if (phase == 1) {
				ttimer = 0;
				music.loopGameplayMusic();
			} else if (phase != 2) {
				ttimer = 0;
				music.stop();
			} else {
				if (e.getButton() == 1) {
					player.playerShoot();
					if (debug) {System.out.println("left clicked"); }
				}
				if (e.getButton() == 3) {
					player.dropMine();
					if (debug) {System.out.println("right clicked"); }
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
		
	}
	
	private class MouseMotion implements MouseMotionListener {

		@Override
		public void mouseDragged(MouseEvent e) {
			
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			
			pTargX = e.getX();
			pTargY = e.getY();
			
			if (debug) {
				//g.setColor(Color.WHITE);
				//g.fillOval(e.getX()-3, e.getY()-3, 6, 6);
				//System.out.println("Mouse distance: " + player.distanceFromTank(e.getX(), e.getY()) + ", Origin distance: " + player.distanceFromTank(WIDTH / 2, HEIGHT / 2));
				
			}
			
		}
		
	}
	
	private class MouseWheel implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			
		}
		
	}
	
	private class Keyboard implements KeyListener {

		@Override
		public void keyTyped(KeyEvent e) {
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
			
			if (e.getKeyCode() == KeyEvent.VK_W) {
				player.setMovement(0, true);
			}
			if (e.getKeyCode() == KeyEvent.VK_D) {
				player.setMovement(1, true);
			}
			if (e.getKeyCode() == KeyEvent.VK_S) {
				player.setMovement(2, true);
			}
			if (e.getKeyCode() == KeyEvent.VK_A) {
				player.setMovement(3, true);
			}
			
			// M toggles debug mode, N toggles the player's debug mode
			if (e.getKeyCode() == KeyEvent.VK_M) {
				debug = !debug;
			}
			if (e.getKeyCode() == KeyEvent.VK_N) {
				player.toggleDebug();
			}
			
			// cheat keycodes to speed up development
			if (e.getKeyCode() == KeyEvent.VK_G && debug) {
				//immediate game over
				phase = 4;
			}
			if (e.getKeyCode() == KeyEvent.VK_C && debug) {
				// mission cleared
				ttimer = gameplayPostviewTime;
				phase = 3;
			}
			
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_W) {
				player.setMovement(0, false);
			}
			if (e.getKeyCode() == KeyEvent.VK_D) {
				player.setMovement(1, false);
			}
			if (e.getKeyCode() == KeyEvent.VK_S) {
				player.setMovement(2, false);
			}
			if (e.getKeyCode() == KeyEvent.VK_A) {
				player.setMovement(3, false);
			}
		}
		
	}
	
}
