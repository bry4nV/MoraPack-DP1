DROP TABLE IF EXISTS moraTravelDaily.segment;
DROP TABLE IF EXISTS moraTravelDaily.route;
DROP TABLE IF EXISTS moraTravelDaily.shipment;
DROP TABLE IF EXISTS moraTravelDaily.user;
DROP TABLE IF EXISTS moraTravelDaily.order;
DROP TABLE IF EXISTS moraTravelDaily.flight;
DROP TABLE IF EXISTS moraTravelDaily.airport;

CREATE TABLE moraTravelDaily.airport (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  continent ENUM('America del Sur.', 'Europa', 'Asia') NOT NULL,
  code CHAR(4) NOT NULL UNIQUE,
  city VARCHAR(100) NOT NULL,
  country VARCHAR(100) NOT NULL,
  city_acronym VARCHAR(10) NOT NULL,
  gmt INT NOT NULL,
  capacity INT NOT NULL,
  latitude VARCHAR(20) NOT NULL,
  longitude VARCHAR(20) NOT NULL,
  status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  is_hub TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = Es sede principal / 0 = No es sede',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE moraTravelDaily.flight (
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
    REFERENCES moraTravelDaily.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

  CONSTRAINT fk_flight_destination
    FOREIGN KEY (airport_destination_code)
    REFERENCES moraTravelDaily.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE moraTravelDaily.order (
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
    REFERENCES moraTravelDaily.airport(code)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE moraTravelDaily.shipment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  order_id BIGINT UNSIGNED NOT NULL,
  quantity_products INT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_shipment_order
    FOREIGN KEY (order_id)
    REFERENCES moraTravelDaily.order(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE moraTravelDaily.route (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  shipment_id BIGINT UNSIGNED NOT NULL,
  creation_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('SCHEDULED', 'IN_TRANSIT', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  PRIMARY KEY (id),
  CONSTRAINT fk_route_shipment
    FOREIGN KEY (shipment_id)
    REFERENCES moraTravelDaily.shipment(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE moraTravelDaily.segment (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  route_id  BIGINT UNSIGNED NOT NULL,
  flight_id BIGINT UNSIGNED NOT NULL,
  segment_order INT NOT NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uk_segment_route_order (route_id, segment_order),

  CONSTRAINT fk_segment_route
    FOREIGN KEY (route_id)
    REFERENCES moraTravelDaily.route(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE,

  CONSTRAINT fk_segment_flight
    FOREIGN KEY (flight_id)
    REFERENCES moraTravelDaily.flight(id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE moraTravelDaily.user (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  second_last_name VARCHAR(100) NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  role ENUM('CLIENT', 'ADMIN', 'OPERATOR') NOT NULL DEFAULT 'CLIENT',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


