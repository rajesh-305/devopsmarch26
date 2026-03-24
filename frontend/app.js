const noteForm = document.getElementById("noteForm");
const notesContainer = document.getElementById("notes");
const statusText = document.getElementById("status");

async function loadNotes() {
  const response = await fetch("/api/notes");
  const notes = await response.json();

  if (!Array.isArray(notes) || notes.length === 0) {
    notesContainer.innerHTML = "<p>No notes yet.</p>";
    return;
  }

  notesContainer.innerHTML = notes
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map(
      (note) => `
        <article class="note">
          <p class="note-title">${escapeHtml(note.title)}</p>
          <p class="note-content">${escapeHtml(note.content)}</p>
        </article>
      `
    )
    .join("");
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

noteForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const formData = new FormData(noteForm);
  const payload = {
    title: formData.get("title"),
    content: formData.get("content")
  };

  statusText.textContent = "Saving...";

  const response = await fetch("/api/notes", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    statusText.textContent = "Failed to save note.";
    return;
  }

  noteForm.reset();
  statusText.textContent = "Saved.";
  await loadNotes();
});

loadNotes().catch(() => {
  statusText.textContent = "Unable to load notes.";
});
