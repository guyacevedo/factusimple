-- =============================================================================
-- V7: datos iniciales — plan FREE por defecto y usuario ADMIN.
--
-- El correo y el hash de la contraseña del admin NO van en el repositorio:
-- llegan como placeholders de Flyway desde application*.properties
-- (variables ADMIN_EMAIL y ADMIN_PASSWORD_HASH). Si faltan, la migración
-- falla con un mensaje claro en lugar de crear un admin sin contraseña.
-- Una vez aplicada, esas variables ya no se usan y se pueden quitar.
-- =============================================================================

DO $check$
BEGIN
    IF '${admin-email}' = '' OR '${admin-password-hash}' NOT LIKE '$2_$%' THEN
        RAISE EXCEPTION 'V7: define ADMIN_EMAIL y ADMIN_PASSWORD_HASH (hash BCrypt, empieza por $2a$/$2b$) antes de migrar';
    END IF;
END
$check$;

-- Plan gratuito por defecto
INSERT INTO plans (id, name, max_products, max_customers, max_invoices,
                   required_invited_code, description, is_active)
VALUES ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'FREE', 20, 10, 10,
        FALSE, 'Plan gratuito por defecto: hasta 20 productos, 10 clientes y 10 facturas', TRUE)
ON CONFLICT (name) DO NOTHING;

-- Usuario ADMIN inicial: activo, pero obligado a cambiar la contraseña en el
-- primer ingreso (login responde PASSWORD_CHANGE_REQUIRED → POST /auth/activate).
-- nit y company_name son NOT NULL en users; para el admin son valores de relleno.
INSERT INTO users (id, plan_id, role, email, password,
                   first_name, last_name, nit, company_name,
                   is_active, must_change_password)
SELECT '00000000-0000-0000-0000-000000000001'::uuid, p.id, 'ADMIN',
       LOWER(TRIM('${admin-email}')), '${admin-password-hash}',
       'Admin', 'FactuSimple', '000000000', 'FactuSimple',
       TRUE, TRUE
FROM plans p
WHERE p.name = 'FREE'
ON CONFLICT ((LOWER(email))) DO NOTHING;