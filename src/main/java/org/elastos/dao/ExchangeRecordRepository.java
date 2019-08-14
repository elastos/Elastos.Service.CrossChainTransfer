package org.elastos.dao;

import org.elastos.dto.ExchangeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Repository
public interface ExchangeRecordRepository extends PagingAndSortingRepository<ExchangeRecord, Long> {
    List<ExchangeRecord> findAllByDid(String did, Pageable pageable);

    List<ExchangeRecord> findAllByStateIn(List<String> states);

}
