package com.devops.threetier.service;

import com.devops.threetier.model.Note;
import com.devops.threetier.repository.NoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Note createNote(String title, String content) {
        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        return noteRepository.save(note);
    }
}
