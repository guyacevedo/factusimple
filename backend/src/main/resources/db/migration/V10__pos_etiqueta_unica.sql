-- #############################################################################
-- V10. POS: one label per held sale
-- #############################################################################
-- The cashier picks a held sale by its label ("Mesa 3", "Señora del vestido
-- rojo"), so two with the same name in the same establishment are ambiguous.
CREATE UNIQUE INDEX uk_held_sale_user_label ON held_sales(user_id, LOWER(label));
