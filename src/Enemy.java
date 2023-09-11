import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;

import java.util.Random;

public class Enemy extends Tank {
	
	final double cannonTurnSpeed = (2 * Math.PI) / 6; //in radians per second, 3 seconds to turn completely
	
	final Random rand = new Random();
	
	// I still need to balance out these variables
	
	static final double slowMoveSpeed = 2.5;
	static final double normalMoveSpeed = 3.0;
	
	static final double normalProjSpeed = 2.7;
	static final double fastProjSpeed = 6.8;
	
	static final double slowFireRateCooldown = 1;
	static final double fastFireRateCooldown = 0.5;
	
	//	Tank types:
	//
	//	Number	Tank	First appearance	Movement	Behaviour	Bullet speed	Fire rate	Ricochets	Bullets		Mines
	//	0		Brown	Mission 1			Stationary	Passive		Normal			Slow		1			1			-
	//	1		Grey	Mission 2			Slow		Defensive	Normal			Slow		1			1			-
	//	2		Teal	Mission 5			Slow		Defensive	Fast			Slow		-			1			-
	//	3		Yellow	Mission 8			Normal		Offensive	Normal			Slow		1			1			4
	//	4		Red		Mission 10			Slow		Offensive	Normal			Fast		1			3			-
	
	int type;
	
	double eCurrSpeed;
	double eMaxSpeed;
	int behaviour;
	double projSpeed;
	double projCooldown;
	int bounces;
	
	final double randCannonSpread = Math.toRadians(20);
	final double cannonPrecision = 0.1;
	double targetCannonDir;
	int[] targetpos;
	double targetAngle;
	final double targetPosPrecision = 15; // approximate distance from center of tank to the target node before it's considered "there"
	int playerNearestNode;
	boolean movingTowardsMapNode = true;
	final double randDirMoveTime = 0.75;
	double ttimer = 0;
	
	final double defMinRange = 80; // in degrees
	final double defMaxRange = 110; // in degrees
	final double offRange = 30;
	
	boolean live;

	double targetCannonAngle = 0;
	
	private static final double[][] types = {
		//	{moveSpeed	 		projSpeed 			projCooldown 			bounces	bullets	mines	behaviour}
			{0, 				normalProjSpeed,	slowFireRateCooldown,	1,		1,		0,		0},
			{slowMoveSpeed,		normalProjSpeed,	slowFireRateCooldown,	1,		1,		0,		1},
			{slowMoveSpeed,		fastProjSpeed,		slowFireRateCooldown,	0,		1,		0,		1},
			{normalMoveSpeed,	normalProjSpeed, 	slowFireRateCooldown,	1,		1,		4,		2},
			{slowMoveSpeed,		normalProjSpeed,	fastFireRateCooldown,	1,		3,		0,		2}};
	static final Color[] colors = {new Color(207, 127, 0), new Color(83, 83, 83), new Color(0, 194, 152).darker(), new Color(240, 232, 0), new Color(247, 35, 102)};
	
	public Enemy(int x, int y, int type) {
		super (x, y, colors[type], (int)types[type][0], types[type][1], (int)types[type][3], types[type][2], (int)types[type][4], (int)types[type][5]);
		this.eMaxSpeed = (int)types[type][0];
		this.projSpeed = types[type][1];
		this.projCooldown = types[type][2];
		this.bounces = (int)types[type][3];
		this.type = type;
		this.behaviour = (int)types[type][6];
		live = true;
		debug = false;
	}
	
	public void enemyMove(int[][] walls, boolean[] liveWalls, int[][] nodes, Enemy[] enemies, Player player, double dt) {
		if (live) {
			enemyAI(walls, liveWalls, nodes, enemies, player, dt);
		} else {
			projMove(walls, liveWalls, enemies, player);
			mineMove(dt, walls, liveWalls, enemies, player);
		}
	}
	
	private void enemyAI(int[][] walls, boolean[] liveWalls, int[][] nodes, Enemy[] enemies, Player player, double dt) {
		ttimer -= dt;
		
		if (!passesWall(walls, liveWalls, player.getX(), player.getY())) {
			//if the player is seen, then aim the cannon towards the player, and do different things depending on behaviour
			
			// shoot if aiming somewhere near player, else turn cannon at player (made it into it's own method)
			turnAndShootCannon(Math.atan2(player.getY() - y, player.getX() - x), dt, true);
			if(targetpos!=null&&debug){System.out.println("" + (getDistance(x, y, targetpos[0], targetpos[1]) < targetPosPrecision) + ", " + (movingTowardsMapNode) + ", " + (ttimer < 0) + ". x: " + x + ", targetX: " + targetpos[0]);}
			if ((targetpos == null || getDistance(x, y, targetpos[0], targetpos[1]) < targetPosPrecision) || movingTowardsMapNode || ttimer < 0) {
				ttimer = randDirMoveTime + ((rand.nextInt(30)) / 100.0); // basically a big check to see 
				boolean blockedByWall = true;
				
				double ang = Math.atan2(player.getX() - x, player.getY() - y);
				ang %= Math.PI * 2;
				if (ang < 0) {
					ang += Math.PI * 2;
				}
				while (blockedByWall) {
					if (behaviour == 1) {
						// if it's a defensive tank, then slowly move somewhere close to the player , a short distance in a 
						// split cone,  moing somewhere tangent to the player
						ang += rand.nextBoolean() ? Math.toRadians((defMinRange / 2) + rand.nextInt(((int)(defMaxRange - defMinRange) / 2 * 10) + 1)/10.0): Math.toRadians((defMinRange / 2) + rand.nextInt(((int)(defMaxRange - defMinRange) / 2 * 10) + 1)/10.0) * -1;
					} else if (behaviour == 2) {
						// if it's an offensive tank, then approach the player in a cone facing them
						ang += rand.nextBoolean() ? Math.toRadians((offRange / 2) + 1) : Math.toRadians((offRange / 2) + 1) * -1;
					}
					
					targetpos = new int[2];
					double dist = rand.nextInt(150) + 50.0;
					targetpos[0] = (int)(x + (dist * Math.sin(ang))); // idk why i have to flip sine and cos but it just works :,)
					targetpos[1] = (int)(y + (dist * Math.cos(ang))); 
					//collision detection to make sure that the new target position isn't in a wall
					blockedByWall = passesWall(walls, liveWalls, targetpos[0], targetpos[1]);
				}
				//shortens it so the node doesnt put part of the tank in the wall
				targetpos[0] -= (int)((size / 2) * Math.sqrt(2) * Math.sin(ang));
				targetpos[1] -= (int)((size / 2) * Math.sqrt(2) * Math.cos(ang));
			}
			eCurrSpeed = eMaxSpeed * 0.6;
			movingTowardsMapNode = false;
			
		} else {
			// if the player isn't seen, then aim the cannon to an angle somewhere near the player's location
			if (Math.abs(targetCannonAngle - cannonDir) < cannonPrecision) {
				targetCannonAngle = Math.atan2(player.getY() - y, player.getX() - x) + (rand.nextInt((int)(randCannonSpread * 20) + 1) / 10.0) - randCannonSpread;
				targetCannonAngle %= 2 * Math.PI;
				if (targetCannonAngle < 0) {
					targetCannonAngle += Math.PI * 2;
				}
			} else {
				// aim within the general direction of the player, but don't shoot
				turnAndShootCannon(targetCannonAngle, dt, false);
			}
			// move towards target node, and if: (a) it's already there, (b) it doesn't have a target node,
			// or (c) the player's nearest node has changed, then set a new target node
			
			if (targetpos == null || getDistance(x, y, targetpos[0], targetpos[1]) < targetPosPrecision || playerNearestNode != player.findNearestNode(nodes, walls, liveWalls)) {
				setPlayerTargetPos(walls, liveWalls, nodes, player);
			}
			eCurrSpeed = eMaxSpeed;
			movingTowardsMapNode = true;
		}
		
		//TODO: add code to make enemy tanks avoid projectiles
		
		//TODO: add AI for placing a mine
		
		// set x and y speed based on target position
		targetAngle = Math.atan2(targetpos[1] - y, targetpos[0] - x);
		targetAngle %= Math.PI * 2;
		if (targetAngle < 0) {
			targetAngle += Math.PI * 2;
		}
		xspeed = eCurrSpeed * Math.cos(targetAngle);
		yspeEd = eCurrSpeed * Math.sin(targetAngle);
		move(walls, liveWalls, enemies, player, dt, false);
	}
	
	private void setPlayerTargetPos(int[][] walls, boolean[] liveWalls, int[][] nodes, Player player) {
		// gets/resets the player's nearest map node
		playerNearestNode = player.findNearestNode(nodes, walls, liveWalls);
		if(debug){System.out.println("player nearest node: " + playerNearestNode);}
		
		int[] nearbyNodes = findAllNodes(nodes, walls, liveWalls);
		//		goes throught all the nearest nodes, and finds the shortest path around the loop to get to the player from each node
		int bestNodeIndex = -1;
		double bestNodeDistance = -1;
		for (int i = 0; i < nearbyNodes.length; i++) {
			// first, it discards a node if the enemy tank is extremely close to it, to avoid this wierd jittering bug
			if (getDistance(x, y, nodes[nearbyNodes[i]][0], nodes[nearbyNodes[i]][1]) < targetPosPrecision) {
				continue;
			}
			
			// starts at the visible node, and goes around the loop from lowest to highest array number. It stores the combined 
			// distance needed to travel throughout the loop to the player's node
			int currNode = nearbyNodes[i];
			double increasingLoopTotalDistance = getDistance(x, y, nodes[currNode][0], nodes[currNode][1]);
			while (currNode != playerNearestNode) {
				increasingLoopTotalDistance += currNode == nodes.length - 1 ? getDistance(nodes[0][0], nodes[0][1], nodes[currNode][0], nodes[currNode][1]) : getDistance(nodes[currNode + 1][0], nodes[currNode + 1][1], nodes[currNode][0], nodes[currNode][1]);
				currNode++;
				if (currNode >= nodes.length) {
					currNode = 0;
				}
			}
			
			//does same as before, but backwards
			currNode = nearbyNodes[i];
			double decreasingLoopTotalDistance = getDistance(x, y, nodes[currNode][0], nodes[currNode][1]);
			while (currNode != playerNearestNode) {
				decreasingLoopTotalDistance += currNode == 0 ? getDistance(nodes[nodes.length - 1][0], nodes[nodes.length - 1][1], nodes[currNode][0], nodes[currNode][1]) : getDistance(nodes[currNode - 1][0], nodes[currNode - 1][1], nodes[currNode][0], nodes[currNode][1]);
				currNode--;
				if (currNode < 0) {
					currNode = nodes.length - 1;
				}
			}
			
			// chooses the route's shortest total distance
			double shortestTotalDistance = Math.min(increasingLoopTotalDistance, decreasingLoopTotalDistance);
			
			// compares to see if this shortest total distance is shorter than the current shortest path, and sets it if needed
			if (bestNodeDistance == -1 || (bestNodeDistance > shortestTotalDistance + targetPosPrecision)) {
				bestNodeIndex = nearbyNodes[i];
				bestNodeDistance = shortestTotalDistance;
				if(debug){System.out.println("success, target node: " + bestNodeIndex);}
			}
		}
		
		// with the best node decided, sets the target x & y position to the node's position
		targetpos = nodes[bestNodeIndex];
		if(debug){System.out.println("target node: " + bestNodeIndex);}
		
	}
	
	private void turnAndShootCannon(double targAngle, double dt, boolean shoot) {
		targAngle %= 2 * Math.PI;
		if (targAngle < 0) {
			targAngle += Math.PI * 2;
		}
		if (Math.abs(targAngle - cannonDir) < cannonPrecision) {
			if (shoot) {
				shoot(cannonDir, projSpeed);
			}
		} else {
			if (Math.abs(targAngle - cannonDir) > cannonTurnSpeed * dt) {
				//if (targetCannonDir - cannonDir > 0) {
				//if (Math.abs((targetCannonDir + (2 * Math.PI)) - cannonDir) < Math.abs((targetCannonDir - (2 * Math.PI)) - cannonDir)) {
				if ((Math.abs(targAngle) - cannonDir < Math.PI && targAngle - cannonDir > 0) || ( targAngle < Math.PI && (cannonDir > Math.PI && cannonDir - targAngle > Math.PI))) {
					setCannonDir(getCannonDir() + (cannonTurnSpeed * dt));
				} else {
					setCannonDir(getCannonDir() - (cannonTurnSpeed * dt));
				}
			} else {
				setCannonDir(targAngle);
			}
			
		}
	}
	
	// returns true if the line fromm the target position to the tank is blocked by a wall
	private boolean passesWall(int[][] walls, boolean[] liveWalls, int x, int y) {
		Line2D nLine = new Line2D.Double(this.x, this.y, x, y);
		boolean passesWall = false;
		Line2D wallT;
		Line2D wallL;
		Line2D wallB;
		Line2D wallR;
		for (int j = 0; j < walls.length; j++) {
			//uses Java's line2D class to determine if there's any live walls in between the enemy tank and the player
			if (liveWalls[j] && walls[j][4] != 2) {
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
		return passesWall; // placeholder
	}
	
	public void enemyDraw(Graphics g) {
		if (live) {
			draw(g);
		}
	}
	
	public void destroy() {
		live = false;
		// do I need to add anything to the enemy tank destruction method?
	}
	
	public boolean isLive() {
		return live;
	}
	
	public int getType() {
		return type;
	}
	
}
