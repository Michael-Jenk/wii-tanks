import java.awt.Color;

public class Player extends Tank {
	
	private static final Color playerColor = new Color(0, 36, 214);
	private static final double playerMaxSpeed = 2.5;
	private static final double playerProjSpeed = 2.2;
	private static final int playerProjNumbers = 3;
	private static final double playerShotCooldown = 1.0;
	private static final int playerMines = 2;
	
	private boolean N;
	private boolean E;
	private boolean S;
	private boolean W;
	
	private boolean live = true;
	
	public Player(int x, int y) {
		super(x, y, playerColor, playerMaxSpeed, playerProjSpeed, 1, playerShotCooldown, playerProjNumbers, playerMines);
		setCannonTarget(x + 1, y);
	}
	
	public void playerMove(int[][] walls, boolean[] liveWalls, Enemy[] tanks, double dt) {
		if (N && !S) {
			setYSpeed(-playerMaxSpeed);
		} else if (!N && S) {
			setYSpeed(playerMaxSpeed);
		} else {
			setYSpeed(0);
		}
		if (E && !W) {
			setXSpeed(playerMaxSpeed);
		} else if (!E && W) {
			setXSpeed(-playerMaxSpeed);
		} else {
			setXSpeed(0);
		}
		move(walls, liveWalls, tanks, this, dt, true);
	}
	
	public void playerShoot() {
		shoot(cannonDir, playerProjSpeed);
	}
	
	// 0 is up, 1 is right, 2 is down, 3 is left
	public void setMovement(int dir, boolean move) {
		if (dir == 0) {
			N = move;
		} else if (dir == 1) {
			E = move;
		} else if (dir == 2) {
			S = move;
		} else if (dir == 3) {
			W = move;
		}
	}
	
	public void toggleDebug() {
		debug = !debug;
	}
	
	public void setCannonTarget(int tx, int ty) {
		setCannonDir(Math.atan2(getY() - ty, getX() - tx) + Math.PI);
	}
	
	
	public void destroy() {
		live = false;
	}
	
	public boolean isLive() {
		return live;
	}
	
}
