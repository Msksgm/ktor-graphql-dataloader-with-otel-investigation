CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    user_id INT NOT NULL
);

-- Generate 100 users
INSERT INTO users (name, age)
SELECT
    'User_' || i,
    20 + (i % 30)
FROM generate_series(1, 100) AS s(i);

-- Generate ~700 books (7 per user on average, varying between 3 and 11)
INSERT INTO books (name, user_id)
SELECT
    'Book_' || u.i || '_' || b.j,
    u.i
FROM generate_series(1, 100) AS u(i),
     generate_series(1, 11) AS b(j)
WHERE b.j <= 3 + (u.i * 7 + b.j * 13) % 9;
