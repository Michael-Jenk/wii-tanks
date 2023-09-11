import java.awt.Color;
import java.awt.Graphics;

public class Projectile {
	
	// dont forget to change the projectile size in Tank class
	final int size = 16;
	
	//decided to use doubles for the x & y to allow for more specific projectile speeds
	double x;
	double y;
	double xspeed;
	double yspeed;
	double maxspeed;
	int bounces;
	boolean firstCheck;
	
	private final Color color = Color.WHITE;
	
	// this boolean exists for the projectile's collision detection to make sure it doesnt collide with itself
	public boolean checking = false;
	
	private boolean live = true;
	
	public Projectile(int x, int y, double dir, double maxSpeed, int bounces) {
		this.x = x;
		this.y = y;
		this.xspeed = Math.cos(dir) * maxSpeed;
		this.yspeed = Math.sin(dir) * maxSpeed;
		this.maxspeed = maxSpeed;
		this.bounces = bounces;
		live = true;
		// method for destroying projectile if it spawns in a wall
		firstCheck = true;
		
	}
	
	public void draw(Graphics g) {
		if (live) {
			g.setColor(color);
			g.fillOval((int)x - (size / 2), (int)y - (size / 2), size, size);
		}
	}
	
	public void move(int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player) {
		if (live) {
		// just a copy/paste of the tank collision detection, with minor changes
			double nx = x + xspeed;
			double ny = y + yspeed;
			
			if (Math.sqrt( Math.pow(xspeed, 2) + Math.pow( yspeed, 2) )> maxspeed) {
				nx = (int)x + (int)(maxspeed * Math.cos(Math.atan2(yspeed, xspeed)));
				ny = (int)y + (int)(maxspeed * Math.sin(Math.atan2(yspeed, xspeed)));
			}
			
			double rightx = nx + (size/2);
			double leftx = nx - (size/2);
			double topy = ny - (size/2);
			double bottomy = ny + (size/2);
			
			boolean movex = true;
			boolean movey = true;
			
			
			for (int i = 0; i < walls.length; i++) {
				int[] wall = walls[i];
				if (liveWalls[i] && wall[4] != 2) {
					if ( !( ((topy < wall[1]) == (bottomy < wall[1])) && ((topy > wall[3]) == (bottomy > wall[3])) ) && ( (rightx > wall[0]) == (leftx < wall[2]))) {
						if (bounces == 0) {
							destroy();
						} else {
							movey = false;
							yspeed *= -1;
							bounces--;
							//resets the projectile normal to the wall
							if (ny > wall[3]) {
								y = wall[3] + (size / 2);
							} else {
								y = wall[1] - (size / 2);
							}
						}
					}
					if ( !( ((rightx > wall[0]) == (leftx > wall[0])) && ((rightx > wall[2]) == (leftx > wall[2])) ) && ( (bottomy > wall[1]) == (topy < wall[3]))) {
						if (bounces == 0) {
							destroy();
						} else {
							movex = false;
							xspeed *= -1;
							bounces--;
							if (nx > wall[2]) {
								x = wall[2] + (size / 2);
							} else {
								x = wall[0] - (size / 2);
							}
						}
					}
				}
			}
			
			if (movex && live) {
				x = nx;
			}
			if (movey && live) {
				y = ny;
			}
			
			if (firstCheck) {
				firstCheck = false;
			} else {
				//enemy & player detection, along with their arrays of mines & projectiles
				for (Enemy tank: enemies) {
					if (tank.distanceFromTank((int)x, (int)y) < (size / 2) && tank.isLive()) {
						tank.destroy();
						destroy();
						//System.out.println("enemy hit");
					}
					checking = true;
					for (Projectile projs: tank.getProjectiles()) {
						if ((projs != null && projs.isLive()) && distFromProj(projs.getX(), projs.getY()) < projs.getSize() && !projs.isChecking()) {
							if (projs.isLive()) {
								projs.destroy();
								destroy();
							}
							//System.out.println("proj hit");
						}
					}
					checking = false;
					for (Mine mines: tank.getMines()) {
						if ((mines != null && mines.isLive()) && distFromProj(mines.getX(), mines.getY()) < mines.getSize()) {
							if (mines.isLive()) {
								mines.explode(walls, liveWalls, enemies, player);
								destroy();
							}
							//System.out.println("mine hit");
						}
					}
				}
				if (player.distanceFromTank((int)x, (int)y) < (size / 2)) {
					player.destroy();
					//System.out.println("player hit");
				}
				checking = true;
				for (Projectile projs: player.getProjectiles()) {
					if ((projs != null && projs.isLive()) && distFromProj(projs.getX(), projs.getY()) < projs.getSize() && !projs.isChecking()) {
						if (projs.isLive()) {
							projs.destroy();
							destroy();
						}
						//System.out.println("proj hit");
					}
				}
				checking = false;
				for (Mine mines: player.getMines()) {
					if ((mines != null && mines.isLive()) && distFromProj(mines.getX(), mines.getY()) < mines.getSize()) {
						if (mines.isLive()) {
							mines.explode(walls, liveWalls, enemies, player);
							destroy();
						}
						//System.out.println("mine hit");
					}
				}
			}
			
		}
	}
	
	//will probably be integrated into the move method
	public void checkWall() {
		
	}
	
	public void destroy() {
		live = false;
		maxspeed = 0;
	}
	
	public boolean isLive() {
		return live;
	}
	
	public int getX() {
		return (int)x;
	}
	
	public int getY() {
		return (int)y;
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isChecking() {
		return checking;
	}
	
	public double distFromProj(int x, int y) {
		return getDistance(x, y, getX(), getY());
	}
	
	private double getDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow( y2 - y1, 2) );
	}
	
	//extra detection to make sure that the projectile could pass corners, deemed unnecessary 
	
	/*
	private double distanceFromWallEdge(int[] wall) {
		double angle = Math.atan2(((wall[3]-wall[1])/2) - y, ((wall[2]-wall[0])/2) - x);
		angle %= Math.PI;
		if (angle == Math.PI/4 || angle == (3*Math.PI)/4) {
			return getDistance(wall[0], wall[1], wall[2], wall[3]);
		} else if ((angle < Math.PI/4) == angle < Math.PI/2) {
			//return Math.sqrt( Math.pow(size / 2, 2) + Math.pow(Math.sin(angle) * (size / 2), 2) );
			return getDistance(((wall[2]-wall[0])/2) + wall[0], ((wall[3]-wall[1])/2) + wall[1], wall[2], wall[1] + (((angle % Math.PI) / (Math.PI / 2)) * ((wall[3]-wall[1])/2)) );
		} else {
			//return Math.sqrt( Math.pow(Math.cos(angle) * (size / 2), 2) + Math.pow(size / 2, 2) );
			return getDistance(((wall[2]-wall[0])/2) + wall[0], ((wall[3]-wall[1])/2) + wall[1], wall[2] + (((angle % Math.PI) / (Math.PI / 2)) * ((wall[2]-wall[0])/2)), wall[1] );
		}
		
		
	}
	private double getDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow( y2 - y1, 2) );
	}
	
	private boolean touchingWall(int[] wall) {
		return getDistance((wall[2]-wall[0])/2, (wall[3]-wall[1])/2, x, y) - distanceFromWallEdge(wall) > size;
	}
	*/
}
