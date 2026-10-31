package com.endlessadventure.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/** plays the sound effects and music loops in assets/sounds, made by tools/audiogen (ids are listed in its sounds.json).
 *  levels are set when the files are made, so nothing is mixed here. without an audio device, or for a missing
 *  or broken file, calls do nothing, so the game runs the same with or without sound */
public final class Sound {
	//BGM
	public static final String MENU = "menu",
							   ADVENTURE = "adventure",
							   COMBAT = "combat";
	
	//UI SFX
	public static final String UI_MOVE = "ui_move",
							   UI_CONFIRM = "ui_confirm",
							   UI_BACK = "ui_back",
							   UI_ERROR = "ui_error";
	
	//Combat SFX
	public static final String HIT = "hit",
							   MISS = "miss",
							   BLOCK = "block";
	
	private static final Path SOUNDS = Path.of("assets", "sounds");
	
	private static final Map<String, Optional<Clip>> cache = new HashMap<>();
	private static boolean noDevice;
	private static String musicId;
	private static Clip music;

	private Sound() {}

	/** plays a sound effect from its start; playing it again before it ends restarts it */
	public static void playEffect(String id) {
		clip(id).ifPresent(c -> {
			c.stop();
			c.setFramePosition(0);
			c.start();
		});
	}

	/** loops a music track until another track or stopMusic(); asking for the track already playing keeps it going */
	public static void playMusic(String id) {
		if (id.equals(musicId)) {
			return;
		}
		stopMusic();
		musicId = id;
		clip(id).ifPresent(c -> {
			c.setFramePosition(0);
			c.loop(Clip.LOOP_CONTINUOUSLY);
			music = c;
		});
	}

	public static void stopMusic() {
		if (music != null) {
			music.stop();
		}
		music = null;
		musicId = null;
	}

	/** releases the audio device; call once when the game exits */
	public static void close() {
		stopMusic();
		cache.values().forEach(c -> c.ifPresent(Clip::close));
		cache.clear();
	}

	/** reads a WAV once and keeps it open, ready to play */
	private static Optional<Clip> clip(String id) {
		if (noDevice) {
			return Optional.empty();
		}
		return cache.computeIfAbsent(id, i -> {
			try (AudioInputStream in = AudioSystem.getAudioInputStream(SOUNDS.resolve(i + ".wav").toFile())) {
				Clip c = AudioSystem.getClip();
				c.open(in);
				return Optional.of(c);
			} catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
				noDevice = true; //no usable output line: stay silent for the rest of the run
				return Optional.empty();
			} catch (IOException | UnsupportedAudioFileException e) {
				return Optional.empty(); //missing or unreadable file: only this sound is silent
			}
		});
	}
}
