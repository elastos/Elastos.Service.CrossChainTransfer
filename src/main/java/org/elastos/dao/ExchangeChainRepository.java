package org.elastos.dao;

import org.elastos.dto.ExchangeChain;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ExchangeChainRepository extends CrudRepository<ExchangeChain, Long> {
}
