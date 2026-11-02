-- =====================================================================
-- Dados iniciais da biblioteca (H2 em memória)
-- Executado automaticamente depois que o Hibernate cria o schema
-- (spring.sql.init.mode=always + spring.jpa.defer-datasource-initialization=true).
-- Os ids são gerados pelo banco (IDENTITY) e as chaves estrangeiras são
-- resolvidas por subselect, então a ordem dos INSERTs define os ids.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Autores (os 8 primeiros são os mesmos do mock do frontend)
-- Regra: todo autor possui pelo menos 1 livro cadastrado.
-- ---------------------------------------------------------------------
INSERT INTO authors (name) VALUES ('Machado de Assis');
INSERT INTO authors (name) VALUES ('Clarice Lispector');
INSERT INTO authors (name) VALUES ('Jorge Amado');
INSERT INTO authors (name) VALUES ('J.R.R. Tolkien');
INSERT INTO authors (name) VALUES ('George Orwell');
INSERT INTO authors (name) VALUES ('Gabriel García Márquez');
INSERT INTO authors (name) VALUES ('Isaac Asimov');
INSERT INTO authors (name) VALUES ('Agatha Christie');
INSERT INTO authors (name) VALUES ('Graciliano Ramos');
INSERT INTO authors (name) VALUES ('Aldous Huxley');
INSERT INTO authors (name) VALUES ('Mary Shelley');
INSERT INTO authors (name) VALUES ('Cecília Meireles');

-- ---------------------------------------------------------------------
-- Gêneros (os 7 primeiros são os mesmos do mock do frontend)
-- ---------------------------------------------------------------------
INSERT INTO genres (name) VALUES ('Romance');
INSERT INTO genres (name) VALUES ('Ficção Científica');
INSERT INTO genres (name) VALUES ('Literatura Brasileira');
INSERT INTO genres (name) VALUES ('Fantasia');
INSERT INTO genres (name) VALUES ('Distopia');
INSERT INTO genres (name) VALUES ('Mistério e Suspense');
INSERT INTO genres (name) VALUES ('Realismo Mágico');
INSERT INTO genres (name) VALUES ('Terror');
INSERT INTO genres (name) VALUES ('Poesia');

-- ---------------------------------------------------------------------
-- Livros (os 10 primeiros são os mesmos do mock do frontend)
-- ---------------------------------------------------------------------
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Dom Casmurro',
     (SELECT id FROM authors WHERE name = 'Machado de Assis'),
     (SELECT id FROM genres WHERE name = 'Literatura Brasileira'),
     DATE '1899-01-01', 'Livraria Garnier');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('A Hora da Estrela',
     (SELECT id FROM authors WHERE name = 'Clarice Lispector'),
     (SELECT id FROM genres WHERE name = 'Romance'),
     DATE '1977-10-26', 'Livraria José Olympio Editora');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Capitães da Areia',
     (SELECT id FROM authors WHERE name = 'Jorge Amado'),
     (SELECT id FROM genres WHERE name = 'Literatura Brasileira'),
     DATE '1937-01-01', 'Editora José Olympio');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('O Hobbit',
     (SELECT id FROM authors WHERE name = 'J.R.R. Tolkien'),
     (SELECT id FROM genres WHERE name = 'Fantasia'),
     DATE '1937-09-21', 'HarperCollins');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('1984',
     (SELECT id FROM authors WHERE name = 'George Orwell'),
     (SELECT id FROM genres WHERE name = 'Distopia'),
     DATE '1949-06-08', 'Companhia das Letras');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Cem Anos de Solidão',
     (SELECT id FROM authors WHERE name = 'Gabriel García Márquez'),
     (SELECT id FROM genres WHERE name = 'Realismo Mágico'),
     DATE '1967-05-30', 'Editorial Sudamericana');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Fundação',
     (SELECT id FROM authors WHERE name = 'Isaac Asimov'),
     (SELECT id FROM genres WHERE name = 'Ficção Científica'),
     DATE '1951-06-01', 'Gnome Press');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('E Não Sobrou Nenhum',
     (SELECT id FROM authors WHERE name = 'Agatha Christie'),
     (SELECT id FROM genres WHERE name = 'Mistério e Suspense'),
     DATE '1939-11-06', 'Globo Livros');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Memórias Póstumas de Brás Cubas',
     (SELECT id FROM authors WHERE name = 'Machado de Assis'),
     (SELECT id FROM genres WHERE name = 'Literatura Brasileira'),
     DATE '1881-01-01', 'Tipografia Nacional');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('A Revolução dos Bichos',
     (SELECT id FROM authors WHERE name = 'George Orwell'),
     (SELECT id FROM genres WHERE name = 'Distopia'),
     DATE '1945-08-17', 'Secker and Warburg');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Vidas Secas',
     (SELECT id FROM authors WHERE name = 'Graciliano Ramos'),
     (SELECT id FROM genres WHERE name = 'Literatura Brasileira'),
     DATE '1938-01-01', 'Livraria José Olympio Editora');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('São Bernardo',
     (SELECT id FROM authors WHERE name = 'Graciliano Ramos'),
     (SELECT id FROM genres WHERE name = 'Literatura Brasileira'),
     DATE '1934-01-01', 'Ariel Editora');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Admirável Mundo Novo',
     (SELECT id FROM authors WHERE name = 'Aldous Huxley'),
     (SELECT id FROM genres WHERE name = 'Distopia'),
     DATE '1932-01-01', 'Chatto & Windus');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Frankenstein',
     (SELECT id FROM authors WHERE name = 'Mary Shelley'),
     (SELECT id FROM genres WHERE name = 'Terror'),
     DATE '1818-01-01', 'Lackington, Hughes, Harding, Mavor & Jones');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Romanceiro da Inconfidência',
     (SELECT id FROM authors WHERE name = 'Cecília Meireles'),
     (SELECT id FROM genres WHERE name = 'Poesia'),
     DATE '1953-01-01', 'Livros de Portugal');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('O Senhor dos Anéis: A Sociedade do Anel',
     (SELECT id FROM authors WHERE name = 'J.R.R. Tolkien'),
     (SELECT id FROM genres WHERE name = 'Fantasia'),
     DATE '1954-07-29', 'Allen & Unwin');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Eu, Robô',
     (SELECT id FROM authors WHERE name = 'Isaac Asimov'),
     (SELECT id FROM genres WHERE name = 'Ficção Científica'),
     DATE '1950-12-02', 'Gnome Press');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Assassinato no Expresso do Oriente',
     (SELECT id FROM authors WHERE name = 'Agatha Christie'),
     (SELECT id FROM genres WHERE name = 'Mistério e Suspense'),
     DATE '1934-01-01', 'Collins Crime Club');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('Gabriela, Cravo e Canela',
     (SELECT id FROM authors WHERE name = 'Jorge Amado'),
     (SELECT id FROM genres WHERE name = 'Romance'),
     DATE '1958-01-01', 'Livraria Martins Editora');
INSERT INTO books (title, author_id, genre_id, publish_date, publisher) VALUES
    ('A Paixão Segundo G.H.',
     (SELECT id FROM authors WHERE name = 'Clarice Lispector'),
     (SELECT id FROM genres WHERE name = 'Romance'),
     DATE '1964-01-01', 'Editora do Autor');

-- ---------------------------------------------------------------------
-- Usuários (os 8 primeiros são os mesmos do mock do frontend)
-- CPF gravado somente com dígitos (11 caracteres).
-- ---------------------------------------------------------------------
INSERT INTO users (name, cpf) VALUES ('Ana Souza', '12345678901');
INSERT INTO users (name, cpf) VALUES ('Bruno Oliveira', '23456789012');
INSERT INTO users (name, cpf) VALUES ('Carla Mendes', '34567890123');
INSERT INTO users (name, cpf) VALUES ('Diego Ferreira', '45678901234');
INSERT INTO users (name, cpf) VALUES ('Elena Rostova', '56789012345');
INSERT INTO users (name, cpf) VALUES ('Fernando Silva', '67890123456');
INSERT INTO users (name, cpf) VALUES ('Gabriela Lima', '78901234567');
INSERT INTO users (name, cpf) VALUES ('Heitor Costa', '89012345678');
INSERT INTO users (name, cpf) VALUES ('Isabela Rocha', '90123456789');
INSERT INTO users (name, cpf) VALUES ('João Pereira', '01234567890');

-- ---------------------------------------------------------------------
-- Empréstimos
-- As datas são relativas ao dia em que a aplicação sobe (CURRENT_DATE),
-- para que sempre existam empréstimos nos três status:
--   ATIVO     -> return_date nulo e end_date >= hoje
--   ATRASADO  -> return_date nulo e end_date < hoje
--   CONCLUIDO -> return_date preenchido
-- Regras respeitadas:
--   * nenhum livro possui dois empréstimos em aberto ao mesmo tempo;
--   * os únicos usuários com empréstimo ATRASADO (com pendência) são
--     Bruno Oliveira, Elena Rostova e Heitor Costa, como no mock;
--   * usuários com pendência também têm histórico CONCLUIDO.
-- ---------------------------------------------------------------------

-- Os 7 primeiros correspondem aos empréstimos do mock do frontend
-- 1. ATIVO: Ana Souza - Dom Casmurro
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -3, CURRENT_DATE), DATEADD('DAY', 11, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '12345678901'),
     (SELECT id FROM books WHERE title = 'Dom Casmurro'));

-- 2. CONCLUIDO: Bruno Oliveira - A Hora da Estrela
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -60, CURRENT_DATE), DATEADD('DAY', -46, CURRENT_DATE), DATEADD('DAY', -50, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '23456789012'),
     (SELECT id FROM books WHERE title = 'A Hora da Estrela'));

-- 3. ATIVO: Carla Mendes - Capitães da Areia
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -6, CURRENT_DATE), DATEADD('DAY', 8, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '34567890123'),
     (SELECT id FROM books WHERE title = 'Capitães da Areia'));

-- 4. ATRASADO: Elena Rostova - 1984
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -30, CURRENT_DATE), DATEADD('DAY', -16, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '56789012345'),
     (SELECT id FROM books WHERE title = '1984'));

-- 5. ATIVO: Fernando Silva - O Hobbit
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -1, CURRENT_DATE), DATEADD('DAY', 13, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '67890123456'),
     (SELECT id FROM books WHERE title = 'O Hobbit'));

-- 6. ATRASADO: Heitor Costa - E Não Sobrou Nenhum
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -25, CURRENT_DATE), DATEADD('DAY', -11, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '89012345678'),
     (SELECT id FROM books WHERE title = 'E Não Sobrou Nenhum'));

-- 7. CONCLUIDO: Diego Ferreira - Fundação
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -90, CURRENT_DATE), DATEADD('DAY', -76, CURRENT_DATE), DATEADD('DAY', -78, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '45678901234'),
     (SELECT id FROM books WHERE title = 'Fundação'));

-- Empréstimos adicionais
-- 8. ATRASADO: Bruno Oliveira - Cem Anos de Solidão
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -20, CURRENT_DATE), DATEADD('DAY', -6, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '23456789012'),
     (SELECT id FROM books WHERE title = 'Cem Anos de Solidão'));

-- 9. CONCLUIDO: Elena Rostova - Memórias Póstumas de Brás Cubas
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -75, CURRENT_DATE), DATEADD('DAY', -61, CURRENT_DATE), DATEADD('DAY', -62, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '56789012345'),
     (SELECT id FROM books WHERE title = 'Memórias Póstumas de Brás Cubas'));

-- 10. CONCLUIDO: Heitor Costa - Vidas Secas
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -45, CURRENT_DATE), DATEADD('DAY', -31, CURRENT_DATE), DATEADD('DAY', -33, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '89012345678'),
     (SELECT id FROM books WHERE title = 'Vidas Secas'));

-- 11. ATIVO: Gabriela Lima - Admirável Mundo Novo
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (CURRENT_DATE, DATEADD('DAY', 14, CURRENT_DATE), NULL,
     (SELECT id FROM users WHERE cpf = '78901234567'),
     (SELECT id FROM books WHERE title = 'Admirável Mundo Novo'));

-- 12. CONCLUIDO: Ana Souza - A Revolução dos Bichos
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -40, CURRENT_DATE), DATEADD('DAY', -26, CURRENT_DATE), DATEADD('DAY', -28, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '12345678901'),
     (SELECT id FROM books WHERE title = 'A Revolução dos Bichos'));

-- 13. CONCLUIDO (devolvido com atraso, mas já sem pendência): Diego Ferreira - Frankenstein
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -35, CURRENT_DATE), DATEADD('DAY', -21, CURRENT_DATE), DATEADD('DAY', -15, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '45678901234'),
     (SELECT id FROM books WHERE title = 'Frankenstein'));

-- 14. CONCLUIDO (histórico de um livro que hoje está emprestado): Gabriela Lima - Dom Casmurro
INSERT INTO lendings (start_date, end_date, return_date, user_id, book_id) VALUES
    (DATEADD('DAY', -70, CURRENT_DATE), DATEADD('DAY', -56, CURRENT_DATE), DATEADD('DAY', -57, CURRENT_DATE),
     (SELECT id FROM users WHERE cpf = '78901234567'),
     (SELECT id FROM books WHERE title = 'Dom Casmurro'));
