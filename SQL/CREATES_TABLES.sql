DROP TABLE IF EXISTS moraTravelSimulation.airport;
CREATE TABLE moraTravelSimulation.airport (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  continent ENUM('America del Sur.', 'Europa', 'Asia') NOT NULL,
  code CHAR(4) NOT NULL UNIQUE,
  city VARCHAR(100) DEFAULT NULL,
  country VARCHAR(100) DEFAULT NULL,
  city_acronym VARCHAR(10) DEFAULT NULL,
  gmt INT DEFAULT NULL,
  capacity INT DEFAULT NULL,
  latitude VARCHAR(20) DEFAULT NULL,
  longitude VARCHAR(20) DEFAULT NULL,
  status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  is_hub TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = Es sede principal / 0 = No es sede',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS moraTravelSimulation.flight;
CREATE TABLE moraTravelSimulation.flight (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

  airport_origin_code CHAR(4) NOT NULL,
  airport_destination_code CHAR(4) NOT NULL,
  flight_date DATE NOT NULL,
  departure_time TIME NOT NULL,
  arrival_time TIME NOT NULL,
  capacity SMALLINT UNSIGNED NOT NULL,
  status ENUM('SCHEDULED', 'CANCELLED', 'COMPLETED', 'DELAYED')
         NOT NULL DEFAULT 'SCHEDULED',

  PRIMARY KEY (id),

  CONSTRAINT fk_flight_origin
    FOREIGN KEY (airport_origin_code)
    REFERENCES moraTravelSimulation.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

  CONSTRAINT fk_flight_destination
    FOREIGN KEY (airport_destination_code)
    REFERENCES moraTravelSimulation.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

DROP TABLE IF EXISTS moraTravelSimulation.order;
CREATE TABLE moraTravelSimulation.order (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_number CHAR(9) NOT NULL,
  order_date DATE NOT NULL,
  order_time TIME NOT NULL,
  airport_destination_code CHAR(4) NOT NULL,
  quantity SMALLINT UNSIGNED NOT NULL,
  client_code CHAR(7) NOT NULL,
  status ENUM('UNASSIGNED', 'PENDING', 'IN_TRANSIT', 'COMPLETED') NOT NULL DEFAULT 'UNASSIGNED',
  PRIMARY KEY (id),

  CONSTRAINT fk_order_airport_dest
    FOREIGN KEY (airport_destination_code)
    REFERENCES moraTravelSimulation.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
