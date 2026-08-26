package com.fipeexplorer.backend.web;

import com.fipeexplorer.backend.auth.CurrentUserProvider;
import com.fipeexplorer.backend.estimate.SavedPriceEstimateService;
import com.fipeexplorer.backend.web.dto.SavePriceEstimateRequest;
import com.fipeexplorer.backend.web.dto.SavedPriceEstimateDto;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/price-estimates")
public class SavedPriceEstimateController {

    private final SavedPriceEstimateService savedPriceEstimateService;
    private final CurrentUserProvider currentUserProvider;

    public SavedPriceEstimateController(SavedPriceEstimateService savedPriceEstimateService,
                                         CurrentUserProvider currentUserProvider) {
        this.savedPriceEstimateService = savedPriceEstimateService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedPriceEstimateDto save(Authentication authentication, @Valid @RequestBody SavePriceEstimateRequest request) {
        return savedPriceEstimateService.save(currentUserProvider.resolve(authentication), request);
    }

    @GetMapping
    public List<SavedPriceEstimateDto> list(Authentication authentication) {
        return savedPriceEstimateService.list(currentUserProvider.resolve(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        boolean deleted = savedPriceEstimateService.delete(currentUserProvider.resolve(authentication), id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estimativa salva não encontrada: " + id);
        }
    }
}
