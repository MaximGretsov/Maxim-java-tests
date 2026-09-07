#!/usr/bin/env bash

set -Eeuo pipefail

# Переходим в папку, где находится скрипт.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

TEST_IMAGE="${TEST_IMAGE:-maximka17/senior-api-tests:latest}"
BROWSERS_CONFIG="./config/browsers.json"

cleanup() {
    local exit_code=$?

    # Отключаем повторный вызов cleanup.
    trap - EXIT

    echo ">>> Остановка тестового окружения"
    docker compose down --remove-orphans || true

    if [[ $exit_code -eq 0 ]]; then
        echo ">>> Окружение остановлено. Все тесты завершились успешно"
    else
        echo ">>> Окружение остановлено. Тесты завершились с ошибкой: $exit_code"
    fi

    exit "$exit_code"
}

trap cleanup EXIT

wait_for_url() {
    local service_name="$1"
    local url="$2"
    local attempts=60
    local http_code

    echo ">>> Ожидание готовности сервиса: $service_name ($url)"

    for ((attempt=1; attempt<=attempts; attempt++)); do
        http_code="$(
            curl \
                --silent \
                --output /dev/null \
                --write-out '%{http_code}' \
                --max-time 2 \
                "$url" || true
        )"

        # Любой HTTP-ответ означает, что сервис принимает соединения.
        if [[ -n "$http_code" && "$http_code" != "000" ]]; then
            echo ">>> Сервис $service_name готов. HTTP-код: $http_code"
            return 0
        fi

        sleep 2
    done

    echo ">>> Ошибка: сервис $service_name не запустился"
    docker compose ps
    docker compose logs --tail=100
    return 1
}

run_tests() {
    local profile="$1"

    echo ">>> Запуск тестов с профилем: $profile"

    docker run --rm \
        --network host \
        --dns 8.8.8.8 \
        --dns 1.1.1.1 \
        -e MAVEN_OPTS="-Djava.net.preferIPv4Stack=true" \
        -e TEST_PROFILE="$profile" \
        -e APIBASEURL=http://localhost:4111 \
        -e UIBASEURL=http://localhost:3000 \
        -e SELENOID_URL=http://localhost:4444 \
        -e SELENOID_UI_URL=http://localhost:8080 \
        "$TEST_IMAGE"
}

echo ">>> Проверка Docker"

command -v docker >/dev/null 2>&1 || {
    echo ">>> Ошибка: Docker не установлен или недоступен в PATH"
    exit 1
}

docker info >/dev/null 2>&1 || {
    echo ">>> Ошибка: Docker Engine не запущен"
    exit 1
}

docker compose version >/dev/null 2>&1 || {
    echo ">>> Ошибка: команда docker compose недоступна"
    exit 1
}

command -v curl >/dev/null 2>&1 || {
    echo ">>> Ошибка: curl не установлен или недоступен в PATH"
    exit 1
}

[[ -f "$BROWSERS_CONFIG" ]] || {
    echo ">>> Ошибка: не найден файл $BROWSERS_CONFIG"
    exit 1
}

echo ">>> Остановка ранее запущенного окружения"
docker compose down --remove-orphans

echo ">>> Загрузка образов браузеров для Selenoid"

while IFS= read -r browser_image; do
    [[ -z "$browser_image" ]] && continue

    echo ">>> Загрузка браузера: $browser_image"
    docker pull "$browser_image"
done < <(
    grep -oE '"image"[[:space:]]*:[[:space:]]*"[^"]+"' "$BROWSERS_CONFIG" \
        | cut -d '"' -f 4 \
        | sort -u
)

echo ">>> Запуск тестового окружения через Docker Compose"
docker compose up -d

echo ">>> Текущее состояние контейнеров"
docker compose ps

wait_for_url "backend" "http://localhost:4111"
wait_for_url "frontend через Nginx" "http://localhost:3000"
wait_for_url "Selenoid" "http://localhost:4444/status"
wait_for_url "Selenoid UI" "http://localhost:8080"

echo ">>> Загрузка образа с API и UI тестами: $TEST_IMAGE"
docker pull "$TEST_IMAGE"

echo ">>> Запуск всех API и UI тестов"

# Не прерываем скрипт после падения API-тестов,
# чтобы UI-тесты тоже обязательно запустились.
set +e

run_tests api
API_EXIT_CODE=$?

run_tests ui
UI_EXIT_CODE=$?

set -e

echo ">>> Результат API-тестов: $API_EXIT_CODE"
echo ">>> Результат UI-тестов: $UI_EXIT_CODE"

if [[ $API_EXIT_CODE -ne 0 || $UI_EXIT_CODE -ne 0 ]]; then
    echo ">>> Один или несколько наборов тестов завершились с ошибкой"
    exit 1
fi

echo ">>> Все API и UI тесты завершились успешно"