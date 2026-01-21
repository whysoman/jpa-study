package com.whysooman.jpa_study.domain;

import com.whysooman.jpa_study.repository.MemberRepository;
import com.whysooman.jpa_study.repository.TeamRepository;
import com.whysooman.jpa_study.support.MariaDbContainerBase;
import jakarta.persistence.EntityManager;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class QueryTest extends MariaDbContainerBase {

    @Autowired
    TestEntityManager tem;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    EntityManagerFactory emf;

    private EntityManager em;
    private Statistics statistics;

    @BeforeEach
    void setUp() {
        this.em = tem.getEntityManager();

        SessionFactory sf = emf.unwrap(SessionFactory.class);
        this.statistics = sf.getStatistics();
        this.statistics.clear();

        // 데이터: Team 12개, 각 Team에 Member 3개 (총 36명)
        for (int t = 1; t <= 12; t++) {
            Team team = new Team("team-" + t);
            for (int m = 1; m <= 3; m++) {
                String email = "t" + t + "m" + m + "@example.com";
                Member member = new Member("member-" + t + "-" + m, email);
                team.addMember(member);
            }
            em.persist(team);
        }
        em.flush();
        em.clear();
        this.statistics.clear();
    }

    @Test
    @DisplayName("JPQL + Pageable: name like 검색을 페이지로 조회한다(Page는 count 쿼리가 추가로 실행됨)")
    void jpql_pagination_searchByName() {
        var page = memberRepository.searchByName("member-", PageRequest.of(0, 5));

        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(36);

        // Page는 일반적으로 select + count로 2회 이상 SQL 준비가 발생 :contentReference[oaicite:7]{index=7}
        assertThat(statistics.getPrepareStatementCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("N+1 재현: Member 조회 후 team 접근 시 team select가 반복된다")
    void nPlusOne_on_lazy_toOne() {
        List<Member> members = memberRepository.findAll(); // 1
        members.forEach(m -> m.getTeam().getName());       // N

        long stmt = statistics.getPrepareStatementCount();
        // 36명 기준이면 1 + 12 정도(팀이 12개)로 기대할 수도 있지만,
        // 프록시 초기화/캐시 상황에 따라 정확한 숫자는 달라질 수 있어 "N+1급" 여부만 잡습니다.
        assertThat(stmt).isGreaterThan(10);
    }

    @Test
    @DisplayName("해결(to-one): EntityGraph + Pageable로 N+1 없이 조회한다")
    void solve_toOne_with_entityGraph_and_pageable() {
        statistics.clear();

        var page = memberRepository.findAll(PageRequest.of(0, 10));
        page.getContent().forEach(m -> m.getTeam().getName());

        long stmt = statistics.getPrepareStatementCount();

        // Page는 count 때문에 최소 2회는 발생할 수 있음 :contentReference[oaicite:8]{index=8}
        // 하지만 N+1이라면 2 + 10 이상으로 확 늘어나므로, 상한으로 방어합니다.
        assertThat(stmt).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("주의(to-many): 컬렉션 fetch join + pagination은 fail_on 설정 시 예외로 막는다")
    void collection_fetch_join_with_pagination_should_fail() {
        // Hibernate는 컬렉션 fetch join + pagination이면 limit을 DB가 아니라 메모리에서 적용해야 할 수 있고,
        // 성능이 매우 나쁘므로 예외로 막는 설정을 제공 :contentReference[oaicite:9]{index=9}
        assertThatThrownBy(() ->
                em.createQuery(
                        "select t from Team t join fetch t.members order by t.id",
                        Team.class
                ).setFirstResult(0).setMaxResults(5).getResultList()
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("해결(to-many paging): 2-step(IDs Slice -> IN fetch join)로 안정적인 pagination을 만든다")
    void two_step_pagination_for_toMany() {
        statistics.clear();

        var idsSlice = teamRepository.findIdSlice(PageRequest.of(0, 5));
        List<Long> ids = idsSlice.getContent();

        List<Team> teams = teamRepository.findWithMembersByIdIn(ids);
        assertThat(teams).hasSize(5);
        teams.forEach(t -> assertThat(t.getMembers()).hasSize(3)); // members가 이미 로딩되어 있어야 함

        // Slice(IDs 1회) + fetch join 1회 = 보통 2회 :contentReference[oaicite:10]{index=10}
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
