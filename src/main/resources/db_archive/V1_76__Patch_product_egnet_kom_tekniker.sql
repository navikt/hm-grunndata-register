UPDATE product_reg_v1
SET product_data = jsonb_set(
        product_data,
        '{attributes,egnetForKommunalTekniker}',
        'true'::jsonb
                   )
WHERE jsonb_array_length(product_data->'attributes'->'compatibleWith'->'productIds') > 0;