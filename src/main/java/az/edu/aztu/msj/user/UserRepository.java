package az.edu.aztu.msj.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from User u where :role member of u.roles and u.status = 'ACTIVE'")
    List<User> findByRole(@Param("role") String role);

    @Query("""
            select u from User u
            where (:role is null or :role member of u.roles)
              and (:q is null or lower(u.firstName) like lower(concat('%', cast(:q as string), '%'))
                              or lower(u.lastName)  like lower(concat('%', cast(:q as string), '%'))
                              or lower(u.email)     like lower(concat('%', cast(:q as string), '%')))
            """)
    Page<User> search(@Param("q") String q, @Param("role") String role, Pageable pageable);
}
