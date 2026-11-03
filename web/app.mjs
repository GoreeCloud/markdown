import { parseMarkdown, MAX_SOURCE_LENGTH } from "../src/markdown.mjs";

const node = id => document.getElementById(id);
const editor = node("source");
const preview = node("preview");
const status = node("status");
const chooser = node("file");
let filename = "Untitled.md";
let dirty = false;
let scheduled = false;

function setStatus(message) { status.textContent = message; }
function markDirty(value) {
  dirty = value;
  node("dirty").textContent = dirty ? "• Unsaved changes" : "";
}

function appendInline(parent, tokens) {
  for (const token of tokens) {
    if (token.type === "text") {
      parent.append(document.createTextNode(token.text));
      continue;
    }
    if (token.type === "code") {
      const code = document.createElement("code");
      code.textContent = token.text;
      parent.append(code);
      continue;
    }
    const tag = { strong: "strong", em: "em", link: "a" }[token.type];
    if (!tag) continue;
    const element = document.createElement(tag);
    if (token.type === "link") {
      element.href = token.href;
      element.target = "_blank";
      element.rel = "noopener noreferrer";
      element.referrerPolicy = "no-referrer";
    }
    appendInline(element, token.children);
    parent.append(element);
  }
}

function renderBlock(block) {
  let element;
  switch (block.type) {
    case "heading":
      element = document.createElement("h" + block.level);
      appendInline(element, block.children);
      break;
    case "paragraph":
      element = document.createElement("p");
      appendInline(element, block.children);
      break;
    case "quote":
      element = document.createElement("blockquote");
      appendInline(element, block.children);
      break;
    case "rule":
      element = document.createElement("hr");
      break;
    case "codeblock": {
      element = document.createElement("pre");
      const code = document.createElement("code");
      code.textContent = block.text;
      if (block.language) code.setAttribute("data-language", block.language);
      element.append(code);
      break;
    }
    case "list":
      element = document.createElement(block.ordered ? "ol" : "ul");
      for (const item of block.items) {
        const li = document.createElement("li");
        if (item.checked !== null) {
          const box = document.createElement("input");
          box.type = "checkbox";
          box.checked = item.checked;
          box.disabled = true;
          box.setAttribute("aria-label", item.checked ? "Completed task" : "Incomplete task");
          li.append(box, document.createTextNode(" "));
        }
        appendInline(li, item.children);
        element.append(li);
      }
      break;
    default:
      return document.createTextNode("");
  }
  return element;
}

function refresh() {
  scheduled = false;
  try {
    const blocks = parseMarkdown(editor.value);
    const fragment = document.createDocumentFragment();
    if (!blocks.length) {
      const empty = document.createElement("p");
      empty.className = "empty";
      empty.textContent = "Your preview will appear here. Start typing on the left.";
      fragment.append(empty);
    } else {
      for (const block of blocks) fragment.append(renderBlock(block));
    }
    preview.replaceChildren(fragment);
  } catch {
    const message = document.createElement("p");
    message.textContent = "This document cannot be previewed within the 512 KiB limit.";
    preview.replaceChildren(message);
    setStatus("Preview unavailable: source exceeds the supported limit.");
  }
  const words = (editor.value.match(/\S+/g) || []).length;
  node("count").textContent = words + " words · " + editor.value.length + " characters";
}
function queueRefresh() {
  if (!scheduled) {
    scheduled = true;
    requestAnimationFrame(refresh);
  }
}

function safeFilename(input) {
  const cleaned = input.replace(/[\\/\u0000-\u001f\u007f\u202e]/g, "_").replace(/^\.+/, "").slice(0, 100);
  const basename = cleaned.replace(/\.(markdown|md|txt)$/i, "").trim();
  return (basename || "Untitled") + ".md";
}

node("open").addEventListener("click", () => chooser.click());
chooser.addEventListener("change", async () => {
  const file = chooser.files && chooser.files[0];
  chooser.value = "";
  if (!file) return;
  if (dirty && !confirm("Discard current unsaved changes and open another file?")) return;
  if (!/\.(md|markdown|txt)$/i.test(file.name)) {
    setStatus("Only Markdown and plain-text files are supported.");
    return;
  }
  if (file.size > MAX_SOURCE_LENGTH) {
    setStatus("File is too large. Maximum size is 512 KiB.");
    return;
  }
  try {
    const contents = await file.text();
    if (contents.length > MAX_SOURCE_LENGTH) throw new RangeError("File is too large");
    editor.value = contents;
    filename = safeFilename(file.name);
    node("filename").textContent = filename;
    markDirty(false);
    queueRefresh();
    setStatus("Opened locally. Original file was not modified.");
    editor.focus();
  } catch {
    setStatus("Could not read this file.");
  }
});

function download() {
  const blob = new Blob([editor.value], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  try {
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
    setStatus("Download requested. Confirm the file was saved before closing; unsaved-change protection remains active.");
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 2000);
  }
}
node("download").addEventListener("click", download);
node("clear").addEventListener("click", () => {
  if (dirty && !confirm("Discard unsaved changes and start a new document?")) return;
  editor.value = "";
  filename = "Untitled.md";
  node("filename").textContent = filename;
  markDirty(false);
  queueRefresh();
  setStatus("New local document. No data has been saved.");
  editor.focus();
});
editor.addEventListener("input", () => {
  markDirty(true);
  queueRefresh();
  setStatus("Editing locally. Download to keep your changes.");
});
document.addEventListener("keydown", event => {
  if ((event.ctrlKey || event.metaKey) && !event.altKey && !event.shiftKey) {
    if (event.key.toLowerCase() === "s") {
      event.preventDefault();
      download();
    } else if (event.key.toLowerCase() === "o") {
      event.preventDefault();
      chooser.click();
    }
  }
});
window.addEventListener("beforeunload", event => {
  if (!dirty) return;
  event.preventDefault();
  event.returnValue = "";
});
refresh();
