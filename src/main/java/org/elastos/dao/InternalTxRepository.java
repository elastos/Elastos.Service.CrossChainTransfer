package org.elastos.dao;

import org.elastos.dto.InternalTxRecord;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface InternalTxRepository extends PagingAndSortingRepository<InternalTxRecord, Long> {
}
