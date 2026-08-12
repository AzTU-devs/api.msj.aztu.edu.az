-- =====================================================================
--  Reference / initial data (ships with the app; safe for production)
-- =====================================================================

-- Roles ----------------------------------------------------------------
INSERT INTO roles (code, name, description) VALUES
  ('ADMIN',            'Administrator',   'Full platform administration'),
  ('EDITOR_IN_CHIEF',  'Editor-in-Chief', 'Oversees the whole editorial process'),
  ('EDITOR',           'Editor',          'Handles assigned submissions and reviews'),
  ('REVIEWER',         'Reviewer',        'Performs peer review of assigned articles'),
  ('AUTHOR',           'Author',          'Submits and manages own manuscripts');

-- Journal settings (singleton) ----------------------------------------
INSERT INTO journal_settings
  (id, journal_title, tagline, about, issn_print, issn_online, doi_prefix, publisher,
   email, phone, indexed_in, publication_fee)
VALUES
  (1,
   '{"az":"Machine Science","en":"Machine Science","ru":"Machine Science"}',
   '{"en":"International scientific and technical journal","az":"Beynəlxalq elmi-texniki jurnal","ru":"Международный научно-технический журнал"}',
   '{"en":"International scientific and technical journal MACHINE SCIENCE is published by Azerbaijan Technical University. Original research papers and reviews are accepted for publication; all manuscripts are peer-reviewed for scientific quality."}',
   '2227-6912', '2790-0479', '10.61413', 'Azerbaijan Technical University',
   'msj@aztu.edu.az', '(+994 12) 539 12 25',
   '["INSPEC","HAC Azerbaijan"]', 'Free of charge');

-- Subject areas are stored on articles.subject_area; canonical list lives in app config.

-- Content pages (multilingual JSONB) ----------------------------------
INSERT INTO content_pages (slug, title, body, sort_order) VALUES
  ('about',
   '{"en":"About the Journal","az":"Jurnal haqqında","ru":"О журнале"}',
   '{"en":"Machine Science is an international scientific and technical journal published by Azerbaijan Technical University since 2001 (as Mechanics-Mechanical Engineering during 2001-2011). It publishes original research and reviews in mechanical engineering, accepted only in English, free of charge."}',
   1),
  ('scope',
   '{"en":"Aims & Scope","az":"Məqsəd və əhatə dairəsi","ru":"Цели и охват"}',
   '{"en":"Machine design; Materials science and metallurgy; Automation and ICT; Energy and environment; Economics and management in mechanical engineering."}',
   2),
  ('author-guidelines',
   '{"en":"Preparation of Manuscript","az":"Məqalənin hazırlanması","ru":"Подготовка рукописи"}',
   '{"en":"Manuscripts are accepted in English. Formulas should be typed in a standard equation editor with main formulas numbered on the right. Authors should attach a photo and a short biographical note."}',
   3),
  ('ethics',
   '{"en":"Publication Ethics","az":"Nəşr etikası","ru":"Издательская этика"}',
   '{"en":"The statement on ethical standards and malpractice was developed based on the guidelines of the Committee on Publication Ethics (COPE)."}',
   4),
  ('open-access',
   '{"en":"Open Access Policy","az":"Açıq giriş siyasəti","ru":"Политика открытого доступа"}',
   '{"en":"This journal provides Gold Open Access under Creative Commons licences. Publication is free of charge for authors."}',
   5),
  ('contact',
   '{"en":"Contact","az":"Əlaqə","ru":"Контакты"}',
   '{"en":"Azerbaijan Technical University, Baku. Tel: (+994 12) 539 12 25. E-mail: msj@aztu.edu.az"}',
   6);

-- Issues (real archive 2021-2025) -------------------------------------
INSERT INTO issues (year, number, title, slug, status, published_at, sort_order) VALUES
  (2025, 2, 'Machine Science 2025 - Number II', 'machine-science-2025-2', 'PUBLISHED', '2025-12-01', 1),
  (2025, 1, 'Machine Science 2025 - Number I',  'machine-science-2025-1', 'PUBLISHED', '2025-06-01', 2),
  (2024, 2, 'Machine Science 2024 - Number II', 'machine-science-2024-2', 'PUBLISHED', '2024-12-01', 3),
  (2024, 1, 'Machine Science 2024 - Number I',  'machine-science-2024-1', 'PUBLISHED', '2024-06-01', 4),
  (2023, 2, 'Machine Science 2023 - Number II', 'machine-science-2023-2', 'PUBLISHED', '2023-12-01', 5),
  (2023, 1, 'Machine Science 2023 - Number I',  'machine-science-2023-1', 'PUBLISHED', '2023-06-01', 6),
  (2022, 2, 'Machine Science 2022 - Number II', 'machine-science-2022-2', 'PUBLISHED', '2022-12-01', 7),
  (2022, 1, 'Machine Science 2022 - Number I',  'machine-science-2022-1', 'PUBLISHED', '2022-06-01', 8),
  (2021, 1, 'Machine Science 2021 - Number I',  'machine-science-2021-1', 'PUBLISHED', '2021-06-01', 9);

-- Editorial board (24 real members with portraits + ORCID/Scopus/email)
INSERT INTO board_members (full_name, title, section, photo_url, orcid_url, scopus_url, email, sort_order) VALUES
('Prof. Dr. Isa Khalilov', 'Head of Department of Machine Design, Mechatronics and Industrial Technologies, Azerbaijan Technical University', 'EDITOR_IN_CHIEF', '/media/board/prof-dr-isa-khalilov.webp', 'https://orcid.org/0000-0001-5026-5742', 'https://www.scopus.com/authid/detail.uri?authorId=36020491400', 'khalilov@aztu.edu.az', 1),
('Prof. Dr.-Ing. Eckart Schnack', 'Distinguished Mechanical Engineer, Karlsruhe, Germany', 'HONORARY', '/media/board/prof-dr-ing-eckart-schnack.webp', 'https://orcid.org/0000-0001-7189-1945', 'https://www.scopus.com/authid/detail.uri?authorId=7005867715', 'eckart.schnack@mach.uni-karlsruhe.de', 2),
('Prof. Dr.-Ing. Albert Albers', 'Head of Institut für Produktentwicklung (IPEK), Karlsruhe Institute of Technology (KIT)', 'BOARD', '/media/board/prof-dr-ing-albert-albers.webp', 'https://orcid.org/0000-0001-5432-704X', 'https://www.scopus.com/authid/detail.uri?authorId=7006029820', 'albert.albers@ipek.kit.edu', 3),
('Prof. Subhan Namazov', 'Vice-rector for science and innovations - doctor of technical sciences, professor', 'BOARD', '/media/board/prof-subhan-namazov.webp', 'https://orcid.org/0009-0002-6884-0842', 'https://www.scopus.com/authid/detail.uri?authorId=8269164400', 'subhan.namazov@aztu.edu.az', 4),
('Prof. Dr.-Ing. Rasim Ismayil Alizade', NULL, 'BOARD', '/media/board/prof-dr-ing-rasim-ismayil-alizade.webp', 'https://orcid.org/0009-0003-4602-8770', 'https://www.scopus.com/authid/detail.uri?authorId=6701555356', 'rasima@aztu.edu.az', 5),
('Prof. Dr.-Ing. Dmitry V. Ardashev', 'Associate Professor and Doctor of Technical Sciences, South Ural State University, Chelyabinsk, Russia', 'BOARD', '/media/board/prof-dr-ing-dmitry-v-ardashev.webp', 'https://orcid.org/0000-0002-8134-2525', 'https://www.scopus.com/authid/detail.uri?authorId=36464934000', 'ardashevdv@susu.ru', 6),
('Prof. Dr. Abdullah H. Avey (Sofiyev)', 'Distinguished Scientist in Mechanics and Materials Science, Turkey', 'BOARD', '/media/board/prof-dr-abdullah-h-avey-sofiyev.webp', 'https://orcid.org/0000-0001-7678-6351', 'https://www.scopus.com/authid/detail.uri?authorId=6603803044', 'aavey@ticaret.edu.tr', 7),
('Prof. Dr. Sci. Nguyen Dinh Duc', 'Leading Scientist in Composite and Advanced Materials, Vietnam National University, Hanoi', 'BOARD', '/media/board/prof-dr-sci-nguyen-dinh-duc.webp', 'https://orcid.org/0000-0003-2656-7497', 'https://www.scopus.com/authid/detail.uri?authorId=55178688800', 'ducnd@vnu.edu.vn', 8),
('Prof. Dr. Tech. Sci. Alexander Anatolyevich Dyakonov', 'Rector, Almetyevsk State Oil Institute, Republic of Tatarstan, Russia', 'BOARD', '/media/board/prof-dr-tech-sci-alexander-anatolyevich-dyakonov.webp', 'https://orcid.org/0000-0001-8384-6359', 'https://www.scopus.com/authid/detail.uri?authorId=57210568937', 'dyakonovaa@agni-rt.ru', 9),
('Prof. Dr. Eng. Dr. Phil. Sc. Viktor Arkad’evich Glazunov', 'Researcher, Mechanical Engineering Research Institute (IMASH RAS), Moscow, Russia', 'BOARD', '/media/board/prof-dr-eng-dr-phil-sc-viktor-arkad-evich-glazunov.webp', 'https://orcid.org/0000-0002-4802-0217', 'https://www.scopus.com/authid/detail.uri?authorId=7005914361', 'vaglznv@mail.ru', 10),
('Prof. Dr. Mustafa Güden', 'Faculty Member, Mechanical Engineering Department, İzmir Institute of Technology, Turkey', 'BOARD', '/media/board/prof-dr-mustafa-g-den.webp', 'https://orcid.org/0000-0002-4802-0217', 'https://www.scopus.com/authid/detail.uri?authorId=7004589851', 'mustafaguden@iyte.edu.tr', 11),
('Prof. Dr. Eng. Andrey Vladimirovich Keller', 'Head of the Department of Land Vehicles, Moscow Polytechnic University, Russia', 'BOARD', '/media/board/prof-dr-eng-andrey-vladimirovich-keller.webp', 'https://orcid.org/0000-0003-4183-9489', 'https://www.scopus.com/authid/detail.uri?authorId=57189699452', 'andrey.keller@nami.ru', 12),
('Prof. Dr. Eng. Mikhail E. Lustenkov', 'Rector, Belarusian-Russian University, Mogilev, Belarus', 'BOARD', '/media/board/prof-dr-eng-mikhail-e-lustenkov.webp', 'https://orcid.org/0000-0002-4912-3824', 'https://www.scopus.com/authid/detail.uri?authorId=36545142000', NULL, 13),
('Professor Vagif Movlazadeh', 'Department of Machine-Building Technology, Azerbaijan Technical University', 'BOARD', '/media/board/professor-vagif-movlazadeh.webp', 'https://orcid.org/0009-0008-7256-1903', NULL, 'movlazade.vaqif@aztu.edu.az', 14),
('Professor Dr. Mehmet Hakkı Omurtag', 'Faculty of Engineering and Natural Sciences, Istanbul Medipol University', 'BOARD', '/media/board/professor-dr-mehmet-hakk-omurtag.webp', 'https://orcid.org/0000-0003-2669-6459', 'https://www.scopus.com/authid/detail.uri?authorId=6602741488', 'mhomurtag@medipol.edu.tr', 15),
('Bernard Roth', 'Professor Emeritus of Mechanical Engineering, Stanford University', 'BOARD', '/media/board/bernard-roth.webp', NULL, 'https://www.scopus.com/authid/detail.uri?authorId=57194763685', 'broth@stanford.edu', 16),
('Professor Mohammad Shariyat', 'Department of Mechanical Engineering, K. N. Toosi University of Technology, Tehran, Iran', 'BOARD', '/media/board/professor-mohammad-shariyat.webp', 'https://orcid.org/0000-0002-1088-7333', 'https://www.scopus.com/authid/detail.uri?authorId=6603477403', 'm_shariyat@yahoo.com', 17),
('Professor Hui-Shen Shen', 'Full Professor, Shanghai Jiao Tong University (SJTU), China', 'BOARD', '/media/board/professor-hui-shen-shen.webp', 'https://orcid.org/0000-0002-5240-9284', 'https://www.scopus.com/authid/detail.uri?authorId=57218664104', 'hsshen@sjtu.edu.cn', 18),
('Professor Dr.-Ing. Karsten Stahl', 'Full Professor, Institute of Machine Elements & Director of Gear Research Center (FZG), Technical University of Munich (TUM), Germany', 'BOARD', '/media/board/professor-dr-ing-karsten-stahl.webp', 'https://orcid.org/0000-0001-7177-5207', 'https://www.scopus.com/authid/detail.uri?authorId=28367988400', 'karsten.stahl@tum.de', 19),
('Professor Dr. Andrzej Urbaniec', 'Assistant Professor, Oil and Gas Institute – National Research Institute, Kraków, Poland', 'BOARD', '/media/board/professor-dr-andrzej-urbaniec.webp', 'https://orcid.org/0000-0002-4717-1806', 'https://www.scopus.com/authid/detail.uri?authorId=35613988000', 'urbaniec@inig.pl', 20),
('Dr. Hakan Yavash', 'Materials Science Researcher, Turkish Aerospace', 'BOARD', '/media/board/dr-hakan-yavash.webp', 'https://orcid.org/0000-0002-3120-2436', 'https://www.scopus.com/authid/detail.uri?authorId=57204427472', 'hakan.yavas@tai.com.tr', 21),
('Prof. Nizami Yusubov', 'Full Professor, Department of Machine-Building Technology, Azerbaijan Technical University', 'BOARD', '/media/board/prof-nizami-yusubov.webp', 'https://orcid.org/0000-0002-6009-9909', 'https://www.scopus.com/authid/detail.uri?authorId=26636765900', 'nizami.yusubov@aztu.edu.az', 22),
('Prof. Beyali Ahmedov', 'Researcher and Academic, Azerbaijan Technical University, Baku', 'BOARD', '/media/board/prof-beyali-ahmedov.webp', 'https://orcid.org/0000-0001-5022-8757', 'https://www.scopus.com/authid/detail.uri?authorId=57204834804', 'ahmedov.beyali@aztu.edu.az', 23),
('Anar Hajiyev', 'Senior Lecturer & Deputy Dean, Faculty of Machine-Building and Metallurgy, Azerbaijan Technical University', 'BOARD', '/media/board/anar-hajiyev.webp', 'https://orcid.org/0000-0003-0636-9397', 'https://www.scopus.com/authid/detail.uri?authorId=57311812000', 'anar.hajiyev@aztu.edu.az', 24);
