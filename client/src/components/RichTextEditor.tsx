import { useEffect } from "react";
import { EditorContent, useEditor } from "@tiptap/react";
import Placeholder from "@tiptap/extension-placeholder";
import StarterKit from "@tiptap/starter-kit";
import {
  Bold as BoldIcon,
  Heading2,
  Link2,
  List,
  ListOrdered,
  Pilcrow,
  Quote,
  Redo2,
  Undo2,
} from "lucide-react";
import { richTextEditorHtml } from "./RichText";

export interface RichTextEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  ariaLabel: string;
}

export default function RichTextEditor({
  value,
  onChange,
  placeholder,
  ariaLabel,
}: RichTextEditorProps) {
  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit.configure({
        heading: { levels: [2] },
        code: false,
        codeBlock: false,
        horizontalRule: false,
        strike: false,
        link: {
          openOnClick: false,
          autolink: true,
          defaultProtocol: "https",
        },
      }),
      Placeholder.configure({ placeholder }),
    ],
    content: richTextEditorHtml(value),
    editorProps: {
      attributes: {
        class: "rich-text-editor-content",
        role: "textbox",
        "aria-label": ariaLabel,
      },
    },
    onUpdate: ({ editor: currentEditor }) => {
      onChange(currentEditor.isEmpty ? "" : currentEditor.getHTML());
    },
  });

  useEffect(() => {
    if (!editor) return;
    const nextContent = richTextEditorHtml(value);
    if (editor.getHTML() !== nextContent) {
      editor.commands.setContent(nextContent, { emitUpdate: false });
    }
  }, [editor, value]);

  if (!editor) return null;

  function editLink() {
    const previousUrl = editor?.getAttributes("link").href || "";
    const url = window.prompt("연결할 주소를 입력해 주세요.", previousUrl);
    if (url === null) return;
    if (!url.trim()) {
      editor?.chain().focus().extendMarkRange("link").unsetLink().run();
      return;
    }

    editor
      ?.chain()
      .focus()
      .extendMarkRange("link")
      .setLink({ href: url.trim() })
      .run();
  }

  const toolbarButtons = [
    {
      label: "본문",
      icon: <Pilcrow size={16} />,
      active: editor.isActive("paragraph"),
      disabled: false,
      action: () => editor.chain().focus().setParagraph().run(),
    },
    {
      label: "제목 2",
      icon: <Heading2 size={16} />,
      active: editor.isActive("heading", { level: 2 }),
      disabled: false,
      action: () => editor.chain().focus().toggleHeading({ level: 2 }).run(),
    },
    {
      label: "굵게",
      icon: <BoldIcon size={16} />,
      active: editor.isActive("bold"),
      disabled: !editor.can().chain().focus().toggleBold().run(),
      action: () => editor.chain().focus().toggleBold().run(),
    },
    {
      label: "글머리 목록",
      icon: <List size={16} />,
      active: editor.isActive("bulletList"),
      disabled: false,
      action: () => editor.chain().focus().toggleBulletList().run(),
    },
    {
      label: "번호 목록",
      icon: <ListOrdered size={16} />,
      active: editor.isActive("orderedList"),
      disabled: false,
      action: () => editor.chain().focus().toggleOrderedList().run(),
    },
    {
      label: "인용문",
      icon: <Quote size={16} />,
      active: editor.isActive("blockquote"),
      disabled: false,
      action: () => editor.chain().focus().toggleBlockquote().run(),
    },
    {
      label: "링크",
      icon: <Link2 size={16} />,
      active: editor.isActive("link"),
      disabled: false,
      action: editLink,
    },
  ];

  return (
    <div className="rich-text-editor">
      <div className="rich-text-toolbar" role="toolbar" aria-label="글 서식">
        {toolbarButtons.map((button) => (
          <button
            key={button.label}
            type="button"
            className={button.active ? "active" : ""}
            aria-label={button.label}
            aria-pressed={button.active}
            title={button.label}
            disabled={button.disabled}
            onClick={button.action}
          >
            {button.icon}
          </button>
        ))}
        <span className="rich-text-toolbar-separator" aria-hidden="true" />
        <button
          type="button"
          aria-label="실행 취소"
          title="실행 취소"
          disabled={!editor.can().chain().focus().undo().run()}
          onClick={() => editor.chain().focus().undo().run()}
        >
          <Undo2 size={16} />
        </button>
        <button
          type="button"
          aria-label="다시 실행"
          title="다시 실행"
          disabled={!editor.can().chain().focus().redo().run()}
          onClick={() => editor.chain().focus().redo().run()}
        >
          <Redo2 size={16} />
        </button>
      </div>
      <EditorContent editor={editor} />
    </div>
  );
}
