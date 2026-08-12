package az.edu.aztu.msj.board;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/board")
@Tag(name = "Editorial Board")
public class BoardController {

    private final BoardMemberRepository repository;

    public BoardController(BoardMemberRepository repository) {
        this.repository = repository;
    }

    public record BoardMemberDto(Long id, String fullName, String title, String section,
                                 String photoUrl, String orcidUrl, String scopusUrl,
                                 String email, String country) {
        static BoardMemberDto from(BoardMember m) {
            return new BoardMemberDto(m.getId(), m.getFullName(), m.getTitle(), m.getSection(),
                    m.getPhotoUrl(), m.getOrcidUrl(), m.getScopusUrl(), m.getEmail(), m.getCountry());
        }
    }

    @GetMapping
    @Operation(summary = "List the editorial board grouped by section")
    public List<BoardMemberDto> list() {
        return repository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(BoardMemberDto::from)
                .toList();
    }
}
