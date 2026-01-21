package com.whysooman.jpa_study.domain;

import com.whysooman.jpa_study.repository.MemberRepository;
import com.whysooman.jpa_study.support.MariaDbContainerBase;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
    Testcontainers 나 Docker Compose 기반 테스트 DB는 교체 대상에서 제외해서 AutoConfigureTestDatabase 생략 가능함
 */
@DataJpaTest
@ActiveProfiles("test")
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AssociationTest extends MariaDbContainerBase {

    @Autowired
    TestEntityManager em;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManagerFactory emf;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        // Hibernate Statistics 준비 (hibernate.generate_statistics=true 필요) :contentReference[oaicite:8]{index=8}
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        this.statistics = sessionFactory.getStatistics();
        this.statistics.clear();

        // ---------- 테스트 데이터 구성 ----------
        // Member.team은 LAZY로 설정되어 있어, 팀을 접근하는 순간 추가 SELECT가 발생할 수 있음.
        // 참고: JPA 스펙에서 @ManyToOne 기본 fetch는 EAGER(그래서 우리는 명시적으로 LAZY를 줌). :contentReference[oaicite:9]{index=9}
        for (int i = 1; i <= 5; i++) {
            Team team = new Team("team-" + i);
            Member member = new Member("member-" + i, "m" + i + "@example.com");
            team.addMember(member);        // 양방향 동기화
            em.persist(team);              // cascade=PERSIST로 member도 함께 persist
        }
        em.flush();
        em.clear();
        this.statistics.clear(); // INSERT 쿼리 카운트가 섞이지 않도록 다시 초기화
    }

    @Test
    @DisplayName("N+1 재현: members 1번 조회 + 각 member의 team 접근 시 team N번 추가 조회")
    void nPlusOne_occurs_when_accessing_lazy_toOne() {
        // when
        List<Member> members = memberRepository.findAll();      // 1 query
        members.forEach(m -> m.getTeam().getName());    // N queries (team이 각각 다르다고 가정)

        // then
        long preparedStatements = statistics.getPrepareStatementCount();

        // findAll 1회 + team 로딩 5회 = 총 6회가 기대값(배치 페치 설정이 없다는 전제)
        assertThat(preparedStatements).isEqualTo(6);
    }

    @Test
    @DisplayName("해결1: JPQL fetch join 사용 시 쿼리 1번으로 team까지 함께 로딩")
    void solve_with_fetch_join() {
        // when
        List<Member> members = memberRepository.findAllFetchTeam(); // join fetch
        members.forEach(m -> m.getTeam().getName());        // 추가 쿼리 없어야 함

        // then
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("해결2: Spring Data JPA @EntityGraph로 동적 fetch-graph 적용")
    void solve_with_entity_graph() {
        // when
        List<Member> members = memberRepository.findAllWithTeamGraph();
        members.forEach(m -> m.getTeam().getName());

        // then
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
