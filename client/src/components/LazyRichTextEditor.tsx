import { lazy, Suspense } from "react";
import type { RichTextEditorProps } from "./RichTextEditor";

const RichTextEditor = lazy(() => import("./RichTextEditor"));

export function LazyRichTextEditor(props: RichTextEditorProps) {
  return (
    <Suspense
      fallback={
        <div className="rich-text-editor-loading">에디터를 불러오는 중...</div>
      }
    >
      <RichTextEditor {...props} />
    </Suspense>
  );
}
