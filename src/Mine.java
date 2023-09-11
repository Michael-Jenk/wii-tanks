import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;

public class Mine {
	
	static final int size = 20;
	static final int explosionRadius = 200;
	static final Color yColor = Color.YELLOW;
	static final Color rColor = Color.RED;
	static final Color expColor = new Color(252, 64, 40, 255);
	
	//TODO: configure time
	static final double time = 4.7; // seconds after placement before it explodes
	
	int x;
	int y;
	
	double timer;
	
	Color color;
	
	private boolean live = true;
	private boolean exploding = false;
	
	public Mine(int x, int y) {
		this.x = x;
		this.y = y;
		live = true;
		timer = time;
	}
	
	public void draw(Graphics g) {
		if (live) {
			color = timer > 2 || timer % 0.25 < .125 ? yColor:rColor; 
			g.setColor(color);
			g.fillOval(x - (size/2), y - (size/2), size, size);
		} else if (exploding) {
			color = new Color( expColor.getRed() - (int)(expColor.getRed() *  (timer) / -.5), expColor.getGreen() - (int)( expColor.getGreen() *  (timer) / -.5), expColor.getBlue() - (int)( expColor.getBlue() *  (timer) / -.5), expColor.getAlpha() - (int)( expColor.getAlpha() *  (timer) / -.5));
			g.setColor(color);
			g.fillOval(x - (explosionRadius / 2), y - (explosionRadius / 2), explosionRadius, explosionRadius );
		}
	}
	
	public void move(double dt, int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player) {
		if (live || exploding) {
			timer -= dt;
			if (timer <= -.5) {
				exploding = false;
			} else if (timer <= 0 && live) {
				explode(walls, liveWalls, enemies, player);
			}
		}
	}
	
	// basically it's "destroy" method
	public void explode(int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player) {
		live = false;
		exploding = true;
		timer = 0.0;
		color = expColor;
		
		// enemy, player, mine, and bullet detection, copied from the projectile class
		for (Enemy tank: enemies) {
			if (tank.distanceFromTank((int)x, (int)y) < (explosionRadius / 2) && tank.isLive()) {
				tank.destroy();
				//System.out.println("enemy hit");
			}
			for (Projectile projs: tank.getProjectiles()) {
				if ((projs != null && projs.isLive()) && getDistance(x, y, projs.getX(), projs.getY()) < (explosionRadius / 2) ) {
					if (projs.isLive()) {
						projs.destroy();
					}
					//System.out.println("proj hit");
				}
			}
			for (Mine mines: tank.getMines()) {
				if ((mines != null && mines.isLive()) && getDistance(x, y, mines.getX(), mines.getY()) < (explosionRadius / 2)) {
					if (mines.isLive()) {
						mines.explode(walls, liveWalls, enemies, player);
					}
					//System.out.println("mine hit");
				}
			}
		}
		if (player.distanceFromTank((int)x, (int)y) < (explosionRadius / 2)) {
			player.destroy();
			//System.out.println("player hit");
		}
		for (Projectile projs: player.getProjectiles()) {
			if ((projs != null && projs.isLive()) && getDistance(x, y, projs.getX(), projs.getY()) < (explosionRadius / 2)) {
				if (projs.isLive()) {
					projs.destroy();
				}
				//System.out.println("proj hit");
			}
		}
		for (Mine mines: player.getMines()) {
			if ((mines != null && mines.isLive()) && getDistance(x, y, mines.getX(), mines.getY()) < (explosionRadius / 2)) {
				if (mines.isLive()) {
					mines.explode(walls, liveWalls, enemies, player);
				}
				//System.out.println("mine hit");
			}
		}
		
		double angle;
		for (int i = 0; i < walls.length; i++) {
			if (walls[i][4] == 1 && liveWalls[i]) {
				angle = Math.atan2( ((walls[i][3]-walls[i][1])/2) + walls[i][1] - y, ((walls[i][2]-walls[i][0])/2) + walls[i][0] - x);
				Line2D exp = new Line2D.Double(x, y, x + (int)(Math.cos(angle) * (explosionRadius / 2)), y + (int)(Math.sin(angle) * (explosionRadius / 2)));
				//wall detection using the line2D's intersectsWith class. it checks if the explosion passes any of the wall's sides
				Line2D wallT = new Line2D.Double(walls[i][0], walls[i][1], walls[i][2], walls[i][1]);
				Line2D wallL = new Line2D.Double(walls[i][0], walls[i][1], walls[i][0], walls[i][3]);
				Line2D wallB = new Line2D.Double(walls[i][0], walls[i][3], walls[i][2], walls[i][3]);
				Line2D wallR = new Line2D.Double(walls[i][2], walls[i][1], walls[i][2], walls[i][3]);
				if (exp.intersectsLine(wallT) || exp.intersectsLine(wallL) || exp.intersectsLine(wallB) || exp.intersectsLine(wallR)) {
					//destroys wall
					liveWalls[i] = false;
				}
				
			}
		}
		
		
	}
	
	public boolean isLive() {
		return live;
	}
	
	public boolean isExploding() {
		return exploding;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getSize() {
		return size;
	}
	
	public double getDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow( y2 - y1, 2) );
	}
	
}
