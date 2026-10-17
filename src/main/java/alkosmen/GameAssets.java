package alkosmen;

import alkosmen.audio.MidiPlayer;
import alkosmen.audio.SoundEffectPlayer;
import alkosmen.gfx.CharacterSpriteAssets;
import alkosmen.gfx.SpriteSheet;
import alkosmen.settings.Constants;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Графические и звуковые ресурсы, загружаемые один раз при старте.
 *
 * Держит готовые <code>Image</code>, звуковые эффекты, MIDI-плеер и кадры анимаций,
 * чтобы игровой класс не занимался загрузкой файлов.
 */
public final class GameAssets {
    public final Image tileFloor;
    public final Image bottleSprite;
    public final Image cellarBackground;
    public final Image cellarChestSprite;
    public final Image npcBoy2Sprite;
    public final Image npcTolyaSprite;
    public final Image npcEboboSprite;
    public final Image npcCopSprite;
    public final Map<Character, Image[]> townNpcFrames = new HashMap<>();
    public final Image[] copWalkLeftFrames;
    public final Image[] copWalkRightFrames;
    public final Image[] copWalkUpFrames;
    public final Image[] copWalkDownFrames;
    public final Image[][] femaleCopFrames;
    public final Image[][] playerSprites;
    public final SoundEffectPlayer stepSound;
    public final SoundEffectPlayer bottleCollectSound;
    public final MidiPlayer levelMidi;

    public GameAssets() throws IOException {
        SpriteSheet sheet = new SpriteSheet("/alkosmen/images/grass_tileset_16x16/grass_tileset_16x16.png", 16);
        this.tileFloor = sheet.tile(0, 0);
        this.bottleSprite = loadImageResource("/alkosmen/images/objects/bottle/bottle_tich_gold.png");
        this.cellarBackground = loadImageResource("/alkosmen/ui/levels/cellar_bg_v1.png");
        this.cellarChestSprite = loadImageResource("/alkosmen/images/objects/chest/mega_chest.png");
        this.npcBoy2Sprite = loadFirstExistingImage("/alkosmen/images/objects/glack/boy2.png", "/alkosmen/images/objects/boy/boy2.png");
        BufferedImage tolyaSheet = (BufferedImage)loadImageResource("/alkosmen/ui/characters/tolya_zuevka_sheet.png");
        this.npcTolyaSprite = tolyaSheet.getSubimage(50, 480, 460, 480);
        this.npcEboboSprite = loadImageResource("/alkosmen/ui/intro/ebobo/walk_right/00.png");
        this.loadTownNpc('H', "red_brother");
        this.loadTownNpc('I', "red_sister");
        this.loadTownNpc('D', "dnb_partygoer");
        this.loadTownNpc('J', "train_gopnik");
        this.loadTownNpc('K', "train_conductor");
        this.loadTownNpc('S', "suspicious_stranger");
        Image[][] maleCopFrames = CharacterSpriteAssets.loadGridAtlas("/alkosmen/images/objects/cop/male/walk_atlas.png", 11, 3);
        this.femaleCopFrames = CharacterSpriteAssets.loadGridAtlas("/alkosmen/images/objects/cop/female/walk_atlas.png", 11, 3);
        this.copWalkLeftFrames = maleCopFrames[0];
        this.copWalkRightFrames = maleCopFrames[1];
        this.copWalkUpFrames = maleCopFrames[2];
        this.copWalkDownFrames = maleCopFrames[2];
        this.npcCopSprite = maleCopFrames[2][0];
        this.stepSound = new SoundEffectPlayer("/alkosmen/sounds/step.wav");
        this.bottleCollectSound = new SoundEffectPlayer("/alkosmen/sounds/scratch_bottle.wav");
        this.levelMidi = new MidiPlayer();
        if (Constants.GameMusicEnabled) {
            this.levelMidi.playLoop("/alkosmen/sounds/Caribbean-Blue.mid", 70);
        }

        this.playerSprites = loadAlkobotImages();
    }

    public void stopMusic() {
        if (this.levelMidi != null) {
            this.levelMidi.stop();
        }
    }

    private void loadTownNpc(char tile, String fileName) {
        try {
            this.townNpcFrames.put(tile, CharacterSpriteAssets.loadGridAtlas(
                "/alkosmen/ui/sprites/npc/" + fileName + ".png", 4, 4)[0]);
        } catch (IOException error) {
            throw new RuntimeException("Failed to load town NPC sprite: " + fileName, error);
        }
    }

    private static Image[][] loadAlkobotImages() {
        try {
            BufferedImage[][] atlas = CharacterSpriteAssets.loadGridAtlas("/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png", 4, 5, 30);
            return new Image[][]{atlas[2], atlas[1], atlas[0], atlas[4], atlas[3]};
        } catch (IOException error) {
            throw new IllegalStateException("Could not load Alkosmen walk atlas", error);
        }
    }

    public static Image loadImageResource(String path) {
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL url = GameAssets.class.getClassLoader().getResource(normalized);
        if (url == null) {
            throw new RuntimeException("Image not found: " + path);
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + path, e);
        }
    }

    private static Image loadFirstExistingImage(String... paths) {
        for (String path : paths) {
            String normalized = path.startsWith("/") ? path.substring(1) : path;
            URL url = GameAssets.class.getClassLoader().getResource(normalized);
            if (url != null) {
                try {
                    return ImageIO.read(url);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read image: " + path, e);
                }
            }
        }

        throw new RuntimeException("Image not found. Tried: " + String.join(", ", paths));
    }
}
