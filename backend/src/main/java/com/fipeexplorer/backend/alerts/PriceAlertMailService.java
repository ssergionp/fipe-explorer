package com.fipeexplorer.backend.alerts;

import com.fipeexplorer.backend.domain.User;
import com.fipeexplorer.backend.domain.VehicleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Envio via Spring Mail configurado pro relay SMTP do Resend (spring.mail.* no application.yml).
 * Falha de envio é logada, nunca propagada - um problema de e-mail (credencial errada, domínio
 * não verificado no Resend etc.) não pode derrubar o job de comparação nem o import.
 */
@Service
public class PriceAlertMailService {

    private static final Logger log = LoggerFactory.getLogger(PriceAlertMailService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PriceAlertMailService(JavaMailSender mailSender, @Value("${fipe.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendPriceAlert(User user, VehicleModel vehicleModel, List<PriceAlertService.PriceChange> changes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("FIPE Explorer: mudança de preço em " + vehicleModel.getBrand().getName()
                    + " " + vehicleModel.getName());
            message.setText(buildBody(vehicleModel, changes));
            mailSender.send(message);
            log.info("Alerta de preço enviado pra {} sobre fipe_code {}.", user.getEmail(), vehicleModel.getFipePriceCode());
        } catch (MailException e) {
            log.error("Falha ao enviar alerta de preço pra {} sobre fipe_code {}: {}",
                    user.getEmail(), vehicleModel.getFipePriceCode(), e.getMessage(), e);
        }
    }

    private String buildBody(VehicleModel vehicleModel, List<PriceAlertService.PriceChange> changes) {
        StringBuilder body = new StringBuilder();
        body.append(vehicleModel.getBrand().getName()).append(' ').append(vehicleModel.getName())
                .append(" (código FIPE ").append(vehicleModel.getFipePriceCode()).append(") teve o preço mudar:\n\n");

        for (PriceAlertService.PriceChange change : changes) {
            String direction = change.changePercent().signum() >= 0 ? "subiu" : "caiu";
            String percentText = formatPercent(change.changePercent().abs());
            body.append("- ").append(change.yearValue()).append(" (").append(change.fuel()).append("): ")
                    .append(formatCurrency(change.oldPrice())).append(" -> ").append(formatCurrency(change.newPrice()))
                    .append(" (").append(direction).append(' ').append(percentText).append(")\n");
        }

        body.append("\nEsse alerta usa o threshold que você configurou pra este veículo. "
                + "Pra parar de observar, acesse a área de veículos observados.");
        return body.toString();
    }

    private String formatCurrency(BigDecimal value) {
        return "R$ " + value.setScale(2, RoundingMode.HALF_UP).toString().replace('.', ',');
    }

    private String formatPercent(BigDecimal fraction) {
        BigDecimal percent = fraction.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
        return String.format(PT_BR, "%.1f%%", percent);
    }
}
