package study.middleware.mqreliability.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import study.middleware.mqreliability.shift.CreateShiftChangeRequest;
import study.middleware.mqreliability.shift.ShiftChange;
import study.middleware.mqreliability.shift.ShiftChangeCommand;
import study.middleware.mqreliability.shift.ShiftChangeRepository;

@RestController
@RequestMapping("/api/shift-changes")
public class ShiftChangeController {
    private final ShiftChangeCommand command;
    private final ShiftChangeRepository repository;

    public ShiftChangeController(ShiftChangeCommand command, ShiftChangeRepository repository) {
        this.command = command;
        this.repository = repository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftChange create(@Valid @RequestBody CreateShiftChangeRequest request) {
        return command.create(request);
    }

    @GetMapping("/{id}")
    public ShiftChange get(@PathVariable String id) {
        return repository.find(id).orElseThrow(() -> new ResourceNotFoundException("shift-change", id));
    }
}
