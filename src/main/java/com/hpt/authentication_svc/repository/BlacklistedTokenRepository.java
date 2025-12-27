package com.hpt.authentication_svc.repository;

import com.hpt.authentication_svc.model.BlacklistedToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlacklistedTokenRepository extends MongoRepository<BlacklistedToken, String> {

    boolean existsByToken(String token);
}

