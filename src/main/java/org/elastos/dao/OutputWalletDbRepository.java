package org.elastos.dao;

import org.elastos.dto.OutputWalletDb;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface OutputWalletDbRepository extends CrudRepository<OutputWalletDb, Long> {
    Optional<OutputWalletDb> findByChainId(Long chainId);
}
