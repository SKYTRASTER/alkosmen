package alkosmen.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Set;

public final class LocalGameStore {
   public static final String TOLYA_QUEST = "tolya_bottles";
   public static final String EBOBO_QUEST = "ebobo_ufo";
   public static final String SACRED_BOTTLE_QUEST = "sacred_tich";
   public static final String SECRET_ENTRANCE = "secret_entrance";
   private final Path databasePath;

   public static LocalGameStore openDefault() throws IOException, SQLException {
      String appData = System.getenv("APPDATA");
      Path folder = appData != null && !appData.isBlank() ? Path.of(appData, "Alkosmen") : Path.of(System.getProperty("user.home"), ".alkosmen");
      return new LocalGameStore(folder.resolve("save.sqlite"));
   }

   public LocalGameStore(Path databasePath) throws IOException, SQLException {
      this.databasePath = databasePath.toAbsolutePath();
      Files.createDirectories(this.databasePath.getParent());
      Connection connection = this.connect();

      try {
         Statement statement = connection.createStatement();

         try {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS npc_routes (\n    npc_id TEXT NOT NULL,\n    step_index INTEGER NOT NULL,\n    tile_x INTEGER NOT NULL,\n    tile_y INTEGER NOT NULL,\n    pause_ms INTEGER NOT NULL DEFAULT 0,\n    PRIMARY KEY (npc_id, step_index)\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_progress (\n    quest_id TEXT PRIMARY KEY,\n    accepted INTEGER NOT NULL DEFAULT 0,\n    completed INTEGER NOT NULL DEFAULT 0,\n    updated_at INTEGER NOT NULL\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_collections (\n    quest_id TEXT NOT NULL,\n    tile_x INTEGER NOT NULL,\n    tile_y INTEGER NOT NULL,\n    PRIMARY KEY (quest_id, tile_x, tile_y)\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_stages (\n    quest_id TEXT PRIMARY KEY,\n    stage INTEGER NOT NULL DEFAULT 0\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS dialogue_texts (\n    text_key TEXT PRIMARY KEY,\n    body TEXT NOT NULL\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_steps (\n    quest_id TEXT NOT NULL,\n    step_index INTEGER NOT NULL,\n    tile_x INTEGER NOT NULL,\n    tile_y INTEGER NOT NULL,\n    text_key TEXT NOT NULL,\n    PRIMARY KEY (quest_id, step_index)\n)\n");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_scene_stages (quest_id TEXT NOT NULL, stage INTEGER NOT NULL, objective_key TEXT NOT NULL, target_id TEXT NOT NULL, target_label_key TEXT NOT NULL, PRIMARY KEY (quest_id, stage))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_scene_targets (quest_id TEXT NOT NULL, target_id TEXT NOT NULL, tile_x REAL NOT NULL, tile_y REAL NOT NULL, width REAL NOT NULL, height REAL NOT NULL, reach_x REAL NOT NULL, reach_y REAL NOT NULL, reach_radius REAL NOT NULL, PRIMARY KEY (quest_id, target_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS quest_scene_events (quest_id TEXT NOT NULL, stage INTEGER NOT NULL, target_id TEXT NOT NULL, action TEXT NOT NULL, next_stage INTEGER NOT NULL, completed INTEGER NOT NULL, dialogue_key TEXT NOT NULL, too_far_key TEXT NOT NULL, PRIMARY KEY (quest_id, stage, target_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS npc_quest_offers (npc_id TEXT NOT NULL, priority INTEGER NOT NULL, quest_id TEXT NOT NULL, offer_prefix TEXT NOT NULL, progress_prefix TEXT NOT NULL, turnin_prefix TEXT NOT NULL, done_prefix TEXT NOT NULL, finish_line_key TEXT NOT NULL, PRIMARY KEY (npc_id, priority))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_migrations (\n    migration_id TEXT PRIMARY KEY\n)\n");
         } catch (Throwable var8) {
            if (statement != null) {
               try {
                  statement.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (statement != null) {
            statement.close();
         }
      } catch (Throwable var9) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }
         }

         throw var9;
      }

      if (connection != null) {
         connection.close();
      }

      this.seedDialogues();
      this.seedQuestSteps();
      this.seedSceneScripts();
      this.seedNpcQuestOffers();
      this.migrateFirstSacredCache();
      this.migrateSacredCacheExpansion();
      this.seedNpcRoutes();
   }

   public Path databasePath() {
      return this.databasePath;
   }

   public List loadRoute(String npcId) throws SQLException {
      List<Waypoint> route = new ArrayList();
      Connection connection = this.connect();

      try {
         PreparedStatement query = connection.prepareStatement("SELECT tile_x, tile_y, pause_ms FROM npc_routes\nWHERE npc_id = ? ORDER BY step_index\n");

         try {
            query.setString(1, npcId);
            ResultSet rows = query.executeQuery();

            try {
               while(rows.next()) {
                  route.add(new Waypoint(rows.getInt(1), rows.getInt(2), rows.getInt(3)));
               }
            } catch (Throwable var11) {
               if (rows != null) {
                  try {
                     rows.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (rows != null) {
               rows.close();
            }
         } catch (Throwable var12) {
            if (query != null) {
               try {
                  query.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (query != null) {
            query.close();
         }
      } catch (Throwable var13) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }
         }

         throw var13;
      }

      if (connection != null) {
         connection.close();
      }

      return List.copyOf(route);
   }

   public QuestState loadTolyaQuest() throws SQLException {
      boolean accepted = false;
      boolean completed = false;
      Set<Tile> collected = new HashSet();
      Connection connection = this.connect();

      try {
         PreparedStatement progress = connection.prepareStatement("SELECT accepted, completed FROM quest_progress WHERE quest_id = ?\n");

         try {
            PreparedStatement bottles = connection.prepareStatement("SELECT tile_x, tile_y FROM quest_collections WHERE quest_id = ?\n");

            try {
               progress.setString(1, "tolya_bottles");
               ResultSet row = progress.executeQuery();

               try {
                  if (row.next()) {
                     accepted = row.getInt(1) != 0;
                     completed = row.getInt(2) != 0;
                  }
               } catch (Throwable var15) {
                  if (row != null) {
                     try {
                        row.close();
                     } catch (Throwable var14) {
                        var15.addSuppressed(var14);
                     }
                  }

                  throw var15;
               }

               if (row != null) {
                  row.close();
               }

               bottles.setString(1, "tolya_bottles");
               row = bottles.executeQuery();

               try {
                  while(row.next()) {
                     collected.add(new Tile(row.getInt(1), row.getInt(2)));
                  }
               } catch (Throwable var16) {
                  if (row != null) {
                     try {
                        row.close();
                     } catch (Throwable var13) {
                        var16.addSuppressed(var13);
                     }
                  }

                  throw var16;
               }

               if (row != null) {
                  row.close();
               }
            } catch (Throwable var17) {
               if (bottles != null) {
                  try {
                     bottles.close();
                  } catch (Throwable var12) {
                     var17.addSuppressed(var12);
                  }
               }

               throw var17;
            }

            if (bottles != null) {
               bottles.close();
            }
         } catch (Throwable var18) {
            if (progress != null) {
               try {
                  progress.close();
               } catch (Throwable var11) {
                  var18.addSuppressed(var11);
               }
            }

            throw var18;
         }

         if (progress != null) {
            progress.close();
         }
      } catch (Throwable var19) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var10) {
               var19.addSuppressed(var10);
            }
         }

         throw var19;
      }

      if (connection != null) {
         connection.close();
      }

      return new QuestState(accepted, completed, Set.copyOf(collected));
   }

   public void saveTolyaQuest(boolean accepted, boolean completed) throws SQLException {
      Connection connection = this.connect();

      try {
         PreparedStatement command = connection.prepareStatement("INSERT INTO quest_progress (quest_id, accepted, completed, updated_at)\nVALUES (?, ?, ?, ?)\nON CONFLICT(quest_id) DO UPDATE SET\n    accepted = excluded.accepted,\n    completed = excluded.completed,\n    updated_at = excluded.updated_at\n");

         try {
            command.setString(1, "tolya_bottles");
            command.setInt(2, accepted ? 1 : 0);
            command.setInt(3, completed ? 1 : 0);
            command.setLong(4, System.currentTimeMillis());
            command.executeUpdate();
         } catch (Throwable var9) {
            if (command != null) {
               try {
                  command.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (command != null) {
            command.close();
         }
      } catch (Throwable var10) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (connection != null) {
         connection.close();
      }

   }

   public StoryState loadStory(String questId) throws SQLException {
      boolean accepted = false;
      boolean completed = false;
      int stage = 0;
      Connection connection = this.connect();

      try {
         PreparedStatement progress = connection.prepareStatement("SELECT accepted, completed FROM quest_progress WHERE quest_id = ?");

         try {
            PreparedStatement stages = connection.prepareStatement("SELECT stage FROM quest_stages WHERE quest_id = ?");

            try {
               progress.setString(1, questId);
               ResultSet row = progress.executeQuery();

               try {
                  if (row.next()) {
                     accepted = row.getInt(1) != 0;
                     completed = row.getInt(2) != 0;
                  }
               } catch (Throwable var17) {
                  if (row != null) {
                     try {
                        row.close();
                     } catch (Throwable var15) {
                        var17.addSuppressed(var15);
                     }
                  }

                  throw var17;
               }

               if (row != null) {
                  row.close();
               }

               stages.setString(1, questId);
               row = stages.executeQuery();

               try {
                  if (row.next()) {
                     stage = row.getInt(1);
                  }
               } catch (Throwable var16) {
                  if (row != null) {
                     try {
                        row.close();
                     } catch (Throwable var14) {
                        var16.addSuppressed(var14);
                     }
                  }

                  throw var16;
               }

               if (row != null) {
                  row.close();
               }
            } catch (Throwable var18) {
               if (stages != null) {
                  try {
                     stages.close();
                  } catch (Throwable var13) {
                     var18.addSuppressed(var13);
                  }
               }

               throw var18;
            }

            if (stages != null) {
               stages.close();
            }
         } catch (Throwable var19) {
            if (progress != null) {
               try {
                  progress.close();
               } catch (Throwable var12) {
                  var19.addSuppressed(var12);
               }
            }

            throw var19;
         }

         if (progress != null) {
            progress.close();
         }
      } catch (Throwable var20) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var11) {
               var20.addSuppressed(var11);
            }
         }

         throw var20;
      }

      if (connection != null) {
         connection.close();
      }

      return new StoryState(accepted, completed, stage);
   }

   public void saveStory(String questId, StoryState state) throws SQLException {
      Connection connection = this.connect();

      try {
         connection.setAutoCommit(false);

         try {
            PreparedStatement progress = connection.prepareStatement("INSERT INTO quest_progress (quest_id, accepted, completed, updated_at)\nVALUES (?, ?, ?, ?)\nON CONFLICT(quest_id) DO UPDATE SET\n    accepted = excluded.accepted,\n    completed = excluded.completed,\n    updated_at = excluded.updated_at\n");

            try {
               PreparedStatement stages = connection.prepareStatement("INSERT INTO quest_stages (quest_id, stage) VALUES (?, ?)\nON CONFLICT(quest_id) DO UPDATE SET stage = excluded.stage\n");

               try {
                  progress.setString(1, questId);
                  progress.setInt(2, state.accepted() ? 1 : 0);
                  progress.setInt(3, state.completed() ? 1 : 0);
                  progress.setLong(4, System.currentTimeMillis());
                  progress.executeUpdate();
                  stages.setString(1, questId);
                  stages.setInt(2, state.stage());
                  stages.executeUpdate();
                  connection.commit();
               } catch (Throwable var11) {
                  if (stages != null) {
                     try {
                        stages.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (stages != null) {
                  stages.close();
               }
            } catch (Throwable var12) {
               if (progress != null) {
                  try {
                     progress.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (progress != null) {
               progress.close();
            }
         } catch (SQLException error) {
            connection.rollback();
            throw error;
         }
      } catch (Throwable var14) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var8) {
               var14.addSuppressed(var8);
            }
         }

         throw var14;
      }

      if (connection != null) {
         connection.close();
      }

   }

   public String text(String key) throws SQLException {
      Connection connection = this.connect();

      String var5;
      try {
         PreparedStatement query = connection.prepareStatement("SELECT body FROM dialogue_texts WHERE text_key = ?");

         try {
            query.setString(1, key);
            ResultSet row = query.executeQuery();

            try {
               var5 = row.next() ? row.getString(1) : key;
            } catch (Throwable var10) {
               if (row != null) {
                  try {
                     row.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (row != null) {
               row.close();
            }
         } catch (Throwable var11) {
            if (query != null) {
               try {
                  query.close();
               } catch (Throwable var8) {
                  var11.addSuppressed(var8);
               }
            }

            throw var11;
         }

         if (query != null) {
            query.close();
         }
      } catch (Throwable var12) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var7) {
               var12.addSuppressed(var7);
            }
         }

         throw var12;
      }

      if (connection != null) {
         connection.close();
      }

      return var5;
   }

   public String randomDialogue(String speaker, String fallback) throws SQLException {
      Connection connection = this.connect();

      String var6;
      try {
         PreparedStatement query = connection.prepareStatement("SELECT body FROM dialogue_texts WHERE text_key LIKE ?\nORDER BY RANDOM() LIMIT 1\n");

         try {
            query.setString(1, "dialogue." + speaker + ".%");
            ResultSet row = query.executeQuery();

            try {
               var6 = row.next() ? row.getString(1) : fallback;
            } catch (Throwable var11) {
               if (row != null) {
                  try {
                     row.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (row != null) {
               row.close();
            }
         } catch (Throwable var12) {
            if (query != null) {
               try {
                  query.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (query != null) {
            query.close();
         }
      } catch (Throwable var13) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }
         }

         throw var13;
      }

      if (connection != null) {
         connection.close();
      }

      return var6;
   }

   public List loadQuestSteps(String questId) throws SQLException {
      List<QuestStep> result = new ArrayList();
      Connection connection = this.connect();

      try {
         PreparedStatement query = connection.prepareStatement("SELECT tile_x, tile_y, text_key FROM quest_steps\nWHERE quest_id = ? ORDER BY step_index\n");

         try {
            query.setString(1, questId);
            ResultSet rows = query.executeQuery();

            try {
               while(rows.next()) {
                  result.add(new QuestStep(new Tile(rows.getInt(1), rows.getInt(2)), rows.getString(3)));
               }
            } catch (Throwable var11) {
               if (rows != null) {
                  try {
                     rows.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (rows != null) {
               rows.close();
            }
         } catch (Throwable var12) {
            if (query != null) {
               try {
                  query.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (query != null) {
            query.close();
         }
      } catch (Throwable var13) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }
         }

         throw var13;
      }

      if (connection != null) {
         connection.close();
      }

      return List.copyOf(result);
   }

   public SceneScript loadSceneScript(String questId) throws SQLException {
      Map<Integer, SceneStage> stages = new HashMap<>();
      Map<String, SceneTarget> targets = new HashMap<>();
      Map<String, SceneEvent> events = new HashMap<>();
      try (Connection connection = this.connect()) {
         try (PreparedStatement query = connection.prepareStatement("SELECT stage, objective_key, target_id, target_label_key FROM quest_scene_stages WHERE quest_id = ?")) {
            query.setString(1, questId);
            try (ResultSet rows = query.executeQuery()) {
               while (rows.next()) {
                  stages.put(rows.getInt(1), new SceneStage(rows.getInt(1), rows.getString(2), rows.getString(3), rows.getString(4)));
               }
            }
         }
         try (PreparedStatement query = connection.prepareStatement("SELECT target_id, tile_x, tile_y, width, height, reach_x, reach_y, reach_radius FROM quest_scene_targets WHERE quest_id = ?")) {
            query.setString(1, questId);
            try (ResultSet rows = query.executeQuery()) {
               while (rows.next()) {
                  targets.put(rows.getString(1), new SceneTarget(rows.getString(1), rows.getDouble(2), rows.getDouble(3), rows.getDouble(4), rows.getDouble(5), rows.getDouble(6), rows.getDouble(7), rows.getDouble(8)));
               }
            }
         }
         try (PreparedStatement query = connection.prepareStatement("SELECT stage, target_id, action, next_stage, completed, dialogue_key, too_far_key FROM quest_scene_events WHERE quest_id = ?")) {
            query.setString(1, questId);
            try (ResultSet rows = query.executeQuery()) {
               while (rows.next()) {
                  SceneEvent event = new SceneEvent(rows.getInt(1), rows.getString(2), rows.getString(3), rows.getInt(4), rows.getInt(5) != 0, rows.getString(6), rows.getString(7));
                  events.put(event.stage() + ":" + event.targetId(), event);
               }
            }
         }
      }
      if (stages.isEmpty() || targets.isEmpty() || events.isEmpty()) {
         throw new SQLException("Missing scene script for " + questId);
      }
      for (SceneStage stage : stages.values()) {
         if (!targets.containsKey(stage.targetId()) || !events.containsKey(stage.stage() + ":" + stage.targetId())) {
            throw new SQLException("Incomplete scene stage " + questId + ":" + stage.stage());
         }
      }
      for (SceneEvent event : events.values()) {
         if (!List.of("LINE", "ADVANCE", "EXIT").contains(event.action()) || !stages.containsKey(event.nextStage())) {
            throw new SQLException("Invalid scene event " + questId + ":" + event.stage() + ":" + event.targetId());
         }
      }
      return new SceneScript(Map.copyOf(stages), Map.copyOf(targets), Map.copyOf(events));
   }

   public List<NpcQuestOffer> loadNpcQuestOffers(String npcId) throws SQLException {
      List<NpcQuestOffer> offers = new ArrayList<>();
      try (Connection connection = this.connect();
           PreparedStatement query = connection.prepareStatement("SELECT quest_id, offer_prefix, progress_prefix, turnin_prefix, done_prefix, finish_line_key FROM npc_quest_offers WHERE npc_id = ? ORDER BY priority")) {
         query.setString(1, npcId);
         try (ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
               offers.add(new NpcQuestOffer(rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4), rows.getString(5), rows.getString(6)));
            }
         }
      }
      if (offers.isEmpty()) {
         throw new SQLException("No quest offers for NPC " + npcId);
      }
      return List.copyOf(offers);
   }

   private List<String[]> readDataRows(String resource, int columns) throws IOException {
      InputStream stream = LocalGameStore.class.getResourceAsStream("/alkosmen/data/" + resource);
      if (stream == null) {
         throw new IOException("Missing " + resource);
      }
      List<String[]> result = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
         String line;
         while ((line = reader.readLine()) != null) {
            if (!line.isBlank() && !line.startsWith("#")) {
               String[] fields = line.split("\\|", -1);
               if (fields.length != columns) {
                  throw new IOException("Bad " + resource + " row: " + line);
               }
               result.add(fields);
            }
         }
      }
      return result;
   }

   private void seedSceneScripts() throws IOException, SQLException {
      try (Connection connection = this.connect()) {
         try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO quest_scene_stages VALUES (?, ?, ?, ?, ?)")) {
            for (String[] row : this.readDataRows("quest_scene_stages.tsv", 5)) {
               insert.setString(1, row[0]);
               insert.setInt(2, Integer.parseInt(row[1]));
               insert.setString(3, row[2]);
               insert.setString(4, row[3]);
               insert.setString(5, row[4]);
               insert.addBatch();
            }
            insert.executeBatch();
         }
         try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO quest_scene_targets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (String[] row : this.readDataRows("quest_scene_targets.tsv", 9)) {
               insert.setString(1, row[0]);
               insert.setString(2, row[1]);
               for (int i = 2; i < row.length; ++i) {
                  insert.setDouble(i + 1, Double.parseDouble(row[i]));
               }
               insert.addBatch();
            }
            insert.executeBatch();
         }
         try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO quest_scene_events VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (String[] row : this.readDataRows("quest_scene_events.tsv", 8)) {
               insert.setString(1, row[0]);
               insert.setInt(2, Integer.parseInt(row[1]));
               insert.setString(3, row[2]);
               insert.setString(4, row[3]);
               insert.setInt(5, Integer.parseInt(row[4]));
               insert.setInt(6, Integer.parseInt(row[5]));
               insert.setString(7, row[6]);
               insert.setString(8, row[7]);
               insert.addBatch();
            }
            insert.executeBatch();
         }
      }
   }

   private void seedNpcRoutes() throws IOException, SQLException {
      Map<String, List<String[]>> routes = new LinkedHashMap<>();
      for (String[] row : this.readDataRows("npc_routes.tsv", 5)) {
         routes.computeIfAbsent(row[0], ignored -> new ArrayList<>()).add(row);
      }
      try (Connection connection = this.connect();
           PreparedStatement count = connection.prepareStatement("SELECT COUNT(*) FROM npc_routes WHERE npc_id = ?");
           PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO npc_routes (npc_id, step_index, tile_x, tile_y, pause_ms) VALUES (?, ?, ?, ?, ?)")) {
         for (Map.Entry<String, List<String[]>> route : routes.entrySet()) {
            count.setString(1, route.getKey());
            try (ResultSet existing = count.executeQuery()) {
               if (existing.next() && existing.getInt(1) > 0) {
                  continue;
               }
            }
            for (String[] row : route.getValue()) {
               insert.setString(1, row[0]);
               for (int i = 1; i < row.length; ++i) {
                  insert.setInt(i + 1, Integer.parseInt(row[i]));
               }
               insert.addBatch();
            }
         }
         insert.executeBatch();
      }
   }

   private void seedNpcQuestOffers() throws IOException, SQLException {
      try (Connection connection = this.connect();
           PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO npc_quest_offers VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
         for (String[] row : this.readDataRows("npc_quest_offers.tsv", 8)) {
            insert.setString(1, row[0]);
            insert.setInt(2, Integer.parseInt(row[1]));
            for (int i = 2; i < row.length; ++i) {
               insert.setString(i + 1, row[i]);
            }
            insert.addBatch();
         }
         insert.executeBatch();
      }
   }

   private void seedQuestSteps() throws IOException, SQLException {
      InputStream stream = LocalGameStore.class.getResourceAsStream("/alkosmen/data/quest_steps.tsv");

      try {
         if (stream == null) {
            throw new IOException("Missing quest_steps.tsv");
         }

         BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

         try {
            Connection connection = this.connect();

            try {
               PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO quest_steps\n(quest_id, step_index, tile_x, tile_y, text_key)\nVALUES (?, ?, ?, ?, ?)\n");

               try {
                  String line;
                  while((line = reader.readLine()) != null) {
                     if (!line.isBlank() && !line.startsWith("#")) {
                        String[] fields = line.split("\\|", -1);
                        if (fields.length != 5) {
                           throw new IOException("Bad quest step: " + line);
                        }

                        insert.setString(1, fields[0]);
                        insert.setInt(2, Integer.parseInt(fields[1]));
                        insert.setInt(3, Integer.parseInt(fields[2]));
                        insert.setInt(4, Integer.parseInt(fields[3]));
                        insert.setString(5, fields[4]);
                        insert.addBatch();
                     }
                  }

                  insert.executeBatch();
               } catch (Throwable var11) {
                  if (insert != null) {
                     try {
                        insert.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (insert != null) {
                  insert.close();
               }
            } catch (Throwable var12) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (connection != null) {
               connection.close();
            }
         } catch (Throwable var13) {
            try {
               reader.close();
            } catch (Throwable var8) {
               var13.addSuppressed(var8);
            }

            throw var13;
         }

         reader.close();
      } catch (Throwable var14) {
         if (stream != null) {
            try {
               stream.close();
            } catch (Throwable var7) {
               var14.addSuppressed(var7);
            }
         }

         throw var14;
      }

      if (stream != null) {
         stream.close();
      }

   }

   private void migrateFirstSacredCache() throws SQLException {
      Connection connection = this.connect();

      label156: {
         try {
            connection.setAutoCommit(false);

            try {
               label157: {
                  PreparedStatement check = connection.prepareStatement("SELECT 1 FROM schema_migrations WHERE migration_id = ?");

                  label158: {
                     try {
                        PreparedStatement update;
                        label159: {
                           update = connection.prepareStatement("UPDATE quest_steps SET tile_x = 11\nWHERE quest_id = ? AND step_index = 0 AND tile_x = 12 AND tile_y = 4\n");

                           try {
                              label160: {
                                 PreparedStatement mark = connection.prepareStatement("INSERT INTO schema_migrations (migration_id) VALUES (?)");

                                 label161: {
                                    try {
                                       check.setString(1, "sacred_cache_0_free_tile");
                                       ResultSet row = check.executeQuery();

                                       label163: {
                                          try {
                                             if (row.next()) {
                                                break label163;
                                             }
                                          } catch (Throwable var13) {
                                             if (row != null) {
                                                try {
                                                   row.close();
                                                } catch (Throwable var12) {
                                                   var13.addSuppressed(var12);
                                                }
                                             }

                                             throw var13;
                                          }

                                          if (row != null) {
                                             row.close();
                                          }

                                          update.setString(1, "sacred_tich");
                                          update.executeUpdate();
                                          mark.setString(1, "sacred_cache_0_free_tile");
                                          mark.executeUpdate();
                                          connection.commit();
                                          break label161;
                                       }

                                       if (row != null) {
                                          row.close();
                                       }
                                    } catch (Throwable var14) {
                                       if (mark != null) {
                                          try {
                                             mark.close();
                                          } catch (Throwable var11) {
                                             var14.addSuppressed(var11);
                                          }
                                       }

                                       throw var14;
                                    }

                                    if (mark != null) {
                                       mark.close();
                                    }
                                    break label160;
                                 }

                                 if (mark != null) {
                                    mark.close();
                                 }
                                 break label159;
                              }
                           } catch (Throwable var15) {
                              if (update != null) {
                                 try {
                                    update.close();
                                 } catch (Throwable var10) {
                                    var15.addSuppressed(var10);
                                 }
                              }

                              throw var15;
                           }

                           if (update != null) {
                              update.close();
                           }
                           break label158;
                        }

                        if (update != null) {
                           update.close();
                        }
                     } catch (Throwable var16) {
                        if (check != null) {
                           try {
                              check.close();
                           } catch (Throwable var9) {
                              var16.addSuppressed(var9);
                           }
                        }

                        throw var16;
                     }

                     if (check != null) {
                        check.close();
                     }
                     break label157;
                  }

                  if (check != null) {
                     check.close();
                  }
                  break label156;
               }
            } catch (SQLException error) {
               connection.rollback();
               throw error;
            }
         } catch (Throwable var18) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var8) {
                  var18.addSuppressed(var8);
               }
            }

            throw var18;
         }

         if (connection != null) {
            connection.close();
         }

         return;
      }

      if (connection != null) {
         connection.close();
      }

   }

   private void migrateSacredCacheExpansion() throws SQLException {
      Connection connection = this.connect();

      label156: {
         try {
            connection.setAutoCommit(false);

            try {
               label157: {
                  PreparedStatement check = connection.prepareStatement("SELECT 1 FROM schema_migrations WHERE migration_id = ?");

                  label158: {
                     try {
                        PreparedStatement update;
                        label159: {
                           update = connection.prepareStatement("UPDATE quest_steps SET tile_x = 7, tile_y = 14, text_key = 'sacred.cache.2.v2'\nWHERE quest_id = ? AND step_index = 2\n  AND tile_x = 17 AND tile_y = 15 AND text_key = 'sacred.cache.2'\n");

                           try {
                              label160: {
                                 PreparedStatement mark = connection.prepareStatement("INSERT INTO schema_migrations (migration_id) VALUES (?)");

                                 label161: {
                                    try {
                                       check.setString(1, "sacred_five_caches");
                                       ResultSet row = check.executeQuery();

                                       label163: {
                                          try {
                                             if (row.next()) {
                                                break label163;
                                             }
                                          } catch (Throwable var13) {
                                             if (row != null) {
                                                try {
                                                   row.close();
                                                } catch (Throwable var12) {
                                                   var13.addSuppressed(var12);
                                                }
                                             }

                                             throw var13;
                                          }

                                          if (row != null) {
                                             row.close();
                                          }

                                          update.setString(1, "sacred_tich");
                                          update.executeUpdate();
                                          mark.setString(1, "sacred_five_caches");
                                          mark.executeUpdate();
                                          connection.commit();
                                          break label161;
                                       }

                                       if (row != null) {
                                          row.close();
                                       }
                                    } catch (Throwable var14) {
                                       if (mark != null) {
                                          try {
                                             mark.close();
                                          } catch (Throwable var11) {
                                             var14.addSuppressed(var11);
                                          }
                                       }

                                       throw var14;
                                    }

                                    if (mark != null) {
                                       mark.close();
                                    }
                                    break label160;
                                 }

                                 if (mark != null) {
                                    mark.close();
                                 }
                                 break label159;
                              }
                           } catch (Throwable var15) {
                              if (update != null) {
                                 try {
                                    update.close();
                                 } catch (Throwable var10) {
                                    var15.addSuppressed(var10);
                                 }
                              }

                              throw var15;
                           }

                           if (update != null) {
                              update.close();
                           }
                           break label158;
                        }

                        if (update != null) {
                           update.close();
                        }
                     } catch (Throwable var16) {
                        if (check != null) {
                           try {
                              check.close();
                           } catch (Throwable var9) {
                              var16.addSuppressed(var9);
                           }
                        }

                        throw var16;
                     }

                     if (check != null) {
                        check.close();
                     }
                     break label157;
                  }

                  if (check != null) {
                     check.close();
                  }
                  break label156;
               }
            } catch (SQLException error) {
               connection.rollback();
               throw error;
            }
         } catch (Throwable var18) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var8) {
                  var18.addSuppressed(var8);
               }
            }

            throw var18;
         }

         if (connection != null) {
            connection.close();
         }

         return;
      }

      if (connection != null) {
         connection.close();
      }

   }

   private void seedDialogues() throws IOException, SQLException {
      Connection connection = this.connect();

      try {
         PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO dialogue_texts (text_key, body) VALUES (?, ?)");

         try {
            InputStream stream = LocalGameStore.class.getResourceAsStream("/alkosmen/data/quest_dialogues.tsv");

            try {
               if (stream == null) {
                  throw new IOException("Missing quest_dialogues.tsv");
               }

               BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));

               String line;
               try {
                  while((line = reader.readLine()) != null) {
                     if (!line.isBlank() && !line.startsWith("#")) {
                        String[] pair = line.split("\\|", 2);
                        if (pair.length != 2) {
                           throw new IOException("Bad quest dialogue: " + line);
                        }

                        insert.setString(1, pair[0]);
                        insert.setString(2, pair[1].replace("\\n", "\n"));
                        insert.addBatch();
                     }
                  }
               } catch (Throwable var13) {
                  try {
                     reader.close();
                  } catch (Throwable var11) {
                     var13.addSuppressed(var11);
                  }

                  throw var13;
               }

               reader.close();
            } catch (Throwable var14) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var10) {
                     var14.addSuppressed(var10);
                  }
               }

               throw var14;
            }

            if (stream != null) {
               stream.close();
            }

            Properties legacy = new Properties();
            InputStream legacyStream = LocalGameStore.class.getResourceAsStream("/alkosmen/lore/dialogues.properties");

            try {
               if (legacyStream != null) {
                  legacy.load(new InputStreamReader(legacyStream, StandardCharsets.UTF_8));
               }
            } catch (Throwable var12) {
               if (legacyStream != null) {
                  try {
                     legacyStream.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (legacyStream != null) {
               legacyStream.close();
            }

            for(String key : legacy.stringPropertyNames()) {
               insert.setString(1, key);
               insert.setString(2, legacy.getProperty(key));
               insert.addBatch();
            }

            insert.executeBatch();
         } catch (Throwable var15) {
            if (insert != null) {
               try {
                  insert.close();
               } catch (Throwable var8) {
                  var15.addSuppressed(var8);
               }
            }

            throw var15;
         }

         if (insert != null) {
            insert.close();
         }
      } catch (Throwable var16) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var7) {
               var16.addSuppressed(var7);
            }
         }

         throw var16;
      }

      if (connection != null) {
         connection.close();
      }

   }

   public boolean collectTolyaBottle(int x, int y) throws SQLException {
      Connection connection = this.connect();

      boolean var5;
      try {
         PreparedStatement command = connection.prepareStatement("INSERT OR IGNORE INTO quest_collections (quest_id, tile_x, tile_y)\nVALUES (?, ?, ?)\n");

         try {
            command.setString(1, "tolya_bottles");
            command.setInt(2, x);
            command.setInt(3, y);
            var5 = command.executeUpdate() == 1;
         } catch (Throwable var9) {
            if (command != null) {
               try {
                  command.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (command != null) {
            command.close();
         }
      } catch (Throwable var10) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (connection != null) {
         connection.close();
      }

      return var5;
   }

   private Connection connect() throws SQLException {
      return DriverManager.getConnection("jdbc:sqlite:" + String.valueOf(this.databasePath));
   }

   public static record Tile(int x, int y) {
   }

   public static record Waypoint(int x, int y, int pauseMs) {
   }

   public static record QuestState(boolean accepted, boolean completed, Set<Tile> collected) {
   }

   public static record StoryState(boolean accepted, boolean completed, int stage) {
   }

   public static record QuestStep(Tile tile, String textKey) {
   }

   public static record SceneStage(int stage, String objectiveKey, String targetId, String targetLabelKey) {
   }

   public static record SceneTarget(String id, double x, double y, double width, double height, double reachX, double reachY, double reachRadius) {
   }

   public static record SceneEvent(int stage, String targetId, String action, int nextStage, boolean completed, String dialogueKey, String tooFarKey) {
   }

   public static record SceneScript(Map<Integer, SceneStage> stages, Map<String, SceneTarget> targets, Map<String, SceneEvent> events) {
      public SceneStage stage(int stage) {
         return this.stages.get(stage);
      }

      public SceneTarget target(String id) {
         return this.targets.get(id);
      }

      public SceneEvent event(int stage, String targetId) {
         return this.events.get(stage + ":" + targetId);
      }
   }

   public static record NpcQuestOffer(String questId, String offerPrefix, String progressPrefix, String turninPrefix, String donePrefix, String finishLineKey) {
   }
}
