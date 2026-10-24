package com.endlessadventure.ui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.imageio.ImageIO;

/** a small picture made of color samples; the Canvas shows two vertical samples per character cell.
 *  images come ready-sized from the asset pipeline (tools/imagegen), so they are drawn 1:1 and never scaled here */
final class PixelImage {
	private static final Path ASSETS = Path.of("assets");
	private static final Map<Path, Optional<PixelImage>> cache = new HashMap<>();

	private final int width, height;
	private final int[] argb; //row by row, 0xAARRGGBB

	private PixelImage(int width, int height, int[] argb) {
		this.width = width;
		this.height = height;
		this.argb = argb;
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	/** the sample's 0xRRGGBB color, or -1 where the image is transparent */
	int getColor(int x, int y) {
		int c = argb[y * width + x];
		return (c >>> 24) < 128 ? -1 : c & 0xFFFFFF;
	}

	/** an item icon at its pixel size (12 for grid boxes, 24 for details), or empty if it has not been generated yet */
	static Optional<PixelImage> icon(String itemId, int size) {
		return load(ASSETS.resolve("icons").resolve(itemId + "-" + size + ".png"));
	}

	/** a scene backdrop for the adventure screen, or empty if there is none with this id */
	static Optional<PixelImage> scene(String sceneId) {
		return load(ASSETS.resolve("scenes").resolve(sceneId + ".png"));
	}

	/** a full-body character portrait for the paper doll, standing on transparency, or empty if there is none with this id */
	static Optional<PixelImage> portrait(String portraitId) {
		return load(ASSETS.resolve("portraits").resolve(portraitId + ".png"));
	}

	/** a figure or prop that stands on the battle map (player, bandit, forest_tree), standing on its bottom edge,
	 *  or empty if there is none with this id */
	static Optional<PixelImage> figure(String figureId) {
		return load(ASSETS.resolve("figures").resolve(figureId + ".png"));
	}

	/** the ground of a whole battle map, 12 x 12 pixels per cell, or empty if there is none with this id */
	static Optional<PixelImage> ground(String groundId) {
		return load(ASSETS.resolve("grounds").resolve(groundId + ".png"));
	}

	/** the leftmost and rightmost solid columns in the bottom share of the rows (0 to 1): what the figure stands on,
	 *  used for the width of its ground shadow; {-1, -1} if those rows are empty */
	int[] footprint(double share) {
		int left = -1, right = -1;
		for (int y = height - Math.max(1, (int) Math.round(height * share)); y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (getColor(x, y) >= 0) {
					left = left < 0 ? x : Math.min(left, x);
					right = Math.max(right, x);
				}
			}
		}
		return new int[] {left, right};
	}

	/** a picture for behind a whole menu screen, drawn inside the screen frame, or empty if there is none with this id */
	static Optional<PixelImage> backdrop(String backdropId) {
		return load(ASSETS.resolve("backdrops").resolve(backdropId + ".png"));
	}

	/** reads a PNG once and keeps it; a missing or unreadable file gives empty, so screens fall back to their placeholder */
	private static Optional<PixelImage> load(Path path) {
		return cache.computeIfAbsent(path, p -> {
			try {
				return Optional.of(read(p));
			} catch (IOException e) {
				//a broken asset is silently treated as missing; log it if assets start failing in practice
				return Optional.empty();
			}
		});
	}

	private static PixelImage read(Path path) throws IOException {
		if (!Files.exists(path)) {
			throw new NoSuchFileException(path.toString());
		}
		BufferedImage image = ImageIO.read(path.toFile());
		if (image == null) {
			throw new IOException("not an image: " + path);
		}
		int w = image.getWidth(), h = image.getHeight();
		return new PixelImage(w, h, image.getRGB(0, 0, w, h, null, 0, w));
	}
}
