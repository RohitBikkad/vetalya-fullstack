package com.pet.vetalya.clinic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/clinic")
@PreAuthorize("isAuthenticated()")
public class ClinicWorkspaceController {
    private final ClinicWorkspaceService service;
    public ClinicWorkspaceController(ClinicWorkspaceService service) { this.service=service; }
    @GetMapping("/hospitals")
    public List<Map<String,Object>> hospitals(@AuthenticationPrincipal Jwt jwt) { return service.hospitals(jwt.getSubject()); }
    @GetMapping("/hospitals/{hospitalId}/dashboard")
    public Map<String,Object> dashboard(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId,@RequestParam(required=false) LocalDate date) {
        return service.dashboard(jwt.getSubject(),hospitalId,date==null?LocalDate.now(ZoneId.of("Asia/Kolkata")):date);
    }
    @GetMapping("/hospitals/{hospitalId}/staff")
    public List<Map<String,Object>> staff(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId) { return service.staff(jwt.getSubject(),hospitalId); }
    public record Assignment(@NotBlank @Email @Size(max=255) String email,@NotBlank @Size(max=100) String department,boolean active) {}
    @PutMapping("/hospitals/{hospitalId}/staff")
    public void assign(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId,@Valid @RequestBody Assignment data) {
        service.assign(jwt.getSubject(),hospitalId,data.email(),data.department(),data.active());
    }
    public record Task(@NotBlank @Size(max=200) String title,@NotBlank String assignedRole,@Pattern(regexp="ROUTINE|URGENT") @NotNull String priority) {}
    @PostMapping("/hospitals/{hospitalId}/tasks")
    public void task(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId,@Valid @RequestBody Task data) {
        service.createTask(jwt.getSubject(),hospitalId,data.title(),data.assignedRole(),data.priority());
    }
    @PostMapping("/hospitals/{hospitalId}/tasks/{taskId}/complete")
    public void complete(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId,@PathVariable String taskId) { service.completeTask(jwt.getSubject(),hospitalId,taskId); }
    @PostMapping("/hospitals/{hospitalId}/appointments/{appointmentId}/check-in")
    public void checkIn(@AuthenticationPrincipal Jwt jwt,@PathVariable String hospitalId,@PathVariable String appointmentId) { service.checkIn(jwt.getSubject(),hospitalId,appointmentId); }
}
