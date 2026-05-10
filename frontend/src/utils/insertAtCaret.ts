export interface CaretInsertResult {
  nextValue: string;
  nextCaretPosition: number;
}

export function insertAtCaret(
  textarea: HTMLTextAreaElement,
  currentValue: string,
  insertion: string
): CaretInsertResult {
  const start = textarea.selectionStart ?? currentValue.length;
  const end = textarea.selectionEnd ?? currentValue.length;
  const before = currentValue.slice(0, start);
  const after = currentValue.slice(end);
  const nextValue = `${before}${insertion}${after}`;
  const nextCaretPosition = before.length + insertion.length;
  return { nextValue, nextCaretPosition };
}
