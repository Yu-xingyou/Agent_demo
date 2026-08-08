package com.habit.agent.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 结构化输出容错解析工具
 * <p>
 * LLM 返回的分析文本虽然被要求输出合法 JSON，但实际输出常出现以下畸形：
 * <ul>
 *   <li>夹带 markdown 代码块围栏（```json ... ```）</li>
 *   <li>JSON 前后夹带说明性文字或 &lt;think&gt; 标签</li>
 *   <li>数组中多个元素被写成"无键名的平级裸字符串"（本次故障的根因）</li>
 *   <li>整体为非合法 JSON</li>
 * </ul>
 * 本工具采用四级递进容错，命中即返回，最大限度抢救出可用文本。
 */
public final class LlmJsonParser {

    /** 解析降级等级：直接成功 / 截取修复 / 结构修复 / 正则抢救 / 全失败 */
    public enum ParseLevel { L1_DIRECT, L2_TRIMMED, L3_REPAIRED, L4_REGEX, FAILED }

    /**
     * 解析结果：携带解析出的 JSON 节点与命中的降级等级
     *
     * @param node  解析得到的 JSON 节点（全失败时为空对象节点）
     * @param level 命中的解析等级
     */
    public record ParseResult(JsonNode node, ParseLevel level) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 字段名白名单，用于 L4 正则抽取与 L3 修复时的"下一字段"锚定 */
    private static final List<String> FIELD_KEYS = List.of(
            "score", "dailyEvaluation", "trendSummary", "riskWarning", "suggestion", "report");

    /** 原始输出打印截断长度，避免日志被整段报告刷屏 */
    public static final int LOG_TRUNCATE = 500;

    private LlmJsonParser() {}

    /**
     * 解析入口：四级递进容错，命中即返回
     *
     * @param raw LLM 原始输出文本（可为 null 或空）
     * @return 解析结果（含 JSON 节点与等级）；输入为空时返回空节点 + FAILED
     */
    public static ParseResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParseResult(MAPPER.createObjectNode(), ParseLevel.FAILED);
        }
        String cleaned = stripFences(raw.trim());

        // L1：直接解析
        JsonNode node = tryRead(cleaned);
        if (node != null) {
            return new ParseResult(node, ParseLevel.L1_DIRECT);
        }

        // L2：截取首个 { 到最后一个 }
        int first = cleaned.indexOf('{');
        int last = cleaned.lastIndexOf('}');
        if (first >= 0 && last > first) {
            String sub = cleaned.substring(first, last + 1);
            node = tryRead(sub);
            if (node != null) {
                return new ParseResult(node, ParseLevel.L2_TRIMMED);
            }
        }

        // L3：将无键名平级裸字符串合并进前一 key 的数组中再解析
        String repaired = repairOrphanStrings(cleaned);
        node = tryRead(repaired);
        if (node != null) {
            return new ParseResult(node, ParseLevel.L3_REPAIRED);
        }
        if (first >= 0 && last > first) {
            String subRepaired = repairOrphanStrings(cleaned.substring(first, last + 1));
            node = tryRead(subRepaired);
            if (node != null) {
                return new ParseResult(node, ParseLevel.L3_REPAIRED);
            }
        }

        // L4：逐字段正则抢救
        node = extractFieldsByRegex(cleaned);
        if (node != null && node.size() > 0) {
            return new ParseResult(node, ParseLevel.L4_REGEX);
        }

        return new ParseResult(MAPPER.createObjectNode(), ParseLevel.FAILED);
    }

    /**
     * 将 suggestion 归一为换行分隔字符串，兼容三种形态：
     * <ul>
     *   <li>JSON 字符串数组节点（["a","b"]）→ 逐元素 trim 后 join("\n")</li>
     *   <li>含换行的字符串 → 原样返回</li>
     *   <li>单行未分隔长串 → 按 "1. 2. 3." / "；" / ";" 智能拆分</li>
     * </ul>
     *
     * @param suggestionNode suggestion 字段节点（可为 null）
     * @return 换行分隔的建议字符串（空时返回空串）
     */
    public static String normalizeSuggestion(JsonNode suggestionNode) {
        if (suggestionNode == null || suggestionNode.isMissingNode() || suggestionNode.isNull()) {
            return "";
        }
        // 数组形态
        if (suggestionNode.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : suggestionNode) {
                String s = item.asText("").trim();
                if (!s.isEmpty()) {
                    items.add(s);
                }
            }
            return String.join("\n", items);
        }
        // 字符串形态
        String text = suggestionNode.asText("").trim();
        if (text.isEmpty()) {
            return "";
        }
        // 已含换行（可能夹杂 \r），直接按行规整
        if (text.contains("\n") || text.contains("\r")) {
            List<String> lines = new ArrayList<>();
            for (String line : text.split("\\r?\\n")) {
                String s = line.trim();
                if (!s.isEmpty()) {
                    lines.add(s);
                }
            }
            return String.join("\n", lines);
        }
        // 单行串：尝试按常见分隔符拆分
        return smartSplitSingleLine(text);
    }

    /**
     * 单行建议按常见分隔符智能拆分
     */
    private static String smartSplitSingleLine(String text) {
        // 优先按 "1. 2. 3." 等有序前缀拆分
        if (ORDERED_PREFIX.matcher(text).find()) {
            String[] parts = text.split("(?<=^|\\n)(?=\\s*\\d+[.、)])");
            List<String> items = new ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (!s.isEmpty()) {
                    items.add(s);
                }
            }
            if (items.size() > 1) {
                return String.join("\n", items);
            }
        }
        // 退而按分号拆分
        String replaced = text.replace('；', ';');
        if (replaced.contains(";")) {
            List<String> items = new ArrayList<>();
            for (String p : replaced.split(";")) {
                String s = p.trim();
                if (!s.isEmpty()) {
                    items.add(s);
                }
            }
            if (items.size() > 1) {
                return String.join("\n", items);
            }
        }
        return text;
    }

    /**
     * 剥离 markdown 代码块围栏（```json ... ``` 或 ``` ... ```）
     */
    private static String stripFences(String text) {
        if (text.startsWith("```")) {
            int firstNl = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNl >= 0 && lastFence > firstNl) {
                return text.substring(firstNl + 1, lastFence).trim();
            }
        }
        return text;
    }

    /**
     * 将"无键名平级裸字符串"合并进前一 key 的数组。
     * 例：{"suggestion":"a", "b", "c", "report":"..."} → {"suggestion":["a","b","c"], "report":"..."}
     * <p>
     * 思路：先逐字符切分顶层 token（忽略嵌套对象/数组内部），得到形如
     * key / value / 裸字符串 的交替序列；再把紧跟在字符串值之后的连续裸字符串并入该 key，
     * 重组为 JSON 数组。
     */
    private static String repairOrphanStrings(String src) {
        List<String> tokens = splitTopLevel(src);
        if (tokens.isEmpty()) {
            return src;
        }
        StringBuilder out = new StringBuilder();
        out.append('{');
        boolean first = true;
        String pendingKey = null;       // 当前待赋值/已赋值的 key
        boolean pendingHasValue = false; // 该 key 是否已有一个字符串值
        List<String> pendingValues = null; // 合并后的多值（null 表示单值）

        for (int idx = 0; idx < tokens.size(); idx++) {
            String tok = tokens.get(idx);
            if (isKeyToken(tok)) {
                // 收尾上一个 key
                if (pendingKey != null) {
                    emit(out, first, pendingKey, pendingHasValue, pendingValues);
                    first = false;
                }
                pendingKey = unquote(tok);
                pendingHasValue = false;
                pendingValues = null;
            } else {
                // 值或裸字符串
                if (pendingKey == null) {
                    continue; // 异常前缀裸串，忽略
                }
                if (!pendingHasValue) {
                    pendingHasValue = true;
                    pendingValues = new ArrayList<>();
                    pendingValues.add(tok);
                } else {
                    pendingValues.add(tok);
                }
            }
        }
        if (pendingKey != null) {
            emit(out, first, pendingKey, pendingHasValue, pendingValues);
        }
        out.append('}');
        return out.toString();
    }

    /**
     * 把待输出的 key 及其值（单值或数组）写入 out
     */
    private static void emit(StringBuilder out, boolean first, String key,
                             boolean hasValue, List<String> values) {
        if (!first) {
            out.append(',');
        }
        out.append('"').append(key).append("\":");
        if (values != null && values.size() > 1) {
            out.append('[');
            for (int k = 0; k < values.size(); k++) {
                if (k > 0) {
                    out.append(',');
                }
                out.append(values.get(k));
            }
            out.append(']');
        } else if (values != null && values.size() == 1) {
            out.append(values.get(0));
        } else {
            out.append("\"\"");
        }
    }

    /**
     * 切分顶层 token：key（"xxx":）与 value（字符串/裸字符串/对象/数组），忽略嵌套内部逗号
     */
    private static List<String> splitTopLevel(String src) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = src.length();
        // 跳过开头的 {
        while (i < n && src.charAt(i) != '{' && Character.isWhitespace(src.charAt(i))) {
            i++;
        }
        if (i < n && src.charAt(i) == '{') {
            i++;
        }
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c) || c == ',' || c == ':') {
                i++;
                continue;
            }
            if (c == '}') {
                break;
            }
            if (c == '"') {
                int end = readString(src, i);
                if (end < 0) {
                    break;
                }
                String str = src.substring(i, end + 1);
                int j = skipWs(src, end + 1);
                boolean isKey = j < n && src.charAt(j) == ':';
                tokens.add(isKey ? str + ":" : str);
                i = end + 1;
            } else if (c == '[') {
                int end = matchBracket(src, i, '[', ']');
                tokens.add(src.substring(i, end + 1));
                i = end + 1;
            } else if (c == '{') {
                int end = matchBracket(src, i, '{', '}');
                tokens.add(src.substring(i, end + 1));
                i = end + 1;
            } else {
                // 数字/布尔/null 等裸值
                int end = i;
                while (end < n && src.charAt(end) != ',' && src.charAt(end) != '}') {
                    end++;
                }
                tokens.add(src.substring(i, end).trim());
                i = end;
            }
        }
        return tokens;
    }

    /**
     * 从 offset（指向开括号）匹配到配对的闭括号，返回闭括号下标
     */
    private static int matchBracket(String src, int offset, char open, char close) {
        int depth = 0;
        int i = offset;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '"') {
                int e = readString(src, i);
                if (e < 0) {
                    return n - 1;
                }
                i = e + 1;
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
            i++;
        }
        return n - 1;
    }

    private static boolean isKeyToken(String tok) {
        return tok.endsWith(":") && tok.startsWith("\"") && tok.length() >= 3;
    }

    private static String unquote(String tok) {
        String t = tok.substring(0, tok.length() - 1).trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    /**
     * 逐字段正则抢救：以"下一个已知字段名或对象结尾"为终止锚点抽取六个已知字段
     */
    private static JsonNode extractFieldsByRegex(String src) {
        ObjectNode root = MAPPER.createObjectNode();
        String text = src;
        // 先尝试从去围栏后的内容中定位 { 区间
        int s = text.indexOf('{');
        int e = text.lastIndexOf('}');
        String body = (s >= 0 && e > s) ? text.substring(s + 1, e) : text;

        for (String key : FIELD_KEYS) {
            Matcher m = fieldValuePattern(key).matcher(body);
            if (m.find()) {
                String rawVal = m.group("val").trim();
                if (key.equals("score")) {
                    try {
                        root.put("score", Integer.parseInt(rawVal.replaceAll("[^0-9-]", "")));
                    } catch (NumberFormatException ignore) {
                        // score 解析失败不放入，上层走默认
                    }
                } else if (rawVal.startsWith("[")) {
                    // 数组形态的建议，逐元素抽取
                    root.put("suggestion", normalizeSuggestion(parseLooseArray(rawVal)));
                } else {
                    // 去除首尾引号
                    String val = stripQuotes(rawVal);
                    root.put(key, val);
                }
            }
        }
        return root.size() > 0 ? root : null;
    }

    /**
     * 宽松解析一个 JSON 数组（用于 L4 抽取到的数组字符串）
     */
    private static JsonNode parseLooseArray(String arrStr) {
        try {
            return MAPPER.readTree(arrStr);
        } catch (Exception e) {
            // 退化为字符串节点
            return MAPPER.getNodeFactory().textNode(arrStr);
        }
    }

    private static String stripQuotes(String s) {
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return t;
    }

    private static JsonNode tryRead(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(s);
            return (node != null && node.isObject()) ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取一个从 offset 处起始（offset 指向开引号）的字符串字面量，返回闭引号下标
     * 正确处理转义字符 \" \\ \/ 等
     */
    private static int readString(String src, int offset) {
        int i = offset + 1;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '"') {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static int skipWs(String src, int offset) {
        int i = offset;
        int n = src.length();
        while (i < n && Character.isWhitespace(src.charAt(i))) {
            i++;
        }
        return i;
    }

    private static final Pattern ORDERED_PREFIX = Pattern.compile("\\d+[.、)]");

    private static final java.util.Map<String, Pattern> FIELD_PATTERN_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static Pattern fieldValuePattern(String key) {
        return FIELD_PATTERN_CACHE.computeIfAbsent(key, k ->
                Pattern.compile("\"" + Pattern.quote(k) + "\"\\s*:\\s*(?<val>(\\[[^\\]]*\\])|(\"[^\"]*(\\\\[\\s\\S][^\"]*)*\")|[^,}\\]]+)",
                        Pattern.DOTALL));
    }
}
