package com.whysooman.jpa_study.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 모든 DB 의존 테스트가 동일한 MariaDB 컨테이너를 사용하도록 공통 베이스 클래스.
 * @ServiceConnection: Spring Boot가 DataSource 연결 정보를 자동 구성해줌 (Boot 3.1+) :contentReference[oaicite:2]{index=2}
 */
@Testcontainers
public abstract class MariaDbContainerBase {

    // abstract(애브스트랙트)
    @Container
    @ServiceConnection
    static final MariaDBContainer<?> MARIADB =
            new MariaDBContainer<>("mariadb:11.8");
}
