package org.elastos.dao;

import org.elastos.dto.ExchangeWalletDb;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface ExchangeWalletDbRepository extends CrudRepository<ExchangeWalletDb, Long> {
    Optional<ExchangeWalletDb> findByChainId(Long chainId);
}
