export async function processAudio(file) {
    const formData = new FormData();
    formData.append("file", file);
    const response = await fetch("http://localhost:8080/api/notes/process", {
        method: "POST",
        body: formData
    });
    if (!response.ok) {
        const errorBody = await response.json().catch(() => ({ error: "Upload failed" }));
        throw new Error(errorBody.error ?? "Upload failed");
    }
    return (await response.json());
}
