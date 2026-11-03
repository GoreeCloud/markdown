import test from "node:test";
import assert from "node:assert/strict";
import { parseMarkdown, parseInline, safeHref, MAX_SOURCE_LENGTH } from "../src/markdown.mjs";

test("headings, paragraphs, emphasis and HTTPS links yield data tokens", () => {
  const blocks = parseMarkdown("# Hello\n\nA **strong** and *soft* [link](https://example.org/notes)");
  assert.equal(blocks[0].type, "heading");
  assert.equal(blocks[0].level, 1);
  assert.equal(blocks[1].type, "paragraph");
  assert.deepEqual(blocks[1].children.map(item => item.type), ["text", "strong", "text", "em", "text", "link"]);
  assert.equal(blocks[1].children[5].href, "https://example.org/notes");
});

test("task lists, ordered items, quotes, rules, code fences", () => {
  const blocks = parseMarkdown("- [x] Done\n- [ ] Pending\n\n1. First\n2. Second\n\n> A quote\n> continued\n\n---\n\n~~~js\n<script>bad()</script>\n~~~");
  assert.deepEqual(blocks.map(block => block.type), ["list", "list", "quote", "rule", "codeblock"]);
  assert.deepEqual(blocks[0].items.map(item => item.checked), [true, false]);
  assert.equal(blocks[1].ordered, true);
  assert.equal(blocks[2].children[0].text, "A quote continued");
  assert.equal(blocks[4].language, "js");
  assert.equal(blocks[4].text, "<script>bad()</script>");
});

test("dangerous href schemes and malformed links remain inert literal source", () => {
  for (const href of ["javascript:alert(1)", "data:text/html,hello", "file:///etc/passwd", "//evil.test",
    "https://user:secret@example.com", "https://evil.test\\@legit.test", "http://example.test/%0a\n"]) {
    assert.equal(safeHref(href), null, href);
    const tokens = parseInline("[click](" + href + ")");
    assert.ok(!tokens.some(token => token.type === "link"), href);
  }
  assert.equal(safeHref("mailto:editor@example.com"), "mailto:editor@example.com");
  assert.equal(safeHref("https://example.com/path?q=1"), "https://example.com/path?q=1");
});

test("raw HTML remains text rather than executable HTML markup", () => {
  const blocks = parseMarkdown("<img src=x onerror=alert(1)>\n<script>alert(1)</script>");
  assert.equal(blocks.length, 1);
  assert.equal(blocks[0].type, "paragraph");
  assert.equal(blocks[0].children[0].type, "text");
  assert.match(blocks[0].children[0].text, /<script>/);
});

test("unclosed fences and CRLF are handled without losing content", () => {
  const blocks = parseMarkdown("~~~txt\r\nfirst\r\nsecond");
  assert.equal(blocks[0].type, "codeblock");
  assert.equal(blocks[0].text, "first\nsecond");
  assert.equal(parseMarkdown("\r\n \r\n").length, 0);
});

test("oversized documents and non-text inputs fail closed", () => {
  assert.throws(() => parseMarkdown("x".repeat(MAX_SOURCE_LENGTH + 1)), RangeError);
  assert.throws(() => parseMarkdown(null), TypeError);
  assert.throws(() => parseInline(null), TypeError);
});

test("nested inline tokens stop at a bounded depth", () => {
  const tokens = parseInline("**hi *there* friend**");
  assert.equal(tokens[0].type, "strong");
  assert.equal(tokens[0].children[1].type, "em");
});

test("no HTML markup is returned in preview nodes", () => {
  const blocks = parseMarkdown("[x](javascript:alert(1))\n\n~~~\n<iframe src=evil></iframe>\n~~~");
  assert.deepEqual(blocks.map(block => block.type), ["paragraph", "codeblock"]);
  assert.ok(!JSON.stringify(blocks).includes('"html"'));
});
