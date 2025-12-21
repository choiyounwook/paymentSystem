CREATE TABLE `settlements`(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `partner_id` bigint DEFAULT NULL,
    `total_amount` decimal(15,2) NOT NULL,
    `status` varchar(20) DEFAULT NULL,
    `payment_date` datetime DEFAULT NULL,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb3;