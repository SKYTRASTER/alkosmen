package alkosmen.lore;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class LoreRepository {
    private final Properties characters;
    private final Properties dialogues;
    private final Properties scenes;
    private final Map<String, List<String>> dialogueCache = new ConcurrentHashMap<>();

    private LoreRepository(Properties characters, Properties dialogues, Properties scenes) {
        this.characters = characters;
        this.dialogues = dialogues;
        this.scenes = scenes;
    }

    public static LoreRepository loadDefault() {
        return new LoreRepository(
                loadProperties("/alkosmen/lore/characters.properties"),
                loadProperties("/alkosmen/lore/dialogues.properties"),
                loadProperties("/alkosmen/lore/scenes.properties")
        );
    }

    public List<String> characterIds() {
        String csv = characters.getProperty("characters.list", "");
        if (csv.isBlank()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String item : csv.split(",")) {
            String id = item.trim();
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return Collections.unmodifiableList(ids);
    }

    public LoreCharacter character(String id) {
        return new LoreCharacter(
                id,
                characters.getProperty("character." + id + ".name", id),
                characters.getProperty("character." + id + ".role", ""),
                characters.getProperty("character." + id + ".description", "")
        );
    }

    public String sceneText(String key, String fallback) {
        return scenes.getProperty(key, fallback);
    }

    public String randomDialogue(String speakerId, String fallback) {
        List<String> lines = dialogueCache.computeIfAbsent(speakerId, this::loadDialogueLines);
        if (lines.isEmpty()) {
            return fallback;
        }
        int idx = ThreadLocalRandom.current().nextInt(lines.size());
        return lines.get(idx);
    }

    public String randomDialogueFromSpeakers(String csvSpeakerIds, String fallback) {
        List<String> speakers = parseCsv(csvSpeakerIds);
        if (speakers.isEmpty()) {
            return fallback;
        }
        int idx = ThreadLocalRandom.current().nextInt(speakers.size());
        return randomDialogue(speakers.get(idx), fallback);
    }

    private List<String> loadDialogueLines(String speakerId) {
        String prefix = "dialogue." + speakerId + ".";
        List<String> keys = new ArrayList<>();
        for (String key : dialogues.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        Collections.sort(keys);

        List<String> lines = new ArrayList<>(keys.size());
        for (String key : keys) {
            String value = dialogues.getProperty(key, "").trim();
            if (!value.isEmpty()) {
                lines.add(value);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    private static List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : csv.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return values;
    }

    private static Properties loadProperties(String resourcePath) {
        try (var in = LoreRepository.class.getResourceAsStream(resourcePath)) {
            Properties properties = new Properties();
            if (in == null) {
                return properties;
            }
            try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load lore resource: " + resourcePath, e);
        }
    }
}