# PowerShell скрипт для установки PostgreSQL JDBC драйвера как модуля в WildFly

$WILDFLY_HOME = "C:\servers\wildfly-34.0.0.Final"
$POSTGRES_VERSION = "42.7.1"

Write-Host "=== Установка PostgreSQL модуля в WildFly ===" -ForegroundColor Green

# 1. Создаем структуру директорий для модуля
$MODULE_DIR = "$WILDFLY_HOME\modules\system\layers\base\org\postgresql\main"
Write-Host "Создание директории модуля: $MODULE_DIR" -ForegroundColor Cyan

if (-not (Test-Path $MODULE_DIR)) {
    New-Item -ItemType Directory -Path $MODULE_DIR -Force | Out-Null
    Write-Host "✓ Директория создана" -ForegroundColor Green
} else {
    Write-Host "✓ Директория уже существует" -ForegroundColor Yellow
}

# 2. Скачиваем PostgreSQL JDBC драйвер
$JDBC_JAR = "postgresql-$POSTGRES_VERSION.jar"
$JDBC_PATH = "$MODULE_DIR\$JDBC_JAR"

if (-not (Test-Path $JDBC_PATH)) {
    Write-Host "Скачивание PostgreSQL JDBC драйвера версии $POSTGRES_VERSION..." -ForegroundColor Cyan
    $DOWNLOAD_URL = "https://jdbc.postgresql.org/download/$JDBC_JAR"
    
    try {
        Invoke-WebRequest -Uri $DOWNLOAD_URL -OutFile $JDBC_PATH -UseBasicParsing
        Write-Host "✓ Драйвер скачан: $JDBC_PATH" -ForegroundColor Green
    } catch {
        Write-Host "✗ Ошибка скачивания: $_" -ForegroundColor Red
        Write-Host "Попробуйте скачать вручную с: $DOWNLOAD_URL" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "✓ Драйвер уже существует: $JDBC_PATH" -ForegroundColor Yellow
}

# 3. Создаем module.xml
$MODULE_XML = @"
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="$JDBC_JAR"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
        <module name="javax.servlet.api" optional="true"/>
    </dependencies>
</module>
"@

$MODULE_XML_PATH = "$MODULE_DIR\module.xml"
Write-Host "Создание module.xml..." -ForegroundColor Cyan
$MODULE_XML | Out-File -FilePath $MODULE_XML_PATH -Encoding UTF8 -Force
Write-Host "✓ module.xml создан" -ForegroundColor Green

# 4. Проверка установки
Write-Host "`n=== Проверка установки ===" -ForegroundColor Green
Write-Host "Файлы модуля:" -ForegroundColor Cyan
Get-ChildItem $MODULE_DIR | ForEach-Object { Write-Host "  - $($_.Name)" }

Write-Host "`n✓ PostgreSQL модуль успешно установлен!" -ForegroundColor Green
Write-Host "`nТеперь вы можете:" -ForegroundColor Cyan
Write-Host "1. Запустить WildFly: $WILDFLY_HOME\bin\standalone.bat" -ForegroundColor White
Write-Host "2. Задеплоить WAR: jboss-cli.bat --connect --command=`"deploy C:\ITMO_University\is\lab1\backend\build\libs\backend.war --force`"" -ForegroundColor White

