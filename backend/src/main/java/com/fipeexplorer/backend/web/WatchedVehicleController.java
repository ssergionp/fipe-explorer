package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.alerts.WatchedVehicleService;
import com.fipeexplorer.backend.auth.CurrentUserProvider;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.web.dto.WatchVehicleRequest;
import com.fipeexplorer.backend.web.dto.WatchedVehicleDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/watched-vehicles")
public class WatchedVehicleController {

    private final WatchedVehicleService watchedVehicleService;
    private final CurrentUserProvider currentUserProvider;

    public WatchedVehicleController(WatchedVehicleService watchedVehicleService,
                                     CurrentUserProvider currentUserProvider) {
        this.watchedVehicleService = watchedVehicleService;
        this.currentUserProvider = currentUserProvider;
    }

    /** Upsert: observar de novo o mesmo fipeCode atualiza o threshold em vez de ignorar. */
    @PostMapping
    public WatchedVehicleDto watch(Authentication authentication, @Valid @RequestBody WatchVehicleRequest request) {
        User user = currentUserProvider.resolve(authentication);
        return watchedVehicleService.watch(user, request.fipeCode(), request.thresholdPercent());
    }

    @GetMapping
    public List<WatchedVehicleDto> listWatched(Authentication authentication) {
        return watchedVehicleService.listWatched(currentUserProvider.resolve(authentication));
    }

    @DeleteMapping("/{fipeCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unwatch(Authentication authentication, @PathVariable String fipeCode) {
        watchedVehicleService.unwatch(currentUserProvider.resolve(authentication), fipeCode);
    }
}
