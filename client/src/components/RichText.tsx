import DOMPurify from "dompurify";

const allowedTags = [
  "p",
  "h2",
  "strong",
  "em",
  "ul",
  "ol",
  "li",
  "blockquote",
  "br",
  "a",
];

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function hasHtmlTag(value: string) {
  return /<\/?[a-z][\s\S]*>/i.test(value);
}

export function richTextEditorHtml(value: string) {
  if (!value) return "";
  if (hasHtmlTag(value)) return value;

  return value
    .split(/\n{2,}/)
    .map(
      (paragraph) => `<p>${escapeHtml(paragraph).replaceAll("\n", "<br>")}</p>`,
    )
    .join("");
}

function displayHtml(value: string) {
  const html = hasHtmlTag(value)
    ? value
    : `<p>${escapeHtml(value).replaceAll("\n", "<br>")}</p>`;

  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: allowedTags,
    ALLOWED_ATTR: ["href", "rel"],
    ALLOWED_URI_REGEXP: /^(?:(?:https?):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i,
  });
}

export function richTextToPlainText(value?: string | null) {
  if (!value) return "";

  const wrapper = document.createElement("div");
  wrapper.innerHTML = displayHtml(value);
  return (wrapper.textContent || "").replace(/\s+/g, " ").trim();
}

export function isRichTextEmpty(value?: string | null) {
  return !richTextToPlainText(value);
}

export function RichTextContent({
  content,
  className = "prose rich-text-content",
}: {
  content: string;
  className?: string;
}) {
  return (
    <div
      className={className}
      dangerouslySetInnerHTML={{ __html: displayHtml(content) }}
    />
  );
}
