package com.balex.rag.repo;

import com.balex.rag.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByIdAndDeletedFalse (Integer id);

    Optional<User> findUserByEmailAndDeletedFalse(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

}


