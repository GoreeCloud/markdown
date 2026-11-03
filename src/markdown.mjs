// Original GoreeCloud Markdown preview subset. No HTML execution or network dependencies.
export const MAX_SOURCE_LENGTH = 512 * 1024;
const TICK = String.fromCharCode(96);

export function safeHref(value) {
  if (typeof value !== "string" || value !== value.trim() || /[\u0000-\u001f\u007f\\]/.test(value)) return null;
  try {
    const url = new URL(value);
    if (!["https:", "http:", "mailto:"].includes(url.protocol)) return null;
    if (url.username || url.password) return null;
    if (url.protocol === "mailto:" && !url.pathname.includes("@")) return null;
    return url.href;
  } catch {
    return null;
  }
}

export function parseInline(text, depth = 0) {
  if (typeof text !== "string") throw new TypeError("Inline source must be text");
  if (depth >= 4) return [{ type: "text", text }];
  const result = [];
  let pending = "";
  const flush = () => {
    if (pending) result.push({ type: "text", text: pending });
    pending = "";
  };
  for (let i = 0; i < text.length;) {
    if (text[i] === TICK) {
      const end = text.indexOf(TICK, i + 1);
      if (end > i + 1) {
        flush();
        result.push({ type: "code", text: text.slice(i + 1, end) });
        i = end + 1;
        continue;
      }
    }
    if (text[i] === "[") {
      const midpoint = text.indexOf("](", i + 1);
      const end = midpoint >= 0 ? text.indexOf(")", midpoint + 2) : -1;
      if (midpoint > i + 1 && end > midpoint + 2) {
        const href = safeHref(text.slice(midpoint + 2, end));
        if (href) {
          flush();
          result.push({ type: "link", href, children: parseInline(text.slice(i + 1, midpoint), depth + 1) });
          i = end + 1;
          continue;
        }
      }
    }
    const strong = text.startsWith("**", i);
    const marker = strong ? "**" : text[i] === "*" ? "*" : null;
    if (marker) {
      const end = text.indexOf(marker, i + marker.length);
      if (end > i + marker.length) {
        flush();
        result.push({
          type: strong ? "strong" : "em",
          children: parseInline(text.slice(i + marker.length, end), depth + 1)
        });
        i = end + marker.length;
        continue;
      }
    }
    pending += text[i];
    i++;
  }
  flush();
  return result;
}

const isFence = line => /^ {0,3}(\x60{3,}|~{3,})([^~\x60]*)$/.exec(line);
const isHeading = line => /^ {0,3}(#{1,6})\s+(.+)$/.exec(line);
const isRule = line => /^ {0,3}(?:-{3,}|\*{3,}|_{3,})\s*$/.test(line);
const listMatch = line => /^ {0,3}([-+*]|\d+[.)])\s+(.+)$/.exec(line);
const isQuote = line => /^ {0,3}>\s?(.*)$/.exec(line);
const startsBlock = line => !!(isFence(line) || isHeading(line) || isRule(line) || listMatch(line) || isQuote(line));

export function parseMarkdown(source) {
  if (typeof source !== "string") throw new TypeError("Markdown source must be text");
  if (source.length > MAX_SOURCE_LENGTH) throw new RangeError("Markdown exceeds 512 KiB preview limit");
  const lines = source.replace(/\r\n?/g, "\n").split("\n");
  const blocks = [];
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];
    if (!line.trim()) { i++; continue; }
    const fence = isFence(line);
    if (fence) {
      const marker = fence[1];
      const language = fence[2].trim().split(/\s+/)[0].slice(0, 32);
      const body = [];
      i++;
      while (i < lines.length) {
        const close = isFence(lines[i]);
        if (close && close[1][0] === marker[0] && close[1].length >= marker.length && !close[2].trim()) {
          i++;
          break;
        }
        body.push(lines[i++]);
      }
      blocks.push({ type: "codeblock", language, text: body.join("\n") });
      continue;
    }
    const heading = isHeading(line);
    if (heading) {
      blocks.push({ type: "heading", level: heading[1].length, children: parseInline(heading[2]) });
      i++;
      continue;
    }
    if (isRule(line)) {
      blocks.push({ type: "rule" });
      i++;
      continue;
    }
    const list = listMatch(line);
    if (list) {
      const ordered = /^\d/.test(list[1]);
      const items = [];
      while (i < lines.length) {
        const found = listMatch(lines[i]);
        if (!found || /^\d/.test(found[1]) !== ordered) break;
        let body = found[2], checked = null;
        const task = /^\[([ xX])\]\s+/.exec(body);
        if (task) {
          checked = task[1].toLowerCase() === "x";
          body = body.slice(task[0].length);
        }
        items.push({ checked, children: parseInline(body) });
        i++;
      }
      blocks.push({ type: "list", ordered, items });
      continue;
    }
    if (isQuote(line)) {
      const contents = [];
      while (i < lines.length) {
        const part = isQuote(lines[i]);
        if (!part) break;
        contents.push(part[1]);
        i++;
      }
      blocks.push({ type: "quote", children: parseInline(contents.join(" ")) });
      continue;
    }
    const paragraph = [];
    while (i < lines.length && lines[i].trim() && (paragraph.length === 0 || !startsBlock(lines[i]))) {
      paragraph.push(lines[i++].trim());
    }
    blocks.push({ type: "paragraph", children: parseInline(paragraph.join(" ")) });
  }
  return blocks;
}
