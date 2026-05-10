export function insertAtCaret(textarea, currentValue, insertion) {
    const start = textarea.selectionStart ?? currentValue.length;
    const end = textarea.selectionEnd ?? currentValue.length;
    const before = currentValue.slice(0, start);
    const after = currentValue.slice(end);
    const nextValue = `${before}${insertion}${after}`;
    const nextCaretPosition = before.length + insertion.length;
    return { nextValue, nextCaretPosition };
}
