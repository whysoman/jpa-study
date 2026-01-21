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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlushDirtyCheckingTest extends MariaDbContainerBase {

    @Autowired
    TestEntityManager em;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    EntityManagerFactory emf;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        // Hibernate Statistics: SQL 개수/엔티티 업데이트 카운트를 수치로 확인하기 위함
        // (application-test.yml에서 hibernate.generate_statistics=true 필요)
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        this.statistics = sessionFactory.getStatistics();
        this.statistics.clear();

        // 테스트 간 데이터 격리를 위해 매번 초기화
        // - Team(부모) + Member(자식) 1개 생성
        Team team = new Team("team-tx");
        Member member = new Member("before", "flush1@example.com");
        team.addMember(member);

        em.persist(team);   // cascade=PERSIST로 Member도 함께 저장
        em.flush();
        em.clear();
        this.statistics.clear(); // 셋업 과정에서 발생한 SQL 카운트를 제거
    }

    @Test
    @DisplayName("더티체킹: save() 없이 managed 엔티티의 필드만 변경해도 flush 시 UPDATE가 실행된다")
    void dirtyChecking_updates_on_flush_without_save() {
        // given: 영속 상태(managed)로 로드
        Member member = memberRepository.findByEmail("flush1@example.com").orElseThrow();

        // when: save() 호출 없이 값 변경
        member.changeName("after");

        // flush 시점에 영속성 컨텍스트의 변경 사항이 DB와 동기화됨
        // Hibernate는 flush 시 변경 감지(dirty checking)를 통해 UPDATE를 생성/실행한다. :contentReference[oaicite:3]{index=3}
        em.flush();
        em.clear();

        // then: DB에서 다시 읽으면 값이 반영되어 있어야 함
        Member reloaded = memberRepository.findByEmail("flush1@example.com").orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("after");

        // 수치 검증(로그 + 통계)
        // - 엔티티 update 카운트가 1 이상이면 더티체킹에 의한 업데이트가 발생한 것
        assertThat(statistics.getEntityUpdateCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("AUTO flush: 변경된 내용이 쿼리 결과에 영향이 있으면, Hibernate가 쿼리 실행 전에 자동 flush 한다")
    void autoFlush_happens_before_query_when_needed() {
        // given
        Member member = memberRepository.findByEmail("flush1@example.com").orElseThrow();

        // managed 엔티티를 변경하되, 아직 flush 호출은 하지 않음
        member.changeName("keyword-hit");

        // when
        // Hibernate는 기본적으로 쿼리 실행 전, '아직 flush되지 않은 변경이 결과에 영향을 줄 수 있으면'
        // 자동 flush를 수행해 "쿼리가 stale 결과를 반환"하지 않게 한다. :contentReference[oaicite:4]{index=4}
        var page = memberRepository.searchByName("keyword-hit", PageRequest.of(0, 10));

        // 이름 변경 후 다시 조회
        // 조회 결과값에 영향이 있으므로 자동 flush 수행. 즉, update 수행 후 select 함
//        member = memberRepository.findByEmail("flush1@example.com").orElseThrow();

        // then: AUTO flush가 일어났다면, 변경된 이름으로 검색 결과가 잡혀야 함
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("keyword-hit");

//        assertThat(member.getName()).isEqualTo("keyword-hit");

        // 수치로도 확인: update가 최소 1회 발생해야 정상(자동 flush가 update를 반영)
        assertThat(statistics.getEntityUpdateCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("IDENTITY: ID는 INSERT 시점에 DB에서 생성된다(INSERT가 필요하므로 타이밍이 학습 포인트)")
    void identity_id_is_generated_by_insert() {
        // given
        Team team = new Team("team-identity");
        Member member = new Member("identity", "identity@example.com");
        team.addMember(member);

        System.out.println("persist 전");

        // when
        em.persist(team);

        System.out.println("persist 후");

        // then
        // IDENTITY는 DB가 INSERT 시점에 키를 만들기 때문에(즉, insert 기반 생성),
        // provider 입장에서 ID를 얻기 위해 insert가 필요한 구조다. :contentReference[oaicite:5]{index=5}
        // 실제 SQL 타이밍은 로그로 관찰하는 게 가장 확실함(테스트마다 최적화 차이 가능).
        assertThat(member.getId()).isNotNull();
    }
}
