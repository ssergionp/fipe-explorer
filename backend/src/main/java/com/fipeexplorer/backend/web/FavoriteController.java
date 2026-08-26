package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.auth.CurrentUserProvider;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.favorites.FavoriteService;
import com.fipeexplorer.backend.web.dto.CreateFavoriteRequest;
import com.fipeexplorer.backend.web.dto.VehicleSearchResultDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final CurrentUserProvider currentUserProvider;

    public FavoriteController(FavoriteService favoriteService, CurrentUserProvider currentUserProvider) {
        this.favoriteService = favoriteService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<VehicleSearchResultDto> addFavorite(Authentication authentication,
                                                                @Valid @RequestBody CreateFavoriteRequest request) {
        User user = currentUserProvider.resolve(authentication);
        FavoriteService.Result result = favoriteService.addFavorite(user, request.priceEntryId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.vehicle());
    }

    @GetMapping
    public List<VehicleSearchResultDto> listFavorites(Authentication authentication) {
        return favoriteService.listFavorites(currentUserProvider.resolve(authentication));
    }

    @DeleteMapping("/{priceEntryId}")
    public ResponseEntity<Void> removeFavorite(Authentication authentication, @PathVariable Long priceEntryId) {
        favoriteService.removeFavorite(currentUserProvider.resolve(authentication), priceEntryId);
        return ResponseEntity.noContent().build();
    }
}
