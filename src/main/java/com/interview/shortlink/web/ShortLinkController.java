package com.interview.shortlink.web;

import com.interview.shortlink.dto.*;
import com.interview.shortlink.service.ShortLinkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Validated
@RestController
public class ShortLinkController {

    private static final String CODE_PATTERN = "^[0-9A-Za-z_-]{4,32}$";
    private final ShortLinkService service;

    public ShortLinkController(ShortLinkService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/links")
    public ResponseEntity<ShortLinkResponse> create(@Valid @RequestBody CreateShortLinkRequest request) {
        ShortLinkResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/links/" + created.shortCode())).body(created);
    }

    @GetMapping("/api/v1/links/{code}")
    public ShortLinkResponse get(@PathVariable @Pattern(regexp = CODE_PATTERN) String code) {
        return service.get(code);
    }

    @PatchMapping("/api/v1/links/{code}/status")
    public ShortLinkResponse changeStatus(@PathVariable @Pattern(regexp = CODE_PATTERN) String code,
                                          @Valid @RequestBody ChangeStatusRequest request) {
        return service.changeStatus(code, request.status());
    }

    @GetMapping("/api/v1/links/{code}/stats")
    public LinkStatsResponse stats(@PathVariable @Pattern(regexp = CODE_PATTERN) String code) {
        return service.stats(code);
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable @Pattern(regexp = CODE_PATTERN) String code) {
        String target = service.resolveTarget(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .build();
    }
}
