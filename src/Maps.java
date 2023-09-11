import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class Maps {
	
	
	private int WIDTH = 1500;
	private int HEIGHT = 800;
	
	private int currMap;
	
	// {x1, y1, x2, y2, wall type (0 if normal, 1 if destructible, 2 if it can be shot over)}
	
	// list of the screen-border walls, included in every map
	private int wallWidth = 50;
	final private int[][] borders = { {0, 0, WIDTH, wallWidth, 0}, {0, HEIGHT - wallWidth, WIDTH, HEIGHT, 0}, {0, wallWidth, wallWidth, HEIGHT - wallWidth, 0}, {WIDTH - wallWidth, wallWidth, WIDTH, HEIGHT - wallWidth, 0} };
	
	// big list of wall locations for each mission in the game, 0 is a blank test map and 1 is the first mission
	final private int[][][] walls = {
			{borders[0], borders[1], borders[2], borders[3]}, 
			{borders[0], borders[1], borders[2], borders[3], {50, 500, 1450, 750, 0}, {400, 125, 450, 225, 0}, {400, 325, 450, 425, 0}, {725, 125, 775, 225, 0}, {725, 225, 775, 275, 1}, {725, 275, 775, 325, 1}, {725, 325, 775, 425, 0}},
			{borders[0], borders[1], borders[2], borders[3], {300, 250, 800, 300, 0}, {800, 250, 850, 300, 1}, {850, 250, 900, 300, 1}, {900, 250, 950, 300, 1}, {950, 250, 1000, 300, 1}, {1000, 250, 1050, 300, 1}, {650, 550, 1150, 600, 0}, {400, 550, 450, 600, 1}, {450, 550, 500, 600, 1}, {500, 550, 550, 600, 1}, {550, 550, 600, 600, 1}, {600, 550, 650, 600, 1} },
			{borders[0], borders[1], borders[2], borders[3], {275, 175, 400, 225, 0}, {400, 175, 750, 225, 1}, {750, 175, 800, 450, 0}, {700, 400, 750, 650, 0}, {750, 600, 1050, 650, 1}, {1050, 600, 1175, 650, 0} },
			{borders[0], borders[1], borders[2], borders[3], {50, 250, 440, 300, 2}, {50, 500, 890, 550, 2}, {500, 610, 550, 750, 2}, {950, 360, 1000, 750, 2}, {1060, 500, 1450, 550, 2}, {610, 250, 1450, 300, 2}, {950, 50, 1000, 190, 2}, {500, 50, 550, 440, 2} },
			{borders[0], borders[1], borders[2], borders[3], {200, 500, 250, 550, 1}, {250, 500, 300, 550, 0}, {250, 550, 300, 600, 1}, {1200, 150, 1250, 200, 1}, {1200, 200, 1250, 250, 0}, {1250, 200, 1300, 250, 1} },
			{borders[0], borders[1], borders[2], borders[3], {300, 150, 1200, 200, 0}, {300, 600, 1200, 650, 0}, {700, 200, 750, 425, 2}, {750, 375, 800, 600, 2} }
	};
	private boolean[] liveWalls;
	
	//big list of pathfinding nodes for each map
	final private int[][][] pathfindingNodes = {
			{ {750, 400} },
			{ {175, 50 + 35}, {450 + 162, 50 + 35}, {950, 50 + 35}, {950, 500 - 35}, {450 + 162, 500 - 35}, {175, 500 - 35} }, // 6-node loop from the top left clockwise to the bottom left
			//{ {175, 150}, {1250, 150}, {1250, 425}, {1250, 675}, {175, 675}, {175, 425} } // 6 node loop for mission 2, is more stable than the infinity sign but is less efficient 
			{ {175, 150}, {1250, 150}, {1250, 420}, {175, 430}, {175, 675}, {1250, 675}, {1250, 430}, {175, 420} }, // 8 node infinity sign loop
			{ {175, 125}, {900, 125}, {1375, 700}, {575, 700} }, // a 4 node trapezoid encompassing the wierd Z shaped map
			{ {750, 400} }, // only needed one node for this map bc all the walls (holes) are see-through
			{ {125, 125}, {1325, 125}, {1325, 675}, {125, 675} }, // 4 node square around the map
			{ {225, 100}, {1325, 100}, {1325, 400}, {1325, 700}, {225, 700}, {225, 400} } // 4 node square around the map
	};
	
	//list of player spawn locations for each mission
	final private int[][] pSpawns = {
			{WIDTH / 2, HEIGHT / 2}, //blank test map
			{200, 250},
			{175, 575},
			{175, 425},
			{275, 650},
			{225, 625},
			{225, 400}
	};
	
	//big list of enemy spawn locations & types: {x, y, type}
	final private int[][][] eSpawns = {
			{ {750, 400, 1} }, // test map
			{ {1100, 250, 0} },
			{ {1250, 150, 1} },
			{ {425, 125, 1}, {1075, 700, 1}, {1275, 400, 0} },
			{ {750, 100, 0}, {1225, 100, 1}, {750, 400, 1}, {1225, 400, 0} },
			{ {1000, 100, 2}, {1300, 400, 2} },
			{ {1175, 225, 1}, {1325, 400, 2}, {1355, 625, 2}, {1175, 700, 1} }
	};
	
	public Maps(int width, int height, int cMap) {
		this.WIDTH = width;
		this.HEIGHT = height;
		this.currMap = cMap;
		liveWalls = new boolean[walls[currMap].length];
		for (int i = 0; i < walls[currMap].length; i++) {
			liveWalls[i] = true;
		}
	}
	
	// wall & bg colors
	final Color bgColor = new Color(247, 217, 156);
	final Color wColor = new Color(232, 182, 81);
	final Color destColor = new Color(255, 145, 105);
	final Color holeColor = new Color(145, 108, 32, 122);
	final Color bannerColor = new Color(188, 227, 152);
	final Color bannerTextColor = new Color(49, 69, 30);
	
	//returns an array of the walls of the current map, combined with the defined borders
	public int[][] getWalls() {
		return walls[currMap];
	}
	
	public int[][] getNodes() {
		return pathfindingNodes[currMap];
	}
	
	public boolean[] getLiveWalls() {
		return liveWalls;
	}
	
	public int[] getpSpawn() {
		return pSpawns[currMap];
	}
	public int[][] geteSpawns() {
		return eSpawns[currMap];
	}
	
	public void drawMap(Graphics g) {
		g.setColor(bgColor);
		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(wColor);
		for (int i = 0; i < walls[currMap].length; i++) {
			if (liveWalls[i]) {
				if (walls[currMap][i][4] == 0) {
					g.setColor(wColor);
				} else if (walls[currMap][i][4] == 1) {
					g.setColor(destColor);
				} else if (walls[currMap][i][4] == 2) {
					g.setColor(holeColor);
				}
				g.fillRect(walls[currMap][i][0], walls[currMap][i][1], walls[currMap][i][2] - walls[currMap][i][0], walls[currMap][i][3] - walls[currMap][i][1]);
			}
		}
		if (currMap == 1) {
			g.setColor(bannerColor);
			g.fillRect(0, 550, WIDTH, 150);
			g.setColor(bannerColor.darker());
			g.fillRect(0, 553, WIDTH, 2);
			g.fillRect(0, 695, WIDTH, 2);
			g.setColor(bannerTextColor);
			g.setFont(new Font("Arial", 0, 40));
			g.drawString("Move the tank with WASD.", 500, 600);
			g.drawString("Aim and shoot with the mouse, right click to drop a mine.", 250, 650);
		}
		
	}
	
	public void drawNodes(Graphics g) {
		g.setColor(Color.RED);
		for (int[] location: pathfindingNodes[currMap]) {
			g.fillOval(location[0] - 4, location[1] - 4, 8, 8);
		}
	}
	
	public void resetMap() {
		for (int i = 0; i < walls[currMap].length; i++) {
			liveWalls[i] = true;
		}
	}
	
	
}
