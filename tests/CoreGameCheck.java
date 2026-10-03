package com.endlessadventure;

import com.endlessadventure.entity.Player;
import com.endlessadventure.Story.Scene;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Run via check-core.ps1: all file operations must stay in its fresh temporary directory. */
public final class CoreGameCheck {
    private static final SaveManager SAVES = new SaveManager();

    public static void main(String[] args) throws Exception {
        Path working = Path.of("").toAbsolutePath();
        check(working.getFileName().toString().startsWith("endless-core-check-")
                && !Files.exists(Path.of("saves")), "Use a fresh isolated check directory");
        checkAttributes();
        checkSaves();
        checkHelp();
        checkMenus();
        checkRendering();
        System.out.println("PASS: core state, save failures, overwrite confirmation, navigation, and input checks");
    }

    private static void checkAttributes() {
        Player player = new Player("  Jacob  ");
        check(player.getName().equals("Jacob"), "Name normalization");
        for (String name : new String[] {null, "", " ", "x".repeat(41), "bad\tname"}) {
            reject(() -> player.setName(name));
        }
        for (double value : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            reject(() -> player.setHp(value));
            reject(() -> player.setMaxHp(value));
            reject(() -> player.setAttack(value));
            reject(() -> player.takeDamage(value));
            reject(() -> player.RestoreHP(value));
        }
        reject(() -> player.setMaxHp(0));
        reject(() -> player.setArmor(Double.NaN));
        reject(() -> player.setArmor(Double.POSITIVE_INFINITY));
        player.setArmor(-2);
        check(player.getHp() == 40 && player.getMaxHp() == 40, "Rejected operations preserve HP");
        player.setMaxHp(10);
        check(player.getHp() == 10, "Reducing maximum HP clamps current HP");
        check(!player.takeDamage(100) && player.getHp() == 0, "Lethal damage clamps HP");
        player.RestoreHP(Double.MAX_VALUE);
        check(player.getHp() == 10 && player.isAlive(), "Healing cannot exceed max HP");
        player.setEnergy(5);
        player.setMaxEnergy(2);
        check(player.getEnergy() == 2, "Reducing energy cap clamps energy");
        reject(() -> player.setMaxEnergy(-1));
        reject(() -> player.setEnergy(3));
        reject(() -> player.setExp(Player.xpToNext(1)));
        player.setExp(10);
        reject(() -> player.setLevel(Player.MAX_LEVEL));
        check(player.getLevel() == 1 && player.getExp() == 10, "Invalid level change preserves progress");
        player.setExp(0);
        player.setLevel(Player.MAX_LEVEL);
        reject(() -> player.setExp(1));
        System.out.println("PASS: names, finite attributes, damage/healing bounds, level/EXP consistency");
    }

    private static void checkSaves() throws Exception {
        check(SAVES.readSummary(1).status() == SaveManager.SlotStatus.EMPTY, "Missing slot is empty");
        Player player = new Player("Original", 60, 32, 2, 8, -1, 6, 4, 17);
        SAVES.writeSave(1, new GameState(player, new Scene(), 8));
        GameState loaded = SAVES.loadSave(1);
        check(loaded.getPlayer().getName().equals("Original") && loaded.getTurn() == 8
                && loaded.getPlayer().getHp() == 32 && loaded.getPlayer().getExp() == 17
                && loaded.getPlayer().getEnergy() == 4 && loaded.getPlayer().getArmor() == -1,
                "Save/load preserves current player fields");
        String good = Files.readString(Path.of("saves/slot1.txt"));
        for (String bad : new String[] {
                "name=Incomplete\nlevel=1\nturn=1\n",
                good.replace("hp=32.0", "hp=NaN"),
                good.replace("hp=32.0", "hp=99"),
                good.replace("EXP=17", "EXP=999"),
                good.replace("name=Original", "name="),
                good.replace("turn=8", "turn=0"),
                good.replace("energy=4", "energy=99"),
                good.replace("name=Original", "name=\\uZZZZ")}) {
            Files.writeString(Path.of("saves/slot2.txt"), bad);
            check(SAVES.readSummary(2).status() == SaveManager.SlotStatus.CORRUPT,
                    "Malformed save must not be shown as readable");
            try {
                SAVES.loadSave(2);
                throw new AssertionError("Malformed save was accepted");
            } catch (BadSaveException expected) {
                check(expected.getSlot() == 2 && expected.getCause() != null, "Bad save retains slot and cause");
            }
            check(SAVES.readAllSummaries()[0].status() == SaveManager.SlotStatus.READABLE,
                    "Bad slot must not hide good slots");
        }
        SAVES.writeSave(1, new GameState(new Player("Replacement")));
        check(SAVES.loadSave(1).getPlayer().getName().equals("Replacement"), "Overwrite succeeds");

        Path blocked = Files.createDirectory(Path.of("saves/slot3.txt"));
        Path marker = Files.writeString(blocked.resolve("keep.txt"), "keep");
        try {
            SAVES.writeSave(3, new GameState(new Player("Blocked")));
            throw new AssertionError("Writing to a directory must fail");
        } catch (IOException expected) {
            check(Files.readString(marker).equals("keep"), "Failed open preserves directory contents");
            check(SAVES.loadSave(1).getPlayer().getName().equals("Replacement"), "Other saves remain intact");
        } finally {
            Files.delete(marker);
            Files.delete(blocked);
        }
        System.out.println("PASS: save roundtrip, corrupt fields, overwrite, and file-open errors");
    }

    private static void checkHelp() throws Exception {
        clearSlots();
        for (String command : new String[] {"3", "[3]", "h", "help"}) {
            String output = runGame(command + "\nb\n4\n", SAVES);
            exited(output);
            check(output.contains("HOW TO PLAY"), "Main menu opens help");
        }
        String output = runGame("1\nHelpCheck\nh\nhelp\nc\nh\nhelp\ns\nh\nhelp\nb\nb\ny\n4\n", SAVES);
        exited(output);
        check(!output.contains("HOW TO PLAY") && output.contains("Unknown Command"),
                "Character creation, adventure, and save selection do not open help");
        output = runGame("2\nh\nhelp\n", SAVES);
        check(!output.contains("HOW TO PLAY") && output.contains("Unknown Command") && output.contains("Input closed"),
                "Load selection does not open help");
        System.out.println("PASS: help is only accessible from the main menu and returns there");
    }

    private static void checkMenus() throws Exception {
        clearSlots();
        check(runGame("", SAVES).contains("Input closed"), "EOF exits cleanly");
        String output = runGame("1\n\n" + "x".repeat(120) + "\nValid\nc\nb\ny\n4\n", SAVES);
        exited(output);
        check(output.contains("Name must contain") && SAVES.loadSave(1).getPlayer().getName().equals("Valid"),
                "Invalid names rejected before saving");

        for (String input : new String[] {
                "2\nb\n4\n",
                "1\nDraft\nc\nb\nb\n4\n"}) {
            exited(runGame(input, SAVES));
        }
        for (String yes : new String[] {"y", "yes"}) {
            SAVES.writeSave(1, new GameState(new Player("Before")));
            exited(runGame("1\nAfter\nc\n1\n" + yes + "\nb\ny\n4\n", SAVES));
            check(SAVES.loadSave(1).getPlayer().getName().equals("After"), "Both yes answers allow overwrite");
        }
        byte[] before = Files.readAllBytes(Path.of("saves/slot1.txt"));
        exited(runGame("1\nCancelled\nc\n1\nn\nb\nb\n4\n", SAVES));
        check(Arrays.equals(before, Files.readAllBytes(Path.of("saves/slot1.txt"))), "Cancel keeps original bytes");

        clearSlots();
        String bad = "name=Broken\nlevel=oops\nturn=1\n";
        Files.writeString(Path.of("saves/slot1.txt"), bad);
        output = runGame("2\n1\nb\n4\n", SAVES);
        exited(output);
        check(output.contains("Unreadable Save") && output.contains("is invalid"), "Bad slot remains navigable");
        exited(runGame("1\nDraft\nc\nb\nb\n4\n", SAVES));
        check(Files.readString(Path.of("saves/slot1.txt")).equals(bad), "Only corrupt saves still require slot selection");
        exited(runGame("1\nRecovered\nc\n1\nyes\nb\ny\n4\n", SAVES));
        check(SAVES.loadSave(1).getPlayer().getName().equals("Recovered"), "Explicitly replace unreadable save");

        clearSlots();
        SaveManager failing = new SaveManager() {
            @Override
            public void writeSave(int slot, GameState state) throws IOException {
                throw new IOException("Simulated write failure");
            }
        };
        output = runGame("1\nDraft\nc\nb\nb\n4\n", failing);
        exited(output);
        check(output.contains("Simulated write failure") && output.contains("SAVE GAME")
                && !output.contains("Saved to slot"), "Failed first save stays in the save flow");
        System.out.println("PASS: menus, invalid input, overwrite/cancel, and failed first save");
    }

    private static void checkRendering() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream previous = System.out;
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            UIHandler ui = new UIHandler();
            ui.renderLoadSaveUI(new SaveManager.SlotOverview[] {
                    new SaveManager.SlotOverview(1, SaveManager.SlotStatus.READABLE, "x".repeat(250), 1, 1),
                    new SaveManager.SlotOverview(2, SaveManager.SlotStatus.EMPTY, null, 0, 0),
                    new SaveManager.SlotOverview(3, SaveManager.SlotStatus.CORRUPT, null, 0, 0)});
        } finally {
            System.setOut(previous);
        }
        for (String row : bytes.toString(StandardCharsets.UTF_8).split("\\R")) {
            if (row.startsWith("║")) {
                check(row.length() == UIHandler.UI_WIDTH && row.endsWith("║"), "Text rows keep their fixed width");
            }
        }
        System.out.println("PASS: long unbroken text and all save-slot rows stay within the frame");
    }

    private static String runGame(String input, SaveManager saves) {
        var oldInput = System.in;
        var oldOutput = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(capture);
            // The adventure's LLM call is not enabled yet; this check performs no network requests.
            new GameEngine(new UIHandler(), saves, null).run();
        } finally {
            System.setIn(oldInput);
            System.setOut(oldOutput);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void clearSlots() throws IOException {
        for (int slot = 1; slot <= SaveManager.SLOT_COUNT; slot++) {
            Files.deleteIfExists(Path.of("saves", "slot" + slot + ".txt"));
        }
    }

    private static void exited(String output) {
        check(output.contains("Game Exited") && !output.contains("Input closed"), "Commands must reach explicit exit");
    }

    private static void reject(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected invalid input to be rejected");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
