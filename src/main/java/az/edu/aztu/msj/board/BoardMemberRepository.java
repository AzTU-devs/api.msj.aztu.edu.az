package az.edu.aztu.msj.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {
    List<BoardMember> findByActiveTrueOrderBySortOrderAsc();
}
