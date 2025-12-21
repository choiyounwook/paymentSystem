CREATE TABLE `payment_cancel_failure_history`(
    `id` bigint NOT NULL AUTO_INCREMENT,
    `imp_uid` varchar(50) NOT NULL,
    `reason` varchar(255) NOT NULL,
    `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb3;