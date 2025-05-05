package hu.gerab.payment.repository;

import hu.gerab.payment.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
