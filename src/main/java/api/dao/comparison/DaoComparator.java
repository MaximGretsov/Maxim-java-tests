package api.dao.comparison;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;

public class DaoComparator {

    private final DaoComparisonConfigLoader configLoader;

    public DaoComparator() {
        this.configLoader =
                new DaoComparisonConfigLoader("dao-comparison.properties");
    }

    public void compare(Object apiResponse, Object dao) {
        DaoComparisonConfigLoader.DaoComparisonRule rule =
                configLoader.getRuleFor(apiResponse.getClass());

        if (rule == null) {
            throw new RuntimeException(
                    "No comparison rule found for "
                            + apiResponse.getClass().getSimpleName()
            );
        }

        Map<String, String> fieldMappings =
                rule.getFieldMappings();

        for (Map.Entry<String, String> mapping
                : fieldMappings.entrySet()) {

            String apiFieldName = mapping.getKey();
            String daoFieldName = mapping.getValue();

            Object apiValue =
                    getFieldValue(apiResponse, apiFieldName);

            Object daoValue =
                    getFieldValue(dao, daoFieldName);

            if (!valuesAreEqual(apiValue, daoValue)) {
                throw new AssertionError(
                        String.format(
                                "Field mismatch for %s: API=%s, DAO=%s",
                                apiFieldName,
                                apiValue,
                                daoValue
                        )
                );
            }
        }
    }

    private boolean valuesAreEqual(
            Object apiValue,
            Object daoValue
    ) {
        if (apiValue instanceof Number apiNumber
                && daoValue instanceof Number daoNumber) {

            BigDecimal apiDecimal =
                    new BigDecimal(apiNumber.toString());

            BigDecimal daoDecimal =
                    new BigDecimal(daoNumber.toString());

            return apiDecimal.compareTo(daoDecimal) == 0;
        }

        return (apiValue == daoValue)
                || (apiValue != null
                && apiValue.equals(daoValue));
    }

    private Object getFieldValue(
            Object obj,
            String fieldName
    ) {
        try {
            Field field =
                    obj.getClass().getDeclaredField(fieldName);

            field.setAccessible(true);

            return field.get(obj);

        } catch (NoSuchFieldException
                 | IllegalAccessException e) {

            throw new RuntimeException(
                    "Failed to get field value: " + fieldName,
                    e
            );
        }
    }
}