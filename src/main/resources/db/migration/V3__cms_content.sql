-- =====================================================================
--  V3 — editable site content (CMS): every homepage string/section moves
--  into the database so the admin dashboard can edit it. No static copy
--  remains hard-coded in the web app.
-- =====================================================================

CREATE TABLE site_texts (
    key        varchar(80) PRIMARY KEY,
    value      jsonb NOT NULL DEFAULT '{}'::jsonb,
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_site_texts_updated BEFORE UPDATE ON site_texts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE hero_slides (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    image_url  varchar(500) NOT NULL,
    caption    jsonb NOT NULL DEFAULT '{}'::jsonb,
    alt_text   varchar(300),
    sort_order int NOT NULL DEFAULT 0,
    is_active  boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TRIGGER trg_hero_slides_updated BEFORE UPDATE ON hero_slides
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE scope_topics (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    icon        varchar(30) NOT NULL DEFAULT 'gear',
    title       jsonb NOT NULL DEFAULT '{}'::jsonb,
    description jsonb NOT NULL DEFAULT '{}'::jsonb,
    sort_order  int NOT NULL DEFAULT 0,
    is_active   boolean NOT NULL DEFAULT true
);

CREATE TABLE author_steps (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    step_no    varchar(6),
    title      jsonb NOT NULL DEFAULT '{}'::jsonb,
    body       jsonb NOT NULL DEFAULT '{}'::jsonb,
    sort_order int NOT NULL DEFAULT 0
);

CREATE TABLE author_terms (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title      jsonb NOT NULL DEFAULT '{}'::jsonb,
    body       jsonb NOT NULL DEFAULT '{}'::jsonb,
    sort_order int NOT NULL DEFAULT 0
);

ALTER TABLE journal_settings ADD COLUMN record jsonb NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE journal_settings ADD COLUMN ticker jsonb NOT NULL DEFAULT '{}'::jsonb;


-- UI labels & short copy
INSERT INTO site_texts (key, value) VALUES
  ('nav.about', '{"en": "About", "az": "Haqqında"}'::jsonb),
  ('nav.current', '{"en": "Current Issue", "az": "Cari nömrə"}'::jsonb),
  ('nav.scope', '{"en": "Scope", "az": "Əhatə dairəsi"}'::jsonb),
  ('nav.board', '{"en": "Editorial Board", "az": "Redaksiya heyəti"}'::jsonb),
  ('nav.archive', '{"en": "Archive", "az": "Arxiv"}'::jsonb),
  ('nav.authors', '{"en": "For Authors", "az": "Müəlliflər üçün"}'::jsonb),
  ('nav.contact', '{"en": "Contact", "az": "Əlaqə"}'::jsonb),
  ('hero.eyebrow', '{"en": "Azerbaijan Technical University · Baku", "az": "Azərbaycan Texniki Universiteti · Bakı"}'::jsonb),
  ('hero.lede', '{"en": "An <em>international scientific and technical journal</em> on the theory of mechanisms and machines — published continuously since 2001, peer-reviewed, and free of charge to authors.", "az": "Mexanizmlər və maşınlar nəzəriyyəsinə həsr olunmuş <em>beynəlxalq elmi-texniki jurnal</em> — 2001-ci ildən fasiləsiz nəşr olunur, rəyləndirilir və müəlliflər üçün ödənişsizdir."}'::jsonb),
  ('hero.cta1', '{"en": "Read current issue", "az": "Cari nömrəni oxu"}'::jsonb),
  ('hero.cta2', '{"en": "Submit a manuscript", "az": "Məqalə göndər"}'::jsonb),
  ('spec.since', '{"en": "Published since", "az": "Nəşrə başlayıb"}'::jsonb),
  ('spec.issues', '{"en": "Issues online", "az": "Onlayn nömrə"}'::jsonb),
  ('spec.inspec', '{"en": "INSPEC indexed", "az": "INSPEC indeksi"}'::jsonb),
  ('spec.cost', '{"en": "Cost to authors", "az": "Müəlliflər üçün xərc"}'::jsonb),
  ('spec.free', '{"en": "Free", "az": "Pulsuz"}'::jsonb),
  ('about.label', '{"en": "About the journal", "az": "Jurnal haqqında"}'::jsonb),
  ('about.title', '{"en": "A quarter-century of machine science", "az": "Maşın elmində çərək əsr"}'::jsonb),
  ('plate.hd', '{"en": "Journal record", "az": "Jurnalın pasportu"}'::jsonb),
  ('current.label', '{"en": "Current issue", "az": "Cari nömrə"}'::jsonb),
  ('current.title', '{"en": "Machine Science<br>2025 — Number II", "az": "Machine Science<br>2025 — II nömrə"}'::jsonb),
  ('current.browse', '{"en": "Browse the archive", "az": "Arxivə bax"}'::jsonb),
  ('feat.tag', '{"en": "Featured · Mechanical engineering technology", "az": "Seçilmiş · Maşınqayırma texnologiyası"}'::jsonb),
  ('scope.label', '{"en": "Scope", "az": "Əhatə dairəsi"}'::jsonb),
  ('scope.title', '{"en": "Where we publish", "az": "Nəşr sahələrimiz"}'::jsonb),
  ('scope.lede', '{"en": "Authors are cordially invited to submit articles to “Machine Science” on the following topics.", "az": "Müəlliflər “Machine Science” jurnalına aşağıdakı mövzular üzrə məqalələr təqdim etməyə dəvət olunurlar."}'::jsonb),
  ('board.label', '{"en": "Editorial board", "az": "Redaksiya heyəti"}'::jsonb),
  ('board.title', '{"en": "Who reviews the work", "az": "Məqalələri kim rəyləndirir"}'::jsonb),
  ('board.eic', '{"en": "Editor-in-Chief", "az": "Baş redaktor"}'::jsonb),
  ('board.hon', '{"en": "Honorary Editor", "az": "Fəxri redaktor"}'::jsonb),
  ('board.eicT', '{"en": "Head of the Department of Machine Design, Mechatronics and Industrial Technologies, Azerbaijan Technical University.", "az": "Maşın konstruksiyası, Mexatronika və Sənaye Texnologiyaları kafedrasının müdiri, Azərbaycan Texniki Universiteti."}'::jsonb),
  ('board.honT', '{"en": "Distinguished mechanical engineer, Karlsruhe, Germany.", "az": "Görkəmli maşınqayırma mühəndisi, Karlsrue, Almaniya."}'::jsonb),
  ('board.members', '{"en": "Editors & peer reviewers", "az": "Redaktorlar və rəyçilər"}'::jsonb),
  ('archive.label', '{"en": "Archive", "az": "Arxiv"}'::jsonb),
  ('archive.title', '{"en": "Every issue, open", "az": "Bütün nömrələr, açıq"}'::jsonb),
  ('archive.lede', '{"en": "All issues are freely available as full-text PDF. No subscription, no author fee.", "az": "Bütün nömrələr tam mətn PDF formatında pulsuz əlçatandır. Abunə yoxdur, müəllif haqqı yoxdur."}'::jsonb),
  ('authors.label', '{"en": "For authors", "az": "Müəlliflər üçün"}'::jsonb),
  ('authors.title', '{"en": "From manuscript<br>to publication", "az": "Əlyazmadan<br>nəşrə qədər"}'::jsonb),
  ('contact.label', '{"en": "Contact", "az": "Əlaqə"}'::jsonb),
  ('contact.title', '{"en": "Send us<br>your research", "az": "Tədqiqatınızı<br>bizə göndərin"}'::jsonb),
  ('contact.lede', '{"en": "Editorial office of “Machine Science”, Azerbaijan Technical University. Open access questions not answered by our policy are welcome by e-mail.", "az": "“Machine Science” jurnalının redaksiyası, Azərbaycan Texniki Universiteti. Açıq giriş siyasətimizdə cavab tapmadığınız suallar üçün e-poçt yazın."}'::jsonb),
  ('contact.cta', '{"en": "Write to the editors", "az": "Redaksiyaya yazın"}'::jsonb),
  ('card.email', '{"en": "E-mail", "az": "E-poçt"}'::jsonb),
  ('card.phone', '{"en": "Telephone", "az": "Telefon"}'::jsonb),
  ('card.office', '{"en": "Editorial office", "az": "Redaksiya"}'::jsonb),
  ('card.addr', '{"en": "H. Javid ave 25, Baku AZ 1073", "az": "H. Cavid pr. 25, Bakı AZ 1073"}'::jsonb),
  ('ft.pub', '{"en": "Azerbaijan Technical University", "az": "Azərbaycan Texniki Universiteti"}'::jsonb),
  ('ft.ethics', '{"en": "Publication ethics", "az": "Nəşr etikası"}'::jsonb),
  ('ft.oa', '{"en": "Open access policy", "az": "Açıq giriş siyasəti"}'::jsonb),
  ('ft.ai', '{"en": "AI policy", "az": "Süni intellekt siyasəti"}'::jsonb),
  ('ft.peer', '{"en": "Peer review", "az": "Rəyləndirmə"}'::jsonb),
  ('fig.cap', '{"en": "<b>Fig. 1</b> — Four-bar crank-rocker driven by a 30:18 spur gear train. The brass locus is the coupler curve traced by point P; hatched triangles mark the fixed pivots.", "az": "<b>Şəkil 1</b> — 30:18 dişli ötürməsi ilə hərəkətə gətirilən dördhəlqəli mexanizm. Tunc rəngli əyri P nöqtəsinin cızdığı şatun əyrisidir; ştrixlənmiş üçbucaqlar sabit oynaqları göstərir."}'::jsonb),
  ('iss.pdf', '{"en": "Full text PDF", "az": "Tam mətn PDF"}'::jsonb),
  ('iss.current', '{"en": "Current", "az": "Cari"}'::jsonb),
  ('iss.arts', '{"en": "articles", "az": "məqalə"}'::jsonb),
  ('about.p1', '{"en": "<b>International scientific and technical journal “MACHINE SCIENCE” is published by Azerbaijan Technical University.</b> Original research papers and reviews are accepted for publication, and all manuscripts are peer-reviewed for scientific quality and acceptance.", "az": "<b>“MACHINE SCIENCE” beynəlxalq elmi-texniki jurnalı Azərbaycan Texniki Universiteti tərəfindən nəşr olunur.</b> Nəşr üçün orijinal tədqiqat məqalələri və icmallar qəbul edilir; bütün əlyazmalar elmi keyfiyyət baxımından rəyləndirilir."}'::jsonb),
  ('about.p2', '{"en": "The journal has been published since 2001. During 2001–2011 it appeared under the name <b>“Mechanics — Mechanical Engineering”</b>, and was renamed “MACHINE SCIENCE” by order № 1861 of 25 November 2011 (registration №3521). At least two editions are published each year.", "az": "Jurnal 2001-ci ildən nəşr olunur. 2001–2011-ci illərdə <b>“Mexanika — Maşınqayırma”</b> adı ilə çıxmış, 25 noyabr 2011-ci il tarixli 1861 nömrəli əmrlə (qeydiyyat №3521) “MACHINE SCIENCE” adlandırılmışdır. İldə ən azı iki nömrə nəşr olunur."}'::jsonb),
  ('about.p3', '{"en": "It is included in the list of periodical scientific publications of the <b>High Attestation Commission (HAC)</b>, in which applicants for an academic degree publish the results of their original research, and has been part of the international citation and indexing system <b>INSPEC since 2011</b>.", "az": "Jurnal elmi dərəcə iddiaçılarının orijinal tədqiqat nəticələrini dərc etdikləri <b>Ali Attestasiya Komissiyasının (AAK)</b> dövri elmi nəşrlər siyahısına daxildir və <b>2011-ci ildən INSPEC</b> beynəlxalq sitat və indeksləşdirmə sistemindədir."}'::jsonb),
  ('about.p4', '{"en": "Research areas include the theory of mechanisms and machines, the theory of friction and wear of parts, and the properties of materials used in mechanical engineering — resistance of materials, the theory of elasticity and of plasticity — alongside the reliability and quality of machines, energy efficiency and productivity, problems of automatic control, and energy and environment.", "az": "Tədqiqat sahələrinə mexanizmlər və maşınlar nəzəriyyəsi, sürtünmə və yeyilmə nəzəriyyəsi, maşınqayırmada istifadə olunan materialların xassələri — materialların müqaviməti, elastiklik və plastiklik nəzəriyyəsi — habelə maşınların etibarlılığı və keyfiyyəti, enerji səmərəliliyi və məhsuldarlığı, avtomatik idarəetmə problemləri, enerji və ətraf mühit daxildir."}'::jsonb);

-- hero slides
INSERT INTO hero_slides (image_url, caption, sort_order) VALUES
  ('/media/slides/slide1.webp', '{"en": "Mechanic", "az": "Mexanika"}'::jsonb, 0),
  ('/media/slides/slide2.webp', '{"en": "Mechanical engineering technology", "az": "Maşınqayırma texnologiyası"}'::jsonb, 1),
  ('/media/slides/slide3.webp', '{"en": "Machine design", "az": "Maşın konstruksiyası"}'::jsonb, 2),
  ('/media/slides/slide4.webp', '{"en": "Mechatronics and Robotics Engineering", "az": "Mexatronika və Robototexnika"}'::jsonb, 3),
  ('/media/slides/slide5.webp', '{"en": "Materials Science and metallurgy", "az": "Materialşünaslıq və metallurgiya"}'::jsonb, 4),
  ('/media/slides/slide6.webp', '{"en": "Automation and ICT", "az": "Avtomatlaşdırma və İKT"}'::jsonb, 5),
  ('/media/slides/slide7.webp', '{"en": "Energy and Environment", "az": "Enerji və ətraf mühit"}'::jsonb, 6);

-- scope topics
INSERT INTO scope_topics (icon, title, description, sort_order) VALUES
  ('layer', '{"en": "Materials Science", "az": "Materialşünaslıq"}'::jsonb, '{"en": "Resistance of materials, elasticity and plasticity, metallurgy, composites and coatings.", "az": "Materialların müqaviməti, elastiklik və plastiklik, metallurgiya, kompozitlər və örtüklər."}'::jsonb, 0),
  ('wave', '{"en": "Mechanics", "az": "Mexanika"}'::jsonb, '{"en": "Theory of mechanisms and machines, vibration, buckling, friction and wear of parts.", "az": "Mexanizmlər və maşınlar nəzəriyyəsi, rəqs, dayanıqlıq, sürtünmə və yeyilmə."}'::jsonb, 1),
  ('gear', '{"en": "Machine design", "az": "Maşın konstruksiyası"}'::jsonb, '{"en": "Gearing, linkages, mechatronics, reliability and quality of machines.", "az": "Dişli ötürmələr, mexanizmlər, mexatronika, maşınların etibarlılığı və keyfiyyəti."}'::jsonb, 2),
  ('tool', '{"en": "Engineering technology", "az": "Maşınqayırma texnologiyası"}'::jsonb, '{"en": "Machining, grinding, CNC, assembly, and manufacturing accuracy.", "az": "Emal, cilalama, CNC, yığma və hazırlanma dəqiqliyi."}'::jsonb, 3),
  ('chip', '{"en": "Automation and ICT", "az": "Avtomatlaşdırma və İKT"}'::jsonb, '{"en": "Automatic control, diagnostics, digital twins, and applied artificial intelligence.", "az": "Avtomatik idarəetmə, diaqnostika, rəqəmsal əkizlər və tətbiqi süni intellekt."}'::jsonb, 4),
  ('leaf', '{"en": "Energy and Environment", "az": "Enerji və ətraf mühit"}'::jsonb, '{"en": "Energy efficiency and productivity of machines, green energy conversion.", "az": "Maşınların enerji səmərəliliyi və məhsuldarlığı, yaşıl enerji çevrilməsi."}'::jsonb, 5),
  ('trend', '{"en": "Economics and management", "az": "İqtisadiyyat və idarəetmə"}'::jsonb, '{"en": "Economics and management within mechanical engineering.", "az": "Maşınqayırmada iqtisadiyyat və idarəetmə məsələləri."}'::jsonb, 6);

-- author steps
INSERT INTO author_steps (step_no, title, body, sort_order) VALUES
  ('01', '{"en": "Prepare the manuscript", "az": "Əlyazmanı hazırlayın"}'::jsonb, '{"en": "Articles are accepted in English only. Formulas are typed in standard Microsoft Equation editors, with main formulas numbered on the right.", "az": "Məqalələr yalnız ingilis dilində qəbul edilir. Düsturlar standart Microsoft Equation redaktorunda yığılır, əsas düsturlar sağ tərəfdən nömrələnir."}'::jsonb, 0),
  ('02', '{"en": "Attach author details", "az": "Müəllif məlumatlarını əlavə edin"}'::jsonb, '{"en": "Authors attach a photograph and a short biographical note — place of work, employer’s address, and position.", "az": "Müəlliflər fotoşəkil və qısa bioqrafik qeyd — iş yeri, işəgötürənin ünvanı və vəzifə — təqdim edirlər."}'::jsonb, 1),
  ('03', '{"en": "Peer review", "az": "Rəyləndirmə"}'::jsonb, '{"en": "Every manuscript is reviewed for scientific quality. Reviewers meet review deadlines and keep submissions confidential.", "az": "Hər bir əlyazma elmi keyfiyyət baxımından rəyləndirilir. Rəyçilər son tarixlərə əməl edir və məlumatları məxfi saxlayır."}'::jsonb, 2),
  ('04', '{"en": "Publication", "az": "Nəşr"}'::jsonb, '{"en": "Accepted articles are assigned a DOI under prefix 10.61413 and published open access, free of charge.", "az": "Qəbul olunmuş məqalələrə 10.61413 prefiksi ilə DOI verilir və açıq girişlə, pulsuz dərc olunur."}'::jsonb, 3);

-- author terms
INSERT INTO author_terms (title, body, sort_order) VALUES
  ('{"en": "No charge to authors", "az": "Müəlliflər üçün ödənişsiz"}'::jsonb, '{"en": "Publication in “Machine Science” is free of charge. There are no submission or article-processing fees.", "az": "“Machine Science”də nəşr pulsuzdur. Təqdimat və ya məqalə emalı haqqı yoxdur."}'::jsonb, 0),
  ('{"en": "Open access", "az": "Açıq giriş"}'::jsonb, '{"en": "Published under Creative Commons licences. Open access questions are answered at msj@aztu.edu.az.", "az": "Creative Commons lisenziyaları ilə nəşr olunur. Açıq girişlə bağlı suallar msj@aztu.edu.az ünvanına yazılır."}'::jsonb, 1),
  ('{"en": "Preprints permitted", "az": "Preprintlərə icazə verilir"}'::jsonb, '{"en": "A preprint may be posted anywhere at any time, including before submission to the journal.", "az": "Preprint istənilən vaxt, jurnala təqdim edilməzdən əvvəl də, istənilən yerdə yerləşdirilə bilər."}'::jsonb, 2),
  ('{"en": "Publication ethics", "az": "Nəşr etikası"}'::jsonb, '{"en": "Our ethics and malpractice statement follows the guidelines of the Committee on Publication Ethics (COPE).", "az": "Etika bəyanatımız Nəşr Etikası Komitəsinin (COPE) təlimatlarına əsaslanır."}'::jsonb, 3),
  ('{"en": "AI policy", "az": "Süni intellekt siyasəti"}'::jsonb, '{"en": "Generative AI may assist with language only. Authors and reviewers remain responsible for accuracy and integrity.", "az": "Generativ süni intellekt yalnız dilin təkmilləşdirilməsinə kömək edə bilər. Dəqiqlik və bütövlüyə görə müəlliflər və rəyçilər cavabdehdir."}'::jsonb, 4);

-- journal record plate + ticker
UPDATE journal_settings SET record = '{"en": [["Print ISSN", "2227-6912"], ["E-ISSN", "2790-0479"], ["Established", "2001"], ["Publisher", "AzTU"], ["Frequency", "2 × / year"], ["Language", "English"], ["Indexing", "INSPEC · HAC"], ["Access", "Open (CC)"], ["DOI prefix", "10.61413"], ["Fees", "None"], ["Contact", "<a href=\"mailto:msj@aztu.edu.az\">msj@aztu.edu.az</a>"]], "az": [["Çap ISSN", "2227-6912"], ["E-ISSN", "2790-0479"], ["Təsis edilib", "2001"], ["Naşir", "AzTU"], ["Tezlik", "2 × / il"], ["Dil", "İngilis"], ["İndeksləşmə", "INSPEC · AAK"], ["Giriş", "Açıq (CC)"], ["DOI prefiksi", "10.61413"], ["Haqq", "Yoxdur"], ["Əlaqə", "<a href=\"mailto:msj@aztu.edu.az\">msj@aztu.edu.az</a>"]]}'::jsonb, ticker = '{"en": [["Print ISSN", "2227-6912"], ["E-ISSN", "2790-0479"], ["Indexed in", "INSPEC since 2011"], ["Listed by", "HAC Azerbaijan"], ["Publication fee", "None"], ["Language", "English"], ["Peer review", "Double-blind"], ["Access", "Open · Creative Commons"], ["Preprints", "Permitted"], ["DOI prefix", "10.61413"], ["Published by", "AzTU, Baku"]], "az": [["Çap ISSN", "2227-6912"], ["E-ISSN", "2790-0479"], ["İndeksləşdirilib", "2011-ci ildən INSPEC"], ["Siyahıda", "AAK Azərbaycan"], ["Nəşr haqqı", "Yoxdur"], ["Dil", "İngilis"], ["Rəyləndirmə", "İkiqat anonim"], ["Giriş", "Açıq · Creative Commons"], ["Preprint", "İcazə verilir"], ["DOI prefiksi", "10.61413"], ["Naşir", "AzTU, Bakı"]]}'::jsonb WHERE id = 1;
