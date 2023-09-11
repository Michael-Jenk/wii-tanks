import java.io.File;
import java.io.IOException;
  
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Music {
	
	private Clip clip;
	private AudioInputStream aiStream;
	
	private boolean isPlaying;
	private String currentTrack;
	
	private int priorityTank;
	
	public Music() {
		this.isPlaying = false;
		this.currentTrack = "";
	}
	
	//
	// *******************These are just presets made to play specific music thoughout the game***********************
	//
	
	public void playLifeScreenMusic() {
		play("Music\\052 - Round Start.wav");
	}
	
	public void playPreviewMusic(Enemy[] enemies, boolean[] liveEnemies) {
		priorityTank = 0;
		for (Enemy tank : enemies) {
			if (tank.getType() > priorityTank) {
				priorityTank = tank.getType();
			}
		}
		
		play(getVariationName(enemies, liveEnemies));
		
		
	}
	
	public void loopGameplayMusic() {
		System.out.println(clip.getLongFramePosition());
		clip.setLoopPoints(177000, 530000);
		clip.loop(100);
	}
	
	public int getFrame() {
		return (int)(clip.getLongFramePosition());
	}
	
	public void updateGameplayMusic(Enemy[] enemies, boolean[] liveEnemies) {
		if (getVariationName(enemies, liveEnemies).equals(currentTrack)) {
			return;
		}
		int frame = (int)((clip.getLongFramePosition() - 177000) % 353000);
		clip.stop();
		play(getVariationName(enemies, liveEnemies));
		if (getVariationName(enemies, liveEnemies).equals("Music\\059 - Variation 4.wav")) {
			clip.setFramePosition(frame + 530000 + 10);
			clip.setLoopPoints(530000, 883000);
		} else {
			clip.setFramePosition(frame + 177000 + 10);
			clip.setLoopPoints(177000, 530000);
		}
		clip.loop(100);
		System.out.println("switched track to " + getVariationName(enemies, liveEnemies));
	}
	
	public void playPostviewMusic(boolean isLive, int lives) {
		if (isLive) {
			play("Music\\055 - Round End.wav");
		} else if (lives > 0) {
			play("Music\\054 - Round Failure.wav");
		} else {
			play("Music\\066 - Game Over.wav");
		}
	}
	
	public void loopResultsMusic() {
		play("Music\\068 - Results.wav");
		clip.setLoopPoints(0, 705600);
		clip.loop(100);
	}
	
	public boolean isPlaying() {
		isPlaying = clip.isRunning();
		return isPlaying;
	}
	
	private String getVariationName(Enemy[] enemies, boolean[] liveEnemies) {
		switch(priorityTank) {
		case 0: //brown tank
			return "Music\\053 - Variation 1.wav";
		case 1: //gray tank
			if (getNumbofTanksLeft(liveEnemies) >= 3) {
				return "Music\\057 - Variation 3.wav";
			} else {
				return "Music\\056 - Variation 2.wav";
			}
		case 2: //teal tank
			if (lastTank(enemies, liveEnemies, 2) && priorityTank == 2) {
				return "Music\\059 - Variation 4.wav";
			}
			return "Music\\Wii Play - Tanks (Variation 4) [Wii Play OST].wav";
		}
		
		System.out.println("error");
		return "";
	}
	
	private int getTypeLiveTanks (Enemy[] enemies, boolean[] liveEnemies, int targetType) {
		int a = 0;
		for (int i = 0; i < enemies.length; i++) {
			if (enemies[i].getType() == targetType && liveEnemies[i]) {
				a++;
			}
		}
		return a;
	}
	
	private int getNumbofTanksLeft(boolean[] liveEnemies) {
		int a = 0;
		for (boolean live : liveEnemies) {
			if (live) {
				a++;
			}
		}
		return a;
	}
	
	private boolean lastTank (Enemy[] enemies, boolean[] liveEnemies, int targetType) {
		return getNumbofTanksLeft(liveEnemies) == 1 && getTypeLiveTanks(enemies, liveEnemies, targetType) == 1;
	}
	
	//
	// *******************This code is to actually play said music, and where I actually use clip and stuff**********************
	//
	
	private void openMusic(String filename) {
		this.currentTrack = filename;
		
		try {
			
			aiStream = AudioSystem.getAudioInputStream(new File(currentTrack).getAbsoluteFile());
			
			clip = AudioSystem.getClip();
			
			clip.open(aiStream);
			
		} catch (UnsupportedAudioFileException e) {
			System.out.println("Unsupported Audio File Exception");
		} catch (IOException e) {
			System.out.println("IO Exception");
		} catch (LineUnavailableException e) {
			System.out.println("Line Unavailable Exception");
		}
		
	}
	
	private void play(String filename) {
		if (currentTrack != filename) {
			openMusic(filename);
		}
		
		clip.start();
		isPlaying = true;
	}
	
	public void stop() {
		if (isPlaying) {
			clip.stop();
			isPlaying = false;
		}
	}
	
	// ******************** Sources **********************
	//
	// Scruffy vid: https://www.youtube.com/watch?v=NkBXgcN3fXo
	// Scruffy chart: https://pbs.twimg.com/media/FI2DJ1jVIAInfBg?format=jpg&name=4096x4096
	// Java Clip Overview: https://docs.oracle.com/javase/7/docs/api/javax/sound/sampled/Clip.html
	
	
}
