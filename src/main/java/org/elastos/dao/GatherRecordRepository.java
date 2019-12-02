package org.elastos.dao;

import org.elastos.dto.GatherRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface GatherRecordRepository extends CrudRepository<GatherRecord, Long> {
    List<GatherRecord> findAllByTxHashIsNull();
}
