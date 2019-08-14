package org.elastos.dao;

import org.elastos.dto.RenewalWalletDb;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface RenewalWalletDbRepository extends CrudRepository<RenewalWalletDb, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value="UPDATE RenewalWalletDb rw SET rw.maxUse=?2 WHERE rw.maxUse<?2 and rw.id=?1")
    int setMaxUse(Long id, Integer maxUse);


    List<RenewalWalletDb> findByChainId(Long chainId);
}
