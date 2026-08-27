package com.riftforged.aicompanion.kb;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ports loadKbChunks/selectKbChunks/scoreKbChunk/buildReference from watcher.js, generalized from
 * a single hardcoded KB file to a whole directory: admins drop any number of ".md" files into the
 * plugin's "kb/" folder (own headings/sections per server, own content), and every one of them is
 * read, split into one chunk per heading, and searched the same way. The KB markdown can get
 * large, so it's never pasted whole into a prompt — only the highest-scoring chunks per question
 * (by heading/bold-name match and word overlap) are pulled in, up to a character budget.
 *
 * Re-reads every file from disk on every call (like the original), so KB edits take effect live
 * without a plugin reload — a handful of markdown files costs nothing meaningful to re-parse.
 */
public final class KnowledgeBase {
    private static final int KB_CHAR_BUDGET = 6000;
    private static final Pattern HEADING_RE = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern WORD_RE = Pattern.compile("[a-z0-9']+");
    private static final Pattern BOLD_RE = Pattern.compile("\\*\\*([^*]+)\\*\\*");
    private static final Pattern PAREN_RE = Pattern.compile("\\([^)]*\\)");

    private static final Set<String> STOPWORDS = Set.of(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "to", "of", "in",
        "on", "for", "and", "or", "with", "what", "how", "do", "does", "i", "my",
        "me", "you", "your", "it", "this", "that", "can", "get", "give", "about",
        "which", "who", "when", "where", "why", "there", "have", "has", "had",
        "will", "would", "should", "could", "if", "not", "no", "just", "some",
        "any", "all", "from", "as", "at", "by", "up", "out"
    );

    private final Path kbDir;
    private final Path serverInfoFile;

    public KnowledgeBase(Path kbDir, Path serverInfoFile) {
        this.kbDir = kbDir;
        this.serverInfoFile = serverInfoFile;
    }

    private static String readOrEmpty(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }

    /** Every ".md" file directly inside kb/, sorted by filename so ordering (and the overview
     *  pick below) is stable and predictable for admins organizing files as "01-intro.md" etc. */
    private List<Path> listKbFiles() {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(kbDir)) return files;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(kbDir, "*.md")) {
            for (Path p : stream) files.add(p);
        } catch (IOException e) {
            return files;
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    public List<KbChunk> loadChunks() {
        List<KbChunk> chunks = new ArrayList<>();
        for (Path file : listKbFiles()) {
            chunks.addAll(parseChunks(readOrEmpty(file)));
        }
        return chunks;
    }

    private static List<KbChunk> parseChunks(String text) {
        List<KbChunk> chunks = new ArrayList<>();
        if (text.isEmpty()) return chunks;

        ArrayDeque<String> titleStack = new ArrayDeque<>();
        ArrayDeque<Integer> levelStack = new ArrayDeque<>();
        List<String> currentPath = null;
        StringBuilder currentBody = new StringBuilder();

        for (String line : text.split("\r?\n", -1)) {
            Matcher m = HEADING_RE.matcher(line);
            if (m.matches()) {
                if (currentPath != null && !currentBody.toString().isBlank()) {
                    chunks.add(new KbChunk(List.copyOf(currentPath),
                        String.join(" > ", currentPath) + "\n" + currentBody.toString().trim()));
                }
                int level = m.group(1).length();
                while (!levelStack.isEmpty() && levelStack.peekLast() >= level) {
                    levelStack.pollLast();
                    titleStack.pollLast();
                }
                levelStack.addLast(level);
                titleStack.addLast(m.group(2).trim());
                currentPath = new ArrayList<>(titleStack);
                currentBody = new StringBuilder();
            } else if (currentPath != null) {
                currentBody.append(line).append("\n");
            }
        }
        if (currentPath != null && !currentBody.toString().isBlank()) {
            chunks.add(new KbChunk(List.copyOf(currentPath),
                String.join(" > ", currentPath) + "\n" + currentBody.toString().trim()));
        }
        return chunks;
    }

    private static List<String> tokenize(String str) {
        List<String> out = new ArrayList<>();
        Matcher m = WORD_RE.matcher(str.toLowerCase());
        while (m.find()) {
            String w = m.group();
            if (w.length() >= 3 && !STOPWORDS.contains(w)) out.add(w);
        }
        return out;
    }

    private static int scoreChunk(String questionLower, List<String> questionTokens, KbChunk chunk) {
        int score = 0;
        Set<String> pathTokens = Set.copyOf(tokenize(String.join(" ", chunk.path())));
        Set<String> bodyTokens = Set.copyOf(tokenize(chunk.text()));

        for (String title : chunk.path()) {
            String cleaned = PAREN_RE.matcher(title).replaceAll("").replace("`", "").trim();
            for (String cand : cleaned.split("\\s*—\\s*")) {
                String t = cand.toLowerCase().trim();
                if (t.length() >= 4 && questionLower.contains(t)) {
                    score += 8;
                    break;
                }
            }
        }
        Matcher bm = BOLD_RE.matcher(chunk.text());
        while (bm.find()) {
            String t = bm.group(1).toLowerCase().trim();
            if (t.length() >= 4 && questionLower.contains(t)) score += 8;
        }
        for (String tok : questionTokens) {
            if (pathTokens.contains(tok)) score += 3;
            else if (bodyTokens.contains(tok)) score += 1;
        }
        return score;
    }

    private record ScoredChunk(KbChunk chunk, int score) {}

    private static String selectChunks(String question, List<KbChunk> chunks) {
        String questionLower = question.toLowerCase();
        List<String> questionTokens = tokenize(question);

        List<ScoredChunk> scored = new ArrayList<>();
        for (KbChunk c : chunks) {
            int score = scoreChunk(questionLower, questionTokens, c);
            if (score >= 3) scored.add(new ScoredChunk(c, score));
        }
        scored.sort((a, b) -> b.score() - a.score());

        int used = 0;
        List<String> picked = new ArrayList<>();
        for (ScoredChunk sc : scored) {
            if (used >= KB_CHAR_BUDGET) break;
            picked.add(sc.chunk().text());
            used += sc.chunk().text().length();
        }
        return String.join("\n\n---\n\n", picked);
    }

    /**
     * Always-relevant framing: whichever top-level (no parent heading) chunk appears first across
     * the kb/ files, in filename order — the natural place for an admin to put a "what is this
     * server" overview (e.g. as the top heading of "01-overview.md"). Not tied to any particular
     * numbering or heading text, so it works regardless of how a given server's KB is organized.
     */
    private static String loadOverview(List<KbChunk> chunks) {
        for (KbChunk c : chunks) {
            if (c.path().size() == 1) return c.text();
        }
        return "";
    }

    /** Union of KB chunks relevant to any of the given questions, plus server info + KB overview. */
    public String buildReference(List<String> questions) {
        List<KbChunk> chunks = loadChunks();
        Set<String> chunkTexts = new LinkedHashSet<>();
        for (String question : questions) {
            String selected = selectChunks(question, chunks);
            if (!selected.isEmpty()) chunkTexts.add(selected);
        }
        List<String> parts = new ArrayList<>();
        String serverInfo = readOrEmpty(serverInfoFile);
        if (!serverInfo.isBlank()) parts.add(serverInfo);
        String overview = loadOverview(chunks);
        if (!overview.isBlank()) parts.add(overview);
        if (!chunkTexts.isEmpty()) parts.add(String.join("\n\n---\n\n", chunkTexts));
        return String.join("\n\n---\n\n", parts);
    }
}
