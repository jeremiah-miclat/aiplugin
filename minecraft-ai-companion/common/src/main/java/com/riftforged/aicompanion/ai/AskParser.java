package com.riftforged.aicompanion.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ports parseAskSubparts/parseAskBatchResponse/sanitizeGive from watcher.js. A model's raw text
 * response is tolerant-parsed: stray text/code fences around the JSON array are stripped by
 * regex, and a response that doesn't parse or doesn't match the expected shape is treated as a
 * failure (null) so the caller falls back to the next model, same as a network error.
 */
public final class AskParser {
    private static final Pattern ARRAY_RE = Pattern.compile("\\[[\\s\\S]*]");
    private static final Pattern ITEM_ID_RE = Pattern.compile("^minecraft:[a-z0-9_]+$");

    private AskParser() {}

    /** Returns up to maxSubparts sanitized subparts, or null if the response was unusable. */
    public static List<AskSubpart> parseSubparts(String text, int maxSubparts) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = ARRAY_RE.matcher(text);
        if (!m.find()) return null;
        JsonArray arr;
        try {
            JsonElement el = JsonParser.parseString(m.group());
            if (!el.isJsonArray()) return null;
            arr = el.getAsJsonArray();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
        if (arr.isEmpty()) return null;

        List<AskSubpart> out = new ArrayList<>();
        for (int i = 0; i < Math.min(arr.size(), maxSubparts); i++) {
            AskSubpart subpart = toSubpart(arr.get(i));
            if (subpart.reply() == null || subpart.reply().isEmpty()) return null;
            out.add(subpart);
        }
        return out;
    }

    /** One array element per numbered message, each itself an array of subparts. Null on mismatch. */
    public static List<List<AskSubpart>> parseBatchResponse(String text, int expectedCount, int maxSubparts) {
        if (text == null || text.isEmpty()) return null;
        Matcher m = ARRAY_RE.matcher(text);
        if (!m.find()) return null;
        JsonArray arr;
        try {
            JsonElement el = JsonParser.parseString(m.group());
            if (!el.isJsonArray()) return null;
            arr = el.getAsJsonArray();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
        if (arr.size() != expectedCount) return null;

        List<List<AskSubpart>> result = new ArrayList<>();
        for (JsonElement item : arr) {
            if (!item.isJsonArray() || item.getAsJsonArray().isEmpty()) return null;
            JsonArray itemArr = item.getAsJsonArray();
            List<AskSubpart> subparts = new ArrayList<>();
            for (int i = 0; i < Math.min(itemArr.size(), maxSubparts); i++) {
                AskSubpart subpart = toSubpart(itemArr.get(i));
                if (subpart.reply() == null || subpart.reply().isEmpty()) return null;
                subparts.add(subpart);
            }
            result.add(subparts);
        }
        return result;
    }

    private static AskSubpart toSubpart(JsonElement el) {
        if (!el.isJsonObject()) return new AskSubpart(null, null);
        JsonObject obj = el.getAsJsonObject();
        String reply = obj.has("reply") && obj.get("reply").isJsonPrimitive()
            ? obj.get("reply").getAsString()
            : null;
        ItemGive give = null;
        if (obj.has("give") && obj.get("give").isJsonObject()) {
            JsonObject giveObj = obj.getAsJsonObject("give");
            String item = giveObj.has("item") && giveObj.get("item").isJsonPrimitive()
                ? giveObj.get("item").getAsString()
                : null;
            if (item != null && ITEM_ID_RE.matcher(item).matches()) {
                int quantity = giveObj.has("quantity") && giveObj.get("quantity").isJsonPrimitive()
                    ? giveObj.get("quantity").getAsInt()
                    : 1;
                give = new ItemGive(item, quantity);
            }
        }
        return new AskSubpart(reply, give);
    }
}
