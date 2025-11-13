INSERT INTO moraTravelDaily.user
(first_name, last_name, second_last_name, email, password, role)
VALUES
-- CLIENTES
('Patricia Natividad', 'Cántaro', 'Márquez', 'patricia.cantaro@cliente', 'cliente', 'CLIENT'),
('Bryan Smith', 'Valdiviezo', 'Jiménez', 'bryan.valdiviezo@cliente', 'cliente', 'CLIENT'),
('Alonso Sebastian', 'Berrospi', 'Castillo', 'alonso.berrospi@cliente', 'cliente', 'CLIENT'),
('Rómulo Guillermo', 'Durán', 'Castañeda', 'romulo.duran@cliente', 'cliente', 'CLIENT'),

-- ADMIN
('Admin', 'User', NULL, 'admin@admin', 'admin', 'ADMIN'),

-- OPERARIO
('Operario', 'User', NULL, 'operario@operario', 'operario', 'OPERATOR');
