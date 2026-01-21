package com.whysooman.jpa_study.service;

import com.whysooman.jpa_study.domain.Member;
import com.whysooman.jpa_study.domain.Team;
import com.whysooman.jpa_study.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    @PersistenceContext
    private EntityManager em;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /** 정상 커밋 케이스 */
    @Transactional
    public Long createMember(String name, String email) {
        Team team = new Team("tx-team");
        Member member = new Member(name, email);
        team.addMember(member);

        // Team.persist는 예제에서 사용하던 cascade=PERSIST 전제
        em.persist(team);

        return member.getId();
    }

    /** RuntimeException -> 기본 롤백 */
    @Transactional
    public void createThenThrowRuntime(String name, String email) {
        createMember(name, email);
        throw new IllegalStateException("boom");
    }

    /** Checked exception -> 기본은 커밋(주의 포인트) */
    @Transactional
    public void createThenThrowChecked(String name, String email) throws CheckedBusinessException {
        createMember(name, email);
        throw new CheckedBusinessException("checked boom");
    }

    /** Checked exception도 롤백시키고 싶으면 rollbackFor 지정 */
    @Transactional(rollbackFor = CheckedBusinessException.class)
    public void createThenThrowCheckedRollback(String name, String email) throws CheckedBusinessException {
        createMember(name, email);
        throw new CheckedBusinessException("checked boom");
    }

    /** 더티체킹 예시: save 없이 변경 */
    @Transactional
    public void changeName(Long memberId, String newName) {
        Member member = memberRepository.findById(memberId).orElseThrow();

        member.changeName(newName);
        // save 호출 없음 -> flush 시 dirty checking으로 UPDATE 기대
    }
}
