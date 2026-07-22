package generators;

import com.github.curiousoddman.rgxgen.RgxGen;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class RandomModelGenerator {

    private static final Random RANDOM = new Random();

    private static final int MAX_GENERATION_DEPTH = 3;
    private static final int COLLECTION_SIZE = 2;
    private static final int DEFAULT_STRING_LENGTH = 10;

    private static final String ALPHANUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "0123456789";

    /*
     * Запрещаем создание объекта utility-класса.
     */
    private RandomModelGenerator() {
    }

    /**
     * Создаёт объект переданного класса
     * и заполняет его поля случайными значениями.
     */
    public static <T> T generate(Class<T> modelClass) {
        if (modelClass == null) {
            throw new IllegalArgumentException(
                    "Класс модели не должен быть null"
            );
        }

        Object generatedObject = generateObject(
                modelClass,
                0,
                new HashSet<>()
        );

        return modelClass.cast(generatedObject);
    }

    /**
     * Создаёт экземпляр класса и заполняет его поля.
     */
    private static Object generateObject(
            Class<?> modelClass,
            int depth,
            Set<Class<?>> generationPath
    ) {
        if (depth > MAX_GENERATION_DEPTH) {
            return null;
        }

        /*
         * Защита от бесконечной рекурсии.
         *
         * Например:
         *
         * class User {
         *     private User parent;
         * }
         */
        if (generationPath.contains(modelClass)) {
            return null;
        }

        if (modelClass.isInterface()) {
            throw new IllegalArgumentException(
                    "Невозможно создать экземпляр интерфейса: "
                            + modelClass.getName()
            );
        }

        if (Modifier.isAbstract(modelClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "Невозможно создать экземпляр абстрактного класса: "
                            + modelClass.getName()
            );
        }

        generationPath.add(modelClass);

        try {
            Object instance = createInstance(modelClass);

            for (Field field : getAllFields(modelClass)) {
                if (shouldSkipField(field)) {
                    continue;
                }

                GeneratingRule generatingRule =
                        field.getAnnotation(GeneratingRule.class);

                Object generatedValue = generateValue(
                        field.getType(),
                        field.getGenericType(),
                        generatingRule,
                        depth + 1,
                        generationPath
                );

                field.setAccessible(true);
                field.set(instance, generatedValue);
            }

            return instance;
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(
                    "Не удалось заполнить поля класса: "
                            + modelClass.getName(),
                    exception
            );
        } finally {
            generationPath.remove(modelClass);
        }
    }

    /**
     * Генерирует значение в зависимости от типа поля.
     */
    private static Object generateValue(
            Class<?> fieldType,
            Type genericType,
            GeneratingRule generatingRule,
            int depth,
            Set<Class<?>> generationPath
    ) {
        /*
         * Если поле содержит @GeneratingRule,
         * значение создаётся на основании regex.
         */
        if (generatingRule != null) {
            return generateByRegex(
                    fieldType,
                    generatingRule.regex()
            );
        }

        if (fieldType == String.class) {
            return generateRandomString(DEFAULT_STRING_LENGTH);
        }

        if (fieldType == byte.class || fieldType == Byte.class) {
            return (byte) (RANDOM.nextInt(127) + 1);
        }

        if (fieldType == short.class || fieldType == Short.class) {
            return (short) (RANDOM.nextInt(10_000) + 1);
        }

        if (fieldType == int.class || fieldType == Integer.class) {
            return RANDOM.nextInt(10_000) + 1;
        }

        if (fieldType == long.class || fieldType == Long.class) {
            return RANDOM.nextLong(1, 100_001);
        }

        if (fieldType == float.class || fieldType == Float.class) {
            return RANDOM.nextFloat() * 10_000;
        }

        if (fieldType == double.class || fieldType == Double.class) {
            return RANDOM.nextDouble() * 10_000;
        }

        if (fieldType == boolean.class || fieldType == Boolean.class) {
            return RANDOM.nextBoolean();
        }

        if (fieldType == char.class || fieldType == Character.class) {
            return generateRandomCharacter();
        }

        if (fieldType == BigDecimal.class) {
            return BigDecimal
                    .valueOf(RANDOM.nextDouble() * 10_000)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (fieldType == BigInteger.class) {
            return BigInteger.valueOf(
                    RANDOM.nextLong(1, 100_001)
            );
        }

        if (fieldType == UUID.class) {
            return new UUID(
                    RANDOM.nextLong(),
                    RANDOM.nextLong()
            );
        }

        if (fieldType == LocalDate.class) {
            return LocalDate.now()
                    .minusDays(RANDOM.nextInt(3650));
        }

        if (fieldType == LocalDateTime.class) {
            return LocalDateTime.now()
                    .minusSeconds(RANDOM.nextInt(1_000_000));
        }

        if (fieldType == Instant.class) {
            return Instant.now()
                    .minusSeconds(RANDOM.nextInt(1_000_000));
        }

        if (fieldType == Date.class) {
            return Date.from(
                    Instant.now()
                            .minusSeconds(
                                    RANDOM.nextInt(1_000_000)
                            )
            );
        }

        if (fieldType.isEnum()) {
            return generateEnumValue(fieldType);
        }

        if (fieldType.isArray()) {
            return generateArray(
                    fieldType.getComponentType(),
                    depth,
                    generationPath
            );
        }

        if (Optional.class.isAssignableFrom(fieldType)) {
            return generateOptional(
                    genericType,
                    depth,
                    generationPath
            );
        }

        if (List.class.isAssignableFrom(fieldType)) {
            return generateList(
                    genericType,
                    depth,
                    generationPath
            );
        }

        if (Set.class.isAssignableFrom(fieldType)) {
            return generateSet(
                    genericType,
                    depth,
                    generationPath
            );
        }

        if (Map.class.isAssignableFrom(fieldType)) {
            return generateMap(
                    genericType,
                    depth,
                    generationPath
            );
        }

        if (Collection.class.isAssignableFrom(fieldType)) {
            return generateList(
                    genericType,
                    depth,
                    generationPath
            );
        }

        if (fieldType == Object.class) {
            return generateRandomString(DEFAULT_STRING_LENGTH);
        }

        /*
         * Если это пользовательский класс,
         * создаём вложенный объект.
         */
        return generateObject(
                fieldType,
                depth,
                generationPath
        );
    }

    /**
     * Создаёт значение по regex из @GeneratingRule.
     */
    private static Object generateByRegex(
            Class<?> fieldType,
            String regex
    ) {
        if (regex == null || regex.isBlank()) {
            throw new IllegalArgumentException(
                    "Regex в @GeneratingRule не должен быть пустым"
            );
        }

        String generatedValue;

        try {
            generatedValue = RgxGen
                    .parse(regex)
                    .generate(RANDOM);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Не удалось сгенерировать значение по regex: "
                            + regex,
                    exception
            );
        }

        /*
         * Дополнительно проверяем результат
         * стандартным Java Pattern.
         */
        if (!Pattern.matches(regex, generatedValue)) {
            throw new IllegalStateException(
                    "Сгенерированное значение не соответствует regex. "
                            + "Regex: " + regex
                            + ", значение: " + generatedValue
            );
        }

        return convertStringToType(
                generatedValue,
                fieldType
        );
    }

    /**
     * Преобразует строку, созданную по regex,
     * в тип поля.
     */
    private static Object convertStringToType(
            String value,
            Class<?> targetType
    ) {
        try {
            if (targetType == String.class) {
                return value;
            }

            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value);
            }

            if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value);
            }

            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value);
            }

            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value);
            }

            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value);
            }

            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value);
            }

            if (targetType == BigDecimal.class) {
                return new BigDecimal(value);
            }

            if (targetType == BigInteger.class) {
                return new BigInteger(value);
            }

            if (targetType == boolean.class || targetType == Boolean.class) {
                if (!value.equalsIgnoreCase("true")
                        && !value.equalsIgnoreCase("false")) {
                    throw new IllegalArgumentException(
                            "Regex для boolean должен генерировать "
                                    + "true или false"
                    );
                }

                return Boolean.parseBoolean(value);
            }

            if (targetType == char.class || targetType == Character.class) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException(
                            "Regex для char должен генерировать "
                                    + "ровно один символ"
                    );
                }

                return value.charAt(0);
            }

            if (targetType == UUID.class) {
                return UUID.fromString(value);
            }

            if (targetType == LocalDate.class) {
                return LocalDate.parse(value);
            }

            if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(value);
            }

            if (targetType == Instant.class) {
                return Instant.parse(value);
            }

            if (targetType.isEnum()) {
                return convertStringToEnum(
                        targetType,
                        value
                );
            }
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Значение '" + value
                            + "' невозможно преобразовать в тип "
                            + targetType.getSimpleName(),
                    exception
            );
        }

        throw new IllegalArgumentException(
                "@GeneratingRule нельзя использовать для типа: "
                        + targetType.getName()
        );
    }

    /**
     * Возвращает случайное значение enum.
     */
    private static Object generateEnumValue(Class<?> enumType) {
        Object[] enumConstants = enumType.getEnumConstants();

        if (enumConstants == null || enumConstants.length == 0) {
            throw new IllegalArgumentException(
                    "Enum не содержит значений: "
                            + enumType.getName()
            );
        }

        return enumConstants[
                RANDOM.nextInt(enumConstants.length)
                ];
    }

    /**
     * Преобразует строку в enum.
     */
    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private static Object convertStringToEnum(
            Class<?> enumType,
            String value
    ) {
        return Enum.valueOf(
                (Class<? extends Enum>) enumType,
                value
        );
    }

    /**
     * Создаёт массив случайных значений.
     */
    private static Object generateArray(
            Class<?> componentType,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Object array = Array.newInstance(
                componentType,
                COLLECTION_SIZE
        );

        for (int i = 0; i < COLLECTION_SIZE; i++) {
            Object generatedValue = generateValue(
                    componentType,
                    componentType,
                    null,
                    depth,
                    generationPath
            );

            Array.set(
                    array,
                    i,
                    generatedValue
            );
        }

        return array;
    }

    /**
     * Создаёт List.
     */
    private static List<Object> generateList(
            Type genericType,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Type elementType = getGenericArgument(
                genericType,
                0
        );

        List<Object> result = new ArrayList<>();

        for (int i = 0; i < COLLECTION_SIZE; i++) {
            result.add(
                    generateValueByType(
                            elementType,
                            depth,
                            generationPath
                    )
            );
        }

        return result;
    }

    /**
     * Создаёт Set.
     */
    private static Set<Object> generateSet(
            Type genericType,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Type elementType = getGenericArgument(
                genericType,
                0
        );

        Set<Object> result = new HashSet<>();

        for (int i = 0; i < COLLECTION_SIZE; i++) {
            result.add(
                    generateValueByType(
                            elementType,
                            depth,
                            generationPath
                    )
            );
        }

        return result;
    }

    /**
     * Создаёт Map.
     */
    private static Map<Object, Object> generateMap(
            Type genericType,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Type keyType = getGenericArgument(
                genericType,
                0
        );

        Type valueType = getGenericArgument(
                genericType,
                1
        );

        Map<Object, Object> result = new HashMap<>();

        for (int i = 0; i < COLLECTION_SIZE; i++) {
            Object key = generateValueByType(
                    keyType,
                    depth,
                    generationPath
            );

            Object value = generateValueByType(
                    valueType,
                    depth,
                    generationPath
            );

            result.put(key, value);
        }

        return result;
    }

    /**
     * Создаёт Optional.
     */
    private static Optional<Object> generateOptional(
            Type genericType,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Type elementType = getGenericArgument(
                genericType,
                0
        );

        Object generatedValue = generateValueByType(
                elementType,
                depth,
                generationPath
        );

        return Optional.ofNullable(generatedValue);
    }

    /**
     * Создаёт значение на основании Type.
     */
    private static Object generateValueByType(
            Type type,
            int depth,
            Set<Class<?>> generationPath
    ) {
        Class<?> rawClass = getRawClass(type);

        return generateValue(
                rawClass,
                type,
                null,
                depth,
                generationPath
        );
    }

    /**
     * Получает generic-параметр.
     *
     * Например:
     * List<String> -> String.
     */
    private static Type getGenericArgument(
            Type genericType,
            int argumentIndex
    ) {
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] arguments =
                    parameterizedType.getActualTypeArguments();

            if (argumentIndex < arguments.length) {
                return arguments[argumentIndex];
            }
        }

        return Object.class;
    }

    /**
     * Получает Class из Type.
     */
    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?> classType) {
            return classType;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            Type rawType = parameterizedType.getRawType();

            if (rawType instanceof Class<?> rawClass) {
                return rawClass;
            }
        }

        return Object.class;
    }

    /**
     * Создаёт экземпляр класса через
     * конструктор без аргументов.
     */
    private static Object createInstance(Class<?> modelClass) {
        try {
            Constructor<?> constructor =
                    modelClass.getDeclaredConstructor();

            constructor.setAccessible(true);

            return constructor.newInstance();
        } catch (NoSuchMethodException exception) {
            throw new IllegalArgumentException(
                    "У класса " + modelClass.getName()
                            + " должен быть конструктор без аргументов. "
                            + "Если используется Lombok, добавь "
                            + "@NoArgsConstructor.",
                    exception
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Не удалось создать объект класса: "
                            + modelClass.getName(),
                    exception
            );
        }
    }

    /**
     * Получает поля класса и его родительских классов.
     */
    private static List<Field> getAllFields(Class<?> modelClass) {
        List<Field> fields = new ArrayList<>();

        Class<?> currentClass = modelClass;

        while (currentClass != null
                && currentClass != Object.class) {

            fields.addAll(
                    List.of(currentClass.getDeclaredFields())
            );

            currentClass = currentClass.getSuperclass();
        }

        return fields;
    }

    /**
     * Пропускает статические, final,
     * transient и служебные поля.
     */
    private static boolean shouldSkipField(Field field) {
        int modifiers = field.getModifiers();

        return Modifier.isStatic(modifiers)
                || Modifier.isFinal(modifiers)
                || Modifier.isTransient(modifiers)
                || field.isSynthetic();
    }

    /**
     * Создаёт случайную строку.
     */
    private static String generateRandomString(int length) {
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(generateRandomCharacter());
        }

        return result.toString();
    }

    /**
     * Создаёт случайный буквенно-цифровой символ.
     */
    private static char generateRandomCharacter() {
        int index = RANDOM.nextInt(
                ALPHANUMERIC.length()
        );

        return ALPHANUMERIC.charAt(index);
    }

    public static float generateValidDepositAmount() {
        int amountInCents = RANDOM.nextInt(2, 500000);

        return amountInCents / 100f;
    }

    public static float generateNegativeDepositAmount() {
        int amountInCents = RANDOM.nextInt(1, 500001);

        return -(amountInCents / 100f);
    }

    public static float generateDepositAmountMoreThanMax() {
        int amountInCents = RANDOM.nextInt(500002, 1000001);

        return amountInCents / 100f;
    }

    public static int generateNonExistingAccountIdBasedOn(int existingAccountId) {
        int offset = RANDOM.nextInt(100_000, 1_000_000);

        return existingAccountId + offset;
    }

    public static String generateStringValue() {
        return generateRandomString(DEFAULT_STRING_LENGTH);
    }

    public static float generateValidTransferAmount() {
        int amountInCents = RANDOM.nextInt(2, 1_000_000);

        return amountInCents / 100f;
    }

    public static float generateNegativeTransferAmount() {
        int amountInCents = RANDOM.nextInt(1, 1_000_001);

        return -(amountInCents / 100f);
    }

    public static float generateTransferAmountMoreThanMax() {
        int amountInCents = RANDOM.nextInt(1_000_002, 2_000_001);

        return amountInCents / 100f;
    }

    public static float generateTransferAmountMoreThanBalance(float currentBalance) {
        if (currentBalance >= 10_000f) {
            throw new IllegalArgumentException(
                    "Current balance must be less than max transfer amount"
            );
        }

        int currentBalanceInCents = Math.round(currentBalance * 100);
        int amountInCents = RANDOM.nextInt(currentBalanceInCents + 1, 1_000_001);

        return amountInCents / 100f;
    }

    // Метод для создания имени в профиле
    public static String generateValidProfileName() {
        return generateRandomLettersWord() + " " + generateRandomLettersWord();
    }

    // Метод для генерации слова в имени в профиля
    private static String generateRandomLettersWord() {
        int wordLength = RANDOM.nextInt(3, 5);
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder word = new StringBuilder();

        for (int i = 0; i < wordLength; i++) {
            int randomIndex = RANDOM.nextInt(letters.length());
            word.append(letters.charAt(randomIndex));
        }

        return word.toString();
    }
}