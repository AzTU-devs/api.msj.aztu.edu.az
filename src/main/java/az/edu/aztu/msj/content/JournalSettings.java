package az.edu.aztu.msj.content;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "journal_settings")
@Getter
@Setter
public class JournalSettings {

    @Id
    private Short id = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "journal_title")
    private Map<String, String> journalTitle;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> tagline;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> about;

    @Column(name = "issn_print")
    private String issnPrint;

    @Column(name = "issn_online")
    private String issnOnline;

    @Column(name = "doi_prefix")
    private String doiPrefix;

    private String publisher;
    private String email;
    private String phone;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "indexed_in")
    private List<String> indexedIn;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> social;

    @Column(name = "publication_fee")
    private String publicationFee;

    @Column(name = "logo_url")
    private String logoUrl;

    /** Journal-record plate rows, i18n: {"en":[["Print ISSN","2227-6912"],...],"az":[...]} */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> record;

    /** Running ticker rows, same shape as {@link #record}. */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> ticker;
}
