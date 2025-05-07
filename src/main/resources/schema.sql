DROP TABLE IF EXISTS member_coupon;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS member;

CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    count BIGINT NOT NULL,
    coupon_type VARCHAR(50) NOT NULL,
    expire_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS member_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    used BOOLEAN NOT NULL,
    used_at DATETIME NOT NULL,
    CONSTRAINT fk_member_coupon_member
    FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_member_coupon_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupon(id)
);
