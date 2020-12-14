CREATE TABLE punishments
(
    id        BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    type      TINYINT UNSIGNED NOT NULL,
    target    CHAR(36)         NOT NULL,
    -- UUID of (0, 0) if console:
    punisher  CHAR(36)         NOT NULL,
    reason    TINYTEXT                  DEFAULT NULL,
    lifted    BOOLEAN          NOT NULL DEFAULT FALSE,
    -- UUID of (0, 0) for console:
    lifted_by CHAR(36),
    time      BIGINT UNSIGNED  NOT NULL,
    -- 0 for permanent:
    duration  BIGINT UNSIGNED  NOT NULL,

    PRIMARY KEY (id),
    INDEX (target),
    INDEX (punisher)
);

CREATE TABLE users
(
    uuid      CHAR(36)    NOT NULL,
    username  VARCHAR(24) NOT NULL,
    timestamp TIMESTAMP   NOT NULL DEFAULT NOW(),

    PRIMARY KEY (uuid),
    UNIQUE INDEX (username)
);

CREATE TABLE usernames
(
    uuid      CHAR(36)    NOT NULL,
    username  VARCHAR(24) NOT NULL,
    timestamp TIMESTAMP,

    PRIMARY KEY (uuid, username, timestamp),
    INDEX (uuid)
);