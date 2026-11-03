# Защита ЗИоВПО: инструкция по кликам

## Один раз перед защитой

1. Нажмите клавишу Windows, напишите `Docker Desktop`, нажмите Enter. Дождитесь сообщения, что Docker работает.
2. Откройте IntelliJ IDEA. На стартовом экране нажмите **Open** и выберите папку `C:\Users\acer\Documents\ChatGPT\zashita\ziovpo_2025`.
3. В IntelliJ нажмите **View -> Tool Windows -> Terminal**.
4. Вставьте команду и нажмите Enter:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\start-defense.ps1
   ```

5. Не закрывайте этот Terminal. Готовность сервера подтверждает строка `Started ZiovpoApplication`.
6. Откройте Postman. Нажмите значок шестерёнки **Settings -> General** и выключите **SSL certificate verification**, потому что локальный HTTPS использует учебный самоподписанный сертификат.
7. Нажмите **Import -> Files** и выберите `C:\Users\acer\Documents\ChatGPT\zashita\ziovpo_2025\demo\ZIiVPO-2025.postman_collection.json`.
8. В левой панели откройте коллекцию **ZIiVPO 2025 - Defense Demo**. Открывайте папки по порядку и в каждом запросе нажимайте **Send** сверху вниз. После ответа откройте вкладку **Test Results**: тесты должны быть зелёными.
9. В запросе **6 - MinIO -> 01 Upload sample file**, если Postman пишет, что файл не найден, откройте **Body -> form-data**, у строки `file` нажмите **Select Files** и выберите `demo\sample.txt`.

Логин демонстрационного администратора: `admin`. Пароль: `AdminDemo1!`.

## Что говорить при показе запросов

1. **JWT and roles**: «Login возвращает access и refresh token. Refresh выдаёт новую пару, повтор старого refresh получает 401. Публичная регистрация всегда создаёт USER, а ADMIN endpoint возвращает ему 403».
2. **Licenses and EDS**: «Создаю тип, продукт и лицензию. Активация возвращает Ticket со временем, сроком, userId, deviceId, блокировкой и непустой электронной подписью. Затем выполняю check и renew».
3. **Signatures**: «Показываю create, update, выборку по ID, full, increment, history, audit и логическое delete. Удалённая запись исчезает из full, но приходит в increment со статусом DELETED».
4. **Binary API**: «Ответ имеет Content-Type multipart/mixed, две бинарные части и magic numbers MF-Erdeneuul и DB-Erdeneuul. Целые числа записываются в Big Endian».
5. **MinIO**: «Файл сохраняется в приватный bucket от имени отдельного пользователя приложения. Сервер выдаёт временный pre-signed URL; анонимный доступ к bucket возвращает 403».

## GitHub: что открыть комиссии

1. Откройте репозиторий на GitHub и вкладку **Actions**. Откройте последний зелёный workflow, раскройте шаги **Test** и **Build JAR**.
2. Откройте **Settings -> Secrets and variables -> Actions**. Покажите только имена секретов, значения никогда не открывайте.
3. Для добавления каждого секрета нажмите **New repository secret**. В Terminal IntelliJ выполните команду ниже, заменяя имя, затем вставьте значение из буфера через Ctrl+V:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\copy-github-secret.ps1 KEYSTORE_B64
   ```

4. Создайте девять секретов: `KEYSTORE_B64`, `SIGNATURE_PUBLIC_CERT_B64`, `KEYSTORE_PASSWORD`, `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`.
5. В GitHub откройте вкладку **Code** и файл `.github/workflows/ci.yml`, чтобы показать отдельные команды тестирования, сборки JAR и публикации artifact.

## Резервный показ одной командой

Если Postman не открылся, откройте второй Terminal в IntelliJ и выполните:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-defense-demo.ps1
```

В конце должна появиться зелёная строка `ALL DEFENSE DEMO CHECKS PASSED`.
