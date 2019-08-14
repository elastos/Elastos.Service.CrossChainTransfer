package org.elastos.dao;

import org.elastos.dto.ExchangeRate;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ExchangeRateRepository extends CrudRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findBySrcChainIdAndDstChainId(Long srcChainId, Long dstChainId);
}
