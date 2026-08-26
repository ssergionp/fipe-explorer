package com.fipeexplorer.backend.favorites;

import com.fipeexplorer.backend.domain.FavoriteVehicle;
import com.fipeexplorer.backend.domain.PriceEntry;
import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.repository.FavoriteVehicleRepository;
import com.fipeexplorer.backend.repository.PriceEntryRepository;
import com.fipeexplorer.backend.web.dto.VehicleSearchResultDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Favoritar é idempotente por design (like-button, não um formulário): favoritar algo já
 * favoritado nunca é erro, só devolve o que já existia. Nenhuma linha duplicada é criada (a
 * constraint UNIQUE em (user_id, price_entry_id) é a garantia final contra corrida).
 */
@Service
public class FavoriteService {

    private final FavoriteVehicleRepository favoriteVehicleRepository;
    private final PriceEntryRepository priceEntryRepository;

    public FavoriteService(FavoriteVehicleRepository favoriteVehicleRepository,
                            PriceEntryRepository priceEntryRepository) {
        this.favoriteVehicleRepository = favoriteVehicleRepository;
        this.priceEntryRepository = priceEntryRepository;
    }

    /** {@code created} indica se essa chamada criou o favorito agora (201) ou ele já existia (200). */
    public record Result(VehicleSearchResultDto vehicle, boolean created) {
    }

    @Transactional
    public Result addFavorite(User user, Long priceEntryId) {
        Optional<FavoriteVehicle> existing = favoriteVehicleRepository.findByUserAndPriceEntry_Id(user, priceEntryId);
        if (existing.isPresent()) {
            return new Result(VehicleSearchResultDto.from(existing.get().getPriceEntry()), false);
        }

        PriceEntry priceEntry = priceEntryRepository.findById(priceEntryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Registro de preço não encontrado: " + priceEntryId));

        try {
            favoriteVehicleRepository.save(new FavoriteVehicle(user, priceEntry));
        } catch (DataIntegrityViolationException e) {
            // corrida entre o findBy acima e este save: outra requisição já criou - segue idempotente.
            return new Result(VehicleSearchResultDto.from(priceEntry), false);
        }

        return new Result(VehicleSearchResultDto.from(priceEntry), true);
    }

    public List<VehicleSearchResultDto> listFavorites(User user) {
        return favoriteVehicleRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(f -> VehicleSearchResultDto.from(f.getPriceEntry()))
                .toList();
    }

    @Transactional
    public void removeFavorite(User user, Long priceEntryId) {
        favoriteVehicleRepository.deleteByUserAndPriceEntry_Id(user, priceEntryId);
    }
}
