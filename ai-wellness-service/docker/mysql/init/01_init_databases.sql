# MySQL grants the wellness_user access to both the dev and main databases.
# This script runs once when the Docker container is first initialised.
# It contains NO business data — only database infrastructure setup.

CREATE DATABASE IF NOT EXISTS wellness CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wellness_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON wellness.* TO 'wellness_user'@'%';
GRANT ALL PRIVILEGES ON wellness_dev.* TO 'wellness_user'@'%';
FLUSH PRIVILEGES;
