package com.whysooman.jpa_study.domain;

import com.whysooman.jpa_study.repository.MemberRepository;
import com.whysooman.jpa_study.repository.TeamRepository;
import com.whysooman.jpa_study.support.MariaDbContainerBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CascadeOrphanTest extends MariaDbContainerBase {

    @Autowired
    TestEntityManager em;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("cascade=PERSIST: 부모(Team)만 persist해도 자식(Member)이 함께 저장된다")
    void cascadePersist_shouldPersistChildren_whenPersistParent() {
        // given
        Team team = new Team("team-A");
        Member m1 = new Member("member-1", "m1@example.com");
        Member m2 = new Member("member-2", "m2@example.com");

        team.addMember(m1);
        team.addMember(m2);

        // when: Team만 persist (Member persist 호출 없음)
        // 영속화. save와 동일. 영속성 컨텍스트에 저장함
        em.persist(team);
        em.flush(); // DB에 실제 INSERT가 나가도록 강제

        // then
        assertThat(team.getId()).isNotNull();
        assertThat(m1.getId()).isNotNull();
        assertThat(m2.getId()).isNotNull();

        assertThat(memberRepository.findByEmail("m1@example.com")).isPresent();
        assertThat(memberRepository.findByEmail("m2@example.com")).isPresent();
    }

    // 오어펀리무발
    // orphanRemoval members 에 orphanRemoval 설정값이 true 여야함
    // false 일 경우 삭제되지 않고(고아 제거 하지 않음) update 쿼리가 실행됨
    @Test
    @DisplayName("orphanRemoval=true: 부모 컬렉션에서 제거하면 자식이 DELETE 된다(Flush 시점에 반영)")
    void orphanRemoval_shouldDeleteChild_whenRemovedFromParentCollection() {
        // given
        Team team = new Team("team-B");
        Member m1 = new Member("member-1", "orphan1@example.com");
        team.addMember(m1);

        em.persist(team);
        em.flush();
        em.clear(); // 영속성 컨텍스트 초기화(진짜 DB 기준으로 검증)
        
        System.out.println("영속성 컨텍스트 초기화");

        // when
        Team foundTeam = em.find(Team.class, team.getId());

        System.out.println("getMembers 전");

        // 컬렉션 초기화 후(필요 시 SELECT 발생), 첫 멤버를 제거
        Member foundMember = foundTeam.getMembers().get(0);

        System.out.println("getMembers 후");

        foundTeam.removeMember(foundMember); // members.remove + member.team=null (양방향 동기화)
        em.flush();  // 여기서 DELETE SQL이 나가며 DB에 반영됨
        em.clear();

        System.out.println("flush 후");

        // then: member는 삭제, team은 유지
        assertThat(memberRepository.findByEmail("orphan1@example.com")).isEmpty();
        assertThat(teamRepository.findById(team.getId())).isPresent();
    }
}
