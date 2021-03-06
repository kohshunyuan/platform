package io.fundrequest.platform.admin.claim.continuous;

import io.fundrequest.core.request.claim.domain.ClaimRequestStatus;
import io.fundrequest.core.request.claim.domain.RequestClaim;
import io.fundrequest.core.request.claim.infrastructure.RequestClaimRepository;
import io.fundrequest.core.request.infrastructure.azrael.AzraelClient;
import io.fundrequest.core.transactions.TransactionStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.fundrequest.core.request.claim.domain.ClaimRequestStatus.APPROVED;

@Component
@ConditionalOnProperty(value = "io.fundrequest.request-claim-vacuumer.enabled", havingValue = "true")
public class RequestClaimVacuumer {

    private final RequestClaimRepository requestClaimRepository;
    private AzraelClient azraelClient;

    public RequestClaimVacuumer(final RequestClaimRepository requestClaimRepository,
                                final AzraelClient azraelClient) {
        this.requestClaimRepository = requestClaimRepository;
        this.azraelClient = azraelClient;
    }

    @Scheduled(fixedDelay = 300_000L)
    public void clean() {
        final List<RequestClaim> claims = requestClaimRepository.findByStatus(APPROVED);

        claims.stream()
              .filter(x -> x.getTransactionHash() != null)
              .filter(x -> azraelClient.getTransactionStatus(x.getTransactionHash()).equals(TransactionStatus.FAILED))
              .forEach(x -> {
                  x.setStatus(ClaimRequestStatus.TRANSACTION_FAILED);
                  requestClaimRepository.save(x);
              });
    }
}
