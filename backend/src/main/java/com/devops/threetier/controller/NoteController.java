package com.devops.threetier.controller;

import com.devops.threetier.model.Note;
import com.devops.threetier.service.NoteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<Note> listNotes() {
        return noteService.getAllNotes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Note createNote(@Valid @RequestBody CreateNoteRequest request) {
        return noteService.createNote(request.title(), request.content());
    }

    public record CreateNoteRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 1000) String content) {
    }
}
