package org.elastos.dao;

import org.elastos.dto.GatherAddress;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GatherAddressRepository extends CrudRepository<GatherAddress, Long> {
}
