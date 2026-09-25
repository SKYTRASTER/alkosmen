package alkosmen;

import alkosmen.audio.MidiPlayer;
import alkosmen.audio.SoundEffectPlayer;
import alkosmen.game.CopSystem;
import alkosmen.game.GameHudRenderer;
import alkosmen.game.PatrolSystem;
import alkosmen.game.TopDownPatrol;
import alkosmen.gfx.CharacterSpriteAssets;
import alkosmen.gfx.SpriteSheet;
import alkosmen.lore.LoreCharacter;
import alkosmen.lore.LoreRepository;
import alkosmen.maps.LevelLoader;
import alkosmen.objects.Player;
import alkosmen.persistence.LocalGameStore;
import alkosmen.settings.Constants;
import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

public final class Game extends Canvas implements Runnable {
   private int currentLevel = 1;
   private int score = 0;
   private int bottleGoal = 8;
   private volatile boolean running;
   private BufferStrategy strategy;
   private Image[][] playerSprites;
   private Player player;
   private char[][] levelMap;
   private static final String[] LEVELS = new String[]{"/alkosmen/maps/demo_level.txt"};
   private static final String[] LEVEL_BACKGROUNDS = new String[]{"/alkosmen/ui/levels/market_square_walk_v1.png"};
   private boolean leftPressed;
   private boolean rightPressed;
   private boolean upPressed;
   private boolean downPressed;
   private boolean jumpPressed;
   private boolean jumpQueued;
   private long jumpBufferUntil;
   private long lastOnGroundAt;
   private int playerDir = 2;
   private int animFrame = 0;
   private int playerMotionTick = 0;
   private long lastAnim = 0L;
   private SoundEffectPlayer stepSound;
   private SoundEffectPlayer jumpSound;
   private SoundEffectPlayer bottleCollectSound;
   private SpriteSheet sheet;
   private Image tileFloor;
   private Image tileWall;
   private Image levelBackground;
   private Image cellarBackground;
   private Image bottleSprite;
   private Image npcBoy1Sprite;
   private Image npcBoy2Sprite;
   private Image npcTolyaSprite;
   private Image npcEboboSprite;
   private Image npcCopSprite;
   private final Map<Character, Image[]> townNpcFrames = new HashMap<>();
   private Image[] copWalkLeftFrames;
   private Image[] copWalkRightFrames;
   private Image[] copWalkUpFrames;
   private Image[] copWalkDownFrames;
   private MidiPlayer levelMidi;
   private float cameraX;
   private float cameraY;
   private final GameHudRenderer hudRenderer = new GameHudRenderer(56, 1200L);
   private final CopSystem copSystem = new CopSystem(0.045, (double)1.0F, (double)4.5F, 900L);
   private final PatrolSystem patrolSystem = new PatrolSystem();
   private final List<TopDownPatrol> patrols = new ArrayList<>();
   private final LoreRepository lore = LoreRepository.loadDefault();
   private LocalGameStore gameStore;
   private int playerSpawnX;
   private int playerSpawnY;
   private boolean hidePressed;
   private boolean gameOver;
   private boolean levelComplete;
   private boolean levelGoalReached;
   private volatile boolean restartRequested;
   private long lastPatrolCaughtAt;
   private int lives = 3;
   private boolean spectatorMode;
   private String cityLine = "";
   private long nextCityLineAt;
   private boolean interactionRequested;
   private String dialogueLine = "";
   private long dialogueUntil;
   private boolean tolyaQuestAccepted;
   private boolean tolyaQuestComplete;
   private LocalGameStore.StoryState eboboQuest = new LocalGameStore.StoryState(false, false, 0);
   private LocalGameStore.StoryState sacredQuest = new LocalGameStore.StoryState(false, false, 0);
   private LocalGameStore.StoryState cellarQuest = new LocalGameStore.StoryState(false, false, 0);
   private final String[] cellarObjectives = new String[3];
   private List eboboPhotos = List.of();
   private List sacredCaches = List.of();
   private LocalGameStore.Tile secretEntrance;
   private String sacredMapTitle = "";
   private List sacredHints = List.of();
   private QuestPanel questPanel;
   private volatile long pendingClick = -1L;
   private long photoFlashUntil;
   private long ufoUntil;
   private long ufoStartedAt;
   private boolean secretArea;
   private volatile boolean questMapOpen;
   private double secretX = 3.5;
   private double secretY = (double)5.0F;
   private static final String CELLAR_QUEST_ID = "secret_cellar";
   private static final int CELLAR_CHEST_X = 6;
   private static final int CELLAR_CHEST_Y = 3;
   private static final int CELLAR_BRICK_X = 5;
   private static final int CELLAR_BRICK_Y = 4;
   private static final double MOVE_SPEED = 0.12;
   private static final double TOP_DOWN_SPEED = 0.115;
   private static final double PATROL_COLLISION_MARGIN = 0.06;
   private static final double BOTTLE_PICKUP_RADIUS = 0.82;
   private static final double GRAVITY = 0.035;
   private static final double JUMP_SPEED = -0.68;
   private static final double MAX_FALL_SPEED = 0.9;
   private static final long JUMP_BUFFER_MS = 140L;
   private static final long COYOTE_TIME_MS = 120L;
   private static final double JUMP_HOLD_GRAVITY_MULT = 0.55;
   private static final double PLAYER_SCALE = 1.7;
   private static final double PLAYER_COLLISION_MARGIN = 0.06;
   private static final double BOTTLE_SCALE = (double)1.3125F;
   private static final double NPC_SCALE = 1.7;
   private static final double PATROL_HEIGHT_SCALE = 1.18;
   private static final int HUD_HEIGHT = 56;
   private static final double COP_SPEED = 0.045;
   private static final double COP_DROP_STEP = (double)1.0F;
   private static final double COP_VIEW_DISTANCE = (double)4.5F;
   private static final long COP_CAUGHT_COOLDOWN_MS = 900L;
   private static final long PATROL_CAUGHT_COOLDOWN_MS = 1000L;
   private static final long COP_CAUGHT_TEXT_MS = 1200L;
   private static final long FRAME_DELAY_MS = 16L;
   private static final long CITY_LINE_REFRESH_MS = 9000L;
   private static final int MAX_LIVES = 3;
   private static final int DEMO_RANDOM_COPS = 12;
   private static final int DEMO_RANDOM_COP_ATTEMPTS = 800;
   private static final int COP_WALK_FRAME_COUNT = 8;

   public void run() {
      try {
         this.init();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }

      while(this.running) {
         this.update();
         this.render();

         try {
            Thread.sleep(16L);
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }
      }

      this.stopAudio();
   }

   public Dimension getPreferredSize() {
      return new Dimension(Constants.Width, Constants.Height);
   }

   private void init() throws Exception {
      this.running = true;
      this.setFocusable(true);
      this.requestFocus();
      this.enableKeys();
      this.enableMouse();

      while(!this.isDisplayable()) {
         Thread.yield();
      }

      this.createBufferStrategy(2);
      this.strategy = this.getBufferStrategy();
      this.renderLoadingScreen("Loading assets...");
      this.sheet = new SpriteSheet("/alkosmen/images/grass_tileset_16x16/grass_tileset_16x16.png", 16);
      this.tileFloor = this.sheet.tile(0, 0);
      this.tileWall = this.sheet.tile(1, 0);
      this.bottleSprite = this.loadImageResource("/alkosmen/images/objects/bottle/bottle_tich_gold.png");
      this.cellarBackground = this.loadImageResource("/alkosmen/ui/levels/cellar_bg_v1.png");
      this.npcBoy1Sprite = this.loadFirstExistingImage("/alkosmen/images/objects/glack/boy1.png", "/alkosmen/images/objects/boy/boy1.png");
      this.npcBoy2Sprite = this.loadFirstExistingImage("/alkosmen/images/objects/glack/boy2.png", "/alkosmen/images/objects/boy/boy2.png");
      this.npcCopSprite = this.loadImageResource("/alkosmen/images/objects/cop/copdown0.png");
      BufferedImage tolyaSheet = (BufferedImage)this.loadImageResource("/alkosmen/ui/characters/tolya_zuevka_sheet.png");
      this.npcTolyaSprite = tolyaSheet.getSubimage(50, 480, 460, 480);
      this.npcEboboSprite = this.loadImageResource("/alkosmen/ui/intro/ebobo/walk_right/00.png");
      this.loadTownNpc('H', "red_brother");
      this.loadTownNpc('I', "red_sister");
      this.loadTownNpc('D', "dnb_partygoer");
      this.loadTownNpc('J', "train_gopnik");
      this.loadTownNpc('K', "train_conductor");
      this.loadTownNpc('S', "suspicious_stranger");
      this.copWalkLeftFrames = this.loadCopTrackFrames("walk_left");
      this.copWalkRightFrames = this.loadCopTrackFrames("walk_right");
      this.copWalkUpFrames = this.loadCopTrackFrames("walk_up");
      this.copWalkDownFrames = this.loadCopTrackFrames("walk_down");
      this.stepSound = new SoundEffectPlayer("/alkosmen/sounds/step.wav");
      this.jumpSound = new SoundEffectPlayer("/alkosmen/sounds/jump.wav");
      this.bottleCollectSound = new SoundEffectPlayer("/alkosmen/sounds/scratch_bottle.wav");
      this.levelMidi = new MidiPlayer();
      if (Constants.GameMusicEnabled) {
         this.levelMidi.playLoop("/alkosmen/sounds/Caribbean-Blue.mid", 70);
      }

      this.playerSprites = this.getAlkobotImages();
      this.renderLoadingScreen("Loading level...");
      this.gameStore = LocalGameStore.openDefault();
      this.loadLevel(1);
      this.rotateCityLine(System.currentTimeMillis(), true);
   }

   private void update() {
      if (this.player != null && this.levelMap != null) {
         if (this.restartRequested) {
            this.restartRequested = false;

            try {
               this.loadLevel(this.currentLevel);
            } catch (Exception e) {
               throw new RuntimeException("Failed to restart level", e);
            }
         } else if (!this.gameOver && !this.levelComplete) {
            long now = System.currentTimeMillis();
            long click = this.pendingClick;
            if (click != -1L) {
               this.pendingClick = -1L;
               this.handleClick((int)(click >>> 32), (int)click, now);
            }

            if (this.secretArea) {
               this.updateSecretArea();
            } else if (!this.questMapOpen) {
               if (this.questPanel == null) {
                  if (now < this.ufoUntil) {
                     this.playerDir = 4;
                  } else {
                     if (now >= this.nextCityLineAt) {
                        this.rotateCityLine(now, false);
                     }

                     double dx = (this.rightPressed ? (double)1.0F : (double)0.0F) - (this.leftPressed ? (double)1.0F : (double)0.0F);
                     double dy = (this.downPressed ? (double)1.0F : (double)0.0F) - (this.upPressed ? (double)1.0F : (double)0.0F);
                     if (dx == (double)0.0F && dy == (double)0.0F) {
                        this.playerDir = 2;
                     } else {
                        double length = Math.hypot(dx, dy);
                        this.moveTopDown(dx / length * 0.115, dy / length * 0.115);
                        this.playerDir = Math.abs(dy) > Math.abs(dx) ? (dy < (double)0.0F ? 3 : 4) : (dx < (double)0.0F ? 0 : 1);
                     }

                     this.animatePlayer();
                     if (this.interactionRequested) {
                        this.interactionRequested = false;
                        this.interactWithNearbyNpc(now);
                     }

                     this.updatePatrols();
                     this.checkPatrolCollision(now);
                     if (!this.gameOver) {
                        this.updateCamera();
                        this.collectNearbyBottle();
                        int px = (int)Math.floor(this.player.x);
                        int py = (int)Math.floor(this.player.y);
                        if (this.isInsideMap(px, py) && this.levelMap[py][px] == 'E' && this.levelGoalReached) {
                           this.levelComplete = true;
                           this.leftPressed = false;
                           this.rightPressed = false;
                           this.upPressed = false;
                           this.downPressed = false;
                        }

                     }
                  }
               }
            }
         }
      }
   }

   private void moveTopDown(double dx, double dy) {
      double nextX = this.player.x + dx;
      if (this.canOccupy(nextX, this.player.y)) {
         this.player.x = nextX;
      }

      double nextY = this.player.y + dy;
      if (this.canOccupy(this.player.x, nextY)) {
         this.player.y = nextY;
      }

   }

   private void interactWithNearbyNpc(long now) {
      char nearest = 0;
      double bestDistance = 2.2;

      for(int y = 0; y < this.levelMap.length; ++y) {
         for(int x = 0; x < this.levelMap[y].length; ++x) {
            char tile = this.levelMap[y][x];
            if (this.isNpcTile(tile)) {
               double distance = Math.hypot(this.player.x - (double)x, this.player.y - (double)y);
               if (distance < bestDistance) {
                  bestDistance = distance;
                  nearest = tile;
               }
            }
         }
      }

      String var10001;
      switch (nearest) {
         case 'G' -> var10001 = this.dbText("npc.cop");
         case 'M' -> var10001 = this.dbText("npc.merchant");
         case 'N' -> var10001 = this.openTolyaPanel();
         case 'V' -> var10001 = this.openEboboPanel();
         default -> var10001 = this.dbText(this.npcDialogueKey(nearest));
      }

      this.dialogueLine = var10001;
      if (this.questPanel == null) {
         this.dialogueUntil = now + 5000L;
      }

   }

   private String dbText(String key) {
      try {
         return this.gameStore.text(key);
      } catch (SQLException error) {
         System.err.println("Dialogue load failed: " + error.getMessage());
         return key;
      }
   }

   private void showLine(String line, long now) {
      this.dialogueLine = line;
      this.dialogueUntil = now + 5000L;
   }

   private String openTolyaPanel() {
      if (!this.tolyaQuestAccepted) {
         this.offer("tolya_bottles", "quest.tolya.offer");
      } else if (!this.tolyaQuestComplete && this.score >= this.bottleGoal) {
         this.turnIn("tolya_bottles", "quest.tolya.finish");
      } else if (!this.tolyaQuestComplete) {
         this.info("tolya_bottles", "quest.tolya.progress", this.dbText("quest.tolya.progress.body").replace("{count}", Integer.toString(this.score)).replace("{total}", Integer.toString(this.bottleGoal)));
      } else if (!this.sacredQuest.accepted()) {
         this.offer("sacred_tich", "quest.sacred.offer.v2");
      } else if (!this.sacredQuest.completed() && this.sacredQuest.stage() >= this.sacredCaches.size()) {
         this.turnIn("sacred_tich", "quest.sacred.finish");
      } else {
         this.info("sacred_tich", "quest.sacred.progress", this.dbText(this.sacredQuest.completed() ? "quest.sacred.done.body" : "quest.sacred.progress.body"));
      }

      return "";
   }

   private String openEboboPanel() {
      if (!this.eboboQuest.accepted()) {
         this.offer("ebobo_ufo", "quest.ebobo.offer");
      } else if (!this.eboboQuest.completed() && this.eboboQuest.stage() >= this.eboboPhotos.size()) {
         this.turnIn("ebobo_ufo", "quest.ebobo.finish");
      } else {
         this.info("ebobo_ufo", "quest.ebobo.progress", this.eboboQuest.completed() ? this.dbText("quest.ebobo.done.body") : this.dbText("quest.ebobo.progress.body").replace("{count}", Integer.toString(this.eboboQuest.stage())).replace("{total}", Integer.toString(this.eboboPhotos.size())));
      }

      return "";
   }

   private void offer(String questId, String prefix) {
      this.questPanel = new QuestPanel(questId, this.dbText(prefix + ".title"), this.dbText(prefix + ".body"), "Принять", Game.PanelAction.ACCEPT);
   }

   private void turnIn(String questId, String prefix) {
      this.questPanel = new QuestPanel(questId, this.dbText(prefix + ".title"), this.dbText(prefix + ".body"), "Завершить", Game.PanelAction.TURN_IN);
   }

   private void info(String questId, String prefix, String body) {
      this.questPanel = new QuestPanel(questId, this.dbText(prefix + ".title"), body, "Закрыть", Game.PanelAction.CLOSE);
   }

   private void enableMouse() {
      this.addMouseListener(new MouseAdapter() {
         public void mousePressed(MouseEvent event) {
            if (event.getButton() == 1) {
               Game.this.pendingClick = (long)event.getX() << 32 | (long)event.getY() & 4294967295L;
            }

         }
      });
   }

   private void handleClick(int screenX, int screenY, long now) {
      if (this.questPanel != null) {
         this.handlePanelClick(screenX, screenY, now);
      } else if (this.questMapOpen) {
         this.questMapOpen = false;
      } else if (this.secretArea) {
         this.handleCellarClick(screenX, screenY, now);
      } else if (!this.secretArea) {
         int cell = Constants.Size;

         for(int y = 0; y < this.levelMap.length; ++y) {
            for(int x = 0; x < this.levelMap[y].length; ++x) {
               char npc = this.levelMap[y][x];
               if (this.isNpcTile(npc) && npc != 'C') {
                  int size = this.npcDrawSize(npc, cell);
                  int left = (int)((double)((float)(x * cell) - this.cameraX) - (double)(size - cell) / (double)2.0F);
                  int top = (int)((float)(y * cell) - this.cameraY - (float)(size - cell));
                  if ((new Rectangle(left, top, size, size)).contains(screenX, screenY)) {
                     if (Math.hypot(this.player.x - (double)x, this.player.y - (double)y) > 2.4) {
                        this.showLine(this.dbText("npc.far"), now);
                     } else if (npc == 'N') {
                        this.openTolyaPanel();
                     } else if (npc == 'V') {
                        this.openEboboPanel();
                     } else {
                        this.showLine(this.dbText(this.npcDialogueKey(npc)), now);
                     }

                     return;
                  }
               }
            }
         }

         if (!this.tryClueClick(screenX, screenY, now, "ebobo_ufo", this.eboboQuest, this.eboboPhotos)) {
            if (!this.tryClueClick(screenX, screenY, now, "sacred_tich", this.sacredQuest, this.sacredCaches)) {
               if (this.sacredQuest.completed() && this.secretEntrance != null && this.hitTile(screenX, screenY, this.secretEntrance, 0.8)) {
                  if (Math.hypot(this.player.x - (double)this.secretEntrance.x(), this.player.y - (double)this.secretEntrance.y()) > 1.8) {
                     this.showLine(this.dbText("npc.far"), now);
                  } else {
                     this.secretArea = true;
                     this.secretX = 3.5;
                     this.secretY = (double)5.0F;
                     this.showLine(this.dbText("quest.secret.enter"), now);
                  }
               }

            }
         }
      }
   }

   private boolean tryClueClick(int sx, int sy, long now, String questId, LocalGameStore.StoryState state, List clues) {
      if (state.accepted() && !state.completed() && state.stage() < clues.size()) {
         LocalGameStore.QuestStep clue = (LocalGameStore.QuestStep)clues.get(state.stage());
         if (!this.hitTile(sx, sy, clue.tile(), 0.8)) {
            return false;
         } else if (Math.hypot(this.player.x - (double)clue.tile().x(), this.player.y - (double)clue.tile().y()) > 1.8) {
            this.showLine(this.dbText("npc.far"), now);
            return true;
         } else {
            LocalGameStore.StoryState next = new LocalGameStore.StoryState(true, false, state.stage() + 1);

            try {
               this.gameStore.saveStory(questId, next);
            } catch (SQLException var11) {
               this.showLine(this.dbText("save.error"), now);
               return true;
            }

            if ("ebobo_ufo".equals(questId)) {
               this.eboboQuest = next;
               this.photoFlashUntil = now + 180L;
               if (next.stage() == clues.size()) {
                  this.ufoStartedAt = now;
                  this.ufoUntil = now + 3500L;
                  this.playerDir = 4;
               }
            } else {
               this.sacredQuest = next;
            }

            this.showLine(this.dbText(clue.textKey()), now);
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean hitTile(int sx, int sy, LocalGameStore.Tile tile, double radius) {
      int cell = Constants.Size;
      double centerX = ((double)tile.x() + (double)0.5F) * (double)cell - (double)this.cameraX;
      double centerY = ((double)tile.y() + (double)0.5F) * (double)cell - (double)this.cameraY;
      return Math.hypot((double)sx - centerX, (double)sy - centerY) < (double)cell * radius;
   }

   private void handlePanelClick(int sx, int sy, long now) {
      Rectangle panel = this.questPanelBounds();
      Rectangle button = new Rectangle(panel.x + panel.width - 185, panel.y + panel.height - 59, 155, 38);
      Rectangle close = new Rectangle(panel.x + panel.width - 40, panel.y + 12, 28, 28);
      if (!close.contains(sx, sy) && panel.contains(sx, sy)) {
         if (button.contains(sx, sy)) {
            QuestPanel selected = this.questPanel;
            if (selected.action() == Game.PanelAction.CLOSE) {
               this.questPanel = null;
            } else {
               try {
                  if ("tolya_bottles".equals(selected.questId())) {
                     boolean finished = selected.action() == Game.PanelAction.TURN_IN;
                     this.gameStore.saveTolyaQuest(true, finished);
                     this.tolyaQuestAccepted = true;
                     this.tolyaQuestComplete = finished;
                     if (finished) {
                        this.showLine(this.dbText("quest.tolya.finish.line"), now);
                     }
                  } else {
                     LocalGameStore.StoryState old = "ebobo_ufo".equals(selected.questId()) ? this.eboboQuest : this.sacredQuest;
                     boolean finished = selected.action() == Game.PanelAction.TURN_IN;
                     LocalGameStore.StoryState next = new LocalGameStore.StoryState(true, finished, old.stage());
                     this.gameStore.saveStory(selected.questId(), next);
                     if ("ebobo_ufo".equals(selected.questId())) {
                        this.eboboQuest = next;
                        if (finished) {
                           this.showLine(this.dbText("quest.ebobo.finish.line"), now);
                        }
                     } else {
                        this.sacredQuest = next;
                        if (finished) {
                           this.showLine(this.dbText("quest.sacred.finish.line"), now);
                        }
                     }
                  }

                  this.questPanel = null;
               } catch (SQLException var12) {
                  this.questPanel = null;
                  this.showLine(this.dbText("save.error"), now);
               }

            }
         }
      } else {
         this.questPanel = null;
      }
   }

   private Rectangle questPanelBounds() {
      int width = Math.min(620, this.getWidth() - 40);
      int height = 278;
      return new Rectangle((this.getWidth() - width) / 2, (this.getHeight() - height) / 2, width, height);
   }

   private boolean canOccupy(double x, double y) {
      double minX = x + PATROL_COLLISION_MARGIN;
      double maxX = x + (double)1.0F - PATROL_COLLISION_MARGIN;
      double minY = y + PATROL_COLLISION_MARGIN;
      double maxY = y + (double)1.0F - PATROL_COLLISION_MARGIN;
      return !this.isSolid((int)Math.floor(minX), (int)Math.floor(minY)) && !this.isSolid((int)Math.floor(maxX), (int)Math.floor(minY)) && !this.isSolid((int)Math.floor(minX), (int)Math.floor(maxY)) && !this.isSolid((int)Math.floor(maxX), (int)Math.floor(maxY));
   }

   private void collectNearbyBottle() {
      if (this.tolyaQuestAccepted && !this.tolyaQuestComplete) {
         double playerCenterX = this.player.x + (double)0.5F;
         double playerCenterY = this.player.y + (double)0.5F;
         int minX = Math.max(0, (int)Math.floor(playerCenterX - 0.82));
         int maxX = Math.min(this.levelMap[0].length - 1, (int)Math.floor(playerCenterX + 0.82));
         int minY = Math.max(0, (int)Math.floor(playerCenterY - 0.82));
         int maxY = Math.min(this.levelMap.length - 1, (int)Math.floor(playerCenterY + 0.82));

         for(int y = minY; y <= maxY; ++y) {
            for(int x = minX; x <= maxX; ++x) {
               if (this.levelMap[y][x] == 'B' && Math.hypot(playerCenterX - ((double)x + (double)0.5F), playerCenterY - ((double)y + (double)0.5F)) <= 0.82) {
                  try {
                     if (!this.gameStore.collectTolyaBottle(x, y)) {
                        this.levelMap[y][x] = '.';
                        return;
                     }
                  } catch (SQLException error) {
                     System.err.println("Bottle save failed: " + error.getMessage());
                     this.dialogueLine = this.dbText("save.error");
                     this.dialogueUntil = System.currentTimeMillis() + 3000L;
                     return;
                  }

                  this.levelMap[y][x] = '.';
                  ++this.score;
                  this.bottleCollectSound.play();
                  this.levelGoalReached = this.score >= this.bottleGoal && this.bottleGoal > 0;
                  return;
               }
            }
         }

      }
   }

   private void updatePatrols() {
      this.patrolSystem.update(this.patrols, System.currentTimeMillis(), this::canPatrolOccupy);
   }

   private boolean canPatrolOccupy(double x, double y) {
      double minX = x + 0.06;
      double maxX = x + (double)1.0F - 0.06;
      double minY = y + 0.06;
      double maxY = y + (double)1.0F - 0.06;
      return !this.isSolid((int)Math.floor(minX), (int)Math.floor(minY)) && !this.isSolid((int)Math.floor(maxX), (int)Math.floor(minY)) && !this.isSolid((int)Math.floor(minX), (int)Math.floor(maxY)) && !this.isSolid((int)Math.floor(maxX), (int)Math.floor(maxY));
   }

   private void checkPatrolCollision(long now) {
      if (this.currentLevel != 1) {
         if (now - this.lastPatrolCaughtAt >= 1000L) {
            for(TopDownPatrol patrol : this.patrols) {
               if (Math.abs(this.player.x - patrol.x()) < 0.55 && Math.abs(this.player.y - patrol.y()) < 0.55) {
                  this.lastPatrolCaughtAt = now;
                  this.lives = Math.max(0, this.lives - 1);
                  if (this.lives == 0) {
                     this.gameOver = true;
                  } else {
                     this.respawnPlayer();
                  }

                  return;
               }
            }

         }
      }
   }

   private void moveHorizontal(double dx) {
      if (dx != (double)0.0F) {
         double nx = this.player.x + dx;
         int tx = (int)Math.floor(nx);
         int ty = (int)Math.floor(this.player.y);
         if (this.isSolid(tx, ty)) {
            if (dx > (double)0.0F) {
               this.player.x = (double)tx - 0.001;
            } else {
               this.player.x = (double)tx + 1.001;
            }

            this.player.vx = (double)0.0F;
         } else {
            this.player.x = nx;
         }

      }
   }

   private void moveVertical(double dy) {
      if (dy != (double)0.0F) {
         double ny = this.player.y + dy;
         int tx = (int)Math.floor(this.player.x);
         int ty = (int)Math.floor(ny);
         if (this.isSolid(tx, ty)) {
            if (dy > (double)0.0F) {
               this.player.y = (double)ty - 0.001;
               this.player.onGround = true;
            } else {
               this.player.y = (double)ty + 1.001;
            }

            this.player.vy = (double)0.0F;
         } else {
            this.player.y = ny;
            this.player.onGround = false;
         }

      }
   }

   private void animatePlayer() {
      boolean isWalking = this.leftPressed || this.rightPressed || this.upPressed || this.downPressed;
      long now = System.currentTimeMillis();
      int frameCount = this.playerSprites != null && this.playerDir >= 0 && this.playerDir < this.playerSprites.length ? this.playerSprites[this.playerDir].length : 0;
      if (frameCount > 0) {
         if (isWalking && now - this.lastAnim > 90L) {
            this.animFrame = (this.animFrame + 1) % frameCount;
            ++this.playerMotionTick;
            this.lastAnim = now;
            if (this.playerMotionTick % 4 == 0) {
               this.stepSound.play();
            }
         }

         if (!isWalking && now - this.lastAnim > 110L) {
            this.animFrame = (this.animFrame + 1) % frameCount;
            ++this.playerMotionTick;
            this.lastAnim = now;
         }

      }
   }

   private void updateCamera() {
      int cell = Constants.Size;
      double focusX = this.player.x;
      double focusY = this.player.y;
      float worldPx = (float)(focusX * (double)cell);
      float worldPy = (float)(focusY * (double)cell);
      int gameplayHeight = Math.max(1, this.getHeight() - 56);
      int mapPxW = this.levelMap[0].length * cell;
      int mapPxH = this.levelMap.length * cell;
      this.cameraX = worldPx - (float)this.getWidth() / 2.0F + (float)cell / 2.0F;
      this.cameraY = worldPy - (float)gameplayHeight / 2.0F + (float)cell / 2.0F;
      float maxX = (float)Math.max(0, mapPxW - this.getWidth());
      float maxY = (float)Math.max(0, mapPxH - gameplayHeight);
      this.cameraX = clamp(this.cameraX, 0.0F, maxX);
      this.cameraY = clamp(this.cameraY, 0.0F, maxY);
   }

   private void render() {
      BufferStrategy bs = this.strategy;
      if (bs != null) {
         Graphics g = bs.getDrawGraphics();
         long sceneTime = System.currentTimeMillis();
         g.setColor(new Color(11, 19, 30));
         g.fillRect(0, 0, this.getWidth(), this.getHeight());
         if (!this.secretArea && this.levelMap != null) {
            int cell = Constants.Size;
            if (this.levelBackground != null) {
               this.drawLevelBackground(g, this.getHeight() - 56);
            }

            int firstTileX = Math.max(0, (int)(this.cameraX / (float)cell));
            int firstTileY = Math.max(0, (int)(this.cameraY / (float)cell));
            int visibleX = this.getWidth() / cell + 3;
            int visibleY = this.getHeight() / cell + 3;
            int lastTileX = Math.min(this.levelMap[0].length, firstTileX + visibleX);
            int lastTileY = Math.min(this.levelMap.length, firstTileY + visibleY);

            for(int y = firstTileY; y < lastTileY; ++y) {
               for(int x = firstTileX; x < lastTileX; ++x) {
                  char c = this.levelMap[y][x];
                  if (c != '.') {
                     int drawX = x * cell - (int)this.cameraX;
                     int drawY = y * cell - (int)this.cameraY;
                     if (c != '#') {
                        if (c == 'B' && this.bottleSprite != null) {
                           int bottleW = (int)Math.round((double)cell * (double)1.3125F);
                           int bottleH = (int)Math.round((double)cell * (double)1.3125F);
                           int bottleX = drawX - (bottleW - cell) / 2;
                           int bottleBob = (int)Math.round(Math.sin((double)(sceneTime + (long)x * 251L + (long)y * 131L) / (double)280.0F));
                           int bottleY = drawY - (bottleH - cell) + bottleBob;
                           g.drawImage(this.bottleSprite, bottleX, bottleY, bottleW, bottleH, (ImageObserver)null);
                        } else if (c == 'E') {
                           this.drawExit(g, drawX, drawY, cell, this.levelGoalReached);
                        } else if (this.isNpcTile(c)) {
                           Image npc = this.npcImageFor(c, sceneTime);
                           if (npc != null) {
                              int npcW = this.npcDrawSize(c, cell);
                              int npcH = npcW;
                              int npcX = drawX - (npcW - cell) / 2;
                              int npcBob = (int)Math.round(Math.sin((double)(sceneTime + (long)x * 173L + (long)y * 97L) / (double)450.0F));
                              int npcY = drawY - (npcH - cell) + npcBob;
                              Graphics2D npcGraphics = (Graphics2D)g.create();
                              npcGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                              npcGraphics.drawImage(npc, npcX, npcY, npcW, npcH, (ImageObserver)null);
                              npcGraphics.dispose();
                              this.drawNpcLabel(g, c, drawX + cell / 2, npcY - 4);
                              if (c == 'N') {
                                 if (this.tolyaQuestAccepted && (!this.tolyaQuestComplete || this.sacredQuest.accepted())) {
                                    if (this.score >= this.bottleGoal && !this.tolyaQuestComplete || this.sacredQuest.accepted() && !this.sacredQuest.completed() && this.sacredQuest.stage() >= this.sacredCaches.size()) {
                                       this.drawQuestMarker(g, drawX + cell / 2, npcY - 27, "?", true);
                                    }
                                 } else {
                                    this.drawQuestMarker(g, drawX + cell / 2, npcY - 27, "!", true);
                                 }
                              }

                              if (c == 'V') {
                                 this.drawQuestMarker(g, drawX + cell / 2, npcY - 27, "?", this.eboboQuest.completed());
                              }
                           }
                        } else {
                           g.drawImage(this.tileFloor, drawX, drawY, cell, cell, (ImageObserver)null);
                        }
                     }
                  }
               }
            }
         }

         if (!this.secretArea && this.levelMap != null) {
            this.drawStorySites(g);
            this.drawUfo(g, sceneTime);
            int cell = Constants.Size;
            this.copSystem.draw(g, cell, 1.7, this.npcCopSprite, this.copWalkLeftFrames, this.copWalkRightFrames, this.cameraX, this.cameraY, sceneTime);
            this.drawPatrols(g, cell);
         }

         if (!this.secretArea && !this.spectatorMode && this.player != null && this.playerSprites != null) {
            int cell = Constants.Size;
            int playerW = (int)Math.round((double)cell * 1.7);
            int playerH = (int)Math.round((double)cell * 1.7);
            int drawX = (int)Math.round(this.player.x * (double)cell - (double)this.cameraX - (double)(playerW - cell) / (double)2.0F);
            int drawY = (int)Math.round(this.player.y * (double)cell - (double)this.cameraY + (double)cell - (double)playerH);
            int maxPlayerY = this.getHeight() - 56 - playerH;
            if (drawY > maxPlayerY) {
               drawY = maxPlayerY;
            }

            boolean playerIsWalking = this.playerDir != 2;
            Image[] track = this.playerSprites[this.playerDir];
            Image img = track[Math.floorMod(this.animFrame, track.length)];
            if (this.isPlayerHidden()) {
               Graphics2D g2 = (Graphics2D)g.create();
               g2.setComposite(AlphaComposite.getInstance(3, 0.33F));
               g2.drawImage(img, drawX, drawY, playerW, playerH, (ImageObserver)null);
               g2.dispose();
            } else {
               g.drawImage(img, drawX, drawY, playerW, playerH, (ImageObserver)null);
            }
         }

         if (this.secretArea) {
            this.drawSecretArea(g);
         }

         if (sceneTime < this.photoFlashUntil) {
            Graphics2D flash = (Graphics2D)g.create();
            flash.setColor(new Color(236, 249, 255, 90));
            flash.fillRect(0, 0, this.getWidth(), this.getHeight() - 56);
            flash.dispose();
         }

         if (sceneTime < this.dialogueUntil) {
            this.drawDialogue(g, this.dialogueLine);
         }

         this.hudRenderer.drawHud(g, this.getWidth(), this.getHeight(), this.currentLevel, this.score, this.bottleGoal, this.questObjective(), this.lives, 3, this.isPlayerHidden(), this.gameOver, this.cityLine, Math.max(this.copSystem.getLastCaughtAt(), this.lastPatrolCaughtAt), sceneTime, this.secretArea);
         this.hudRenderer.drawGameOverOverlay(g, this.getWidth(), this.getHeight(), this.gameOver);
         this.hudRenderer.drawLevelCompleteOverlay(g, this.getWidth(), this.getHeight(), this.levelComplete);
         if (this.questMapOpen) {
            this.drawQuestMap(g);
         }

         if (this.questPanel != null) {
            this.drawQuestPanel(g);
         }

         g.dispose();
         bs.show();
      }
   }

   private void drawPatrols(Graphics g, int cell) {
      if (this.npcCopSprite != null) {
         for(TopDownPatrol patrol : this.patrols) {
            Image[] var10000;
            switch (patrol.facing()) {
               case 0:
                  var10000 = this.copWalkLeftFrames;
                  break;
               case 1:
               default:
                  var10000 = this.copWalkRightFrames;
                  break;
               case 2:
                  var10000 = this.copWalkUpFrames;
                  break;
               case 3:
                  var10000 = this.copWalkDownFrames;
            }

            Image[] frames = var10000;
            Image sprite = frames != null && frames.length != 0 ? frames[Math.floorMod(patrol.animationTick() / 7, frames.length)] : this.npcCopSprite;
            int spriteH = (int)Math.round((double)cell * 1.18);
            int spriteW = Math.max(1, (int)Math.round((double)spriteH * (double)sprite.getWidth((ImageObserver)null) / (double)sprite.getHeight((ImageObserver)null)));
            int drawX = (int)Math.round(patrol.x() * (double)cell - (double)this.cameraX + (double)(cell - spriteW) / (double)2.0F);
            int drawY = (int)Math.round(patrol.y() * (double)cell - (double)this.cameraY + (double)cell - (double)spriteH);
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(sprite, drawX, drawY, spriteW, spriteH, (ImageObserver)null);
            g2.dispose();
         }

      }
   }

   private void drawNpcLabel(Graphics g, char npc, int centerX, int y) {
      String var10000;
      switch (npc) {
         case 'G' -> var10000 = "ПОЛИЦИЯ";
         case 'M' -> var10000 = "ТОРГОВЕЦ";
         case 'N' -> var10000 = "ТОЛЯ";
         case 'V' -> var10000 = "ЕБобо";
         case 'H' -> var10000 = "РЫЖИЙ";
         case 'I' -> var10000 = "РЫЖАЯ";
         case 'D' -> var10000 = "РЕЙВЕР";
         case 'J' -> var10000 = "ГОПНИК";
         case 'K' -> var10000 = "КОНТРОЛЁР";
         case 'S' -> var10000 = "НЕЗНАКОМЕЦ";
         default -> var10000 = "";
      }

      String name = var10000;
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setFont(new Font("Dialog", 1, 11));
      int width = g2.getFontMetrics().stringWidth(name) + 12;
      g2.setColor(new Color(8, 13, 27, 205));
      g2.fillRoundRect(centerX - width / 2, y - 15, width, 17, 6, 6);
      g2.setColor(new Color(255, 220, 164));
      g2.drawString(name, centerX - width / 2 + 6, y - 3);
      g2.dispose();
   }

   private void drawQuestMarker(Graphics g, int centerX, int y, String symbol, boolean gold) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setFont(new Font("Dialog", 1, 27));
      g2.setColor(new Color(15, 12, 16));
      g2.drawString(symbol, centerX - 6, y + 2);
      g2.setColor(gold ? new Color(255, 207, 58) : new Color(205, 218, 235));
      g2.drawString(symbol, centerX - 7, y);
      g2.dispose();
   }

   private void drawStorySites(Graphics g) {
      if (this.eboboQuest.accepted() && !this.eboboQuest.completed() && this.eboboQuest.stage() < this.eboboPhotos.size()) {
         this.drawSite(g, ((LocalGameStore.QuestStep)this.eboboPhotos.get(this.eboboQuest.stage())).tile(), true);
      }

      if (this.sacredQuest.accepted() && !this.sacredQuest.completed() && this.sacredQuest.stage() < this.sacredCaches.size()) {
         this.drawSite(g, ((LocalGameStore.QuestStep)this.sacredCaches.get(this.sacredQuest.stage())).tile(), false);
      }

      if (this.sacredQuest.completed() && this.secretEntrance != null) {
         this.drawSite(g, this.secretEntrance, false);
      }

   }

   private void drawSite(Graphics g, LocalGameStore.Tile tile, boolean camera) {
      int cell = Constants.Size;
      int x = (int)Math.round(((double)tile.x() + (double)0.5F) * (double)cell - (double)this.cameraX);
      int y = (int)Math.round(((double)tile.y() + (double)0.5F) * (double)cell - (double)this.cameraY);
      if (x >= -cell && x <= this.getWidth() + cell && y >= -cell && y <= this.getHeight() - 56 + cell) {
         Graphics2D g2 = (Graphics2D)g.create();
         g2.setColor(new Color(10, 16, 27, 220));
         g2.fillRoundRect(x - 19, y - 21, 38, 38, 12, 12);
         g2.setColor(camera ? new Color(185, 223, 249) : new Color(255, 209, 86));
         g2.drawRoundRect(x - 19, y - 21, 37, 37, 12, 12);
         if (camera) {
            g2.fillRoundRect(x - 12, y - 9, 24, 17, 3, 3);
            g2.fillRect(x - 6, y - 13, 10, 5);
            g2.setColor(new Color(10, 16, 27));
            g2.fillOval(x - 5, y - 7, 10, 10);
         } else {
            g2.setFont(new Font("Dialog", 1, 27));
            g2.drawString("?", x - 8, y + 10);
         }

         g2.dispose();
      }
   }

   private void drawUfo(Graphics g, long now) {
      if (now < this.ufoUntil) {
         Graphics2D g2 = (Graphics2D)g.create();
         int x = (int)((now - this.ufoStartedAt) * ((long)this.getWidth() + 130L) / 3500L) - 65;
         int y = Math.max(55, this.getHeight() / 6);
         g2.setColor(new Color(85, 215, 225, 80));
         g2.fillOval(x - 30, y - 16, 110, 38);
         g2.setColor(new Color(30, 51, 71));
         g2.fillOval(x, y, 55, 14);
         g2.setColor(new Color(168, 242, 250));
         g2.fillArc(x + 13, y - 11, 29, 19, 0, 180);
         g2.fillOval(x + 10, y + 9, 7, 4);
         g2.fillOval(x + 39, y + 9, 7, 4);
         g2.dispose();
      }
   }

   private void drawQuestPanel(Graphics g) {
      Rectangle panel = this.questPanelBounds();
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setColor(new Color(4, 8, 17, 175));
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      g2.setColor(new Color(24, 30, 43));
      g2.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 14, 14);
      g2.setColor(new Color(190, 155, 88));
      g2.drawRoundRect(panel.x, panel.y, panel.width, panel.height, 14, 14);
      g2.setFont(new Font("Dialog", 1, 23));
      g2.setColor(new Color(255, 222, 161));
      g2.drawString(this.questPanel.title(), panel.x + 28, panel.y + 44);
      g2.setFont(new Font("Dialog", 0, 17));
      g2.setColor(new Color(236, 232, 217));
      String[] lines = this.questPanel.body().split("\n");

      for(int i = 0; i < lines.length; ++i) {
         g2.drawString(lines[i], panel.x + 28, panel.y + 91 + i * 30);
      }

      Rectangle button = new Rectangle(panel.x + panel.width - 185, panel.y + panel.height - 59, 155, 38);
      g2.setColor(new Color(87, 57, 31));
      g2.fillRoundRect(button.x, button.y, button.width, button.height, 8, 8);
      g2.setColor(new Color(247, 193, 98));
      g2.drawRoundRect(button.x, button.y, button.width, button.height, 8, 8);
      g2.setFont(new Font("Dialog", 1, 17));
      g2.drawString(this.questPanel.button(), button.x + 21, button.y + 25);
      g2.drawString("×", panel.x + panel.width - 34, panel.y + 34);
      g2.dispose();
   }

   private void drawQuestMap(Graphics g) {
      int width = Math.min(700, this.getWidth() - 48);
      int height = 440;
      int left = (this.getWidth() - width) / 2;
      int top = (this.getHeight() - height) / 2;
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setColor(new Color(5, 9, 15, 190));
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      g2.setColor(new Color(186, 157, 112));
      g2.fillRoundRect(left, top, width, height, 12, 12);
      g2.setColor(new Color(91, 64, 39));
      g2.drawRoundRect(left, top, width - 1, height - 1, 12, 12);
      g2.setFont(new Font("Dialog", 1, 22));
      g2.drawString(this.sacredMapTitle, left + 28, top + 38);
      int mapLeft = left + 70;
      int mapTop = top + 70;
      int mapWidth = width - 140;
      int mapHeight = 260;
      double scaleX = (double)mapWidth / (double)this.levelMap[0].length;
      double scaleY = (double)mapHeight / (double)this.levelMap.length;
      g2.setColor(new Color(207, 186, 143));
      g2.fillRect(mapLeft, mapTop, mapWidth, mapHeight);
      g2.setColor(new Color(118, 100, 71));

      for(int y = 0; y < this.levelMap.length; ++y) {
         for(int x = 0; x < this.levelMap[y].length; ++x) {
            if (this.levelMap[y][x] == '#') {
               int px = mapLeft + (int)((double)x * scaleX);
               int py = mapTop + (int)((double)y * scaleY);
               g2.fillRect(px, py, (int)Math.ceil(scaleX), (int)Math.ceil(scaleY));
            }
         }
      }

      g2.setFont(new Font("Dialog", 1, 22));

      for(int i = 0; i < this.sacredCaches.size(); ++i) {
         LocalGameStore.Tile tile = ((LocalGameStore.QuestStep)this.sacredCaches.get(i)).tile();
         int px = mapLeft + (int)(((double)tile.x() + (double)0.5F) * scaleX);
         int py = mapTop + (int)(((double)tile.y() + (double)0.5F) * scaleY);
         g2.setColor(i < this.sacredQuest.stage() ? new Color(99, 90, 72) : (i == this.sacredQuest.stage() ? new Color(178, 65, 30) : new Color(135, 111, 77)));
         g2.drawString("×", px - 6, py + 7);
      }

      int heroX = mapLeft + (int)((this.player.x + (double)0.5F) * scaleX);
      int heroY = mapTop + (int)((this.player.y + (double)0.5F) * scaleY);
      g2.setColor(new Color(225, 42, 45));
      g2.fillOval(heroX - 4, heroY - 4, 9, 9);
      int hint = Math.min(this.sacredQuest.stage(), this.sacredCaches.size() - 1);
      g2.setFont(new Font("Dialog", 1, 16));
      g2.setColor(new Color(62, 42, 28));
      if (hint >= 0 && hint < this.sacredHints.size()) {
         g2.drawString((String)this.sacredHints.get(hint), left + 28, top + 374);
      }

      g2.setFont(new Font("Dialog", 0, 14));
      g2.drawString("Красная точка — ты. Крестики — тайники. M или клик — закрыть.", left + 28, top + 412);
      g2.dispose();
   }

   private String questObjective() {
      if (this.secretArea) {
         return this.cellarObjective();
      } else if (this.eboboQuest.accepted() && !this.eboboQuest.completed()) {
         return this.eboboQuest.stage() >= this.eboboPhotos.size() ? "НЛО: вернись к ЕБобо" : "НЛО: фото " + this.eboboQuest.stage() + "/" + this.eboboPhotos.size() + " — найди серебряную камеру";
      } else if (this.sacredQuest.accepted() && !this.sacredQuest.completed()) {
         return this.sacredQuest.stage() >= this.sacredCaches.size() ? "«Тич» найден — вернись к Толе" : "«Тич»: тайник " + (this.sacredQuest.stage() + 1) + "/" + this.sacredCaches.size() + " — жёлтый знак [M: карта]";
      } else if (!this.tolyaQuestAccepted) {
         return "Кликни на Толю и прими задание";
      } else if (!this.tolyaQuestComplete) {
         return this.score >= this.bottleGoal ? "Все бутылки собраны — вернись к Толе" : "Бутылки для Толи: " + this.score + "/" + this.bottleGoal;
      } else {
         return !this.sacredQuest.accepted() ? "У Толи есть новая легенда — кликни на него" : "Секретный подвал открыт на северо-востоке площади";
      }
   }

   private String cellarObjective() {
      int step = Math.min(2, Math.max(0, this.cellarQuest.stage()));
      String objective = this.cellarObjectives[step];
      return objective != null ? objective : "Осмотри ящик справа [ЛКМ]";
   }

   private void handleCellarClick(int screenX, int screenY, long now) {
      Rectangle exit = this.cellarExitBounds();
      Rectangle chest = this.cellarChestBounds();
      Rectangle brick = this.cellarBrickBounds();

      if (exit.contains(screenX, screenY)) {
         if (Math.hypot(this.secretX - 1.5, this.secretY - 5.5) <= 1.8) {
            this.leaveCellar(now);
         } else {
            this.showLine(this.dbText("quest.secret.too_far"), now);
         }
      } else if (chest.contains(screenX, screenY)) {
         if (this.cellarQuest.stage() > 0) {
            this.showLine(this.dbText("quest.secret.inspect"), now);
         } else if (Math.hypot(this.secretX - 7.0, this.secretY - 3.5) > 2.0) {
            this.showLine(this.dbText("quest.secret.too_far"), now);
         } else {
            this.advanceCellarQuest(new LocalGameStore.StoryState(true, false, 1), "quest.secret.inspect", now);
         }
      } else if (brick.contains(screenX, screenY)) {
         if (this.cellarQuest.stage() == 0) {
            this.showLine(this.dbText("quest.secret.first"), now);
         } else if (Math.hypot(this.secretX - 5.5, this.secretY - 4.5) > 1.8) {
            this.showLine(this.dbText("quest.secret.too_far"), now);
         } else if (this.cellarQuest.completed()) {
            this.showLine(this.dbText("quest.secret.note"), now);
         } else {
            this.advanceCellarQuest(new LocalGameStore.StoryState(true, true, 2), "quest.secret.note", now);
         }
      }
   }

   private void advanceCellarQuest(LocalGameStore.StoryState next, String dialogueKey, long now) {
      try {
         this.gameStore.saveStory(CELLAR_QUEST_ID, next);
         this.cellarQuest = next;
         this.showLine(this.dbText(dialogueKey), now);
      } catch (SQLException error) {
         System.err.println("Cellar progress save failed: " + error.getMessage());
         this.showLine(this.dbText("quest.secret.save_error"), now);
      }
   }

   private void leaveCellar(long now) {
      this.secretArea = false;
      this.showLine(this.dbText("quest.secret.exit"), now);
   }

   private Rectangle cellarExitBounds() {
      int tile = this.cellarTileSize();
      return new Rectangle((this.getWidth() - 10 * tile) / 2 + tile,
         (this.getHeight() - 56 - 8 * tile) / 2 + 5 * tile, tile, tile);
   }

   private Rectangle cellarChestBounds() {
      int tile = this.cellarTileSize();
      return new Rectangle((this.getWidth() - 10 * tile) / 2 + CELLAR_CHEST_X * tile,
         (this.getHeight() - 56 - 8 * tile) / 2 + CELLAR_CHEST_Y * tile, 2 * tile, tile);
   }

   private Rectangle cellarBrickBounds() {
      int tile = this.cellarTileSize();
      return new Rectangle((this.getWidth() - 10 * tile) / 2 + CELLAR_BRICK_X * tile,
         (this.getHeight() - 56 - 8 * tile) / 2 + CELLAR_BRICK_Y * tile, tile, tile);
   }

   private int cellarTileSize() {
      return Math.min(64, Math.max(32, (this.getHeight() - 56 - 70) / 8));
   }

   private void updateSecretArea() {
      if (this.interactionRequested) {
         this.interactionRequested = false;
         long now = System.currentTimeMillis();
         Rectangle target = this.cellarQuest.stage() == 0 && Math.hypot(this.secretX - 7.0, this.secretY - 3.5) <= 2.0
            ? this.cellarChestBounds()
            : this.cellarQuest.stage() == 1 && Math.hypot(this.secretX - 5.5, this.secretY - 4.5) <= 1.8
               ? this.cellarBrickBounds()
               : Math.hypot(this.secretX - 1.5, this.secretY - 5.5) <= 1.8 ? this.cellarExitBounds() : null;
         if (target != null) {
            this.handleCellarClick(target.x + target.width / 2, target.y + target.height / 2, now);
            if (!this.secretArea) {
               return;
            }
         }
      }
      double dx = (this.rightPressed ? (double)1.0F : (double)0.0F) - (this.leftPressed ? (double)1.0F : (double)0.0F);
      double dy = (this.downPressed ? (double)1.0F : (double)0.0F) - (this.upPressed ? (double)1.0F : (double)0.0F);
      if (dx == (double)0.0F && dy == (double)0.0F) {
         this.playerDir = 2;
      } else {
         double length = Math.hypot(dx, dy);
         this.secretX = Math.max((double)1.0F, Math.min((double)8.0F, this.secretX + dx / length * 0.115));
         this.secretY = Math.max(1.0, Math.min(6.0, this.secretY + dy / length * 0.115));
         this.playerDir = Math.abs(dy) > Math.abs(dx) ? (dy < (double)0.0F ? 3 : 4) : (dx < (double)0.0F ? 0 : 1);
      }

      this.animatePlayer();
      if (this.secretX < 1.3 && this.secretY > 4.4) {
         this.leaveCellar(System.currentTimeMillis());
      }

   }

   private void drawSecretArea(Graphics g) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setClip(0, 0, this.getWidth(), this.getHeight() - 56);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setColor(new Color(8, 13, 19));
      g2.fillRect(0, 0, this.getWidth(), this.getHeight() - 56);
      if (this.cellarBackground != null) {
         int sceneHeight = this.getHeight() - 56;
         double scale = Math.max(this.getWidth() / (double)this.cellarBackground.getWidth(null),
            sceneHeight / (double)this.cellarBackground.getHeight(null));
         int drawWidth = (int)Math.ceil(this.cellarBackground.getWidth(null) * scale);
         int drawHeight = (int)Math.ceil(this.cellarBackground.getHeight(null) * scale);
         g2.drawImage(this.cellarBackground, (this.getWidth() - drawWidth) / 2,
            (sceneHeight - drawHeight) / 2, drawWidth, drawHeight, null);
      }

      int bannerX = 24;
      int bannerY = 18;
      g2.setColor(new Color(12, 18, 25, 225));
      g2.fillRoundRect(bannerX, bannerY, Math.min(680, this.getWidth() - 48), 88, 10, 10);
      g2.setColor(new Color(255, 209, 140));
      g2.setFont(new Font("Dialog", 1, 18));
      g2.drawString("ЗАБРОШЕННЫЙ ПОДВАЛ ЗУЕВКИ", bannerX + 16, bannerY + 23);
      g2.setColor(new Color(245, 238, 222));
      g2.setFont(new Font("Dialog", 1, 15));
      g2.drawString(this.cellarObjective(), bannerX + 16, bannerY + 47);
      g2.setFont(new Font("Dialog", 0, 13));
      g2.drawString("WASD / стрелки — идти     ЛКМ / E — осмотреть или выйти", bannerX + 16, bannerY + 68);

      Rectangle exit = this.cellarExitBounds();
      g2.setColor(new Color(20, 16, 11, 190));
      g2.fillRoundRect(exit.x + 9, exit.y + 12, exit.width - 18, exit.height - 20, 6, 6);
      g2.setColor(new Color(232, 167, 84));
      g2.drawRoundRect(exit.x + 9, exit.y + 12, exit.width - 19, exit.height - 21, 6, 6);
      g2.setColor(new Color(12, 18, 25, 205));
      g2.fillRoundRect(exit.x + 5, exit.y + 16, exit.width - 10, 24, 6, 6);
      g2.setColor(new Color(255, 216, 130));
      g2.setFont(new Font("Dialog", 1, 12));
      g2.drawString("ВЫХОД", exit.x + (exit.width - g2.getFontMetrics().stringWidth("ВЫХОД")) / 2, exit.y + 33);

      Rectangle chest = this.cellarChestBounds();
      g2.setColor(new Color(21, 12, 8, 205));
      g2.fillRoundRect(chest.x + 3, chest.y + 20, chest.width - 6, chest.height - 20, 7, 7);
      g2.setColor(this.cellarQuest.stage() == 0 ? new Color(112, 59, 28) : new Color(66, 49, 38));
      g2.fillRoundRect(chest.x + 8, chest.y + 23, chest.width - 16, chest.height - 27, 5, 5);
      g2.setColor(this.cellarQuest.stage() == 0 ? new Color(238, 184, 80) : new Color(143, 120, 84));
      g2.drawRoundRect(chest.x + 8, chest.y + 23, chest.width - 17, chest.height - 28, 5, 5);
      g2.drawLine(chest.x + 12, chest.y + 39, chest.x + chest.width - 12, chest.y + 39);
      g2.drawLine(chest.x + 25, chest.y + 25, chest.x + 25, chest.y + chest.height - 8);
      g2.drawLine(chest.x + chest.width - 25, chest.y + 25, chest.x + chest.width - 25, chest.y + chest.height - 8);
      g2.fillRect(chest.x + chest.width / 2 - 4, chest.y + 38, 8, 8);
      g2.setColor(new Color(12, 18, 25, 220));
      g2.fillRoundRect(chest.x + 10, chest.y - 6, chest.width - 20, 23, 6, 6);
      g2.setColor(new Color(255, 221, 166));
      g2.setFont(new Font("Dialog", 1, 12));
      String chestName = this.cellarQuest.stage() == 0 ? "ЯЩИК [ЛКМ]" : "ПУСТОЙ «ТИЧ»";
      g2.drawString(chestName, chest.x + (chest.width - g2.getFontMetrics().stringWidth(chestName)) / 2, chest.y + 10);

      if (this.cellarQuest.stage() > 0) {
         Rectangle brick = this.cellarBrickBounds();
         g2.setColor(new Color(20, 15, 10, 180));
         g2.fillRoundRect(brick.x + 7, brick.y + 7, brick.width - 14, brick.height - 14, 5, 5);
         g2.setColor(this.cellarQuest.completed() ? new Color(170, 145, 100) : new Color(249, 193, 72));
         g2.drawRoundRect(brick.x + 7, brick.y + 7, brick.width - 15, brick.height - 15, 4, 4);
         g2.setFont(new Font("Dialog", 1, 20));
         String mark = this.cellarQuest.completed() ? "!" : "?";
         g2.drawString(mark, brick.x + (brick.width - g2.getFontMetrics().stringWidth(mark)) / 2, brick.y + brick.height / 2 + 7);
      }
      if (this.playerSprites != null) {
         Image[] track = this.playerSprites[this.playerDir];
         Image frame = track[Math.floorMod(this.animFrame, track.length)];
         int tile = this.cellarTileSize();
         int originX = (this.getWidth() - 10 * tile) / 2;
         int originY = (this.getHeight() - 56 - 8 * tile) / 2;
         int size = (int)Math.round(tile * 1.7);
         int px = originX + (int)(this.secretX * tile) - (size - tile) / 2;
         int py = originY + (int)(this.secretY * tile) - (size - tile);
         g2.drawImage(frame, px, py, size, size, (ImageObserver)null);
      }

      g2.dispose();
   }

   private void drawDialogue(Graphics g, String line) {
      Graphics2D g2 = (Graphics2D)g.create();
      int y = this.getHeight() - 56 - 70;
      g2.setColor(new Color(12, 18, 30, 225));
      g2.fillRoundRect(24, y, this.getWidth() - 48, 54, 12, 12);
      g2.setColor(new Color(236, 174, 105));
      g2.drawRoundRect(24, y, this.getWidth() - 49, 53, 12, 12);
      g2.setFont(new Font("Dialog", 1, 18));
      g2.setColor(new Color(255, 237, 209));
      g2.drawString(line, 42, y + 34);
      g2.dispose();
   }

   private void enableKeys() {
      this.addKeyListener(new KeyAdapter() {
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case 37:
               case 65:
                  Game.this.leftPressed = true;
                  break;
               case 38:
               case 87:
                  Game.this.upPressed = true;
                  break;
               case 39:
               case 68:
                  Game.this.rightPressed = true;
                  break;
               case 40:
               case 83:
                  Game.this.downPressed = true;
                  break;
               case 69:
                  Game.this.interactionRequested = true;
                  break;
               case 77:
                  if (Game.this.sacredQuest.accepted() && !Game.this.secretArea && Game.this.questPanel == null) {
                     Game.this.questMapOpen = !Game.this.questMapOpen;
                  }
                  break;
               case 82:
                  Game.this.restartRequested = true;
            }

         }

         public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
               case 37:
               case 65:
                  Game.this.leftPressed = false;
                  break;
               case 38:
               case 87:
                  Game.this.upPressed = false;
                  break;
               case 39:
               case 68:
                  Game.this.rightPressed = false;
                  break;
               case 40:
               case 83:
                  Game.this.downPressed = false;
            }

         }
      });
   }

   public void start() {
      if (!this.running) {
         this.running = true;
         Thread loopThread = new Thread(this, "alkosmen-game-loop");
         loopThread.start();
      }
   }

   public void stopGame() {
      this.running = false;
   }

   private void stopAudio() {
      if (this.levelMidi != null) {
         this.levelMidi.stop();
      }

   }

   private boolean isInsideMap(int x, int y) {
      return y >= 0 && y < this.levelMap.length && x >= 0 && x < this.levelMap[0].length;
   }

   private boolean isSolid(int x, int y) {
      if (!this.isInsideMap(x, y)) {
         return true;
      } else {
         return this.levelMap[y][x] == '#';
      }
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private Image getImage(String fileName) {
      try {
         String full = "alkosmen/images/objects/alkoman/" + fileName;
         URL url = this.getClass().getClassLoader().getResource(full);
         if (url == null) {
            throw new RuntimeException("Image not found: " + full);
         } else {
            BufferedImage sourceImage = ImageIO.read(url);
            return sourceImage;
         }
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private Image loadImageResource(String path) {
      String normalized = path.startsWith("/") ? path.substring(1) : path;
      URL url = this.getClass().getClassLoader().getResource(normalized);
      if (url == null) {
         throw new RuntimeException("Image not found: " + path);
      } else {
         try {
            return ImageIO.read(url);
         } catch (IOException e) {
            throw new RuntimeException("Failed to read image: " + path, e);
         }
      }
   }

   private Image loadFirstExistingImage(String... paths) {
      for(String path : paths) {
         String normalized = path.startsWith("/") ? path.substring(1) : path;
         URL url = this.getClass().getClassLoader().getResource(normalized);
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

   private boolean isNpcTile(char c) {
      return c == 'N' || c == 'M' || c == 'V' || c == 'G' || c == 'C' || this.townNpcFrames.containsKey(c);
   }

   private int npcDrawSize(char c, int cell) {
      return (int)Math.round(cell * (this.townNpcFrames.containsKey(c) ? 2.0 : 1.7));
   }

   private String npcDialogueKey(char c) {
      return switch (c) {
         case 'M' -> "npc.merchant";
         case 'G' -> "npc.cop";
         case 'H' -> "npc.red_brother";
         case 'I' -> "npc.red_sister";
         case 'D' -> "npc.dnb_partygoer";
         case 'J' -> "npc.train_gopnik";
         case 'K' -> "npc.train_conductor";
         case 'S' -> "npc.suspicious_stranger";
         default -> "npc.near";
      };
   }

   private void loadTownNpc(char tile, String fileName) throws IOException {
      this.townNpcFrames.put(tile, CharacterSpriteAssets.loadGridAtlas(
         "/alkosmen/ui/sprites/npc/" + fileName + ".png", 4, 4)[0]);
   }

   private Image npcImageFor(char c, long sceneTime) {
      Image[] frames = this.townNpcFrames.get(c);
      if (frames != null) {
         return frames[(int)Math.floorMod(sceneTime / 240L + c, frames.length)];
      }
      Image var10000;
      switch (c) {
         case 'C' -> var10000 = this.npcCopSprite;
         case 'G' -> var10000 = this.npcCopSprite;
         case 'M' -> var10000 = this.npcBoy2Sprite;
         case 'N' -> var10000 = this.npcTolyaSprite;
         case 'V' -> var10000 = this.npcEboboSprite;
         default -> var10000 = null;
      }

      return var10000;
   }

   private Image[][] getAlkobotImages() {
      try {
         BufferedImage[][] atlas = CharacterSpriteAssets.loadGridAtlas("/alkosmen/ui/sprites/alkosmen/walk_atlas_v1.png", 4, 5, 30);
         return new Image[][]{atlas[2], atlas[1], atlas[0], atlas[4], atlas[3]};
      } catch (IOException error) {
         throw new IllegalStateException("Could not load Alkosmen walk atlas", error);
      }
   }

   private Image[] loadTrackFrames(String trackName) {
      Image[] frames = new Image[10];

      for(int i = 0; i < frames.length; ++i) {
         String framePath = String.format("/alkosmen/images/objects/alkoman/frames/alk_%s_%02d.png", trackName, i);
         frames[i] = this.loadImageResource(framePath);
      }

      return frames;
   }

   private Image[] loadCopTrackFrames(String trackName) {
      Image[] frames = new Image[8];

      for(int i = 0; i < frames.length; ++i) {
         String framePath = String.format("/alkosmen/images/objects/cop/male/%s/%02d.png", trackName, i);
         frames[i] = this.removeWhiteBackdrop(this.loadImageResource(framePath));
      }

      return frames;
   }

   private Image removeWhiteBackdrop(Image source) {
      int width = source.getWidth((ImageObserver)null);
      int height = source.getHeight((ImageObserver)null);
      BufferedImage cleaned = new BufferedImage(width, height, 2);
      Graphics2D graphics = cleaned.createGraphics();
      graphics.drawImage(source, 0, 0, (ImageObserver)null);
      graphics.dispose();
      boolean[] removed = new boolean[width * height];
      ArrayDeque<Integer> pending = new ArrayDeque();

      for(int x = 0; x < width; ++x) {
         this.addWhiteBackgroundPixel(cleaned, x, 0, removed, pending);
         this.addWhiteBackgroundPixel(cleaned, x, height - 1, removed, pending);
      }

      for(int y = 1; y < height - 1; ++y) {
         this.addWhiteBackgroundPixel(cleaned, 0, y, removed, pending);
         this.addWhiteBackgroundPixel(cleaned, width - 1, y, removed, pending);
      }

      while(!pending.isEmpty()) {
         int index = (Integer)pending.removeFirst();
         int x = index % width;
         int y = index / width;
         cleaned.setRGB(x, y, cleaned.getRGB(x, y) & 16777215);
         if (x > 0) {
            this.addWhiteBackgroundPixel(cleaned, x - 1, y, removed, pending);
         }

         if (x + 1 < width) {
            this.addWhiteBackgroundPixel(cleaned, x + 1, y, removed, pending);
         }

         if (y > 0) {
            this.addWhiteBackgroundPixel(cleaned, x, y - 1, removed, pending);
         }

         if (y + 1 < height) {
            this.addWhiteBackgroundPixel(cleaned, x, y + 1, removed, pending);
         }
      }

      for(int y = 0; y < height; ++y) {
         for(int x = 0; x < width; ++x) {
            int color = cleaned.getRGB(x, y);
            int red = color >>> 16 & 255;
            int green = color >>> 8 & 255;
            int blue = color & 255;
            int spread = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
            if (red >= 220 && green >= 220 && blue >= 220 && spread <= 18) {
               cleaned.setRGB(x, y, color & 16777215);
            }
         }
      }

      return cleaned;
   }

   private void addWhiteBackgroundPixel(BufferedImage image, int x, int y, boolean[] removed, ArrayDeque pending) {
      int index = y * image.getWidth() + x;
      if (!removed[index]) {
         int color = image.getRGB(x, y);
         int red = color >>> 16 & 255;
         int green = color >>> 8 & 255;
         int blue = color & 255;
         int spread = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
         if (red >= 220 && green >= 220 && blue >= 220 && spread <= 18) {
            removed[index] = true;
            pending.addLast(index);
         }
      }
   }

   private void loadLevel(int level) throws Exception {
      if (level >= 1 && level <= LEVELS.length) {
         String path = LEVELS[level - 1];
         URL url = LevelLoader.class.getResource(path);
         if (url == null) {
            throw new RuntimeException("Level resource not found: " + path);
         } else {
            this.levelMap = LevelLoader.load(path);
            if (this.levelMap == null) {
               throw new RuntimeException("LevelLoader.load returned NULL for: " + path);
            } else if (this.levelMap.length != 0 && this.levelMap[0].length != 0) {
               String backgroundPath = LEVEL_BACKGROUNDS[level - 1];
               this.levelBackground = backgroundPath == null ? null : this.loadImageResource(backgroundPath);
               this.currentLevel = level;
               this.score = 0;
               this.bottleGoal = this.countTiles('B');
               this.player = null;
               this.copSystem.reset();
               this.patrols.clear();

               for(int y = 0; y < this.levelMap.length; ++y) {
                  for(int x = 0; x < this.levelMap[0].length; ++x) {
                     if (this.levelMap[y][x] == 'P') {
                        this.playerSpawnX = x;
                        this.playerSpawnY = y;
                        this.player = new Player(x, y);
                        this.levelMap[y][x] = '.';
                     } else if (this.levelMap[y][x] == 'C') {
                        int index = this.patrols.size();
                        this.patrols.add(new TopDownPatrol((double)x, (double)y, index % 2 == 0 ? 1 : -1, this.gameStore.loadRoute("patrol_" + index)));
                        this.levelMap[y][x] = '.';
                     }
                  }
               }

               if (this.player == null) {
                  throw new RuntimeException("No 'P' (player start) in map: " + path);
               } else {
                  this.spectatorMode = false;
                  this.playerDir = 2;
                  this.animFrame = 0;
                  this.playerMotionTick = 0;
                  this.cameraX = 0.0F;
                  this.cameraY = 0.0F;
                  this.jumpPressed = false;
                  this.jumpQueued = false;
                  this.jumpBufferUntil = 0L;
                  this.lastOnGroundAt = 0L;
                  this.hidePressed = false;
                  this.interactionRequested = false;
                  this.dialogueLine = "";
                  this.dialogueUntil = 0L;
                  this.tolyaQuestAccepted = false;
                  this.tolyaQuestComplete = false;
                  this.questPanel = null;
                  this.secretArea = false;
                  this.questMapOpen = false;
                  this.ufoUntil = 0L;
                  this.pendingClick = -1L;
                  this.upPressed = false;
                  this.downPressed = false;
                  this.gameOver = false;
                  this.levelComplete = false;
                  this.levelGoalReached = false;
                  this.lastPatrolCaughtAt = 0L;
                  this.lives = 3;
                  LocalGameStore.QuestState savedQuest = this.gameStore.loadTolyaQuest();
                  this.eboboPhotos = this.gameStore.loadQuestSteps("ebobo_ufo");
                  this.sacredCaches = this.gameStore.loadQuestSteps("sacred_tich");
                  this.sacredMapTitle = this.gameStore.text("quest.sacred.map.title");
                  List<String> hints = new ArrayList();

                  for(int i = 0; i < this.sacredCaches.size(); ++i) {
                     hints.add(this.gameStore.text("quest.sacred.hint." + i));
                  }

                  this.sacredHints = List.copyOf(hints);
                  List<LocalGameStore.QuestStep> exits = this.gameStore.loadQuestSteps("secret_entrance");
                  this.secretEntrance = exits.isEmpty() ? null : ((LocalGameStore.QuestStep)exits.get(0)).tile();
                  this.eboboQuest = this.gameStore.loadStory("ebobo_ufo");
                  this.sacredQuest = this.gameStore.loadStory("sacred_tich");
                  this.cellarQuest = this.gameStore.loadStory(CELLAR_QUEST_ID);
                  this.cellarObjectives[0] = this.gameStore.text("quest.secret.objective.0");
                  this.cellarObjectives[1] = this.gameStore.text("quest.secret.objective.1");
                  this.cellarObjectives[2] = this.gameStore.text("quest.secret.objective.done");
                  this.tolyaQuestAccepted = savedQuest.accepted();
                  this.tolyaQuestComplete = savedQuest.completed();

                  for(LocalGameStore.Tile tile : savedQuest.collected()) {
                     if (this.isInsideMap(tile.x(), tile.y()) && this.levelMap[tile.y()][tile.x()] == 'B') {
                        this.levelMap[tile.y()][tile.x()] = '.';
                        ++this.score;
                     }
                  }

                  this.levelGoalReached = this.score >= this.bottleGoal && this.bottleGoal > 0;
                  Window w = SwingUtilities.getWindowAncestor(this);
                  if (w != null) {
                     w.pack();
                  }

               }
            } else {
               throw new RuntimeException("Loaded empty map for: " + path);
            }
         }
      } else {
         throw new IllegalArgumentException("Bad level: " + level);
      }
   }

   private int countTiles(char target) {
      int count = 0;

      for(int y = 0; y < this.levelMap.length; ++y) {
         for(int x = 0; x < this.levelMap[0].length; ++x) {
            if (this.levelMap[y][x] == target) {
               ++count;
            }
         }
      }

      return count;
   }

   private void respawnPlayer() {
      this.player.x = (double)this.playerSpawnX;
      this.player.y = (double)this.playerSpawnY;
      this.player.vx = (double)0.0F;
      this.player.vy = (double)0.0F;
      this.player.onGround = false;
      this.leftPressed = false;
      this.rightPressed = false;
      this.upPressed = false;
      this.downPressed = false;
      this.jumpPressed = false;
      this.jumpQueued = false;
      this.hidePressed = false;
   }

   private boolean isPlayerHidden() {
      if (this.spectatorMode) {
         return false;
      } else {
         return this.hidePressed && this.player != null && Math.abs(this.player.vx) < 1.0E-4 && this.player.onGround;
      }
   }

   private void rotateCityLine(long now, boolean firstLine) {
      String speakers = this.lore.sceneText("scene.city.speakers", "tolya_mozol,vyatskiy_ebobo,cops");
      String fallback = this.lore.sceneText("scene.city.default_line", "Ночной город живет по своим правилам.");
      String[] speakerIds = speakers.split(",");
      String selectedSpeaker = speakerIds[ThreadLocalRandom.current().nextInt(speakerIds.length)].trim();

      String picked;
      try {
         picked = this.gameStore.randomDialogue(selectedSpeaker, fallback);
      } catch (SQLException var10) {
         picked = fallback;
      }

      if (firstLine && !picked.isBlank()) {
         LoreCharacter hero = this.lore.character("alkosmen");
         String var10001 = hero.name();
         this.cityLine = var10001 + ": " + hero.role() + ". " + picked;
      } else {
         this.cityLine = picked;
      }

      this.nextCityLineAt = now + 9000L;
   }

   private void spawnRandomCops(int targetCount) {
      int h = this.levelMap.length;
      int w = this.levelMap[0].length;
      int added = 0;

      for(int i = 0; i < 800 && added < targetCount; ++i) {
         int x = ThreadLocalRandom.current().nextInt(1, Math.max(2, w - 1));
         int y = ThreadLocalRandom.current().nextInt(1, Math.max(2, h - 1));
         if (this.levelMap[y][x] == '.' && y + 1 < h && this.levelMap[y + 1][x] == '#') {
            this.copSystem.addCop(x, y);
            ++added;
         }
      }

   }

   private void drawLevelBackground(Graphics g, int gameplayHeight) {
      int drawW = this.levelMap[0].length * Constants.Size;
      int drawH = this.levelMap.length * Constants.Size;
      g.drawImage(this.levelBackground, -((int)this.cameraX), -((int)this.cameraY), drawW, drawH, (ImageObserver)null);
   }

   private void drawMazeWall(Graphics g, int x, int y, int cell) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setColor(new Color(16, 22, 30, 220));
      g2.fillRoundRect(x + 2, y + 2, cell - 4, cell - 4, 6, 6);
      g2.setColor(new Color(94, 106, 119));
      g2.drawRoundRect(x + 2, y + 2, cell - 5, cell - 5, 6, 6);
      g2.setColor(new Color(42, 50, 62));
      g2.drawLine(x + 5, y + cell / 2, x + cell - 5, y + cell / 2);
      g2.drawLine(x + cell / 2, y + 5, x + cell / 2, y + cell - 5);
      g2.dispose();
   }

   private void drawExit(Graphics g, int x, int y, int cell, boolean active) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setColor(active ? new Color(85, 240, 180, 210) : new Color(80, 80, 90, 190));
      g2.fillOval(x + 4, y + 4, cell - 8, cell - 8);
      g2.setColor(active ? Color.WHITE : new Color(170, 170, 175));
      g2.drawOval(x + 4, y + 4, cell - 8, cell - 8);
      g2.dispose();
   }

   private void renderLoadingScreen(String text) {
      BufferStrategy bs = this.strategy;
      if (bs != null) {
         Graphics g = bs.getDrawGraphics();
         g.setColor(Color.BLACK);
         g.fillRect(0, 0, this.getWidth(), this.getHeight());
         g.setColor(Color.WHITE);
         g.setFont(new Font("Monospaced", 1, 36));
         int textW = g.getFontMetrics().stringWidth(text);
         int textX = Math.max(12, (this.getWidth() - textW) / 2);
         int textY = Math.max(48, this.getHeight() / 2);
         g.drawString(text, textX, textY);
         String teaser = this.lore.sceneText("scene.menu.teaser", "Ночной город не спит: на линии мент-патруль и городские чудики.");
         g.setColor(new Color(190, 205, 220));
         g.setFont(new Font("Dialog", 0, 16));
         int teaserW = g.getFontMetrics().stringWidth(teaser);
         int teaserX = Math.max(12, (this.getWidth() - teaserW) / 2);
         g.drawString(teaser, teaserX, textY + 30);
         g.dispose();
         bs.show();
      }
   }

   private static enum PanelAction {
      ACCEPT,
      TURN_IN,
      CLOSE;

      // $FF: synthetic method
      private static PanelAction[] $values() {
         return new PanelAction[]{ACCEPT, TURN_IN, CLOSE};
      }
   }

   private static record QuestPanel(String questId, String title, String body, String button, PanelAction action) {
   }


}
