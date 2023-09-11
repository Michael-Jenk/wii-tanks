import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;

public abstract class Tank {
	
	final int size = 50;
	final int radius = 42; // I was gonna use it for a lazy collision detection, will probably delete soon
	final int pSize = 16;
	final int cannonLength = 30;
	
	double x;
	double y;
	protected double xspeed;
	protected double yspeEd; // I capitalized the second E just to spite Austin lol
	private double maxspeed;
	
	protected Color color;
	protected double cannonDir;
	
	// toggles debug mode for an individual tank (probably the player)
	protected boolean debug = false;
	
	protected Projectile[] projectiles;
	double projSpeed;
	int projBounces;
	double projCooldown;
	double cooldown;
	
	protected Mine[] mines;
	
	public Tank(int x, int y, Color color, double maxSpeed, double pSpeed, int pBounces, double pCooldown, int projs, int mines) {
		this.x = x;
		this.y = y;
		this.color = color;
		this.maxspeed = maxSpeed;
		this.projSpeed = pSpeed;
		this.projBounces = pBounces;
		this.projCooldown = pCooldown;
		projectiles = new Projectile[projs];
		this.mines = new Mine[mines];
	}
	
	protected void draw(Graphics g) {
		mineDraw(g);
		g.setColor(color);
		g.fillRect((int)x - (size / 2), (int)y - (size / 2), size, size);
		g.setColor(color.darker());
		g.drawLine((int)x, (int)y, (int)(x + (cannonLength + pSize) * Math.cos(cannonDir)), (int)(y + (cannonLength + pSize) * Math.sin(cannonDir)));
		projDraw(g);
	}
	
	public void projDraw(Graphics g) {
		for (Projectile proj: projectiles) {
			if (proj != null && proj.isLive()) {
				proj.draw(g);
			}
		}
	}
	
	public void mineDraw(Graphics g) {
		for (Mine mine: mines) {
			if (mine != null && !(mine.isLive() && mine.isExploding())) {
				mine.draw(g);
			}
		}
	}
	
	protected void move(int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player, double dt, boolean capSpeed) {
		//TODO: add collision for other tanks
		
		//rather than change the x & y and then check if it's in a wall, i check if the changed x & y will work, and then apply it if it does
		double nx = x + (xspeed );
		double ny = y + (yspeEd );
		
		// limits the total speed 
		if (getDistance(0, 0, xspeed, yspeEd) > maxspeed && capSpeed) {
			nx = x + maxspeed * Math.cos(Math.atan2(yspeEd, xspeed));
			ny = y + maxspeed * Math.sin(Math.atan2(yspeEd, xspeed));
			if (debug ) {System.out.println("Speed capped: " + (nx - x) + ", " + (ny - y) + ". "); }
		}
		
		int rightx = (int)nx + (size/2);
		int leftx = (int)nx - (size/2);
		int topy = (int)ny - (size/2);
		int bottomy = (int)ny + (size/2);
		
		boolean movex = true;
		boolean movey = true;
		// collision detection for the walls, each wall is an int[4] {x1, y1, x2, y2}, top left is 1 and bottom right is 2
		for (int i = 0; i < walls.length; i++) {
			int[] wall = walls[i];
			if (liveWalls[i]) {
				//   (                 returns true if the tank passes an edge of the wall y-wise                ) && (returns true if the tank is anywhere in the wall x-wise)
				if ( !( ((topy <= wall[1]) == (bottomy <= wall[1])) && ((topy >= wall[3]) == (bottomy >= wall[3])) ) && ( (rightx > wall[0]) == (leftx < wall[2]))) {
					movey = false;
					if (debug) {System.out.println("Passed Y-edge of wall."); }
				}
				//   (                 returns true if the tank passes an edge of the wall x-wise                ) && (returns true if the tank is anywhere in the wall y-wise)
				if ( !( ((rightx >= wall[0]) == (leftx >= wall[0])) && ((rightx >= wall[2]) == (leftx >= wall[2])) ) && ( (bottomy > wall[1]) == (topy < wall[3]))) {
					movex = false;
					if (debug) {System.out.println("Passed X-edge of wall."); }
				}
				
			}
		}
		
		if (movex) {
			x = nx;
		}
		if (movey) {
			y = ny;
		}
		
		projMove(walls, liveWalls, enemies, player);
		mineMove(dt, walls, liveWalls, enemies, player);
		
	}
	
	public void projMove(int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player) {
		for (Projectile proj: projectiles) {
			if (proj != null && proj.isLive()) {
				proj.move(walls, liveWalls, enemies, player);
			}
		}
	}
	
	public void mineMove(double dt, int[][] walls, boolean[] liveWalls, Enemy[] enemies, Player player) {
		for (Mine mine: mines) {
			if (mine != null && !(mine.isLive() && mine.isExploding())) {
				mine.move(dt, walls, liveWalls, enemies, player);
			}
		}
	}
	
	//                in radians
	public void shoot(double dir, double projSpeed) {
		for (int i = 0; i < projectiles.length; i++) {
			if (projectiles[i] == null || !projectiles[i].isLive()) {
				if (debug) {System.out.println("shot"); }
				projectiles[i] = new Projectile( (int)(x + (cannonLength + pSize) * Math.cos(dir)), (int)(y + (cannonLength + pSize) * Math.sin(dir)), dir, projSpeed, projBounces  );
				break;
				//TODO: briefly stop tank after each shot
			}
		}
	}
	
	public void dropMine() {
		for (int i = 0; i < mines.length; i++) {
			if (mines[i] == null || !(mines[i].isLive() || mines[i].isExploding())) {
				if (debug) {System.out.println("dropped mine"); }
				mines[i] = new Mine((int)x, (int)y);
				break;
				//TODO: briefly stop tank after dropping mine
			}
		}
	}
	
	public abstract void destroy();
	
	// returns an array of all unobstructed nodes
	public int[] findAllNodes(int[][] nodes, int[][] walls, boolean[] liveWalls) {
		int[][] testNodes = nodes;
		boolean[] boolOfGoodNodes = new boolean[testNodes.length];
		for (int i = 0; i < testNodes.length; i++) {
			Line2D nLine = new Line2D.Double(x, y, testNodes[i][0], testNodes[i][1]);
			boolean passesWall = false;
			Line2D wallT;
			Line2D wallL;
			Line2D wallB;
			Line2D wallR;
			for (int j = 0; j < walls.length; j++) {
				if (liveWalls[j]) {
					//wall detection using the line2D's intersectsWith class. it checks if the explosion passes any of the wall's sides
					wallT = new Line2D.Double(walls[j][0], walls[j][1], walls[j][2], walls[j][1]);
					wallL = new Line2D.Double(walls[j][0], walls[j][1], walls[j][0], walls[j][3]);
					wallB = new Line2D.Double(walls[j][0], walls[j][3], walls[j][2], walls[j][3]);
					wallR = new Line2D.Double(walls[j][2], walls[j][1], walls[j][2], walls[j][3]);
					if (nLine.intersectsLine(wallT) || nLine.intersectsLine(wallL) || nLine.intersectsLine(wallB) || nLine.intersectsLine(wallR)) {
						passesWall = true;
					}
				} else {
					continue;
				}
				
			}
			if (passesWall) {
				//if it's blocked by a wall then its a bad node
				boolOfGoodNodes[i] = false;
				continue;
			} else {
				boolOfGoodNodes[i] = true;
			}
		}
		int w = 0;
		for (boolean gNode: boolOfGoodNodes) {
			if (gNode) {
				w++;
			}
		}
		int[] goodNodes = new int[w];
		int g = 0;
		for (int i = 0; i < testNodes.length; i++) {
			if (testNodes[i] != null && boolOfGoodNodes[i]) {
				goodNodes[g] = i;
				g++;
			}
		}
		return goodNodes; //placeholder
	}
	
	// returns index of the nearest node
	public int findNearestNode(int[][] nodes, int[][] walls, boolean[] liveWalls) {
		int[] goodNodes = findAllNodes(nodes, walls, liveWalls);
		int k = 0;
		for (int i = 0; i < goodNodes.length; i++ ) {
			if (getDistance(x, y, nodes[goodNodes[i]][0], nodes[goodNodes[i]][1]) < getDistance(x, y, nodes[goodNodes[k]][0], nodes[goodNodes[k]][1])) {
				k = i;
			}
		}
		return goodNodes[k];
	}
	
	//this method uses a rough radius of the tank to determine distance, prob won't use it 
	public double distanceFromTankR(int x, int y) {
		return getDistance(this.x, this.y, x, y) - radius;
	}
	
	// returns the distance from a point (x,y) to the edge of the tank, will return negative if it's inside the tank
	public double distanceFromTank(int x, int y) {
		return getDistance(this.x, this.y, x, y) - centerToEdge(Math.atan2(y - this.y, x - this.x));
	}
	
	//calculates distance from the center of the tank to the square's edge, given an angle in radians
	protected double centerToEdge(double angle) {
		angle %= Math.PI/2;
		angle = Math.abs(angle);
		if (angle == Math.PI/4) {
			return (size / 2) * Math.sqrt(2);
		} else if (angle < Math.PI/4) {
			//return Math.sqrt( Math.pow(size / 2, 2) + Math.pow(Math.sin(angle) * (size / 2), 2) );
			return Math.sqrt(Math.pow(size / 2 , 2) + Math.pow(((angle / (Math.PI / 4)) * (size/2)) , 2) );
		} else {
			//return Math.sqrt( Math.pow(Math.cos(angle) * (size / 2), 2) + Math.pow(size / 2, 2) );
			return Math.sqrt(Math.pow(size / 2 , 2) + Math.pow(((( (Math.PI / 4) - (angle % (Math.PI / 4) ) ) / (Math.PI / 4)) * (size/2)) , 2) );
		}
	}
	
	protected double getDistance(double x1, double y1, double x2, double y2) {
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow( y2 - y1, 2) );
	}
	
	public int getX() {
		return (int)x;
	}
	
	public int getY() {
		return (int)y;
	}
	
	public double getMaxSpeed() {
		return maxspeed;
	}
	
	protected void setXSpeed(double xspd) {
		this.xspeed = xspd;
	}
	
	protected void setYSpeed(double yspd) {
		this.yspeEd = yspd;
	}
	
	// in radians
	protected void setCannonDir(double dir) {
		this.cannonDir = dir;
		this.cannonDir %= 2 * Math.PI;
		if (this.cannonDir < 0) {
			this.cannonDir += 2 * Math.PI;
		}
	}
	
	public double getCannonDir() {
		return cannonDir;
	}
	
	public Projectile[] getProjectiles() {
		return projectiles;
	}
	
	public Mine[] getMines() {
		return mines;
	}
	
}
